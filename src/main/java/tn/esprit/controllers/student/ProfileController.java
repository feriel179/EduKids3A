package tn.esprit.controllers.student;

import tn.esprit.models.Student;
import tn.esprit.services.StudentService;
import tn.esprit.util.SweetAlert;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public class ProfileController {
    @FXML
    private Label nameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label coursesCountLabel;
    @FXML
    private ComboBox<Integer> ageComboBox;
    @FXML
    private ComboBox<String> preferredCategoryComboBox;
    @FXML
    private Label ageSummaryLabel;
    @FXML
    private Label categorySummaryLabel;
    @FXML
    private Button saveProfileButton;

    private final StudentService studentService = new StudentService();

    @FXML
    private void initialize() {
        ageComboBox.getItems().setAll(8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18);
        preferredCategoryComboBox.getItems().setAll(
                "Mathematique",
                "Francais",
                "Anglais",
                "Sciences",
                "Informatique",
                "Arabe",
                "Histoire",
                "Geographie",
                "General"
        );

        Student student = studentService.getCurrentStudent();
        if (student != null) {
            populateProfile(student);
        }
    }

    @FXML
    private void handleSaveProfile() {
        Student updatedStudent = studentService.updateCurrentStudentProfile(ageComboBox.getValue(), preferredCategoryComboBox.getValue());
        populateProfile(updatedStudent);
        if (StudentShellController.getInstance() != null) {
            StudentShellController.getInstance().refreshStudentHeader();
        }
        SweetAlert.success("Profile Updated", "Your personalized learning profile is ready.");
    }

    private void populateProfile(Student student) {
        nameLabel.setText(student.getName());
        emailLabel.setText(student.getEmail());
        coursesCountLabel.setText(String.valueOf(student.getEnrolledCourses().size()));
        ageComboBox.setValue(student.getAge() >= 8 ? student.getAge() : 8);
        preferredCategoryComboBox.setValue(student.getPreferredCategory());
        ageSummaryLabel.setText(student.getAgeGroupLabel());
        categorySummaryLabel.setText(student.getPreferredCategory());
    }
}
