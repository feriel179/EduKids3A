package com.edukids.utils;

import javafx.scene.Scene;

public class ThemeManager {

    public static final String LIGHT_THEME = "light-theme";
    public static final String DARK_THEME = "dark-theme";
    
    private static String currentTheme = LIGHT_THEME;

    public static void setTheme(Scene scene, String theme) {
        scene.getStylesheets().removeIf(url -> 
            url.contains("light-theme") || url.contains("dark-theme")
        );
        
        String cssResource = "/com/edukids/css/" + theme + ".css";
        String cssUrl = ThemeManager.class.getResource(cssResource).toExternalForm();
        scene.getStylesheets().add(cssUrl);
        
        currentTheme = theme;
    }

    public static void toggleTheme(Scene scene) {
        String newTheme = currentTheme.equals(LIGHT_THEME) ? DARK_THEME : LIGHT_THEME;
        setTheme(scene, newTheme);
    }

    public static String getCurrentTheme() {
        return currentTheme;
    }

    public static boolean isDarkTheme() {
        return currentTheme.equals(DARK_THEME);
    }
}

