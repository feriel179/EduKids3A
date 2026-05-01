package com.edukids.edukids3a.model;

public class ChatTopUser {
    private Long userId;
    private int rank;
    private String name;
    private String email;
    private int messages;
    private double progress;
    private String lastActivity;

    public ChatTopUser() {
    }

    public ChatTopUser(Long userId, int rank, String name, String email, int messages, double progress, String lastActivity) {
        this.userId = userId;
        this.rank = rank;
        this.name = name;
        this.email = email;
        this.messages = messages;
        this.progress = progress;
        this.lastActivity = lastActivity;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getMessages() {
        return messages;
    }

    public void setMessages(int messages) {
        this.messages = messages;
    }

    public double getProgress() {
        return progress;
    }

    public void setProgress(double progress) {
        this.progress = progress;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(String lastActivity) {
        this.lastActivity = lastActivity;
    }
}
