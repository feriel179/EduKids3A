package com.ecom.model;

public class User {
    private final int id;
    private final String email;
    private final String role;
    private final String passwordHash;
    private final String firstName;
    private final String lastName;
    private final boolean active;

    public User(int id, String email, String role, String passwordHash, String firstName, String lastName, boolean active) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public boolean isActive() {
        return active;
    }
}
