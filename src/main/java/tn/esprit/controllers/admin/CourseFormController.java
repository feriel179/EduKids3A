package tn.esprit.controllers.admin;

import javafx.concurrent.Task;
import tn.esprit.models.Course;
import tn.esprit.services.CourseService;
import tn.esprit.services.OpenAiContentService;
import tn.esprit.util.FormValidator;
import tn.esprit.util.SweetAlert;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private ImageView previewImageView;

    private final CourseService courseService = new CourseService();
    private final OpenAiContentService openAiContentService = new OpenAiContentService();
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

        titleField.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        subjectField.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        levelComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        statusComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        descriptionArea.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());

        updateAiStatus(
                openAiContentService.hasApiKey()
                        ? "AI assistant ready. Generate copy, translate content, or create a cover image."
                        : "Add OPENAI_API_KEY to enable the AI tools on this page.",
                openAiContentService.hasApiKey() ? "ai-status-idle" : "ai-status-error"
        );
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
        updateAiStatus(
                openAiContentService.hasApiKey()
                        ? "AI assistant ready. Generate copy, translate content, or create a cover image."
                        : "Add OPENAI_API_KEY to enable the AI tools on this page.",
                openAiContentService.hasApiKey() ? "ai-status-idle" : "ai-status-error"
        );
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
                () -> openAiContentService.generateCourseDescription(
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
                () -> openAiContentService.suggestObjectives(
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
                () -> openAiContentService.translateCourse(
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
                () -> openAiContentService.generateCourseImage(
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
            SweetAlert.error("AI Assistant", message);
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

    private void updateAiStatus(String message, String variantStyle) {
        aiStatusLabel.setText(safeValue(message, "AI assistant ready."));
        aiStatusLabel.getStyleClass().removeAll("ai-status-idle", "ai-status-running", "ai-status-success", "ai-status-error");
        if (!aiStatusLabel.getStyleClass().contains("ai-status-label")) {
            aiStatusLabel.getStyleClass().add("ai-status-label");
        }
        aiStatusLabel.getStyleClass().add(variantStyle);
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
            previewImageStatusLabel.setText("No cover image generated yet.");
            previewImageStatusLabel.setVisible(true);
            previewImageStatusLabel.setManaged(true);
            return;
        }

        previewImageView.setImage(new Image(previewImageSource, true));
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
