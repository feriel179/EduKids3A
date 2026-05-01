package com.edukids.controllers;

import com.edukids.entities.User;
import com.edukids.services.UserService;
import com.edukids.utils.Navigator;
import com.edukids.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;
import java.util.UUID;

public class ProfileController implements Initializable {

    // Profile edit
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;
    @FXML private VBox rootPane;
    @FXML private ImageView avatarImageView;
    @FXML private Label avatarInitials;

    // Password change
    @FXML private PasswordField currentPasswordField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmNewPasswordField;
    @FXML private Label passwordErrorLabel;

    // Settings section
    @FXML private Label memberSinceLabel;

    private final UserService userService = new UserService();
    private static final String AVATARS_DIR = "uploads/avatars/";
    private File selectedAvatarFile = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);
        passwordErrorLabel.setVisible(false);

        // Ensure avatars directory exists
        new File(AVATARS_DIR).mkdirs();

        User user = SessionManager.getCurrentUser();
        if (user != null) {
            firstNameField.setText(user.getFirstName());
            lastNameField.setText(user.getLastName());
            emailField.setText(user.getEmail());
            roleLabel.setText(user.getPrimaryRole().getDisplayName());

            String roleClass = switch (user.getPrimaryRole()) {
                case ROLE_ADMIN -> "badge-admin";
                case ROLE_PARENT -> "badge-parent";
                case ROLE_ELEVE -> "badge-eleve";
            };
            roleLabel.getStyleClass().addAll("badge", roleClass);

            statusLabel.setText(user.isVerified() ? "Verified" : "Not Verified");
            statusLabel.getStyleClass().addAll("badge", user.isVerified() ? "badge-verified" : "badge-unverified");

            loadAvatar(user);
        }
    }

    private void loadAvatar(User user) {
        if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
            File avatarFile = new File(AVATARS_DIR + user.getAvatar());
            if (avatarFile.exists()) {
                avatarImageView.setImage(new Image(avatarFile.toURI().toString(), 100, 100, true, true));
                avatarImageView.setVisible(true);
                avatarInitials.setVisible(false);
                return;
            }
        }
        // Fallback to initials
        avatarImageView.setVisible(false);
        avatarInitials.setVisible(true);
        String initials = "";
        if (user.getFirstName() != null && !user.getFirstName().isEmpty())
            initials += user.getFirstName().substring(0, 1).toUpperCase();
        if (user.getLastName() != null && !user.getLastName().isEmpty())
            initials += user.getLastName().substring(0, 1).toUpperCase();
        avatarInitials.setText(initials.isEmpty() ? "?" : initials);
    }

    @FXML
    private void handleChooseAvatar() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Avatar Image");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp")
        );
        File file = fileChooser.showOpenDialog(rootPane.getScene().getWindow());
        if (file != null) {
            // Check size (max 2MB)
            if (file.length() > 2 * 1024 * 1024) {
                showError(errorLabel, "Image must be less than 2MB.");
                return;
            }
            selectedAvatarFile = file;
            avatarImageView.setImage(new Image(file.toURI().toString(), 100, 100, true, true));
            avatarImageView.setVisible(true);
            avatarInitials.setVisible(false);
        }
    }

    @FXML
    private void handleUpdateProfile() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            showError(errorLabel, "All fields are required.");
            return;
        }

        if (firstName.length() < 2 || !firstName.matches("[a-zA-Z\\s]+")) {
            showError(errorLabel, "First name: min 2 chars, letters only.");
            return;
        }

        if (lastName.length() < 2 || !lastName.matches("[a-zA-Z\\s]+")) {
            showError(errorLabel, "Last name: min 2 chars, letters only.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError(errorLabel, "Please enter a valid email.");
            return;
        }

        if (!email.equals(user.getEmail()) && userService.emailExists(email)) {
            showError(errorLabel, "This email is already in use.");
            return;
        }

        // Save avatar if selected
        if (selectedAvatarFile != null) {
            try {
                // Delete old avatar
                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    new File(AVATARS_DIR + user.getAvatar()).delete();
                }
                // Save new avatar
                String ext = selectedAvatarFile.getName().substring(selectedAvatarFile.getName().lastIndexOf('.'));
                String filename = firstName.toLowerCase().replace(" ", "-") + "-" + UUID.randomUUID().toString().substring(0, 8) + ext;
                Path dest = Path.of(AVATARS_DIR, filename);
                Files.copy(selectedAvatarFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                user.setAvatar(filename);
                userService.updateAvatar(user.getId(), filename);
                selectedAvatarFile = null;
            } catch (IOException e) {
                showError(errorLabel, "Error saving avatar: " + e.getMessage());
                return;
            }
        }

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        userService.update(user);
        SessionManager.setCurrentUser(user);
        errorLabel.setVisible(false);

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Profile updated successfully.", ButtonType.OK);
        alert.setHeaderText("Success");
        alert.showAndWait();
    }

    @FXML
    private void handleChangePassword() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        String currentPwd = currentPasswordField.getText();
        String newPwd = newPasswordField.getText();
        String confirmPwd = confirmNewPasswordField.getText();

        if (currentPwd.isEmpty() || newPwd.isEmpty() || confirmPwd.isEmpty()) {
            showError(passwordErrorLabel, "All password fields are required.");
            return;
        }

        User authCheck = userService.authenticate(user.getEmail(), currentPwd);
        if (authCheck == null) {
            showError(passwordErrorLabel, "Current password is incorrect.");
            return;
        }

        if (newPwd.length() < 8) {
            showError(passwordErrorLabel, "Password must be at least 8 characters.");
            return;
        }

        if (!newPwd.equals(confirmPwd)) {
            showError(passwordErrorLabel, "Passwords do not match.");
            return;
        }

        userService.updatePassword(user.getEmail(), newPwd);
        passwordErrorLabel.setVisible(false);
        currentPasswordField.clear();
        newPasswordField.clear();
        confirmNewPasswordField.clear();

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Password changed successfully.", ButtonType.OK);
        alert.setHeaderText("Success");
        alert.showAndWait();
    }

    @FXML
    private void handleDeleteAccount() {
        User user = SessionManager.getCurrentUser();
        if (user == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to permanently delete your account?\n\nThis action cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Delete Account");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                // Delete avatar file
                if (user.getAvatar() != null && !user.getAvatar().isEmpty()) {
                    new File(AVATARS_DIR + user.getAvatar()).delete();
                }
                userService.delete(user.getId());
                SessionManager.clearSession();
                Navigator.navigateTo("login.fxml", Navigator.getStageFromNode(rootPane));
            }
        });
    }

    @FXML
    private void handleBack() {
        User user = SessionManager.getCurrentUser();
        if (user != null && user.getPrimaryRole().getDbValue().equals("ROLE_ADMIN")) {
            Navigator.navigateTo("dashboard.fxml", Navigator.getStageFromNode(rootPane));
        } else {
            Navigator.navigateTo("user-home.fxml", Navigator.getStageFromNode(rootPane));
        }
    }

    private void showError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
    }
}
