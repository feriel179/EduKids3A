package com.edukids.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Navigator {

    private static final String VIEWS_PATH = "/com/edukids/views/";
    private static final String CSS_PATH = "/com/edukids/css/style.css";

    public static void navigateTo(String fxmlFile, Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(VIEWS_PATH + fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            String css = Navigator.class.getResource(CSS_PATH).toExternalForm();
            scene.getStylesheets().add(css);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to navigate to " + fxmlFile + ": " + e.getMessage(), e);
        }
    }

    public static <T> T navigateToAndGetController(String fxmlFile, Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(Navigator.class.getResource(VIEWS_PATH + fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            String css = Navigator.class.getResource(CSS_PATH).toExternalForm();
            scene.getStylesheets().add(css);
            stage.setScene(scene);
            stage.show();
            return loader.getController();
        } catch (IOException e) {
            throw new RuntimeException("Failed to navigate to " + fxmlFile + ": " + e.getMessage(), e);
        }
    }

    public static Stage getStageFromNode(javafx.scene.Node node) {
        return (Stage) node.getScene().getWindow();
    }
}
