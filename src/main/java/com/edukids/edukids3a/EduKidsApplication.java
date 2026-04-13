package com.edukids.edukids3a;

import com.edukids.edukids3a.service.QuizService;
import com.edukids.edukids3a.service.QuestionService;
import com.edukids.edukids3a.ui.QuizBackOfficeView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EduKidsApplication extends Application {

    @Override
    public void start(Stage stage) {
        QuizService quizService = new QuizService();
        QuestionService questionService = new QuestionService(quizService);
        QuizBackOfficeView view = new QuizBackOfficeView(quizService, questionService);

        Scene scene = new Scene(view.build(), 1120, 760);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        stage.setTitle("EduKids - Evenements & programmes");
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
