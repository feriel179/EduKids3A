package com.edukids.edukids3a.model;

import java.time.LocalDateTime;

public class ConversationParticipant {

    private Long id;
    private Long conversationId;
    private Long userId;
    private String role;
    private LocalDateTime deletedAt;
    private LocalDateTime lastReadAt;
    private LocalDateTime joinedAt;
    private LocalDateTime hiddenAt;

    // 🔹 Constructeur
    public ConversationParticipant() {
        this.joinedAt = LocalDateTime.now();
    }

    // 🔹 Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getLastReadAt() {
        return lastReadAt;
    }

    public void setLastReadAt(LocalDateTime lastReadAt) {
        this.lastReadAt = lastReadAt;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getHiddenAt() {
        return hiddenAt;
    }

    public void setHiddenAt(LocalDateTime hiddenAt) {
        this.hiddenAt = hiddenAt;
    }

    // 🔹 Méthodes utiles

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isHidden() {
        return hiddenAt != null;
    }
}
