package com.edukids.edukids3a.model;

public class ChatDashboardKpis {
    private long totalMessages;
    private long totalUsers;
    private long activeUsers7;
    private long activeUsers30;
    private String topUserName;
    private long topUserMessages;
    private long messagesToday;
    private long messagesWeek;

    public ChatDashboardKpis() {
    }

    public ChatDashboardKpis(long totalMessages, long totalUsers, long activeUsers7, long activeUsers30,
                             String topUserName, long topUserMessages, long messagesToday, long messagesWeek) {
        this.totalMessages = totalMessages;
        this.totalUsers = totalUsers;
        this.activeUsers7 = activeUsers7;
        this.activeUsers30 = activeUsers30;
        this.topUserName = topUserName;
        this.topUserMessages = topUserMessages;
        this.messagesToday = messagesToday;
        this.messagesWeek = messagesWeek;
    }

    public long getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(long totalMessages) {
        this.totalMessages = totalMessages;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getActiveUsers7() {
        return activeUsers7;
    }

    public void setActiveUsers7(long activeUsers7) {
        this.activeUsers7 = activeUsers7;
    }

    public long getActiveUsers30() {
        return activeUsers30;
    }

    public void setActiveUsers30(long activeUsers30) {
        this.activeUsers30 = activeUsers30;
    }

    public String getTopUserName() {
        return topUserName;
    }

    public void setTopUserName(String topUserName) {
        this.topUserName = topUserName;
    }

    public long getTopUserMessages() {
        return topUserMessages;
    }

    public void setTopUserMessages(long topUserMessages) {
        this.topUserMessages = topUserMessages;
    }

    public long getMessagesToday() {
        return messagesToday;
    }

    public void setMessagesToday(long messagesToday) {
        this.messagesToday = messagesToday;
    }

    public long getMessagesWeek() {
        return messagesWeek;
    }

    public void setMessagesWeek(long messagesWeek) {
        this.messagesWeek = messagesWeek;
    }
}
