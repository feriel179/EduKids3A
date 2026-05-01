package tn.esprit.controllers.admin;

import tn.esprit.models.Course;
import tn.esprit.models.Lesson;
import tn.esprit.services.CourseService;
import tn.esprit.services.LessonService;
import tn.esprit.util.FormValidator;
import tn.esprit.util.SweetAlert;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;

public class LessonFormController {
    @FXML
    private Label badgeLabel;
    @FXML
    private Label pageTitleLabel;
    @FXML
    private Label pageSubtitleLabel;
    @FXML
    private Label previewLessonTitleLabel;
    @FXML
    private Label previewCourseLabel;
    @FXML
    private Label previewOrderLabel;
    @FXML
    private Label previewMediaLabel;
    @FXML
    private Label previewStatusLabel;
    @FXML
    private Label previewDurationLabel;
    @FXML
    private Label previewUrlLabel;
    @FXML
    private ComboBox<Course> courseComboBox;
    @FXML
    private TextField lessonTitleField;
    @FXML
    private TextField orderField;
    @FXML
    private ComboBox<String> statusComboBox;
    @FXML
    private TextField durationField;
    @FXML
    private TextField pdfUrlField;
    @FXML
    private TextField videoUrlField;
    @FXML
    private TextField youtubeUrlField;
    @FXML
    private Button saveButton;

    private final CourseService courseService = new CourseService();
    private final LessonService lessonService = new LessonService();

    private Lesson selectedLesson;
    private Course defaultCourse;
    private boolean editMode;

