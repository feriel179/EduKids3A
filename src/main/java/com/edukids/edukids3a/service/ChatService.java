package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.Conversation;
import com.edukids.edukids3a.model.ConversationParticipant;
import com.edukids.edukids3a.model.Message;
import com.edukids.edukids3a.model.User;
import com.edukids.edukids3a.utils.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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
import java.util.stream.Collectors;

public class ChatService {

    private static final int DEFAULT_SEARCH_LIMIT = 12;
    private static final Object SCHEMA_LOCK = new Object();
    private static volatile boolean schemaEnsured;

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

        try (Connection connection = DBConnection.getConnection();
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

        try (Connection connection = DBConnection.getConnection();
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

    public Optional<ConversationParticipant> findParticipant(long conversationId, long userId) {
        String sql = """
                SELECT id, conversation_id, user_id, role, deleted_at, last_read_at, joined_at, hidden_at
                FROM conversation_participant
                WHERE conversation_id = ?
                  AND user_id = ?
                ORDER BY id DESC
                LIMIT 1
                """;

        try (Connection connection = DBConnection.getConnection();
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

        try (Connection connection = DBConnection.getConnection()) {
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

        try (Connection connection = DBConnection.getConnection();
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

        try (Connection connection = DBConnection.getConnection();
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

        try (Connection connection = DBConnection.getConnection();
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

        try (Connection connection = DBConnection.getConnection();
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

        try (Connection connection = DBConnection.getConnection();
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

        try (Connection connection = DBConnection.getConnection();
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

        if (!group) {
            long otherUserId = participants.stream()
                    .filter(id -> id != creatorUserId)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Participant manquant."));
            Conversation existing = findPrivateConversation(creatorUserId, otherUserId);
            if (existing != null) {
                return existing;
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Conversation conversation = new Conversation();
        conversation.setTitle(title == null ? null : title.trim());
        conversation.setGroup(group);
        conversation.setUpdatedAt(now);

        try (Connection connection = DBConnection.getConnection()) {
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
        String trimmed = content == null ? "" : content.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException("Le message ne peut pas être vide.");
        }

        LocalDateTime now = LocalDateTime.now();
        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setContent(trimmed);
        message.setCreatedAt(now);
        message.setUpdatedAt(now);

        String insertMessageSql = """
                INSERT INTO message (sender_id, content, created_at, is_read, conversation_id, type, file_path, updated_at, deleted_at, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String updateConversationSql = """
                UPDATE conversation
                SET updated_at = ?
                WHERE id = ?
                """;

        try (Connection connection = DBConnection.getConnection()) {
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

                update.setTimestamp(1, Timestamp.valueOf(now));
                update.setLong(2, conversationId);
                update.executeUpdate();

                connection.commit();
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
        try (Connection connection = DBConnection.getConnection();
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
        try (Connection connection = DBConnection.getConnection();
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

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            statement.setLong(2, conversationId);
            statement.setLong(3, userId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Impossible de masquer la conversation.", e);
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

        try (Connection connection = DBConnection.getConnection();
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

            try (Connection connection = DBConnection.getConnection();
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

        try (Connection connection = DBConnection.getConnection();
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
}
