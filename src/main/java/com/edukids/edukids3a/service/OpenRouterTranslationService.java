package com.edukids.edukids3a.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

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
    private static final List<String> DOT_ENV_FILES = List.of(".env.local", ".env");

    private static final Gson GSON = new Gson();

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

        return sendChatCompletion(
                MODEL,
                "You are a translation engine. Return only the translated text.",
                prompt,
                128
        );
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

        return sendChatCompletion(
                PDF_SUMMARY_MODEL,
                "Tu es un assistant qui résume des documents PDF en français.",
                prompt,
                180
        );
    }

    private String sendChatCompletion(String model, String systemPrompt, String userPrompt, int maxTokens) {
        String apiKey = resolveApiKey();

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("model", model);
            payload.addProperty("temperature", 0);
            payload.addProperty("max_tokens", maxTokens);

            JsonArray messages = new JsonArray();
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", systemPrompt);
            messages.add(systemMessage);

            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", userPrompt);
            messages.add(userMessage);
            payload.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder(URI.create(ENDPOINT))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("HTTP-Referer", "https://edukids.local")
                    .header("X-Title", "EduKids3A")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new IllegalStateException(buildHttpError(response.statusCode(), response.body()));
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonElement contentNode = readContentNode(root);
            if (contentNode == null || contentNode.isJsonNull() || contentNode.getAsString().isBlank()) {
                String apiError = readApiError(root);
                if (!apiError.isBlank()) {
                    throw new IllegalStateException("Réponse OpenRouter invalide: " + apiError);
                }
                throw new IllegalStateException("Réponse OpenRouter invalide: choices[0].message.content est manquant.");
            }

            return contentNode.getAsString().trim();
        } catch (JsonSyntaxException e) {
            throw new IllegalStateException("Réponse OpenRouter invalide ou tronquée: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalStateException("Erreur réseau ou JSON lors de l'appel OpenRouter: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Requête OpenRouter interrompue.", e);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Réponse OpenRouter invalide ou trop volumineuse: " + e.getMessage(), e);
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
        Path current = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        while (current != null) {
            for (String fileName : DOT_ENV_FILES) {
                Path dotEnvPath = current.resolve(fileName);
                if (Files.isRegularFile(dotEnvPath)) {
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
                        throw new IllegalStateException("Impossible de lire le fichier " + fileName + ": " + e.getMessage(), e);
                    }
                }
            }
            current = current.getParent();
        }

        return null;
    }

    private JsonElement readContentNode(JsonObject root) {
        if (root == null || !root.has("choices") || !root.get("choices").isJsonArray()) {
            return null;
        }

        JsonArray choices = root.getAsJsonArray("choices");
        if (choices.isEmpty() || !choices.get(0).isJsonObject()) {
            return null;
        }

        JsonObject firstChoice = choices.get(0).getAsJsonObject();
        if (!firstChoice.has("message") || !firstChoice.get("message").isJsonObject()) {
            return null;
        }

        JsonObject message = firstChoice.getAsJsonObject("message");
        return message.has("content") ? message.get("content") : null;
    }

    private String readApiError(JsonObject root) {
        if (root == null) {
            return "";
        }
        if (root.has("error") && root.get("error").isJsonObject()) {
            JsonObject error = root.getAsJsonObject("error");
            if (error.has("message") && !error.get("message").isJsonNull()) {
                return error.get("message").getAsString();
            }
        }
        return "";
    }

    private String buildHttpError(int statusCode, String body) {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.length() > 400) {
            trimmed = trimmed.substring(0, 400) + "...";
        }
        return "OpenRouter a retourné le statut " + statusCode + (trimmed.isBlank() ? "" : " - " + trimmed);
    }
}
