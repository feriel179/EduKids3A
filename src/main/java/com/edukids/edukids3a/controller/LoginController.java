package com.edukids.edukids3a.controller;

import com.edukids.edukids3a.model.User;
import com.edukids.edukids3a.security.AuthSession;
import com.edukids.edukids3a.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;

public class LoginController {

    @FXML
    private TextField tfIdentifier;

    @FXML
    private PasswordField pfPassword;

    @FXML
    private Label lblError;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    @FXML
    private void onLogin() {
        String identifier = tfIdentifier.getText() == null ? "" : tfIdentifier.getText().trim();
        String password = pfPassword.getText() == null ? "" : pfPassword.getText().trim();

        if (identifier.isEmpty() || password.isEmpty()) {
            showError("Veuillez saisir l'identifiant et le mot de passe.");
            return;
        }

        User user;
        try {
            user = authService.authenticate(identifier, password);
        } catch (IllegalStateException ex) {
            showError(buildDatabaseErrorMessage(ex));
            return;
        }

        if (user == null) {
            showError("Identifiants incorrects.");
            return;
        }

        String role = user.getRole() == null ? "" : user.getRole().trim().toLowerCase(Locale.ROOT);
        AuthSession.setCurrentUser(user);

        String targetFxml;
        String title;
        if ("parent".equals(role)) {
            targetFxml = "/fxml/FrontOffice.fxml";
            title = "EduKids - Front Office";
        } else if ("admin".equals(role)) {
            targetFxml = "/fxml/BackOffice.fxml";
            title = "EduKids - Back Office";
        } else {
            AuthSession.clear();
            showError("Rôle non autorisé.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(targetFxml));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof FrontOfficeController frontController) {
                frontController.setConnectedUser(user);
            } else if (controller instanceof BackOfficeController backController) {
                backController.setConnectedUser(user);
            }

            Stage stage = (Stage) tfIdentifier.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.setMinWidth(920);
            stage.setMinHeight(620);
            stage.setMaximized(true);
        } catch (IOException e) {
            AuthSession.clear();
            showError("Erreur lors de l'ouverture de l'interface.");
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private String buildDatabaseErrorMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) {
            current = current.getCause();
        }

        String detail = current.getMessage();
        if (detail == null || detail.isBlank()) {
            detail = error.getMessage();
        }
        if (detail == null || detail.isBlank()) {
            detail = "cause inconnue";
        }

        return "Connexion base impossible : " + detail;
    }
}
