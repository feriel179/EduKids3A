package com.edukids.edukids3a.controller;

import com.edukids.edukids3a.model.Conversation;
import com.edukids.edukids3a.model.ConversationParticipant;
import com.edukids.edukids3a.model.User;
import com.edukids.edukids3a.service.ChatService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class GroupMembersViewDialogController {

    private final ObservableList<MemberRow> members = FXCollections.observableArrayList();

    private ChatService chatService;
    private Conversation conversation;
    private User currentUser;
    private Stage stage;

    @FXML
    private Label lblTitle;
    @FXML
    private Label lblSubtitle;
    @FXML
    private Label lblCount;
    @FXML
    private TableView<MemberRow> tableMembers;
    @FXML
    private TableColumn<MemberRow, String> colName;
    @FXML
    private TableColumn<MemberRow, String> colEmail;
    @FXML
    private TableColumn<MemberRow, String> colRole;
    @FXML
    private Button btnClose;

    @FXML
    private void initialize() {
        tableMembers.setItems(members);
        colName.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(() -> displayName(cell.getValue().user())));
        colEmail.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(() -> Optional.ofNullable(cell.getValue().user().getEmail()).orElse("")));
        colRole.setCellValueFactory(cell -> javafx.beans.binding.Bindings.createStringBinding(() -> roleLabel(cell.getValue().role())));
    }

    public void setContext(ChatService chatService, Conversation conversation, User currentUser, Stage stage) {
        this.chatService = chatService;
        this.conversation = conversation;
        this.currentUser = currentUser;
        this.stage = stage;

        String title = conversation != null && conversation.getTitle() != null && !conversation.getTitle().isBlank()
                ? conversation.getTitle()
                : "Conversation de groupe";
        lblTitle.setText("Membres du groupe");
        lblSubtitle.setText(title);

        refreshMembers();
    }

    @FXML
    private void onClose() {
        if (stage != null) {
            stage.close();
        }
    }

    private void refreshMembers() {
        if (conversation == null || chatService == null) {
            members.clear();
            lblCount.setText("0 membre");
            return;
        }

        List<ConversationParticipant> participants = chatService.loadParticipants(conversation.getId()).stream()
                .filter(p -> p.getDeletedAt() == null)
                .toList();
        Map<Long, User> users = chatService.findUsersByIds(participants.stream()
                .map(ConversationParticipant::getUserId)
                .filter(Objects::nonNull)
                .toList());

        Map<Long, MemberRow> ordered = new LinkedHashMap<>();
        for (ConversationParticipant participant : participants) {
            User user = users.get(participant.getUserId());
            if (user != null) {
                ordered.put(user.getId().longValue(), new MemberRow(user, participant.getRole()));
            }
        }
        members.setAll(ordered.values());
        lblCount.setText(members.size() + " membre" + (members.size() > 1 ? "s" : ""));
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

    public record MemberRow(User user, String role) {
    }
}
