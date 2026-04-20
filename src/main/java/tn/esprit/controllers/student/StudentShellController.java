package tn.esprit.controllers.student;

import tn.esprit.MainFX;
import tn.esprit.models.Course;
import tn.esprit.models.Lesson;
import tn.esprit.models.Student;
import tn.esprit.services.StudentService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class StudentShellController {
    private static StudentShellController instance;

    @FXML
    private StackPane contentPane;
    @FXML
    private Button catalogButton;
    @FXML
    private Button myCoursesButton;
    @FXML
    private Button profileButton;
    @FXML
    private Label studentNameLabel;
    @FXML
    private Label studentEmailLabel;
    @FXML
    private Label studentAvatarLabel;

    private final StudentService studentService = new StudentService();

    public static StudentShellController getInstance() {
        return instance;
    }

    @FXML
    private void initialize() {
        instance = this;
        populateStudentHeader();
        showCatalog();
    }

    @FXML
    public void showCatalog() {
        loadCenterView("/tn/esprit/fxml/student/catalog.fxml", null);
        setActiveNavigation(catalogButton);
    }

    @FXML
    public void showMyCourses() {
        loadCenterView("/tn/esprit/fxml/student/my-courses.fxml", null);
        setActiveNavigation(myCoursesButton);
    }

    @FXML
    public void showProfile() {
        loadCenterView("/tn/esprit/fxml/student/profile.fxml", null);
        setActiveNavigation(profileButton);
    }

    @FXML
    private void handleLogout() {
        try {
            MainFX.getInstance().showLoginView();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void showCourseDetail(Course course) {
        loadCenterView("/tn/esprit/fxml/student/course-detail.fxml", loader -> {
            CourseDetailController controller = loader.getController();
            controller.setCourse(course);
        });
        setActiveNavigation(catalogButton);
    }

    public void showExerciseStudio(Course course, Lesson lesson) {
        loadCenterView("/tn/esprit/fxml/student/exercise-studio.fxml", loader -> {
            ExerciseStudioController controller = loader.getController();
            controller.setContext(course, lesson);
        });
        setActiveNavigation(catalogButton);
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

    private void populateStudentHeader() {
        Student student = studentService.getCurrentStudent();
        if (student == null) {
            studentNameLabel.setText("EduKids Learner");
            studentEmailLabel.setText("student@edukids.local");
            studentAvatarLabel.setText("ST");
            return;
        }

        studentNameLabel.setText(student.getName());
        studentEmailLabel.setText(student.getAgeGroupLabel() + " | " + student.getPreferredCategory());
        studentAvatarLabel.setText(buildInitials(student.getName(), student.getEmail()));
    }

    public void refreshStudentHeader() {
        populateStudentHeader();
    }

    private void setActiveNavigation(Button activeButton) {
        clearNavigationState();
        if (activeButton != null && !activeButton.getStyleClass().contains("student-nav-button-active")) {
            activeButton.getStyleClass().add("student-nav-button-active");
        }
    }

    private void clearNavigationState() {
        Button[] buttons = {catalogButton, myCoursesButton, profileButton};
        for (Button button : buttons) {
            if (button != null) {
                button.getStyleClass().remove("student-nav-button-active");
            }
        }
    }

    private String buildInitials(String name, String email) {
        String source = name != null && !name.isBlank() ? name : email;
        if (source == null || source.isBlank()) {
            return "ST";
        }

        String[] parts = source.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }

        String clean = source.replaceAll("[^A-Za-z0-9]", "");
        if (clean.length() >= 2) {
            return clean.substring(0, 2).toUpperCase();
        }

        return clean.isBlank() ? "ST" : clean.toUpperCase();
    }

    @FunctionalInterface
    private interface LoaderCallback {
        void accept(FXMLLoader loader);
    }
}
