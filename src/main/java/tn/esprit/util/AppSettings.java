package tn.esprit.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AppSettings {
    private static final Path ENV_PATH = Path.of(System.getProperty("user.dir"), ".env");
    private static final Path ENV_LOCAL_PATH = Path.of(System.getProperty("user.dir"), ".env.local");
    private static final Map<String, String> FILE_SETTINGS = new LinkedHashMap<>();

    static {
        reload();
    }

    private AppSettings() {
    }

    public static synchronized String get(String key, String fallback) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }

        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String fileValue = FILE_SETTINGS.get(key);
        if (fileValue != null && !fileValue.isBlank()) {
            return fileValue.trim();
        }

        return fallback;
    }

    public static synchronized String get(String key) {
        return get(key, "");
    }

    public static synchronized void saveLocal(String key, String value) {
        if (key == null || key.isBlank()) {
            return;
        }

        Map<String, String> localSettings = readEnvFile(ENV_LOCAL_PATH);
        if (value == null || value.isBlank()) {
            localSettings.remove(key.trim());
        } else {
            localSettings.put(key.trim(), value.trim());
        }

        writeEnvFile(ENV_LOCAL_PATH, localSettings);
        reload();
    }

    public static synchronized void reload() {
        FILE_SETTINGS.clear();
        loadEnvFile(FILE_SETTINGS, ENV_PATH);
        loadEnvFile(FILE_SETTINGS, ENV_LOCAL_PATH);
    }

    private static Map<String, String> readEnvFile(Path envPath) {
        Map<String, String> settings = new LinkedHashMap<>();
        loadEnvFile(settings, envPath);
        return settings;
    }

    private static void loadEnvFile(Map<String, String> settings, Path envPath) {
        if (!Files.exists(envPath)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(envPath);
            for (String line : lines) {
                parseEnvLine(settings, line);
            }
        } catch (IOException ignored) {
        }
    }

    private static void writeEnvFile(Path envPath, Map<String, String> settings) {
        try {
            if (envPath.getParent() != null) {
                Files.createDirectories(envPath.getParent());
            }

            List<String> lines = settings.entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue())
                    .toList();

            Files.write(envPath, lines);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save local application settings.", exception);
        }
    }

    private static void parseEnvLine(Map<String, String> settings, String line) {
        if (line == null) {
            return;
        }

        String trimmedLine = line.trim();
        if (trimmedLine.isBlank() || trimmedLine.startsWith("#")) {
            return;
        }

        if (trimmedLine.startsWith("export ")) {
            trimmedLine = trimmedLine.substring(7).trim();
        }

        int equalsIndex = trimmedLine.indexOf('=');
        if (equalsIndex <= 0) {
            return;
        }

        String key = trimmedLine.substring(0, equalsIndex).trim();
        String value = trimmedLine.substring(equalsIndex + 1).trim();
        if (key.isBlank() || value.isBlank()) {
            return;
        }

        settings.put(key, stripQuotes(value));
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            boolean doubleQuoted = value.startsWith("\"") && value.endsWith("\"");
            boolean singleQuoted = value.startsWith("'") && value.endsWith("'");
            if (doubleQuoted || singleQuoted) {
                return value.substring(1, value.length() - 1).trim();
            }
        }
        return value;
    }
}
