package com.edukids.controllers;

import com.edukids.entities.User;
import com.edukids.enums.Role;
import com.edukids.services.GoogleAuthService;
import com.edukids.services.UserService;
import com.edukids.utils.Navigator;
import com.edukids.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.esprit.MainFX;

import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.UUID;

public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Label errorLabel;
    @FXML private VBox rootPane;
    @FXML private Label captchaQuestionLabel;
    @FXML private TextField captchaAnswerField;
    @FXML private Button googleSignInButton;

    private final UserService userService = new UserService();
    private final GoogleAuthService googleAuthService = new GoogleAuthService();
    private final Random random = new Random();
    private int captchaAnswer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);
        generateCaptcha();
        configureGoogleSignIn();
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        if (!isCaptchaValid()) {
            return;
        }

        // Check if user exists first to give proper error
        try {
            User found = userService.getByEmail(email);
            if (found != null && !found.isActive()) {
                showError("Your account has been banned. Please contact administrator for support.");
                return;
            }

            User user = userService.authenticate(email, password);
            if (user == null) {
                showError("Invalid email or password.");
                generateCaptcha();
                return;
            }

            redirectUser(user);
        } catch (RuntimeException exception) {
            showError("Login failed: " + exception.getMessage());
        }
    }

    @FXML
    public void generateCaptcha() {
        int first = random.nextInt(10) + 1;
        int second = random.nextInt(10) + 1;
        captchaAnswer = first + second;

        if (captchaQuestionLabel != null) {
            captchaQuestionLabel.setText(first + " + " + second + " = ?");
        }
        if (captchaAnswerField != null) {
            captchaAnswerField.clear();
        }
    }

    @FXML
    private void handleGoogleSignIn() {
        if (!isGoogleSignInConfigured()) {
            showError("Google sign-in is not configured yet.");
            return;
        }

        new Thread(() -> {
            try {
                GoogleAuthService.GoogleUserInfo googleUser = googleAuthService.authorizeAndFetchUser();
                User user = userService.getByEmail(googleUser.email());

                if (user == null) {
                    user = new User();
                    user.setEmail(googleUser.email());
                    user.setFirstName(googleUser.givenName() == null ? "" : googleUser.givenName());
                    user.setLastName(googleUser.familyName() == null ? "" : googleUser.familyName());
                    user.setPassword(UUID.randomUUID().toString());
                    user.setRoles(List.of(Role.ROLE_ELEVE));
                    user.setVerified(true);
                    user.setActive(true);
                    userService.addOAuthUser(user);
                } else if (!user.isActive()) {
                    Platform.runLater(() -> showError("Your account has been banned. Please contact administrator for support."));
                    return;
                }

                User finalUser = user;
                Platform.runLater(() -> redirectUser(finalUser));
            } catch (Exception exception) {
                Platform.runLater(() -> showError("Google sign-in failed: " + exception.getMessage()));
            }
        }, "google-sign-in").start();
    }

    private boolean isCaptchaValid() {
        if (captchaAnswerField == null) {
            return true;
        }

        String answer = captchaAnswerField.getText().trim();
        if (answer.isEmpty()) {
            showError("Please answer the CAPTCHA calculation.");
            return false;
        }

        try {
            if (Integer.parseInt(answer) != captchaAnswer) {
                showError("Incorrect CAPTCHA answer. Please try again.");
                generateCaptcha();
                return false;
            }
            return true;
        } catch (NumberFormatException exception) {
            showError("The CAPTCHA answer must be a number.");
            generateCaptcha();
            return false;
        }
    }

    private void configureGoogleSignIn() {
        if (googleSignInButton != null) {
            googleSignInButton.setVisible(true);
            googleSignInButton.setManaged(true);
            googleSignInButton.setDisable(false);
        }
    }

    private boolean isGoogleSignInConfigured() {
        return googleAuthService.isConfigured();
    }

    private void redirectUser(User user) {
        SessionManager.setCurrentUser(user);

        try {
            if (user.getPrimaryRole() == Role.ROLE_ADMIN) {
                MainFX.getInstance().showUserDashboard();
            } else if (user.getPrimaryRole() == Role.ROLE_ELEVE) {
                MainFX.getInstance().showStudentShellForUser(user);
            } else {
                MainFX.getInstance().showParentHome();
            }
        } catch (Exception exception) {
            showError("Unable to open the requested screen.");
            exception.printStackTrace();
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
