package com.edukids.edukids3a.model;

public enum ChatDashboardPeriod {
    TODAY("today", "Aujourd'hui"),
    DAYS_7("7d", "7 jours"),
    DAYS_30("30d", "30 jours");

    private final String apiValue;
    private final String label;

    ChatDashboardPeriod(String apiValue, String label) {
        this.apiValue = apiValue;
        this.label = label;
    }

    public String getApiValue() {
        return apiValue;
    }

    public String getLabel() {
        return label;
    }
}
