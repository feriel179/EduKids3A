package com.edukids.edukids3a.controller;

import com.edukids.edukids3a.model.Conversation;
import com.edukids.edukids3a.model.Message;
import com.edukids.edukids3a.model.MessageAttachment;
import com.edukids.edukids3a.model.User;
import com.edukids.edukids3a.security.AuthSession;
import com.edukids.edukids3a.service.ChatService;
import com.edukids.edukids3a.service.OpenRouterTranslationService;
import com.edukids.edukids3a.service.PdfSummaryService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Cursor;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.Duration;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatController {

    private static final Logger LOG = LoggerFactory.getLogger(ChatController.class);

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm").withLocale(Locale.FRENCH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int SEARCH_LIMIT = 12;
    private static final int IMAGE_MAX_WIDTH = 420;
    private static final int IMAGE_MAX_HEIGHT = 320;
    private static final Path ATTACHMENT_STORE_ROOT = Paths.get(System.getProperty("user.home"), ".edukids3a", "chat-attachments");
    private static final List<TranslationTarget> TRANSLATION_TARGETS = List.of(
            new TranslationTarget("FR", "French", "FR"),
            new TranslationTarget("ENG", "English", "ENG"),
            new TranslationTarget("AR", "Arabic", "AR")
    );

    private ChatService chatService;
    private final OpenRouterTranslationService translationService = new OpenRouterTranslationService();
    private final PdfSummaryService pdfSummaryService = new PdfSummaryService(translationService);
    private final ObservableList<ConversationItem> conversations = FXCollections.observableArrayList();
    private final ObservableList<MessageItem> messages = FXCollections.observableArrayList();
    private final ObservableList<User> selectedParticipants = FXCollections.observableArrayList();
    private final ConcurrentMap<Long, Map<String, TranslationResult>> translationResults = new ConcurrentHashMap<>();
    private final Set<String> translationRequestsInFlight = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, PdfSummaryResult> pdfSummaryResults = new ConcurrentHashMap<>();
    private final Set<String> pdfSummaryRequestsInFlight = ConcurrentHashMap.newKeySet();

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
    private Button btnVoiceMessage;
    @FXML
    private Button btnClearAttachment;
    @FXML
    private HBox boxAttachmentPreview;
    @FXML
    private Label lblSelectedAttachment;
    @FXML
    private Label lblVoiceRecordState;
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

    private MessageAttachment selectedAttachment;
    private String selectedAttachmentPath;
    private String selectedAttachmentName;
    private boolean recordingVoice;
    private Path voiceRecordingFile;
    private TargetDataLine voiceRecordingLine;
    private Thread voiceRecordingThread;
    private long voiceRecordingStartedAtMillis;
    private Clip activeAudioClip;
    private MessageAttachment activeAudioAttachment;
    private Button activeAudioButton;
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

                VBox bubble = buildMessageBubble(item);

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

        try {
            chatService = new ChatService();
        } catch (RuntimeException ex) {
            LOG.error("Impossible d'initialiser le module de messagerie.", ex);
            if (lblChatStatus != null) {
                lblChatStatus.setText("Module de messagerie indisponible");
            }
            if (btnNewConversation != null) {
                btnNewConversation.setDisable(true);
            }
            if (btnRefreshConversations != null) {
                btnRefreshConversations.setDisable(true);
            }
            if (btnSendMessage != null) {
                btnSendMessage.setDisable(true);
            }
            if (taMessageDraft != null) {
                taMessageDraft.setDisable(true);
            }
            showEmptyConversationState();
            return;
        }

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
        try {
            refreshAll();
        } catch (RuntimeException ex) {
            LOG.error("Impossible de charger les conversations au démarrage.", ex);
            if (lblChatStatus != null) {
                lblChatStatus.setText("Messagerie chargée partiellement");
            }
            showEmptyConversationState();
        }
    }

    @FXML
    private void onRefreshConversations() {
        refreshAll();
    }

    public void refresh() {
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
            Throwable cause = ex.getCause();
            String detail = cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()
                    ? cause.getMessage()
                    : ex.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = ex.getClass().getSimpleName();
            }
            erreur("Impossible de créer la conversation : " + detail);
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
        boolean hasAttachment = selectedAttachment != null && selectedAttachment.getStoragePath() != null && !selectedAttachment.getStoragePath().isBlank();
        if (draft.isBlank() && !hasAttachment) {
            return;
        }

        try {
            if (hasAttachment) {
                Path storedAttachment = storeAttachmentForConversation(Path.of(selectedAttachment.getStoragePath()), currentConversation.getId(), currentUser.getId().longValue());
                MessageAttachment attachment = copyAttachment(selectedAttachment, storedAttachment);
                String content = draft.isBlank() ? buildAttachmentCaption(attachment) : draft;
                chatService.sendMessage(currentConversation.getId(), currentUser.getId().longValue(), content, attachment);
            } else {
                chatService.sendMessage(currentConversation.getId(), currentUser.getId().longValue(), draft);
            }
            taMessageDraft.clear();
            clearAttachmentSelection();
            refreshCurrentConversation();
            refreshConversationsOnly();
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            String detail = cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()
                    ? cause.getMessage()
                    : ex.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = ex.getClass().getSimpleName();
            }
            erreur("Impossible d'envoyer le message : " + detail);
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
        if (recordingVoice) {
            stopVoiceRecordingAndPrepare();
        }

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
                new FileChooser.ExtensionFilter("Documents Office", "*.pdf", "*.doc", "*.docx", "*.txt", "*.rtf"),
                new FileChooser.ExtensionFilter("Excel", "*.xls", "*.xlsx", "*.xlsm"),
                new FileChooser.ExtensionFilter("PowerPoint", "*.ppt", "*.pptx", "*.pptm")
        );

        File file = chooser.showOpenDialog(window);
        if (file == null) {
            return;
        }

        selectedAttachment = buildSelectedAttachment(file);
        selectedAttachmentPath = selectedAttachment.getStoragePath();
        selectedAttachmentName = selectedAttachment.getOriginalName();
        updateAttachmentPreview();
    }

    @FXML
    private void onVoiceRecord() {
        if (currentUser == null) {
            erreur("Aucun utilisateur connecté.");
            return;
        }
        if (currentConversation == null || currentConversation.getId() == null) {
            erreur("Sélectionnez une conversation.");
            return;
        }

        if (recordingVoice) {
            stopVoiceRecordingAndPrepare();
        } else {
            startVoiceRecording();
        }
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
        MenuItem deleteConversation = new MenuItem("Supprimer la conversation");
        deleteConversation.setOnAction(e -> onDeleteConversationForMe());
        conversationMenu.getItems().addAll(viewMembers, addMember, removeMember, deleteConversation);
    }

    private void updateConversationMenuState() {
        if (btnConversationMenu == null) {
            return;
        }

        boolean hasConversation = currentUser != null
                && currentConversation != null
                && currentConversation.getId() != null;
        boolean canView = hasConversation
                && currentConversation.isGroup()
                && chatService.canViewGroupMembers(currentConversation.getId(), currentUser.getId().longValue());

        boolean canManage = canView
                && chatService.canManageGroupMembers(currentConversation.getId(), currentUser.getId().longValue());

        btnConversationMenu.setVisible(hasConversation);
        btnConversationMenu.setManaged(hasConversation);

        if (conversationMenu != null) {
            conversationMenu.getItems().forEach(item -> {
                if ("Afficher les membres".equals(item.getText())) {
                    item.setVisible(canView);
                    item.setDisable(!canView);
                } else if ("Ajouter un membre".equals(item.getText()) || "Supprimer un membre".equals(item.getText())) {
                    item.setVisible(canManage);
                    item.setDisable(!canManage);
                } else if ("Supprimer la conversation".equals(item.getText())) {
                    item.setVisible(hasConversation);
                    item.setDisable(!hasConversation);
                } else {
                    item.setVisible(true);
                    item.setDisable(false);
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

    private void onDeleteConversationForMe() {
        if (currentUser == null || currentConversation == null || currentConversation.getId() == null) {
            return;
        }
        String title = resolveConversationTitle(currentConversation);
        if (!confirmer("Supprimer la conversation \"" + title + "\" pour votre compte uniquement ?")) {
            return;
        }
        try {
            chatService.hideConversation(currentConversation.getId(), currentUser.getId().longValue());
            currentConversation = null;
            refreshAll();
        } catch (Exception ex) {
            erreur("Impossible de supprimer la conversation pour vous : " + ex.getMessage());
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
                selected = lvUserSearchResults.getItems().get(0);
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
        boolean hasAttachment = selectedAttachment != null;
        lblSelectedAttachment.setText(hasAttachment ? selectedAttachment.getOriginalName() : "");
        boxAttachmentPreview.setVisible(hasAttachment);
        boxAttachmentPreview.setManaged(hasAttachment);
    }

    private VBox buildMessageBubble(MessageItem item) {
        Message message = item.message();

        Label sender = new Label(item.senderName());
        sender.getStyleClass().add("chat-message-sender");

        Label time = new Label(item.timeText());
        time.getStyleClass().add("chat-message-time");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, sender, spacer, time);
        header.getStyleClass().add("chat-message-header");

        VBox body = new VBox(10);
        body.getStyleClass().add("chat-message-body");

        String text = Optional.ofNullable(message.getContent()).orElse("").trim();
        boolean hasText = !text.isBlank();
        MessageKind kind = resolveMessageKind(message);
        boolean canTranslate = hasText && kind == MessageKind.TEXT;

        if (canTranslate) {
            HBox translationActions = buildTranslationActions(item, text, true);
            body.getChildren().add(translationActions);
        }

        if (hasText) {
            Label content = new Label(text);
            content.getStyleClass().add("chat-message-content");
            content.setWrapText(true);
            body.getChildren().add(content);
        }

        Node attachmentNode = createAttachmentNode(message, kind);
        if (attachmentNode != null) {
            body.getChildren().add(attachmentNode);
        }

        if (body.getChildren().isEmpty()) {
            Label fallback = new Label(kind == MessageKind.IMAGE ? "Image" : kind == MessageKind.PDF ? "PDF" : "Message");
            fallback.getStyleClass().add("chat-message-content");
            body.getChildren().add(fallback);
        }

        VBox translationHistory = canTranslate ? buildTranslationHistory(message.getId()) : null;
        if (translationHistory != null) {
            body.getChildren().add(translationHistory);
        }

        VBox bubble = new VBox(8, header, body);
        bubble.getStyleClass().addAll("chat-message-bubble", item.mine() ? "chat-message-mine" : "chat-message-other");
        return bubble;
    }

    private HBox buildTranslationActions(MessageItem item, String originalText, boolean hasText) {
        HBox actions = new HBox(6);
        actions.getStyleClass().add("chat-translation-actions");
        actions.setAlignment(Pos.CENTER_LEFT);

        for (TranslationTarget target : TRANSLATION_TARGETS) {
            Button button = new Button(target.buttonLabel());
            button.getStyleClass().add("chat-translation-button");
            button.setFocusTraversable(false);
            button.setDisable(!hasText);
            button.setOnAction(event -> requestTranslation(item, originalText, target));
            actions.getChildren().add(button);
        }

        return actions;
    }

    private VBox buildTranslationHistory(Long messageId) {
        if (messageId == null) {
            return null;
        }

        Map<String, TranslationResult> perMessage = translationResults.get(messageId);
        if (perMessage == null || perMessage.isEmpty()) {
            return null;
        }

        VBox translations = new VBox(6);
        translations.getStyleClass().add("chat-translation-history");

        for (TranslationTarget target : TRANSLATION_TARGETS) {
            TranslationResult result = perMessage.get(target.key());
            if (result == null) {
                continue;
            }

            translations.getChildren().add(buildTranslationResultNode(target, result));
        }

        return translations.getChildren().isEmpty() ? null : translations;
    }

    private Node buildTranslationResultNode(TranslationTarget target, TranslationResult result) {
        VBox box = new VBox(4);
        box.getStyleClass().add("chat-translation-result");

        Label label = new Label(target.buttonLabel() + " traduction");
        label.getStyleClass().add("chat-translation-label");
        box.getChildren().add(label);

        if (result.loading()) {
            Label loading = new Label("Traduction en cours...");
            loading.getStyleClass().add("chat-translation-loading");
            box.getChildren().add(loading);
        } else if (result.errorMessage() != null) {
            Label error = new Label(result.errorMessage());
            error.getStyleClass().add("chat-translation-error");
            error.setWrapText(true);
            box.getChildren().add(error);
        } else {
            Label translated = new Label(result.translatedText());
            translated.getStyleClass().add("chat-translation-text");
            translated.setWrapText(true);
            if ("AR".equals(target.key())) {
                translated.setNodeOrientation(javafx.geometry.NodeOrientation.RIGHT_TO_LEFT);
            }
            box.getChildren().add(translated);
        }

        return box;
    }

    private void requestTranslation(MessageItem item, String originalText, TranslationTarget target) {
        if (item == null || item.message() == null || item.message().getId() == null) {
            return;
        }

        String trimmedText = Optional.ofNullable(originalText).orElse("").trim();
        if (trimmedText.isBlank()) {
            showTranslationFailure(item.message().getId(), target, "Ce message ne contient pas de texte à traduire.");
            return;
        }

        String requestKey = translationRequestKey(item.message().getId(), target.key());
        if (!translationRequestsInFlight.add(requestKey)) {
            return;
        }

        showTranslationLoading(item.message().getId(), target);

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                return translationService.translate(trimmedText, target.apiLanguage());
            }
        };

        task.setOnSucceeded(event -> {
            translationRequestsInFlight.remove(requestKey);
            String translatedText = task.getValue();
            showTranslationSuccess(item.message().getId(), target, translatedText);
        });

        task.setOnFailed(event -> {
            translationRequestsInFlight.remove(requestKey);
            Throwable error = task.getException();
            String message = error != null && error.getMessage() != null && !error.getMessage().isBlank()
                    ? error.getMessage()
                    : "La traduction a échoué pour une raison inconnue.";
            showTranslationFailure(item.message().getId(), target, message);
        });

        Thread worker = new Thread(task, "openrouter-translation-" + item.message().getId() + "-" + target.key());
        worker.setDaemon(true);
        worker.start();
    }

    private void showTranslationLoading(Long messageId, TranslationTarget target) {
        updateTranslationResult(messageId, target.key(), TranslationResult.inProgress());
        refreshMessageCells();
    }

    private void showTranslationSuccess(Long messageId, TranslationTarget target, String translatedText) {
        updateTranslationResult(messageId, target.key(), TranslationResult.success(translatedText));
        refreshMessageCells();
    }

    private void showTranslationFailure(Long messageId, TranslationTarget target, String errorMessage) {
        updateTranslationResult(messageId, target.key(), TranslationResult.failure(errorMessage));
        refreshMessageCells();
    }

    private void updateTranslationResult(Long messageId, String targetKey, TranslationResult result) {
        translationResults.compute(messageId, (id, existing) -> {
            Map<String, TranslationResult> map = existing == null ? new LinkedHashMap<>() : new LinkedHashMap<>(existing);
            map.put(targetKey, result);
            return map;
        });
    }

    private void refreshMessageCells() {
        Platform.runLater(() -> {
            if (lvMessages != null) {
                lvMessages.refresh();
            }
        });
    }

    private String translationRequestKey(Long messageId, String targetKey) {
        return messageId + ":" + targetKey;
    }

    private Node createAttachmentNode(Message message, MessageKind kind) {
        return switch (kind) {
            case IMAGE -> createImageAttachmentNode(message);
            case AUDIO -> createAudioAttachmentNode(message);
            case PDF, WORD, POWERPOINT, EXCEL, FILE -> createFileAttachmentNode(message, kind);
            default -> null;
        };
    }

    private Node createImageAttachmentNode(Message message) {
        MessageAttachment attachment = primaryAttachment(message);
        String filePath = normalizeAttachmentPath(attachment != null ? attachment.getStoragePath() : message.getFilePath());
        if (filePath == null) {
            Label unavailable = new Label("Image indisponible");
            unavailable.getStyleClass().add("chat-message-attachment-fallback");
            return unavailable;
        }

        Path path = Path.of(filePath);
        if (!Files.exists(path)) {
            Label unavailable = new Label("Image introuvable");
            unavailable.getStyleClass().add("chat-message-attachment-fallback");
            return unavailable;
        }

        try {
            Image image = new Image(path.toUri().toString(), false);
            if (image.isError()) {
                throw new IllegalStateException("Image illisible");
            }

            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(IMAGE_MAX_WIDTH);
            imageView.setFitHeight(IMAGE_MAX_HEIGHT);
            imageView.getStyleClass().add("chat-message-image");

            StackPane frame = new StackPane(imageView);
            frame.getStyleClass().add("chat-message-image-frame");
            frame.setMaxWidth(IMAGE_MAX_WIDTH + 24);
            return frame;
        } catch (RuntimeException ex) {
            Label unavailable = new Label("Image indisponible");
            unavailable.getStyleClass().add("chat-message-attachment-fallback");
            return unavailable;
        }
    }

    private Node createFileAttachmentNode(Message message, MessageKind kind) {
        MessageAttachment attachment = primaryAttachment(message);
        String filePath = normalizeAttachmentPath(attachment != null ? attachment.getStoragePath() : message.getFilePath());
        String fileName = resolveAttachmentFileName(attachment != null ? attachment.getOriginalName() : filePath);

        Label badge = new Label(kind.badgeText());
        badge.getStyleClass().add("chat-message-file-badge");

        Label name = new Label(fileName);
        name.getStyleClass().add("chat-message-file-name");
        name.setWrapText(true);

        Label hint = new Label("Cliquer pour télécharger");
        hint.getStyleClass().add("chat-message-file-hint");

        VBox textBox = new VBox(2, name, hint);
        textBox.getStyleClass().add("chat-message-file-text");

        if (kind == MessageKind.PDF) {
            return createPdfAttachmentView(message, badge, textBox, filePath);
        }

        HBox card = new HBox(12, badge, textBox);
        card.getStyleClass().add("chat-message-file-card");
        card.setAlignment(Pos.CENTER_LEFT);
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 1) {
                downloadAttachment(message);
            }
        });

        return card;
    }

    private Node createPdfAttachmentView(Message message, Label badge, VBox textBox, String filePath) {
        Button summaryButton = new Button("Résumé PDF");
        summaryButton.getStyleClass().add("chat-pdf-summary-button");
        summaryButton.setFocusTraversable(false);
        summaryButton.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_CLICKED, event -> event.consume());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, badge, textBox, spacer, summaryButton);
        row.getStyleClass().add("chat-message-file-card-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setCursor(Cursor.HAND);
        row.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 1) {
                downloadAttachment(message);
            }
        });

        VBox card = new VBox(8, row);
        card.getStyleClass().add("chat-message-file-card");
        card.setAlignment(Pos.CENTER_LEFT);

        String summaryKey = pdfSummaryKey(message, filePath);
        PdfSummaryResult summaryResult = pdfSummaryResults.get(summaryKey);
        if (summaryResult != null) {
            card.getChildren().add(createPdfSummaryNode(summaryResult));
        }

        summaryButton.setOnAction(event -> {
            event.consume();
            requestPdfSummary(message, filePath);
        });

        return card;
    }

    private Node createPdfSummaryNode(PdfSummaryResult result) {
        Label summary = new Label();
        summary.setWrapText(true);
        summary.getStyleClass().add("chat-pdf-summary-text");

        if (result.loading()) {
            summary.setText("Résumé en cours...");
            summary.getStyleClass().add("chat-pdf-summary-loading");
        } else if (result.errorMessage() != null) {
            summary.setText(result.errorMessage());
            summary.getStyleClass().add("chat-pdf-summary-error");
        } else {
            summary.setText(result.summary());
        }

        return summary;
    }

    private void requestPdfSummary(Message message, String filePath) {
        String summaryKey = pdfSummaryKey(message, filePath);
        if (!pdfSummaryRequestsInFlight.add(summaryKey)) {
            return;
        }

        updatePdfSummaryResult(summaryKey, PdfSummaryResult.inProgress());
        refreshMessageCells();

        Task<String> task = new Task<>() {
            @Override
            protected String call() {
                String normalizedPath = normalizeAttachmentPath(filePath);
                if (normalizedPath == null || normalizedPath.isBlank()) {
                    throw new IllegalStateException("PDF introuvable.");
                }
                return pdfSummaryService.summarize(Path.of(normalizedPath));
            }
        };

        task.setOnSucceeded(event -> {
            pdfSummaryRequestsInFlight.remove(summaryKey);
            updatePdfSummaryResult(summaryKey, PdfSummaryResult.success(task.getValue()));
            refreshMessageCells();
        });

        task.setOnFailed(event -> {
            pdfSummaryRequestsInFlight.remove(summaryKey);
            Throwable error = task.getException();
            String messageText = error != null && error.getMessage() != null && !error.getMessage().isBlank()
                    ? error.getMessage()
                    : "Impossible de résumer ce PDF.";
            updatePdfSummaryResult(summaryKey, PdfSummaryResult.failure(messageText));
            refreshMessageCells();
        });

        Thread worker = new Thread(task, "openrouter-pdf-summary-" + summaryKey);
        worker.setDaemon(true);
        worker.start();
    }

    private void updatePdfSummaryResult(String summaryKey, PdfSummaryResult result) {
        pdfSummaryResults.put(summaryKey, result);
    }

    private String pdfSummaryKey(Message message, String filePath) {
        if (message != null && message.getId() != null) {
            return "message:" + message.getId();
        }
        return "file:" + Optional.ofNullable(filePath).orElse("");
    }

    private Node createAudioAttachmentNode(Message message) {
        MessageAttachment attachment = primaryAttachment(message);
        String filePath = normalizeAttachmentPath(attachment != null ? attachment.getStoragePath() : message.getFilePath());
        String fileName = resolveAttachmentFileName(attachment != null ? attachment.getOriginalName() : filePath);
        String durationText = attachment != null && attachment.getDuration() != null ? formatDuration(attachment.getDuration()) : "";

        Label badge = new Label("\uD83C\uDFA4");
        badge.getStyleClass().add("chat-message-audio-badge");

        Label name = new Label(fileName);
        name.getStyleClass().add("chat-message-file-name");
        name.setWrapText(true);

        Label hint = new Label(durationText.isBlank() ? "Message vocal" : "Message vocal \u2022 " + durationText);
        hint.getStyleClass().add("chat-message-audio-time");

        HBox waveform = createAudioWaveform();

        VBox textBox = new VBox(2, name, waveform, hint);
        textBox.getStyleClass().add("chat-message-file-text");

        Button play = new Button("\u25B6");
        play.getStyleClass().add("chat-message-audio-play");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox cardRow = new HBox(12, badge, textBox, spacer, play);
        cardRow.getStyleClass().add("chat-message-audio-card-row");
        cardRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(6, cardRow);
        card.getStyleClass().add("chat-message-audio-card");
        card.setAlignment(Pos.CENTER_LEFT);

        play.setOnAction(event -> toggleAudioPlayback(message, play));
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 1 && event.getTarget() == card) {
                toggleAudioPlayback(message, play);
            }
        });

        return card;
    }

    private HBox createAudioWaveform() {
        HBox wave = new HBox(3);
        wave.getStyleClass().add("chat-message-audio-wave");
        wave.setAlignment(Pos.CENTER_LEFT);

        double[] heights = {8, 14, 20, 12, 18, 10, 16};
        for (double height : heights) {
            Region bar = new Region();
            bar.getStyleClass().add("chat-message-audio-wave-bar");
            bar.setMinWidth(4);
            bar.setPrefWidth(4);
            bar.setMaxWidth(4);
            bar.setMinHeight(height);
            bar.setPrefHeight(height);
            bar.setMaxHeight(height);
            wave.getChildren().add(bar);
        }

        return wave;
    }

    private void downloadAttachment(Message message) {
        if (message == null) {
            return;
        }

        MessageAttachment attachment = primaryAttachment(message);
        String filePath = normalizeAttachmentPath(attachment != null ? attachment.getStoragePath() : message.getFilePath());
        if (filePath == null) {
            erreur("Aucune pièce jointe associée à ce message.");
            return;
        }

        Path source = Path.of(filePath);
        if (!Files.exists(source)) {
            erreur("Le fichier source est introuvable.");
            return;
        }

        Task<Path> downloadTask = new Task<>() {
            @Override
            protected Path call() throws Exception {
                Path downloadsDir = resolveDownloadDirectory();
                Files.createDirectories(downloadsDir);
                String fileName = resolveDownloadFileName(attachment != null && attachment.getOriginalName() != null ? attachment.getOriginalName() : source.getFileName().toString());
                Path target = uniqueTargetPath(downloadsDir, fileName);
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                return target;
            }
        };

        downloadTask.setOnSucceeded(event -> {
            Path target = downloadTask.getValue();
            info("Fichier téléchargé dans : " + target.toAbsolutePath());
        });
        downloadTask.setOnFailed(event -> {
            Throwable error = downloadTask.getException();
            erreur("Impossible de télécharger le fichier : " + (error == null ? "erreur inconnue" : error.getMessage()));
        });

        Thread thread = new Thread(downloadTask, "chat-pdf-download");
        thread.setDaemon(true);
        thread.start();
    }

    private void toggleAudioPlayback(Message message, Button playButton) {
        MessageAttachment attachment = primaryAttachment(message);
        if (attachment == null || attachment.getStoragePath() == null || attachment.getStoragePath().isBlank()) {
            erreur("Aucun message vocal associé.");
            return;
        }

        Path source = Path.of(attachment.getStoragePath());
        if (!Files.exists(source)) {
            erreur("Le fichier audio est introuvable.");
            return;
        }

        try {
            if (activeAudioClip != null && activeAudioClip.isRunning() && activeAudioAttachment != null
                    && Objects.equals(activeAudioAttachment.getStoragePath(), attachment.getStoragePath())) {
                activeAudioClip.stop();
                activeAudioClip.close();
                activeAudioClip = null;
                activeAudioAttachment = null;
                if (activeAudioButton != null) {
                    activeAudioButton.setText("\u25B6");
                    activeAudioButton.getStyleClass().remove("chat-message-audio-play-active");
                }
                activeAudioButton = null;
                playButton.setText("\u25B6");
                playButton.getStyleClass().remove("chat-message-audio-play-active");
                return;
            }

            stopCurrentAudioPlayback();

            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(source.toFile());
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            activeAudioClip = clip;
            activeAudioAttachment = attachment;
            activeAudioButton = playButton;
            playButton.setText("\u23F8");
            playButton.getStyleClass().add("chat-message-audio-play-active");
            clip.addLineListener(event -> {
                if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                    Platform.runLater(() -> {
                        if (activeAudioClip == clip) {
                            playButton.setText("\u25B6");
                            playButton.getStyleClass().remove("chat-message-audio-play-active");
                            activeAudioClip = null;
                            activeAudioAttachment = null;
                            activeAudioButton = null;
                        }
                    });
                }
            });
            clip.start();
        } catch (Exception ex) {
            erreur("Impossible de lire le message vocal : " + ex.getMessage());
        }
    }

    private void stopCurrentAudioPlayback() {
        if (activeAudioClip != null) {
            try {
                activeAudioClip.stop();
                activeAudioClip.close();
            } catch (Exception ignored) {
                // ignore
            } finally {
                if (activeAudioButton != null) {
                    activeAudioButton.setText("\u25B6");
                    activeAudioButton.getStyleClass().remove("chat-message-audio-play-active");
                }
                activeAudioClip = null;
                activeAudioAttachment = null;
                activeAudioButton = null;
            }
        }
    }

    private Path storeAttachmentForConversation(Path source, Long conversationId, Long senderId) {
        if (source == null) {
            throw new IllegalArgumentException("Le fichier joint est invalide.");
        }
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Le fichier joint est introuvable.");
        }

        try {
            Path destinationDir = ATTACHMENT_STORE_ROOT
                    .resolve(String.valueOf(conversationId == null ? 0L : conversationId))
                    .resolve(String.valueOf(senderId == null ? 0L : senderId));
            Files.createDirectories(destinationDir);
            Path destination = uniqueTargetPath(destinationDir, source.getFileName().toString());
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            return destination;
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'enregistrer la pièce jointe.", e);
        }
    }

    private MessageKind resolveMessageKind(Message message) {
        if (message == null) {
            return MessageKind.TEXT;
        }
        MessageAttachment attachment = primaryAttachment(message);
        String type = normalizeAttachmentType(
                attachment != null ? attachment.getType() : message.getType(),
                attachment != null ? attachment.getStoragePath() : message.getFilePath()
        );
        return switch (type) {
            case "image" -> MessageKind.IMAGE;
            case "audio" -> MessageKind.AUDIO;
            case "pdf" -> MessageKind.PDF;
            case "word" -> MessageKind.WORD;
            case "powerpoint" -> MessageKind.POWERPOINT;
            case "excel" -> MessageKind.EXCEL;
            case "file" -> MessageKind.FILE;
            default -> MessageKind.TEXT;
        };
    }

    private String detectAttachmentType(Path file) {
        if (file == null) {
            return "file";
        }
        return normalizeAttachmentType(null, file.toString());
    }

    private String normalizeAttachmentType(String type, String filePath) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!normalized.isBlank()) {
            if (normalized.equals("image")
                    || normalized.equals("pdf")
                    || normalized.equals("word")
                    || normalized.equals("powerpoint")
                    || normalized.equals("excel")
                    || normalized.equals("audio")
                    || normalized.equals("file")
                    || normalized.equals("text")) {
                return normalized;
            }
        }

        String normalizedPath = normalizeAttachmentPath(filePath);
        if (normalizedPath == null) {
            return "text";
        }

        String lower = normalizedPath.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "pdf";
        }
        if (lower.endsWith(".doc") || lower.endsWith(".docx") || lower.endsWith(".rtf")) {
            return "word";
        }
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx") || lower.endsWith(".pptm")) {
            return "powerpoint";
        }
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".xlsm")) {
            return "excel";
        }
        if (lower.endsWith(".wav") || lower.endsWith(".aiff") || lower.endsWith(".au") || lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".aac") || lower.endsWith(".ogg")) {
            return "audio";
        }
        if (isImageExtension(lower)) {
            return "image";
        }
        return "file";
    }

    private String buildAttachmentCaption(String type, Path file) {
        String fileName = resolveAttachmentFileName(file == null ? null : file.toString());
        return switch (type == null ? "file" : type) {
            case "image" -> "Image : " + fileName;
            case "pdf" -> "PDF : " + fileName;
            case "word" -> "Word : " + fileName;
            case "powerpoint" -> "PowerPoint : " + fileName;
            case "excel" -> "Excel : " + fileName;
            case "audio" -> "Message vocal : " + fileName;
            default -> "Fichier : " + fileName;
        };
    }

    private String buildAttachmentCaption(MessageAttachment attachment) {
        if (attachment == null) {
            return "Fichier";
        }
        String fileName = attachment.getOriginalName() != null ? attachment.getOriginalName() : "fichier";
        return switch (normalizeAttachmentType(attachment.getType(), attachment.getStoragePath())) {
            case "image" -> "Image : " + fileName;
            case "audio" -> "Message vocal : " + fileName;
            case "pdf" -> "PDF : " + fileName;
            case "word" -> "Word : " + fileName;
            case "powerpoint" -> "PowerPoint : " + fileName;
            case "excel" -> "Excel : " + fileName;
            default -> "Fichier : " + fileName;
        };
    }

    private String normalizeAttachmentPath(String filePath) {
        String normalized = filePath == null ? "" : filePath.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String resolveAttachmentFileName(String filePath) {
        String normalized = normalizeAttachmentPath(filePath);
        if (normalized == null) {
            return "fichier";
        }
        try {
            Path path = Path.of(normalized);
            Path fileName = path.getFileName();
            if (fileName != null) {
                return fileName.toString();
            }
        } catch (RuntimeException ignored) {
            // Fallback below.
        }
        return normalized;
    }

    private Path resolveDownloadDirectory() {
        Path downloads = Paths.get(System.getProperty("user.home"), "Downloads");
        if (Files.exists(downloads)) {
            return downloads;
        }
        return Paths.get(System.getProperty("user.home"));
    }

    private String resolveDownloadFileName(String originalName) {
        return originalName == null || originalName.isBlank() ? "document" : originalName.trim();
    }

    private String formatDuration(int seconds) {
        int total = Math.max(0, seconds);
        int minutes = total / 60;
        int remaining = total % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, remaining);
    }

    private String detectAttachmentMimeType(Path file) {
        String lower = file.getFileName() == null ? file.toString().toLowerCase(Locale.ROOT) : file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        if (lower.endsWith(".ppt") || lower.endsWith(".pptx")) {
            return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
        }
        if (lower.endsWith(".xls") || lower.endsWith(".xlsx")) {
            return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        }
        if (isImageExtension(lower)) {
            return "image/*";
        }
        return "application/octet-stream";
    }

    private MessageAttachment buildSelectedAttachment(File file) {
        MessageAttachment attachment = new MessageAttachment();
        attachment.setOriginalName(file.getName());
        attachment.setStoredName(file.getName());
        attachment.setStoragePath(file.getAbsolutePath());
        attachment.setMimeType(detectAttachmentMimeType(file.toPath()));
        attachment.setSize(file.length());
        attachment.setImage(isImageExtension(file.getName().toLowerCase(Locale.ROOT)));
        attachment.setType(detectAttachmentType(file.toPath()));
        attachment.setCreatedAt(LocalDateTime.now());
        return attachment;
    }

    private MessageAttachment copyAttachment(MessageAttachment source, Path storedPath) {
        MessageAttachment attachment = new MessageAttachment();
        attachment.setOriginalName(source != null && source.getOriginalName() != null ? source.getOriginalName() : storedPath.getFileName().toString());
        attachment.setStoredName(storedPath.getFileName().toString());
        attachment.setStoragePath(storedPath.toString());
        attachment.setMimeType(source != null && source.getMimeType() != null ? source.getMimeType() : detectAttachmentMimeType(storedPath));
        attachment.setSize(source != null ? source.getSize() : null);
        attachment.setImage(source != null && source.isImage());
        attachment.setType(source != null ? source.getType() : detectAttachmentType(storedPath));
        attachment.setCreatedAt(LocalDateTime.now());
        return attachment;
    }

    private MessageAttachment primaryAttachment(Message message) {
        if (message == null) {
            return null;
        }
        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            return message.getAttachments().get(0);
        }

        String filePath = message.getFilePath();
        if (filePath == null || filePath.isBlank()) {
            return null;
        }

        MessageAttachment attachment = new MessageAttachment();
        attachment.setOriginalName(resolveAttachmentFileName(filePath));
        attachment.setStoredName(attachment.getOriginalName());
        attachment.setStoragePath(filePath);
        attachment.setMimeType(detectAttachmentMimeType(Path.of(filePath)));
        attachment.setImage("image".equalsIgnoreCase(message.getType()) || isImageExtension(filePath.toLowerCase(Locale.ROOT)));
        attachment.setType(normalizeAttachmentType(message.getType(), filePath));
        attachment.setCreatedAt(message.getCreatedAt());
        return attachment;
    }

    private Path uniqueTargetPath(Path directory, String fileName) throws IOException {
        Path candidate = directory.resolve(fileName);
        if (!Files.exists(candidate)) {
            return candidate;
        }

        String baseName = fileName;
        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            baseName = fileName.substring(0, dot);
            extension = fileName.substring(dot);
        }

        int counter = 1;
        while (Files.exists(candidate)) {
            candidate = directory.resolve(baseName + " (" + counter++ + ")" + extension);
        }
        return candidate;
    }

    private boolean isImageExtension(String lowerPath) {
        return lowerPath.endsWith(".png")
                || lowerPath.endsWith(".jpg")
                || lowerPath.endsWith(".jpeg")
                || lowerPath.endsWith(".gif")
                || lowerPath.endsWith(".webp")
                || lowerPath.endsWith(".bmp")
                || lowerPath.endsWith(".svg");
    }

    private void clearAttachmentSelection() {
        selectedAttachment = null;
        selectedAttachmentPath = null;
        selectedAttachmentName = null;
        updateAttachmentPreview();
    }

    private void startVoiceRecording() {
        try {
            Path recordingsDir = ATTACHMENT_STORE_ROOT.resolve("voice-recordings");
            Files.createDirectories(recordingsDir);

            voiceRecordingFile = recordingsDir.resolve("voice-" + System.currentTimeMillis() + ".wav");
            AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                erreur("L'enregistrement vocal n'est pas supporté sur cette machine.");
                return;
            }

            voiceRecordingLine = (TargetDataLine) AudioSystem.getLine(info);
            voiceRecordingLine.open(format);
            voiceRecordingLine.start();

            recordingVoice = true;
            voiceRecordingStartedAtMillis = System.currentTimeMillis();
            updateVoiceRecordingUi(true);

            AudioInputStream audioInputStream = new AudioInputStream(voiceRecordingLine);
            voiceRecordingThread = new Thread(() -> {
                try {
                    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, voiceRecordingFile.toFile());
                } catch (Exception ex) {
                    Platform.runLater(() -> erreur("Impossible d'enregistrer le message vocal : " + ex.getMessage()));
                }
            }, "voice-recorder");
            voiceRecordingThread.setDaemon(true);
            voiceRecordingThread.start();
        } catch (Exception ex) {
            stopVoiceRecordingInternal(false);
            erreur("Impossible de démarrer l'enregistrement vocal : " + ex.getMessage());
        }
    }

    private void stopVoiceRecordingAndPrepare() {
        long startedAt = voiceRecordingStartedAtMillis;
        Path file = stopVoiceRecordingInternal(true);
        if (file == null) {
            return;
        }

        Task<MessageAttachment> finalizeTask = new Task<>() {
            @Override
            protected MessageAttachment call() {
                MessageAttachment attachment = buildSelectedAttachment(file.toFile());
                attachment.setType("audio");
                attachment.setMimeType("audio/wav");
                attachment.setSize(file.toFile().length());
                attachment.setDuration(Math.max(1, (int) ((System.currentTimeMillis() - startedAt) / 1000L)));
                attachment.setOriginalName("message-vocal-" + System.currentTimeMillis() + ".wav");
                attachment.setStoredName(file.getFileName().toString());
                attachment.setStoragePath(file.toString());
                return attachment;
            }
        };

        finalizeTask.setOnSucceeded(event -> {
            selectedAttachment = finalizeTask.getValue();
            selectedAttachmentPath = selectedAttachment.getStoragePath();
            selectedAttachmentName = selectedAttachment.getOriginalName();
            updateAttachmentPreview();
            lblVoiceRecordState.setText("Message vocal prêt à être envoyé");
            lblVoiceRecordState.getStyleClass().remove("chat-voice-state-recording");
        });
        finalizeTask.setOnFailed(event -> erreur("Impossible de préparer le message vocal."));

        Thread finalizeThread = new Thread(finalizeTask, "voice-finalize");
        finalizeThread.setDaemon(true);
        finalizeThread.start();
    }

    private Path stopVoiceRecordingInternal(boolean prepareSelection) {
        if (!recordingVoice) {
            return voiceRecordingFile;
        }

        recordingVoice = false;
        updateVoiceRecordingUi(false);

        try {
            if (voiceRecordingLine != null) {
                voiceRecordingLine.stop();
                voiceRecordingLine.close();
            }
        } catch (Exception ignored) {
            // ignore
        }

        Thread thread = voiceRecordingThread;
        if (thread != null && thread.isAlive()) {
            try {
                thread.join(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Path file = voiceRecordingFile;
        voiceRecordingLine = null;
        voiceRecordingThread = null;
        voiceRecordingFile = null;
        voiceRecordingStartedAtMillis = 0L;
        if (!prepareSelection) {
            lblVoiceRecordState.setText("");
            lblVoiceRecordState.getStyleClass().remove("chat-voice-state-recording");
        }
        return file;
    }

    private void updateVoiceRecordingUi(boolean recording) {
        if (btnVoiceMessage != null) {
            btnVoiceMessage.setText(recording ? "\u23F9" : "\uD83C\uDFA4");
            if (recording) {
                if (!btnVoiceMessage.getStyleClass().contains("chat-voice-button-recording")) {
                    btnVoiceMessage.getStyleClass().add("chat-voice-button-recording");
                }
            } else {
                btnVoiceMessage.getStyleClass().remove("chat-voice-button-recording");
            }
        }
        if (lblVoiceRecordState != null) {
            lblVoiceRecordState.setText(recording ? "Enregistrement vocal en cours..." : "");
            if (recording) {
                if (!lblVoiceRecordState.getStyleClass().contains("chat-voice-state-recording")) {
                    lblVoiceRecordState.getStyleClass().add("chat-voice-state-recording");
                }
            } else {
                lblVoiceRecordState.getStyleClass().remove("chat-voice-state-recording");
            }
        }
        if (btnAttachFile != null) {
            btnAttachFile.setDisable(recording);
        }
        if (btnEmoji != null) {
            btnEmoji.setDisable(recording);
        }
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
            User keep = selectedParticipants.get(0);
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
                : displayName(userById.get(lastMessage.getSenderId())) + " : " + truncate(buildMessagePreview(lastMessage), 90);

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

    private String buildMessagePreview(Message message) {
        if (message == null) {
            return "";
        }
        String content = Optional.ofNullable(message.getContent()).orElse("").trim();
        if (!content.isBlank()) {
            return content;
        }

        String fileName = resolveAttachmentFileName(message.getFilePath());
        return switch (resolveMessageKind(message)) {
            case IMAGE -> "Image : " + fileName;
            case PDF -> "PDF : " + fileName;
            case WORD -> "Word : " + fileName;
            case POWERPOINT -> "PowerPoint : " + fileName;
            case EXCEL -> "Excel : " + fileName;
            default -> "Fichier : " + fileName;
        };
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

    private enum MessageKind {
        TEXT,
        IMAGE,
        AUDIO,
        PDF,
        WORD,
        POWERPOINT,
        EXCEL,
        FILE;

        private String badgeText() {
            return switch (this) {
                case PDF -> "PDF";
                case AUDIO -> "VOC";
                case WORD -> "DOCX";
                case POWERPOINT -> "PPTX";
                case EXCEL -> "XLSX";
                case FILE -> "FILE";
                default -> "";
            };
        }
    }

    private record MessageItem(Message message, String senderName, boolean mine, String timeText) {
    }

    private record TranslationTarget(String buttonLabel, String apiLanguage, String key) {
    }

    private record TranslationResult(boolean loading, String translatedText, String errorMessage) {

        private static TranslationResult inProgress() {
            return new TranslationResult(true, null, null);
        }

        private static TranslationResult success(String translatedText) {
            return new TranslationResult(false, translatedText, null);
        }

        private static TranslationResult failure(String errorMessage) {
            return new TranslationResult(false, null, errorMessage);
        }
    }

    private record PdfSummaryResult(boolean loading, String summary, String errorMessage) {

        private static PdfSummaryResult inProgress() {
            return new PdfSummaryResult(true, null, null);
        }

        private static PdfSummaryResult success(String summary) {
            return new PdfSummaryResult(false, summary, null);
        }

        private static PdfSummaryResult failure(String errorMessage) {
            return new PdfSummaryResult(false, null, errorMessage);
        }
    }
}
