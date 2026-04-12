package com.edukids.edukids3a.controller;

import com.edukids.edukids3a.model.Conversation;
import com.edukids.edukids3a.model.Message;
import com.edukids.edukids3a.model.User;
import com.edukids.edukids3a.security.AuthSession;
import com.edukids.edukids3a.service.ChatService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class ChatController {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm").withLocale(Locale.FRENCH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int SEARCH_LIMIT = 12;

    private final ChatService chatService = new ChatService();
    private final ObservableList<ConversationItem> conversations = FXCollections.observableArrayList();
    private final ObservableList<MessageItem> messages = FXCollections.observableArrayList();
    private final ObservableList<User> selectedParticipants = FXCollections.observableArrayList();

    private final ToggleGroup conversationModeGroup = new ToggleGroup();

    private User currentUser;
    private Conversation currentConversation;

    @FXML
    private Label lblCurrentUser;
    @FXML
    private Label lblChatStatus;
    @FXML
    private Label lblConversationCount;
    @FXML
    private Button btnNewConversation;
    @FXML
    private Button btnRefreshConversations;
    @FXML
    private Button btnConversationMenu;
    @FXML
    private TextField tfConversationSearch;
    @FXML
    private ListView<ConversationItem> lvConversations;
    @FXML
    private Label lblConversationTitle;
    @FXML
    private Label lblConversationSubtitle;
    @FXML
    private ListView<MessageItem> lvMessages;
    @FXML
    private Label lblMessagesEmpty;
    @FXML
    private TextArea taMessageDraft;
    @FXML
    private Button btnSendMessage;
    @FXML
    private Button btnEmoji;
    @FXML
    private Button btnAttachFile;
    @FXML
    private Button btnClearAttachment;
    @FXML
    private HBox boxAttachmentPreview;
    @FXML
    private Label lblSelectedAttachment;
    @FXML
    private VBox paneCreateConversation;
    @FXML
    private RadioButton rbPrivateConversation;
    @FXML
    private RadioButton rbGroupConversation;
    @FXML
    private VBox boxGroupName;
    @FXML
    private TextField tfConversationGroupName;
    @FXML
    private TextField tfUserSearch;
    @FXML
    private ListView<User> lvUserSearchResults;
    @FXML
    private FlowPane fpSelectedParticipants;
    @FXML
    private Label lblCreateHint;
    @FXML
    private Button btnAddSelectedUser;
    @FXML
    private Button btnCreateConversation;
    @FXML
    private Button btnCancelCreate;

    private String selectedAttachmentPath;
    private String selectedAttachmentName;
    private Popup emojiPopup;
    private TextField tfEmojiSearch;
    private final Map<String, List<String>> emojiCategories = new LinkedHashMap<>();
    private final List<Button> emojiButtons = new ArrayList<>();
    private ContextMenu conversationMenu;

    @FXML
    private void initialize() {
        currentUser = AuthSession.getCurrentUser();

        rbPrivateConversation.setToggleGroup(conversationModeGroup);
        rbGroupConversation.setToggleGroup(conversationModeGroup);
        rbPrivateConversation.setSelected(true);

        lvConversations.setItems(conversations);
        lvMessages.setItems(messages);
        lvUserSearchResults.setItems(FXCollections.observableArrayList());

        lvConversations.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ConversationItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label title = new Label(item.title());
                title.getStyleClass().add("chat-conversation-title");

                Label preview = new Label(item.preview());
                preview.getStyleClass().add("chat-conversation-preview");
                preview.setWrapText(true);

                Label meta = new Label(item.meta());
                meta.getStyleClass().add("chat-conversation-meta");

                VBox box = new VBox(4, title, preview, meta);
                box.getStyleClass().add("chat-conversation-cell");
                box.setPadding(new Insets(12));
                setText(null);
                setGraphic(box);
            }
        });

        lvMessages.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(MessageItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                Label sender = new Label(item.senderName());
                sender.getStyleClass().add("chat-message-sender");

                Label time = new Label(item.timeText());
                time.getStyleClass().add("chat-message-time");

                HBox header = new HBox(10, sender, new Region(), time);
                HBox.setHgrow(header.getChildren().get(1), javafx.scene.layout.Priority.ALWAYS);
                header.getStyleClass().add("chat-message-header");

                Label content = new Label(item.message().getContent());
                content.getStyleClass().add("chat-message-content");
                content.setWrapText(true);

                VBox bubble = new VBox(6, header, content);
                bubble.getStyleClass().addAll("chat-message-bubble", item.mine() ? "chat-message-mine" : "chat-message-other");

                HBox row = new HBox(8);
                row.setPadding(new Insets(8, 4, 8, 4));
                row.setAlignment(item.mine() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                HBox.setHgrow(bubble, Priority.NEVER);

                if (item.mine()) {
                    Button menuButton = new Button("⋮");
                    menuButton.getStyleClass().add("chat-message-menu-button");
                    menuButton.setOnAction(e -> showOwnMessageMenu(menuButton, item));
                    row.getChildren().add(menuButton);
                }
                row.getChildren().add(bubble);

                setText(null);
                setGraphic(row);
            }
        });

        lvUserSearchResults.setCellFactory(list -> new ListCell<>() {
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

        lvUserSearchResults.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                addSelectedUserFromResults();
            }
        });

        tfConversationSearch.textProperty().addListener((obs, oldValue, newValue) -> filterConversations());
        tfUserSearch.textProperty().addListener((obs, oldValue, newValue) -> refreshUserSearch());

        conversationModeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> updateCreateMode());

        lvConversations.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null) {
                selectConversation(newValue.conversation());
            }
        });

        btnSendMessage.setDisable(true);
        taMessageDraft.addEventFilter(KeyEvent.KEY_PRESSED, this::handleMessageShortcut);
        taMessageDraft.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (focused) {
                taMessageDraft.getStyleClass().remove("chat-compose-output");
            }
        });
        setupEmojiPicker();
        setupConversationMenu();

        if (currentUser != null) {
            lblCurrentUser.setText(displayName(currentUser) + " • " + Optional.ofNullable(currentUser.getRole()).orElse("utilisateur"));
            lblChatStatus.setText("Messagerie connectée");
        } else {
            lblCurrentUser.setText("Aucun utilisateur connecté");
            lblChatStatus.setText("Session absente");
            btnNewConversation.setDisable(true);
            btnSendMessage.setDisable(true);
            taMessageDraft.setDisable(true);
        }

        showCreatePane(false);
        refreshAll();
    }

    @FXML
    private void onRefreshConversations() {
        refreshAll();
    }

    @FXML
    private void onNewConversation() {
        if (currentUser == null) {
            info("Connectez-vous pour créer une conversation.");
            return;
        }
        clearCreateForm();
        showCreatePane(true);
        refreshUserSearch();
    }

    @FXML
    private void onCancelNewConversation() {
        showCreatePane(false);
    }

    @FXML
    private void onAddSelectedUser() {
        addSelectedUserFromResults();
    }

    @FXML
    private void onCreateConversation() {
        if (currentUser == null) {
            erreur("Aucun utilisateur connecté.");
            return;
        }

        try {
            boolean group = rbGroupConversation.isSelected();
            List<Long> participantIds = selectedParticipants.stream()
                    .map(User::getId)
                    .filter(Objects::nonNull)
                    .map(Integer::longValue)
                    .distinct()
                    .toList();

            Conversation created;
            if (group) {
                String title = Optional.ofNullable(tfConversationGroupName.getText()).orElse("").trim();
                if (title.isBlank()) {
                    throw new IllegalArgumentException("Le nom du groupe est obligatoire.");
                }
                if (participantIds.isEmpty()) {
                    throw new IllegalArgumentException("Ajoutez au moins un autre utilisateur au groupe.");
                }
                created = chatService.createConversation(currentUser.getId().longValue(), title, true, participantIds);
            } else {
                if (participantIds.size() != 1) {
                    throw new IllegalArgumentException("Une conversation privée nécessite exactement un destinataire.");
                }
                created = chatService.createConversation(currentUser.getId().longValue(), null, false, participantIds);
            }

            showCreatePane(false);
            refreshAll();
            selectConversationById(created.getId());
        } catch (Exception ex) {
            erreur(ex.getMessage());
        }
    }

    @FXML
    private void onSendMessage() {
        if (currentUser == null) {
            erreur("Aucun utilisateur connecté.");
            return;
        }
        if (currentConversation == null || currentConversation.getId() == null) {
            erreur("Sélectionnez une conversation.");
            return;
        }

        String draft = Optional.ofNullable(taMessageDraft.getText()).orElse("").trim();
        if (draft.isBlank()) {
            return;
        }

        if (selectedAttachmentName != null && !selectedAttachmentName.isBlank()) {
            draft = draft + System.lineSeparator() + "[Fichier joint] " + selectedAttachmentName;
        }

        try {
            chatService.sendMessage(currentConversation.getId(), currentUser.getId().longValue(), draft);
            taMessageDraft.clear();
            clearAttachmentSelection();
            refreshCurrentConversation();
            refreshConversationsOnly();
        } catch (Exception ex) {
            erreur(ex.getMessage());
        }
    }

    @FXML
    private void onConversationModeChanged() {
        updateCreateMode();
    }

    private void showOwnMessageMenu(Button anchor, MessageItem item) {
        if (anchor == null || item == null || item.message() == null || currentUser == null || currentUser.getId() == null) {
            return;
        }
        if (!Objects.equals(item.message().getSenderId(), currentUser.getId().longValue())) {
            erreur("Vous ne pouvez modifier ou supprimer que vos propres messages.");
            return;
        }

        ContextMenu menu = new ContextMenu();
        MenuItem edit = new MenuItem("Modifier");
        edit.setOnAction(e -> onEditMessage(item));
        MenuItem delete = new MenuItem("Supprimer");
        delete.setOnAction(e -> onDeleteMessage(item));
        menu.getItems().addAll(edit, delete);

        if (anchor.getScene() == null) {
            return;
        }
        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds == null) {
            return;
        }
        menu.show(anchor, bounds.getMinX(), bounds.getMaxY() + 6);
    }

    private void onEditMessage(MessageItem item) {
        if (!canEditDeleteMessage(item)) {
            erreur("Vous ne pouvez modifier ou supprimer que vos propres messages.");
            return;
        }

        Optional<String> updated = showEditMessageDialog(item.message().getContent());
        if (updated.isEmpty()) {
            return;
        }

        try {
            chatService.editMessage(item.message().getId(), currentUser.getId().longValue(), updated.get());
            refreshCurrentConversation();
            refreshConversationsOnly();
        } catch (Exception ex) {
            erreur(ex.getMessage());
        }
    }

    private void onDeleteMessage(MessageItem item) {
        if (!canEditDeleteMessage(item)) {
            erreur("Vous ne pouvez modifier ou supprimer que vos propres messages.");
            return;
        }

        if (!confirmer("Supprimer ce message ?")) {
            return;
        }

        try {
            chatService.deleteMessage(item.message().getId(), currentUser.getId().longValue());
            refreshCurrentConversation();
            refreshConversationsOnly();
        } catch (Exception ex) {
            erreur(ex.getMessage());
        }
    }

    @FXML
    private void onConversationMenu() {
        updateConversationMenuState();
        if (conversationMenu == null || btnConversationMenu == null || !btnConversationMenu.isVisible()) {
            return;
        }
        if (btnConversationMenu.getScene() == null) {
            return;
        }
        var bounds = btnConversationMenu.localToScreen(btnConversationMenu.getBoundsInLocal());
        if (bounds == null) {
            return;
        }
        conversationMenu.show(btnConversationMenu, bounds.getMinX(), bounds.getMaxY() + 6);
    }

    @FXML
    private void onEmojiPicker() {
        if (btnEmoji == null) {
            return;
        }
        if (emojiPopup != null && emojiPopup.isShowing()) {
            emojiPopup.hide();
            return;
        }

        if (emojiPopup == null) {
            setupEmojiPicker();
        }

        Window window = btnEmoji.getScene() != null ? btnEmoji.getScene().getWindow() : null;
        if (window == null) {
            return;
        }

        var bounds = btnEmoji.localToScreen(btnEmoji.getBoundsInLocal());
        if (bounds == null) {
            return;
        }
        double x = bounds.getMinX();
        double y = bounds.getMaxY() + 8;
        emojiPopup.show(window, x, y);
        if (tfEmojiSearch != null) {
            tfEmojiSearch.requestFocus();
        }
    }

    @FXML
    private void onAttachFile() {
        Window window = btnAttachFile != null && btnAttachFile.getScene() != null
                ? btnAttachFile.getScene().getWindow()
                : null;
        if (window == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importer un fichier");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt")
        );

        File file = chooser.showOpenDialog(window);
        if (file == null) {
            return;
        }

        selectedAttachmentPath = file.getAbsolutePath();
        selectedAttachmentName = file.getName();
        updateAttachmentPreview();
    }

    @FXML
    private void onClearAttachment() {
        clearAttachmentSelection();
    }

    private void refreshAll() {
        refreshConversationsOnly();
        if (currentConversation != null) {
            selectConversationById(currentConversation.getId());
        } else if (!conversations.isEmpty()) {
            lvConversations.getSelectionModel().selectFirst();
        } else {
            showEmptyConversationState();
        }
        refreshUserSearch();
    }

    private void refreshConversationsOnly() {
        if (currentUser == null) {
            conversations.clear();
            lblConversationCount.setText("0 conversation");
            return;
        }

        Long keepId = Optional.ofNullable(currentConversation).map(Conversation::getId).orElse(null);
        List<Conversation> loaded = chatService.listConversationsForUser(currentUser.getId().longValue());
        Set<Long> participantIds = loaded.stream()
                .filter(Objects::nonNull)
                .flatMap(conv -> conv.getParticipants().stream())
                .map(participant -> participant.getUserId())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, User> userById = chatService.findUsersByIds(participantIds);

        List<ConversationItem> items = new ArrayList<>();
        for (Conversation conversation : loaded) {
            items.add(buildConversationItem(conversation, userById));
        }
        conversations.setAll(items);
        lblConversationCount.setText(items.size() + " conversation" + (items.size() > 1 ? "s" : ""));

        if (keepId != null) {
            boolean found = selectConversationById(keepId);
            if (!found) {
                currentConversation = null;
                showEmptyConversationState();
            }
        } else if (!items.isEmpty() && lvConversations.getSelectionModel().getSelectedIndex() < 0) {
            lvConversations.getSelectionModel().selectFirst();
        }
        filterConversations();
    }

    private void filterConversations() {
        String query = Optional.ofNullable(tfConversationSearch.getText()).orElse("").trim().toLowerCase(Locale.ROOT);
        if (query.isBlank()) {
            lvConversations.setItems(conversations);
            if (lvConversations.getSelectionModel().getSelectedIndex() < 0 && !conversations.isEmpty()) {
                lvConversations.getSelectionModel().selectFirst();
            }
            return;
        }

        ObservableList<ConversationItem> filtered = FXCollections.observableArrayList(
                conversations.filtered(item -> item.matches(query)));
        lvConversations.setItems(filtered);
        if (filtered.isEmpty()) {
            currentConversation = null;
            showEmptyConversationState();
            return;
        }
        if (lvConversations.getSelectionModel().getSelectedIndex() < 0 || !filtered.contains(lvConversations.getSelectionModel().getSelectedItem())) {
            lvConversations.getSelectionModel().selectFirst();
        }
    }

    private void refreshCurrentConversation() {
        if (currentConversation == null || currentConversation.getId() == null) {
            showEmptyConversationState();
            return;
        }

        Conversation refreshed = chatService.findConversationById(currentConversation.getId());
        if (refreshed == null) {
            currentConversation = null;
            messages.clear();
            showEmptyConversationState();
            return;
        }

        currentConversation = refreshed;
        List<Message> loaded = chatService.listMessages(currentConversation.getId());
        Set<Long> senderIds = loaded.stream()
                .map(Message::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, User> senders = chatService.findUsersByIds(senderIds);

        List<MessageItem> items = loaded.stream()
                .map(message -> new MessageItem(message, displayName(senders.get(message.getSenderId())), isMine(message), formatTime(message.getCreatedAt())))
                .toList();
        messages.setAll(items);
        updateConversationHeader(currentConversation, senders);
        updateConversationMenuState();

        if (messages.isEmpty()) {
            lblMessagesEmpty.setText("Aucun message pour le moment. Écrivez le premier message.");
            lblMessagesEmpty.setVisible(true);
            lblMessagesEmpty.setManaged(true);
        } else {
            lblMessagesEmpty.setVisible(false);
            lblMessagesEmpty.setManaged(false);
        }

        btnSendMessage.setDisable(false);
    }

    private boolean selectConversationById(Long conversationId) {
        if (conversationId == null) {
            return false;
        }

        for (ConversationItem item : lvConversations.getItems()) {
            if (item.conversation() != null && conversationId.equals(item.conversation().getId())) {
                lvConversations.getSelectionModel().select(item);
                return true;
            }
        }
        return false;
    }

    private void selectConversation(Conversation conversation) {
        currentConversation = conversation;
        if (conversation == null) {
            showEmptyConversationState();
            return;
        }
        refreshCurrentConversation();
    }

    private void showEmptyConversationState() {
        currentConversation = null;
        messages.clear();
        lblConversationTitle.setText("Aucune conversation sélectionnée");
        lblConversationSubtitle.setText("Choisissez une conversation ou créez-en une nouvelle.");
        lblMessagesEmpty.setText("Sélectionnez une conversation dans la colonne de gauche.");
        lblMessagesEmpty.setVisible(true);
        lblMessagesEmpty.setManaged(true);
        btnSendMessage.setDisable(true);
        lvMessages.getSelectionModel().clearSelection();
        updateConversationMenuState();
    }

    private void setupConversationMenu() {
        conversationMenu = new ContextMenu();
        MenuItem viewMembers = new MenuItem("Afficher les membres");
        viewMembers.setOnAction(e -> openGroupMembersViewDialog());
        MenuItem addMember = new MenuItem("Ajouter un membre");
        addMember.setOnAction(e -> openGroupMembersDialog(GroupMembersDialogController.Mode.ADD));
        MenuItem removeMember = new MenuItem("Supprimer un membre");
        removeMember.setOnAction(e -> openGroupMembersDialog(GroupMembersDialogController.Mode.REMOVE));
        conversationMenu.getItems().addAll(viewMembers, addMember, removeMember);
    }

    private void updateConversationMenuState() {
        if (btnConversationMenu == null) {
            return;
        }

        boolean canView = currentUser != null
                && currentConversation != null
                && currentConversation.isGroup()
                && currentConversation.getId() != null
                && chatService.canViewGroupMembers(currentConversation.getId(), currentUser.getId().longValue());

        boolean canManage = canView
                && chatService.canManageGroupMembers(currentConversation.getId(), currentUser.getId().longValue());

        btnConversationMenu.setVisible(canView);
        btnConversationMenu.setManaged(canView);

        if (conversationMenu != null) {
            boolean canAdmin = canManage;
            conversationMenu.getItems().forEach(item -> {
                if ("Afficher les membres".equals(item.getText())) {
                    item.setVisible(canView);
                    item.setDisable(!canView);
                } else {
                    item.setVisible(canAdmin);
                    item.setDisable(!canAdmin);
                }
            });
        }
    }

    private void openGroupMembersViewDialog() {
        if (currentUser == null || currentConversation == null || currentConversation.getId() == null || !currentConversation.isGroup()) {
            return;
        }
        if (!chatService.canViewGroupMembers(currentConversation.getId(), currentUser.getId().longValue())) {
            erreur("Vous n'avez pas accès à cette liste de membres.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat/GroupMembersViewDialog.fxml"));
            Parent root = loader.load();
            GroupMembersViewDialogController controller = loader.getController();

            Stage dialog = new Stage();
            dialog.initModality(Modality.WINDOW_MODAL);
            Window owner = btnConversationMenu.getScene() != null ? btnConversationMenu.getScene().getWindow() : null;
            if (owner != null) {
                dialog.initOwner(owner);
            }
            dialog.setTitle("Membres du groupe");
            dialog.setScene(new Scene(root));
            controller.setContext(chatService, currentConversation, currentUser, dialog);
            dialog.showAndWait();
        } catch (Exception ex) {
            erreur("Impossible d'afficher les membres : " + ex.getMessage());
        }
    }

    private void openGroupMembersDialog(GroupMembersDialogController.Mode mode) {
        if (currentUser == null || currentConversation == null || currentConversation.getId() == null || !currentConversation.isGroup()) {
            return;
        }
        if (!chatService.canManageGroupMembers(currentConversation.getId(), currentUser.getId().longValue())) {
            erreur("Vous n'avez pas les droits pour gérer les membres de ce groupe.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat/GroupMembersDialog.fxml"));
            Parent root = loader.load();
            GroupMembersDialogController controller = loader.getController();

            Stage dialog = new Stage();
            dialog.initModality(Modality.WINDOW_MODAL);
            Window owner = btnConversationMenu.getScene() != null ? btnConversationMenu.getScene().getWindow() : null;
            if (owner != null) {
                dialog.initOwner(owner);
            }
            dialog.setTitle("Gestion des membres");
            dialog.setScene(new Scene(root));
            controller.setContext(chatService, currentConversation, currentUser, mode, this::refreshAfterMemberChange, dialog);
            dialog.showAndWait();
        } catch (Exception ex) {
            erreur("Impossible d'ouvrir la gestion des membres : " + ex.getMessage());
        }
    }

    private void refreshAfterMemberChange() {
        if (currentConversation != null && currentConversation.getId() != null) {
            refreshConversationsOnly();
            refreshCurrentConversation();
        }
    }

    private void refreshUserSearch() {
        if (currentUser == null) {
            lvUserSearchResults.setItems(FXCollections.observableArrayList());
            return;
        }

        String query = Optional.ofNullable(tfUserSearch.getText()).orElse("").trim();
        if (query.isBlank()) {
            lvUserSearchResults.setItems(FXCollections.observableArrayList());
            return;
        }

        Set<Long> excluded = selectedParticipants.stream()
                .map(User::getId)
                .filter(Objects::nonNull)
                .map(Integer::longValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<User> found = chatService.searchUsers(query, currentUser.getId().longValue(), excluded, SEARCH_LIMIT);
        lvUserSearchResults.setItems(FXCollections.observableArrayList(found));
    }

    private void addSelectedUserFromResults() {
        User selected = lvUserSearchResults.getSelectionModel().getSelectedItem();
        if (selected == null) {
            if (lvUserSearchResults.getItems().size() == 1) {
                selected = lvUserSearchResults.getItems().getFirst();
            } else {
                return;
            }
        }
        addSelectedParticipant(selected);
    }

    private void addSelectedParticipant(User user) {
        if (user == null || user.getId() == null) {
            return;
        }
        if (currentUser != null && currentUser.getId() != null && currentUser.getId().equals(user.getId())) {
            return;
        }

        if (rbPrivateConversation.isSelected()) {
            selectedParticipants.setAll(user);
        } else if (selectedParticipants.stream().noneMatch(existing -> existing.getId() != null && existing.getId().equals(user.getId()))) {
            selectedParticipants.add(user);
        }

        refreshSelectedParticipantsView();
        refreshUserSearch();
    }

    private void removeSelectedParticipant(User user) {
        selectedParticipants.removeIf(existing -> existing.getId() != null && existing.getId().equals(user.getId()));
        refreshSelectedParticipantsView();
        refreshUserSearch();
    }

    private void refreshSelectedParticipantsView() {
        fpSelectedParticipants.getChildren().clear();
        for (User participant : selectedParticipants) {
            HBox chip = new HBox(8);
            chip.getStyleClass().add("chat-chip");
            chip.setAlignment(Pos.CENTER_LEFT);

            Label label = new Label(displayName(participant));
            label.getStyleClass().add("chat-chip-label");

            Button remove = new Button("×");
            remove.getStyleClass().add("chat-chip-remove");
            remove.setOnAction(e -> removeSelectedParticipant(participant));

            chip.getChildren().addAll(label, remove);
            fpSelectedParticipants.getChildren().add(chip);
        }
        updateCreateMode();
    }

    private void clearCreateForm() {
        selectedParticipants.clear();
        tfConversationGroupName.clear();
        tfUserSearch.clear();
        lvUserSearchResults.getItems().clear();
        rbPrivateConversation.setSelected(true);
        refreshSelectedParticipantsView();
        updateCreateMode();
    }

    private void setupEmojiPicker() {
        emojiCategories.clear();
        emojiCategories.put("Visages", List.of("😀", "😁", "😂", "🤣", "🙂", "😉", "😍", "🤩", "😎", "😢", "😡", "🤔"));
        emojiCategories.put("Gestes", List.of("👍", "👎", "👏", "🙏", "✌️", "👌", "💪", "👋", "🤝", "🙌", "🫶", "🫡"));
        emojiCategories.put("Cœurs", List.of("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💕", "💞", "💖", "💘"));
        emojiCategories.put("Fête", List.of("🎉", "🥳", "🎂", "🎁", "✨", "🎈", "🎊", "🏆", "🌟", "🔥", "💯", "🚀"));
        emojiCategories.put("École", List.of("📚", "✏️", "📝", "📎", "🎒", "🖍️", "📖", "🧪", "🧠", "🔬", "🖥️", "🎓"));
        emojiCategories.put("Objets", List.of("📱", "⌚", "📷", "🎧", "💡", "🔔", "📦", "🧩", "📌", "🗂️", "🗓️", "🪄"));

        VBox root = new VBox(14);
        root.getStyleClass().add("chat-emoji-popup");
        root.setPadding(new Insets(16));
        root.setPrefWidth(360);
        root.setMaxWidth(360);

        Label title = new Label("Sélecteur d'emoji");
        title.getStyleClass().add("chat-emoji-title");

        tfEmojiSearch = new TextField();
        tfEmojiSearch.setPromptText("Rechercher un emoji, ex. coeur, fête...");
        tfEmojiSearch.getStyleClass().add("chat-emoji-search");
        tfEmojiSearch.textProperty().addListener((obs, oldValue, newValue) -> filterEmojiButtons(newValue));

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("chat-emoji-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        emojiButtons.clear();
        for (Map.Entry<String, List<String>> entry : emojiCategories.entrySet()) {
            FlowPane grid = new FlowPane();
            grid.getStyleClass().add("chat-emoji-grid");
            grid.setHgap(8);
            grid.setVgap(8);
            grid.setPrefWrapLength(320);

            for (String emoji : entry.getValue()) {
                Button emojiButton = new Button(emoji);
                emojiButton.getStyleClass().add("chat-emoji-button");
                emojiButton.setOnAction(evt -> {
                    insertEmoji(emoji);
                    if (emojiPopup != null) {
                        emojiPopup.hide();
                    }
                });
                emojiButton.setUserData(entry.getKey() + " " + emoji);
                grid.getChildren().add(emojiButton);
                emojiButtons.add(emojiButton);
            }

            ScrollPane scrollPane = new ScrollPane(grid);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefViewportHeight(220);
            scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.getStyleClass().add("chat-emoji-scroll");

            Tab tab = new Tab(entry.getKey(), scrollPane);
            tab.getStyleClass().add("chat-emoji-tab");
            tabs.getTabs().add(tab);
        }

        root.getChildren().addAll(title, tfEmojiSearch, tabs);

        emojiPopup = new Popup();
        emojiPopup.setAutoHide(true);
        emojiPopup.setHideOnEscape(true);
        emojiPopup.getContent().add(root);
        emojiPopup.setOnHidden(e -> {
            if (tfEmojiSearch != null) {
                tfEmojiSearch.clear();
            }
            filterEmojiButtons("");
        });
    }

    private void filterEmojiButtons(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        for (Button button : emojiButtons) {
            Object data = button.getUserData();
            String searchable = data == null ? button.getText() : data.toString();
            boolean visible = q.isBlank() || searchable.toLowerCase(Locale.ROOT).contains(q);
            button.setVisible(visible);
            button.setManaged(visible);
        }
    }

    private void insertEmoji(String emoji) {
        if (emoji == null || emoji.isBlank()) {
            return;
        }
        int caret = taMessageDraft.getCaretPosition();
        String text = Optional.ofNullable(taMessageDraft.getText()).orElse("");
        String updated = text.substring(0, caret) + emoji + text.substring(caret);
        taMessageDraft.setText(updated);
        taMessageDraft.positionCaret(caret + emoji.length());
        taMessageDraft.requestFocus();
    }

    private void updateAttachmentPreview() {
        boolean hasAttachment = selectedAttachmentName != null && !selectedAttachmentName.isBlank();
        lblSelectedAttachment.setText(hasAttachment ? selectedAttachmentName : "");
        boxAttachmentPreview.setVisible(hasAttachment);
        boxAttachmentPreview.setManaged(hasAttachment);
    }

    private void clearAttachmentSelection() {
        selectedAttachmentPath = null;
        selectedAttachmentName = null;
        updateAttachmentPreview();
    }

    private Optional<String> showEditMessageDialog(String initialContent) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Modifier le message");
        dialog.setHeaderText("Modifier votre message");

        DialogPane pane = dialog.getDialogPane();
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextArea editor = new TextArea(initialContent == null ? "" : initialContent);
        editor.setWrapText(true);
        editor.setPrefRowCount(6);
        editor.getStyleClass().add("chat-edit-textarea");
        pane.setContent(editor);

        Button okButton = (Button) pane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setText("Enregistrer");
        }
        Button cancelButton = (Button) pane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.setText("Annuler");
        }

        dialog.setResultConverter(buttonType -> buttonType == ButtonType.OK ? editor.getText() : null);
        Optional<String> result = dialog.showAndWait();
        return result.map(String::trim).filter(s -> !s.isBlank());
    }

    private boolean canEditDeleteMessage(MessageItem item) {
        if (item == null || item.message() == null || currentUser == null || currentUser.getId() == null) {
            return false;
        }
        return Objects.equals(item.message().getSenderId(), currentUser.getId().longValue());
    }

    private void updateCreateMode() {
        boolean group = rbGroupConversation.isSelected();
        boxGroupName.setVisible(group);
        boxGroupName.setManaged(group);
        tfConversationGroupName.setDisable(!group);
        btnCreateConversation.setText(group ? "Créer le groupe" : "Créer la conversation");

        if (!group && selectedParticipants.size() > 1) {
            User keep = selectedParticipants.getFirst();
            selectedParticipants.setAll(keep);
            refreshSelectedParticipantsView();
        }

        if (group) {
            lblCreateHint.setText("Ajoutez plusieurs utilisateurs. Un groupe nécessite un nom et au moins un autre participant.");
        } else {
            lblCreateHint.setText("Sélectionnez un seul destinataire pour une conversation privée.");
        }
    }

    private void showCreatePane(boolean show) {
        paneCreateConversation.setVisible(show);
        paneCreateConversation.setManaged(show);
        if (show) {
            paneCreateConversation.toFront();
        }
    }

    private void updateConversationHeader(Conversation conversation, Map<Long, User> senders) {
        if (conversation == null) {
            showEmptyConversationState();
            return;
        }

        String title = resolveConversationTitle(conversation);
        lblConversationTitle.setText(title);

        List<User> participantUsers = conversation.getParticipants().stream()
                .map(participant -> senders.getOrDefault(participant.getUserId(), null))
                .filter(Objects::nonNull)
                .toList();

        String subtitle = conversation.isGroup()
                ? participantUsers.size() + " participant" + (participantUsers.size() > 1 ? "s" : "")
                : participantUsers.stream()
                .filter(user -> currentUser == null || user.getId() == null || !user.getId().equals(currentUser.getId()))
                .map(ChatController::displayName)
                .collect(Collectors.joining(", "));

        if (subtitle.isBlank()) {
            subtitle = conversation.isGroup() ? "Conversation de groupe" : "Conversation privée";
        }
        lblConversationSubtitle.setText(subtitle);
    }

    private ConversationItem buildConversationItem(Conversation conversation, Map<Long, User> userById) {
        String title = resolveConversationTitle(conversation, userById);
        Message lastMessage = chatService.findLastMessage(conversation.getId()).orElse(null);
        String preview = lastMessage == null
                ? "Aucun message pour le moment."
                : displayName(userById.get(lastMessage.getSenderId())) + " : " + truncate(lastMessage.getContent(), 90);

        LocalDateTime lastActivity = lastMessage != null ? lastMessage.getCreatedAt() : conversation.getUpdatedAt();
        StringBuilder meta = new StringBuilder();
        if (lastActivity != null) {
            meta.append(DATE_TIME_FMT.format(lastActivity));
        }
        if (!conversation.getParticipants().isEmpty()) {
            if (meta.length() > 0) {
                meta.append(" • ");
            }
            meta.append(conversation.getParticipants().size())
                    .append(" participant")
                    .append(conversation.getParticipants().size() > 1 ? "s" : "");
        }
        return new ConversationItem(conversation, title, preview, meta.toString());
    }

    private String resolveConversationTitle(Conversation conversation) {
        return resolveConversationTitle(conversation, Map.of());
    }

    private String resolveConversationTitle(Conversation conversation, Map<Long, User> userById) {
        if (conversation == null) {
            return "";
        }
        if (conversation.getTitle() != null && !conversation.getTitle().isBlank()) {
            return conversation.getTitle().trim();
        }

        List<String> names = new ArrayList<>();
        for (var participant : conversation.getParticipants()) {
            User user = userById.get(participant.getUserId());
            if (user == null) {
                continue;
            }
            if (currentUser != null && currentUser.getId() != null && currentUser.getId().equals(user.getId())) {
                continue;
            }
            names.add(displayName(user));
        }

        if (!names.isEmpty()) {
            return String.join(", ", names);
        }

        return conversation.isGroup() ? "Groupe sans nom" : "Conversation privée";
    }

    private boolean isMine(Message message) {
        return currentUser != null
                && currentUser.getId() != null
                && message != null
                && message.getSenderId() != null
                && currentUser.getId().longValue() == message.getSenderId();
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? "" : TIME_FMT.format(time);
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

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= max) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, max - 1)) + "…";
    }

    private void handleMessageShortcut(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER && event.isControlDown()) {
            onSendMessage();
            event.consume();
        }
    }

    private static void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static void erreur(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private static boolean confirmer(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setHeaderText(null);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.YES;
    }

    private record ConversationItem(Conversation conversation, String title, String preview, String meta) {
        boolean matches(String query) {
            String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            if (q.isBlank()) {
                return true;
            }
            return contains(title, q) || contains(preview, q) || contains(meta, q);
        }

        private static boolean contains(String value, String query) {
            return value != null && value.toLowerCase(Locale.ROOT).contains(query);
        }
    }

    private record MessageItem(Message message, String senderName, boolean mine, String timeText) {
    }
}
