package tn.esprit.services;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenAiContentService {
    private static final Pattern TAGGED_SECTION_PATTERN = Pattern.compile("(?is)\\[(TITLE|SUBJECT|DESCRIPTION)]\\s*(.*?)\\s*\\[/\\1]");
    private static final Pattern OUTPUT_TEXT_TYPE_PATTERN = Pattern.compile("\"type\"\\s*:\\s*\"output_text\"");
    private static final String DEFAULT_TEXT_MODEL = "gpt-5-mini";
    private static final String DEFAULT_IMAGE_MODEL = "gpt-image-1.5";
    private static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(20))
            .build();

    public boolean hasApiKey() {
        return !readSetting("OPENAI_API_KEY").isBlank() || !readSetting("openai.api.key").isBlank();
    }

    public String generateCourseDescription(String title, String subject, int level, String status, String currentDescription) {
        String instructions = """
                You are an educational content assistant for the EduKids platform.
                Write polished, parent-friendly course copy and follow formatting instructions exactly.
                """;

        String prompt = """
                Create a concise course description for EduKids.

                Course title: %s
                Subject: %s
                Level: Niveau %d
                Status: %s
                Current notes: %s

                Requirements:
                - Use the same language as the course content when it is clear. Otherwise use French.
                - Write 90 to 130 words.
                - Focus on audience, progression, benefits, and outcomes.
                - No markdown, no bullets, no heading.

                Return only the final description.
                """.formatted(
                safeText(title, "Untitled course"),
                safeText(subject, "General education"),
                Math.max(1, level),
                safeText(status, "DRAFT"),
                safeText(currentDescription, "No additional notes.")
        );

        return callTextModel(instructions, prompt, 260);
    }

    public String suggestObjectives(String title, String subject, int level, String description) {
        String instructions = """
                You are an educational designer for the EduKids platform.
                Generate short, measurable, classroom-friendly learning objectives.
                """;

        String prompt = """
                Suggest pedagogical objectives for this course.

                Course title: %s
                Subject: %s
                Level: Niveau %d
                Description: %s

                Requirements:
                - Use the same language as the course content when it is clear. Otherwise use French.
                - Return exactly 4 lines.
                - Each line must start with "- ".
                - Keep each objective concise and action-oriented.

                Return only the objectives.
                """.formatted(
                safeText(title, "Untitled course"),
                safeText(subject, "General education"),
                Math.max(1, level),
                safeText(description, "No description provided.")
        );

        return callTextModel(instructions, prompt, 220);
    }

    public TranslatedCourseContent translateCourse(String title, String subject, String description, String targetLanguage) {
        String instructions = """
                You translate educational course content naturally and accurately.
                Keep the tone suitable for a learning platform and preserve the meaning.
                """;

        String prompt = """
                Translate the following course fields to %s.

                Return exactly in this format:
                [TITLE]
                translated title
                [/TITLE]
                [SUBJECT]
                translated subject
                [/SUBJECT]
                [DESCRIPTION]
                translated description
                [/DESCRIPTION]

                Source values:
                Title: %s
                Subject: %s
                Description: %s
                """.formatted(
                safeText(targetLanguage, "French"),
                safeText(title, "Untitled course"),
                safeText(subject, "General education"),
                safeText(description, "No description provided.")
        );

        return parseTranslatedContent(callTextModel(instructions, prompt, 420));
    }

    public String generateCourseImage(String title, String subject, int level, String description) {
        String prompt = """
                Create a child-friendly educational cover illustration for an EduKids course.
                Course title: %s
                Subject: %s
                Level: Niveau %d
                Description: %s

                Visual requirements:
                - warm and modern illustration style
                - colorful, clean, and suitable for children
                - clear educational theme connected to the subject
                - no text, no letters, no numbers, no watermark, no logo
                - square composition
                """.formatted(
                safeText(title, "EduKids course"),
                safeText(subject, "General education"),
                Math.max(1, level),
                safeText(description, "A beginner-friendly learning course.")
        );

        String requestBody = """
                {
                  "model": "%s",
                  "prompt": "%s",
                  "size": "1024x1024",
                  "quality": "medium",
                  "background": "opaque",
                  "output_format": "png"
                }
                """.formatted(
                escapeJson(readSettingOrDefault("OPENAI_IMAGE_MODEL", DEFAULT_IMAGE_MODEL)),
                escapeJson(prompt)
        );

        String responseBody = postJson("/images/generations", requestBody);
        String base64Image = extractFirstJsonString(responseBody, "b64_json");
        if (!base64Image.isBlank()) {
            return saveBase64Image(base64Image, title, ".png");
        }

        String imageUrl = extractFirstJsonString(responseBody, "url");
        if (!imageUrl.isBlank()) {
            return downloadImage(imageUrl, title);
        }

        throw new IllegalStateException("OpenAI did not return an image payload.");
    }

    private String callTextModel(String instructions, String prompt, int maxOutputTokens) {
        String requestBody = """
                {
                  "model": "%s",
                  "instructions": "%s",
                  "input": "%s",
                  "max_output_tokens": %d
                }
                """.formatted(
                escapeJson(readSettingOrDefault("OPENAI_TEXT_MODEL", DEFAULT_TEXT_MODEL)),
                escapeJson(instructions),
                escapeJson(prompt),
                Math.max(64, maxOutputTokens)
        );

        String responseBody = postJson("/responses", requestBody);
        String outputText = extractOutputText(responseBody);
        if (outputText.isBlank()) {
            throw new IllegalStateException("OpenAI returned an empty text response.");
        }
        return outputText.trim();
    }

    private String postJson(String path, String requestBody) {
        String apiKey = readApiKey();
        String baseUrl = readSettingOrDefault("OPENAI_BASE_URL", DEFAULT_BASE_URL);

        HttpRequest request = HttpRequest.newBuilder(URI.create(trimTrailingSlash(baseUrl) + path))
                .timeout(Duration.ofSeconds(90))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(buildErrorMessage(response.statusCode(), response.body()));
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The OpenAI request was interrupted.");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to reach the OpenAI API. Check your internet connection and API key.", exception);
        }
    }

    private String downloadImage(String imageUrl, String title) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(imageUrl))
                .timeout(Duration.ofSeconds(90))
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Image download failed with status " + response.statusCode() + ".");
            }

            String contentType = response.headers().firstValue("Content-Type").orElse("image/png");
            String extension = contentType.contains("webp") ? ".webp" : contentType.contains("jpeg") ? ".jpg" : ".png";
            return writeImageBytes(response.body(), title, extension);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The image download was interrupted.");
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save the generated image locally.", exception);
        }
    }

    private String saveBase64Image(String base64Image, String title, String extension) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            return writeImageBytes(imageBytes, title, extension);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("OpenAI returned an invalid image payload.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to save the generated image locally.", exception);
        }
    }

    private String writeImageBytes(byte[] imageBytes, String title, String extension) throws IOException {
        Path directory = Path.of(System.getProperty("user.dir"), "generated", "course-images");
        Files.createDirectories(directory);

        String safeExtension = extension == null || extension.isBlank() ? ".png" : extension;
        Path imagePath = directory.resolve(slugify(title) + "-" + Instant.now().toEpochMilli() + safeExtension);
        Files.write(imagePath, imageBytes);
        return imagePath.toUri().toString();
    }

    private String extractOutputText(String responseBody) {
        List<String> outputs = new ArrayList<>();
        Matcher matcher = OUTPUT_TEXT_TYPE_PATTERN.matcher(responseBody);
        while (matcher.find()) {
            int textKeyIndex = responseBody.indexOf("\"text\"", matcher.end());
            if (textKeyIndex < 0) {
                break;
            }

            int colonIndex = responseBody.indexOf(':', textKeyIndex);
            if (colonIndex < 0) {
                break;
            }

            int openingQuoteIndex = responseBody.indexOf('"', colonIndex + 1);
            if (openingQuoteIndex < 0) {
                break;
            }

            ParsedJsonString parsedJsonString = readJsonString(responseBody, openingQuoteIndex);
            outputs.add(parsedJsonString.value().trim());
        }

        if (!outputs.isEmpty()) {
            return String.join("\n\n", outputs).trim();
        }

        return extractFirstJsonString(responseBody, "output_text");
    }

    private String extractFirstJsonString(String json, String fieldName) {
        String token = "\"" + fieldName + "\"";
        int fieldIndex = json.indexOf(token);
        if (fieldIndex < 0) {
            return "";
        }

        int colonIndex = json.indexOf(':', fieldIndex + token.length());
        if (colonIndex < 0) {
            return "";
        }

        int openingQuoteIndex = json.indexOf('"', colonIndex + 1);
        if (openingQuoteIndex < 0) {
            return "";
        }

        return readJsonString(json, openingQuoteIndex).value();
    }

    private ParsedJsonString readJsonString(String json, int openingQuoteIndex) {
        StringBuilder rawValue = new StringBuilder();
        boolean escaped = false;

        for (int index = openingQuoteIndex + 1; index < json.length(); index++) {
            char current = json.charAt(index);

            if (escaped) {
                rawValue.append('\\').append(current);
                escaped = false;
                continue;
            }

            if (current == '\\') {
                escaped = true;
                continue;
            }

            if (current == '"') {
                return new ParsedJsonString(unescapeJson(rawValue.toString()), index + 1);
            }

            rawValue.append(current);
        }

        throw new IllegalStateException("Unable to parse the JSON response from OpenAI.");
    }

    private String unescapeJson(String value) {
        StringBuilder output = new StringBuilder();

        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current != '\\' || index + 1 >= value.length()) {
                output.append(current);
                continue;
            }

            char escaped = value.charAt(++index);
            switch (escaped) {
                case '"', '\\', '/' -> output.append(escaped);
                case 'b' -> output.append('\b');
                case 'f' -> output.append('\f');
                case 'n' -> output.append('\n');
                case 'r' -> output.append('\r');
                case 't' -> output.append('\t');
                case 'u' -> {
                    if (index + 4 < value.length()) {
                        String hex = value.substring(index + 1, index + 5);
                        output.append((char) Integer.parseInt(hex, 16));
                        index += 4;
                    }
                }
                default -> output.append(escaped);
            }
        }

        return output.toString();
    }

    private TranslatedCourseContent parseTranslatedContent(String rawOutput) {
        String title = "";
        String subject = "";
        String description = "";

        Matcher matcher = TAGGED_SECTION_PATTERN.matcher(rawOutput);
        while (matcher.find()) {
            String section = matcher.group(1).toUpperCase(Locale.ROOT);
            String value = matcher.group(2).trim();
            switch (section) {
                case "TITLE" -> title = value;
                case "SUBJECT" -> subject = value;
                case "DESCRIPTION" -> description = value;
                default -> {
                }
            }
        }

        if (title.isBlank() || subject.isBlank() || description.isBlank()) {
            throw new IllegalStateException("The translated content could not be parsed correctly.");
        }

        return new TranslatedCourseContent(title, subject, description);
    }

    private String buildErrorMessage(int statusCode, String responseBody) {
        String message = extractFirstJsonString(responseBody, "message");
        if (message.isBlank()) {
            message = "OpenAI request failed.";
        }
        return "OpenAI request failed (" + statusCode + "): " + message;
    }

    private String readApiKey() {
        String apiKey = readSetting("OPENAI_API_KEY");
        if (apiKey.isBlank()) {
            apiKey = readSetting("openai.api.key");
        }
        if (apiKey.isBlank()) {
            throw new IllegalStateException("OpenAI API key not found. Set OPENAI_API_KEY before using AI actions.");
        }
        return apiKey;
    }

    private String readSettingOrDefault(String key, String fallback) {
        String value = readSetting(key);
        return value.isBlank() ? fallback : value;
    }

    private String readSetting(String key) {
        String value = System.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(key);
        }
        return value == null ? "" : value.trim();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : safeText(value, DEFAULT_BASE_URL);
    }

    private String slugify(String value) {
        String normalized = Normalizer.normalize(safeText(value, "course"), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return normalized.isBlank() ? "course" : normalized;
    }

    private String escapeJson(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (current < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) current));
                    } else {
                        escaped.append(current);
                    }
                }
            }
        }
        return escaped.toString();
    }

    public record TranslatedCourseContent(String title, String subject, String description) {
    }

    private record ParsedJsonString(String value, int nextIndex) {
    }
}
