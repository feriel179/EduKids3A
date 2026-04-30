package com.edukids.utils;

import javafx.scene.Scene;

import java.net.URL;

public class ThemeManager {

    public static final String LIGHT_THEME = "light-theme";
    public static final String DARK_THEME = "dark-theme";

    private static String currentTheme = LIGHT_THEME;

    public static void setTheme(Scene scene, String theme) {
        if (scene == null || theme == null || theme.isBlank()) {
            return;
        }

        scene.getStylesheets().removeIf(url ->
                url.contains("light-theme") || url.contains("dark-theme"));

        URL cssResource = ThemeManager.class.getResource("/com/edukids/css/" + theme + ".css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
            currentTheme = theme;
        }
    }

    public static void toggleTheme(Scene scene) {
        setTheme(scene, currentTheme.equals(LIGHT_THEME) ? DARK_THEME : LIGHT_THEME);
    }

    public static String getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDarkTheme() {
        return currentTheme.equals(DARK_THEME);
    }
}
