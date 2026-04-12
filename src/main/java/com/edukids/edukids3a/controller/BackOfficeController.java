package com.edukids.edukids3a.controller;

import com.edukids.edukids3a.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Chat");
        alert.setHeaderText(null);
        alert.setContentText("Le module Chat n'est pas encore connecte.");
        alert.showAndWait();
    }
}
