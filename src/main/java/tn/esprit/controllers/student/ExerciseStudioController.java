package tn.esprit.controllers.student;

import javafx.embed.swing.SwingFXUtils;
import javafx.concurrent.Task;
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
import tn.esprit.services.LocalAiContentService;
import tn.esprit.services.StudentService;
import tn.esprit.util.SweetAlert;
import tn.esprit.util.StudentAiSupportDialogs;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExerciseStudioController {
    private static final double CANVAS_WIDTH = 720;
    private static final double CANVAS_HEIGHT = 320;
    private static final int REVIEW_IMAGE_WIDTH = 448;
    private static final int REVIEW_IMAGE_HEIGHT = 200;

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
    @FXML
    private Button askAiButton;
    @FXML
    private Button reviewAnswerButton;

    private final ExerciseService exerciseService = new ExerciseService();
    private final StudentService studentService = new StudentService();
    private final LocalAiContentService localAiContentService = new LocalAiContentService();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private final Map<LessonExercise, Button> exerciseButtons = new LinkedHashMap<>();

    private Canvas drawingCanvas;
    private GraphicsContext graphicsContext;
    private Course currentCourse;
    private Lesson currentLesson;
    private List<LessonExercise> currentExercises = List.of();
    private LessonExercise currentExercise;
    private boolean drawingEnabled;
    private boolean canvasHasVisibleDrawing;
    private double previousX;
    private double previousY;
    private long exerciseLoadSequence;

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
        Student student = studentService.getCurrentStudent();
        drawingEnabled = isDrawingEnabled(student);
        courseTitleLabel.setText(course == null ? "Course" : course.getTitle());
        lessonTitleLabel.setText(lesson == null ? "Lesson" : lesson.getTitle());
        studentAgeLabel.setText(student == null
                ? "Age profile pending"
                : student.getAgeGroupLabel() + " | " + student.getPreferredCategory());
        configureDrawingAccess();
        updateDrawingPrompt();
        showLoadingState();
        loadExercisesAsync(lesson);
    }

    private void loadExercisesAsync(Lesson lesson) {
        long loadToken = ++exerciseLoadSequence;
        Task<List<LessonExercise>> loadTask = new Task<>() {
            @Override
            protected List<LessonExercise> call() {
                return new ExerciseService().getExercisesByLesson(lesson);
            }
        };

        loadTask.setOnSucceeded(event -> {
            if (loadToken != exerciseLoadSequence) {
                return;
            }

            currentExercises = loadTask.getValue();
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
        });

        loadTask.setOnFailed(event -> {
            if (loadToken != exerciseLoadSequence) {
                return;
            }

            currentExercises = List.of();
            exerciseSelectorBox.getChildren().clear();
            selectedExerciseTitleLabel.setText("Exercise loading failed");
            studioStatusLabel.setText("Unable to load the exercises right now.");
            promptTextArea.clear();
            answerTextArea.clear();
            resetCanvas();
            drawingPromptLabel.setText("Open the studio again after the exercises finish loading.");
        });

        Thread loaderThread = new Thread(loadTask, "exercise-studio-loader");
        loaderThread.setDaemon(true);
        loaderThread.start();
    }

    private void showLoadingState() {
        currentExercises = List.of();
        currentExercise = null;
        exerciseSelectorBox.getChildren().clear();
        selectedExerciseTitleLabel.setText("Loading exercises...");
        studioStatusLabel.setText("Opening the studio and preparing the exercises...");
        promptTextArea.clear();
        answerTextArea.clear();
        resetCanvas();
        if (drawingPromptLabel != null) {
            drawingPromptLabel.setText("The drawing area is ready. The exercise prompt is loading...");
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

    @FXML
    private void handleAskAi() {
        if (currentLesson == null || currentCourse == null) {
            SweetAlert.warning("AI Tutor", "Open a lesson before asking the AI tutor.");
            return;
        }

        String helperText = currentExercise == null
                ? "Ask a question about this lesson. Example: explique-moi cette lecon plus simplement."
                : "Ask a question about this lesson or the current exercise. Example: explique-moi cette lecon plus simplement.";
        String defaultQuestion = currentExercise == null
                ? "Explique-moi cette lecon plus simplement."
                : "Aide-moi a mieux comprendre cet exercice et la lecon.";

        var question = StudentAiSupportDialogs.promptQuestion("Ask AI", helperText, defaultQuestion);
        if (question.isEmpty()) {
            return;
        }
        if (question.get().isBlank()) {
            SweetAlert.warning("AI Tutor", "Write a question before asking the AI tutor.");
            return;
        }

        if (askAiButton != null) {
            askAiButton.setDisable(true);
        }

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
                        currentLesson.getTitle(),
                        buildTutorResourceSummary(),
                        studentAge,
                        question.get()
                );
            }
        };

        tutorTask.setOnSucceeded(event -> {
            if (askAiButton != null) {
                askAiButton.setDisable(false);
            }
            StudentAiSupportDialogs.showTutorAnswer("AI Tutor - " + currentLesson.getTitle(), tutorTask.getValue());
        });

        tutorTask.setOnFailed(event -> {
            if (askAiButton != null) {
                askAiButton.setDisable(false);
            }
            String message = tutorTask.getException() == null ? "The AI tutor is unavailable right now." : tutorTask.getException().getMessage();
            SweetAlert.error("AI Tutor", message);
        });

        Thread worker = new Thread(tutorTask, "exercise-ai-tutor");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void handleReviewAnswer() {
        if (currentExercise == null || currentLesson == null || currentCourse == null) {
            SweetAlert.warning("AI Review", "Choose an exercise before asking for AI feedback.");
            return;
        }

        boolean hasWrittenAnswer = answerTextArea != null && answerTextArea.getText() != null && !answerTextArea.getText().isBlank();
        String reviewPrompt = promptTextArea == null ? currentExercise.getResolvedPrompt() : promptTextArea.getText();
        String drawingImagePath;
        try {
            drawingImagePath = resolveDrawingForReview(currentExercise);
        } catch (IllegalStateException exception) {
            SweetAlert.error("AI Review", exception.getMessage());
            return;
        }
        boolean hasDrawingForReview = drawingImagePath != null && !drawingImagePath.isBlank();

        if (!hasWrittenAnswer && !hasDrawingForReview) {
            SweetAlert.warning("AI Review", "Write an answer or draw something before asking for AI feedback.");
            return;
        }

        if (reviewAnswerButton != null) {
            reviewAnswerButton.setDisable(true);
            reviewAnswerButton.setText("Reviewing...");
        }
        studioStatusLabel.setText("AI review is analyzing the exercise...");

        Student student = studentService.getCurrentStudent();
        int studentAge = student == null ? 10 : Math.max(8, student.getAge());

        Task<LocalAiContentService.ExerciseReview> reviewTask = new Task<>() {
            @Override
            protected LocalAiContentService.ExerciseReview call() {
                return localAiContentService.reviewExerciseAnswer(
                        currentCourse.getTitle(),
                        currentCourse.getSubject(),
                        currentCourse.getLevel(),
                        currentCourse.getDescription(),
                        currentLesson.getTitle(),
                        currentExercise.getTitle(),
                        reviewPrompt,
                        answerTextArea.getText(),
                        drawingImagePath,
                        hasDrawingForReview,
                        studentAge
                );
            }
        };

        reviewTask.setOnSucceeded(event -> {
            if (reviewAnswerButton != null) {
                reviewAnswerButton.setDisable(false);
                reviewAnswerButton.setText("AI Review");
            }
            studioStatusLabel.setText("AI review ready. Improve your answer, then save the exercise.");
            StudentAiSupportDialogs.showExerciseReview("AI Review - " + currentExercise.getTitle(), reviewTask.getValue());
        });

        reviewTask.setOnFailed(event -> {
            if (reviewAnswerButton != null) {
                reviewAnswerButton.setDisable(false);
                reviewAnswerButton.setText("AI Review");
            }
            String message = reviewTask.getException() == null ? "The AI review is unavailable right now." : reviewTask.getException().getMessage();
            SweetAlert.error("AI Review", message);
        });

        Thread worker = new Thread(reviewTask, "exercise-ai-review");
        worker.setDaemon(true);
        worker.start();
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
            canvasHasVisibleDrawing = true;
        });
    }

    private void resetCanvas() {
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        graphicsContext.setStroke(Color.web("#173258"));
        graphicsContext.setLineWidth(3.0);
        canvasHasVisibleDrawing = false;
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
            canvasHasVisibleDrawing = true;
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

            writeCanvasSnapshot(outputPath);
            return outputPath.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save the exercise drawing.", exception);
        }
    }

    private String resolveDrawingForReview(LessonExercise exercise) {
        if (!drawingEnabled || exercise == null) {
            return "";
        }
        if (canvasHasVisibleDrawing) {
            return saveReviewDrawingSnapshot(exercise);
        }
        if (exercise.hasDrawing()) {
            Path savedDrawingPath = Path.of(exercise.getDrawingPath());
            if (Files.exists(savedDrawingPath)) {
                return savedDrawingPath.toString();
            }
        }
        return "";
    }

    private String saveReviewDrawingSnapshot(LessonExercise exercise) {
        try {
            Student student = studentService.getCurrentStudent();
            long studentId = student == null ? 0 : student.getId();
            Path outputDirectory = Path.of(System.getProperty("user.dir"), "generated", "exercise-drawings", "reviews");
            Files.createDirectories(outputDirectory);

            String fileName = "student-" + studentId + "-exercise-" + exercise.getId() + "-review-" + System.currentTimeMillis() + ".png";
            Path outputPath = outputDirectory.resolve(fileName);
            writeCanvasSnapshot(outputPath, REVIEW_IMAGE_WIDTH, REVIEW_IMAGE_HEIGHT);
            outputPath.toFile().deleteOnExit();
            return outputPath.toString();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to prepare the drawing for AI review.", exception);
        }
    }

    private void writeCanvasSnapshot(Path outputPath) throws IOException {
        writeCanvasSnapshot(outputPath, (int) CANVAS_WIDTH, (int) CANVAS_HEIGHT);
    }

    private void writeCanvasSnapshot(Path outputPath, int targetWidth, int targetHeight) throws IOException {
        WritableImage writableImage = new WritableImage((int) CANVAS_WIDTH, (int) CANVAS_HEIGHT);
        drawingCanvas.snapshot(new SnapshotParameters(), writableImage);
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(writableImage, null);

        if (bufferedImage.getWidth() == targetWidth && bufferedImage.getHeight() == targetHeight) {
            ImageIO.write(bufferedImage, "png", outputPath.toFile());
            return;
        }

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = resizedImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.drawImage(bufferedImage, 0, 0, targetWidth, targetHeight, null);
        graphics.dispose();
        ImageIO.write(resizedImage, "png", outputPath.toFile());
    }

    private boolean isDrawingEnabled(Student student) {
        return true;
    }

    private String buildTutorResourceSummary() {
        StringBuilder summary = new StringBuilder();
        if (currentLesson != null) {
            summary.append("Lesson resources: ").append(currentLesson.getUrlSummary());
        }
        if (currentExercise != null) {
            if (!summary.isEmpty()) {
                summary.append(" | ");
            }
            summary.append("Current exercise: ").append(currentExercise.getTitle())
                    .append(" - ").append(currentExercise.getResolvedPrompt());
        }
        return summary.isEmpty() ? "No extra lesson or exercise context provided." : summary.toString();
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
