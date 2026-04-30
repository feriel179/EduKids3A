package tn.esprit.util;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.esprit.services.LocalAiContentService;

import java.util.Optional;

public final class StudentAiSupportDialogs {
    private StudentAiSupportDialogs() {
    }

    public static Optional<String> promptQuestion(String title, String helperText, String defaultQuestion) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);

        DialogPane pane = dialog.getDialogPane();
        configurePane(pane);

        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType askButton = new ButtonType("Ask AI", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().setAll(cancelButton, askButton);
        styleButton(pane, cancelButton, "secondary-button");
        styleButton(pane, askButton, "primary-button");

        Label helperLabel = new Label(helperText);
        helperLabel.getStyleClass().add("ai-dialog-helper");
        helperLabel.setWrapText(true);

        TextArea questionArea = new TextArea(defaultQuestion == null ? "" : defaultQuestion);
        questionArea.getStyleClass().addAll("student-exercise-textarea", "ai-response-area");
        questionArea.setWrapText(true);
        questionArea.setPrefRowCount(5);
        VBox.setVgrow(questionArea, Priority.ALWAYS);

        VBox content = new VBox(10, helperLabel, questionArea);
        content.setPadding(new Insets(4, 0, 0, 0));
        content.getStyleClass().add("ai-dialog-content");
        pane.setContent(content);
        pane.setPrefWidth(540);

        Node askNode = pane.lookupButton(askButton);
        if (askNode != null) {
            askNode.disableProperty().bind(questionArea.textProperty().isEmpty());
        }

        dialog.setResultConverter(result -> result == askButton ? questionArea.getText().trim() : null);
        return dialog.showAndWait();
    }

    public static void showTutorAnswer(String title, String answer) {
        Alert alert = createDisplayAlert(title);
        DialogPane pane = alert.getDialogPane();

        Label headerLabel = new Label("AI Tutor");
        headerLabel.getStyleClass().add("ai-dialog-section-title");

        TextArea answerArea = new TextArea(answer == null ? "" : answer);
        answerArea.getStyleClass().addAll("student-exercise-textarea", "ai-response-area");
        answerArea.setEditable(false);
        answerArea.setWrapText(true);
        answerArea.setPrefRowCount(12);

        VBox content = new VBox(10, headerLabel, answerArea);
        content.getStyleClass().add("ai-dialog-content");
        pane.setContent(content);
        pane.setPrefWidth(620);
        alert.showAndWait();
    }

    public static void showExerciseReview(String title, LocalAiContentService.ExerciseReview review) {
        Alert alert = createDisplayAlert(title);
        DialogPane pane = alert.getDialogPane();

        Label scoreLabel = new Label(review.score());
        scoreLabel.getStyleClass().add("ai-review-score");

        VBox content = new VBox(
                12,
                scoreLabel,
                createSection("Summary", review.summary()),
                createSection("Strengths", review.strengths()),
                createSection("Improvements", review.improvements()),
                createSection("Next Step", review.nextStep())
        );
        content.getStyleClass().add("ai-dialog-content");

        pane.setContent(content);
        pane.setPrefWidth(640);
        alert.showAndWait();
    }

    private static VBox createSection(String title, String text) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("ai-dialog-section-title");

        Label textLabel = new Label(text == null ? "" : text);
        textLabel.getStyleClass().add("ai-dialog-body");
        textLabel.setWrapText(true);

        VBox section = new VBox(6, titleLabel, textLabel);
        section.getStyleClass().add("ai-dialog-section");
        return section;
    }

    private static Alert createDisplayAlert(String title) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(title);
        alert.setHeaderText(null);

        DialogPane pane = alert.getDialogPane();
        configurePane(pane);

        ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().setAll(closeButton);
        styleButton(pane, closeButton, "secondary-button");
        return alert;
    }

    private static void configurePane(DialogPane pane) {
        pane.getStylesheets().add(StudentAiSupportDialogs.class.getResource("/tn/esprit/css/styles.css").toExternalForm());
        if (!pane.getStyleClass().contains("ai-dialog-pane")) {
            pane.getStyleClass().add("ai-dialog-pane");
        }
    }

    private static void styleButton(DialogPane pane, ButtonType buttonType, String styleClass) {
        Button button = (Button) pane.lookupButton(buttonType);
        if (button != null && !button.getStyleClass().contains(styleClass)) {
            button.getStyleClass().add(styleClass);
            button.setMinWidth(120);
        }
    }
}
