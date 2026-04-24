package com.edukids.controllers;

import com.edukids.entities.User;
import com.edukids.enums.Role;
import com.edukids.services.UserService;
import com.edukids.utils.Navigator;
import com.edukids.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import tn.esprit.MainFX;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // --- Sidebar ---
    @FXML private Label currentUserLabel;
    @FXML private Button dashboardButton;
    @FXML private Button allUsersButton;
    @FXML private VBox userSubmenuBox;
    @FXML private Button adminsButton;
    @FXML private Button studentsButton;
    @FXML private Button parentsButton;
    @FXML private HBox rootPane;
    @FXML private Label topbarRoleLabel;
    @FXML private Label topbarNameLabel;
    @FXML private Label topbarAvatarLabel;

    // --- Main content area ---
    @FXML private StackPane contentArea;

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

    @Override
    public void initialize(URL url, ResourceBundle rb) {
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
        setActiveUserFilter(null);

        setActiveNavigation(dashboardButton);
        loadDashboardStats();

        // Live search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    // ======================== SIDEBAR NAV ========================

    @FXML
    private void handleDashboardNav() {
        showUserSubmenu(false);
        setActiveUserFilter(null);
        setActiveNavigation(dashboardButton);
        showView("dashboard");
        loadDashboardStats();
    }

    @FXML
    private void handleAllUsersNav() {
        currentRoleFilter = null;
        userMgmtTitle.setText("Users Management");
        roleFilterCombo.setValue("All Roles");
        showUserSubmenu(true);
        setActiveUserFilter(null);
        setActiveNavigation(allUsersButton);
        showView("users");
        applyFilters();
    }

    @FXML
    private void handleAdminsNav() {
        currentRoleFilter = Role.ROLE_ADMIN;
        userMgmtTitle.setText("Admins Management");
        roleFilterCombo.setValue("Admin");
        showUserSubmenu(true);
        setActiveUserFilter(adminsButton);
        setActiveNavigation(allUsersButton);
        showView("users");
        applyFilters();
    }

    @FXML
    private void handleStudentsNav() {
        currentRoleFilter = Role.ROLE_ELEVE;
        userMgmtTitle.setText("Students Management");
        roleFilterCombo.setValue("Student (Eleve)");
        showUserSubmenu(true);
        setActiveUserFilter(studentsButton);
        setActiveNavigation(allUsersButton);
        showView("users");
        applyFilters();
    }

    @FXML
    private void handleParentsNav() {
        currentRoleFilter = Role.ROLE_PARENT;
        userMgmtTitle.setText("Parents Management");
        roleFilterCombo.setValue("Parent");
        showUserSubmenu(true);
        setActiveUserFilter(parentsButton);
        setActiveNavigation(allUsersButton);
        showView("users");
        applyFilters();
    }

    @FXML
    private void handleCoursNav() {
        try {
            MainFX.getInstance().showAdminCourses();
        } catch (Exception exception) {
            showAlert(Alert.AlertType.ERROR, "Navigation", "Unable to open the courses module.");
            exception.printStackTrace();
        }
    }

    @FXML
    private void handleEventsNav() {
        // TODO: Navigate to Events management
    }

    @FXML
    private void handleProduitsNav() {
        // TODO: Navigate to Produits management
    }

    @FXML
    private void handleChatNav() {
        // TODO: Navigate to Chat management
    }

    @FXML
    private void handleProfileNav() {
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
                allUsersButton
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

    private void showUserSubmenu(boolean visible) {
        userSubmenuBox.setVisible(visible);
        userSubmenuBox.setManaged(visible);
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
