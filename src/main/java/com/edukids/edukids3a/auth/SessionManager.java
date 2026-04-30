package com.edukids.edukids3a.auth;

public class SessionManager {
    private boolean connected = true;

    public boolean isConnected() {
        return connected;
    }

    public void disconnect() {
        connected = false;
    }

    public void reconnect() {
        connected = true;
    }
}