    @FXML
    private void initialize() {
        lessonTitleField.setTextFormatter(FormValidator.createLengthFormatter(120));
        orderField.setTextFormatter(FormValidator.createDigitsFormatter(3));
        durationField.setTextFormatter(FormValidator.createDigitsFormatter(4));
        pdfUrlField.setTextFormatter(FormValidator.createLengthFormatter(255));
        videoUrlField.setTextFormatter(FormValidator.createLengthFormatter(255));
        youtubeUrlField.setTextFormatter(FormValidator.createLengthFormatter(255));

        loadCoursesSafely();
        statusComboBox.getItems().setAll("DRAFT", "PUBLISHED", "HIDDEN");
        statusComboBox.setValue("DRAFT");

        courseComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!editMode && newValue != null) {
                orderField.setText(String.valueOf(lessonService.getNextOrderForCourse(newValue)));
            }
            refreshPreview();
        });
        lessonTitleField.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        orderField.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        statusComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        durationField.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        pdfUrlField.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        videoUrlField.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        youtubeUrlField.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());

        refreshPreview();
    }

    public void initForCreate(Course preselectedCourse) {
        editMode = false;
        selectedLesson = null;
        defaultCourse = preselectedCourse;

        badgeLabel.setText("Create Lesson");
        pageTitleLabel.setText("Build a lesson on its own dedicated page");
        pageSubtitleLabel.setText("Use the same editor style to prepare the lesson, choose its course, and save it to EduKids.");
        saveButton.setText("Submit Lesson");

        handleResetForm();
    }

    public void initForEdit(Lesson lesson) {
        editMode = true;
        selectedLesson = lesson;
        defaultCourse = lesson == null ? null : lesson.getCourse();

        badgeLabel.setText("Update Lesson");
        pageTitleLabel.setText("Refine an existing lesson page");
        pageSubtitleLabel.setText("Edit the selected lesson in its own workspace, keep the same visual style, then confirm the update with a quick popup.");
        saveButton.setText("Save Changes");

        loadCoursesSafely();
        if (lesson != null) {
            courseComboBox.setValue(findMatchingCourse(lesson.getCourse()));
            lessonTitleField.setText(lesson.getTitle());
            orderField.setText(String.valueOf(lesson.getOrder()));
            statusComboBox.setValue(lesson.getStatus());
            durationField.setText(String.valueOf(lesson.getDurationMinutes()));
            pdfUrlField.setText(safeValue(lesson.getPdfUrl(), ""));
            videoUrlField.setText(safeValue(lesson.getVideoUrl(), ""));
            youtubeUrlField.setText(safeValue(lesson.getYoutubeUrl(), ""));
        }

        refreshPreview();
    }

    @FXML
    private void handleSaveLesson() {
        if (!isValidForm()) {
            return;
        }

        Course course = courseComboBox.getValue();
        String title = lessonTitleField.getText().trim();
        int order = Integer.parseInt(orderField.getText().trim());
        int durationMinutes = Integer.parseInt(durationField.getText().trim());
        String status = safeValue(statusComboBox.getValue(), "DRAFT");
        String pdfUrl = safeValue(pdfUrlField.getText(), "");
        String videoUrl = safeValue(videoUrlField.getText(), "");
        String youtubeUrl = safeValue(youtubeUrlField.getText(), "");

        try {
            Lesson lesson;
            if (editMode && selectedLesson != null) {
                lessonService.updateLesson(selectedLesson, course, order, title, pdfUrl, videoUrl, youtubeUrl, status, durationMinutes);
                lesson = selectedLesson;
            } else {
                lesson = lessonService.addLesson(course, order, title, pdfUrl, videoUrl, youtubeUrl, status, durationMinutes);
            }

            String actionLabel = editMode ? "Lesson updated" : "Lesson added";
            SweetAlert.success(actionLabel, "The lesson \"" + lesson.getTitle() + "\" was saved successfully.");
            AdminModuleNavigator.showLessons();
        } catch (RuntimeException exception) {
            SweetAlert.error("Database Error", exception.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        AdminModuleNavigator.showLessons();
    }

    @FXML
    private void handleResetForm() {
        if (editMode && selectedLesson != null) {
            courseComboBox.setValue(findMatchingCourse(selectedLesson.getCourse()));
            lessonTitleField.setText(selectedLesson.getTitle());
            orderField.setText(String.valueOf(selectedLesson.getOrder()));
            statusComboBox.setValue(selectedLesson.getStatus());
            durationField.setText(String.valueOf(selectedLesson.getDurationMinutes()));
            pdfUrlField.setText(safeValue(selectedLesson.getPdfUrl(), ""));
            videoUrlField.setText(safeValue(selectedLesson.getVideoUrl(), ""));
            youtubeUrlField.setText(safeValue(selectedLesson.getYoutubeUrl(), ""));
        } else {
            loadCourses();
            courseComboBox.setValue(findMatchingCourse(defaultCourse));
            if (courseComboBox.getValue() == null && !courseComboBox.getItems().isEmpty()) {
                courseComboBox.getSelectionModel().selectFirst();
            }

            lessonTitleField.clear();
            statusComboBox.setValue("DRAFT");
            durationField.setText("10");
            pdfUrlField.clear();
            videoUrlField.clear();
            youtubeUrlField.clear();
            updateSuggestedOrder();
        }

        refreshPreview();
    }

    @FXML
    private void handleChoosePdfFile() {
        chooseLocalFile(
                pdfUrlField,
                "Choose PDF File",
                new FileChooser.ExtensionFilter("PDF Files", "*.pdf")
        );
    }

    @FXML
    private void handleChooseVideoFile() {
        chooseLocalFile(
                videoUrlField,
                "Choose Video File",
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.mkv", "*.avi", "*.mov", "*.wmv", "*.webm")
        );
    }

    private void loadCourses() {
        courseService.refreshCourses();
        courseComboBox.setItems(courseService.getAllCourses());
    }

    private void loadCoursesSafely() {
        try {
            loadCourses();
        } catch (RuntimeException exception) {
            courseComboBox.getItems().clear();
            courseComboBox.setPromptText("Courses unavailable");
        }
    }

    private Course findMatchingCourse(Course course) {
        if (course == null) {
            return null;
        }
        return courseComboBox.getItems().stream()
                .filter(item -> item.getId() == course.getId())
                .findFirst()
                .orElse(null);
    }

    private void refreshPreview() {
        String lessonTitle = safeValue(lessonTitleField.getText(), "Your lesson title will appear here");
        String selectedCourse = courseComboBox.getValue() == null ? "Select a course" : courseComboBox.getValue().getTitle();
        String lessonOrder = safeValue(orderField.getText(), "1");
        String mediaType = buildMediaLabel(pdfUrlField.getText(), videoUrlField.getText(), youtubeUrlField.getText());
        String status = safeValue(statusComboBox.getValue(), "DRAFT");
        String duration = safeValue(durationField.getText(), "10");
        String activeUrl = buildUrlSummary(pdfUrlField.getText(), videoUrlField.getText(), youtubeUrlField.getText());

        previewLessonTitleLabel.setText(lessonTitle);
        previewCourseLabel.setText(selectedCourse);
        previewOrderLabel.setText("Lesson " + lessonOrder);
        previewMediaLabel.setText(mediaType);
        previewStatusLabel.setText(toDisplayStatus(status));
        previewDurationLabel.setText(duration + " min");
        previewUrlLabel.setText(activeUrl.isBlank() ? "Add one or more lesson links here." : activeUrl);
    }

    private boolean isValidForm() {
        if (courseComboBox.getValue() == null) {
            SweetAlert.warning("Validation", "Please choose a course.");
            return false;
        }

        String validationMessage = FormValidator.validateLesson(
                lessonTitleField.getText(),
                orderField.getText(),
                durationField.getText(),
                pdfUrlField.getText(),
                videoUrlField.getText(),
                youtubeUrlField.getText()
        );
        if (validationMessage != null) {
            SweetAlert.warning("Validation", validationMessage);
            return false;
        }
        return true;
    }

    private String buildMediaLabel(String pdfUrl, String videoUrl, String youtubeUrl) {
        StringBuilder builder = new StringBuilder();
        appendMedia(builder, safeValue(pdfUrl, ""), "PDF");
        appendMedia(builder, safeValue(videoUrl, ""), "VIDEO");
        appendMedia(builder, safeValue(youtubeUrl, ""), "YOUTUBE");
        return builder.isEmpty() ? "MEDIA" : builder.toString();
    }

    private String buildUrlSummary(String pdfUrl, String videoUrl, String youtubeUrl) {
        StringBuilder builder = new StringBuilder();
        appendUrl(builder, safeValue(pdfUrl, ""), "PDF");
        appendUrl(builder, safeValue(videoUrl, ""), "VIDEO");
        appendUrl(builder, safeValue(youtubeUrl, ""), "YOUTUBE");
        return builder.toString();
    }

    private void appendMedia(StringBuilder builder, String value, String label) {
        if (value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(" + ");
        }
        builder.append(label);
    }

    private void appendUrl(StringBuilder builder, String value, String label) {
        if (value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append("\n");
        }
        builder.append(label).append(": ").append(value);
    }

    private void chooseLocalFile(TextField targetField, String dialogTitle, FileChooser.ExtensionFilter extensionFilter) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(dialogTitle);
        chooser.getExtensionFilters().add(extensionFilter);

        Window window = targetField.getScene() == null ? null : targetField.getScene().getWindow();
        File selectedFile = chooser.showOpenDialog(window);
        if (selectedFile != null) {
            targetField.setText(selectedFile.toURI().toString());
            refreshPreview();
        }
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private void updateSuggestedOrder() {
        Course course = courseComboBox.getValue();
        if (course == null) {
            orderField.setText("1");
            return;
        }
        orderField.setText(String.valueOf(lessonService.getNextOrderForCourse(course)));
    }

    private String toDisplayStatus(String value) {
        return switch (safeValue(value, "DRAFT")) {
            case "PUBLISHED" -> "Published";
            case "HIDDEN" -> "Hidden";
            default -> "Draft";
        };
    }
}
