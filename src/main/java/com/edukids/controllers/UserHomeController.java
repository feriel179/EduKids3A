package com.edukids.controllers;

import com.edukids.entities.User;
import com.edukids.utils.Navigator;
import com.edukids.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class UserHomeController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private Button homeButton;
    @FXML private HBox rootPane;
    @FXML private Label topbarRoleLabel;
    @FXML private Label topbarNameLabel;
    @FXML private Label topbarAvatarLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User current = SessionManager.getCurrentUser();
        if (current != null) {
            welcomeLabel.setText("Welcome, " + current.getFullName());
            roleLabel.setText(current.getPrimaryRole().getDisplayName());
            topbarRoleLabel.setText(current.getPrimaryRole().getDisplayName());
            topbarNameLabel.setText(current.getFullName());
            topbarAvatarLabel.setText(buildInitials(current));
        }
    }

    // ======================== SIDEBAR NAVIGATION (Empty Routes) ========================

    @FXML
    private void handleHome() {
        if (!homeButton.getStyleClass().contains("shell-nav-button-active")) {
            homeButton.getStyleClass().add("shell-nav-button-active");
        }
    }

    @FXML
    private void handleCoursNav() {
        // TODO: Navigate to Cours section
    }

    @FXML
    private void handleQuizNav() {
        // TODO: Navigate to Quiz section
    }

    @FXML
    private void handleEventsNav() {
        // TODO: Navigate to Events section
    }

    @FXML
    private void handleShopNav() {
        // TODO: Navigate to Shop/Produits section
    }

    @FXML
    private void handleChatNav() {
        // TODO: Navigate to Chat section
    }

    @FXML
    private void handleProfileNav() {
        Navigator.navigateTo("profile.fxml", Navigator.getStageFromNode(rootPane));
    }

    @FXML
    private void handleLogout() {
        SessionManager.clearSession();
        Navigator.navigateTo("login.fxml", Navigator.getStageFromNode(rootPane));
    }

    private String buildInitials(User user) {
        if (user == null) {
            return "?";
        }

        String initials = "";
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            initials += user.getFirstName().substring(0, 1).toUpperCase();
        }
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            initials += user.getLastName().substring(0, 1).toUpperCase();
        }
        return initials.isEmpty() ? "?" : initials;
    }
}
