package com.ecom.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatbotService {

    private static final String OPENAI_CONFIG_PATH = "/config/openai.properties";
    private static final Path LOCAL_OPENAI_CONFIG_PATH = Paths.get("config", "openai.local.properties");
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private final Properties properties;
    private final HttpClient httpClient;

    public ChatbotService() {
        this.properties = loadProperties();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String ask(String userMessage) throws IOException, InterruptedException {
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("Le message du chatbot est vide.");
        }

        String apiKey = resolveRequiredProperty("openai.api.key", "OPENAI_API_KEY");
        String endpoint = resolveProperty("openai.text.endpoint", "OPENAI_TEXT_ENDPOINT", "https://openrouter.ai/api/v1/chat/completions");
        String model = resolveProperty("openai.text.model", "OPENAI_TEXT_MODEL", "openai/gpt-4o-mini");

        String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "Tu es un assistant client pour une application e-commerce EduKids. Reponds en francais simple, concret et utile."},
                    {"role": "user", "content": "%s"}
                  ],
                  "temperature": 0.7
                }
                """.formatted(escapeJson(model), escapeJson(userMessage.trim()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(45))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", resolveProperty("openai.http.referer", "OPENAI_HTTP_REFERER", "http://localhost"))
                .header("X-Title", resolveProperty("openai.http.title", "OPENAI_HTTP_TITLE", "java-ecom"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException("Chatbot echoue (HTTP " + response.statusCode() + "): " + compactBody(response.body()));
        }

        Matcher contentMatcher = CONTENT_PATTERN.matcher(response.body());
        while (contentMatcher.find()) {
            String content = unescapeJson(contentMatcher.group(1)).trim();
            if (!content.isBlank() && !content.startsWith("{")) {
                return content;
            }
        }

        throw new IOException("Reponse chatbot invalide: contenu absent.");
    }

    private String resolveRequiredProperty(String propertyName, String envName) {
        String value = resolveProperty(propertyName, envName, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Configuration manquante: " + propertyName + " (ou variable " + envName + ").");
        }
        return value.trim();
    }

    private String resolveProperty(String propertyName, String envName, String defaultValue) {
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = properties.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        return defaultValue;
    }

    private Properties loadProperties() {
        Properties loaded = new Properties();
        try (InputStream inputStream = ChatbotService.class.getResourceAsStream(OPENAI_CONFIG_PATH)) {
            if (inputStream != null) {
                loaded.load(inputStream);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de charger la configuration OpenAI.", exception);
        }

        if (Files.exists(LOCAL_OPENAI_CONFIG_PATH)) {
            try (InputStream inputStream = Files.newInputStream(LOCAL_OPENAI_CONFIG_PATH)) {
                loaded.load(inputStream);
            } catch (IOException exception) {
                throw new IllegalStateException("Impossible de charger la configuration OpenAI locale.", exception);
            }
        }

        return loaded;
    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private String unescapeJson(String raw) {
        return raw.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }

    private String compactBody(String rawBody) {
        if (rawBody == null) {
            return "";
        }
        String compact = rawBody.replaceAll("\\s+", " ").trim();
        return compact.length() > 280 ? compact.substring(0, 280) + "..." : compact;
    }
}
