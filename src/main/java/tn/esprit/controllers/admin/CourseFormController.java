package tn.esprit.controllers.admin;

import tn.esprit.models.Course;
import tn.esprit.services.CourseService;
import tn.esprit.util.FormValidator;
import tn.esprit.util.SweetAlert;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.stream.IntStream;

public class CourseFormController {
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
    private Label previewDescriptionLabel;
    @FXML
    private TextField titleField;
    @FXML
    private TextField subjectField;
    @FXML
    private ComboBox<String> levelComboBox;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private Button saveButton;

    private final CourseService courseService = new CourseService();
    private Course selectedCourse;
    private boolean editMode;

    @FXML
    private void initialize() {
        titleField.setTextFormatter(FormValidator.createLengthFormatter(120));
        subjectField.setTextFormatter(FormValidator.createLengthFormatter(80));
        descriptionArea.setTextFormatter(FormValidator.createLengthFormatter(1000));

        levelComboBox.getItems().setAll(IntStream.rangeClosed(1, 9).mapToObj(level -> "Niveau " + level).toList());
        levelComboBox.setValue("Niveau 1");

        titleField.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        subjectField.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        levelComboBox.valueProperty().addListener((obs, oldValue, newValue) -> refreshPreview());
        descriptionArea.textProperty().addListener((obs, oldValue, newValue) -> refreshPreview());

        refreshPreview();
    }

    public void initForCreate() {
        editMode = false;
        selectedCourse = null;
        badgeLabel.setText("Create Course");
        pageTitleLabel.setText("Launch a new EduKids course");
        pageSubtitleLabel.setText("Use a dedicated page to prepare the course details before publishing them to the EduKids database.");
        saveButton.setText("Submit Course");
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
        descriptionArea.setText(course.getDescription());
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

        try {
            Course course;
            if (editMode && selectedCourse != null) {
                courseService.updateCourse(selectedCourse, title, description, level, subject);
                course = selectedCourse;
            } else {
                course = courseService.addCourse(title, description, level, subject);
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
            descriptionArea.setText(selectedCourse.getDescription());
        } else {
            titleField.clear();
            subjectField.clear();
            levelComboBox.setValue("Niveau 1");
            descriptionArea.clear();
        }
        refreshPreview();
    }

    private void refreshPreview() {
        String title = safeValue(titleField.getText(), "Your course title will appear here");
        String subject = safeValue(subjectField.getText(), "Select a subject");
        String level = safeValue(levelComboBox.getValue(), "Niveau 1");
        String description = safeValue(descriptionArea.getText(), "A short and engaging summary will help parents and students understand the value of the course.");

        previewTitleLabel.setText(title);
        previewSubjectLabel.setText(subject);
        previewLevelLabel.setText(level);
        previewDescriptionLabel.setText(description);
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
}
