package tn.esprit.controllers.student;

import tn.esprit.models.Course;
import tn.esprit.models.Student;
import tn.esprit.services.CourseService;
import tn.esprit.services.StudentService;
import tn.esprit.util.SweetAlert;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class CatalogController {
    private static final int PAGE_SIZE = 6;
    private static final double COURSE_CARD_WIDTH = 248;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> subjectFilterComboBox;
    @FXML
    private ComboBox<String> levelFilterComboBox;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private TilePane catalogContainer;
    @FXML
    private HBox paginationBox;
    @FXML
    private Label resultCountLabel;
    @FXML
    private Label paginationSummaryLabel;
    @FXML
    private Label totalCoursesValueLabel;
    @FXML
    private Label subjectsValueLabel;
    @FXML
    private Label savedCoursesValueLabel;
    @FXML
    private Label heroStudentChipLabel;
    @FXML
    private Label heroSavedCoursesChipLabel;
    @FXML
    private Label studentSnapshotNameLabel;
    @FXML
    private Label studentSnapshotEmailLabel;
    @FXML
    private Label featuredCourseLabel;

    private final CourseService courseService = new CourseService();
    private final StudentService studentService = new StudentService();

    private List<Course> filteredCourses = List.of();
    private Set<Long> enrolledCourseIds = Set.of();
    private Student currentStudent;
    private int currentPage = 1;

    @FXML
    private void initialize() {
        courseService.refreshCourses();
        configureFilters();
        refreshStudentSnapshot();
        applyFilters();
    }

    @FXML
    private void applyFilters() {
        String keyword = safeText(searchField.getText()).toLowerCase(Locale.ROOT);
        String subject = subjectFilterComboBox.getValue();
        String level = levelFilterComboBox.getValue();
        Comparator<Course> comparator = resolveComparator(sortComboBox.getValue());

        filteredCourses = courseService.getPublishedCourses().stream()
                .filter(course -> matchesKeyword(course, keyword))
                .filter(course -> subject == null
                        || subject.equals("All Subjects")
                        || buildDisplaySubjectName(course.getSubject()).equalsIgnoreCase(subject))
                .filter(course -> level == null
                        || level.equals("All Levels")
                        || safeText(course.getLevelText()).equalsIgnoreCase(level))
                .sorted(comparator)
                .toList();

        int totalPages = getTotalPages();
        if (totalPages == 0) {
            currentPage = 1;
        } else if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        updateMetrics();
        updateFeaturedCourse();
        renderCurrentPage();
        rebuildPagination();
        updateResultLabels();
    }

    @FXML
    private void handleClearFilters() {
        searchField.clear();
        subjectFilterComboBox.setValue("All Subjects");
        levelFilterComboBox.setValue("All Levels");
        sortComboBox.setValue("Newest");
        currentPage = 1;
        applyFilters();
    }

    @FXML
    private void openMyCoursesPage() {
        if (StudentShellController.getInstance() != null) {
            StudentShellController.getInstance().showMyCourses();
        }
    }

    private void configureFilters() {
        Set<String> subjects = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> levels = new TreeSet<>(Comparator.comparingInt(this::extractLevelNumber));

        courseService.getPublishedCourses().forEach(course -> {
            subjects.add(buildDisplaySubjectName(course.getSubject(), "Unknown Subject"));
            levels.add(safeText(course.getLevelText(), "Unknown Level"));
        });

        subjectFilterComboBox.getItems().setAll("All Subjects");
        subjectFilterComboBox.getItems().addAll(subjects);
        subjectFilterComboBox.setValue("All Subjects");

        levelFilterComboBox.getItems().setAll("All Levels");
        levelFilterComboBox.getItems().addAll(levels);
        levelFilterComboBox.setValue("All Levels");

        sortComboBox.getItems().setAll("Newest", "Most Liked", "A to Z", "Level Ascending", "Level Descending");
        sortComboBox.setValue("Newest");

        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            currentPage = 1;
            applyFilters();
        });
        subjectFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            currentPage = 1;
            applyFilters();
        });
        levelFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            currentPage = 1;
            applyFilters();
        });
        sortComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            currentPage = 1;
            applyFilters();
        });
    }

    private void renderCurrentPage() {
        catalogContainer.getChildren().clear();
        if (filteredCourses.isEmpty()) {
            Label empty = new Label("No courses match your filters yet. Try another search or reset the filters.");
            empty.getStyleClass().add("empty-state");
            empty.setWrapText(true);
            empty.setMaxWidth(420);
            catalogContainer.getChildren().add(empty);
            return;
        }

        int startIndex = (currentPage - 1) * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, filteredCourses.size());
        for (Course course : filteredCourses.subList(startIndex, endIndex)) {
            catalogContainer.getChildren().add(createCourseCard(course));
        }
    }

    private VBox createCourseCard(Course course) {
        boolean enrolled = enrolledCourseIds.contains(course.getId());

        VBox card = new VBox(0);
        card.getStyleClass().addAll("card", "course-card", "student-course-card");
        card.setPrefWidth(COURSE_CARD_WIDTH);
        card.setMaxWidth(COURSE_CARD_WIDTH);

        StackPane media = new StackPane();
        media.getStyleClass().add("student-course-media");
        media.setPrefHeight(156);
        media.setStyle(buildMediaStyle(course));

        ImageView coverImageView = createCourseCoverView(course, COURSE_CARD_WIDTH, 156, 28);
        if (coverImageView != null) {
            Region coverOverlay = new Region();
            coverOverlay.getStyleClass().add("course-card-cover-overlay");
            coverOverlay.setPrefSize(COURSE_CARD_WIDTH, 156);
            media.getChildren().addAll(coverImageView, coverOverlay);
        }

        String displaySubject = buildDisplaySubjectName(course.getSubject(), "General");

        Label subjectChip = new Label(displaySubject);
        subjectChip.getStyleClass().add("student-course-chip");
        subjectChip.setWrapText(true);
        subjectChip.setMaxWidth(COURSE_CARD_WIDTH - 108);

        Label likesChip = new Label(course.getLikes() + " likes");
        likesChip.getStyleClass().add("student-course-chip-muted");

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);
        HBox topRow = new HBox(8, subjectChip, topSpacer, likesChip);
        topRow.setAlignment(Pos.TOP_LEFT);

        Label kicker = new Label(course.getLevelText());
        kicker.getStyleClass().add("student-course-media-kicker");

        Label initials = new Label(buildCourseInitials(course));
        initials.getStyleClass().add("student-course-media-initials");
        initials.setVisible(coverImageView == null);
        initials.setManaged(coverImageView == null);

        Region mediaSpacer = new Region();
        VBox.setVgrow(mediaSpacer, Priority.ALWAYS);

        VBox mediaContent = new VBox(12, topRow, mediaSpacer, kicker, initials);
        mediaContent.setPadding(new Insets(16));
        StackPane.setAlignment(mediaContent, Pos.TOP_LEFT);
        media.getChildren().add(mediaContent);

        VBox body = new VBox(10);
        body.setPadding(new Insets(18));

        Label badge = new Label(course.getLevelText());
        badge.getStyleClass().add("level-badge");
        badge.setStyle("-fx-background-color: " + course.getLevelColor() + ";");

        Label idBadge = new Label("Course #" + course.getId());
        idBadge.getStyleClass().add("student-mini-badge");

        HBox badgeRow = new HBox(8, badge, idBadge);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(safeText(course.getTitle(), "Untitled course"));
        title.getStyleClass().add("course-card-title");
        title.setWrapText(true);

        Label description = new Label(summarize(course.getDescription(), 96));
        description.getStyleClass().add("student-course-description");
        description.setWrapText(true);

        Label subjectMeta = new Label("Subject: " + displaySubject);
        subjectMeta.getStyleClass().add("student-course-meta");
        subjectMeta.setWrapText(true);

        Label reactions = new Label("Dislikes: " + course.getDislikes());
        reactions.getStyleClass().add("student-course-meta");

        VBox metaBox = new VBox(4, subjectMeta, reactions);
        metaBox.setAlignment(Pos.CENTER_LEFT);

        Button detailsButton = new Button("View Details");
        detailsButton.getStyleClass().add("secondary-button");
        detailsButton.setOnAction(event -> StudentShellController.getInstance().showCourseDetail(course));

        Button enrollButton = new Button(enrolled ? "Saved" : "Add to My List");
        enrollButton.getStyleClass().add(enrolled ? "ghost-button" : "primary-button");
        enrollButton.setDisable(enrolled);
        enrollButton.setOnAction(event -> handleEnroll(course));

        HBox actions = new HBox(10, detailsButton, enrollButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(detailsButton, Priority.ALWAYS);
        HBox.setHgrow(enrollButton, Priority.ALWAYS);
        detailsButton.setMaxWidth(Double.MAX_VALUE);
        enrollButton.setMaxWidth(Double.MAX_VALUE);

        body.getChildren().addAll(badgeRow, title, description, metaBox, actions);
        card.getChildren().addAll(media, body);
        return card;
    }

    private void handleEnroll(Course course) {
        boolean enrolled = studentService.enrollInCourse(course);
        refreshStudentSnapshot();
        applyFilters();

        if (enrolled) {
            SweetAlert.success("Added", course.getTitle() + " was added to your list.");
        } else {
            SweetAlert.warning("Already Enrolled", "This course is already in your list.");
        }
    }

    private void refreshStudentSnapshot() {
        currentStudent = studentService.getCurrentStudent();
        if (currentStudent == null) {
            enrolledCourseIds = Set.of();
            heroStudentChipLabel.setText("Welcome, Student");
            heroSavedCoursesChipLabel.setText("0 saved courses");
            studentSnapshotNameLabel.setText("No student loaded");
            studentSnapshotEmailLabel.setText("student@edukids.local");
            return;
        }

        enrolledCourseIds = currentStudent.getEnrolledCourses().stream()
                .map(Course::getId)
                .collect(Collectors.toSet());

        heroStudentChipLabel.setText("Welcome, " + extractFirstName(currentStudent.getName()));
        heroSavedCoursesChipLabel.setText(enrolledCourseIds.size() + " saved courses");
        studentSnapshotNameLabel.setText(currentStudent.getName());
        studentSnapshotEmailLabel.setText(currentStudent.getEmail());
    }

    private void updateMetrics() {
        List<Course> allCourses = courseService.getPublishedCourses();
        totalCoursesValueLabel.setText(String.valueOf(allCourses.size()));
        subjectsValueLabel.setText(String.valueOf(allCourses.stream()
                .map(course -> buildDisplaySubjectName(course.getSubject()))
                .filter(subject -> !subject.isBlank())
                .collect(Collectors.toSet())
                .size()));
        savedCoursesValueLabel.setText(String.valueOf(enrolledCourseIds.size()));
    }

    private void updateFeaturedCourse() {
        Course featured = filteredCourses.stream()
                .max(Comparator.comparingInt(Course::getLikes).thenComparingLong(Course::getId))
                .orElse(null);
        featuredCourseLabel.setText(featured == null
                ? "No recommendation yet. Start by choosing a subject."
                : featured.getTitle());
    }

    private void rebuildPagination() {
        paginationBox.getChildren().clear();
        int totalPages = getTotalPages();
        if (totalPages == 0) {
            return;
        }

        Button previousButton = createPaginationButton("<", false);
        previousButton.setDisable(currentPage == 1);
        previousButton.setOnAction(event -> {
            if (currentPage > 1) {
                currentPage--;
                renderCurrentPage();
                rebuildPagination();
                updateResultLabels();
            }
        });
        paginationBox.getChildren().add(previousButton);

        for (int page = 1; page <= totalPages; page++) {
            final int pageNumber = page;
            Button pageButton = createPaginationButton(String.valueOf(page), page == currentPage);
            pageButton.setOnAction(event -> {
                currentPage = pageNumber;
                renderCurrentPage();
                rebuildPagination();
                updateResultLabels();
            });
            paginationBox.getChildren().add(pageButton);
        }

        Button nextButton = createPaginationButton(">", false);
        nextButton.setDisable(currentPage == totalPages);
        nextButton.setOnAction(event -> {
            if (currentPage < totalPages) {
                currentPage++;
                renderCurrentPage();
                rebuildPagination();
                updateResultLabels();
            }
        });
        paginationBox.getChildren().add(nextButton);
    }

    private void updateResultLabels() {
        if (filteredCourses.isEmpty()) {
            resultCountLabel.setText("Showing 0 courses");
            paginationSummaryLabel.setText("No pages available");
            return;
        }

        int start = (currentPage - 1) * PAGE_SIZE + 1;
        int end = Math.min(currentPage * PAGE_SIZE, filteredCourses.size());
        resultCountLabel.setText("Showing " + start + "-" + end + " of " + filteredCourses.size() + " courses");
        paginationSummaryLabel.setText("Page " + currentPage + " of " + getTotalPages());
    }

    private Comparator<Course> resolveComparator(String selectedSort) {
        String sort = selectedSort == null ? "Newest" : selectedSort;
        return switch (sort) {
            case "Most Liked" -> Comparator.comparingInt(Course::getLikes)
                    .reversed()
                    .thenComparing(Comparator.comparingLong(Course::getId).reversed());
            case "A to Z" -> Comparator.comparing(course -> safeText(course.getTitle()), String.CASE_INSENSITIVE_ORDER);
            case "Level Ascending" -> Comparator.comparingInt(Course::getLevel)
                    .thenComparing(course -> safeText(course.getTitle()), String.CASE_INSENSITIVE_ORDER);
            case "Level Descending" -> Comparator.comparingInt(Course::getLevel)
                    .reversed()
                    .thenComparing(course -> safeText(course.getTitle()), String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparingLong(Course::getId).reversed();
        };
    }

    private boolean matchesKeyword(Course course, String keyword) {
        if (keyword.isBlank()) {
            return true;
        }
        return safeText(course.getTitle()).toLowerCase(Locale.ROOT).contains(keyword)
                || buildDisplaySubjectName(course.getSubject()).toLowerCase(Locale.ROOT).contains(keyword)
                || safeText(course.getSubject()).toLowerCase(Locale.ROOT).contains(keyword)
                || safeText(course.getDescription()).toLowerCase(Locale.ROOT).contains(keyword);
    }

    private Button createPaginationButton(String text, boolean active) {
        Button button = new Button(text);
        button.getStyleClass().add("pagination-button");
        button.getStyleClass().add(active ? "pagination-button-active" : "pagination-button-idle");
        return button;
    }

    private int getTotalPages() {
        if (filteredCourses.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil((double) filteredCourses.size() / PAGE_SIZE);
    }

    private int extractLevelNumber(String label) {
        String digits = label == null ? "" : label.replaceAll("\\D+", "");
        return digits.isBlank() ? Integer.MAX_VALUE : Integer.parseInt(digits);
    }

    private String buildMediaStyle(Course course) {
        int palette = Math.abs(safeText(course.getSubject()).toLowerCase(Locale.ROOT).hashCode() + course.getLevel()) % 5;
        String gradient = switch (palette) {
            case 0 -> "linear-gradient(to bottom right, #0f4c81 0%, #1f73ff 60%, #6ea8ff 100%)";
            case 1 -> "linear-gradient(to bottom right, #ff9a62 0%, #ff7b7b 55%, #ffb067 100%)";
            case 2 -> "linear-gradient(to bottom right, #7446d8 0%, #9277ff 58%, #c4b5fd 100%)";
            case 3 -> "linear-gradient(to bottom right, #0f8a7e 0%, #18b38a 58%, #71e6c1 100%)";
            default -> "linear-gradient(to bottom right, #23334f 0%, #39598a 58%, #6d89b8 100%)";
        };
        return "-fx-background-color: " + gradient + ";"
                + "-fx-background-radius: 24 24 0 0;";
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

    private String buildDisplaySubjectName(String subject) {
        return buildDisplaySubjectName(subject, "");
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
        String value = safeText(text, "A fresh EduKids course is ready to explore.");
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private String extractFirstName(String value) {
        String text = safeText(value, "Student").trim();
        int firstSpace = text.indexOf(' ');
        return firstSpace > 0 ? text.substring(0, firstSpace) : text;
    }

    private String safeText(String value) {
        return safeText(value, "");
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
