package com.edukids.edukids3a.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public Conversation() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.messages = new ArrayList<>();
        this.participants = new ArrayList<>();
    }

    public Conversation(Long id, String title, boolean isGroup, String privateKey) {
        this();
        this.id = id;
        this.title = title;
        this.isGroup = isGroup;
        this.privateKey = privateKey;
    }

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

    public boolean isPrivate() {
        return !isGroup;
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
        if (messages == null) {
            messages = new ArrayList<>();
        }
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages == null ? new ArrayList<>() : messages;
    }

    public void addMessage(Message message) {
        if (message == null) {
            return;
        }
        getMessages().add(message);
        updatedAt = LocalDateTime.now();
    }

    public void removeMessage(Message message) {
        if (message == null || messages == null) {
            return;
        }
        if (messages.remove(message)) {
            updatedAt = LocalDateTime.now();
        }
    }

    public List<ConversationParticipant> getParticipants() {
        if (participants == null) {
            participants = new ArrayList<>();
        }
        return participants;
    }

    public void setParticipants(List<ConversationParticipant> participants) {
        this.participants = participants == null ? new ArrayList<>() : participants;
    }

    public void addParticipant(ConversationParticipant participant) {
        if (participant == null) {
            return;
        }
        getParticipants().add(participant);
    }

    public void removeParticipant(ConversationParticipant participant) {
        if (participant == null || participants == null) {
            return;
        }
        participants.remove(participant);
    }

    @Override
    public String toString() {
        if (title != null && !title.isBlank()) {
            return title;
        }
        if (isGroup) {
            return "Conversation de groupe";
        }
        return "Conversation privée";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Conversation that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
