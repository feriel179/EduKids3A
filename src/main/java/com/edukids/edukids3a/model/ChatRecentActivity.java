package com.edukids.edukids3a.model;

public class ChatRecentActivity {
    private String title;
    private String subtitle;
    private String timestamp;
    private String type;

    public ChatRecentActivity() {
    }

    public ChatRecentActivity(String title, String subtitle, String timestamp, String type) {
        this.title = title;
        this.subtitle = subtitle;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
