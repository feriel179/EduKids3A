package com.edukids.edukids3a.controller;

import com.edukids.edukids3a.model.Conversation;
import com.edukids.edukids3a.model.ConversationParticipant;
import com.edukids.edukids3a.model.User;
import com.edukids.edukids3a.service.ChatService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class GroupMembersDialogController {

    public enum Mode {
        ADD,
        REMOVE
    }

    private static final int SEARCH_LIMIT = 12;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withLocale(Locale.FRENCH);

    private final ObservableList<User> addResults = FXCollections.observableArrayList();
    private final ObservableList<MemberEntry> members = FXCollections.observableArrayList();

    private ChatService chatService;
    private Conversation conversation;
    private User currentUser;
    private Runnable onUpdated;
    private Stage stage;
    private Mode initialMode = Mode.ADD;

    @FXML
    private Label lblDialogTitle;
    @FXML
    private Label lblDialogSubtitle;
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab tabAdd;
    @FXML
    private Tab tabRemove;
    @FXML
    private TextField tfAddSearch;
    @FXML
    private ListView<User> lvAddResults;
    @FXML
    private ListView<MemberEntry> lvGroupMembers;
    @FXML
    private Button btnAddSelected;
    @FXML
    private Button btnRemoveSelected;

    @FXML
    private void initialize() {
        lvAddResults.setItems(addResults);
        lvGroupMembers.setItems(members);

        lvAddResults.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label name = new Label(displayName(item));
                name.getStyleClass().add("chat-user-name");
                Label email = new Label(Optional.ofNullable(item.getEmail()).orElse(""));
                email.getStyleClass().add("chat-user-email");
                VBox box = new VBox(2, name, email);
                box.getStyleClass().add("chat-user-result");
                setGraphic(box);
                setText(null);
            }
        });

        lvGroupMembers.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(MemberEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label name = new Label(displayName(item.user()));
                name.getStyleClass().add("chat-user-name");
                Label email = new Label(Optional.ofNullable(item.user().getEmail()).orElse(""));
                email.getStyleClass().add("chat-user-email");
                Label role = new Label(roleLabel(item.role()));
                role.getStyleClass().add("chat-member-role");
                VBox box = new VBox(2, name, email, role);
                box.getStyleClass().add("chat-user-result");
                setGraphic(box);
                setText(null);
            }
        });

        tfAddSearch.textProperty().addListener((obs, oldValue, newValue) -> refreshAddResults());
        lvAddResults.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                onAddSelected();
            }
        });
        lvGroupMembers.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                onRemoveSelected();
            }
        });
    }

    public void setContext(ChatService chatService, Conversation conversation, User currentUser, Mode initialMode, Runnable onUpdated, Stage stage) {
        this.chatService = chatService;
        this.conversation = conversation;
        this.currentUser = currentUser;
        this.initialMode = initialMode == null ? Mode.ADD : initialMode;
        this.onUpdated = onUpdated;
        this.stage = stage;

        lblDialogTitle.setText("Gestion des membres");
        lblDialogSubtitle.setText(conversation != null && conversation.getTitle() != null && !conversation.getTitle().isBlank()
                ? conversation.getTitle()
                : "Conversation de groupe");

        if (this.initialMode == Mode.ADD) {
            tabPane.getSelectionModel().select(tabAdd);
        } else {
            tabPane.getSelectionModel().select(tabRemove);
        }

        refreshMembers();
        refreshAddResults();
    }

    @FXML
    private void onAddSelected() {
        User selected = lvAddResults.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        try {
            chatService.addGroupMember(conversation.getId(), selected.getId().longValue(), currentUser.getId().longValue());
            refreshMembers();
            refreshAddResults();
            inform("Membre ajouté.");
            if (onUpdated != null) {
                onUpdated.run();
            }
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    @FXML
    private void onRemoveSelected() {
        MemberEntry selected = lvGroupMembers.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        if (selected.user() != null && currentUser != null && currentUser.getId() != null
                && Objects.equals(selected.user().getId(), currentUser.getId())) {
            error("Vous ne pouvez pas vous supprimer vous-même.");
            return;
        }

        try {
            chatService.removeGroupMember(conversation.getId(), selected.user().getId().longValue(), currentUser.getId().longValue());
            refreshMembers();
            refreshAddResults();
            inform("Membre supprimé.");
            if (onUpdated != null) {
                onUpdated.run();
            }
        } catch (Exception ex) {
            error(ex.getMessage());
        }
    }

    @FXML
    private void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private void refreshAddResults() {
        if (conversation == null || chatService == null || currentUser == null) {
            addResults.clear();
            return;
        }
        String query = Optional.ofNullable(tfAddSearch.getText()).orElse("").trim();
        if (query.isBlank()) {
            addResults.clear();
            return;
        }
        addResults.setAll(chatService.searchUsersForGroupAdd(conversation.getId(), query, currentUser.getId().longValue(), SEARCH_LIMIT));
    }

    private void refreshMembers() {
        if (conversation == null || chatService == null) {
            members.clear();
            return;
        }

        List<ConversationParticipant> participants = chatService.loadParticipants(conversation.getId());
        Map<Long, User> users = chatService.findUsersByIds(participants.stream()
                .map(ConversationParticipant::getUserId)
                .filter(Objects::nonNull)
                .toList());

        Map<Long, MemberEntry> ordered = new LinkedHashMap<>();
        for (ConversationParticipant participant : participants) {
            User user = users.get(participant.getUserId());
            if (user != null) {
                ordered.put(user.getId().longValue(), new MemberEntry(user, participant.getRole(), participant.getJoinedAt()));
            }
        }
        members.setAll(ordered.values());
    }

    private static String displayName(User user) {
        if (user == null) {
            return "Utilisateur";
        }
        String name = Optional.ofNullable(user.getNom()).orElse("");
        if (name.isBlank()) {
            name = Optional.ofNullable(user.getEmail()).orElse("Utilisateur");
        }
        return name;
    }

    private static String roleLabel(String role) {
        if (role == null || role.isBlank()) {
            return "Membre";
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "owner" -> "Administrateur";
            case "admin" -> "Admin";
            default -> "Membre";
        };
    }

    private static void inform(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private record MemberEntry(User user, String role, java.time.LocalDateTime joinedAt) {
    }
}
