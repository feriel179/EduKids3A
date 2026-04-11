package tn.esprit.util;

import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Optional;

public final class SweetAlert {
    private SweetAlert() {
    }

    public static void success(String title, String message) {
        Alert alert = createAlert("success", title, message);
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        DialogPane pane = alert.getDialogPane();
        pane.getButtonTypes().setAll(okButton);
        styleButton(pane, okButton, "primary-button");
        alert.showAndWait();
    }

    public static void warning(String title, String message) {
        Alert alert = createAlert("warning", title, message);
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        DialogPane pane = alert.getDialogPane();
        pane.getButtonTypes().setAll(okButton);
        styleButton(pane, okButton, "secondary-button");
        alert.showAndWait();
    }

    public static void error(String title, String message) {
        Alert alert = createAlert("error", title, message);
        ButtonType okButton = new ButtonType("Close", ButtonBar.ButtonData.OK_DONE);
        DialogPane pane = alert.getDialogPane();
        pane.getButtonTypes().setAll(okButton);
        styleButton(pane, okButton, "danger-button");
        alert.showAndWait();
    }

    public static boolean confirmDanger(String title, String message, String confirmLabel) {
        Alert alert = createAlert("confirm", title, message);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirmButton = new ButtonType(confirmLabel, ButtonBar.ButtonData.OK_DONE);

        DialogPane pane = alert.getDialogPane();
        pane.getButtonTypes().setAll(cancelButton, confirmButton);
        styleButton(pane, cancelButton, "secondary-button");
        styleButton(pane, confirmButton, "danger-button");

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE;
    }

    private static Alert createAlert(String variant, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle(title);
        alert.setHeaderText(null);

        DialogPane pane = alert.getDialogPane();
        pane.getStylesheets().add(SweetAlert.class.getResource("/tn/esprit/css/styles.css").toExternalForm());
        pane.getStyleClass().addAll("sweet-alert", "sweet-alert-" + variant);

        Label iconLabel = new Label(resolveIcon(variant));
        iconLabel.getStyleClass().addAll("sweet-alert-icon", "sweet-alert-icon-" + variant);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("sweet-alert-title");
        titleLabel.setWrapText(true);

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("sweet-alert-message");
        messageLabel.setWrapText(true);

        VBox content = new VBox(12, iconLabel, titleLabel, messageLabel);
        content.setAlignment(Pos.CENTER);
        content.getStyleClass().add("sweet-alert-box");

        pane.setContent(content);
        pane.setGraphic(null);
        return alert;
    }

    private static void styleButton(DialogPane pane, ButtonType buttonType, String styleClass) {
        Button button = (Button) pane.lookupButton(buttonType);
        if (button != null) {
            button.getStyleClass().add(styleClass);
            button.setMinWidth(120);
        }
    }

    private static String resolveIcon(String variant) {
        return switch (variant) {
            case "success" -> "OK";
            case "warning" -> "!";
            case "error" -> "X";
            case "confirm" -> "?";
            default -> "i";
        };
    }
}
