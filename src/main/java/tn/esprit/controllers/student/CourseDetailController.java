package tn.esprit.controllers.student;

import javafx.concurrent.Task;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import tn.esprit.models.Course;
import tn.esprit.models.CourseProgressSummary;
import tn.esprit.models.Lesson;
import tn.esprit.models.Student;
import tn.esprit.services.LessonService;
import tn.esprit.services.LocalAiContentService;
import tn.esprit.services.StudentService;
import tn.esprit.util.SweetAlert;
import tn.esprit.util.StudentAiSupportDialogs;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

public class CourseDetailController {
    private static final double HERO_HEIGHT = 270;

    @FXML
    private StackPane courseHeroPane;
    @FXML
    private ImageView courseImageView;
    @FXML
    private Label courseInitialsLabel;
    @FXML
    private Label courseTitleLabel;
    @FXML
    private Label courseSummaryLabel;
    @FXML
    private Label subjectLabel;
    @FXML
    private Label courseIdLabel;
    @FXML
    private Label levelBadgeLabel;
    @FXML
    private Label lessonCountLabel;
    @FXML
    private Label durationLabel;
    @FXML
    private Label descriptionLabel;
    @FXML
    private VBox lessonContainer;

    private final LessonService lessonService = new LessonService();
    private final StudentService studentService = new StudentService();
    private final LocalAiContentService localAiContentService = new LocalAiContentService();
    private final Rectangle heroClip = new Rectangle();
    private Course currentCourse;
    private ObservableList<Lesson> currentLessons;
    private Set<Long> completedLessonIds = Set.of();

    @FXML
    private void initialize() {
        courseHeroPane.setMinHeight(HERO_HEIGHT);
        courseHeroPane.setPrefHeight(HERO_HEIGHT);
        courseHeroPane.setMaxHeight(HERO_HEIGHT);

        courseImageView.fitWidthProperty().bind(courseHeroPane.widthProperty());
        courseImageView.setFitHeight(HERO_HEIGHT);
        courseImageView.setPreserveRatio(true);
        heroClip.setArcWidth(34);
        heroClip.setArcHeight(34);
        heroClip.widthProperty().bind(courseHeroPane.widthProperty());
        heroClip.setHeight(HERO_HEIGHT);
        courseImageView.setClip(heroClip);
    }

    public void setCourse(Course course) {
        currentCourse = course;
        currentLessons = lessonService.getPublishedLessonsByCourse(course);
        completedLessonIds = studentService.getCompletedLessonIds(course);

        courseTitleLabel.setText(safeText(course.getTitle(), "Untitled course"));
        subjectLabel.setText(safeText(course.getSubject(), "General"));
        courseIdLabel.setText("Course #" + course.getId());
        levelBadgeLabel.setText(course.getLevelText());
        levelBadgeLabel.setStyle("-fx-background-color: " + course.getLevelColor() + ";");
        courseSummaryLabel.setText(summarize(course.getDescription(), 168));
        descriptionLabel.setText(safeText(course.getDescription(), "No description is available for this course yet."));
        lessonCountLabel.setText(formatLessonCount(currentLessons.size()));
        durationLabel.setText(resolveDurationLabel(course, currentLessons));
        courseInitialsLabel.setText(buildCourseInitials(course));
        courseHeroPane.setStyle(buildHeroStyle(course));
        updateHeroImage(course);
        renderLessons();
    }

    private void renderLessons() {
        lessonContainer.getChildren().clear();
        if (currentLessons == null || currentLessons.isEmpty()) {
            Label empty = new Label("No lessons or exercises are available for this course yet.");
            empty.getStyleClass().add("empty-state");
            empty.setWrapText(true);
            lessonContainer.getChildren().add(empty);
            return;
        }

        for (Lesson lesson : currentLessons) {
            lessonContainer.getChildren().add(createLessonCard(lesson));
        }
    }

