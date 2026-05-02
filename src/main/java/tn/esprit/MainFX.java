package tn.esprit;

import com.edukids.entities.User;
import com.edukids.enums.Role;
import com.edukids.edukids3a.security.AuthSession;
import com.edukids.edukids3a.realtime.ChatRealtimeHub;
import com.edukids.utils.SessionManager;
import tn.esprit.models.Student;
import tn.esprit.services.StudentService;
import com.edukids.utils.MyConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class MainFX extends Application {

    private static MainFX instance;
    private Stage primaryStage;

    public static MainFX getInstance() {
        return instance;
    }

    @Override
    public void start(Stage stage) {
        try {
            instance = this;
            this.primaryStage = stage;

            initializeDatabase();

            // 🔥 CORRECTION ICI
            showLoginView();   // 👈 afficher login

            primaryStage.setMinWidth(1100);
            primaryStage.setMinHeight(720);
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeDatabase() {
        try {
            MyConnection.getInstance().getCnx();
        } catch (RuntimeException exception) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Database Warning");
            alert.setHeaderText("EduKids database is not ready");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
        }

        try {
            ChatRealtimeHub.getInstance().ensureStarted();
        } catch (RuntimeException exception) {
            System.out.println("Chat WebSocket server unavailable: " + exception.getMessage());
        }
    }

    public void showLoginView() throws Exception {
        SessionManager.clearSession();
        StudentService.clearCurrentStudent();
        Parent root = loadView("/com/edukids/views/login.fxml");
        setScene(root, "EduKids - Sign In", "/com/edukids/css/style.css");
    }

    public void showUserDashboard() throws Exception {
        Parent root = loadView("/com/edukids/views/dashboard.fxml");
        setScene(root, "EduKids - Users Dashboard", "/tn/esprit/css/styles.css", "/com/edukids/css/style.css");
    }

    public void showParentHome() throws Exception {
        Parent root = loadView("/com/edukids/views/user-home.fxml");
        setScene(root, "EduKids - Parent Space", "/tn/esprit/css/styles.css", "/com/edukids/css/style.css");
    }

    public void showAdminShell() throws Exception {
        Parent root = loadView("/tn/esprit/fxml/admin/admin-shell.fxml");
        setScene(root, "CourseApp - Admin Panel");
    }

    public void showStudentShell(Student student) throws Exception {
        Parent root = loadView("/tn/esprit/fxml/student/student-shell.fxml");
        setScene(root, "CourseApp - Student Panel - " + student.getName(), "/tn/esprit/css/styles.css", "/com/edukids/css/style.css");
    }

    public void showAdminCourses() throws Exception {
        showAdminShell();
        var controller = tn.esprit.controllers.admin.AdminShellController.getInstance();
        if (controller != null) {
            controller.showCourses();
        }
    }

    public void showStudentShellForUser(User user) throws Exception {
        if (user == null) {
            throw new IllegalArgumentException("Aucun utilisateur connecte.");
        }

        Student student = new StudentService().loginOrCreateStudent(user.getEmail());
        showStudentShell(student);
    }

    public void showStudentCoursesForUser(User user) throws Exception {
        showStudentShellForUser(user);
        var controller = tn.esprit.controllers.student.StudentShellController.getInstance();
        if (controller != null) {
            controller.showCatalog();
        }
    }

    public void showStudentQuizForUser(User user) throws Exception {
        showStudentShellForUser(user);
        var controller = tn.esprit.controllers.student.StudentShellController.getInstance();
        if (controller != null) {
            controller.showQuiz();
        }
    }

    public void showStudentEventsForUser(User user) throws Exception {
        showStudentShellForUser(user);
        var controller = tn.esprit.controllers.student.StudentShellController.getInstance();
        if (controller != null) {
            controller.showEvents();
        }
    }

    public void showEventsManagement() throws Exception {
        showUserDashboard();
        var controller = com.edukids.controllers.DashboardController.getInstance();
        if (controller != null) {
            controller.showEvents();
        }
    }

    public void showChatViewForUser(User user) throws Exception {
        if (user == null) {
            throw new IllegalArgumentException("Aucun utilisateur connecte.");
        }

        AuthSession.setCurrentUser(toChatUser(user));
        Parent root = loadView("/tn/esprit/fxml/chat/ChatView.fxml");
        setScene(root, "EduKids - Chat", "/tn/esprit/css/chat.css", "/com/edukids/css/style.css");
    }

    public Parent loadView(String path) throws Exception {
        if (MainFX.class.getResource(path) == null) {
            throw new RuntimeException("FXML introuvable: " + path);
        }
        FXMLLoader loader = new FXMLLoader(MainFX.class.getResource(path));
        return loader.load();
    }

    private com.edukids.edukids3a.model.User toChatUser(User user) {
        com.edukids.edukids3a.model.User chatUser = new com.edukids.edukids3a.model.User();
        chatUser.setId(user.getId());
        chatUser.setEmail(user.getEmail());
        chatUser.setFirstName(user.getFirstName());
        chatUser.setLastName(user.getLastName());
        chatUser.setActive(user.isActive());
        Role primaryRole = user.getPrimaryRole();
        chatUser.setRole(primaryRole != null ? primaryRole.getDbValue() : "user");
        return chatUser;
    }

    private void setScene(Parent root, String title) {
        Scene scene = new Scene(root, 1280, 820);

        var css = MainFX.class.getResource("/tn/esprit/css/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            System.out.println("CSS non trouvé !");
        }

        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
    }

    private void setScene(Parent root, String title, String... cssPaths) {
        Scene scene = new Scene(root, 1280, 820);

        for (String cssPath : cssPaths) {
            var css = MainFX.class.getResource(cssPath);
            if (css != null) {
                scene.getStylesheets().add(css.toExternalForm());
            } else {
                System.out.println("CSS non trouve: " + cssPath);
            }
        }

        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
