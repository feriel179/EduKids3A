package com.edukids.edukids3a.controllers;

import com.edukids.edukids3a.util.JdbcAuthDataSource;
import com.edukids.edukids3a.services.SessionManager;
import com.edukids.edukids3a.models.User;
import com.edukids.edukids3a.services.UserAuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * Connexion (interface adaptée depuis le projet Rami / ZIP).
 * Après succès : callback vers {@link com.edukids.edukids3a.MainFX}.
 */
public class LoginController implements Initializable {

    private Runnable onAuthentificationReussie;

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private CheckBox rememberMeCheck;
    @FXML
    private Label errorLabel;
    @FXML
    private VBox rootPane;

    public void setOnAuthentificationReussie(Runnable callback) {
        this.onAuthentificationReussie = callback;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        masquerErreur();
        Platform.runLater(() -> emailField.requestFocus());
    }

    @FXML
    private void handleLogin() {
        masquerErreur();
        String email = emailField.getText() != null ? emailField.getText().trim() : "";
        String password = passwordField.getText() != null ? passwordField.getText() : "";

        if (email.isEmpty() || password.isEmpty()) {
            afficherErreur("Veuillez remplir tous les champs.");
            return;
        }

        try {
            UserAuthService svc = new UserAuthService(JdbcAuthDataSource.getConnection());
            User found = svc.getByEmail(email);
            if (found != null && !found.isActive()) {
                afficherErreur("Ce compte est désactivé. Contactez un administrateur.");
                return;
            }
            User user = svc.authenticate(email, password);
            if (user == null) {
                afficherErreur("E-mail ou mot de passe incorrect.");
                passwordField.clear();
                return;
            }
            SessionManager.setCurrentUser(user);
            if (onAuthentificationReussie != null) {
                onAuthentificationReussie.run();
            }
        } catch (SQLException e) {
            afficherErreur("Erreur base de données : " + e.getMessage());
        }
    }

    @FXML
    private void handleGoToRegister() {
        new Alert(Alert.AlertType.INFORMATION,
                "L’inscription depuis l’application sera branchée plus tard. Utilisez un compte déjà créé en base.",
                ButtonType.OK).showAndWait();
    }

    @FXML
    private void handleForgotPassword() {
        new Alert(Alert.AlertType.INFORMATION,
                "La réinitialisation du mot de passe sera branchée plus tard.",
                ButtonType.OK).showAndWait();
    }

    private void masquerErreur() {
        if (errorLabel != null) {
            errorLabel.setText("");
            errorLabel.setVisible(false);
            errorLabel.setManaged(false);
        }
    }

    private void afficherErreur(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }
}
