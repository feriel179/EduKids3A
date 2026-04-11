package com.edukids.controllers;

import com.edukids.entities.User;
import com.edukids.utils.Navigator;
import com.edukids.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class UserHomeController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private HBox rootPane;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        User current = SessionManager.getCurrentUser();
        if (current != null) {
            welcomeLabel.setText("Welcome, " + current.getFullName());
            roleLabel.setText(current.getPrimaryRole().getDisplayName());
        }
    }

    // ======================== SIDEBAR NAVIGATION (Empty Routes) ========================

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
}
