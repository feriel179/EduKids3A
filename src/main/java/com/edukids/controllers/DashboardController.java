package com.edukids.controllers;

import com.edukids.entities.User;
import com.edukids.enums.Role;
import com.edukids.edukids3a.security.AuthSession;
import com.edukids.services.UserService;
import com.edukids.utils.Navigator;
import com.edukids.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import com.edukids.edukids3a.controller.QuestionController;
import com.edukids.edukids3a.controller.QuizController;
import com.edukids.edukids3a.controller.QuizResultController;
import com.edukids.edukids3a.persistence.DatabaseManager;
import com.edukids.edukids3a.service.QuestionService;
import com.edukids.edukids3a.service.QuizResultService;
import com.edukids.edukids3a.service.QuizService;
import com.edukids.edukids3a.ui.QuizBackOfficeView;
import tn.esprit.MainFX;
import tn.esprit.controllers.admin.CourseFormController;
import tn.esprit.controllers.admin.CourseSuccessController;
import tn.esprit.controllers.admin.LessonFormController;
import tn.esprit.controllers.admin.LessonSuccessController;
import tn.esprit.models.Course;
import tn.esprit.models.Lesson;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    private static DashboardController instance;

    // --- Sidebar ---
    @FXML private Label currentUserLabel;
    @FXML private Button dashboardButton;
    @FXML private Button allUsersButton;
    @FXML private Button coursButton;
    @FXML private Button produitsButton;
    @FXML private VBox produitsSubmenuBox;
    @FXML private Button produitsCommandesButton;
    @FXML private Button produitsListButton;
    @FXML private Button produitsFormButton;
    @FXML private Button produitsCategoriesButton;
    @FXML private Button produitsCategoryFormButton;
    @FXML private VBox userSubmenuBox;
    @FXML private VBox courseSubmenuBox;
    @FXML private VBox quizSubmenuBox;
    @FXML private Button adminsButton;
    @FXML private Button studentsButton;
    @FXML private Button parentsButton;
    @FXML private Button createCourseButton;
    @FXML private Button lessonsButton;
    @FXML private Button createLessonButton;
    @FXML private Button chatButton;
    @FXML private VBox chatSubmenuBox;
    @FXML private Button chatMessageButton;
    @FXML private Button chatStatsButton;
    @FXML private Button quizButton;
    @FXML private Button quizListButton;
    @FXML private Button quizFormButton;
    @FXML private Button quizQuestionsButton;
    @FXML private Button quizQuestionFormButton;
    @FXML private Button quizStatsButton;
    @FXML private HBox rootPane;
    @FXML private Label topbarRoleLabel;
    @FXML private Label topbarNameLabel;
    @FXML private Label topbarAvatarLabel;
    @FXML private Label topbarKickerLabel;
    @FXML private Label topbarTitleLabel;

    // --- Main content area ---
    @FXML private StackPane contentArea;
    @FXML private StackPane moduleView;

    // --- Dashboard View ---
    @FXML private VBox dashboardView;
    @FXML private Label totalUsersLabel;
    @FXML private Label studentsLabel;
    @FXML private Label parentsLabel;
    @FXML private Label adminsLabel;
    @FXML private Label activeUsersLabel;
    @FXML private ProgressBar activeProgressBar;
    @FXML private PieChart roleChart;
    @FXML private VBox recentUsersBox;

    // --- User Management View ---
    @FXML private VBox userManagementView;
    @FXML private Label userMgmtTitle;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> roleFilterCombo;
    @FXML private ComboBox<String> sortCombo;
    @FXML private ListView<HBox> userListView;

    // --- Edit Form ---
    @FXML private VBox editFormPane;
    @FXML private Label editFormTitle;
    @FXML private TextField editFirstName;
    @FXML private TextField editLastName;
    @FXML private TextField editEmail;
    @FXML private ComboBox<String> editRole;
    @FXML private CheckBox editActiveCheck;
    @FXML private CheckBox editVerifiedCheck;
    @FXML private VBox passwordSection;
    @FXML private PasswordField editPassword;
    @FXML private PasswordField editConfirmPassword;

    private final UserService userService = new UserService();
    private User selectedUser;
    private boolean isCreateMode = false;
    private Role currentRoleFilter = null; // null = all users
    private BorderPane quizBackOfficeRoot;
    private QuizBackOfficeView quizBackOfficeView;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        instance = this;
        User current = SessionManager.getCurrentUser();
        if (current != null) {
            currentUserLabel.setText(current.getFullName());
            topbarRoleLabel.setText(current.getPrimaryRole().getDisplayName());
            topbarNameLabel.setText(current.getFullName());
            topbarAvatarLabel.setText(buildInitials(current));
        }

        // Setup combos
        roleFilterCombo.getItems().addAll("All Roles", "Admin", "Student (Eleve)", "Parent");
        roleFilterCombo.setValue("All Roles");
        roleFilterCombo.setOnAction(e -> applyFilters());

        sortCombo.getItems().addAll("Newest First", "Oldest First", "Name A-Z", "Name Z-A", "Email A-Z");
        sortCombo.setValue("Newest First");
        sortCombo.setOnAction(e -> applyFilters());

        editRole.getItems().addAll("Admin", "Student (Eleve)", "Parent");

        editFormPane.setVisible(false);
        editFormPane.setManaged(false);
        userManagementView.setVisible(false);
        userManagementView.setManaged(false);
        showUserSubmenu(false);
        showCourseSubmenu(false);
        showProduitsSubmenu(false);
        showChatSubmenu(false);
        showQuizSubmenu(false);
        setActiveUserFilter(null);
        setActiveCourseSubNavigation(null);
        setActiveChatSubNavigation(null);
        setActiveQuizSubNavigation(null);

        setActiveNavigation(dashboardButton);
        loadDashboardStats();

        // Live search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    public static DashboardController getInstance() {
        return instance;
    }

    public boolean isAttachedToShowingWindow() {
        return rootPane != null
                && rootPane.getScene() != null
                && rootPane.getScene().getWindow() != null
                && rootPane.getScene().getWindow().isShowing();
    }

    // ======================== SIDEBAR NAV ========================

    @FXML
    private void handleDashboardNav() {
        showUserSubmenu(false);
        showCourseSubmenu(false);
        showChatSubmenu(false);
        showQuizSubmenu(false);
        setActiveUserFilter(null);
        setActiveCourseSubNavigation(null);
        setActiveChatSubNavigation(null);
        setActiveQuizSubNavigation(null);
        setActiveNavigation(dashboardButton);
        setTopbarContext("Users space", "Dashboard");
        showView("dashboard");
        loadDashboardStats();
    }

    @FXML
    private void handleAllUsersNav() {
        currentRoleFilter = null;
        userMgmtTitle.setText("Users Management");
        roleFilterCombo.setValue("All Roles");
        showUserSubmenu(true);
        showCourseSubmenu(false);
        showChatSubmenu(false);
        showQuizSubmenu(false);
        setActiveUserFilter(null);
        setActiveCourseSubNavigation(null);
        setActiveChatSubNavigation(null);
        setActiveQuizSubNavigation(null);
        setActiveNavigation(allUsersButton);
        setTopbarContext("Users space", "All Users");
        showView("users");
        applyFilters();
    }

    @FXML
    private void handleAdminsNav() {
        currentRoleFilter = Role.ROLE_ADMIN;
        userMgmtTitle.setText("Admins Management");
        roleFilterCombo.setValue("Admin");
        showUserSubmenu(true);
        showCourseSubmenu(false);
        showChatSubmenu(false);
        showQuizSubmenu(false);
        setActiveUserFilter(adminsButton);
        setActiveCourseSubNavigation(null);
        setActiveChatSubNavigation(null);
        setActiveQuizSubNavigation(null);
        setActiveNavigation(allUsersButton);
        setTopbarContext("Users space", "Admins");
        showView("users");
        applyFilters();
    }

    @FXML
    private void handleStudentsNav() {
        currentRoleFilter = Role.ROLE_ELEVE;
        userMgmtTitle.setText("Students Management");
        roleFilterCombo.setValue("Student (Eleve)");
        showUserSubmenu(true);
        showCourseSubmenu(false);
        showChatSubmenu(false);
        showQuizSubmenu(false);
        setActiveUserFilter(studentsButton);
        setActiveCourseSubNavigation(null);
        setActiveChatSubNavigation(null);
        setActiveQuizSubNavigation(null);
        setActiveNavigation(allUsersButton);
        setTopbarContext("Users space", "Students");
        showView("users");
        applyFilters();
    }

    @FXML
    private void handleParentsNav() {
        currentRoleFilter = Role.ROLE_PARENT;
        userMgmtTitle.setText("Parents Management");
        roleFilterCombo.setValue("Parent");
        showUserSubmenu(true);
        showCourseSubmenu(false);
        showChatSubmenu(false);
        showQuizSubmenu(false);
        setActiveUserFilter(parentsButton);
        setActiveCourseSubNavigation(null);
        setActiveChatSubNavigation(null);
        setActiveQuizSubNavigation(null);
        setActiveNavigation(allUsersButton);
        setTopbarContext("Users space", "Parents");
        showView("users");
        applyFilters();
    }

    @FXML
    private void handleCoursNav() {
        showCourses();
    }

    @FXML
    private void handleCreateCourseNav() {
        showCreateCourse();
    }

    @FXML
    private void handleLessonsNav() {
        showLessons();
    }

    @FXML
    private void handleCreateLessonNav() {
        showCreateLesson(null);
    }

    @FXML
    private void handleEventsNav() {
        showCourseSubmenu(false);
        showQuizSubmenu(false);
        // TODO: Navigate to Events management
    }

    @FXML
    private void handleProduitsNav() {
        boolean expanded = produitsSubmenuBox != null && produitsSubmenuBox.isVisible();
        if (expanded) {
            showProduitsSubmenu(false);
            return;
        }
        showProduitsCommandes();
    }

    @FXML
    private void handleProduitsCommandesNav() {
        showProduitsCommandes();
    }

    @FXML
    private void handleProduitsListNav() {
        openProduitsModule("Shop management", "Liste des produits", produitsListButton, com.ecom.ui.MainController::showProduitList);
    }

    @FXML
    private void handleProduitsFormNav() {
        openProduitsModule("Shop management", "Creer ou modifier un produit", produitsFormButton, com.ecom.ui.MainController::showProduitForm);
    }

    @FXML
    private void handleProduitsCategoriesNav() {
        openProduitsModule("Shop management", "Liste des categories", produitsCategoriesButton, com.ecom.ui.MainController::showCategoryList);
    }

    @FXML
    private void handleProduitsCategoryFormNav() {
        openProduitsModule("Shop management", "Creer ou modifier une categorie", produitsCategoryFormButton, com.ecom.ui.MainController::showCategoryForm);
    }

    private void showProduitsCommandes() {
        openProduitsModule("Shop management", "Liste des commandes", produitsCommandesButton, com.ecom.ui.MainController::showCommandeList);
    }

    private void openProduitsModule(String kicker, String title, Button activeSubButton, EcomModuleAction action) {
        showUserSubmenu(false);
        showCourseSubmenu(false);
        showProduitsSubmenu(true);
        showChatSubmenu(false);
        showQuizSubmenu(false);
        setActiveUserFilter(null);
        setActiveCourseSubNavigation(null);
        setActiveProduitsSubNavigation(activeSubButton);
        setActiveChatSubNavigation(null);
        setActiveQuizSubNavigation(null);
        setActiveNavigation(produitsButton);
        setTopbarContext(kicker, title);
        showView("module");
        loadModuleView("/fxml/main-view.fxml", loader -> {
            com.ecom.ui.MainController controller = loader.getController();
            controller.openAdminMode();
            action.apply(controller);
        });
    }

    @FXML
    private void handleChatNav() {
        showCourseSubmenu(false);
        showQuizSubmenu(false);
        boolean expanded = chatSubmenuBox != null && chatSubmenuBox.isVisible();
        showChatSubmenu(!expanded);
        setActiveUserFilter(null);
        setActiveCourseSubNavigation(null);
        setActiveChatSubNavigation(null);
        setActiveQuizSubNavigation(null);
        setActiveNavigation(chatButton);
        setTopbarContext("Chat space", "Menu de chat");
    }

    @FXML
    private void handleChatMessageNav() {
        try {
            showChatModule();
            setActiveChatSubNavigation(chatMessageButton);
        } catch (Exception exception) {
            showAlert(Alert.AlertType.ERROR, "Chat", exception.getMessage());
            exception.printStackTrace();
        }
    }

    @FXML
    private void handleChatStatsNav() {
        showCourseSubmenu(false);
        showQuizSubmenu(false);
        try {
            showChatStatisticsModule();
        } catch (Exception exception) {
            showAlert(Alert.AlertType.ERROR, "Chat statistics", exception.getMessage());
            exception.printStackTrace();
        }
    }

    private void showChatModule() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Aucun utilisateur connecte.");
        }

        AuthSession.setCurrentUser(toChatUser(currentUser));
        showUserSubmenu(false);
        showCourseSubmenu(false);
        showChatSubmenu(true);
        showQuizSubmenu(false);
        setActiveUserFilter(null);
        setActiveCourseSubNavigation(null);
        setActiveChatSubNavigation(chatMessageButton);
        setActiveQuizSubNavigation(null);
        setActiveNavigation(chatButton);
        setTopbarContext("Chat space", "Messagerie");
        showView("module");
        loadModuleView("/tn/esprit/fxml/chat/ChatView.fxml", null);
    }

    private void showChatStatisticsModule() {
        showUserSubmenu(false);
        showCourseSubmenu(false);
        showChatSubmenu(true);
        showQuizSubmenu(false);
        setActiveUserFilter(null);
        setActiveCourseSubNavigation(null);
        setActiveChatSubNavigation(chatStatsButton);
        setActiveQuizSubNavigation(null);
        setActiveNavigation(chatButton);
        setTopbarContext("Chat analytics", "Statistiques Chat");
        showView("module");
        loadModuleView("/tn/esprit/fxml/chat/ChatStatisticsDashboard.fxml", null);
    }

    @FXML
    private void handleQuizNav() {
        showQuizList();
    }

    @FXML
    private void handleQuizListNav() {
        showQuizList();
    }

    @FXML
    private void handleQuizFormNav() {
        showQuizForm();
    }

    @FXML
    private void handleQuizQuestionsNav() {
        showQuizQuestions();
    }

    @FXML
    private void handleQuizQuestionFormNav() {
        showQuizQuestionForm();
    }

    @FXML
    private void handleQuizStatsNav() {
        showQuizStats();
    }

    private QuizBackOfficeView openQuizModule(String kicker, String title, Button activeSubButton) {
        showUserSubmenu(false);
        showCourseSubmenu(false);
        showQuizSubmenu(true);
        setActiveUserFilter(null);
        setActiveCourseSubNavigation(null);
        setActiveQuizSubNavigation(activeSubButton);
        setActiveNavigation(quizButton);
        setTopbarContext(kicker, title);
        showView("module");

        if (quizBackOfficeRoot == null) {
            try {
                DatabaseManager.initialize();
                QuizService quizService = new QuizService();
                QuizController quizController = new QuizController(quizService);
                QuestionController questionController = new QuestionController(new QuestionService(quizService));
                QuizResultController quizResultController = new QuizResultController(new QuizResultService());
                quizBackOfficeView = new QuizBackOfficeView(
                        quizController,
                        questionController,
                        quizResultController
                );
                quizBackOfficeRoot = quizBackOfficeView.buildDashboardEmbedded();
            } catch (RuntimeException exception) {
                showAlert(Alert.AlertType.ERROR, "Quiz", exception.getMessage());
                exception.printStackTrace();
                return null;
            }
        }

        moduleView.getChildren().setAll(quizBackOfficeRoot);
        return quizBackOfficeView;
    }

    public void showQuizList() {
        QuizBackOfficeView quizView = openQuizModule("Quiz space", "Liste des quiz", quizListButton);
        if (quizView != null) {
            quizView.showQuizList();
        }
    }

    public void showQuizForm() {
        QuizBackOfficeView quizView = openQuizModule("Quiz editor", "Creer un quiz", quizFormButton);
        if (quizView != null) {
            quizView.showQuizForm();
        }
    }

    public void showQuizQuestions() {
        QuizBackOfficeView quizView = openQuizModule("Quiz space", "Questions", quizQuestionsButton);
        if (quizView != null) {
            quizView.showQuestionList();
        }
    }

    public void showQuizQuestionForm() {
        QuizBackOfficeView quizView = openQuizModule("Question editor", "Creer une question", quizQuestionFormButton);
        if (quizView != null) {
            quizView.showQuestionForm();
        }
    }

    public void showQuizStats() {
        QuizBackOfficeView quizView = openQuizModule("Quiz analytics", "Statistiques", quizStatsButton);
        if (quizView != null) {
            quizView.showStatisticsPage();
        }
    }

    @FXML
    private void handleProfileNav() {
        showCourseSubmenu(false);
        showQuizSubmenu(false);
        Navigator.navigateTo("profile.fxml", Navigator.getStageFromNode(rootPane));
    }

    @FXML
    private void handleLogout() {
        try {
            MainFX.getInstance().showLoginView();
        } catch (Exception exception) {
            SessionManager.clearSession();
            Navigator.navigateTo("login.fxml", Navigator.getStageFromNode(rootPane));
            exception.printStackTrace();
        }
    }

    // ======================== VIEW SWITCHING ========================

    private void showView(String view) {
        dashboardView.setVisible("dashboard".equals(view));
        dashboardView.setManaged("dashboard".equals(view));
        userManagementView.setVisible("users".equals(view));
        userManagementView.setManaged("users".equals(view));
        editFormPane.setVisible(false);
        editFormPane.setManaged(false);
        moduleView.setVisible("module".equals(view));
        moduleView.setManaged("module".equals(view));
    }

    public void showCourses() {
        setModuleContext("Course space", "Cours");
        setActiveCourseSubNavigation(null);
        setActiveChatSubNavigation(null);
        loadModuleView("/tn/esprit/fxml/admin/courses.fxml", null);
    }

    public void showCreateCourse() {
        setModuleContext("Course editor", "Create Course");
        setActiveCourseSubNavigation(createCourseButton);
        setActiveChatSubNavigation(null);
        loadModuleView("/tn/esprit/fxml/admin/course-form.fxml", loader -> {
            CourseFormController controller = loader.getController();
            controller.initForCreate();
        });
    }

    public void showEditCourse(Course course) {
        setModuleContext("Course editor", "Edit Course");
        setActiveCourseSubNavigation(createCourseButton);
        setActiveChatSubNavigation(null);
        loadModuleView("/tn/esprit/fxml/admin/course-form.fxml", loader -> {
            CourseFormController controller = loader.getController();
            controller.initForEdit(course);
        });
    }

    public void showCourseSuccess(Course course, boolean updated) {
        setModuleContext("Course result", "Course saved");
        setActiveCourseSubNavigation(null);
        setActiveChatSubNavigation(null);
        loadModuleView("/tn/esprit/fxml/admin/course-success.fxml", loader -> {
            CourseSuccessController controller = loader.getController();
            controller.setResult(course, updated);
        });
    }

    public void showLessons() {
        setModuleContext("Lesson space", "Lessons");
        setActiveCourseSubNavigation(lessonsButton);
        setActiveChatSubNavigation(null);
        loadModuleView("/tn/esprit/fxml/admin/lessons.fxml", null);
    }

    public void showCreateLesson(Course preselectedCourse) {
        setModuleContext("Lesson editor", "Create Lesson");
        setActiveCourseSubNavigation(createLessonButton);
        setActiveChatSubNavigation(null);
        loadModuleView("/tn/esprit/fxml/admin/lesson-form.fxml", loader -> {
            LessonFormController controller = loader.getController();
            controller.initForCreate(preselectedCourse);
        });
    }

    public void showEditLesson(Lesson lesson) {
        setModuleContext("Lesson editor", "Edit Lesson");
        setActiveCourseSubNavigation(createLessonButton);
        setActiveChatSubNavigation(null);
        loadModuleView("/tn/esprit/fxml/admin/lesson-form.fxml", loader -> {
            LessonFormController controller = loader.getController();
            controller.initForEdit(lesson);
        });
    }

    public void showLessonSuccess(Lesson lesson, boolean updated) {
        setModuleContext("Lesson result", "Lesson saved");
        setActiveCourseSubNavigation(lessonsButton);
        setActiveChatSubNavigation(null);
        loadModuleView("/tn/esprit/fxml/admin/lesson-success.fxml", loader -> {
            LessonSuccessController controller = loader.getController();
            controller.setResult(lesson, updated);
        });
    }

    private void setModuleContext(String kicker, String title) {
        showUserSubmenu(false);
        showCourseSubmenu(true);
        showChatSubmenu(false);
        showQuizSubmenu(false);
        setActiveUserFilter(null);
        setActiveChatSubNavigation(null);
        setActiveQuizSubNavigation(null);
        setActiveNavigation(coursButton);
        setTopbarContext(kicker, title);
        showView("module");
    }

    private void loadModuleView(String fxmlPath, LoaderCallback callback) {
        try {
            FXMLLoader loader = new FXMLLoader(MainFX.class.getResource(fxmlPath));
            Parent view = loader.load();
            if (callback != null) {
                callback.accept(loader);
            }
            moduleView.getChildren().setAll(view);
        } catch (Exception exception) {
            String detail = exception.getMessage();
            if (detail == null || detail.isBlank()) {
                Throwable cause = exception.getCause();
                detail = cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()
                        ? cause.getMessage()
                        : exception.getClass().getSimpleName();
            }
            showAlert(Alert.AlertType.ERROR, "Navigation", "Unable to open the requested module.\n" + detail);
            exception.printStackTrace();
        }
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

    // ======================== DASHBOARD ========================

    private void loadDashboardStats() {
        int total = userService.countAll();
        int students = userService.countByRole(Role.ROLE_ELEVE);
        int parents = userService.countByRole(Role.ROLE_PARENT);
        int admins = userService.countByRole(Role.ROLE_ADMIN);
        int active = userService.countActive();

        totalUsersLabel.setText(String.valueOf(total));
        studentsLabel.setText(String.valueOf(students));
        parentsLabel.setText(String.valueOf(parents));
        adminsLabel.setText(String.valueOf(admins));
        activeUsersLabel.setText(active + " / " + total);
        activeProgressBar.setProgress(total > 0 ? (double) active / total : 0);

        // Pie chart
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        if (admins > 0) pieData.add(new PieChart.Data("Admin (" + admins + ")", admins));
        if (students > 0) pieData.add(new PieChart.Data("Student (" + students + ")", students));
        if (parents > 0) pieData.add(new PieChart.Data("Parent (" + parents + ")", parents));
        roleChart.setData(pieData);
        roleChart.setTitle("");

        // Recent users
        loadRecentUsers();
    }

    private void loadRecentUsers() {
        recentUsersBox.getChildren().clear();
        List<User> recent = userService.getRecentUsers(5);
        for (User user : recent) {
            HBox row = createRecentUserRow(user);
            recentUsersBox.getChildren().add(row);
        }
    }

    private HBox createRecentUserRow(User user) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.getStyleClass().add("dashboard-user-row");

        Label avatar = createAvatarLabel(user, "avatar-circle-sm");
        VBox info = new VBox(1);
        Label name = new Label(user.getFullName());
        name.getStyleClass().add("user-name");
        Label email = new Label(user.getEmail());
        email.getStyleClass().add("user-email");
        info.getChildren().addAll(name, email);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label roleBadge = createRoleBadge(user);
        Label statusBadge = createStatusBadge(user);

        row.getChildren().addAll(avatar, info, roleBadge, statusBadge);
        return row;
    }

    // ======================== USER MANAGEMENT ========================

    private void applyFilters() {
        String keyword = searchField.getText();
        String roleStr = roleFilterCombo.getValue();
        String sortStr = sortCombo.getValue();

        Role filter = currentRoleFilter;
        if (filter == null && roleStr != null) {
            filter = switch (roleStr) {
                case "Admin" -> Role.ROLE_ADMIN;
                case "Student (Eleve)" -> Role.ROLE_ELEVE;
                case "Parent" -> Role.ROLE_PARENT;
                default -> null;
            };
        }

        String sortBy = "id";
        String sortOrder = "DESC";
        if (sortStr != null) {
            switch (sortStr) {
                case "Oldest First" -> { sortBy = "id"; sortOrder = "ASC"; }
                case "Name A-Z" -> { sortBy = "first_name"; sortOrder = "ASC"; }
                case "Name Z-A" -> { sortBy = "first_name"; sortOrder = "DESC"; }
                case "Email A-Z" -> { sortBy = "email"; sortOrder = "ASC"; }
            }
        }

        List<User> users = userService.searchUsers(keyword, filter, sortBy, sortOrder);
        displayUsers(users);
    }

    private void displayUsers(List<User> users) {
        userListView.getItems().clear();
        for (User user : users) {
            userListView.getItems().add(createUserRow(user));
        }
    }

    private HBox createUserRow(User user) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 15, 10, 15));
        row.getStyleClass().add("user-row");

        // Avatar
        Label avatar = createAvatarLabel(user, "avatar-circle");
        avatar.setMinSize(40, 40);
        avatar.setMaxSize(40, 40);
        avatar.setAlignment(Pos.CENTER);

        // Info
        VBox info = new VBox(2);
        Label nameLabel = new Label(user.getFullName());
        nameLabel.getStyleClass().add("user-name");
        Label emailLabel = new Label(user.getEmail());
        emailLabel.getStyleClass().add("user-email");
        info.getChildren().addAll(nameLabel, emailLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Role badge
        Label roleBadge = createRoleBadge(user);

        // Status badge
        Label statusBadge = createStatusBadge(user);

        // Actions
        Button viewBtn = new Button("View");
        viewBtn.getStyleClass().addAll("btn-info", "btn-sm");
        viewBtn.setOnAction(e -> openEditForm(user, false));

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().addAll("btn-outline-primary", "btn-sm");
        editBtn.setOnAction(e -> openEditForm(user, false));

        // Ban/Unban - prevent self-block
        Button blockBtn = new Button(user.isActive() ? "Ban" : "Unban");
        blockBtn.getStyleClass().addAll(user.isActive() ? "btn-warning" : "btn-success", "btn-sm");
        boolean isSelf = SessionManager.getCurrentUser() != null && user.getId() == SessionManager.getCurrentUser().getId();
        blockBtn.setDisable(isSelf);
        blockBtn.setOnAction(e -> handleBlockUser(user));

        // Delete - prevent self-delete
        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().addAll("btn-danger", "btn-sm");
        deleteBtn.setDisable(isSelf);
        deleteBtn.setOnAction(e -> handleDeleteUser(user));

        HBox actions = new HBox(5, editBtn, blockBtn, deleteBtn);
        actions.setAlignment(Pos.CENTER);

        row.getChildren().addAll(avatar, info, roleBadge, statusBadge, actions);
        return row;
    }

    // ======================== ADD NEW USER ========================

    @FXML
    private void handleAddNewUser() {
        isCreateMode = true;
        selectedUser = null;
        editFormTitle.setText("Add New User");

        editFirstName.clear();
        editLastName.clear();
        editEmail.clear();
        editPassword.clear();
        editConfirmPassword.clear();

        // Set role based on current filter
        if (currentRoleFilter != null) {
            editRole.setValue(currentRoleFilter.getDisplayName().equals("Eleve") ? "Student (Eleve)" : currentRoleFilter.getDisplayName());
            editRole.setDisable(true);
        } else {
            editRole.setValue("Student (Eleve)");
            editRole.setDisable(false);
        }

        editActiveCheck.setSelected(true);
        editVerifiedCheck.setSelected(false);

        passwordSection.setVisible(true);
        passwordSection.setManaged(true);

        editFormPane.setVisible(true);
        editFormPane.setManaged(true);
    }

    // ======================== EDIT USER ========================

    private void openEditForm(User user, boolean viewOnly) {
        isCreateMode = false;
        selectedUser = user;
        editFormTitle.setText("Edit User");

        editFirstName.setText(user.getFirstName());
        editLastName.setText(user.getLastName());
        editEmail.setText(user.getEmail());

        String roleDisplay = user.getPrimaryRole().getDisplayName();
        editRole.setValue(roleDisplay.equals("Eleve") ? "Student (Eleve)" : roleDisplay);
        editRole.setDisable(false);

        editActiveCheck.setSelected(user.isActive());
        editVerifiedCheck.setSelected(user.isVerified());

        passwordSection.setVisible(false);
        passwordSection.setManaged(false);
        editPassword.clear();
        editConfirmPassword.clear();

        editFormPane.setVisible(true);
        editFormPane.setManaged(true);
    }

    @FXML
    private void handleSaveUser() {
        String firstName = editFirstName.getText().trim();
        String lastName = editLastName.getText().trim();
        String email = editEmail.getText().trim();

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "First name, last name, and email are required.");
            return;
        }

        if (firstName.length() < 2 || !firstName.matches("[a-zA-Z\\s]+")) {
            showAlert(Alert.AlertType.WARNING, "Validation", "First name: min 2 chars, letters only.");
            return;
        }

        if (lastName.length() < 2 || !lastName.matches("[a-zA-Z\\s]+")) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Last name: min 2 chars, letters only.");
            return;
        }

        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Please enter a valid email address.");
            return;
        }

        String roleStr = editRole.getValue();
        Role newRole = switch (roleStr) {
            case "Admin" -> Role.ROLE_ADMIN;
            case "Parent" -> Role.ROLE_PARENT;
            default -> Role.ROLE_ELEVE;
        };

        if (isCreateMode) {
            String pwd = editPassword.getText();
            String confirmPwd = editConfirmPassword.getText();

            if (pwd.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Password is required for new users.");
                return;
            }
            if (pwd.length() < 8) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Password must be at least 8 characters.");
                return;
            }
            if (!pwd.equals(confirmPwd)) {
                showAlert(Alert.AlertType.WARNING, "Validation", "Passwords do not match.");
                return;
            }
            if (userService.emailExists(email)) {
                showAlert(Alert.AlertType.WARNING, "Validation", "This email is already registered.");
                return;
            }

            User newUser = new User(email, pwd, firstName, lastName, java.util.List.of(newRole));
            newUser.setActive(editActiveCheck.isSelected());
            newUser.setVerified(editVerifiedCheck.isSelected());
            userService.add(newUser);
            showAlert(Alert.AlertType.INFORMATION, "Success", "User created successfully.");
        } else {
            if (userService.emailExistsExcluding(email, selectedUser.getId())) {
                showAlert(Alert.AlertType.WARNING, "Validation", "This email is already in use by another user.");
                return;
            }

            selectedUser.setFirstName(firstName);
            selectedUser.setLastName(lastName);
            selectedUser.setEmail(email);
            selectedUser.setRoles(java.util.List.of(newRole));
            selectedUser.setActive(editActiveCheck.isSelected());
            selectedUser.setVerified(editVerifiedCheck.isSelected());
            userService.update(selectedUser);
            showAlert(Alert.AlertType.INFORMATION, "Success", "User updated successfully.");
        }

        editFormPane.setVisible(false);
        editFormPane.setManaged(false);
        applyFilters();
        loadDashboardStats();
    }

    @FXML
    private void handleCancelEdit() {
        editFormPane.setVisible(false);
        editFormPane.setManaged(false);
        selectedUser = null;
    }

    // ======================== BLOCK / DELETE ========================

    private void handleBlockUser(User user) {
        String action = user.isActive() ? "ban" : "unban";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to " + action + " " + user.getFullName() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(action.substring(0, 1).toUpperCase() + action.substring(1) + " User");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                userService.toggleBlock(user.getId());
                applyFilters();
                loadDashboardStats();
            }
        });
    }

    private void handleDeleteUser(User user) {
        // Prevent self-delete
        if (SessionManager.getCurrentUser() != null && user.getId() == SessionManager.getCurrentUser().getId()) {
            showAlert(Alert.AlertType.WARNING, "Cannot Delete", "You cannot delete your own account.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Are you sure you want to permanently delete " + user.getFullName() + "?\nThis action cannot be undone.",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Delete User");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                userService.delete(user.getId());
                applyFilters();
                loadDashboardStats();
            }
        });
    }

    // ======================== HELPERS ========================

    private Label createAvatarLabel(User user, String styleClass) {
        String initials = buildInitials(user);
        Label avatar = new Label(initials);
        avatar.getStyleClass().add(styleClass);
        avatar.setAlignment(Pos.CENTER);
        return avatar;
    }

    private void setActiveNavigation(Button activeButton) {
        List<Button> buttons = List.of(
                dashboardButton,
                allUsersButton,
                coursButton,
                produitsButton,
                chatButton,
                quizButton
        );

        for (Button button : buttons) {
            button.getStyleClass().remove("shell-nav-button-active");
        }

        if (activeButton != null && !activeButton.getStyleClass().contains("shell-nav-button-active")) {
            activeButton.getStyleClass().add("shell-nav-button-active");
        }
    }

    private void setActiveChatSubNavigation(Button activeButton) {
        List<Button> buttons = List.of(
                chatMessageButton,
                chatStatsButton
        );

        for (Button button : buttons) {
            button.getStyleClass().remove("shell-nav-button-active");
        }

        if (activeButton != null && !activeButton.getStyleClass().contains("shell-nav-button-active")) {
            activeButton.getStyleClass().add("shell-nav-button-active");
        }
    }

    private void setActiveUserFilter(Button activeButton) {
        List<Button> buttons = List.of(
                adminsButton,
                studentsButton,
                parentsButton
        );

        for (Button button : buttons) {
            button.getStyleClass().remove("shell-nav-sub-button-active");
        }

        if (activeButton != null && !activeButton.getStyleClass().contains("shell-nav-sub-button-active")) {
            activeButton.getStyleClass().add("shell-nav-sub-button-active");
        }
    }

    private void setActiveCourseSubNavigation(Button activeButton) {
        List<Button> buttons = List.of(
                createCourseButton,
                lessonsButton,
                createLessonButton
        );

        for (Button button : buttons) {
            button.getStyleClass().remove("shell-nav-sub-button-active");
        }

        if (activeButton != null && !activeButton.getStyleClass().contains("shell-nav-sub-button-active")) {
            activeButton.getStyleClass().add("shell-nav-sub-button-active");
        }
    }

    private void setActiveProduitsSubNavigation(Button activeButton) {
        List<Button> buttons = List.of(
                produitsCommandesButton,
                produitsListButton,
                produitsFormButton,
                produitsCategoriesButton,
                produitsCategoryFormButton
        );

        for (Button button : buttons) {
            button.getStyleClass().remove("shell-nav-sub-button-active");
        }

        if (activeButton != null && !activeButton.getStyleClass().contains("shell-nav-sub-button-active")) {
            activeButton.getStyleClass().add("shell-nav-sub-button-active");
        }
    }

    private void setActiveQuizSubNavigation(Button activeButton) {
        List<Button> buttons = List.of(
                quizListButton,
                quizFormButton,
                quizQuestionsButton,
                quizQuestionFormButton,
                quizStatsButton
        );

        for (Button button : buttons) {
            button.getStyleClass().remove("shell-nav-sub-button-active");
        }

        if (activeButton != null && !activeButton.getStyleClass().contains("shell-nav-sub-button-active")) {
            activeButton.getStyleClass().add("shell-nav-sub-button-active");
        }
    }

    private void showUserSubmenu(boolean visible) {
        userSubmenuBox.setVisible(visible);
        userSubmenuBox.setManaged(visible);
    }

    private void showCourseSubmenu(boolean visible) {
        courseSubmenuBox.setVisible(visible);
        courseSubmenuBox.setManaged(visible);
    }

    private void showProduitsSubmenu(boolean visible) {
        produitsSubmenuBox.setVisible(visible);
        produitsSubmenuBox.setManaged(visible);
    }

    private void showChatSubmenu(boolean visible) {
        chatSubmenuBox.setVisible(visible);
        chatSubmenuBox.setManaged(visible);
    }

    private void showQuizSubmenu(boolean visible) {
        quizSubmenuBox.setVisible(visible);
        quizSubmenuBox.setManaged(visible);
    }

    private void setTopbarContext(String kicker, String title) {
        topbarKickerLabel.setText(kicker);
        topbarTitleLabel.setText(title);
    }

    @FunctionalInterface
    private interface LoaderCallback {
        void accept(FXMLLoader loader);
    }

    @FunctionalInterface
    private interface EcomModuleAction {
        void apply(com.ecom.ui.MainController controller);
    }

    private String buildInitials(User user) {
        if (user == null) {
            return "?";
        }

        String initials = "";
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            initials += user.getFirstName().substring(0, 1).toUpperCase();
        }
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            initials += user.getLastName().substring(0, 1).toUpperCase();
        }
        return initials.isEmpty() ? "?" : initials;
    }

    private Label createRoleBadge(User user) {
        Label badge = new Label(user.getPrimaryRole().getDisplayName());
        String roleClass = switch (user.getPrimaryRole()) {
            case ROLE_ADMIN -> "badge-admin";
            case ROLE_PARENT -> "badge-parent";
            case ROLE_ELEVE -> "badge-eleve";
        };
        badge.getStyleClass().addAll("badge", roleClass);
        return badge;
    }

    private Label createStatusBadge(User user) {
        Label badge = new Label(user.isActive() ? "Active" : "Banned");
        badge.getStyleClass().addAll("badge", user.isActive() ? "badge-active" : "badge-inactive");
        return badge;
    }

    private void showAlert(Alert.AlertType type, String header, String content) {
        Alert alert = new Alert(type, content, ButtonType.OK);
        alert.setHeaderText(header);
        alert.showAndWait();
    }
}
