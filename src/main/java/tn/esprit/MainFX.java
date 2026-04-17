package tn.esprit;

import tn.esprit.models.Student;
import tn.esprit.util.MyConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.io.IOException;

public class MainFX extends Application {
    private static MainFX instance;
    private Stage primaryStage;

    public static MainFX getInstance() {
        return instance;
    }

    @Override
    public void start(Stage stage) throws IOException { 
        instance = this;
        this.primaryStage = stage;
        initializeDatabase();
        showLoginView();
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(720);
        primaryStage.show();
    }

    private void initializeDatabase() {
        try {
            MyConnection.getInstance().getCnx();
        } catch (RuntimeException exception) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Database Warning");
            alert.setHeaderText("EduKids database is not ready");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }
    }

    public void showLoginView() throws IOException {
        Parent root = loadView("/tn/esprit/fxml/login.fxml");
        setScene(root, "CourseApp - Login");
    }

    public void showAdminShell() throws IOException {
        Parent root = loadView("/tn/esprit/fxml/admin/admin-shell.fxml");
        setScene(root, "CourseApp - Admin Panel");
    }

    public void showStudentShell(Student student) throws IOException {
        Parent root = loadView("/tn/esprit/fxml/student/student-shell.fxml");
        setScene(root, "CourseApp - Student Panel - " + student.getName());
    }

    public Parent loadView(String resourcePath) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainFX.class.getResource(resourcePath));
        return loader.load();
    }

    private void setScene(Parent root, String title) {
        Scene scene = new Scene(root, 1280, 820);
        scene.getStylesheets().add(MainFX.class.getResource("/tn/esprit/css/styles.css").toExternalForm());
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
