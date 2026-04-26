package com.edukids.edukids3a.controller;

import com.edukids.edukids3a.model.User;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class BackOfficeController {

    @FXML
    private Label lblUserInfo;

    public void setConnectedUser(User user) {
        if (lblUserInfo != null && user != null) {
            lblUserInfo.setText("Connecte en tant que " + user.getNom() + " (" + user.getRole() + ")");
        }
    }

    @FXML
    private void onChat() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Scene scene = new Scene(loader.load());
            MainController controller = loader.getController();
            if (controller != null) {
                controller.selectChatMode();
            }

            Stage stage = (Stage) lblUserInfo.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("EduKids - Chat");
            stage.setMaximized(true);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Chat");
            alert.setHeaderText(null);
            alert.setContentText("Impossible d'ouvrir le chat : " + e.getMessage());
            alert.showAndWait();
        }
    }
}