    private VBox createLessonCard(Lesson lesson) {
        boolean completed = completedLessonIds.contains(lesson.getId());
        boolean drawingEnabled = isDrawingEnabled();

        Label lessonOrderBadge = new Label("Lesson " + lesson.getOrder());
        lessonOrderBadge.getStyleClass().add("student-mini-badge");

        Label durationBadge = new Label(lesson.getDurationLabel());
        durationBadge.getStyleClass().add("student-mini-badge");

        Label mediaBadge = new Label(lesson.getDisplayMediaType());
        mediaBadge.getStyleClass().add("student-mini-badge");

        Label completionBadge = new Label(completed ? "Completed" : "To do");
        completionBadge.getStyleClass().add(completed ? "student-progress-complete-badge" : "student-progress-todo-badge");

        HBox badgeRow = new HBox(8, lessonOrderBadge, durationBadge, mediaBadge, completionBadge);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(lesson.getTitle());
        titleLabel.getStyleClass().add("student-lesson-title");
        titleLabel.setWrapText(true);

        Label helperLabel = new Label(drawingEnabled
                ? "Open the lesson resources, then continue to the exercise studio to draw and write."
                : "Open the lesson resources, then continue to the exercise studio to write your answer.");
        helperLabel.getStyleClass().add("student-detail-section-subtitle");
        helperLabel.setWrapText(true);

        FlowPane actions = new FlowPane();
        actions.setHgap(10);
        actions.setVgap(10);
        boolean hasResource = false;
        hasResource = addResourceButton(actions, "Open PDF", lesson.getPdfUrl(), "PDF") || hasResource;
        hasResource = addResourceButton(actions, "Open Video", lesson.getVideoUrl(), "Video") || hasResource;
        hasResource = addResourceButton(actions, "Open YouTube", lesson.getYoutubeUrl(), "YouTube") || hasResource;
        actions.getChildren().add(createCompletionButton(lesson, completed));

        if (!hasResource) {
            helperLabel.setText("No PDF, video, or YouTube resource is available yet. You can still open the exercise studio.");
        }

        Label exerciseSummaryLabel = new Label(drawingEnabled
                ? "Exercises and drawing tools will load when you open the studio."
                : "Exercises will load when you open the studio.");
        exerciseSummaryLabel.getStyleClass().add("student-detail-section-subtitle");
        exerciseSummaryLabel.setWrapText(true);

        Button studioButton = new Button("Open Exercise Studio");
        studioButton.getStyleClass().add("primary-button");
        studioButton.setOnAction(event -> {
            if (StudentShellController.getInstance() != null && currentCourse != null) {
                StudentShellController.getInstance().showExerciseStudio(currentCourse, lesson);
            }
        });

        Button askAiButton = new Button("Ask AI");
        askAiButton.getStyleClass().addAll("student-lesson-action-button", "student-lesson-action-ai");
        askAiButton.setOnAction(event -> handleAskAiForLesson(lesson, askAiButton));

        HBox aiActionRow = new HBox(10, askAiButton, studioButton);
        aiActionRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, badgeRow, titleLabel, helperLabel, actions, exerciseSummaryLabel, aiActionRow);
        card.getStyleClass().add("student-lesson-card");
        card.setPadding(new Insets(18));
        return card;
    }

    private boolean isDrawingEnabled() {
        return true;
    }

    private Button createResourceButton(String text, String location, String resourceType) {
        if (!hasText(location)) {
            return null;
        }

        Button button = new Button(text);
        button.getStyleClass().addAll("student-lesson-action-button", resolveResourceButtonStyleClass(resourceType));
        button.setOnAction(event -> openResource(location, resourceType));
        return button;
    }

    private Button createCompletionButton(Lesson lesson, boolean completed) {
        Button button = new Button(completed ? "Mark as Not Done" : "Mark Complete");
        button.getStyleClass().addAll(completed ? "ghost-button" : "primary-button", "student-lesson-progress-button");
        button.setOnAction(event -> handleToggleLessonCompletion(lesson, !completed));
        return button;
    }

    private boolean addResourceButton(FlowPane actions, String text, String location, String resourceType) {
        Button button = createResourceButton(text, location, resourceType);
        if (button != null) {
            actions.getChildren().add(button);
            return true;
        }
        return false;
    }

    private void handleToggleLessonCompletion(Lesson lesson, boolean completed) {
        if (currentCourse == null || lesson == null) {
            return;
        }

        CourseProgressSummary summary = studentService.updateLessonCompletion(currentCourse, lesson, completed);
        completedLessonIds = studentService.getCompletedLessonIds(currentCourse);
        updateCurrentCourseProgress(summary);
        renderLessons();
    }

    private void handleAskAiForLesson(Lesson lesson, Button sourceButton) {
        if (lesson == null || currentCourse == null) {
            SweetAlert.warning("AI Tutor", "Open a lesson before asking the AI tutor.");
            return;
        }

        String helperText = "Ask a question about \"" + safeText(lesson.getTitle(), "this lesson")
                + "\". Example: explique-moi cette lecon plus simplement.";
        String defaultQuestion = "Explique-moi cette lecon plus simplement.";

        var question = StudentAiSupportDialogs.promptQuestion("Ask AI", helperText, defaultQuestion);
        if (question.isEmpty()) {
            return;
        }
        if (question.get().isBlank()) {
            SweetAlert.warning("AI Tutor", "Write a question before asking the AI tutor.");
            return;
        }

        sourceButton.setDisable(true);

        Student student = studentService.getCurrentStudent();
        int studentAge = student == null ? 10 : Math.max(8, student.getAge());

        Task<String> tutorTask = new Task<>() {
            @Override
            protected String call() {
                return localAiContentService.answerLessonQuestion(
                        currentCourse.getTitle(),
                        currentCourse.getSubject(),
                        currentCourse.getLevel(),
                        currentCourse.getDescription(),
                        lesson.getTitle(),
                        lesson.getUrlSummary(),
                        studentAge,
                        question.get()
                );
            }
        };

        tutorTask.setOnSucceeded(event -> {
            sourceButton.setDisable(false);
            StudentAiSupportDialogs.showTutorAnswer("AI Tutor - " + safeText(lesson.getTitle(), "Lesson"), tutorTask.getValue());
        });

        tutorTask.setOnFailed(event -> {
            sourceButton.setDisable(false);
            String message = tutorTask.getException() == null ? "The AI tutor is unavailable right now." : tutorTask.getException().getMessage();
            SweetAlert.error("AI Tutor", message);
        });

        Thread worker = new Thread(tutorTask, "lesson-ai-tutor");
        worker.setDaemon(true);
        worker.start();
    }

    private void updateCurrentCourseProgress(CourseProgressSummary summary) {
        if (summary == null) {
            return;
        }

        if (currentCourse != null) {
            currentCourse.setCompletedLessonCount(summary.getCompletedLessons());
            currentCourse.setProgressPercent(summary.getProgressPercent());
            currentCourse.setLessonCount(summary.getTotalLessons());
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

    private void updateHeroImage(Course course) {
        String imageSource = resolveCourseImageSource(course.getImage());
        if (imageSource == null) {
            courseImageView.setImage(null);
            courseImageView.setVisible(false);
            courseImageView.setManaged(false);
            courseInitialsLabel.setVisible(true);
            courseInitialsLabel.setManaged(true);
            return;
        }

        Image image = new Image(imageSource, true);
        courseImageView.setImage(image);
        courseImageView.setVisible(true);
        courseImageView.setManaged(true);
        courseInitialsLabel.setVisible(false);
        courseInitialsLabel.setManaged(false);
        image.errorProperty().addListener((obs, oldValue, hasError) -> {
            if (Boolean.TRUE.equals(hasError)) {
                courseImageView.setImage(null);
                courseImageView.setVisible(false);
                courseImageView.setManaged(false);
                courseInitialsLabel.setVisible(true);
                courseInitialsLabel.setManaged(true);
            }
        });
    }

    private String buildHeroStyle(Course course) {
        int palette = Math.abs(safeText(course.getSubject()).toLowerCase(Locale.ROOT).hashCode() + course.getLevel()) % 5;
        String gradient = switch (palette) {
            case 0 -> "linear-gradient(to bottom right, #0f4c81 0%, #1f73ff 60%, #6ea8ff 100%)";
            case 1 -> "linear-gradient(to bottom right, #ff9a62 0%, #ff7b7b 55%, #ffb067 100%)";
            case 2 -> "linear-gradient(to bottom right, #7446d8 0%, #9277ff 58%, #c4b5fd 100%)";
            case 3 -> "linear-gradient(to bottom right, #0f8a7e 0%, #18b38a 58%, #71e6c1 100%)";
            default -> "linear-gradient(to bottom right, #23334f 0%, #39598a 58%, #6d89b8 100%)";
        };
        return "-fx-background-color: " + gradient + ";";
    }

    private String resolveResourceButtonStyleClass(String resourceType) {
        return switch (safeText(resourceType).toLowerCase(Locale.ROOT)) {
            case "pdf" -> "student-lesson-action-pdf";
            case "video" -> "student-lesson-action-video";
            default -> "student-lesson-action-youtube";
        };
    }

    private String buildCourseInitials(Course course) {
        String source = safeText(course.getTitle(), safeText(course.getSubject(), "Course")).trim();
        String[] parts = source.split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }

        String clean = source.replaceAll("[^\\p{L}\\p{N}]", "");
        if (clean.length() >= 2) {
            return clean.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        return clean.isBlank() ? "ED" : clean.toUpperCase(Locale.ROOT);
    }

    private String resolveCourseImageSource(String value) {
        if (value == null || value.isBlank() || "course-default.png".equalsIgnoreCase(value.trim())) {
            return null;
        }

        String trimmedValue = value.trim();
        if (trimmedValue.startsWith("http://") || trimmedValue.startsWith("https://") || trimmedValue.startsWith("file:/")) {
            return trimmedValue;
        }

        Path localPath = Path.of(trimmedValue);
        if (Files.exists(localPath)) {
            return localPath.toUri().toString();
        }

        URL resource = getClass().getResource("/tn/esprit/images/" + trimmedValue);
        return resource != null ? resource.toExternalForm() : null;
    }

    private String formatLessonCount(int lessonCount) {
        return lessonCount + (lessonCount == 1 ? " lesson" : " lessons");
    }

    private String resolveDurationLabel(Course course, ObservableList<Lesson> lessons) {
        int totalDuration = lessons.stream().mapToInt(Lesson::getDurationMinutes).sum();
        if (totalDuration <= 0) {
            totalDuration = course.getTotalDurationMinutes();
        }
        return Course.formatDuration(totalDuration);
    }

    private String summarize(String text, int maxLength) {
        String value = safeText(text, "Open this course to discover the full learning path.");
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeText(String value) {
        return safeText(value, "");
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
