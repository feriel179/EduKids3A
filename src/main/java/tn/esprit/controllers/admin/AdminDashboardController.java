package tn.esprit.controllers.admin;

import tn.esprit.services.CourseService;
import tn.esprit.services.LessonService;
import tn.esprit.services.StudentService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class AdminDashboardController {
    @FXML
    private Label totalCoursesLabel;
    @FXML
    private Label totalLessonsLabel;
    @FXML
    private Label totalStudentsLabel;

    private final CourseService courseService = new CourseService();
    private final LessonService lessonService = new LessonService();
    private final StudentService studentService = new StudentService();

    @FXML
    private void initialize() {
        refreshMetrics();
    }

    private void refreshMetrics() {
        totalCoursesLabel.setText(String.valueOf(courseService.countCourses()));
        totalLessonsLabel.setText(String.valueOf(lessonService.countLessons()));
        totalStudentsLabel.setText(String.valueOf(studentService.countStudents()));
    }
}
