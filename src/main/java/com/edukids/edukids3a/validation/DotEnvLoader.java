package com.edukids.edukids3a.validation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lecture minimale des fichiers {@code .env.local} puis {@code .env} à la racine du processus (répertoire de travail).
 */
public final class DotEnvLoader {

    private DotEnvLoader() {
    }

    public static String get(String key) {
        String v = readValue(Path.of(".env.local"), key);
        if (v != null && !v.isBlank()) {
            return v.trim();
        }
        v = readValue(Path.of(".env"), key);
        if (v != null && !v.isBlank()) {
            return v.trim();
        }
        return null;
    }

    private static String readValue(Path path, String key) {
        try {
            if (!Files.isRegularFile(path)) {
                return null;
            }
            String prefix = key + "=";
            for (String rawLine : Files.readAllLines(path)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.startsWith(prefix)) {
                    continue;
                }
                String value = line.substring(prefix.length()).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        } catch (IOException ignored) {
            return null;
        }
        return null;
    }
}
