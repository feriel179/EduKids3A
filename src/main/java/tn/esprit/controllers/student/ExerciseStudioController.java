package tn.esprit.controllers.student;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import tn.esprit.models.Course;
import tn.esprit.models.Lesson;
import tn.esprit.models.LessonExercise;
import tn.esprit.models.Student;
import tn.esprit.services.ExerciseService;
import tn.esprit.services.StudentService;
import tn.esprit.util.SweetAlert;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExerciseStudioController {
    private static final double CANVAS_WIDTH = 720;
    private static final double CANVAS_HEIGHT = 320;

    @FXML
    private Label courseTitleLabel;
    @FXML
    private Label lessonTitleLabel;
    @FXML
    private Label headerSubtitleLabel;
    @FXML
    private Label studentAgeLabel;
    @FXML
    private Label selectedExerciseTitleLabel;
    @FXML
    private Label studioStatusLabel;
    @FXML
    private Label drawingPromptLabel;
    @FXML
    private HBox exerciseSelectorBox;
    @FXML
    private TextArea promptTextArea;
    @FXML
    private TextArea answerTextArea;
    @FXML
    private VBox drawingContentBox;
    @FXML
    private StackPane drawingSurfacePane;
    @FXML
    private Button clearDrawingButton;

    private final ExerciseService exerciseService = new ExerciseService();
    private final StudentService studentService = new StudentService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private final Map<LessonExercise, Button> exerciseButtons = new LinkedHashMap<>();

    private Canvas drawingCanvas;
    private GraphicsContext graphicsContext;
    private Course currentCourse;
    private Lesson currentLesson;
    private List<LessonExercise> currentExercises = List.of();
    private LessonExercise currentExercise;
    private boolean drawingEnabled;
    private double previousX;
    private double previousY;

    @FXML
    private void initialize() {
        drawingCanvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        graphicsContext = drawingCanvas.getGraphicsContext2D();
        resetCanvas();
        installDrawingHandlers();
        drawingSurfacePane.getChildren().setAll(drawingCanvas);
        StackPane.setMargin(drawingCanvas, new Insets(0));
    }

    public void setContext(Course course, Lesson lesson) {
        currentCourse = course;
        currentLesson = lesson;
        currentExercises = exerciseService.getExercisesByLesson(lesson);

        Student student = studentService.getCurrentStudent();
        drawingEnabled = isDrawingEnabled(student);
        courseTitleLabel.setText(course == null ? "Course" : course.getTitle());
        lessonTitleLabel.setText(lesson == null ? "Lesson" : lesson.getTitle());
        studentAgeLabel.setText(student == null
                ? "Age profile pending"
                : student.getAgeGroupLabel() + " | " + student.getPreferredCategory());
        configureDrawingAccess();
        updateDrawingPrompt();

        renderExerciseButtons();
        if (!currentExercises.isEmpty()) {
            selectExercise(currentExercises.get(0));
        } else {
            selectedExerciseTitleLabel.setText("No exercise available");
            studioStatusLabel.setText("This lesson has no exercise yet.");
            promptTextArea.clear();
            answerTextArea.clear();
            resetCanvas();
        }
    }

    @FXML
    private void handleBackToCourse() {
        if (StudentShellController.getInstance() != null && currentCourse != null) {
            StudentShellController.getInstance().showCourseDetail(currentCourse);
        }
    }

    @FXML
    private void handleClearDrawing() {
        if (!drawingEnabled) {
            return;
        }
        resetCanvas();
        if (currentExercise != null) {
            currentExercise.setDrawingPath("");
        }
        studioStatusLabel.setText("Drawing cleared. Save the exercise to keep the new version.");
    }

    @FXML
    private void handleSaveExercise() {
        if (currentExercise == null) {
            SweetAlert.warning("No Exercise", "Choose an exercise before saving.");
            return;
        }

        try {
            String drawingPath = drawingEnabled ? saveDrawingSnapshot(currentExercise) : "";
            exerciseService.saveStudentExerciseWork(currentExercise, promptTextArea.getText(), answerTextArea.getText(), drawingPath);
            studioStatusLabel.setText(buildStatusLabel(currentExercise));
            SweetAlert.success("Exercise Saved", drawingEnabled
                    ? "The drawing and the writing were saved."
                    : "The written answer was saved.");
        } catch (RuntimeException exception) {
            SweetAlert.error("Save Failed", exception.getMessage());
        }
    }

    private void renderExerciseButtons() {
        exerciseSelectorBox.getChildren().clear();
        exerciseButtons.clear();
        for (LessonExercise exercise : currentExercises) {
            Button button = new Button(exercise.getTitle());
            button.getStyleClass().add("student-studio-exercise-button");
            button.setOnAction(event -> selectExercise(exercise));
            exerciseButtons.put(exercise, button);
            exerciseSelectorBox.getChildren().add(button);
        }
    }

    private void selectExercise(LessonExercise exercise) {
        currentExercise = exercise;
        selectedExerciseTitleLabel.setText(exercise.getTitle());
        promptTextArea.setText(exercise.getResolvedPrompt());
        answerTextArea.setText(exercise.getAnswer());
        if (drawingEnabled) {
            loadDrawing(exercise.getDrawingPath());
        } else {
            resetCanvas();
        }
        studioStatusLabel.setText(buildStatusLabel(exercise));
        updateDrawingPrompt();
        updateExerciseButtonStyles();
    }

    private void updateExerciseButtonStyles() {
        for (Map.Entry<LessonExercise, Button> entry : exerciseButtons.entrySet()) {
            Button button = entry.getValue();
            button.getStyleClass().remove("student-studio-exercise-button-active");
            if (entry.getKey().equals(currentExercise)) {
                button.getStyleClass().add("student-studio-exercise-button-active");
            }
        }
    }

    private String buildStatusLabel(LessonExercise exercise) {
        if (exercise == null) {
            return "No exercise selected";
        }
        if (exercise.getUpdatedAt() == null) {
            return (drawingEnabled ? exercise.hasDrawing() : false) || exercise.hasAnswer()
                    ? "Draft updated"
                    : (drawingEnabled ? "Start by drawing or writing the answer." : "Start by writing the answer.");
        }
        return "Saved on " + formatter.format(exercise.getUpdatedAt());
    }

    private void configureDrawingAccess() {
        if (drawingContentBox != null) {
            drawingContentBox.setVisible(drawingEnabled);
            drawingContentBox.setManaged(drawingEnabled);
        }
        if (clearDrawingButton != null) {
            clearDrawingButton.setVisible(drawingEnabled);
            clearDrawingButton.setManaged(drawingEnabled);
        }
        if (headerSubtitleLabel != null) {
            headerSubtitleLabel.setText(drawingEnabled
                    ? "Draw and write in one place for the selected lesson."
                    : "Write and save the answer for the selected lesson.");
        }
    }

    private void updateDrawingPrompt() {
        if (drawingPromptLabel == null) {
            return;
        }

        LessonExercise drawingExercise = findDrawingExercise();
        if (!drawingEnabled) {
            drawingPromptLabel.setText("");
            return;
        }

        drawingPromptLabel.setText(drawingExercise == null
                ? "Use the drawing area to represent the lesson, then explain your drawing."
                : drawingExercise.getResolvedPrompt());
    }

    private void installDrawingHandlers() {
        drawingCanvas.setOnMousePressed(event -> {
            previousX = event.getX();
            previousY = event.getY();
            graphicsContext.beginPath();
            graphicsContext.moveTo(previousX, previousY);
            graphicsContext.stroke();
        });

        drawingCanvas.setOnMouseDragged(event -> {
            graphicsContext.setStroke(Color.web("#173258"));
            graphicsContext.setLineWidth(3.0);
            graphicsContext.strokeLine(previousX, previousY, event.getX(), event.getY());
            previousX = event.getX();
            previousY = event.getY();
        });
    }

    private void resetCanvas() {
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        graphicsContext.setStroke(Color.web("#173258"));
        graphicsContext.setLineWidth(3.0);
    }

    private void loadDrawing(String drawingPath) {
        resetCanvas();
        if (drawingPath == null || drawingPath.isBlank()) {
            return;
        }

        Path path = Path.of(drawingPath);
        if (!Files.exists(path)) {
            return;
        }

        Image image = new Image(path.toUri().toString(), false);
        if (!image.isError()) {
            graphicsContext.drawImage(image, 0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        }
    }

    private String saveDrawingSnapshot(LessonExercise exercise) {
        if (exercise == null) {
            return "";
        }

        try {
            Student student = studentService.getCurrentStudent();
            long studentId = student == null ? 0 : student.getId();
            Path outputDirectory = Path.of(System.getProperty("user.dir"), "generated", "exercise-drawings");
            Files.createDirectories(outputDirectory);

            String fileName = "student-" + studentId + "-exercise-" + exercise.getId() + ".png";
            Path outputPath = outputDirectory.resolve(fileName);

            WritableImage writableImage = new WritableImage((int) CANVAS_WIDTH, (int) CANVAS_HEIGHT);
            drawingCanvas.snapshot(new SnapshotParameters(), writableImage);
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);
            ImageIO.write(bufferedImage, "png", outputPath.toFile());
            return outputPath.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save the exercise drawing.", exception);
        }
    }

    private boolean isDrawingEnabled(Student student) {
        if (student == null) {
            return false;
        }
        int age = student.getAge();
        return age >= 8 && age <= 10;
    }

    private LessonExercise findDrawingExercise() {
        for (LessonExercise exercise : currentExercises) {
            String title = exercise.getTitle() == null ? "" : exercise.getTitle().toLowerCase();
            String prompt = exercise.getResolvedPrompt() == null ? "" : exercise.getResolvedPrompt().toLowerCase();
            if (title.contains("draw") || title.contains("dessin")
                    || prompt.contains("draw") || prompt.contains("dessin")
                    || prompt.contains("drawing area")) {
                return exercise;
            }
        }
        return currentExercise;
    }
}
