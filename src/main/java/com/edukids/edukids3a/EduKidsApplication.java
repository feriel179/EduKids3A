package com.edukids.edukids3a;

import com.edukids.edukids3a.persistence.DatabaseInitializer;
import com.edukids.edukids3a.persistence.JpaUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.KeyCode;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

public class EduKidsApplication extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(EduKidsApplication.class);
    private static volatile Throwable persistenceBootstrapError;

    @Override
    public void init() {
        try {
            // Initialiser la base de données (créer si nécessaire)
            DatabaseInitializer.initializeDatabase();
            LOG.info("Database initialized successfully.");
            
            // Initialiser JPA
            JpaUtil.getEntityManagerFactory();
            LOG.info("JPA EntityManagerFactory initialized successfully.");
        } catch (Throwable t) {
            persistenceBootstrapError = t;
            LOG.error("Impossible d'initialiser la persistance JPA (MySQL / persistence.xml).", t);
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        if (persistenceBootstrapError != null) {
            Alert a = new Alert(AlertType.ERROR);
            a.setTitle("EduKids");
            a.setHeaderText("Impossible d'ouvrir la base de données");
            a.setContentText(
                    "Vérifiez que MySQL est démarré et que la base configurée existe (par défaut : edukids sur localhost:3308).\n\n"
                            + Optional.ofNullable(persistenceBootstrapError.getMessage()).orElse(""));
            a.showAndWait();
            Platform.exit();
            return;
        }
        FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(
                getClass().getResource("/fxml/Login.fxml")));
        Scene scene = new Scene(loader.load());
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setMinWidth(Math.min(760, bounds.getWidth() * 0.72));
        stage.setMinHeight(Math.min(560, bounds.getHeight() * 0.7));
        stage.setTitle("EduKids - Connexion");
        stage.setScene(scene);
        stage.setMaximized(false);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
                e.consume();
            }
        });
        stage.show();
    }

    @Override
    public void stop() {
        JpaUtil.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
