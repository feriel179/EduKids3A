package tn.esprit.services;

import tn.esprit.util.AppSettings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalAiContentService {
    private static final Pattern TAGGED_SECTION_PATTERN = Pattern.compile("(?is)\\[(TITLE|SUBJECT|DESCRIPTION)]\\s*(.*?)\\s*\\[/\\1]");
    private static final String DEFAULT_BASE_URL = "http://localhost:11434/api";
    private static final String DEFAULT_TEXT_MODEL = "gemma3";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String generateCourseDescription(String title, String subject, int level, String status, String currentDescription) {
        String prompt = """
                You are an educational content assistant for the EduKids platform.
                Write a concise course description.

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
                - Return only the final description.
                """.formatted(
                safeText(title, "Untitled course"),
                safeText(subject, "General education"),
                Math.max(1, level),
                safeText(status, "DRAFT"),
                safeText(currentDescription, "No additional notes.")
        );

        return callTextModel(prompt);
    }

    public String suggestObjectives(String title, String subject, int level, String description) {
        String prompt = """
                You are an educational designer for the EduKids platform.
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
                - Return only the objectives.
                """.formatted(
                safeText(title, "Untitled course"),
                safeText(subject, "General education"),
                Math.max(1, level),
                safeText(description, "No description provided.")
        );

        return callTextModel(prompt);
    }

    public TranslatedCourseContent translateCourse(String title, String subject, String description, String targetLanguage) {
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

        return parseTranslatedContent(callTextModel(prompt));
    }

    public String getBaseUrl() {
        return readSettingOrDefault("OLLAMA_BASE_URL", DEFAULT_BASE_URL);
    }

    public String getTextModel() {
        return readSettingOrDefault("OLLAMA_TEXT_MODEL", DEFAULT_TEXT_MODEL);
    }

    private String callTextModel(String prompt) {
        String requestBody = """
                {
                  "model": "%s",
                  "prompt": "%s",
                  "stream": false
                }
                """.formatted(
                escapeJson(getTextModel()),
                escapeJson(prompt)
        );

        String responseBody = postJson("/generate", requestBody);
        String responseText = extractFirstJsonString(responseBody, "response");
        if (responseText.isBlank()) {
            throw new IllegalStateException("Local AI returned an empty response.");
        }
        return responseText.trim();
    }

    private String postJson(String path, String requestBody) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(trimTrailingSlash(getBaseUrl()) + path))
                .timeout(Duration.ofSeconds(90))
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
            throw new IllegalStateException("The local AI request was interrupted.");
        } catch (IOException exception) {
            throw new IllegalStateException("Local AI unavailable. Install and start Ollama, then check " + getBaseUrl() + ".", exception);
        }
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
        String message = extractFirstJsonString(responseBody, "error");
        if (message.isBlank()) {
            message = extractFirstJsonString(responseBody, "message");
        }
        if (message.isBlank()) {
            message = "Local AI request failed.";
        }
        if (message.toLowerCase(Locale.ROOT).contains("not found")) {
            return "Ollama model not found. Run `ollama pull " + getTextModel() + "` and retry.";
        }
        return "Local AI request failed (" + statusCode + "): " + message;
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

        throw new IllegalStateException("Unable to parse the local AI response.");
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

    private String readSettingOrDefault(String key, String fallback) {
        String value = AppSettings.get(key, "");
        return value.isBlank() ? fallback : value;
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trimTrailingSlash(String value) {
        return value != null && value.endsWith("/") ? value.substring(0, value.length() - 1) : safeText(value, DEFAULT_BASE_URL);
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
