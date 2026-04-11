package tn.esprit.controllers;

import tn.esprit.MainFX;
import tn.esprit.models.Student;
import tn.esprit.services.StudentService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;

public class LoginController {
    @FXML
    private ComboBox<String> roleComboBox;
    @FXML
    private TextField studentNameField;
    @FXML
    private Label studentNameLabel;

    private final StudentService studentService = new StudentService();

    @FXML
    private void initialize() {
        roleComboBox.getItems().addAll("Admin", "Student");
        roleComboBox.setValue("Student");
        toggleStudentFields();
        roleComboBox.valueProperty().addListener((obs, oldValue, newValue) -> toggleStudentFields());
    }

    @FXML
    private void handleLogin() {
        String role = roleComboBox.getValue();
        try {
            if ("Admin".equals(role)) {
                MainFX.getInstance().showAdminShell();
                return;
            }

            String identifier = studentNameField.getText();
            if (identifier == null || identifier.isBlank()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Please enter a student name or email.");
                return;
            }

            Student student = studentService.loginOrCreateStudent(identifier);
            MainFX.getInstance().showStudentShell(student);
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error", exception.getMessage());
        } catch (RuntimeException exception) {
            showAlert(Alert.AlertType.ERROR, "Database Error", exception.getMessage());
        }
    }

    private void toggleStudentFields() {
        boolean studentMode = "Student".equals(roleComboBox.getValue());
        studentNameField.setVisible(studentMode);
        studentNameField.setManaged(studentMode);
        studentNameLabel.setVisible(studentMode);
        studentNameLabel.setManaged(studentMode);
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
