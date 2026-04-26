package com.edukids.edukids3a.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Message {

    private Long id;
    private Long senderId;
    private String content;
    private LocalDateTime createdAt;
    private boolean isRead;
    private Long conversationId;
    private String type;
    private String filePath;
    private List<MessageAttachment> attachments;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private String status;

    // 🔹 Constructeur
    public Message() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isRead = false;
        this.type = "text";
        this.status = "sent";
        this.attachments = new ArrayList<>();
    }

    // 🔹 Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public List<MessageAttachment> getAttachments() {
        if (attachments == null) {
            attachments = new ArrayList<>();
        }
        return attachments;
    }

    public void setAttachments(List<MessageAttachment> attachments) {
        this.attachments = attachments == null ? new ArrayList<>() : attachments;
    }

    public void addAttachment(MessageAttachment attachment) {
        if (attachment == null) {
            return;
        }
        attachment.setMessage(this);
        if (id != null) {
            attachment.setMessageId(id);
        }
        getAttachments().add(attachment);
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // 🔹 Méthode utile
    public boolean isDeleted() {
        return deletedAt != null;
    }
}
