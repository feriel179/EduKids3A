package com.edukids.edukids3a;

import com.edukids.edukids3a.controller.QuestionController;
import com.edukids.edukids3a.controller.QuizController;
import com.edukids.edukids3a.controller.AppShellController;
import com.edukids.edukids3a.controller.QuizResultController;
import com.edukids.edukids3a.service.QuizService;
import com.edukids.edukids3a.service.QuestionService;
import com.edukids.edukids3a.service.QuizResultService;
import com.edukids.edukids3a.ui.QuizBackOfficeView;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class EduKidsApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        QuizService quizService = new QuizService();
        QuestionService questionService = new QuestionService(quizService);
        QuizResultService quizResultService = new QuizResultService();
        QuizController quizController = new QuizController(quizService);
        QuestionController questionController = new QuestionController(questionService);
        QuizResultController quizResultController = new QuizResultController(quizResultService);
        QuizBackOfficeView view = new QuizBackOfficeView(quizController, questionController, quizResultController);
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/app-shell.fxml"));
        Parent root = loader.load();
        AppShellController shellController = loader.getController();
        shellController.setContent(view.build());

        Scene scene = new Scene(root, 1120, 760);
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
