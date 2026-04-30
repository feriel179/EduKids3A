package com.edukids.entities;

import java.sql.Timestamp;

public class Session {
    private int id;
    private int userId;
    private String username;
    private String role;
    private Timestamp loginTime;
    private Timestamp logoutTime;
    private String sessionStatus;
    private String sessionToken;
    private String ipAddress;
    private Timestamp createdAt;

    public Session() {
    }

    public Session(int id, int userId, String username, String role, Timestamp loginTime,
                   Timestamp logoutTime, String sessionStatus, String sessionToken,
                   String ipAddress, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.loginTime = loginTime;
        this.logoutTime = logoutTime;
        this.sessionStatus = sessionStatus;
        this.sessionToken = sessionToken;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Timestamp getLoginTime() { return loginTime; }
    public void setLoginTime(Timestamp loginTime) { this.loginTime = loginTime; }

    public Timestamp getLogoutTime() { return logoutTime; }
    public void setLogoutTime(Timestamp logoutTime) { this.logoutTime = logoutTime; }

    public String getSessionStatus() { return sessionStatus; }
    public void setSessionStatus(String sessionStatus) { this.sessionStatus = sessionStatus; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public boolean isActive() {
        return "active".equalsIgnoreCase(sessionStatus);
    }

    public String getDuration() {
        if (loginTime == null) {
            return "N/A";
        }

        Timestamp endTime = logoutTime != null ? logoutTime : new Timestamp(System.currentTimeMillis());
        long minutes = (endTime.getTime() - loginTime.getTime()) / (1000 * 60);
        long hours = minutes / 60;
        String suffix = logoutTime == null && isActive() ? " (active)" : "";

        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m" + suffix;
        }
        return minutes + "m" + suffix;
    }
}
