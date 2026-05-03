package com.ecom.service;

import tn.esprit.util.AppSettings;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProductAiService {

    private static final String OPENAI_CONFIG_PATH = "/config/openai.properties";
    private static final Path LOCAL_OPENAI_CONFIG_PATH = Paths.get("config", "openai.local.properties");
    private static final Pattern CONTENT_PATTERN = Pattern.compile("\"content\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern B64_PATTERN = Pattern.compile("\"b64_json\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern DATA_URL_PATTERN = Pattern.compile("data:image/([a-zA-Z0-9.+-]+);base64,([A-Za-z0-9+/=\\r\\n]+)");
    private static final Pattern IMAGE_URL_PATTERN = Pattern.compile("\"(?:url|imageUrl|image_url)\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern GENERIC_IMAGE_URL_PATTERN = Pattern.compile("https?://[^\\s\\\"')<>\\\\]+");
    private static final Pattern CONTENT_TYPE_EXTENSION_PATTERN = Pattern.compile("image/([a-zA-Z0-9.+-]+)");
    private static final DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Properties properties;
    private final HttpClient httpClient;

    public ProductAiService() {
        this.properties = loadProperties();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String generateDescriptionFromName(String productName, String categoryName) throws IOException, InterruptedException {
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire pour generer une description.");
        }

        String category = (categoryName == null || categoryName.isBlank()) ? "non specifiee" : categoryName.trim();
        String prompt = """
                Redige une description e-commerce en francais pour ce produit.
                Produit: %s
                Categorie: %s
                Contraintes:
                - 2 a 3 phrases
                - Ton commercial clair
                - Mettre en avant les benefices client
                - Pas d'emojis
                - Retourne uniquement la description finale
                """.formatted(productName.trim(), category);

        String apiKey = resolveProperty("openai.api.key", "OPENAI_API_KEY", "");
        if (apiKey == null || apiKey.isBlank()) {
            try {
                return generateDescriptionWithOllama(prompt);
            } catch (IOException exception) {
                if (isLocalAiUnavailable(exception)) {
                    return generateLocalProductDescription(productName, category);
                }
                throw exception;
            }
        }

        String endpoint = resolveProperty("openai.text.endpoint", "OPENAI_TEXT_ENDPOINT", "https://api.openai.com/v1/chat/completions");
        String model = resolveProperty("openai.text.model", "OPENAI_TEXT_MODEL", "gpt-4o-mini");

        String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "Tu es un assistant e-commerce expert en descriptions produits."},
                    {"role": "user", "content": "%s"}
                  ],
                  "temperature": 0.7
                }
                """.formatted(escapeJson(model), escapeJson(prompt));

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
            throw new IOException("Generation description echouee (HTTP " + response.statusCode() + "): " + compactBody(response.body()));
        }

        Matcher contentMatcher = CONTENT_PATTERN.matcher(response.body());
        while (contentMatcher.find()) {
            String content = unescapeJson(contentMatcher.group(1)).trim();
            if (!content.isBlank() && !content.startsWith("{")) {
                return content;
            }
        }

        throw new IOException("Reponse IA invalide: description absente.");
    }

    private String generateDescriptionWithOllama(String prompt) throws IOException, InterruptedException {
        String baseUrl = readSetting("OLLAMA_BASE_URL", "http://localhost:11434/api");
        String model = readSetting("OLLAMA_TEXT_MODEL", "gemma3");
        String body = """
                {
                  "model": "%s",
                  "prompt": "%s",
                  "stream": false,
                  "keep_alive": "15m",
                  "think": false,
                  "options": {
                    "num_predict": 180
                  }
                }
                """.formatted(escapeJson(model), escapeJson(prompt));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(trimTrailingSlash(baseUrl) + "/generate"))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String error = extractJsonField(response.body(), "error");
            if (error.toLowerCase().contains("not found")) {
                throw new IOException("Modele Ollama introuvable. Lance: ollama pull " + model);
            }
            throw new IOException("Generation Ollama echouee (HTTP " + response.statusCode() + "): " + compactBody(response.body()));
        }

        String generated = extractJsonField(response.body(), "response").trim();
        if (generated.isBlank()) {
            throw new IOException("Ollama a retourne une reponse vide.");
        }
        return generated;
    }

    private String generateLocalProductDescription(String productName, String categoryName) {
        String name = productName == null ? "Ce produit" : productName.trim();
        String category = categoryName == null ? "" : categoryName.trim();
        String categoryPart = category.isBlank() || category.equalsIgnoreCase("non specifiee")
                ? "notre selection e-commerce"
                : "la categorie " + category;

        return "%s est un choix pratique et soigneusement selectionne pour %s. "
                .formatted(name, categoryPart)
                + "Sa presentation claire aide vos clients a comprendre rapidement son utilite, "
                + "ses avantages et la valeur qu'il apporte au quotidien.";
    }

    private boolean isLocalAiUnavailable(IOException exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof ConnectException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String normalized = message.toLowerCase();
                if (normalized.contains("connection refused")
                        || normalized.contains("connexion refusee")
                        || normalized.contains("connect timed out")
                        || normalized.contains("ollama")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    public String generateImageFromName(String productName, String categoryName) throws IOException, InterruptedException {
        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException("Le nom du produit est obligatoire pour generer une image.");
        }

        String apiKey = resolveProperty("openai.api.key", "OPENAI_API_KEY", "");
        String category = (categoryName == null || categoryName.isBlank()) ? "" : categoryName.trim();

        String prompt = "Photo e-commerce professionnelle du produit " + productName.trim()
                + (category.isBlank() ? "" : ", categorie " + category)
                + ", fond studio propre, lumiere nette, haute qualite.";

        if (apiKey == null || apiKey.isBlank()) {
            return generateImageFromPublicFallback(prompt);
        }

        String endpoint = resolveProperty("openai.image.endpoint", "OPENAI_IMAGE_ENDPOINT", "https://api.openai.com/v1/images/generations");
        String requestEndpoint = normalizeImageEndpoint(endpoint);
        String imageModel = resolveProperty("openai.image.model", "OPENAI_IMAGE_MODEL", "gpt-image-1");
        String chatModel = resolveProperty("openai.image.chat.model", "OPENAI_IMAGE_CHAT_MODEL",
                resolveProperty("openai.text.model", "OPENAI_TEXT_MODEL", "gpt-4o-mini"));
        String size = resolveProperty("openai.image.size", "OPENAI_IMAGE_SIZE", "1024x1024");

        String body = buildImageRequestBody(requestEndpoint, chatModel, imageModel, prompt, size);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(requestEndpoint))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", resolveProperty("openai.http.referer", "OPENAI_HTTP_REFERER", "http://localhost"))
                .header("X-Title", resolveProperty("openai.http.title", "OPENAI_HTTP_TITLE", "java-ecom"))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            if (isOpenRouterEndpoint(requestEndpoint) && response.statusCode() == 404) {
                return generateImageFromPublicFallback(prompt);
            }
            throw new IOException("Generation image echouee (HTTP " + response.statusCode() + "): " + compactBody(response.body()));
        }

        String imagePath = saveImageFromResponse(response.body());
        if (imagePath != null) {
            return imagePath;
        }

        if (isOpenRouterEndpoint(requestEndpoint)) {
            return generateImageFromPublicFallback(prompt);
        }

        throw new IOException("Reponse IA invalide: image absente.");
    }

    private String generateImageFromPublicFallback(String prompt) throws IOException, InterruptedException {
        String fallbackBase = resolveProperty(
                "openai.image.fallback.endpoint",
                "OPENAI_IMAGE_FALLBACK_ENDPOINT",
                "https://image.pollinations.ai/prompt/"
        );

        String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8);
        String imageUrl = fallbackBase + encodedPrompt + "?width=1024&height=1024&nologo=true";

        HttpRequest imageRequest = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<byte[]> imageResponse = httpClient.send(imageRequest, HttpResponse.BodyHandlers.ofByteArray());
        if (imageResponse.statusCode() >= 400 || imageResponse.body() == null || imageResponse.body().length == 0) {
            throw new IOException("Generation image fallback echouee (HTTP " + imageResponse.statusCode() + ").");
        }

        Path outputPath = buildOutputPath("jpg");
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, imageResponse.body());
        return outputPath.toAbsolutePath().toString();
    }

    private String normalizeImageEndpoint(String endpoint) {
        if (!isOpenRouterEndpoint(endpoint)) {
            return endpoint;
        }

        int apiVersionIndex = endpoint.indexOf("/api/v1/");
        if (apiVersionIndex < 0) {
            return endpoint;
        }
        return endpoint.substring(0, apiVersionIndex) + "/api/v1/chat/completions";
    }

    private String buildImageRequestBody(String endpoint, String chatModel, String imageModel, String prompt, String size) {
        if (isOpenRouterEndpoint(endpoint)) {
            return """
                    {
                      "model": "%s",
                      "messages": [
                        {"role": "user", "content": "%s"}
                      ],
                      "tools": [
                        {
                          "type": "openrouter:image_generation",
                          "parameters": {
                            "model": "%s",
                            "size": "%s"
                          }
                        }
                      ],
                      "stream": false
                    }
                    """.formatted(escapeJson(chatModel), escapeJson(prompt), escapeJson(imageModel), escapeJson(size));
        }

        return """
                {
                  "model": "%s",
                  "prompt": "%s",
                  "size": "%s"
                }
                """.formatted(escapeJson(imageModel), escapeJson(prompt), escapeJson(size));
    }

    private String saveImageFromResponse(String responseBody) throws IOException, InterruptedException {
        String normalizedBody = responseBody == null ? "" : responseBody.replace("\\/", "/");

        Matcher b64Matcher = B64_PATTERN.matcher(normalizedBody);
        if (b64Matcher.find()) {
            return saveBase64Image(b64Matcher.group(1), "png");
        }

        Matcher dataUrlMatcher = DATA_URL_PATTERN.matcher(normalizedBody);
        if (dataUrlMatcher.find()) {
            return saveBase64Image(dataUrlMatcher.group(2), dataUrlMatcher.group(1));
        }

        Matcher imageUrlMatcher = IMAGE_URL_PATTERN.matcher(normalizedBody);
        if (imageUrlMatcher.find()) {
            String imageUrl = unescapeJson(imageUrlMatcher.group(1));
            if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                return downloadImage(imageUrl);
            }
        }

        Matcher genericUrlMatcher = GENERIC_IMAGE_URL_PATTERN.matcher(normalizedBody);
        if (genericUrlMatcher.find()) {
            return downloadImage(genericUrlMatcher.group());
        }

        return null;
    }

    private String saveBase64Image(String base64Image, String extension) throws IOException {
        byte[] imageBytes = Base64.getMimeDecoder().decode(unescapeJson(base64Image).replaceAll("\\s+", ""));
        Path outputPath = buildOutputPath(extension);
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, imageBytes);
        return outputPath.toAbsolutePath().toString();
    }

    private String downloadImage(String imageUrl) throws IOException, InterruptedException {
        HttpRequest imageRequest = HttpRequest.newBuilder()
                .uri(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<byte[]> imageResponse = httpClient.send(imageRequest, HttpResponse.BodyHandlers.ofByteArray());
        if (imageResponse.statusCode() >= 400 || imageResponse.body() == null || imageResponse.body().length == 0) {
            throw new IOException("Telechargement image echoue (HTTP " + imageResponse.statusCode() + ").");
        }

        String extension = imageResponse.headers()
                .firstValue("Content-Type")
                .map(this::extensionFromContentType)
                .orElse("png");

        Path outputPath = buildOutputPath(extension);
        Files.createDirectories(outputPath.getParent());
        Files.write(outputPath, imageResponse.body());
        return outputPath.toAbsolutePath().toString();
    }

    private Path buildOutputPath(String extension) {
        String outputDir = resolveProperty("openai.image.output.dir", "OPENAI_IMAGE_OUTPUT_DIR", "data/ai-images");
        String safeExtension = normalizeImageExtension(extension);
        String fileName = "produit-ai-" + FILE_FORMATTER.format(LocalDateTime.now()) + "." + safeExtension;
        return Paths.get(outputDir).resolve(fileName);
    }

    private String extensionFromContentType(String contentType) {
        Matcher matcher = CONTENT_TYPE_EXTENSION_PATTERN.matcher(contentType);
        return matcher.find() ? matcher.group(1) : "png";
    }

    private String normalizeImageExtension(String extension) {
        if (extension == null || extension.isBlank()) {
            return "png";
        }
        String normalized = extension.toLowerCase().replace("jpeg", "jpg");
        if (normalized.equals("png") || normalized.equals("jpg") || normalized.equals("webp") || normalized.equals("gif")) {
            return normalized;
        }
        return "png";
    }

    private boolean isOpenRouterEndpoint(String endpoint) {
        return endpoint != null && endpoint.contains("openrouter.ai");
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

        try (InputStream inputStream = ProductAiService.class.getResourceAsStream(OPENAI_CONFIG_PATH)) {
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

    private String readSetting(String key, String fallback) {
        String value = AppSettings.get(key, "");
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trimTrailingSlash(String value) {
        String safeValue = value == null || value.isBlank() ? "http://localhost:11434/api" : value.trim();
        return safeValue.endsWith("/") ? safeValue.substring(0, safeValue.length() - 1) : safeValue;
    }

    private String extractJsonField(String json, String fieldName) {
        if (json == null || json.isBlank()) {
            return "";
        }

        String marker = "\"" + fieldName + "\"";
        int fieldIndex = json.indexOf(marker);
        if (fieldIndex < 0) {
            return "";
        }

        int colonIndex = json.indexOf(':', fieldIndex + marker.length());
        if (colonIndex < 0) {
            return "";
        }

        int quoteIndex = json.indexOf('"', colonIndex + 1);
        if (quoteIndex < 0) {
            return "";
        }

        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int index = quoteIndex + 1; index < json.length(); index++) {
            char current = json.charAt(index);
            if (escaped) {
                switch (current) {
                    case 'n' -> value.append('\n');
                    case 'r' -> value.append('\r');
                    case 't' -> value.append('\t');
                    case '"', '\\', '/' -> value.append(current);
                    default -> value.append(current);
                }
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '"') {
                return value.toString();
            } else {
                value.append(current);
            }
        }
        return "";
    }
}
