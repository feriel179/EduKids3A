package com.edukids.controllers;

import com.edukids.services.UserService;
import com.edukids.utils.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class ResetPasswordController implements Initializable {

    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private VBox rootPane;

    private final UserService userService = new UserService();
    private String email;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @FXML
    private void handleResetPassword() {
        String newPwd = newPasswordField.getText();
        String confirmPwd = confirmPasswordField.getText();

        if (newPwd.isEmpty() || confirmPwd.isEmpty()) {
            showError("All fields are required.");
            return;
        }

        if (newPwd.length() < 8 || !newPwd.matches(".*\\d.*") || !newPwd.matches(".*[A-Z].*")) {
            showError("Password: min 8 chars, 1 digit, 1 uppercase letter.");
            return;
        }

        if (!newPwd.equals(confirmPwd)) {
            showError("Passwords do not match.");
            return;
        }

        userService.updatePassword(email, newPwd);

        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Password reset successfully! Please login with your new password.", ButtonType.OK);
        alert.setHeaderText("Password Reset");
        alert.showAndWait();

        Navigator.navigateTo("login.fxml", Navigator.getStageFromNode(rootPane));
    }

    @FXML
    private void handleBackToLogin() {
        Navigator.navigateTo("login.fxml", Navigator.getStageFromNode(rootPane));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
