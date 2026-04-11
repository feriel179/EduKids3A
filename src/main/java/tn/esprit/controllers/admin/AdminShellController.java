package tn.esprit.controllers.admin;

import tn.esprit.MainFX;
import tn.esprit.models.Course;
import tn.esprit.models.Lesson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.List;

public class AdminShellController {
    private static AdminShellController instance;

    @FXML
    private StackPane contentPane;
    @FXML
    private Label contextKickerLabel;
    @FXML
    private Label contextTitleLabel;
    @FXML
    private Button dashboardButton;
    @FXML
    private Button coursesButton;
    @FXML
    private Button createCourseButton;
    @FXML
    private Button lessonsButton;
    @FXML
    private Button createLessonButton;

    public static AdminShellController getInstance() {
        return instance;
    }

    @FXML
    private void initialize() {
        instance = this;
        showDashboard();
    }

    @FXML
    private void showDashboard() {
        setContext("Overview", "Dashboard");
        setActiveNavigation(dashboardButton);
        loadCenterView("/tn/esprit/fxml/admin/dashboard.fxml", null);
    }

    @FXML
    public void showCourses() {
        setContext("Course space", "Courses");
        setActiveNavigation(coursesButton);
        loadCenterView("/tn/esprit/fxml/admin/courses.fxml", null);
    }

    @FXML
    public void showCreateCourse() {
        setContext("Course editor", "Create Course");
        setActiveNavigation(createCourseButton);
        loadCenterView("/tn/esprit/fxml/admin/course-form.fxml", loader -> {
            CourseFormController controller = loader.getController();
            controller.initForCreate();
        });
    }

    public void showEditCourse(Course course) {
        setContext("Course editor", "Edit Course");
        setActiveNavigation(createCourseButton);
        loadCenterView("/tn/esprit/fxml/admin/course-form.fxml", loader -> {
            CourseFormController controller = loader.getController();
            controller.initForEdit(course);
        });
    }

    public void showCourseSuccess(Course course, boolean updated) {
        setContext("Course result", "Course saved");
        setActiveNavigation(coursesButton);
        loadCenterView("/tn/esprit/fxml/admin/course-success.fxml", loader -> {
            CourseSuccessController controller = loader.getController();
            controller.setResult(course, updated);
        });
    }

    @FXML
    public void showLessons() {
        setContext("Lesson space", "Lessons");
        setActiveNavigation(lessonsButton);
        loadCenterView("/tn/esprit/fxml/admin/lessons.fxml", null);
    }

    @FXML
    public void showCreateLesson() {
        showCreateLesson(null);
    }

    public void showCreateLesson(Course preselectedCourse) {
        setContext("Lesson editor", "Create Lesson");
        setActiveNavigation(createLessonButton);
        loadCenterView("/tn/esprit/fxml/admin/lesson-form.fxml", loader -> {
            LessonFormController controller = loader.getController();
            controller.initForCreate(preselectedCourse);
        });
    }

    public void showEditLesson(Lesson lesson) {
        setContext("Lesson editor", "Edit Lesson");
        setActiveNavigation(createLessonButton);
        loadCenterView("/tn/esprit/fxml/admin/lesson-form.fxml", loader -> {
            LessonFormController controller = loader.getController();
            controller.initForEdit(lesson);
        });
    }

    public void showLessonSuccess(Lesson lesson, boolean updated) {
        setContext("Lesson result", "Lesson saved");
        setActiveNavigation(lessonsButton);
        loadCenterView("/tn/esprit/fxml/admin/lesson-success.fxml", loader -> {
            LessonSuccessController controller = loader.getController();
            controller.setResult(lesson, updated);
        });
    }

    @FXML
    private void handleLogout() {
        try {
            MainFX.getInstance().showLoginView();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void setContext(String kicker, String title) {
        contextKickerLabel.setText(kicker);
        contextTitleLabel.setText(title);
    }

    private void setActiveNavigation(Button activeButton) {
        List<Button> buttons = List.of(
                dashboardButton,
                coursesButton,
                createCourseButton,
                lessonsButton,
                createLessonButton
        );

        for (Button button : buttons) {
            button.getStyleClass().remove("shell-nav-button-active");
        }

        if (activeButton != null && !activeButton.getStyleClass().contains("shell-nav-button-active")) {
            activeButton.getStyleClass().add("shell-nav-button-active");
        }
    }

    private void loadCenterView(String fxmlPath, LoaderCallback callback) {
        try {
            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource(fxmlPath));
            Parent view = loader.load();
            if (callback != null) {
                callback.accept(loader);
            }
            contentPane.getChildren().setAll(view);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @FunctionalInterface
    private interface LoaderCallback {
        void accept(FXMLLoader loader);
    }
}
