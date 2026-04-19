package tn.esprit.controllers.admin;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import tn.esprit.models.Course;
import tn.esprit.services.CourseService;
import tn.esprit.services.FreeImageContentService;
import tn.esprit.services.LocalAiContentService;
import tn.esprit.util.AppSettings;
import tn.esprit.util.FormValidator;
import tn.esprit.util.SweetAlert;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class CourseFormController {
    private static final int TITLE_MAX_LENGTH = 120;
    private static final int SUBJECT_MAX_LENGTH = 80;
    private static final int DESCRIPTION_MAX_LENGTH = 1000;

    @FXML
    private Label badgeLabel;
    @FXML
    private Label pageTitleLabel;
    @FXML
    private Label pageSubtitleLabel;
    @FXML
    private Label previewTitleLabel;
    @FXML
    private Label previewSubjectLabel;
    @FXML
    private Label previewLevelLabel;
    @FXML
    private Label previewStatusLabel;
    @FXML
    private Label previewDescriptionLabel;
    @FXML
    private Label aiStatusLabel;
    @FXML
    private Label previewImageStatusLabel;
    @FXML
    private TextField titleField;
    @FXML
    private TextField subjectField;
    @FXML
    private ComboBox<String> levelComboBox;
    @FXML
    private ComboBox<String> statusComboBox;
    @FXML
    private ComboBox<String> translationLanguageComboBox;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Button saveButton;
    @FXML
    private Button generateDescriptionButton;
    @FXML
    private Button suggestObjectivesButton;
    @FXML
    private Button translateButton;
    @FXML
    private Button generateImageButton;
    @FXML
    private Button configureAiButton;
    @FXML
    private ImageView previewImageView;

    private final CourseService courseService = new CourseService();
    private final LocalAiContentService localAiContentService = new LocalAiContentService();
    private final FreeImageContentService freeImageContentService = new FreeImageContentService();
    private Course selectedCourse;
    private boolean editMode;
    private String courseImagePath = "course-default.png";

    @FXML
    private void initialize() {
        titleField.setTextFormatter(FormValidator.createLengthFormatter(TITLE_MAX_LENGTH));
        subjectField.setTextFormatter(FormValidator.createLengthFormatter(SUBJECT_MAX_LENGTH));
        descriptionArea.setTextFormatter(FormValidator.createLengthFormatter(DESCRIPTION_MAX_LENGTH));

        levelComboBox.getItems().setAll(IntStream.rangeClosed(1, 9).mapToObj(level -> "Niveau " + level).toList());
        levelComboBox.setValue("Niveau 1");
        statusComboBox.getItems().setAll("DRAFT", "PUBLISHED", "ARCHIVED");
        statusComboBox.setValue("DRAFT");
        translationLanguageComboBox.getItems().setAll("French", "English", "Arabic");
        translationLanguageComboBox.setValue("French");
        generateImageButton.setText("Generate Cover Image");

        titleField.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        subjectField.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        levelComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        statusComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        descriptionArea.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());

        refreshAiState();
        refreshPreview();
    }

    public void initForCreate() {
        editMode = false;
        selectedCourse = null;
        badgeLabel.setText("Create Course");
        pageTitleLabel.setText("Launch a new EduKids course");
        pageSubtitleLabel.setText("Use a dedicated page to prepare the course details before publishing them to the EduKids database.");
        saveButton.setText("Submit Course");
        courseImagePath = "course-default.png";
        handleResetForm();
    }

    public void initForEdit(Course course) {
        editMode = true;
        selectedCourse = course;
        badgeLabel.setText("Update Course");
        pageTitleLabel.setText("Refine an existing course page");
        pageSubtitleLabel.setText("Edit the selected EduKids course in a dedicated workspace, then confirm the update with a quick popup.");
        saveButton.setText("Save Changes");

        titleField.setText(course.getTitle());
        subjectField.setText(course.getSubject());
        levelComboBox.setValue(course.getLevelText());
        statusComboBox.setValue(course.getStatus());
        descriptionArea.setText(course.getDescription());
        courseImagePath = course.getImage();
        refreshPreview();
    }

    @FXML
    private void handleSaveCourse() {
        if (!isValidForm()) {
            return;
        }

        String title = titleField.getText().trim();
        String subject = subjectField.getText().trim();
        String description = descriptionArea.getText().trim();
        int level = mapLevel(levelComboBox.getValue());
        String status = safeValue(statusComboBox.getValue(), "DRAFT");

        try {
            Course course;
            if (editMode && selectedCourse != null) {
                courseService.updateCourse(selectedCourse, title, description, level, subject, status, courseImagePath);
                course = selectedCourse;
            } else {
                course = courseService.addCourse(title, description, level, subject, status, courseImagePath);
            }

            String actionLabel = editMode ? "Course updated" : "Course added";
            SweetAlert.success(actionLabel, "The course \"" + course.getTitle() + "\" was saved successfully.");
            AdminShellController.getInstance().showCourses();
        } catch (RuntimeException exception) {
            SweetAlert.error("Database Error", exception.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        AdminShellController.getInstance().showCourses();
    }

    @FXML
    private void handleResetForm() {
        if (editMode && selectedCourse != null) {
            titleField.setText(selectedCourse.getTitle());
            subjectField.setText(selectedCourse.getSubject());
            levelComboBox.setValue(selectedCourse.getLevelText());
            statusComboBox.setValue(selectedCourse.getStatus());
            descriptionArea.setText(selectedCourse.getDescription());
            courseImagePath = selectedCourse.getImage();
        } else {
            titleField.clear();
            subjectField.clear();
            levelComboBox.setValue("Niveau 1");
            statusComboBox.setValue("DRAFT");
            descriptionArea.clear();
            courseImagePath = "course-default.png";
        }
        translationLanguageComboBox.setValue("French");
        refreshAiState();
        refreshPreview();
    }

    @FXML
    private void handleGenerateDescription() {
        if (!hasMinimumCourseContext()) {
            SweetAlert.warning("AI Assistant", "Enter at least a course title and subject before generating a description.");
            return;
        }

        runAiTask(
                "Generating a course description...",
                () -> localAiContentService.generateCourseDescription(
                        titleField.getText(),
                        subjectField.getText(),
                        mapLevel(levelComboBox.getValue()),
                        statusComboBox.getValue(),
                        descriptionArea.getText()
                ),
                generatedDescription -> {
                    descriptionArea.setText(fitToLength(generatedDescription, DESCRIPTION_MAX_LENGTH));
                    refreshPreview();
                    updateAiStatus("Description generated. You can still edit it before saving.", "ai-status-success");
                }
        );
    }

    @FXML
    private void handleSuggestObjectives() {
        if (!hasMinimumCourseContext()) {
            SweetAlert.warning("AI Assistant", "Enter at least a course title and subject before suggesting objectives.");
            return;
        }

        runAiTask(
                "Preparing pedagogical objectives...",
                () -> localAiContentService.suggestObjectives(
                        titleField.getText(),
                        subjectField.getText(),
                        mapLevel(levelComboBox.getValue()),
                        descriptionArea.getText()
                ),
                objectives -> {
                    descriptionArea.setText(fitToLength(mergeDescriptionWithObjectives(descriptionArea.getText(), objectives), DESCRIPTION_MAX_LENGTH));
                    refreshPreview();
                    updateAiStatus("Objectives added to the description area.", "ai-status-success");
                }
        );
    }

    @FXML
    private void handleTranslateCourse() {
        if (!hasAnyCourseContent()) {
            SweetAlert.warning("AI Assistant", "Add some course content before requesting a translation.");
            return;
        }

        runAiTask(
                "Translating the course content...",
                () -> localAiContentService.translateCourse(
                        titleField.getText(),
                        subjectField.getText(),
                        descriptionArea.getText(),
                        translationLanguageComboBox.getValue()
                ),
                translatedContent -> {
                    titleField.setText(fitToLength(translatedContent.title(), TITLE_MAX_LENGTH));
                    subjectField.setText(fitToLength(translatedContent.subject(), SUBJECT_MAX_LENGTH));
                    descriptionArea.setText(fitToLength(translatedContent.description(), DESCRIPTION_MAX_LENGTH));
                    refreshPreview();
                    updateAiStatus("Course content translated to " + safeValue(translationLanguageComboBox.getValue(), "the selected language") + ".", "ai-status-success");
                }
        );
    }

    @FXML
    private void handleGenerateImage() {
        if (!hasMinimumCourseContext()) {
            SweetAlert.warning("AI Assistant", "Enter at least a course title and subject before generating a cover image.");
            return;
        }

        runAiTask(
                "Creating a course cover image...",
                () -> freeImageContentService.generateCourseImage(
                        titleField.getText(),
                        subjectField.getText(),
                        mapLevel(levelComboBox.getValue()),
                        descriptionArea.getText()
                ),
                generatedImagePath -> {
                    courseImagePath = generatedImagePath;
                    refreshPreview();
                    updateAiStatus("Cover image generated and attached to this course.", "ai-status-success");
                }
        );
    }

    @FXML
    private void handleConfigureAi() {
        showLocalAiSetupDialog();
    }

    private void refreshPreview() {
        String title = safeValue(titleField.getText(), "Your course title will appear here");
        String subject = safeValue(subjectField.getText(), "Select a subject");
        String level = safeValue(levelComboBox.getValue(), "Niveau 1");
        String status = safeValue(statusComboBox.getValue(), "DRAFT");
        String description = safeValue(descriptionArea.getText(), "A short and engaging summary will help parents and students understand the value of the course.");

        previewTitleLabel.setText(title);
        previewSubjectLabel.setText(subject);
        previewLevelLabel.setText(level);
        previewStatusLabel.setText(toDisplayStatus(status));
        previewDescriptionLabel.setText(description);
        refreshPreviewImage();
    }

    private boolean isValidForm() {
        String validationMessage = FormValidator.validateCourse(
                titleField.getText(),
                subjectField.getText(),
                descriptionArea.getText()
        );
        if (validationMessage != null) {
            SweetAlert.warning("Validation", validationMessage);
            return false;
        }
        return true;
    }

    private int mapLevel(String levelLabel) {
        if (levelLabel == null) {
            return 1;
        }
        String digits = levelLabel.replaceAll("\\D+", "");
        return digits.isBlank() ? 1 : Integer.parseInt(digits);
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String toDisplayStatus(String value) {
        return switch (safeValue(value, "DRAFT")) {
            case "PUBLISHED" -> "Published";
            case "ARCHIVED" -> "Archived";
            default -> "Draft";
        };
    }

    private <T> void runAiTask(String runningMessage, Supplier<T> taskSupplier, Consumer<T> successHandler) {
        setAiBusy(true, runningMessage);

        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return taskSupplier.get();
            }
        };

        task.setOnSucceeded(event -> {
            try {
                successHandler.accept(task.getValue());
            } finally {
                setAiBusy(false, null);
            }
        });

        task.setOnFailed(event -> {
            setAiBusy(false, null);
            String message = task.getException() != null && task.getException().getMessage() != null
                    ? task.getException().getMessage()
                    : "The AI request could not be completed.";
            updateAiStatus(message, "ai-status-error");
            if (message.toLowerCase().contains("local ai")
                    || message.toLowerCase().contains("ollama")
                    || message.toLowerCase().contains("model not found")) {
                SweetAlert.warning("AI Assistant", message);
            } else {
                SweetAlert.error("AI Assistant", message);
            }
        });

        Thread worker = new Thread(task, "course-ai-request");
        worker.setDaemon(true);
        worker.start();
    }

    private void setAiBusy(boolean busy, String message) {
        generateDescriptionButton.setDisable(busy);
        suggestObjectivesButton.setDisable(busy);
        translateButton.setDisable(busy);
        generateImageButton.setDisable(busy);
        translationLanguageComboBox.setDisable(busy);

        if (busy && message != null) {
            updateAiStatus(message, "ai-status-running");
        }
    }

    private void refreshAiState() {
        configureAiButton.setText("Local AI Setup");
        updateAiStatus(
                "Text uses local Ollama. Images use the free Pollinations provider (" + freeImageContentService.getImageModel() + ").",
                "ai-status-idle"
        );
    }

    private void updateAiStatus(String message, String variantStyle) {
        aiStatusLabel.setText(safeValue(message, "AI assistant ready."));
        aiStatusLabel.getStyleClass().removeAll("ai-status-idle", "ai-status-warning", "ai-status-running", "ai-status-success", "ai-status-error");
        if (!aiStatusLabel.getStyleClass().contains("ai-status-label")) {
            aiStatusLabel.getStyleClass().add("ai-status-label");
        }
        aiStatusLabel.getStyleClass().add(variantStyle);
    }

    private void showLocalAiSetupDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Local AI Setup");
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(getClass().getResource("/tn/esprit/css/styles.css").toExternalForm());
        pane.getStyleClass().addAll("sweet-alert");

        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType saveButtonType = new ButtonType("Save Setup", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().setAll(cancelButton, saveButtonType);

        TextField baseUrlField = new TextField(AppSettings.get("OLLAMA_BASE_URL", localAiContentService.getBaseUrl()));
        baseUrlField.setPromptText("http://localhost:11434/api");

        TextField modelField = new TextField(AppSettings.get("OLLAMA_TEXT_MODEL", localAiContentService.getTextModel()));
        modelField.setPromptText("gemma3");

        Label helperLabel = new Label("EduKids uses Ollama for free local text generation and Pollinations for free cover images. Install Ollama, start it, then pull a model such as: ollama pull gemma3");
        helperLabel.setWrapText(true);
        helperLabel.getStyleClass().add("hero-text");

        Label baseUrlLabel = new Label("Base URL");
        Label modelLabel = new Label("Text Model");
        Label imageProviderLabel = new Label("Image Provider");
        Label imageProviderValue = new Label(freeImageContentService.getBaseUrl() + " [" + freeImageContentService.getImageModel() + "]");
        imageProviderValue.setWrapText(true);
        imageProviderValue.getStyleClass().add("hero-text");
        VBox content = new VBox(12, helperLabel, baseUrlLabel, baseUrlField, modelLabel, modelField, imageProviderLabel, imageProviderValue);
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPadding(new Insets(8, 6, 2, 6));
        pane.setContent(content);

        Button saveDialogButton = (Button) pane.lookupButton(saveButtonType);
        Button cancelDialogButton = (Button) pane.lookupButton(cancelButton);
        if (saveDialogButton != null) {
            saveDialogButton.getStyleClass().add("primary-button");
        }
        if (cancelDialogButton != null) {
            cancelDialogButton.getStyleClass().add("secondary-button");
        }

        dialog.setResultConverter(buttonType -> buttonType == saveButtonType ? Boolean.TRUE : null);

        Optional<Boolean> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }

        try {
            AppSettings.saveLocal("OLLAMA_BASE_URL", safeValue(baseUrlField.getText(), localAiContentService.getBaseUrl()));
            AppSettings.saveLocal("OLLAMA_TEXT_MODEL", safeValue(modelField.getText(), localAiContentService.getTextModel()));
            refreshAiState();
            updateAiStatus("Local AI setup saved. Text tools are ready, and cover images use the free provider.", "ai-status-success");
        } catch (RuntimeException exception) {
            updateAiStatus(exception.getMessage(), "ai-status-error");
            SweetAlert.error("AI Assistant", exception.getMessage());
        }
    }

    private boolean hasMinimumCourseContext() {
        return !titleField.getText().isBlank() && !subjectField.getText().isBlank();
    }

    private boolean hasAnyCourseContent() {
        return !titleField.getText().isBlank()
                || !subjectField.getText().isBlank()
                || !descriptionArea.getText().isBlank();
    }

    private String mergeDescriptionWithObjectives(String description, String objectives) {
        String cleanDescription = description == null ? "" : description.trim();
        String cleanObjectives = objectives == null ? "" : objectives.trim();
        if (cleanDescription.isBlank()) {
            return cleanObjectives;
        }
        if (cleanObjectives.isBlank()) {
            return cleanDescription;
        }
        return cleanDescription + System.lineSeparator() + System.lineSeparator() + cleanObjectives;
    }

    private String fitToLength(String value, int maxLength) {
        String cleanValue = safeValue(value, "");
        return cleanValue.length() <= maxLength ? cleanValue : cleanValue.substring(0, maxLength).trim();
    }

    private void refreshPreviewImage() {
        String previewImageSource = resolvePreviewImageSource(courseImagePath);
        if (previewImageSource == null) {
            previewImageView.setImage(null);
            previewImageView.setVisible(false);
            previewImageView.setManaged(false);
            previewImageStatusLabel.setText("Generate a cover image to preview it here.");
            previewImageStatusLabel.setVisible(true);
            previewImageStatusLabel.setManaged(true);
            return;
        }

        Image previewImage = new Image(previewImageSource, false);
        if (previewImage.isError() || previewImage.getWidth() <= 0 || previewImage.getHeight() <= 0) {
            previewImageView.setImage(null);
            previewImageView.setVisible(false);
            previewImageView.setManaged(false);
            previewImageStatusLabel.setText("The generated image could not be previewed. Try generating it again.");
            previewImageStatusLabel.setVisible(true);
            previewImageStatusLabel.setManaged(true);
            return;
        }

        previewImageView.setImage(previewImage);
        previewImageView.setVisible(true);
        previewImageView.setManaged(true);
        previewImageStatusLabel.setText("AI cover image attached.");
        previewImageStatusLabel.setVisible(true);
        previewImageStatusLabel.setManaged(true);
    }

    private String resolvePreviewImageSource(String value) {
        if (value == null || value.isBlank() || "course-default.png".equalsIgnoreCase(value.trim())) {
            return null;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.startsWith("http://") || trimmedValue.startsWith("https://") || trimmedValue.startsWith("file:/")) {
            return trimmedValue;
        }

        Path localPath = Path.of(trimmedValue);
        if (Files.exists(localPath)) {
            return localPath.toUri().toString();
        }

        URL resource = getClass().getResource("/tn/esprit/images/" + trimmedValue);
        return resource != null ? resource.toExternalForm() : null;
    }
}
