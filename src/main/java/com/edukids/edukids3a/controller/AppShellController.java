package com.edukids.edukids3a.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class AppShellController {

    @FXML
    private StackPane contentHost;

    public void setContent(Node content) {
        contentHost.getChildren().setAll(content);
    }
}
