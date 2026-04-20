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
        for (Role role : values()) {
            if (role.dbValue.equals(dbValue)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + dbValue);
    }

    @Override
    public String toString() {
        return displayName;
    }
}