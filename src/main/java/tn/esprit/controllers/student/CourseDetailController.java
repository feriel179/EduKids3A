package tn.esprit.controllers.student;

import tn.esprit.models.Course;
import tn.esprit.models.Lesson;
import tn.esprit.services.LessonService;
import tn.esprit.util.SweetAlert;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;

public class CourseDetailController {
    @FXML
    private Label courseTitleLabel;
    @FXML
    private Label subjectLabel;
    @FXML
    private Label levelBadgeLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private ListView<Lesson> lessonListView;

    private final LessonService lessonService = new LessonService();

    @FXML
    private void initialize() {
        lessonListView.setPlaceholder(new Label("No lessons available for this course."));
        lessonListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Lesson lesson, boolean empty) {
                super.updateItem(lesson, empty);
                if (!getStyleClass().contains("student-lesson-cell")) {
                    getStyleClass().add("student-lesson-cell");
                }
                if (empty || lesson == null) {
                    setText(null);
                    setGraphic(null);
                    setContentDisplay(ContentDisplay.TEXT_ONLY);
                } else {
                    setText(null);
                    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                    setGraphic(createLessonCard(lesson));
                }
            }
        });
    }

    public void setCourse(Course course) {
        courseTitleLabel.setText(course.getTitle());
        subjectLabel.setText(course.getSubject());
        levelBadgeLabel.setText(course.getLevelText());
        levelBadgeLabel.setStyle("-fx-background-color: " + course.getLevelColor() + ";");
        descriptionLabel.setText(course.getDescription());
        lessonListView.setItems(lessonService.getPublishedLessonsByCourse(course));
    }

    private VBox createLessonCard(Lesson lesson) {
        Label lessonOrderBadge = new Label("Lesson " + lesson.getOrder());
        lessonOrderBadge.getStyleClass().add("student-mini-badge");

        Label mediaBadge = new Label(lesson.getDisplayMediaType());
        mediaBadge.getStyleClass().add("student-mini-badge");

        HBox badgeRow = new HBox(8, lessonOrderBadge, mediaBadge);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(lesson.getTitle());
        titleLabel.getStyleClass().add("student-lesson-title");
        titleLabel.setWrapText(true);

        Label helperLabel = new Label("Choose a lesson resource to open.");
        helperLabel.getStyleClass().add("muted-label");
        helperLabel.setWrapText(true);

        FlowPane actions = new FlowPane();
        actions.setHgap(10);
        actions.setVgap(10);
        addResourceButton(actions, "Open PDF", lesson.getPdfUrl(), "PDF");
        addResourceButton(actions, "Open Video", lesson.getVideoUrl(), "Video");
        addResourceButton(actions, "Open YouTube", lesson.getYoutubeUrl(), "YouTube");

        if (actions.getChildren().isEmpty()) {
            helperLabel.setText("No PDF, video, or YouTube resource is available for this lesson yet.");
        }

        VBox card = new VBox(10, badgeRow, titleLabel, helperLabel, actions);
        card.getStyleClass().addAll("card", "student-lesson-card");
        card.setPadding(new Insets(16));
        return card;
    }

    private Button createResourceButton(String text, String location, String resourceType) {
        if (!hasText(location)) {
            return null;
        }

        Button button = new Button(text);
        button.getStyleClass().addAll("secondary-button", "student-lesson-action-button");
        button.setOnAction(event -> openResource(location, resourceType));
        return button;
    }

    private void addResourceButton(FlowPane actions, String text, String location, String resourceType) {
        Button button = createResourceButton(text, location, resourceType);
        if (button != null) {
            actions.getChildren().add(button);
        }
    }

    private void openResource(String location, String resourceType) {
        String value = location == null ? "" : location.trim();
        if (value.isBlank()) {
            SweetAlert.warning("Unavailable", "No " + resourceType + " resource is available for this lesson.");
            return;
        }

        try {
            if (!Desktop.isDesktopSupported()) {
                throw new IllegalStateException("Desktop integration is not supported.");
            }

            Desktop desktop = Desktop.getDesktop();
            if (value.startsWith("file:/")) {
                File file = new File(URI.create(value));
                if (!file.exists()) {
                    SweetAlert.warning("File Missing", "The selected " + resourceType + " file could not be found.");
                    return;
                }
                desktop.open(file);
                return;
            }

            File plainFile = new File(value);
            if (plainFile.exists()) {
                desktop.open(plainFile);
                return;
            }

            desktop.browse(URI.create(value));
        } catch (Exception exception) {
            SweetAlert.error("Open Failed", "Unable to open the " + resourceType + " resource.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
