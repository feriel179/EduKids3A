package com.edukids.edukids3a.auth;

public enum Role {
    ROLE_ADMIN("ROLE_ADMIN", "Admin"),
    ROLE_PARENT("ROLE_PARENT", "Parent"),
    ROLE_ELEVE("ROLE_ELEVE", "Élève");

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
        for (Role role : values()) {
            if (role.dbValue.equals(dbValue)) {
                return role;
            }
        }
        return ROLE_ELEVE;
    }
}
