package tn.esprit.controllers.admin;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import tn.esprit.models.Course;
import tn.esprit.models.Lesson;
import tn.esprit.services.CourseService;
import tn.esprit.services.LessonService;
import tn.esprit.util.SweetAlert;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class LessonController {
    private static final int PAGE_SIZE = 3;

    @FXML
    private ComboBox<Course> courseComboBox;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private Label totalLessonsMetricLabel;
    @FXML
    private Label publishedLessonsMetricLabel;
    @FXML
    private Label totalDurationMetricLabel;
    @FXML
    private Label pageSummaryLabel;
    @FXML
    private HBox paginationBox;
    @FXML
    private TableView<Lesson> lessonTable;
    @FXML
    private TableColumn<Lesson, Number> idColumn;
    @FXML
    private TableColumn<Lesson, Number> orderColumn;
    @FXML
    private TableColumn<Lesson, String> titleColumn;
    @FXML
    private TableColumn<Lesson, String> courseColumn;
    @FXML
    private TableColumn<Lesson, String> statusColumn;
    @FXML
    private TableColumn<Lesson, String> durationColumn;
    @FXML
    private TableColumn<Lesson, String> mediaTypeColumn;
    @FXML
    private TableColumn<Lesson, String> urlColumn;
    @FXML
    private Button createButton;

    private final CourseService courseService = new CourseService();
    private final LessonService lessonService = new LessonService();
    private final ObservableList<Lesson> allLessons = FXCollections.observableArrayList();
    private List<Lesson> filteredSortedLessons = List.of();
    private int currentPageIndex;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()));
        orderColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getOrder()));
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        courseColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getCourse() == null ? "" : data.getValue().getCourse().getTitle()
        ));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusLabel()));
        durationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDurationLabel()));
        mediaTypeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDisplayMediaType()));
        urlColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUrlSummary()));

        titleColumn.setCellFactory(column -> createTextCell("table-course-title"));
        courseColumn.setCellFactory(column -> createTextCell("table-subtle-text"));
        statusColumn.setCellFactory(column -> createStatusCell());
        durationColumn.setCellFactory(column -> createTextCell("table-subtle-text"));
        mediaTypeColumn.setCellFactory(column -> createMediaCell());
        urlColumn.setCellFactory(column -> createUrlCell());

        lessonTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        lessonTable.setPlaceholder(new Label("No lessons found."));

        sortComboBox.getItems().setAll(
                "Order Ascending",
                "Order Descending",
                "Title A-Z",
                "Title Z-A"
        );
        sortComboBox.setValue("Order Ascending");

        courseComboBox.setPromptText("All Courses");
        courseComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndSort(true));
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndSort(true));
        sortComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndSort(true));

        loadCoursesPreservingSelection(courseComboBox.getValue());
        refreshTable();
    }

    @FXML
    private void handleCreateLesson() {
        AdminModuleNavigator.showCreateLesson(courseComboBox.getValue());
    }

    @FXML
    private void handleEditLesson() {
        Lesson lesson = lessonTable.getSelectionModel().getSelectedItem();
        if (lesson == null) {
            SweetAlert.warning("No Selection", "Please select a lesson to edit.");
            return;
        }
        AdminModuleNavigator.showEditLesson(lesson);
    }

    @FXML
    private void handleDeleteLesson() {
        Lesson lesson = lessonTable.getSelectionModel().getSelectedItem();
        if (lesson == null) {
            SweetAlert.warning("No Selection", "Please select a lesson to delete.");
            return;
        }

        boolean confirmed = SweetAlert.confirmDanger(
                "Delete Lesson",
                "This lesson will be removed from the selected EduKids course.",
                "Delete"
        );
        if (confirmed) {
            lessonService.deleteLesson(lesson);
            refreshTable();
            SweetAlert.success("Lesson deleted", "The selected lesson was removed successfully.");
        }
    }

    @FXML
    private void handleRefreshLessons() {
        loadCoursesPreservingSelection(courseComboBox.getValue());
        refreshTable();
    }

    private void refreshTable() {
        try {
            allLessons.setAll(lessonService.getAllLessons());
            updateMetrics();
            applyFiltersAndSort(false);
        } catch (RuntimeException exception) {
            allLessons.clear();
            filteredSortedLessons = List.of();
            updateMetrics();
            updatePagination();
            updateCurrentPage();
            SweetAlert.warning(
                    "Lessons unavailable",
                    "The lesson list could not be loaded right now.\n" + safeMessage(exception)
            );
        }
    }

    private void updateMetrics() {
        totalLessonsMetricLabel.setText(String.valueOf(allLessons.size()));
        publishedLessonsMetricLabel.setText(String.valueOf(allLessons.stream().filter(Lesson::isPublished).count()));
        totalDurationMetricLabel.setText(Course.formatDuration(allLessons.stream().mapToInt(Lesson::getDurationMinutes).sum()));
    }

    private void loadCoursesPreservingSelection(Course preferredCourse) {
        Long preferredId = preferredCourse == null ? null : preferredCourse.getId();
        try {
            courseService.refreshCourses();
            courseComboBox.setItems(courseService.getAllCourses());

            if (preferredId != null) {
                courseComboBox.getItems().stream()
                        .filter(course -> course.getId() == preferredId)
                        .findFirst()
                        .ifPresentOrElse(
                                course -> courseComboBox.getSelectionModel().select(course),
                                () -> courseComboBox.getSelectionModel().clearSelection()
                        );
            }
        } catch (RuntimeException exception) {
            courseComboBox.setItems(FXCollections.observableArrayList());
            courseComboBox.getSelectionModel().clearSelection();
            courseComboBox.setPromptText("Courses unavailable");
        }
    }

    private void applyFiltersAndSort(boolean resetPage) {
        Course selectedCourse = courseComboBox.getValue();
        String searchValue = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        filteredSortedLessons = allLessons.stream()
                .filter(lesson -> matchesCourseFilter(lesson, selectedCourse))
                .filter(lesson -> matchesSearch(lesson, searchValue))
                .sorted(resolveComparator())
                .toList();

        if (resetPage) {
            currentPageIndex = 0;
        }
        updatePagination();
        updateCurrentPage();
    }

    private boolean matchesCourseFilter(Lesson lesson, Course selectedCourse) {
        return selectedCourse == null
                || (lesson.getCourse() != null && lesson.getCourse().getId() == selectedCourse.getId());
    }

    private boolean matchesSearch(Lesson lesson, String searchValue) {
        if (searchValue.isBlank()) {
            return true;
        }

        String courseTitle = lesson.getCourse() == null ? "" : lesson.getCourse().getTitle().toLowerCase(Locale.ROOT);
        return String.valueOf(lesson.getId()).contains(searchValue)
                || lesson.getTitle().toLowerCase(Locale.ROOT).contains(searchValue)
                || lesson.getStatusLabel().toLowerCase(Locale.ROOT).contains(searchValue)
                || lesson.getDisplayMediaType().toLowerCase(Locale.ROOT).contains(searchValue)
                || courseTitle.contains(searchValue);
    }

    private Comparator<Lesson> resolveComparator() {
        String selectedSort = sortComboBox.getValue();
        if ("Order Descending".equals(selectedSort)) {
            return Comparator.comparingInt(Lesson::getOrder)
                    .reversed()
                    .thenComparing(Lesson::getTitle, String.CASE_INSENSITIVE_ORDER);
        }
        if ("Title A-Z".equals(selectedSort)) {
            return Comparator.comparing(Lesson::getTitle, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(Lesson::getOrder);
        }
        if ("Title Z-A".equals(selectedSort)) {
            return Comparator.comparing(Lesson::getTitle, String.CASE_INSENSITIVE_ORDER)
                    .reversed()
                    .thenComparingInt(Lesson::getOrder);
        }
        return Comparator.comparingInt(Lesson::getOrder)
                .thenComparing(Lesson::getTitle, String.CASE_INSENSITIVE_ORDER);
    }

    private void updatePagination() {
        paginationBox.getChildren().clear();

        if (filteredSortedLessons.isEmpty()) {
            return;
        }

        int pageCount = Math.max(1, (int) Math.ceil((double) filteredSortedLessons.size() / PAGE_SIZE));
        if (currentPageIndex >= pageCount) {
            currentPageIndex = pageCount - 1;
        }

        Button previousButton = createPageButton("<", currentPageIndex > 0, () -> {
            currentPageIndex--;
            updateCurrentPage();
            updatePagination();
        });
        paginationBox.getChildren().add(previousButton);

        for (int i = 0; i < pageCount; i++) {
            int pageIndex = i;
            boolean active = pageIndex == currentPageIndex;
            Button pageButton = createPageButton(String.valueOf(i + 1), true, () -> {
                currentPageIndex = pageIndex;
                updateCurrentPage();
                updatePagination();
            });
            pageButton.getStyleClass().add(active ? "pagination-button-active" : "pagination-button-idle");
            paginationBox.getChildren().add(pageButton);
        }

        Button nextButton = createPageButton(">", currentPageIndex < pageCount - 1, () -> {
            currentPageIndex++;
            updateCurrentPage();
            updatePagination();
        });
        paginationBox.getChildren().add(nextButton);
    }

    private Button createPageButton(String text, boolean enabled, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("pagination-button");
        button.setDisable(!enabled);
        if (enabled) {
            button.setOnAction(event -> action.run());
        }
        return button;
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error.";
        }
        String message = throwable.getMessage();
        if (message != null && !message.isBlank()) {
            return message;
        }
        Throwable cause = throwable.getCause();
        if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
            return cause.getMessage();
        }
        return throwable.getClass().getSimpleName();
    }

    private void updateCurrentPage() {
        int totalItems = filteredSortedLessons.size();
        int fromIndex = Math.min(currentPageIndex * PAGE_SIZE, totalItems);
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);

        if (totalItems == 0) {
            lessonTable.setItems(FXCollections.observableArrayList());
            pageSummaryLabel.setText("Showing 0 of 0 entries");
            return;
        }

        lessonTable.setItems(FXCollections.observableArrayList(filteredSortedLessons.subList(fromIndex, toIndex)));
        pageSummaryLabel.setText("Showing " + (fromIndex + 1) + " to " + toIndex + " of " + totalItems + " entries");
    }

    private TableCell<Lesson, String> createTextCell(String styleClass) {
        return new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove(styleClass);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                setText(item);
                setGraphic(null);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
                if (!getStyleClass().contains(styleClass)) {
                    getStyleClass().add(styleClass);
                }
            }
        };
    }

    private TableCell<Lesson, String> createMediaCell() {
        return new TableCell<>() {
            private final Label badge = new Label();
            private final StackPane wrapper = new StackPane(badge);

            {
                wrapper.getStyleClass().add("table-badge-wrap");
                badge.getStyleClass().addAll("table-badge", "table-badge-blue");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                badge.setText(item);
                setText(null);
                setGraphic(wrapper);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        };
    }

    private TableCell<Lesson, String> createUrlCell() {
        return new TableCell<>() {
            private final Tooltip tooltip = new Tooltip();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("table-link-text");

                if (empty || item == null || item.isBlank()) {
                    setText(null);
                    setTooltip(null);
                    return;
                }

                setText(shorten(item, 72));
                tooltip.setText(item);
                setTooltip(tooltip);
                if (!getStyleClass().contains("table-link-text")) {
                    getStyleClass().add("table-link-text");
                }
            }
        };
    }

    private TableCell<Lesson, String> createStatusCell() {
        return new TableCell<>() {
            private final Label badge = new Label();
            private final StackPane wrapper = new StackPane(badge);

            {
                wrapper.getStyleClass().add("table-badge-wrap");
                badge.getStyleClass().add("table-badge");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                badge.getStyleClass().removeAll(
                        "table-badge-draft",
                        "table-badge-published",
                        "table-badge-hidden"
                );

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Lesson lesson = getTableRow() == null ? null : (Lesson) getTableRow().getItem();
                badge.setText(item);
                if (lesson != null) {
                    if (lesson.isPublished()) {
                        badge.getStyleClass().add("table-badge-published");
                    } else if ("HIDDEN".equals(lesson.getStatus())) {
                        badge.getStyleClass().add("table-badge-hidden");
                    } else {
                        badge.getStyleClass().add("table-badge-draft");
                    }
                }

                setText(null);
                setGraphic(wrapper);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        };
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String shorten(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }
}
