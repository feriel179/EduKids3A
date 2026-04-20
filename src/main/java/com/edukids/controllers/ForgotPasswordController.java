package com.edukids.controllers;

import com.edukids.services.UserService;
import com.edukids.utils.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.Random;
import java.util.ResourceBundle;

public class ForgotPasswordController implements Initializable {

    @FXML private TextField emailField;
    @FXML private TextField otpField;
    @FXML private Button sendCodeBtn;
    @FXML private Button verifyBtn;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;
    @FXML private VBox rootPane;

    private final UserService userService = new UserService();
    private String generatedOtp;
    private String verifiedEmail;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);
        successLabel.setVisible(false);
        otpField.setVisible(false);
        otpField.setManaged(false);
        verifyBtn.setVisible(false);
        verifyBtn.setManaged(false);
    }

    @FXML
    private void handleSendCode() {
        String email = emailField.getText().trim();

        if (email.isEmpty()) {
            showError("Please enter your email.");
            return;
        }

        if (!userService.emailExists(email)) {
            showError("No account found with this email.");
            return;
        }

        // Generate OTP
        generatedOtp = String.valueOf(100000 + new Random().nextInt(900000));
        verifiedEmail = email;

        // TODO: Send email via SendGrid
        // For now, show OTP in console for development
        System.out.println("OTP for " + email + ": " + generatedOtp);

        showSuccess("Verification code sent to your email.");
        otpField.setVisible(true);
        otpField.setManaged(true);
        verifyBtn.setVisible(true);
        verifyBtn.setManaged(true);
    }

    @FXML
    private void handleVerifyCode() {
        String enteredOtp = otpField.getText().trim();

        if (enteredOtp.isEmpty()) {
            showError("Please enter the verification code.");
            return;
        }

        if (!enteredOtp.equals(generatedOtp)) {
            showError("Invalid verification code.");
            return;
        }

        // OTP verified, navigate to reset password
        ResetPasswordController controller = Navigator.navigateToAndGetController(
                "reset-password.fxml", Navigator.getStageFromNode(rootPane));
        controller.setEmail(verifiedEmail);
    }

    @FXML
    private void handleBackToLogin() {
        Navigator.navigateTo("login.fxml", Navigator.getStageFromNode(rootPane));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        successLabel.setVisible(false);
    }

    private void showSuccess(String message) {
        successLabel.setText(message);
        successLabel.setVisible(true);
        errorLabel.setVisible(false);
    }
}
