package com.edukids.edukids3a.model;

import java.util.Locale;

public class User {

    private Integer id;
    private String email;
    private String roles;
    private String password;
    private String firstName;
    private String lastName;
    private boolean active;

    public User() {
    }

    public User(Integer id, String email, String roles, String password, String firstName, String lastName, boolean active) {
        this.id = id;
        this.email = email;
        this.roles = roles;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.active = active;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getRole() {
        return resolveEffectiveRole(roles);
    }

    public void setRole(String role) {
        this.roles = role;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getMotDePasse() {
        return password;
    }

    public void setMotDePasse(String motDePasse) {
        this.password = motDePasse;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getNom() {
        StringBuilder sb = new StringBuilder();
        if (firstName != null && !firstName.isBlank()) {
            sb.append(firstName.trim());
        }
        if (lastName != null && !lastName.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(lastName.trim());
        }
        if (!sb.isEmpty()) {
            return sb.toString();
        }
        return email;
    }

    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) {
            this.firstName = null;
            this.lastName = null;
            return;
        }

        String trimmed = nom.trim();
        int firstSpace = trimmed.indexOf(' ');
        if (firstSpace < 0) {
            this.firstName = trimmed;
            this.lastName = null;
            return;
        }

        this.firstName = trimmed.substring(0, firstSpace).trim();
        this.lastName = trimmed.substring(firstSpace + 1).trim();
    }

    private static String resolveEffectiveRole(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }

        String compact = normalized.toLowerCase(Locale.ROOT)
                .replace("[", " ")
                .replace("]", " ")
                .replace("\"", " ")
                .replace("'", " ")
                .replace("{", " ")
                .replace("}", " ");

        if (compact.contains("admin")) {
            return "admin";
        }
        if (compact.contains("parent")) {
            return "parent";
        }
        if (compact.contains("user")) {
            return "user";
        }

        int separator = indexOfAny(normalized, ',', '|', ';', ' ');
        if (separator >= 0) {
            normalized = normalized.substring(0, separator).trim();
        }

        if (normalized.regionMatches(true, 0, "ROLE_", 0, 5)) {
            normalized = normalized.substring(5);
        }

        return normalized.toLowerCase(Locale.ROOT);
    }

    private static int indexOfAny(String value, char... chars) {
        int best = -1;
        for (char c : chars) {
            int idx = value.indexOf(c);
            if (idx >= 0 && (best < 0 || idx < best)) {
                best = idx;
            }
        }
        return best;
    }
}
