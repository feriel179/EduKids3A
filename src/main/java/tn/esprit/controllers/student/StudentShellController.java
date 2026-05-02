package tn.esprit.controllers.student;

import com.edukids.edukids3a.controller.QuestionController;
import com.edukids.edukids3a.controller.QuizController;
import com.edukids.edukids3a.controller.QuizResultController;
import com.edukids.edukids3a.persistence.DatabaseManager;
import com.edukids.edukids3a.security.AuthSession;
import com.edukids.edukids3a.service.QuestionService;
import com.edukids.edukids3a.service.QuizResultService;
import com.edukids.edukids3a.service.QuizService;
import com.edukids.edukids3a.ui.QuizBackOfficeView;
import com.edukids.enums.Role;
import com.edukids.entities.User;
import com.edukids.utils.SessionManager;
import tn.esprit.MainFX;
import tn.esprit.models.Course;
import tn.esprit.models.Lesson;
import tn.esprit.models.Student;
import tn.esprit.services.StudentService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class StudentShellController {

    private static StudentShellController instance;

    @FXML
    private StackPane contentPane;

    @FXML
    private Button catalogButton;

    @FXML
    private Button myCoursesButton;

    @FXML
    private Button shopButton;

    @FXML
    private Button eventsButton;

    @FXML
    private Button quizButton;

    @FXML
    private Button chatButton;

    @FXML
    private Button profileButton;

    @FXML
    private Label studentNameLabel;

    @FXML
    private Label studentEmailLabel;

    @FXML
    private Label studentAvatarLabel;

    private final StudentService studentService = new StudentService();

    public static StudentShellController getInstance() {
        return instance;
    }

    @FXML
    private void initialize() {
        instance = this;
        if (eventsButton != null) {
            eventsButton.setDisable(false);
            eventsButton.setOpacity(1.0);
        }
        populateStudentHeader();
        showCatalog();
    }

    @FXML
    public void showCatalog() {
        loadCenterView("/tn/esprit/fxml/student/catalog.fxml", null);
        setActiveNavigation(catalogButton);
    }

    @FXML
    public void showMyCourses() {
        loadCenterView("/tn/esprit/fxml/student/my-courses.fxml", null);
        setActiveNavigation(myCoursesButton);
    }

    @FXML
    public void showShop() {
        loadCenterView("/fxml/main-view.fxml", loader -> {
            com.ecom.ui.MainController controller = loader.getController();
            controller.openStudentMode();
        }, true);
        setActiveNavigation(shopButton);
    }

    @FXML
    public void showEvents() {
        loadEventsView(false);
        setActiveNavigation(eventsButton);
    }

    @FXML
    public void showProfile() {
        loadCenterView("/tn/esprit/fxml/student/profile.fxml", null);
        setActiveNavigation(profileButton);
    }

    @FXML
    public void showQuiz() {
        try {
            DatabaseManager.initialize();
            QuizService quizService = new QuizService();
            QuizController quizController = new QuizController(quizService);
            QuestionController questionController = new QuestionController(new QuestionService(quizService));
            QuizResultController quizResultController = new QuizResultController(new QuizResultService());
            BorderPane quizRoot = new QuizBackOfficeView(
                    quizController,
                    questionController,
                    quizResultController
            ).buildStudent();

            contentPane.getChildren().setAll(quizRoot);
            setActiveNavigation(quizButton);
        } catch (RuntimeException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Quiz");
            alert.setHeaderText("Impossible d'ouvrir les quiz");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
            exception.printStackTrace();
        }
    }

    @FXML
    public void showChat() {
        try {
            User user = SessionManager.getCurrentUser();
            if (user == null) {
                throw new IllegalStateException("Aucun utilisateur connecte.");
            }

            AuthSession.setCurrentUser(toChatUser(user));
            loadCenterView("/tn/esprit/fxml/chat/ChatView.fxml", null);
            setActiveNavigation(chatButton);
        } catch (RuntimeException exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Chat");
            alert.setHeaderText("Impossible d'ouvrir la messagerie");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
            exception.printStackTrace();
        }
    }

    // 🔥 CORRECTION ICI
    @FXML
    private void handleLogout() {
        try {
            MainFX.getInstance().showLoginView();
        } catch (Exception e) {   // ✅ FIX
            e.printStackTrace();
        }
    }

    public void showCourseDetail(Course course) {
        loadCenterView("/tn/esprit/fxml/student/course-detail.fxml", loader -> {
            CourseDetailController controller = loader.getController();
            controller.setCourse(course);
        });
        setActiveNavigation(catalogButton);
    }

    public void showExerciseStudio(Course course, Lesson lesson) {
        loadCenterView("/tn/esprit/fxml/student/exercise-studio.fxml", loader -> {
            ExerciseStudioController controller = loader.getController();
            controller.setContext(course, lesson);
        });
        setActiveNavigation(catalogButton);
    }

    // 🔥 BONUS FIX
    private void loadCenterView(String fxmlPath, LoaderCallback callback) {
        loadCenterView(fxmlPath, callback, false);
    }

    private void loadCenterView(String fxmlPath, LoaderCallback callback, boolean scrollable) {
        try {
            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource(fxmlPath));
            Parent view = loader.load();

            if (callback != null) {
                callback.accept(loader);
            }

            if (scrollable) {
                ScrollPane scrollPane = new ScrollPane(view);
                scrollPane.setFitToWidth(true);
                scrollPane.setFitToHeight(false);
                scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                scrollPane.getStyleClass().add("scroll-pane-transparent");
                contentPane.getChildren().setAll(scrollPane);
            } else {
                contentPane.getChildren().setAll(view);
            }

        } catch (Exception e) {  // ✅ FIX
            e.printStackTrace();
        }
    }

    private void loadEventsView(boolean adminMode) {
        try {
            com.edukids.edukids3a.controller.MainController controller =
                    new com.edukids.edukids3a.controller.MainController();
            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource("/fxml/MainView.fxml"));
            loader.setController(controller);
            loader.setControllerFactory(type -> {
                if (type == com.edukids.edukids3a.controller.MainController.class) {
                    return controller;
                }
                try {
                    return type.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException exception) {
                    throw new IllegalStateException("Controleur FXML inattendu: " + type.getName(), exception);
                }
            });

            Parent view = loader.load();
            controller.initialiserApresChargementFxml();
            if (adminMode) {
                controller.openAdminMode();
            } else {
                controller.openStudentMode();
            }

            ScrollPane scrollPane = new ScrollPane(view);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(false);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.getStyleClass().add("scroll-pane-transparent");
            contentPane.getChildren().setAll(scrollPane);
        } catch (Exception exception) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Events");
            alert.setHeaderText("Impossible d'ouvrir le module Events");
            alert.setContentText(exception.getMessage());
            alert.showAndWait();
            exception.printStackTrace();
        }
    }

    private void populateStudentHeader() {
        Student student = studentService.getCurrentStudent();

        if (student == null) {
            studentNameLabel.setText("EduKids Learner");
            studentEmailLabel.setText("student@edukids.local");
            studentAvatarLabel.setText("ST");
            return;
        }

        studentNameLabel.setText(student.getName());
        studentEmailLabel.setText(student.getAgeGroupLabel() + " | " + student.getPreferredCategory());
        studentAvatarLabel.setText(buildInitials(student.getName(), student.getEmail()));
    }

    public void refreshStudentHeader() {
        populateStudentHeader();
    }

    private void setActiveNavigation(Button activeButton) {
        clearNavigationState();

        if (activeButton != null && !activeButton.getStyleClass().contains("student-nav-button-active")) {
            activeButton.getStyleClass().add("student-nav-button-active");
        }
    }

    private void clearNavigationState() {
        Button[] buttons = {catalogButton, myCoursesButton, shopButton, eventsButton, quizButton, chatButton, profileButton};

        for (Button button : buttons) {
            if (button != null) {
                button.getStyleClass().remove("student-nav-button-active");
            }
        }
    }

    private String buildInitials(String name, String email) {
        String source = name != null && !name.isBlank() ? name : email;

        if (source == null || source.isBlank()) {
            return "ST";
        }

        String[] parts = source.trim().split("\\s+");

        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }

        String clean = source.replaceAll("[^A-Za-z0-9]", "");

        if (clean.length() >= 2) {
            return clean.substring(0, 2).toUpperCase();
        }

        return clean.isBlank() ? "ST" : clean.toUpperCase();
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

    @FunctionalInterface
    private interface LoaderCallback {
        void accept(FXMLLoader loader);
    }
}
