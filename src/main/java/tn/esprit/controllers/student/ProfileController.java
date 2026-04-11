package tn.esprit.controllers.student;

import tn.esprit.models.Student;
import tn.esprit.services.StudentService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ProfileController {
    @FXML
    private Label nameLabel;
    @FXML
    private Label emailLabel;
    @FXML
    private Label coursesCountLabel;

    private final StudentService studentService = new StudentService();

    @FXML
    private void initialize() {
        Student student = studentService.getCurrentStudent();
        if (student != null) {
            nameLabel.setText(student.getName());
            emailLabel.setText(student.getEmail());
            coursesCountLabel.setText(String.valueOf(student.getEnrolledCourses().size()));
        }
    }
}
