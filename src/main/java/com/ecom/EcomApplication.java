package com.ecom;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EcomApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(EcomApplication.class.getResource("/fxml/main-view.fxml"));
        Scene scene = new Scene(loader.load(), 1320, 820);
        scene.getStylesheets().add(EcomApplication.class.getResource("/css/app.css").toExternalForm());

        stage.setTitle("EduKids - E-commerce");
        stage.setScene(scene);
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
