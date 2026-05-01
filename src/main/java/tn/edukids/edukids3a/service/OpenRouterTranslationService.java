package com.edukids.edukids3a.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Service de traduction basé sur OpenRouter.
 * Le modèle doit uniquement renvoyer la traduction, sans explication.
 */
public class OpenRouterTranslationService {

    private static final String ENDPOINT = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "openai/gpt-oss-120b:free";
    private static final String PDF_SUMMARY_MODEL = "openai/gpt-oss-120b:free";
    private static final String API_KEY_ENV = "OPENROUTER_API_KEY";
    private static final String API_KEY_PROPERTY = "openrouter.api.key";
    private static final String DOT_ENV_FILE = ".env";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    /**
     * Traduit un texte vers une langue cible.
     *
     * @param text texte source
     * @param targetLanguage langue cible lisible par le modèle, par exemple "French", "English" ou "Arabic"
     * @return la traduction brute renvoyée par le modèle
     */
    public String translate(String text, String targetLanguage) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Le texte à traduire est vide.");
        }
        if (targetLanguage == null || targetLanguage.isBlank()) {
            throw new IllegalArgumentException("La langue cible est vide.");
        }

        String prompt = """
                Translate the following text into %s.
                Return only the translation, with no explanation, no quotes, and no markdown.

                Text:
                %s
                """.formatted(targetLanguage, text.trim());

        return sendChatCompletion(MODEL, "You are a translation engine. Return only the translated text.", prompt);
    }

    public String summarizePdfText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Le texte du PDF est vide.");
        }

        String prompt = """
                Résume ce document PDF en français en 3 lignes maximum. Ne donne aucune explication supplémentaire.

                Document PDF:
                %s
                """.formatted(limitPdfText(text.trim()));

        return sendChatCompletion(PDF_SUMMARY_MODEL, "Tu es un assistant qui résume des documents PDF en français.", prompt);
    }

    private String sendChatCompletion(String model, String systemPrompt, String userPrompt) {
        String apiKey = resolveApiKey();

        try {
            ObjectNode payload = MAPPER.createObjectNode();
            payload.put("model", model);
            payload.put("temperature", 0);

            ArrayNode messages = payload.putArray("messages");
            messages.addObject()
                    .put("role", "system")
                    .put("content", systemPrompt);
            messages.addObject()
                    .put("role", "user")
                    .put("content", userPrompt);

            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("HTTP-Referer", "https://edukids.local")
                    .header("X-Title", "EduKids3A")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(payload), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(buildHttpError(response.statusCode(), response.body()));
            }

            JsonNode root = MAPPER.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.isNull() || contentNode.asText().isBlank()) {
                String apiError = root.path("error").path("message").asText("");
                if (!apiError.isBlank()) {
                    throw new IllegalStateException("Réponse OpenRouter invalide: " + apiError);
                }
                throw new IllegalStateException("Réponse OpenRouter invalide: choices[0].message.content est manquant.");
            }

            return contentNode.asText().trim();
        } catch (IOException e) {
            throw new IllegalStateException("Erreur réseau ou JSON lors de l'appel OpenRouter: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Requête OpenRouter interrompue.", e);
        }
    }

    private String limitPdfText(String text) {
        int maxChars = 12_000;
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "\n\n[Texte tronqué pour respecter la limite de requête.]";
    }

    private String resolveApiKey() {
        String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getProperty(API_KEY_PROPERTY);
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = readApiKeyFromDotEnv();
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Clé OpenRouter manquante. Définis la variable d'environnement " + API_KEY_ENV
                    + ", la propriété Java " + API_KEY_PROPERTY + " ou un fichier .env.");
        }
        return apiKey.trim();
    }

    private String readApiKeyFromDotEnv() {
        Path dotEnvPath = Path.of(System.getProperty("user.dir", "."), DOT_ENV_FILE);
        if (!Files.isRegularFile(dotEnvPath)) {
            return null;
        }

        try {
            List<String> lines = Files.readAllLines(dotEnvPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.startsWith(API_KEY_ENV + "=")) {
                    continue;
                }

                String value = trimmed.substring((API_KEY_ENV + "=").length()).trim();
                if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                return value;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le fichier .env: " + e.getMessage(), e);
        }

        return null;
    }

    private String buildHttpError(int statusCode, String body) {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.length() > 400) {
            trimmed = trimmed.substring(0, 400) + "...";
        }
        return "OpenRouter a retourné le statut " + statusCode + (trimmed.isBlank() ? "" : " - " + trimmed);
    }
}
