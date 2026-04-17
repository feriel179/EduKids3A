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
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import tn.esprit.models.Course;
import tn.esprit.services.CourseService;
import tn.esprit.util.SweetAlert;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class CourseController {
    private static final int PAGE_SIZE = 3;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private Label totalCoursesMetricLabel;
    @FXML
    private Label publishedCoursesMetricLabel;
    @FXML
    private Label totalLessonsMetricLabel;
    @FXML
    private Label pageSummaryLabel;
    @FXML
    private HBox paginationBox;
    @FXML
    private TableView<Course> courseTable;
    @FXML
    private TableColumn<Course, Number> idColumn;
    @FXML
    private TableColumn<Course, String> titleColumn;
    @FXML
    private TableColumn<Course, String> subjectColumn;
    @FXML
    private TableColumn<Course, String> levelColumn;
    @FXML
    private TableColumn<Course, String> statusColumn;
    @FXML
    private TableColumn<Course, Number> lessonCountColumn;
    @FXML
    private TableColumn<Course, String> durationColumn;

    private final CourseService courseService = new CourseService();
    private final ObservableList<Course> allCourses = FXCollections.observableArrayList();
    private List<Course> filteredSortedCourses = List.of();
    private int currentPageIndex;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(data -> new SimpleLongProperty(data.getValue().getId()));
        titleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTitle()));
        subjectColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSubject()));
        levelColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getLevelText()));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatusLabel()));
        lessonCountColumn.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getLessonCount()));
        durationColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTotalDurationLabel()));

        titleColumn.setCellFactory(column -> createTextCell("table-course-title"));
        subjectColumn.setCellFactory(column -> createTextCell("table-subtle-text"));
        levelColumn.setCellFactory(column -> createLevelCell());
        statusColumn.setCellFactory(column -> createStatusCell());
        durationColumn.setCellFactory(column -> createTextCell("table-subtle-text"));

        courseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        courseTable.setPlaceholder(new Label("No courses found."));

        sortComboBox.getItems().setAll(
                "Level Ascending",
                "Level Descending",
                "Title A-Z",
                "Title Z-A"
        );
        sortComboBox.setValue("Level Ascending");

        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndSort(true));
        sortComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFiltersAndSort(true));

        refreshTable();
    }

    @FXML
    private void handleCreateCourse() {
        AdminShellController.getInstance().showCreateCourse();
    }

    @FXML
    private void handleEditCourse() {
        Course course = courseTable.getSelectionModel().getSelectedItem();
        if (course == null) {
            SweetAlert.warning("No Selection", "Please select a course to edit.");
            return;
        }
        AdminShellController.getInstance().showEditCourse(course);
    }

    @FXML
    private void handleRefreshCourses() {
        refreshTable();
    }

    @FXML
    private void handleDeleteCourse() {
        Course course = courseTable.getSelectionModel().getSelectedItem();
        if (course == null) {
            SweetAlert.warning("No Selection", "Please select a course to delete.");
            return;
        }

        boolean confirmed = SweetAlert.confirmDanger(
                "Delete Course",
                "Its lessons and EduKids progress rows will also be removed.",
                "Delete"
        );
        if (confirmed) {
            courseService.deleteCourse(course);
            refreshTable();
            SweetAlert.success("Course deleted", "The selected course and its related lessons were removed.");
        }
    }

    private void refreshTable() {
        courseService.refreshCourses();
        allCourses.setAll(courseService.getAllCourses());
        updateMetrics();
        applyFiltersAndSort(false);
    }

    private void updateMetrics() {
        totalCoursesMetricLabel.setText(String.valueOf(allCourses.size()));
        publishedCoursesMetricLabel.setText(String.valueOf(allCourses.stream().filter(Course::isPublished).count()));
        totalLessonsMetricLabel.setText(String.valueOf(allCourses.stream().mapToInt(Course::getLessonCount).sum()));
    }

    private void applyFiltersAndSort(boolean resetPage) {
        String searchValue = searchField == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);

        filteredSortedCourses = allCourses.stream()
                .filter(course -> matchesSearch(course, searchValue))
                .sorted(resolveComparator())
                .toList();

        if (resetPage) {
            currentPageIndex = 0;
        }
        updatePagination();
        updateCurrentPage();
    }

    private boolean matchesSearch(Course course, String searchValue) {
        if (searchValue.isBlank()) {
            return true;
        }
        return String.valueOf(course.getId()).contains(searchValue)
                || course.getTitle().toLowerCase(Locale.ROOT).contains(searchValue)
                || course.getSubject().toLowerCase(Locale.ROOT).contains(searchValue)
                || course.getLevelText().toLowerCase(Locale.ROOT).contains(searchValue)
                || course.getStatusLabel().toLowerCase(Locale.ROOT).contains(searchValue);
    }

    private Comparator<Course> resolveComparator() {
        String selectedSort = sortComboBox.getValue();
        if ("Level Descending".equals(selectedSort)) {
            return Comparator.comparingInt(Course::getLevel)
                    .reversed()
                    .thenComparing(Course::getTitle, String.CASE_INSENSITIVE_ORDER);
        }
        if ("Title A-Z".equals(selectedSort)) {
            return Comparator.comparing(Course::getTitle, String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(Course::getLevel);
        }
        if ("Title Z-A".equals(selectedSort)) {
            return Comparator.comparing(Course::getTitle, String.CASE_INSENSITIVE_ORDER)
                    .reversed()
                    .thenComparingInt(Course::getLevel);
        }
        return Comparator.comparingInt(Course::getLevel)
                .thenComparing(Course::getTitle, String.CASE_INSENSITIVE_ORDER);
    }

    private void updatePagination() {
        paginationBox.getChildren().clear();

        if (filteredSortedCourses.isEmpty()) {
            return;
        }

        int pageCount = Math.max(1, (int) Math.ceil((double) filteredSortedCourses.size() / PAGE_SIZE));
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

    private void updateCurrentPage() {
        int totalItems = filteredSortedCourses.size();
        int fromIndex = Math.min(currentPageIndex * PAGE_SIZE, totalItems);
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);

        if (totalItems == 0) {
            courseTable.setItems(FXCollections.observableArrayList());
            pageSummaryLabel.setText("Showing 0 of 0 entries");
            return;
        }

        courseTable.setItems(FXCollections.observableArrayList(filteredSortedCourses.subList(fromIndex, toIndex)));
        pageSummaryLabel.setText("Showing " + (fromIndex + 1) + " to " + toIndex + " of " + totalItems + " entries");
    }

    private TableCell<Course, String> createTextCell(String styleClass) {
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

    private TableCell<Course, String> createLevelCell() {
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
                        "table-badge-beginner",
                        "table-badge-intermediate",
                        "table-badge-advanced"
                );

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Course course = getTableRow() == null ? null : (Course) getTableRow().getItem();
                badge.setText(item);

                if (course != null) {
                    if (course.getLevel() <= 3) {
                        badge.getStyleClass().add("table-badge-beginner");
                    } else if (course.getLevel() <= 6) {
                        badge.getStyleClass().add("table-badge-intermediate");
                    } else {
                        badge.getStyleClass().add("table-badge-advanced");
                    }
                }

                setText(null);
                setGraphic(wrapper);
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        };
    }

    private TableCell<Course, String> createStatusCell() {
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
                        "table-badge-archived"
                );

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Course course = getTableRow() == null ? null : (Course) getTableRow().getItem();
                badge.setText(item);
                if (course != null) {
                    if (course.isPublished()) {
                        badge.getStyleClass().add("table-badge-published");
                    } else if (course.isArchived()) {
                        badge.getStyleClass().add("table-badge-archived");
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
}
