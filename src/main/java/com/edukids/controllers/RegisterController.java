package com.edukids.controllers;

import com.edukids.entities.User;
import com.edukids.enums.Role;
import com.edukids.services.UserService;
import com.edukids.utils.Navigator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.security.SecureRandom;
import java.util.List;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label errorLabel;
    @FXML private Label suggestedPasswordLabel;
    @FXML private HBox suggestBox;
    @FXML private CheckBox termsCheckBox;
    @FXML private VBox rootPane;
    @FXML private Button togglePasswordBtn;

    private final UserService userService = new UserService();
    private boolean passwordVisible = false;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);
        suggestBox.setVisible(false);
        suggestBox.setManaged(false);

        passwordVisibleField.setVisible(false);
        passwordVisibleField.setManaged(false);

        // Bind password fields
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());

        roleComboBox.getItems().addAll("Admin", "Student (Eleve)", "Parent");
        roleComboBox.setValue("Student (Eleve)");
    }

    @FXML
    private void handleRegister() {
        errorLabel.setVisible(false);

        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()
                || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("All fields are required.");
            return;
        }

        if (firstName.length() < 2 || firstName.length() > 255 || !firstName.matches("[a-zA-Z\\s]+")) {
            showError("First name: 2-255 characters, letters only.");
            return;
        }

        if (lastName.length() < 2 || lastName.length() > 255 || !lastName.matches("[a-zA-Z\\s]+")) {
            showError("Last name: 2-255 characters, letters only.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$") || email.length() > 180) {
            showError("Please enter a valid email address (max 180 chars).");
            return;
        }

        if (password.length() < 8) {
            showError("Password must be at least 8 characters.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        if (!termsCheckBox.isSelected()) {
            showError("You must accept the terms of service.");
            return;
        }

        if (userService.emailExists(email)) {
            showError("This email is already registered.");
            return;
        }

        String selectedRoleValue = roleComboBox.getValue();
        Role selectedRole = switch (selectedRoleValue) {
            case "Admin" -> Role.ROLE_ADMIN;
            case "Parent" -> Role.ROLE_PARENT;
            default -> Role.ROLE_ELEVE;
        };
        User user = new User(email, password, firstName, lastName, List.of(selectedRole));
        user.setActive(true);
        user.setVerified(false);

        userService.add(user);

        Alert alert = new Alert(Alert.AlertType.INFORMATION,
                "Account created successfully! Please login.", ButtonType.OK);
        alert.setHeaderText("Registration Successful");
        alert.showAndWait();

        Navigator.navigateTo("login.fxml", Navigator.getStageFromNode(rootPane));
    }

    @FXML
    private void handleSuggestPassword() {
        String suggested = generateSecurePassword(14);
        suggestedPasswordLabel.setText(suggested);
        suggestBox.setVisible(true);
        suggestBox.setManaged(true);
    }

    @FXML
    private void handleUseSuggestedPassword() {
        String suggested = suggestedPasswordLabel.getText();
        passwordField.setText(suggested);
        confirmPasswordField.setText(suggested);
        suggestBox.setVisible(false);
        suggestBox.setManaged(false);
    }

    @FXML
    private void handleTogglePassword() {
        passwordVisible = !passwordVisible;
        if (passwordVisible) {
            passwordVisibleField.setVisible(true);
            passwordVisibleField.setManaged(true);
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            togglePasswordBtn.setText("Hide");
        } else {
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            passwordVisibleField.setVisible(false);
            passwordVisibleField.setManaged(false);
            togglePasswordBtn.setText("Show");
        }
    }

    @FXML
    private void handleGoToLogin() {
        Navigator.navigateTo("login.fxml", Navigator.getStageFromNode(rootPane));
    }

    private String generateSecurePassword(int length) {
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%&*";
        String all = upper + lower + digits + special;

        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        sb.append(upper.charAt(random.nextInt(upper.length())));
        sb.append(lower.charAt(random.nextInt(lower.length())));
        sb.append(digits.charAt(random.nextInt(digits.length())));
        sb.append(special.charAt(random.nextInt(special.length())));
        for (int i = 4; i < length; i++) {
            sb.append(all.charAt(random.nextInt(all.length())));
        }
        // Shuffle
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
