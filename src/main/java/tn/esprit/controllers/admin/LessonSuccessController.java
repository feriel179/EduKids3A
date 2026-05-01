package tn.esprit.controllers.admin;

import tn.esprit.models.Course;
import tn.esprit.models.Lesson;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class LessonSuccessController {
    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;
    @FXML
    private Label lessonMetaLabel;
    @FXML
    private Label helperLabel;

    private Course currentCourse;

    public void setResult(Lesson lesson, boolean updated) {
        currentCourse = lesson == null ? null : lesson.getCourse();

        if (updated) {
            titleLabel.setText("Your lesson has been updated successfully.");
            subtitleLabel.setText("The selected EduKids course now uses the latest lesson details and media links.");
            helperLabel.setText("You can return to the lesson list, open the editor again, or create another lesson with the same visual workflow.");
        } else {
            titleLabel.setText("Your lesson has been submitted successfully.");
            subtitleLabel.setText("The new lesson has been added to EduKids and is ready to appear in the selected course flow.");
            helperLabel.setText("Continue by reviewing the lesson list, creating another lesson, or going back to course management.");
        }

        if (lesson != null) {
            String courseTitle = lesson.getCourse() == null ? "No course" : lesson.getCourse().getTitle();
            lessonMetaLabel.setText(lesson.getTitle() + "   |   " + courseTitle + "   |   " + lesson.getDisplayMediaType());
        } else {
            lessonMetaLabel.setText("Lesson details are ready.");
        }
    }

    @FXML
    private void handleBackToLessons() {
        AdminModuleNavigator.showLessons();
    }

    @FXML
    private void handleCreateAnother() {
        AdminModuleNavigator.showCreateLesson(currentCourse);
    }
}
