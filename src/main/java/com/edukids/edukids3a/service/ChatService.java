package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.Conversation;
import com.edukids.edukids3a.model.ConversationParticipant;
import com.edukids.edukids3a.model.Message;
import com.edukids.edukids3a.model.MessageAttachment;
import com.edukids.edukids3a.model.User;
import com.edukids.edukids3a.realtime.ChatRealtimeHub;
import com.edukids.utils.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DatabaseMetaData;
import java.text.Normalizer;
import java.time.LocalDateTime;
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
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class ChatService {

    private static final int DEFAULT_SEARCH_LIMIT = 12;
    private static final Object SCHEMA_LOCK = new Object();
    private static volatile boolean schemaEnsured;
    private static final List<AutoReplyRule> AUTO_REPLY_RULES = List.of(
            new AutoReplyRule(List.of("bonjour"), List.of(
                    "Bonjour {Nom}, merci de nous avoir contactés. Comment puis-je vous assister aujourd’hui ?")),
            new AutoReplyRule(List.of("bjr"), List.of("Bonjour {Nom} 😄 Que puis-je faire pour vous ?")),
            new AutoReplyRule(List.of("salut"), List.of("Bonjour {Nom} 👋")),
            new AutoReplyRule(List.of("bjr", "slt", "coucou", "hey", "cc", "yo", "re bonjour", "bonjour à tous", "bonjour admin", "salut admin"), List.of(
                    "Bonjour {Nom} 👋",
                    "Bonjour {Nom}, comment puis-je vous aider ?",
                    "Salut {Nom} 😄",
                    "Bonjour {Nom}, bienvenue !",
                    "Bonjour {Nom}, je suis disponible pour vous aider."
            )),
            new AutoReplyRule(List.of("comment ça va ?", "vous allez bien ?", "ça va admin ?", "ça va ?", "tout va bien ?"), List.of(
                    "Merci {Nom}, je vais très bien 😊",
                    "Je vais bien {Nom}, comment puis-je vous aider ?",
                    "Merci pour votre message {Nom}."
            )),
            new AutoReplyRule(List.of("aide", "help", "besoin d’aide", "pouvez-vous m’aider ?", "j’ai un problème", "problème"), List.of(
                    "Bien sûr {Nom}, expliquez-moi votre problème.",
                    "Je suis là pour vous aider {Nom}.",
                    "Pouvez-vous me donner plus de détails ?"
            )),
            new AutoReplyRule(List.of("merci", "merci beaucoup", "thanks", "merci admin", "c’est bon merci"), List.of(
                    "Avec plaisir {Nom} 😊",
                    "Je vous en prie {Nom}.",
                    "Toujours à votre service 👌"
            )),
            new AutoReplyRule(List.of("ok", "d’accord", "c bon", "parfait", "nickel"), List.of(
                    "Très bien {Nom} 👍",
                    "Parfait {Nom}.",
                    "D’accord, n’hésitez pas si besoin."
            )),
            new AutoReplyRule(List.of("bon matin", "bon après-midi", "bonne soirée", "bonne nuit"), List.of(
                    "Bon après-midi {Nom} ☀️",
                    "Bonne soirée {Nom} 🌙",
                    "Bonne nuit {Nom}, à demain !"
            )),
            new AutoReplyRule(List.of("bonjour tout le monde", "salut à tous", "hello groupe"), List.of(
                    "Bonjour {Nom} et bienvenue à tous 👋",
                    "Salut {Nom} 😊"
            ))
    );

    public ChatService() {
        ensureSchema();
    }

    public List<User> searchUsers(String query, long currentUserId, Collection<Long> excludedUserIds, int limit) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        int max = limit > 0 ? limit : DEFAULT_SEARCH_LIMIT;
        String like = "%" + normalized.toLowerCase(Locale.ROOT) + "%";
        Set<Long> excluded = new LinkedHashSet<>();
        excluded.add(currentUserId);
        if (excludedUserIds != null) {
            excluded.addAll(excludedUserIds);
        }

        String sql = """
                SELECT id, email, roles, password, first_name, last_name, is_active
                FROM `user`
                WHERE is_active = 1
                  AND (
                        LOWER(email) LIKE ?
                     OR LOWER(first_name) LIKE ?
                     OR LOWER(last_name) LIKE ?
                     OR LOWER(CONCAT(first_name, ' ', last_name)) LIKE ?
                  )
                ORDER BY last_name, first_name, email
                LIMIT ?
                """;

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, like);
            statement.setString(2, like);
            statement.setString(3, like);
            statement.setString(4, like);
            statement.setInt(5, max);

            try (ResultSet rs = statement.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) {
                    long id = rs.getLong("id");
                    if (excluded.contains(id)) {
                        continue;
                    }
                    users.add(readUser(rs));
                }
                return users;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erreur lors de la recherche d'utilisateurs.", e);
        }
    }

    public Map<Long, User> findUsersByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        List<Long> orderedIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (orderedIds.isEmpty()) {
            return Map.of();
        }

        String placeholders = orderedIds.stream().map(id -> "?").collect(Collectors.joining(", "));
        String sql = """
                SELECT id, email, roles, password, first_name, last_name, is_active
                FROM `user`
                WHERE id IN (%s)
                ORDER BY last_name, first_name, email
                """.formatted(placeholders);

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < orderedIds.size(); i++) {
                statement.setLong(i + 1, orderedIds.get(i));
            }

            Map<Long, User> users = new LinkedHashMap<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    User user = readUser(rs);
                    users.put(user.getId().longValue(), user);
                }
            }
            return users;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les utilisateurs du chat.", e);
        }
    }

    public Optional<User> findFirstAdminUser(long excludedUserId) {
        String sql = """
                SELECT id, email, roles, password, first_name, last_name, is_active
                FROM `user`
                WHERE is_active = 1
                  AND id <> ?
                  AND LOWER(roles) LIKE '%admin%'
                ORDER BY id
                LIMIT 1
                """;

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, excludedUserId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readUser(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de trouver un administrateur pour la conversation.", e);
        }
    }

    public Optional<ConversationParticipant> findParticipant(long conversationId, long userId) {
        String sql = """
                SELECT id, conversation_id, user_id, role, deleted_at, last_read_at, joined_at, hidden_at
                FROM conversation_participant
                WHERE conversation_id = ?
                  AND user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """;

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, conversationId);
            statement.setLong(2, userId);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readParticipant(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger le participant.", e);
        }
    }

    public boolean canManageGroupMembers(long conversationId, long userId) {
        Conversation conversation = findConversationById(conversationId);
        if (conversation == null || !conversation.isGroup()) {
            return false;
        }
        return findParticipant(conversationId, userId)
                .filter(participant -> participant.getDeletedAt() == null)
                .map(participant -> isAdminRole(participant.getRole()))
                .orElse(false);
    }

    public boolean canViewGroupMembers(long conversationId, long userId) {
        Conversation conversation = findConversationById(conversationId);
        if (conversation == null || !conversation.isGroup()) {
            return false;
        }
        return findParticipant(conversationId, userId)
                .filter(participant -> participant.getDeletedAt() == null)
                .isPresent();
    }

    public List<User> searchUsersForGroupAdd(long conversationId, String query, long currentUserId, int limit) {
        Set<Long> excluded = new LinkedHashSet<>();
        excluded.add(currentUserId);
        excluded.addAll(loadParticipants(conversationId).stream()
                .map(ConversationParticipant::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return searchUsers(query, currentUserId, excluded, limit);
    }

    public List<User> listGroupMembers(long conversationId) {
        List<ConversationParticipant> participants = loadParticipants(conversationId);
        if (participants.isEmpty()) {
            return List.of();
        }
        Map<Long, User> users = findUsersByIds(participants.stream()
                .map(ConversationParticipant::getUserId)
                .filter(Objects::nonNull)
                .toList());
        List<User> members = new ArrayList<>();
        for (ConversationParticipant participant : participants) {
            User user = users.get(participant.getUserId());
            if (user != null) {
                members.add(user);
            }
        }
        return members;
    }

    public ConversationParticipant addGroupMember(long conversationId, long userId, long requestedByUserId) {
        Conversation conversation = findConversationById(conversationId);
        if (conversation == null || !conversation.isGroup()) {
            throw new IllegalArgumentException("La conversation n'est pas un groupe.");
        }
        if (!canManageGroupMembers(conversationId, requestedByUserId)) {
            throw new SecurityException("Accès refusé.");
        }
        if (findParticipant(conversationId, userId).filter(p -> p.getDeletedAt() == null).isPresent()) {
            throw new IllegalArgumentException("Cet utilisateur est déjà membre du groupe.");
        }

        try (Connection connection = MyConnection.getInstance().getCnx()) {
            connection.setAutoCommit(false);
            try {
                ConversationParticipant participant = findParticipant(conversationId, userId)
                        .filter(ConversationParticipant::isDeleted)
                        .map(existing -> {
                            try {
                                return reactivateParticipant(connection, existing.getId(), conversationId, userId, "member");
                            } catch (SQLException e) {
                                throw new IllegalStateException("Impossible de réactiver le membre.", e);
                            }
                        })
                        .orElseGet(() -> {
                            try {
                                return insertParticipantRecord(connection, conversationId, userId, "member");
                            } catch (SQLException e) {
                                throw new IllegalStateException("Impossible d'ajouter le membre.", e);
                            }
                        });
                connection.commit();
                return participant;
            } catch (RuntimeException | SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'ajouter le membre.", e);
        }
    }

    public void removeGroupMember(long conversationId, long userId, long requestedByUserId) {
        Conversation conversation = findConversationById(conversationId);
        if (conversation == null || !conversation.isGroup()) {
            throw new IllegalArgumentException("La conversation n'est pas un groupe.");
        }
        if (!canManageGroupMembers(conversationId, requestedByUserId)) {
            throw new SecurityException("Accès refusé.");
        }
        ConversationParticipant participant = findParticipant(conversationId, userId)
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new IllegalArgumentException("Ce membre n'est plus présent dans le groupe."));
        if (Objects.equals(participant.getUserId(), requestedByUserId) && isAdminRole(participant.getRole())) {
            throw new IllegalArgumentException("Vous ne pouvez pas retirer l'administrateur courant.");
        }

        String sql = """
                UPDATE conversation_participant
                SET deleted_at = ?, hidden_at = ?
                WHERE id = ?
                """;

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setTimestamp(1, Timestamp.valueOf(now));
            statement.setTimestamp(2, Timestamp.valueOf(now));
            statement.setLong(3, participant.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer le membre.", e);
        }
    }

    public List<Conversation> listConversationsForUser(long userId) {
        String sql = """
                SELECT c.id, c.created_at, c.updated_at, c.title, c.is_group, c.private_key, c.last_auto_reply_at
                FROM conversation c
                WHERE EXISTS (
                    SELECT 1
                    FROM conversation_participant cp
                    WHERE cp.conversation_id = c.id
                      AND cp.user_id = ?
                      AND cp.deleted_at IS NULL
                      AND cp.hidden_at IS NULL
                )
                ORDER BY COALESCE(c.updated_at, c.created_at) DESC, c.id DESC
                """;

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);

            List<Conversation> conversations = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    conversations.add(readConversation(rs));
                }
            }

            for (Conversation conversation : conversations) {
                conversation.setParticipants(loadParticipants(conversation.getId()));
                conversation.setMessages(new ArrayList<>());
            }

            return conversations;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les conversations.", e);
        }
    }

    public Conversation findConversationById(long conversationId) {
        String sql = """
                SELECT id, created_at, updated_at, title, is_group, private_key, last_auto_reply_at
                FROM conversation
                WHERE id = ?
                """;

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, conversationId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Conversation conversation = readConversation(rs);
                    conversation.setParticipants(loadParticipants(conversationId));
                    conversation.setMessages(new ArrayList<>());
                    return conversation;
                }
            }
            return null;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger la conversation.", e);
        }
    }

    public List<ConversationParticipant> loadParticipants(long conversationId) {
        String sql = """
                SELECT id, conversation_id, user_id, role, deleted_at, last_read_at, joined_at, hidden_at
                FROM conversation_participant
                WHERE conversation_id = ?
                  AND deleted_at IS NULL
                ORDER BY CASE WHEN role = 'owner' THEN 0 ELSE 1 END, joined_at ASC, id ASC
                """;

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, conversationId);

            List<ConversationParticipant> participants = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    participants.add(readParticipant(rs));
                }
            }
            return participants;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les participants.", e);
        }
    }

    public List<Message> listMessages(long conversationId) {
        return listMessages(conversationId, Integer.MAX_VALUE);
    }

    public List<Message> listMessages(long conversationId, int limit) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, sender_id, content, created_at, is_read, conversation_id, type, file_path, updated_at, deleted_at, status
                FROM message
                WHERE conversation_id = ?
                  AND deleted_at IS NULL
                ORDER BY created_at ASC, id ASC
                """);
        if (limit > 0 && limit != Integer.MAX_VALUE) {
            sql.append(" LIMIT ?");
        }

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setLong(1, conversationId);
            if (limit > 0 && limit != Integer.MAX_VALUE) {
                statement.setInt(2, limit);
            }

            List<Message> messages = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    messages.add(readMessage(rs));
                }
            }
            return messages;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger les messages.", e);
        }
    }

    public Optional<Message> findLastMessage(long conversationId) {
        String sql = """
                SELECT id, sender_id, content, created_at, is_read, conversation_id, type, file_path, updated_at, deleted_at, status
                FROM message
                WHERE conversation_id = ?
                  AND deleted_at IS NULL
                ORDER BY created_at DESC, id DESC
                LIMIT 1
                """;

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, conversationId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readMessage(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger le dernier message.", e);
        }
    }

    public Conversation createConversation(long creatorUserId, String title, boolean group, Collection<Long> participantUserIds) {
        Set<Long> participants = new LinkedHashSet<>();
        participants.add(creatorUserId);
        if (participantUserIds != null) {
            participantUserIds.stream()
                    .filter(Objects::nonNull)
                    .filter(id -> id != creatorUserId)
                    .forEach(participants::add);
        }

        if (group) {
            if (participants.size() < 2) {
                throw new IllegalArgumentException("Une conversation de groupe doit contenir au moins un autre participant.");
            }
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Le nom du groupe est obligatoire.");
            }
        } else {
            if (participants.size() != 2) {
                throw new IllegalArgumentException("Une conversation privée doit contenir exactement deux participants.");
            }
        }

        String conversationTitle = title == null ? null : title.trim();
        if (!group && (conversationTitle == null || conversationTitle.isBlank())) {
            conversationTitle = "Conversation privée";
        }

        if (!group) {
            long otherUserId = participants.stream()
                    .filter(id -> id != creatorUserId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Participant manquant."));
            Conversation existing = findPrivateConversation(creatorUserId, otherUserId);
            if (existing != null) {
                reactivatePrivateConversationForUser(existing.getId(), creatorUserId);
                return existing;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = new Conversation();
        conversation.setTitle(conversationTitle);
        conversation.setGroup(group);
        conversation.setPrivateKey(UUID.randomUUID().toString());
        conversation.setUpdatedAt(now);

        try (Connection connection = MyConnection.getInstance().getCnx()) {
            connection.setAutoCommit(false);
            try {
                long conversationId = insertConversation(connection, conversation);
                conversation.setId(conversationId);

                insertParticipant(connection, conversationId, creatorUserId, "owner");
                for (Long userId : participants) {
                    if (userId == creatorUserId) {
                        continue;
                    }
                    insertParticipant(connection, conversationId, userId, "member");
                }

                connection.commit();
                return findConversationById(conversationId);
            } catch (RuntimeException | SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de créer la conversation.", e);
        }
    }

    public Conversation createPrivateConversation(long creatorUserId, long otherUserId) {
        return createConversation(creatorUserId, null, false, List.of(otherUserId));
    }

    public Message sendMessage(long conversationId, long senderId, String content) {
        return sendMessage(conversationId, senderId, content, null, null);
    }

    public Message sendMessage(long conversationId, long senderId, String content, MessageAttachment attachment) {
        String type = attachment == null ? null : attachment.getType();
        String filePath = attachment == null ? null : attachment.getStoragePath();
        Message message = sendMessage(conversationId, senderId, content, type, filePath);
        if (attachment != null) {
            attachment.setMessage(message);
            attachment.setMessageId(message.getId());
            message.setAttachments(List.of(attachment));
        }
        return message;
    }

    public Message sendMessage(long conversationId, long senderId, String content, String type, String filePath) {
        return sendMessageInternal(conversationId, senderId, content, type, filePath, false);
    }

    private Message sendMessageInternal(long conversationId, long senderId, String content, String type, String filePath, boolean automaticReply) {
        String trimmed = content == null ? "" : content.trim();
        String normalizedType = normalizeMessageType(type, filePath);
        String normalizedFilePath = normalizeFilePath(filePath);

        if (trimmed.isBlank() && normalizedFilePath == null) {
            throw new IllegalArgumentException("Le message ne peut pas être vide.");
        }

        LocalDateTime now = LocalDateTime.now();
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setContent(trimmed.isBlank() ? buildAttachmentLabel(normalizedType, normalizedFilePath) : trimmed);
        message.setType(normalizedType);
        message.setFilePath(normalizedFilePath);
        if (automaticReply) {
            message.setStatus("auto_reply");
        }
        message.setAttachments(buildAttachments(message));
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        boolean shouldTryAutoReply = !automaticReply;

        String insertMessageSql = """
                INSERT INTO message (sender_id, content, created_at, is_read, conversation_id, type, file_path, updated_at, deleted_at, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String updateConversationSql = """
                UPDATE conversation
                SET updated_at = ?
                WHERE id = ?
                """;

        try (Connection connection = MyConnection.getInstance().getCnx()) {
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement(insertMessageSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement update = connection.prepareStatement(updateConversationSql)) {
                bindMessageInsert(insert, message);
                insert.executeUpdate();

                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (keys.next()) {
                        message.setId(keys.getLong(1));
                    }
                }

                syncAttachmentsWithMessage(message);

                update.setTimestamp(1, Timestamp.valueOf(now));
                update.setLong(2, conversationId);
                update.executeUpdate();

                connection.commit();
                try {
                    ChatRealtimeHub.getInstance().broadcastMessageSaved(message);
                } catch (RuntimeException exception) {
                    // La diffusion temps reel ne doit jamais faire echouer l'envoi principal.
                    // Le message est deja en base, on se contente de logger si besoin.
                }
                return message;
            } catch (RuntimeException | SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible d'envoyer le message.", e);
        }
        finally {
            if (shouldTryAutoReply) {
                maybeSendAutomaticReply(message);
            }
        }
    }

    private void maybeSendAutomaticReply(Message incomingMessage) {
        try {
            Optional<AutoReplyMatch> match = buildAutoReplyMatch(incomingMessage);
            if (match.isEmpty()) {
                return;
            }

            AutoReplyMatch autoReply = match.get();
            Message reply = sendMessageInternal(
                    incomingMessage.getConversationId(),
                    autoReply.adminUserId(),
                    autoReply.replyText(),
                    "text",
                    null,
                    true
            );
            markConversationAutoReply(incomingMessage.getConversationId(), reply.getCreatedAt());
        } catch (RuntimeException ignored) {
            // Les réponses automatiques ne doivent jamais bloquer le message principal.
        }
    }

    private Optional<AutoReplyMatch> buildAutoReplyMatch(Message incomingMessage) {
        if (incomingMessage == null || incomingMessage.getSenderId() == null) {
            return Optional.empty();
        }
        if (!"text".equalsIgnoreCase(incomingMessage.getType())) {
            return Optional.empty();
        }
        if ("auto_reply".equalsIgnoreCase(incomingMessage.getStatus())) {
            return Optional.empty();
        }
        if (isAdminUser(incomingMessage.getSenderId())) {
            return Optional.empty();
        }

        long conversationId = incomingMessage.getConversationId() == null ? -1L : incomingMessage.getConversationId();
        Optional<Long> adminId = findConversationAdminUserId(conversationId);
        if (adminId.isEmpty() || adminId.get() == incomingMessage.getSenderId()) {
            return Optional.empty();
        }

        Map<Long, User> users = findUsersByIds(List.of(incomingMessage.getSenderId(), adminId.get()));
        User sender = users.get(incomingMessage.getSenderId());
        if (sender == null) {
            return Optional.empty();
        }

        String normalizedMessage = normalizeForTrigger(incomingMessage.getContent());
        if (normalizedMessage.isBlank()) {
            return Optional.empty();
        }

        for (AutoReplyRule rule : AUTO_REPLY_RULES) {
            if (!rule.matches(normalizedMessage)) {
                continue;
            }
            String template = rule.randomResponse();
            String replyText = template.replace("{Nom}", resolveRealName(sender));
            return Optional.of(new AutoReplyMatch(adminId.get(), replyText));
        }

        return Optional.empty();
    }

    private Optional<Long> findConversationAdminUserId(long conversationId) {
        if (conversationId <= 0) {
            return Optional.empty();
        }

        List<ConversationParticipant> participants = loadParticipants(conversationId);
        if (participants.isEmpty()) {
            return Optional.empty();
        }

        Map<Long, User> users = findUsersByIds(participants.stream()
                .map(ConversationParticipant::getUserId)
                .filter(Objects::nonNull)
                .toList());

        for (ConversationParticipant participant : participants) {
            User user = users.get(participant.getUserId());
            if (user != null && isAdminUser(user)) {
                return Optional.of(user.getId().longValue());
            }
        }

        return Optional.empty();
    }

    private void markConversationAutoReply(long conversationId, LocalDateTime autoReplyAt) {
        if (conversationId <= 0 || autoReplyAt == null) {
            return;
        }

        String sql = """
                UPDATE conversation
                SET last_auto_reply_at = ?, updated_at = ?
                WHERE id = ?
                """;

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(autoReplyAt));
            statement.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            statement.setLong(3, conversationId);
            statement.executeUpdate();
        } catch (SQLException ignored) {
            // best effort
        }
    }

    private boolean isAdminUser(long userId) {
        if (userId <= 0) {
            return false;
        }
        User user = findUsersByIds(List.of(userId)).get(userId);
        return isAdminUser(user);
    }

    private static boolean isAdminUser(User user) {
        if (user == null) {
            return false;
        }
        return isAdminRole(user.getRole());
    }

    private String resolveRealName(User user) {
        if (user == null) {
            return "utilisateur";
        }
        if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
            return user.getFirstName().trim();
        }
        if (user.getLastName() != null && !user.getLastName().isBlank()) {
            return user.getLastName().trim();
        }
        String fullName = user.getNom();
        if (fullName != null && !fullName.isBlank()) {
            return fullName.trim();
        }
        return "utilisateur";
    }

    private static String normalizeForTrigger(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");
        normalized = normalized.toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^\\p{Alnum}\\s]", " ");
        normalized = normalized.replaceAll("\\s+", " ").trim();
        return normalized;
    }

    private static boolean containsTrigger(String normalizedMessage, String normalizedTrigger) {
        if (normalizedMessage == null || normalizedTrigger == null || normalizedMessage.isBlank() || normalizedTrigger.isBlank()) {
            return false;
        }
        String haystack = " " + normalizedMessage + " ";
        String needle = " " + normalizedTrigger + " ";
        return haystack.contains(needle);
    }

    private String normalizeMessageType(String type, String filePath) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!normalized.isBlank()) {
            return switch (normalized) {
                case "image", "pdf", "word", "powerpoint", "excel", "audio", "file", "text" -> normalized;
                default -> detectMessageTypeFromFilePath(filePath);
            };
        }
        return detectMessageTypeFromFilePath(filePath);
    }

    private String detectMessageTypeFromFilePath(String filePath) {
        String normalizedPath = normalizeFilePath(filePath);
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
        if (lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".gif")
                || lower.endsWith(".webp")
                || lower.endsWith(".bmp")
                || lower.endsWith(".svg")) {
            return "image";
        }
        return "file";
    }

    private String normalizeFilePath(String filePath) {
        String normalized = filePath == null ? "" : filePath.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeAttachmentType(String type, String filePath) {
        String normalized = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        if (!normalized.isBlank()) {
            return switch (normalized) {
                case "image", "pdf", "word", "powerpoint", "excel", "audio", "file", "text" -> normalized;
                default -> detectMessageTypeFromFilePath(filePath);
            };
        }
        return detectMessageTypeFromFilePath(filePath);
    }

    private String buildAttachmentLabel(String type, String filePath) {
        String fileName = filePath == null ? "fichier" : java.nio.file.Paths.get(filePath).getFileName().toString();
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

    private List<MessageAttachment> buildAttachments(Message message) {
        if (message == null || message.getFilePath() == null || message.getFilePath().isBlank()) {
            return List.of();
        }

        Path path;
        try {
            path = Path.of(message.getFilePath().trim());
        } catch (RuntimeException ex) {
            return List.of();
        }

        MessageAttachment attachment = new MessageAttachment();
        attachment.setMessage(message);
        attachment.setOriginalName(path.getFileName() == null ? message.getFilePath() : path.getFileName().toString());
        attachment.setStoredName(attachment.getOriginalName());
        attachment.setStoragePath(message.getFilePath().trim());
        attachment.setMimeType(detectMimeType(path));
        attachment.setSize(readFileSize(path));
        attachment.setImage("image".equalsIgnoreCase(message.getType()));
        attachment.setType(normalizeAttachmentType(message.getType(), message.getFilePath()));
        attachment.setDuration(readAudioDurationSeconds(path, attachment.getType()));
        attachment.setCreatedAt(message.getCreatedAt());
        return List.of(attachment);
    }

    private void syncAttachmentsWithMessage(Message message) {
        if (message == null) {
            return;
        }
        if (message.getAttachments() == null || message.getAttachments().isEmpty()) {
            return;
        }
        for (MessageAttachment attachment : message.getAttachments()) {
            if (attachment == null) {
                continue;
            }
            attachment.setMessage(message);
            attachment.setMessageId(message.getId());
        }
    }

    private String detectMimeType(Path path) {
        try {
            String mimeType = Files.probeContentType(path);
            if (mimeType != null && !mimeType.isBlank()) {
                return mimeType;
            }
        } catch (Exception ignored) {
            // best effort
        }

        String lower = (path.getFileName() == null ? path.toString() : path.getFileName().toString()).toLowerCase(Locale.ROOT);
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
        if (lower.endsWith(".wav")) {
            return "audio/wav";
        }
        if (lower.endsWith(".mp3")) {
            return "audio/mpeg";
        }
        if (lower.endsWith(".m4a")) {
            return "audio/mp4";
        }
        if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp") || lower.endsWith(".svg")) {
            return "image/*";
        }
        return "application/octet-stream";
    }

    private Integer readAudioDurationSeconds(Path path, String type) {
        if (!"audio".equalsIgnoreCase(type) || path == null) {
            return null;
        }
        try {
            javax.sound.sampled.AudioInputStream stream = javax.sound.sampled.AudioSystem.getAudioInputStream(path.toFile());
            try (stream) {
                javax.sound.sampled.AudioFormat format = stream.getFormat();
                long frames = stream.getFrameLength();
                if (frames <= 0 || format.getFrameRate() <= 0) {
                    return null;
                }
                return Math.max(1, (int) Math.round(frames / format.getFrameRate()));
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long readFileSize(Path path) {
        try {
            if (Files.exists(path)) {
                return Files.size(path);
            }
        } catch (Exception ignored) {
            // best effort
        }
        return null;
    }

    public Message editMessage(long messageId, long senderId, String newContent) {
        String trimmed = newContent == null ? "" : newContent.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Le message ne peut pas être vide.");
        }

        String sql = """
                UPDATE message
                SET content = ?, updated_at = ?
                WHERE id = ?
                  AND sender_id = ?
                  AND deleted_at IS NULL
                """;

        LocalDateTime now = LocalDateTime.now();
        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, trimmed);
            statement.setTimestamp(2, Timestamp.valueOf(now));
            statement.setLong(3, messageId);
            statement.setLong(4, senderId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new SecurityException("Vous ne pouvez modifier que vos propres messages.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de modifier le message.", e);
        }

        return findMessageById(messageId)
                .orElseThrow(() -> new IllegalStateException("Message introuvable après modification."));
    }

    public void deleteMessage(long messageId, long senderId) {
        String sql = """
                UPDATE message
                SET deleted_at = ?, status = 'deleted', updated_at = ?
                WHERE id = ?
                  AND sender_id = ?
                  AND deleted_at IS NULL
                """;

        LocalDateTime now = LocalDateTime.now();
        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(now));
            statement.setTimestamp(2, Timestamp.valueOf(now));
            statement.setLong(3, messageId);
            statement.setLong(4, senderId);
            int updated = statement.executeUpdate();
            if (updated == 0) {
                throw new SecurityException("Vous ne pouvez supprimer que vos propres messages.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de supprimer le message.", e);
        }
    }

    public void hideConversation(long conversationId, long userId) {
        String sql = """
                UPDATE conversation_participant
                SET hidden_at = ?
                WHERE conversation_id = ?
                  AND user_id = ?
                  AND hidden_at IS NULL
                """;

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            statement.setLong(2, conversationId);
            statement.setLong(3, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de masquer la conversation.", e);
        }
    }

    private void reactivatePrivateConversationForUser(long conversationId, long userId) {
        String sql = """
                UPDATE conversation_participant
                SET hidden_at = NULL, deleted_at = NULL
                WHERE conversation_id = ?
                  AND user_id = ?
                """;

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, conversationId);
            statement.setLong(2, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de réactiver la conversation privée.", e);
        }
    }

    private Conversation findPrivateConversation(long userA, long userB) {
        String sql = """
                SELECT c.id, c.created_at, c.updated_at, c.title, c.is_group, c.private_key, c.last_auto_reply_at
                FROM conversation c
                JOIN conversation_participant cp ON cp.conversation_id = c.id AND cp.deleted_at IS NULL
                WHERE c.is_group = 0
                GROUP BY c.id, c.created_at, c.updated_at, c.title, c.is_group, c.private_key, c.last_auto_reply_at
                HAVING SUM(CASE WHEN cp.user_id = ? THEN 1 ELSE 0 END) > 0
                   AND SUM(CASE WHEN cp.user_id = ? THEN 1 ELSE 0 END) > 0
                   AND COUNT(*) = 2
                ORDER BY c.updated_at DESC, c.id DESC
                LIMIT 1
                """;

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userA);
            statement.setLong(2, userB);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Conversation conversation = readConversation(rs);
                    conversation.setParticipants(loadParticipants(conversation.getId()));
                    conversation.setMessages(new ArrayList<>());
                    return conversation;
                }
            }
            return null;
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de rechercher une conversation privée existante.", e);
        }
    }

    private long insertConversation(Connection connection, Conversation conversation) throws SQLException {
        String sql = """
                INSERT INTO conversation (created_at, updated_at, title, is_group, private_key, last_auto_reply_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setTimestamp(1, Timestamp.valueOf(Optional.ofNullable(conversation.getCreatedAt()).orElse(LocalDateTime.now())));
            statement.setTimestamp(2, Timestamp.valueOf(Optional.ofNullable(conversation.getUpdatedAt()).orElse(LocalDateTime.now())));
            statement.setString(3, conversation.getTitle());
            statement.setBoolean(4, conversation.isGroup());
            statement.setString(5, conversation.getPrivateKey());
            if (conversation.getLastAutoReplyAt() != null) {
                statement.setTimestamp(6, Timestamp.valueOf(conversation.getLastAutoReplyAt()));
            } else {
                statement.setNull(6, java.sql.Types.TIMESTAMP);
            }
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getLong(1);
                }
            }
        }
        throw new SQLException("Aucune clé générée pour la conversation.");
    }

    private void insertParticipant(Connection connection, long conversationId, long userId, String role) throws SQLException {
        insertParticipantRecord(connection, conversationId, userId, role);
    }

    private ConversationParticipant insertParticipantRecord(Connection connection, long conversationId, long userId, String role) throws SQLException {
        String sql = """
                INSERT INTO conversation_participant (conversation_id, user_id, role, deleted_at, last_read_at, joined_at, hidden_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, conversationId);
            statement.setLong(2, userId);
            statement.setString(3, role);
            statement.setNull(4, java.sql.Types.TIMESTAMP);
            statement.setNull(5, java.sql.Types.TIMESTAMP);
            statement.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            statement.setNull(7, java.sql.Types.TIMESTAMP);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    ConversationParticipant participant = new ConversationParticipant();
                    participant.setId(keys.getLong(1));
                    participant.setConversationId(conversationId);
                    participant.setUserId(userId);
                    participant.setRole(role);
                    participant.setJoinedAt(LocalDateTime.now());
                    return participant;
                }
            }
        }
        throw new SQLException("Aucune clé générée pour le participant.");
    }

    private ConversationParticipant reactivateParticipant(Connection connection, long participantId, long conversationId, long userId, String role) throws SQLException {
        String sql = """
                UPDATE conversation_participant
                SET role = ?, deleted_at = NULL, last_read_at = NULL, joined_at = ?, hidden_at = NULL
                WHERE id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            LocalDateTime now = LocalDateTime.now();
            statement.setString(1, role);
            statement.setTimestamp(2, Timestamp.valueOf(now));
            statement.setLong(3, participantId);
            statement.executeUpdate();

            ConversationParticipant participant = new ConversationParticipant();
            participant.setId(participantId);
            participant.setConversationId(conversationId);
            participant.setUserId(userId);
            participant.setRole(role);
            participant.setJoinedAt(now);
            participant.setDeletedAt(null);
            participant.setHiddenAt(null);
            participant.setLastReadAt(null);
            return participant;
        }
    }

    private void bindMessageInsert(PreparedStatement statement, Message message) throws SQLException {
        statement.setLong(1, message.getSenderId());
        statement.setString(2, message.getContent());
        statement.setTimestamp(3, Timestamp.valueOf(message.getCreatedAt()));
        statement.setBoolean(4, message.isRead());
        statement.setLong(5, message.getConversationId());
        statement.setString(6, message.getType());
        statement.setString(7, message.getFilePath());
        statement.setTimestamp(8, Timestamp.valueOf(message.getUpdatedAt()));
        if (message.getDeletedAt() != null) {
            statement.setTimestamp(9, Timestamp.valueOf(message.getDeletedAt()));
        } else {
            statement.setNull(9, java.sql.Types.TIMESTAMP);
        }
        statement.setString(10, message.getStatus());
    }

    private void ensureSchema() {
        if (schemaEnsured) {
            return;
        }
        synchronized (SCHEMA_LOCK) {
            if (schemaEnsured) {
                return;
            }

            try (Connection connection = MyConnection.getInstance().getCnx();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS conversation (
                            id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            created_at DATETIME(6) NOT NULL,
                            updated_at DATETIME(6) NOT NULL,
                            title VARCHAR(255) NULL,
                            is_group BOOLEAN NOT NULL DEFAULT FALSE,
                            private_key VARCHAR(255) NULL,
                            last_auto_reply_at DATETIME(6) NULL
                        )
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS conversation_participant (
                            id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            conversation_id INT NOT NULL,
                            user_id INT NOT NULL,
                            role VARCHAR(50) NULL,
                            deleted_at DATETIME(6) NULL,
                            last_read_at DATETIME(6) NULL,
                            joined_at DATETIME(6) NOT NULL,
                            hidden_at DATETIME(6) NULL,
                            CONSTRAINT fk_cp_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE,
                            CONSTRAINT fk_cp_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
                        )
                        """);
                ensureColumnExists(connection, "conversation", "private_key", "VARCHAR(255) NULL");
                ensureColumnExists(connection, "conversation", "last_auto_reply_at", "DATETIME(6) NULL");
                ensureColumnExists(connection, "conversation_participant", "hidden_at", "DATETIME(6) NULL");
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS message (
                            id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            sender_id INT NOT NULL,
                            content LONGTEXT NOT NULL,
                            created_at DATETIME(6) NOT NULL,
                            is_read BOOLEAN NOT NULL DEFAULT FALSE,
                            conversation_id INT NOT NULL,
                            type VARCHAR(50) NOT NULL DEFAULT 'text',
                            file_path VARCHAR(500) NULL,
                            updated_at DATETIME(6) NOT NULL,
                            deleted_at DATETIME(6) NULL,
                            status VARCHAR(50) NOT NULL DEFAULT 'sent',
                            CONSTRAINT fk_message_sender FOREIGN KEY (sender_id) REFERENCES `user`(id) ON DELETE CASCADE,
                            CONSTRAINT fk_message_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(id) ON DELETE CASCADE
                        )
                        """);
                createIndexIfNeeded(statement, "CREATE INDEX idx_conversation_updated_at ON conversation(updated_at)");
                createIndexIfNeeded(statement, "CREATE INDEX idx_cp_user ON conversation_participant(user_id)");
                createIndexIfNeeded(statement, "CREATE INDEX idx_message_conversation ON message(conversation_id)");
                schemaEnsured = true;
            } catch (SQLException e) {
                throw new IllegalStateException("Impossible d'initialiser le schéma du chat.", e);
            }
        }
    }

    private void createIndexIfNeeded(Statement statement, String sql) {
        try {
            statement.executeUpdate(sql);
        } catch (SQLException ignored) {
            // L'index existe déjà ou le SGBD refuse la création non bloquante.
        }
    }

    private void ensureColumnExists(Connection connection, String tableName, String columnName, String columnDefinition) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            if (columns.next()) {
                return;
            }
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition);
        }
    }

    private Conversation readConversation(ResultSet rs) throws SQLException {
        Conversation conversation = new Conversation();
        conversation.setId(rs.getLong("id"));
        conversation.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        conversation.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        conversation.setTitle(rs.getString("title"));
        conversation.setGroup(rs.getBoolean("is_group"));
        conversation.setPrivateKey(rs.getString("private_key"));
        conversation.setLastAutoReplyAt(toLocalDateTime(rs.getTimestamp("last_auto_reply_at")));
        conversation.setMessages(new ArrayList<>());
        conversation.setParticipants(new ArrayList<>());
        return conversation;
    }

    private ConversationParticipant readParticipant(ResultSet rs) throws SQLException {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setId(rs.getLong("id"));
        participant.setConversationId(rs.getLong("conversation_id"));
        participant.setUserId(rs.getLong("user_id"));
        participant.setRole(rs.getString("role"));
        participant.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        participant.setLastReadAt(toLocalDateTime(rs.getTimestamp("last_read_at")));
        participant.setJoinedAt(toLocalDateTime(rs.getTimestamp("joined_at")));
        participant.setHiddenAt(toLocalDateTime(rs.getTimestamp("hidden_at")));
        return participant;
    }

    private Message readMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getLong("id"));
        message.setSenderId(rs.getLong("sender_id"));
        message.setContent(rs.getString("content"));
        message.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        message.setRead(rs.getBoolean("is_read"));
        message.setConversationId(rs.getLong("conversation_id"));
        message.setType(rs.getString("type"));
        message.setFilePath(rs.getString("file_path"));
        message.setAttachments(buildAttachments(message));
        message.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        message.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
        message.setStatus(rs.getString("status"));
        return message;
    }

    private Optional<Message> findMessageById(long messageId) {
        String sql = """
                SELECT id, sender_id, content, created_at, is_read, conversation_id, type, file_path, updated_at, deleted_at, status
                FROM message
                WHERE id = ?
                """;

        try (Connection connection = MyConnection.getInstance().getCnx();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, messageId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readMessage(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de charger le message.", e);
        }
    }

    private User readUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setRoles(rs.getString("roles"));
        user.setPassword(rs.getString("password"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private static boolean isAdminRole(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.trim().toLowerCase(Locale.ROOT);
        return "owner".equals(normalized) || "admin".equals(normalized);
    }

    private record AutoReplyRule(List<String> triggers, List<String> responses) {
        boolean matches(String normalizedMessage) {
            if (normalizedMessage == null || normalizedMessage.isBlank()) {
                return false;
            }
            for (String trigger : triggers) {
                String normalizedTrigger = normalizeForTrigger(trigger);
                if (containsTrigger(normalizedMessage, normalizedTrigger)) {
                    return true;
                }
            }
            return false;
        }

        String randomResponse() {
            if (responses == null || responses.isEmpty()) {
                return "";
            }
            int index = ThreadLocalRandom.current().nextInt(responses.size());
            return responses.get(index);
        }
    }

    private record AutoReplyMatch(long adminUserId, String replyText) {
    }
}
