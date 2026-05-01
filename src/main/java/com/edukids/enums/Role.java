package com.edukids.enums;

public enum Role {
    ROLE_ADMIN("ROLE_ADMIN", "Admin"),
    ROLE_PARENT("ROLE_PARENT", "Parent"),
    ROLE_ELEVE("ROLE_ELEVE", "Eleve");

    private final String dbValue;
    private final String displayName;

    Role(String dbValue, String displayName) {
        this.dbValue = dbValue;
        this.displayName = displayName;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Role fromDbValue(String dbValue) {
        if (dbValue == null) {
            return ROLE_ELEVE;
        }
        String normalized = dbValue.trim();
        for (Role role : values()) {
            if (role.dbValue.equalsIgnoreCase(normalized)
                    || role.displayName.equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        return switch (normalized.toUpperCase()) {
            case "ADMIN", "ROLE_ADMIN" -> ROLE_ADMIN;
            case "PARENT", "ROLE_PARENT" -> ROLE_PARENT;
            case "ELEVE", "ROLE_ELEVE", "USER" -> ROLE_ELEVE;
            default -> throw new IllegalArgumentException("Unknown role: " + dbValue);
        };
    }

    @Override
    public String toString() {
        return displayName;
    }
}
