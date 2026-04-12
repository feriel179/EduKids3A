package com.edukids.edukids3a.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Conversation {

    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String title;
    private boolean isGroup;
    private String privateKey;
    private LocalDateTime lastAutoReplyAt;

    private List<Message> messages;
    private List<ConversationParticipant> participants;

    // 🔹 Constructeur par défaut
    public Conversation() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.messages = new ArrayList<>();
        this.participants = new ArrayList<>();
    }

    // 🔹 Constructeur avec paramètres
    public Conversation(Long id, String title, boolean isGroup, String privateKey) {
        this();
        this.id = id;
        this.title = title;
        this.isGroup = isGroup;
        this.privateKey = privateKey;
    }

    // 🔹 Getters & Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isGroup() {
        return isGroup;
    }

    public void setGroup(boolean group) {
        isGroup = group;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public LocalDateTime getLastAutoReplyAt() {
        return lastAutoReplyAt;
    }

    public void setLastAutoReplyAt(LocalDateTime lastAutoReplyAt) {
        this.lastAutoReplyAt = lastAutoReplyAt;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        this.updatedAt = LocalDateTime.now();
    }

    public void removeMessage(Message message) {
        this.messages.remove(message);
    }

    public List<ConversationParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<ConversationParticipant> participants) {
        this.participants = participants;
    }

    public void addParticipant(ConversationParticipant participant) {
        this.participants.add(participant);
    }

    public void removeParticipant(ConversationParticipant participant) {
        this.participants.remove(participant);
    }
}
