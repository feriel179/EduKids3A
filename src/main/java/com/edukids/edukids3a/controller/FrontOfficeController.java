package com.edukids.edukids3a.controller;

import com.edukids.edukids3a.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class FrontOfficeController {

    @FXML
    private Label lblUserInfo;

    public void setConnectedUser(User user) {
        if (lblUserInfo != null && user != null) {
            lblUserInfo.setText("Connecté en tant que " + user.getNom() + " (" + user.getRole() + ")");
        }
    }
}
