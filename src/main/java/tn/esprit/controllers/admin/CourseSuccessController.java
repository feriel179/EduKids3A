package tn.esprit.controllers.admin;

import tn.esprit.models.Course;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class CourseSuccessController {
    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;
    @FXML
    private Label courseMetaLabel;
    @FXML
    private Label helperLabel;

    public void setResult(Course course, boolean updated) {
        if (updated) {
            titleLabel.setText("Your course has been updated successfully.");
            subtitleLabel.setText("The EduKids catalog now reflects the latest version of the selected course.");
            helperLabel.setText("You can return to the course list, open the editor again, or create a brand-new course with the same polished flow.");
        } else {
            titleLabel.setText("Your course has been submitted successfully.");
            subtitleLabel.setText("The EduKids database has received the new course and it is ready for review inside your admin workflow.");
            helperLabel.setText("Continue by reviewing the course list, adding another course, or moving to lesson management.");
        }

        if (course != null) {
            courseMetaLabel.setText(course.getTitle() + "   |   " + course.getSubject() + "   |   " + course.getLevelText());
        } else {
            courseMetaLabel.setText("Course details are ready.");
        }
    }

    @FXML
    private void handleBackToCourses() {
        AdminModuleNavigator.showCourses();
    }

    @FXML
    private void handleCreateAnother() {
        AdminModuleNavigator.showCreateCourse();
    }
}
