package tn.esprit.controllers.student;

import tn.esprit.models.Course;
import tn.esprit.models.Student;
import tn.esprit.services.StudentService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

public class MyCourseController {
    @FXML
    private FlowPane myCoursesContainer;

    private final StudentService studentService = new StudentService();

    @FXML
    private void initialize() {
        renderCourses();
    }

    private void renderCourses() {
        myCoursesContainer.getChildren().clear();
        Student student = studentService.getCurrentStudent();
        if (student == null || student.getEnrolledCourses().isEmpty()) {
            Label empty = new Label("Your course list is empty. Browse the catalog to enroll.");
            empty.getStyleClass().add("empty-state");
            myCoursesContainer.getChildren().add(empty);
            return;
        }

        List<Course> courses = student.getEnrolledCourses();
        for (Course course : courses) {
            myCoursesContainer.getChildren().add(createCourseCard(course));
        }
    }

    private VBox createCourseCard(Course course) {
        VBox card = new VBox(12);
        card.getStyleClass().addAll("card", "course-card");
        card.setPrefWidth(280);
        card.setPadding(new Insets(18));

        Label title = new Label(course.getTitle());
        title.getStyleClass().add("course-card-title");
        title.setWrapText(true);

        Label subject = new Label(course.getSubject());
        subject.getStyleClass().add("muted-label");

        Label badge = new Label(course.getLevelText());
        badge.getStyleClass().add("level-badge");
        badge.setStyle("-fx-background-color: " + course.getLevelColor() + ";");

        Label description = new Label(course.getDescription());
        description.getStyleClass().add("muted-label");
        description.setWrapText(true);

        Button openButton = new Button("Open Course");
        openButton.getStyleClass().add("primary-button");
        openButton.setOnAction(event -> StudentShellController.getInstance().showCourseDetail(course));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(openButton);
        actions.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(title, subject, badge, description, spacer, actions);
        card.setOnMouseClicked(event -> StudentShellController.getInstance().showCourseDetail(course));
        return card;
    }
}
