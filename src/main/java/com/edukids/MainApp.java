package com.edukids;

import com.edukids.utils.Navigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("EduKids - Educational Platform");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        Navigator.navigateTo("login.fxml", primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
