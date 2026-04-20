package com.edukids.controllers;

import com.edukids.entities.User;
import com.edukids.enums.Role;
import com.edukids.services.UserService;
import com.edukids.utils.Navigator;
import com.edukids.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Label errorLabel;
    @FXML private VBox rootPane;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        // Check if user exists first to give proper error
        User found = userService.getByEmail(email);
        if (found != null && !found.isActive()) {
            showError("Your account has been banned. Please contact administrator for support.");
            return;
        }

        User user = userService.authenticate(email, password);
        if (user == null) {
            showError("Invalid email or password.");
            return;
        }

        SessionManager.setCurrentUser(user);

        if (user.getPrimaryRole() == Role.ROLE_ADMIN) {
            Navigator.navigateTo("dashboard.fxml", Navigator.getStageFromNode(rootPane));
        } else {
            Navigator.navigateTo("user-home.fxml", Navigator.getStageFromNode(rootPane));
        }
    }

    @FXML
    private void handleGoToRegister() {
        Navigator.navigateTo("register.fxml", Navigator.getStageFromNode(rootPane));
    }

    @FXML
    private void handleForgotPassword() {
        Navigator.navigateTo("forgot-password.fxml", Navigator.getStageFromNode(rootPane));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
