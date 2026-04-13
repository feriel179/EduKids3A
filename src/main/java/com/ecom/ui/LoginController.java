package com.ecom.ui;

import com.ecom.EcomApplication;
import com.ecom.model.User;
import com.ecom.service.AuthService;
import com.ecom.validation.ValidationException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label loginHintLabel;

    private final AuthService authService = new AuthService();

    @FXML
    private void handleLogin() {
        try {
            User user = authService.login(emailField.getText(), passwordField.getText());
            openMainView(user);
        } catch (ValidationException exception) {
            showAlert(Alert.AlertType.ERROR, "Connexion impossible", exception.getMessage());
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Erreur base de donnees", exception.getMessage());
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Erreur interface", exception.getMessage());
        }
    }

    private void openMainView(User user) throws IOException {
        FXMLLoader loader = new FXMLLoader(EcomApplication.class.getResource("/fxml/main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1320, 820);
        scene.getStylesheets().add(EcomApplication.class.getResource("/css/app.css").toExternalForm());

        Stage stage = (Stage) emailField.getScene().getWindow();
        stage.setTitle("EduKids - " + user.getFirstName() + " " + user.getLastName());
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
