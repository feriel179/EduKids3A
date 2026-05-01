package tn.esprit.services;

import tn.esprit.util.AppSettings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalAiContentService {
    private static final Pattern TAGGED_SECTION_PATTERN = Pattern.compile("(?is)\\[(TITLE|SUBJECT|DESCRIPTION)]\\s*(.*?)\\s*\\[/\\1]");
    private static final String DEFAULT_BASE_URL = "http://localhost:11434/api";
    private static final String DEFAULT_TEXT_MODEL = "gemma3";
    private static final String DEFAULT_KEEP_ALIVE = "15m";
    private static final int TUTOR_MAX_TOKENS = 180;
    private static final int REVIEW_MAX_TOKENS = 140;
    private static final int TEXT_TIMEOUT_SECONDS = 90;
    private static final int VISION_TIMEOUT_SECONDS = 180;

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

        return callTextModel(prompt, TUTOR_MAX_TOKENS);
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

        return parseTranslatedContent(callTextModel(prompt), title, subject);
    }

    public GeneratedExerciseQuestions generateExerciseQuestions(String courseTitle, String subject, int level,
                                                                String courseDescription, String lessonTitle,
                                                                int age, boolean drawingEnabled) {
        String prompt = """
                You are an educational question generator for the EduKids platform.
                Create exactly 2 exercise questions related to the course and the lesson.

                Course title: %s
                Subject: %s
                Level: Niveau %d
                Course description: %s
                Lesson title: %s
                Student age: %d
                Drawing available: %s

                Requirements:
                - Use the same language as the course content when it is clear. Otherwise use French.
                - The questions must be directly about the course and the lesson, not generic.
                - Question 1 should test comprehension of the lesson.
                - Question 2 should ask the student to apply, explain, compare, or create using the lesson content.
                - If drawing is available, question 2 must explicitly ask the student to use the drawing area and then explain the drawing in writing.
                - If drawing is not available, question 2 must be writing-only.
                - Keep each question clear and classroom-ready.

                Return exactly in this format:
                [Q1_TITLE]
                title of question 1
                [/Q1_TITLE]
                [Q1_PROMPT]
                prompt of question 1
                [/Q1_PROMPT]
                [Q2_TITLE]
                title of question 2
                [/Q2_TITLE]
                [Q2_PROMPT]
                prompt of question 2
                [/Q2_PROMPT]
                """.formatted(
                safeText(courseTitle, "Untitled course"),
                safeText(subject, "General education"),
                Math.max(1, level),
                safeText(courseDescription, "No description provided."),
                safeText(lessonTitle, "Untitled lesson"),
                Math.max(8, age),
                drawingEnabled ? "yes" : "no"
        );

        return parseGeneratedExerciseQuestions(callTextModel(prompt));
    }

    public String answerLessonQuestion(String courseTitle, String subject, int level, String courseDescription,
                                       String lessonTitle, String lessonResources, int age, String question) {
        String prompt = """
                You are a patient AI tutor for the EduKids platform.
                Answer the student's question about the course lesson.

                Course title: %s
                Subject: %s
                Level: Niveau %d
                Course description: %s
                Lesson title: %s
                Lesson resources: %s
                Student age: %d
                Student question: %s

                Requirements:
                - Use the same language as the student's question when it is clear. Otherwise use French.
                - Be warm, simple, and age-appropriate.
                - Explain the lesson more clearly before giving extra details.
                - If the lesson resources are limited, say so honestly and stay close to the provided context.
                - No markdown, no bullet list, no heading.
                - Keep the answer between 90 and 170 words.
                - End with one short encouraging sentence.
                - Return only the final answer.
                """.formatted(
                safeText(courseTitle, "Untitled course"),
                safeText(subject, "General education"),
                Math.max(1, level),
                safeText(courseDescription, "No description provided."),
                safeText(lessonTitle, "Untitled lesson"),
                safeText(lessonResources, "No extra lesson resources provided."),
                Math.max(8, age),
                safeText(question, "Explain this lesson more simply.")
        );

        return callTextModel(prompt);
    }

    public ExerciseReview reviewExerciseAnswer(String courseTitle, String subject, int level, String courseDescription,
                                               String lessonTitle, String exerciseTitle, String exercisePrompt,
                                               String answer, String drawingImagePath, boolean drawingProvided, int age) {
        boolean hasWrittenAnswer = answer != null && !answer.isBlank();
        boolean hasReviewImage = drawingImagePath != null
                && !drawingImagePath.isBlank()
                && Files.exists(Path.of(drawingImagePath));

        if (!hasWrittenAnswer && drawingProvided && !hasReviewImage) {
            return new ExerciseReview(
                    "Good start",
                    "You already used the drawing area, which shows that you started the exercise seriously.",
                    "Beginning with a drawing is a good idea because it helps organize what you understood from the lesson.",
                    "There is not enough written explanation yet for a precise correction. In this version, the AI review mainly relies on the written answer.",
                    "Write two or three short sentences explaining what your drawing shows, then run AI Review again."
            );
        }

        String prompt = buildExerciseReviewPrompt(
                courseTitle,
                subject,
                level,
                courseDescription,
                lessonTitle,
                exerciseTitle,
                exercisePrompt,
                answer,
                drawingProvided,
                hasReviewImage,
                age
        );

        if (hasReviewImage) {
            try {
                return parseExerciseReview(callVisionModel(prompt, Path.of(drawingImagePath)));
            } catch (IllegalStateException exception) {
                if (!hasWrittenAnswer) {
                    return new ExerciseReview(
                            "Drawing captured",
                            "Your drawing was attached, but the local AI could not analyze the image in this review.",
                            "You still made a real attempt, and the drawing shows that you engaged with the exercise.",
                            "The local model may not support image input yet, so the review cannot judge the drawing accurately this time.",
                            "Add two or three short sentences explaining your drawing, or switch Ollama to a vision-capable model and try AI Review again."
                    );
                }

                String textOnlyPrompt = buildExerciseReviewPrompt(
                        courseTitle,
                        subject,
                        level,
                        courseDescription,
                        lessonTitle,
                        exerciseTitle,
                        exercisePrompt,
                        answer,
                        drawingProvided,
                        false,
                        age
                ) + "\n\nImportant note: image analysis was unavailable for this request, so base the feedback mainly on the written answer.";
                return parseExerciseReview(callTextModel(textOnlyPrompt, REVIEW_MAX_TOKENS));
            }
        }

        return parseExerciseReview(callTextModel(prompt, REVIEW_MAX_TOKENS));
    }

    public String getBaseUrl() {
        return readSettingOrDefault("OLLAMA_BASE_URL", DEFAULT_BASE_URL);
    }

    public String getTextModel() {
        return readSettingOrDefault("OLLAMA_TEXT_MODEL", DEFAULT_TEXT_MODEL);
    }

    public String getVisionModel() {
        return readSettingOrDefault("OLLAMA_VISION_MODEL", getTextModel());
    }

    private String callTextModel(String prompt) {
        return callTextModel(prompt, 0);
    }

    private String callTextModel(String prompt, int maxTokens) {
        return callModel(getTextModel(), prompt, List.of(), maxTokens);
    }

    private String callVisionModel(String prompt, Path imagePath) {
        return callVisionModel(prompt, imagePath, REVIEW_MAX_TOKENS);
    }

    private String callVisionModel(String prompt, Path imagePath, int maxTokens) {
        return callModel(getVisionModel(), prompt, List.of(encodeImage(imagePath)), maxTokens);
    }

    private String callModel(String model, String prompt, List<String> images, int maxTokens) {
        String requestBody = buildGenerateRequestBody(model, prompt, images, maxTokens);
        int timeoutSeconds = images.isEmpty() ? TEXT_TIMEOUT_SECONDS : VISION_TIMEOUT_SECONDS;
        String responseBody = postJson("/generate", requestBody, model, timeoutSeconds);
        String responseText = extractFirstJsonString(responseBody, "response");
        if (responseText.isBlank()) {
            throw new IllegalStateException("Local AI returned an empty response.");
        }
        return responseText.trim();
    }

    private String buildGenerateRequestBody(String model, String prompt, List<String> images, int maxTokens) {
        StringBuilder requestBody = new StringBuilder();
        requestBody.append("{\n");
        requestBody.append("  \"model\": \"").append(escapeJson(model)).append("\",\n");
        requestBody.append("  \"prompt\": \"").append(escapeJson(prompt)).append("\",\n");
        requestBody.append("  \"keep_alive\": \"").append(DEFAULT_KEEP_ALIVE).append("\",\n");
        requestBody.append("  \"think\": false");
        if (!images.isEmpty()) {
            StringJoiner joiner = new StringJoiner(", ");
            for (String image : images) {
                joiner.add("\"" + escapeJson(image) + "\"");
            }
            requestBody.append(",\n  \"images\": [").append(joiner).append("]");
        }
        if (maxTokens > 0) {
            requestBody.append(",\n  \"options\": {\n");
            requestBody.append("    \"num_predict\": ").append(maxTokens).append("\n");
            requestBody.append("  }");
        }
        requestBody.append(",\n  \"stream\": false\n");
        requestBody.append("}");
        return requestBody.toString();
    }

    private String postJson(String path, String requestBody, String model, int timeoutSeconds) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(trimTrailingSlash(getBaseUrl()) + path))
                .timeout(Duration.ofSeconds(Math.max(30, timeoutSeconds)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(buildErrorMessage(response.statusCode(), response.body(), model));
            }
            return response.body();
        } catch (HttpTimeoutException exception) {
            throw new IllegalStateException(buildTimeoutMessage(imagesSupportedByRequest(path, requestBody), timeoutSeconds), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The local AI request was interrupted.");
        } catch (IOException exception) {
            throw new IllegalStateException("Local AI unavailable. Install and start Ollama, then check " + getBaseUrl() + ".", exception);
        }
    }

    private boolean imagesSupportedByRequest(String path, String requestBody) {
        return "/generate".equals(path) && requestBody != null && requestBody.contains("\"images\"");
    }

    private String buildTimeoutMessage(boolean imageRequest, int timeoutSeconds) {
        if (imageRequest) {
            return "AI Review took too long to answer. Ollama is running, but the vision model did not finish within "
                    + timeoutSeconds + " seconds.";
        }
        return "The AI tutor took too long to answer. Ollama is running, but the model did not finish within "
                + timeoutSeconds + " seconds.";
    }

    private TranslatedCourseContent parseTranslatedContent(String rawOutput, String fallbackTitle, String fallbackSubject) {
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

        if (title.isBlank()) {
            title = extractLineAfterLabel(rawOutput, "title", "titre");
        }
        if (subject.isBlank()) {
            subject = extractLineAfterLabel(rawOutput, "subject", "matiere", "matière");
        }
        if (description.isBlank()) {
            description = extractDescriptionFallback(rawOutput);
        }

        return new TranslatedCourseContent(
                title.isBlank() ? safeText(fallbackTitle, "Untitled course") : title,
                subject.isBlank() ? safeText(fallbackSubject, "General education") : subject,
                description
        );
    }

    private String extractDescriptionFallback(String rawOutput) {
        String labeledDescription = extractLineAfterLabel(rawOutput, "description", "description du cours");
        if (!labeledDescription.isBlank()) {
            return labeledDescription;
        }

        String cleanedOutput = normalizeAiPlainText(rawOutput);
        cleanedOutput = cleanedOutput
                .replaceAll("(?im)^\\s*(title|titre|subject|matiere|matière)\\s*[:\\-]\\s*.*$", "")
                .replaceAll("(?im)^\\s*description\\s*[:\\-]\\s*", "")
                .trim();

        if (cleanedOutput.isBlank()) {
            throw new IllegalStateException("The translated content could not be parsed correctly.");
        }

        return cleanedOutput;
    }

    private String normalizeAiPlainText(String value) {
        return safeText(value, "")
                .replace("```", "")
                .replace("**", "")
                .replace("__", "")
                .replaceAll("(?is)\\[(TITLE|SUBJECT|DESCRIPTION)]", "")
                .replaceAll("(?is)\\[/(TITLE|SUBJECT|DESCRIPTION)]", "")
                .trim();
    }

    private GeneratedExerciseQuestions parseGeneratedExerciseQuestions(String rawOutput) {
        String q1Title = "";
        String q1Prompt = "";
        String q2Title = "";
        String q2Prompt = "";

        Matcher matcher = Pattern.compile("(?is)\\[(Q1_TITLE|Q1_PROMPT|Q2_TITLE|Q2_PROMPT)]\\s*(.*?)\\s*\\[/\\1]").matcher(rawOutput);
        while (matcher.find()) {
            String section = matcher.group(1).toUpperCase(Locale.ROOT);
            String value = matcher.group(2).trim();
            switch (section) {
                case "Q1_TITLE" -> q1Title = value;
                case "Q1_PROMPT" -> q1Prompt = value;
                case "Q2_TITLE" -> q2Title = value;
                case "Q2_PROMPT" -> q2Prompt = value;
                default -> {
                }
            }
        }

        if (q1Title.isBlank() || q1Prompt.isBlank() || q2Title.isBlank() || q2Prompt.isBlank()) {
            throw new IllegalStateException("The generated exercise questions could not be parsed correctly.");
        }

        return new GeneratedExerciseQuestions(q1Title, q1Prompt, q2Title, q2Prompt);
    }

    private ExerciseReview parseExerciseReview(String rawOutput) {
        String score = extractTaggedSection(rawOutput, "SCORE");
        String summary = extractTaggedSection(rawOutput, "SUMMARY");
        String strengths = extractTaggedSection(rawOutput, "STRENGTHS");
        String improvements = extractTaggedSection(rawOutput, "IMPROVEMENTS");
        String nextStep = extractTaggedSection(rawOutput, "NEXT_STEP");

        if (score.isBlank()) {
            score = extractLineAfterLabel(rawOutput, "score", "evaluation", "note");
        }
        if (summary.isBlank()) {
            summary = extractLineAfterLabel(rawOutput, "summary", "resume", "overall");
        }
        if (strengths.isBlank()) {
            strengths = extractLineAfterLabel(rawOutput, "strengths", "points forts", "positive points");
        }
        if (improvements.isBlank()) {
            improvements = extractLineAfterLabel(rawOutput, "improvements", "ameliorations", "to improve");
        }
        if (nextStep.isBlank()) {
            nextStep = extractLineAfterLabel(rawOutput, "next step", "prochaine etape", "next");
        }

        List<String> paragraphs = extractContentParagraphs(rawOutput);
        int paragraphIndex = 0;

        if (score.isBlank()) {
            if (!paragraphs.isEmpty() && paragraphs.get(0).length() <= 80) {
                score = paragraphs.get(0);
                paragraphIndex = 1;
            } else {
                score = "AI feedback ready";
            }
        }
        if (summary.isBlank()) {
            summary = paragraphIndex < paragraphs.size()
                    ? paragraphs.get(paragraphIndex++)
                    : "Your answer shows effort, and the review was generated from a less structured AI response.";
        }
        if (strengths.isBlank()) {
            strengths = paragraphIndex < paragraphs.size()
                    ? paragraphs.get(paragraphIndex++)
                    : "You started working on the exercise and gave the AI something to review.";
        }
        if (improvements.isBlank()) {
            improvements = paragraphIndex < paragraphs.size()
                    ? paragraphs.get(paragraphIndex++)
                    : "Add clearer details and connect your answer more directly to the lesson prompt.";
        }
        if (nextStep.isBlank()) {
            nextStep = paragraphIndex < paragraphs.size()
                    ? paragraphs.get(paragraphIndex)
                    : "Revise your answer in two or three clearer sentences, then ask for AI Review again.";
        }

        return new ExerciseReview(score, summary, strengths, improvements, nextStep);
    }

    private String buildExerciseReviewPrompt(String courseTitle, String subject, int level, String courseDescription,
                                             String lessonTitle, String exerciseTitle, String exercisePrompt,
                                             String answer, boolean drawingProvided, boolean drawingImageAttached, int age) {
        return """
                You are a supportive educational reviewer for the EduKids platform.
                Review the student's exercise answer and give constructive feedback.

                Course title: %s
                Subject: %s
                Level: Niveau %d
                Course description: %s
                Lesson title: %s
                Exercise title: %s
                Exercise prompt: %s
                Student answer: %s
                Drawing available in the exercise: %s
                Drawing image attached for this review: %s
                Student age: %d

                Requirements:
                - Use the same language as the student's answer when it is clear. Otherwise use French.
                - Be encouraging, precise, and age-appropriate.
                - If the answer is empty or too short, say so gently and guide the student.
                - If a drawing image is attached, analyze the visible drawing carefully and mention only what can reasonably be seen.
                - If the drawing is unclear or incomplete, say so gently instead of inventing details.
                - If there is a drawing but little text, still give useful feedback based on the visible attempt.
                - If both writing and drawing are available, combine both in the feedback.
                - Do not mention any grading rubric or percentages.
                - Do not use markdown.
                - Keep the whole answer concise and under 120 words when possible.
                - Keep each section to one or two short sentences maximum.
                - Start exactly with [SCORE] and keep the exact section tags unchanged.
                - Return exactly in this format:
                [SCORE]
                short score label such as "Strong answer", "Good start", or "Needs more detail"
                [/SCORE]
                [SUMMARY]
                one short paragraph explaining the overall quality of the answer
                [/SUMMARY]
                [STRENGTHS]
                one short paragraph about what is correct or promising
                [/STRENGTHS]
                [IMPROVEMENTS]
                one short paragraph about what should be improved
                [/IMPROVEMENTS]
                [NEXT_STEP]
                one short concrete next step the student can do now
                [/NEXT_STEP]
                """.formatted(
                safeText(courseTitle, "Untitled course"),
                safeText(subject, "General education"),
                Math.max(1, level),
                safeText(courseDescription, "No description provided."),
                safeText(lessonTitle, "Untitled lesson"),
                safeText(exerciseTitle, "Exercise"),
                safeText(exercisePrompt, "No exercise prompt provided."),
                safeText(answer, "No answer submitted."),
                drawingProvided ? "yes" : "no",
                drawingImageAttached ? "yes" : "no",
                Math.max(8, age)
        );
    }

    private String buildErrorMessage(int statusCode, String responseBody, String model) {
        String message = extractFirstJsonString(responseBody, "error");
        if (message.isBlank()) {
            message = extractFirstJsonString(responseBody, "message");
        }
        if (message.isBlank()) {
            message = "Local AI request failed.";
        }
        if (message.toLowerCase(Locale.ROOT).contains("not found")) {
            return "Ollama model not found. Run `ollama pull " + model + "` and retry.";
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

    private String encodeImage(Path imagePath) {
        try {
            return Base64.getEncoder().encodeToString(Files.readAllBytes(imagePath));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read the drawing image for the AI review.", exception);
        }
    }

    private String readSettingOrDefault(String key, String fallback) {
        String value = AppSettings.get(key, "");
        return value.isBlank() ? fallback : value;
    }

    private String extractTaggedSection(String rawOutput, String sectionName) {
        Matcher matcher = Pattern.compile("(?is)\\[" + Pattern.quote(sectionName) + "]\\s*(.*?)\\s*\\[/" + Pattern.quote(sectionName) + "]")
                .matcher(rawOutput);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private String extractLineAfterLabel(String rawOutput, String... labels) {
        for (String label : labels) {
            Matcher matcher = Pattern.compile("(?im)^\\s*" + Pattern.quote(label) + "\\s*[:\\-]\\s*(.+)$")
                    .matcher(normalizeReviewText(rawOutput));
            if (matcher.find()) {
                String value = matcher.group(1).trim();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        return "";
    }

    private List<String> extractContentParagraphs(String rawOutput) {
        String normalized = normalizeReviewText(rawOutput)
                .replaceAll("(?im)^\\s*(score|evaluation|note|summary|resume|overall|strengths|points forts|positive points|improvements|ameliorations|to improve|next step|prochaine etape|next)\\s*[:\\-]\\s*", "")
                .replaceAll("(?im)^\\s*[-*]\\s*", "")
                .trim();

        String[] blocks = normalized.split("(\\r?\\n){2,}");
        List<String> paragraphs = new ArrayList<>();
        for (String block : blocks) {
            String value = block.replaceAll("\\s+", " ").trim();
            if (!value.isBlank()) {
                paragraphs.add(value);
            }
        }
        if (paragraphs.isEmpty() && !normalized.isBlank()) {
            paragraphs.add(normalized.replaceAll("\\s+", " ").trim());
        }
        return paragraphs;
    }

    private String normalizeReviewText(String value) {
        return safeText(value, "")
                .replace("**", "")
                .replace("##", "")
                .replace("__", "")
                .replace("[SCORE]", "")
                .replace("[/SCORE]", "")
                .replace("[SUMMARY]", "")
                .replace("[/SUMMARY]", "")
                .replace("[STRENGTHS]", "")
                .replace("[/STRENGTHS]", "")
                .replace("[IMPROVEMENTS]", "")
                .replace("[/IMPROVEMENTS]", "")
                .replace("[NEXT_STEP]", "")
                .replace("[/NEXT_STEP]", "")
                .trim();
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

    public record GeneratedExerciseQuestions(String questionOneTitle, String questionOnePrompt,
                                             String questionTwoTitle, String questionTwoPrompt) {
    }

    public record ExerciseReview(String score, String summary, String strengths, String improvements, String nextStep) {
    }

    private record ParsedJsonString(String value, int nextIndex) {
    }
}
