package com.edukids.edukids3a.model;

import java.util.ArrayList;
import java.util.List;

public class ChatDashboardSnapshot {
    private long totalMessages;
    private long totalUsers;
    private long activeUsers7;
    private long activeUsers30;
    private String topUserName;
    private long topUserMessages;
    private long messagesToday;
    private long messagesWeek;
    private ChatDashboardCharts charts = new ChatDashboardCharts();
    private List<ChatTopUser> topUsers = new ArrayList<>();
    private List<ChatRecentActivity> recentActivities = new ArrayList<>();
    private String timezone;
    private String generatedAt;

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

    public ChatDashboardCharts getCharts() {
        return charts;
    }

    public void setCharts(ChatDashboardCharts charts) {
        this.charts = charts == null ? new ChatDashboardCharts() : charts;
    }

    public List<ChatTopUser> getTopUsers() {
        return topUsers;
    }

    public void setTopUsers(List<ChatTopUser> topUsers) {
        this.topUsers = topUsers == null ? new ArrayList<>() : new ArrayList<>(topUsers);
    }

    public List<ChatRecentActivity> getRecentActivities() {
        return recentActivities;
    }

    public void setRecentActivities(List<ChatRecentActivity> recentActivities) {
        this.recentActivities = recentActivities == null ? new ArrayList<>() : new ArrayList<>(recentActivities);
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(String generatedAt) {
        this.generatedAt = generatedAt;
    }

    public ChatDashboardKpis asKpis() {
        return new ChatDashboardKpis(totalMessages, totalUsers, activeUsers7, activeUsers30, topUserName, topUserMessages, messagesToday, messagesWeek);
    }
}
