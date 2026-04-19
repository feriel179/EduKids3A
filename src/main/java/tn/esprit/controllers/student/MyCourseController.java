package tn.esprit.controllers.student;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import tn.esprit.models.Course;
import tn.esprit.models.Student;
import tn.esprit.services.StudentService;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public class MyCourseController {
    private static final double COURSE_CARD_WIDTH = 316;
    private static final double COURSE_CARD_HEIGHT = 500;

    @FXML
    private FlowPane myCoursesContainer;
    @FXML
    private Label savedCountLabel;
    @FXML
    private Label savedDurationLabel;
    @FXML
    private Label coursesReadPercentLabel;
    @FXML
    private Label coursesReadMetaLabel;
    @FXML
    private Label completedCoursesValueLabel;
    @FXML
    private Label completedCoursesMetaLabel;
    @FXML
    private Label remainingCoursesValueLabel;
    @FXML
    private Label remainingCoursesMetaLabel;

    private final StudentService studentService = new StudentService();

    @FXML
    private void initialize() {
        renderCourses();
    }

    private void renderCourses() {
        myCoursesContainer.getChildren().clear();
        Student student = studentService.getCurrentStudent();
        if (student == null || student.getEnrolledCourses().isEmpty()) {
            savedCountLabel.setText("0 courses");
            savedDurationLabel.setText("0 min");
            updateStats(List.of());

            Label empty = new Label("Your course list is empty. Browse the catalog to enroll.");
            empty.getStyleClass().add("empty-state");
            empty.setMaxWidth(420);
            empty.setWrapText(true);
            myCoursesContainer.getChildren().add(empty);
            return;
        }

        List<Course> courses = student.getEnrolledCourses();
        savedCountLabel.setText(courses.size() + (courses.size() == 1 ? " course" : " courses"));
        savedDurationLabel.setText(Course.formatDuration(courses.stream().mapToInt(Course::getTotalDurationMinutes).sum()));
        updateStats(courses);

        for (Course course : courses) {
            myCoursesContainer.getChildren().add(createCourseCard(course));
        }
    }

    private VBox createCourseCard(Course course) {
        VBox card = new VBox(0);
        card.getStyleClass().addAll("card", "course-card", "my-course-card");
        card.setPrefWidth(COURSE_CARD_WIDTH);
        card.setMaxWidth(COURSE_CARD_WIDTH);
        card.setMinHeight(COURSE_CARD_HEIGHT);
        card.setPrefHeight(COURSE_CARD_HEIGHT);
        card.setMaxHeight(COURSE_CARD_HEIGHT);

        StackPane header = new StackPane();
        header.getStyleClass().add("my-course-card-header");
        header.setPrefHeight(126);
        header.setMinHeight(126);
        header.setMaxWidth(Double.MAX_VALUE);
        header.setStyle(buildHeaderStyle(course));

        ImageView coverImageView = createCourseCoverView(course, COURSE_CARD_WIDTH, 126, 30);
        if (coverImageView != null) {
            Region coverOverlay = new Region();
            coverOverlay.getStyleClass().add("course-card-cover-overlay");
            coverOverlay.setPrefSize(COURSE_CARD_WIDTH, 126);
            header.getChildren().addAll(coverImageView, coverOverlay);
        }

        Label subjectChip = new Label(buildDisplaySubjectName(course.getSubject(), "General"));
        subjectChip.getStyleClass().add("student-course-chip");

        Label idChip = new Label("Course #" + course.getId());
        idChip.getStyleClass().add("student-course-chip-muted");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topRow = new HBox(8, subjectChip, topSpacer, idChip);
        topRow.setAlignment(Pos.TOP_LEFT);

        Label initials = new Label(buildCourseInitials(course));
        initials.getStyleClass().add("student-course-media-initials");
        initials.setVisible(coverImageView == null);
        initials.setManaged(coverImageView == null);

        Label kicker = new Label("Status: " + course.getStatusLabel());
        kicker.getStyleClass().addAll("my-course-header-kicker", "my-course-status-badge");
        kicker.getStyleClass().add(resolveStatusStyleClass(course));

        Region mediaSpacer = new Region();
        VBox.setVgrow(mediaSpacer, Priority.ALWAYS);

        VBox headerContent = new VBox(10, topRow, mediaSpacer, initials, kicker);
        headerContent.setPadding(new Insets(16));
        StackPane.setAlignment(headerContent, Pos.TOP_LEFT);
        header.getChildren().add(headerContent);

        VBox body = new VBox(12);
        body.getStyleClass().add("my-course-card-body");
        body.setPadding(new Insets(18));
        body.setFillWidth(true);
        VBox.setVgrow(body, Priority.ALWAYS);

        Label title = new Label(safeText(course.getTitle(), "Untitled course"));
        title.getStyleClass().add("course-card-title");
        title.setWrapText(true);

        Label summary = new Label(summarize(course.getDescription(), 150));
        summary.getStyleClass().add("my-course-summary");
        summary.setWrapText(true);
        summary.setMinHeight(92);
        summary.setPrefHeight(92);
        summary.setMaxHeight(92);

        Label levelBadge = new Label(course.getLevelText());
        levelBadge.getStyleClass().add("level-badge");
        levelBadge.setStyle("-fx-background-color: " + course.getLevelColor() + ";");

        Label lessonBadge = new Label(course.getLessonCount() + (course.getLessonCount() == 1 ? " lesson" : " lessons"));
        lessonBadge.getStyleClass().add("student-mini-badge");

        Label durationBadge = new Label(course.getTotalDurationLabel());
        durationBadge.getStyleClass().add("student-mini-badge");

        HBox badgeRow = new HBox(8, levelBadge, lessonBadge, durationBadge);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        Label subjectMeta = new Label("Subject: " + buildDisplaySubjectName(course.getSubject(), "General"));
        subjectMeta.getStyleClass().add("my-course-meta");

        Label statusMeta = new Label("Status: " + course.getStatusLabel());
        statusMeta.getStyleClass().add("my-course-meta");

        Label readPercentMeta = new Label("Read: " + course.getProgressPercent() + "%");
        readPercentMeta.getStyleClass().add("my-course-meta");

        Label readLessonsMeta = new Label(course.getCompletedLessonCount() + "/" + course.getLessonCount() + " lessons read");
        readLessonsMeta.getStyleClass().add("my-course-meta");

        Label readingStatusBadge = new Label(resolveReadingStatusLabel(course));
        readingStatusBadge.getStyleClass().addAll("my-course-reading-badge", resolveReadingStatusStyleClass(course));

        VBox metaBox = new VBox(8, subjectMeta, statusMeta, readPercentMeta, readLessonsMeta, readingStatusBadge);
        metaBox.setMinHeight(108);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button openButton = new Button("Open Course");
        openButton.getStyleClass().addAll("primary-button", "my-course-open-button");
        openButton.setMaxWidth(Double.MAX_VALUE);
        openButton.setOnAction(event -> StudentShellController.getInstance().showCourseDetail(course));

        body.getChildren().addAll(title, badgeRow, summary, metaBox, spacer, openButton);
        card.getChildren().addAll(header, body);
        card.setOnMouseClicked(event -> StudentShellController.getInstance().showCourseDetail(course));
        return card;
    }

    private void updateStats(List<Course> courses) {
        int totalCourses = courses.size();
        long readCourses = courses.stream().filter(course -> course.getProgressPercent() > 0).count();
        long completedCourses = courses.stream().filter(this::isCourseCompleted).count();
        long remainingCourses = Math.max(0, totalCourses - completedCourses);
        int readPercent = totalCourses == 0 ? 0 : (int) Math.round((readCourses * 100.0) / totalCourses);

        coursesReadPercentLabel.setText(readPercent + "%");
        coursesReadMetaLabel.setText(readCourses + " of " + totalCourses + (totalCourses == 1 ? " course read" : " courses read"));

        completedCoursesValueLabel.setText(completedCourses + "/" + totalCourses);
        completedCoursesMetaLabel.setText(totalCourses == 1 ? "course completed" : "courses completed");

        remainingCoursesValueLabel.setText(String.valueOf(remainingCourses));
        remainingCoursesMetaLabel.setText(remainingCourses == 1 ? "course left" : "courses left");
    }

    private boolean isCourseCompleted(Course course) {
        return course.getLessonCount() > 0 && course.getProgressPercent() >= 100;
    }

    private String resolveReadingStatusLabel(Course course) {
        if (isCourseCompleted(course)) {
            return "Completed";
        }
        if (course.getProgressPercent() > 0) {
            return "In Progress";
        }
        return "Not Started";
    }

    private String resolveReadingStatusStyleClass(Course course) {
        if (isCourseCompleted(course)) {
            return "my-course-reading-completed";
        }
        if (course.getProgressPercent() > 0) {
            return "my-course-reading-progress";
        }
        return "my-course-reading-idle";
    }

    private String buildHeaderStyle(Course course) {
        int palette = Math.abs(safeText(course.getSubject()).toLowerCase(Locale.ROOT).hashCode() + course.getLevel()) % 5;
        String gradient = switch (palette) {
            case 0 -> "linear-gradient(to bottom right, #0f4c81 0%, #1f73ff 60%, #6ea8ff 100%)";
            case 1 -> "linear-gradient(to bottom right, #ff9a62 0%, #ff7b7b 55%, #ffb067 100%)";
            case 2 -> "linear-gradient(to bottom right, #7446d8 0%, #9277ff 58%, #c4b5fd 100%)";
            case 3 -> "linear-gradient(to bottom right, #0f8a7e 0%, #18b38a 58%, #71e6c1 100%)";
            default -> "linear-gradient(to bottom right, #23334f 0%, #39598a 58%, #6d89b8 100%)";
        };
        return "-fx-background-color: " + gradient + "; -fx-background-radius: 26 26 0 0;";
    }

    private String buildCourseInitials(Course course) {
        String source = buildDisplaySubjectName(course.getSubject(), "Course").trim();
        String[] parts = source.split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase(Locale.ROOT);
        }

        String clean = source.replaceAll("[^A-Za-z0-9]", "");
        if (clean.length() >= 2) {
            return clean.substring(0, 2).toUpperCase(Locale.ROOT);
        }
        return clean.isBlank() ? "ED" : clean.toUpperCase(Locale.ROOT);
    }

    private ImageView createCourseCoverView(Course course, double width, double height, double arc) {
        String imageSource = resolveCourseImageSource(course.getImage());
        if (imageSource == null) {
            return null;
        }

        Image image = new Image(imageSource, false);
        if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
            return null;
        }

        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(width);
        imageView.setFitHeight(height);
        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("course-card-cover-image");

        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(arc);
        clip.setArcHeight(arc);
        imageView.setClip(clip);
        return imageView;
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

    private String buildDisplaySubjectName(String subject, String fallback) {
        String value = safeText(subject, fallback);
        if (value.isBlank()) {
            return value;
        }

        String normalized = normalizeSubjectKey(value);
        return switch (normalized) {
            case "math", "maths", "mathematique", "mathematiques" -> "Mathematique";
            case "fr", "francais" -> "Francais";
            case "ang", "anglais", "english" -> "Anglais";
            case "ar", "arab", "arabe" -> "Arabe";
            case "info", "informatique", "computer science" -> "Informatique";
            case "sci", "science", "sciences" -> "Sciences";
            case "svt" -> "Sciences de la vie et de la Terre";
            case "hist", "histoire", "history" -> "Histoire";
            case "geo", "geographie", "geography" -> "Geographie";
            case "philo", "philosophie", "philosophy" -> "Philosophie";
            default -> toDisplayCase(value);
        };
    }

    private String toDisplayCase(String value) {
        String[] parts = safeText(value).replaceAll("\\s+", " ").trim().split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }

    private String normalizeSubjectKey(String value) {
        String normalized = Normalizer.normalize(safeText(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
    }

    private String summarize(String text, int maxLength) {
        String value = safeText(text, "Open the course to see the full learning path.");
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private String resolveStatusStyleClass(Course course) {
        if (course.isPublished()) {
            return "my-course-status-published";
        }
        if (course.isArchived()) {
            return "my-course-status-archived";
        }
        return "my-course-status-draft";
    }

    private String safeText(String value) {
        return safeText(value, "");
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
