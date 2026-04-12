package com.edukids.edukids3a.security;

import com.edukids.edukids3a.model.User;

public final class AuthSession {

    private static volatile User currentUser;

    private AuthSession() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }
}
