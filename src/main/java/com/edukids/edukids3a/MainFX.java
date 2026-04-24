package com.edukids.edukids3a;

import com.edukids.edukids3a.controllers.LoginController;
import com.edukids.edukids3a.controllers.app.MainController;
import com.edukids.edukids3a.services.SessionManager;
import com.edukids.edukids3a.util.AuthSchema;
import com.edukids.edukids3a.util.JdbcAuthDataSource;
import com.edukids.edukids3a.util.JpaUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

/** Point d’entrée JavaFX (équivalent {@code MainFX} / structure MVC par dossiers). */
public class MainFX extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(MainFX.class);
    private static volatile Throwable persistenceBootstrapError;

    @Override
    public void init() {
        try {
            JpaUtil.getEntityManagerFactory();
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
                    "Vérifiez que MySQL est démarré et que la base existe (voir persistence.xml : edukidsj).\n\n"
                            + Optional.ofNullable(persistenceBootstrapError.getMessage()).orElse(""));
            a.showAndWait();
            Platform.exit();
            return;
        }
        try {
            Connection c = JdbcAuthDataSource.getConnection();
            AuthSchema.ensureUserTableAndSeed(c);
        } catch (SQLException e) {
            LOG.error("Initialisation table user / JDBC auth", e);
            Alert a = new Alert(AlertType.ERROR,
                    "Connexion JDBC pour la connexion utilisateur impossible.\n"
                            + "Vérifiez les paramètres (persistence.xml et optionnellement config/auth-jdbc.properties).\n\n"
                            + e.getMessage(),
                    ButtonType.OK);
            a.setHeaderText("Authentification");
            a.showAndWait();
            Platform.exit();
            return;
        }
        afficherSceneConnexion(stage);
        stage.show();
    }

    public static void afficherSceneConnexion(Stage stage) {
        SessionManager.clearSession();
        try {
            var url = Objects.requireNonNull(MainFX.class.getResource("/fxml/LoginView.fxml"));
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            LoginController loginController = loader.getController();
            loginController.setOnAuthentificationReussie(() -> {
                try {
                    chargerEtAfficherScenePrincipale(stage);
                } catch (IOException ex) {
                    LOG.error("Chargement interface principale", ex);
                    Alert a = new Alert(AlertType.ERROR, "Impossible d'ouvrir l'interface : " + ex.getMessage(),
                            ButtonType.OK);
                    a.setHeaderText(null);
                    a.showAndWait();
                }
            });
            Scene scene = new Scene(root);
            stage.setTitle("EduKids — Connexion");
            stage.setScene(scene);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.setMaximized(false);
            stage.centerOnScreen();
            scene.setOnKeyPressed(null);
        } catch (IOException e) {
            LOG.error("Chargement LoginView", e);
            Alert a = new Alert(AlertType.ERROR, e.getMessage(), ButtonType.OK);
            a.setHeaderText("Impossible d'afficher la connexion");
            a.showAndWait();
            Platform.exit();
        }
    }

    public static void chargerEtAfficherScenePrincipale(Stage stage) throws IOException {
        var url = Objects.requireNonNull(MainFX.class.getResource("/fxml/MainView.fxml"));
        MainController mainController = new MainController();
        FXMLLoader loader = new FXMLLoader(url);
        loader.setController(mainController);
        loader.setControllerFactory(type -> {
            if (type == MainController.class) {
                return mainController;
            }
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Contrôleur FXML inattendu: " + type.getName(), e);
            }
        });
        Scene scene = new Scene(loader.load());
        mainController.initialiserApresChargementFxml();
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
        stage.setMinWidth(Math.min(920, bounds.getWidth() * 0.85));
        stage.setMinHeight(Math.min(620, bounds.getHeight() * 0.75));
        stage.setTitle("EduKids — Événements & programmes");
        stage.setScene(scene);
        stage.setMaximized(true);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
                e.consume();
            }
        });
    }

    @Override
    public void stop() {
        JpaUtil.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
