package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.Question;
import com.edukids.edukids3a.model.Quiz;
import com.edukids.edukids3a.model.Reponse;
import com.edukids.edukids3a.model.TypeQuestion;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class OpenAiChatService {
    private static final String API_URL = "https://api.openai.com/v1/responses";
    private static final String DEFAULT_MODEL = "gpt-5-mini";
    private static final Path LOCAL_CONFIG_PATH = Path.of("data", "openai.properties");

    private final HttpClient httpClient;
    private final Gson gson;

    public OpenAiChatService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build(), new Gson());
    }

    OpenAiChatService(HttpClient httpClient, Gson gson) {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    public String resolveApiKey() {
        String envKey = normalizeApiKeyValue(System.getenv("OPENAI_API_KEY"));
        if (!envKey.isBlank()) {
            return envKey;
        }

        String systemPropertyKey = normalizeApiKeyValue(System.getProperty("openai.api.key"));
        if (!systemPropertyKey.isBlank()) {
            return systemPropertyKey;
        }

        if (!Files.exists(LOCAL_CONFIG_PATH)) {
            return "";
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(LOCAL_CONFIG_PATH)) {
            properties.load(reader);
            return normalizeApiKeyValue(properties.getProperty("openai.api.key", ""));
        } catch (IOException ignored) {
            return "";
        }
    }

    public boolean hasApiKeyConfigured() {
        return !resolveApiKey().isBlank();
    }

    public String askQuizAssistant(
            String apiKey,
            Quiz quiz,
            Question currentQuestion,
            int questionNumber,
            int totalQuestions,
            String studentAnswer,
            List<ChatTurn> history,
            String userMessage
    ) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("La cle API OpenAI est requise.");
        }
        return requestText(
                apiKey,
                buildInstructions(),
                buildConversationInput(quiz, currentQuestion, questionNumber, totalQuestions, studentAnswer, history, userMessage),
                420
        );
    }

    public String generateQuizDescription(
            String apiKey,
            String title,
            String level,
            String ageCategory,
            int questionCount,
            int durationMinutes,
            String status
    ) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            return buildLocalQuizDescription(title, level, ageCategory, questionCount, durationMinutes, status);
        }
        String instructions = "You write concise, attractive French quiz descriptions for an educational JavaFX app. "
                + "Return only the final description in French, between 2 and 4 sentences, no bullets, no title. "
                + "Mention the learning style implied by the age category when useful.";
        String input = "Quiz title: " + safeValue(title) + "\n"
                + "Level: " + safeValue(level) + "\n"
                + "Age category: " + safeValue(ageCategory) + "\n"
                + "Question count: " + questionCount + "\n"
                + "Duration: " + durationMinutes + " minutes\n"
                + "Status: " + safeValue(status) + "\n"
                + "Task: Generate a polished student-facing description.";
        try {
            return requestText(apiKey, instructions, input, 220).trim();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return buildLocalQuizDescription(title, level, ageCategory, questionCount, durationMinutes, status);
        } catch (IOException ex) {
            return buildLocalQuizDescription(title, level, ageCategory, questionCount, durationMinutes, status);
        }
    }

    public String improveQuestionText(
            String apiKey,
            String quizTitle,
            String level,
            String ageCategory,
            TypeQuestion typeQuestion,
            String currentQuestionText
    ) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            return buildLocalImprovedQuestionText(quizTitle, level, ageCategory, typeQuestion, currentQuestionText);
        }
        String instructions = "You improve French quiz questions for clarity and pedagogy. "
                + "Return only one improved question sentence in French. No bullets, no explanations. "
                + "Keep the wording adapted to the student's age and to the given question type.";
        String input = "Quiz title: " + safeValue(quizTitle) + "\n"
                + "Level: " + safeValue(level) + "\n"
                + "Age category: " + safeValue(ageCategory) + "\n"
                + "Question type: " + typeQuestion + "\n"
                + "Current question: " + safeValue(currentQuestionText) + "\n"
                + "Task: Rewrite the question so it is clearer and better phrased for students.";
        try {
            return requestText(apiKey, instructions, input, 140).trim();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return buildLocalImprovedQuestionText(quizTitle, level, ageCategory, typeQuestion, currentQuestionText);
        } catch (IOException ex) {
            return buildLocalImprovedQuestionText(quizTitle, level, ageCategory, typeQuestion, currentQuestionText);
        }
    }

    public QuestionDraft generateQuestionDraft(
            String apiKey,
            String quizTitle,
            String level,
            String ageCategory,
            TypeQuestion typeQuestion,
            String questionText
    ) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            return buildLocalQuestionDraft(quizTitle, level, ageCategory, typeQuestion, questionText, 0);
        }
        String instructions = "You generate French quiz answer drafts for an educational app. "
                + "Return only plain text with this exact format:\n"
                + "TYPE: exact enum value\n"
                + "QUESTION: ...\n"
                + "CHOICE1: ...\n"
                + "CHOICE2: ...\n"
                + "CHOICE3: ...\n"
                + "CHOICE4: ...\n"
                + "CORRECT: comma-separated numbers\n"
                + "FREE_TEXT: ...\n"
                + "When TYPE is RELIER_FLECHE, each CHOICEX must use the format gauche|||droite. "
                + "If the type expects one answer only, exactly one choice must be correct. "
                + "If the type allows several correct answers, one or more choices may be correct. "
                + "If the type is REPONSE_LIBRE, fill FREE_TEXT and leave choices generic if needed. "
                + "If the type is VRAI_FAUX, set CHOICE1 to Vrai and CHOICE2 to Faux. "
                + "Keep content coherent with the question and age category.";
        String input = "Quiz title: " + safeValue(quizTitle) + "\n"
                + "Level: " + safeValue(level) + "\n"
                + "Age category: " + safeValue(ageCategory) + "\n"
                + "Question type: " + typeQuestion + "\n"
                + "Question text: " + safeValue(questionText) + "\n"
                + "Task: Generate a complete answer draft.";
        try {
            String raw = requestText(apiKey, instructions, input, 340);
            QuestionDraft draft = parseQuestionDraft(raw, typeQuestion, questionText);
            return ensureUsableDraft(draft, quizTitle, level, ageCategory, typeQuestion, questionText, 0);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return buildLocalQuestionDraft(quizTitle, level, ageCategory, typeQuestion, questionText, 0);
        } catch (IOException ex) {
            return buildLocalQuestionDraft(quizTitle, level, ageCategory, typeQuestion, questionText, 0);
        }
    }

    public List<QuestionDraft> generateQuizPack(
            String apiKey,
            String quizTitle,
            String level,
            String ageCategory,
            String description,
            int durationMinutes,
            int questionCount
    ) throws IOException, InterruptedException {
        int expectedCount = Math.max(1, questionCount);
        if (apiKey == null || apiKey.isBlank()) {
            return buildLocalQuizPack(quizTitle, level, ageCategory, description, durationMinutes, expectedCount);
        }
        String instructions = "You generate a small complete French quiz pack for an educational app. "
                + "Return exactly the requested number of questions. "
                + "Use this exact format for each block and separate blocks with --- :\n"
                + "QUESTION: ...\n"
                + "TYPE: exact enum value such as QCU, QCM, REPONSE_LIBRE, VRAI_FAUX, RELIER_FLECHE, PETIT_JEU\n"
                + "CHOICE1: ...\n"
                + "CHOICE2: ...\n"
                + "CHOICE3: ...\n"
                + "CHOICE4: ...\n"
                + "CORRECT: comma-separated numbers\n"
                + "FREE_TEXT: ...\n"
                + "If TYPE is RELIER_FLECHE, each choice must use gauche|||droite. "
                + "If the age category is 8-10 ans, prefer VRAI_FAUX, RELIER_FLECHE, QCM and PETIT_JEU with very simple wording. "
                + "If the age category is 10 ans et plus, prefer QCU, QCM and REPONSE_LIBRE. "
                + "Do not add explanations outside the required format.";
        String input = "Quiz title: " + safeValue(quizTitle) + "\n"
                + "Level: " + safeValue(level) + "\n"
                + "Age category: " + safeValue(ageCategory) + "\n"
                + "Description: " + safeValue(description) + "\n"
                + "Duration: " + durationMinutes + " minutes\n"
                + "Question count: " + expectedCount + "\n"
                + "Task: Generate " + expectedCount + " coherent educational questions for this quiz.";
        try {
            String raw = requestText(apiKey, instructions, input, Math.max(900, expectedCount * 260));
            return completeQuizPack(parseQuizPack(raw), quizTitle, level, ageCategory, description, durationMinutes, expectedCount);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return buildLocalQuizPack(quizTitle, level, ageCategory, description, durationMinutes, expectedCount);
        } catch (IOException ex) {
            return buildLocalQuizPack(quizTitle, level, ageCategory, description, durationMinutes, expectedCount);
        }
    }

    public String buildFallbackAssistantReply(
            Quiz quiz,
            Question currentQuestion,
            int questionNumber,
            int totalQuestions,
            String studentAnswer,
            String userMessage
    ) {
        String normalizedMessage = normalize(userMessage);
        List<String> answerChoices = getAnswerChoices(currentQuestion);
        List<String> correctAnswers = getCorrectAnswers(currentQuestion);
        List<String> wrongAnswers = getWrongAnswers(currentQuestion);
        boolean asksForHint = containsAny(normalizedMessage, "indice", "hint", "aide", "sans me dire", "sans donner");
        boolean asksToSimplify = containsAny(normalizedMessage, "plus simple", "simplifie", "simplifier", "reformule", "resume");
        boolean asksForChoices = containsAny(normalizedMessage, "choix", "option", "proposition");
        boolean asksForWhy = containsAny(normalizedMessage, "pourquoi", "explique", "explication");
        boolean asksForMethod = containsAny(normalizedMessage, "comment", "methode", "strategie", "faire");
        boolean asksForWrongAnswers = containsAny(normalizedMessage, "fausse", "faux", "mauvaise", "incorrecte", "incorrect");
        boolean asksForDirectAnswer = containsAny(
                normalizedMessage,
                "bonne reponse",
                "bonnes reponses",
                "solution",
                "donne la reponse",
                "donne moi la reponse",
                "quelle est la reponse",
                "quelle est la bonne reponse",
                "corrige",
                "correction"
        );
        boolean questionAboutStudentAnswer = containsAny(normalizedMessage, "ma reponse", "mon choix", "j'ai choisi", "est ce correct", "est-ce correct", "c'est correct", "cest correct");

        StringBuilder builder = new StringBuilder();
        builder.append("Question ").append(questionNumber).append(" / ").append(totalQuestions).append(". ");

        if (containsAny(normalizedMessage, "bonjour", "salut", "hello", "bonsoir")) {
            builder.append("Bonjour. Je peux te donner un indice, t'expliquer la question ou verifier ta reponse. ");
        } else if (asksToSimplify) {
            builder.append(buildSimpleRephrase(currentQuestion)).append(' ');
            builder.append(buildHint(currentQuestion, quiz, correctAnswers));
        } else if (asksForHint) {
            builder.append(buildHint(currentQuestion, quiz, correctAnswers)).append(' ');
            builder.append(buildMethod(currentQuestion));
        } else if (questionAboutStudentAnswer) {
            appendStudentAnswerFeedback(builder, currentQuestion, studentAnswer, correctAnswers);
            builder.append("Explication: ").append(buildExplanation(currentQuestion, quiz)).append(". ");
            if (!isStudentAnswerCorrect(currentQuestion, studentAnswer, correctAnswers)) {
                builder.append(buildHint(currentQuestion, quiz, correctAnswers));
            }
        } else if (asksForChoices) {
            appendChoicesOverview(builder, currentQuestion, answerChoices);
        } else if (asksForWhy) {
            appendAnswer(builder, currentQuestion, correctAnswers);
            builder.append("Explication: ").append(buildExplanation(currentQuestion, quiz)).append(". ");
            if (!wrongAnswers.isEmpty()) {
                builder.append("Reponses a eviter: ").append(String.join(" | ", wrongAnswers)).append(". ");
            }
            builder.append(buildMethod(currentQuestion));
        } else if (asksForMethod) {
            builder.append(buildMethod(currentQuestion)).append(' ');
            builder.append(buildHint(currentQuestion, quiz, correctAnswers));
        } else if (asksForWrongAnswers) {
            if (!wrongAnswers.isEmpty()) {
                builder.append("Les reponses incorrectes sont: ").append(String.join(" | ", wrongAnswers)).append(". ");
            } else {
                builder.append("Je n'ai pas detecte de mauvaise proposition enregistree pour cette question. ");
            }
            appendAnswer(builder, currentQuestion, correctAnswers);
        } else if (asksForDirectAnswer) {
            appendAnswer(builder, currentQuestion, correctAnswers);
            builder.append("Explication: ").append(buildExplanation(currentQuestion, quiz)).append(". ");
        } else {
            builder.append(buildHint(currentQuestion, quiz, correctAnswers)).append(' ');
            builder.append(buildMethod(currentQuestion));
        }

        if (!answerChoices.isEmpty() && !asksForChoices && currentQuestion.getType() != TypeQuestion.RELIER_FLECHE) {
            builder.append("Choix proposes: ").append(String.join(" | ", answerChoices)).append(". ");
        }

        return builder.toString().trim();
    }

    public boolean shouldUseVerifiedResponseLegacy(String userMessage) {
        String normalizedMessage = normalize(userMessage);
        return normalizedMessage.isBlank()
                || containsAny(
                normalizedMessage,
                "reponse",
                "réponse",
                "solution",
                "bonne",
                "correct",
                "corrige",
                "correction",
                "pourquoi",
                "explique",
                "explication",
                "choix",
                "option",
                "proposition",
                "faux",
                "fausse",
                "mauvaise",
                "incorrect",
                "comment",
                "methode",
                "méthode",
                "ma reponse",
                "mon choix",
                "j'ai choisi",
                "est ce correct",
                "est-ce correct",
                "c'est correct",
                "cest correct"
        );
    }

    private String buildInstructionsLegacy() {
        return "You are EduKids Coach, a quiz chatbot embedded inside a JavaFX quiz app. "
                + "Your job is to answer the student's exact question about the current quiz question. "
                + "You may reveal the correct answer and explain it clearly. "
                + "Use the current question text, answer choices, and correct answer metadata as the source of truth. "
                + "If the student asks for the answer, give it directly. "
                + "If the student asks why, explain why. "
                + "If the student asks about the options, mention the options. "
                + "Keep responses concise, clear, and in French unless the user writes in another language.";
    }

    private String buildConversationInputLegacy(
            Quiz quiz,
            Question currentQuestion,
            int questionNumber,
            int totalQuestions,
            String studentAnswer,
            List<ChatTurn> history,
            String userMessage
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("Quiz context").append('\n');
        builder.append("Title: ").append(safeValue(quiz.getTitre())).append('\n');
        builder.append("Level: ").append(safeValue(quiz.getNiveau())).append('\n');
        builder.append("Description: ").append(safeValue(quiz.getDescription())).append('\n');
        builder.append("Current question: ").append(questionNumber).append(" / ").append(totalQuestions).append('\n');
        builder.append("Question text: ").append(safeValue(currentQuestion.getIntitule())).append('\n');
        builder.append("Question type: ").append(currentQuestion.getType()).append('\n');
        builder.append("Answer choices: ").append(formatList(getAnswerChoices(currentQuestion))).append('\n');
        builder.append("Correct answer metadata: ").append(formatList(getCorrectAnswers(currentQuestion))).append('\n');
        builder.append("Student current answer: ").append(safeValue(studentAnswer)).append('\n');
        builder.append('\n');
        builder.append("Recent conversation").append('\n');

        int start = Math.max(0, history.size() - 8);
        for (int index = start; index < history.size(); index++) {
            ChatTurn turn = history.get(index);
            builder.append(turn.role()).append(": ").append(turn.message()).append('\n');
        }

        builder.append("User: ").append(userMessage);
        return builder.toString();
    }

    public boolean shouldUseVerifiedResponse(String userMessage) {
        String normalizedMessage = normalize(userMessage);
        return normalizedMessage.isBlank()
                || containsAny(
                normalizedMessage,
                "solution",
                "bonne reponse",
                "bonnes reponses",
                "reponse exacte",
                "reponses exactes",
                "quelle est la reponse",
                "quelle est la bonne reponse",
                "donne la reponse",
                "donne moi la reponse",
                "correct",
                "corrige",
                "correction",
                "ma reponse",
                "mon choix",
                "j'ai choisi",
                "est ce correct",
                "est-ce correct",
                "c'est correct",
                "cest correct"
        );
    }

    private String buildInstructions() {
        return "You are EduKids Coach, a warm tutoring assistant inside a JavaFX quiz app for children. "
                + "Answer the student's latest message first, not the older history. "
                + "Use only the quiz and question metadata provided as the source of truth. Never invent answers, options, rules, or facts. "
                + "Adapt the tone to the age category. For 8-10 ans, use very simple French and short sentences. "
                + "If the student asks for a hint, give a clue without revealing the full answer unless they explicitly ask for it. "
                + "If the student asks for the exact answer, correction, or whether their current answer is correct, answer clearly and directly, then explain briefly. "
                + "If the question is a matching question, format each pair exactly as gauche -> droite. "
                + "Be encouraging, concrete, and concise. Usually answer in 2 to 5 short sentences. "
                + "Reply in French unless the student writes in another language.";
    }

    private String buildConversationInput(
            Quiz quiz,
            Question currentQuestion,
            int questionNumber,
            int totalQuestions,
            String studentAnswer,
            List<ChatTurn> history,
            String userMessage
    ) {
        List<String> answerChoices = getAnswerChoices(currentQuestion);
        List<String> correctAnswers = getCorrectAnswers(currentQuestion);
        List<String> wrongAnswers = getWrongAnswers(currentQuestion);
        StringBuilder builder = new StringBuilder();
        builder.append("Quiz context").append('\n');
        builder.append("Title: ").append(safeValue(quiz.getTitre())).append('\n');
        builder.append("Level: ").append(safeValue(quiz.getNiveau())).append('\n');
        builder.append("Age category: ").append(safeValue(quiz.getCategorieAge())).append('\n');
        builder.append("Audience hint: ").append(safeValue(quiz.getAudienceHint())).append('\n');
        builder.append("Description: ").append(safeValue(quiz.getDescription())).append('\n');
        builder.append("Current question: ").append(questionNumber).append(" / ").append(totalQuestions).append('\n');
        builder.append("Question text: ").append(safeValue(currentQuestion.getIntitule())).append('\n');
        builder.append("Question type: ").append(currentQuestion.getType() == null ? "-" : currentQuestion.getType().getLabel()).append('\n');
        builder.append("Answer choices: ").append(formatList(answerChoices)).append('\n');
        if (currentQuestion.getType() == TypeQuestion.RELIER_FLECHE) {
            builder.append("Matching left items: ").append(formatList(getMatchingLeftItems(currentQuestion))).append('\n');
            builder.append("Matching right items: ").append(formatList(getMatchingRightItems(currentQuestion))).append('\n');
        }
        builder.append("Correct answer metadata: ").append(formatList(correctAnswers)).append('\n');
        builder.append("Incorrect answer metadata: ").append(formatList(wrongAnswers)).append('\n');
        builder.append("Pedagogical hint: ").append(buildHint(currentQuestion, quiz, correctAnswers)).append('\n');
        builder.append("Suggested method: ").append(buildMethod(currentQuestion)).append('\n');
        builder.append("Simple restatement: ").append(buildSimpleRephrase(currentQuestion)).append('\n');
        builder.append("Student current answer: ").append(formatStudentAnswer(studentAnswer, currentQuestion)).append('\n');
        builder.append("Important: if older conversation conflicts with the current question, always trust the current question above.").append('\n');
        builder.append('\n');
        builder.append("Recent conversation").append('\n');

        int start = Math.max(0, history.size() - 6);
        for (int index = start; index < history.size(); index++) {
            ChatTurn turn = history.get(index);
            builder.append(turn.role()).append(": ").append(turn.message()).append('\n');
        }

        builder.append("Latest student message: ").append(safeValue(userMessage)).append('\n');
        builder.append("Task: answer the latest student message now.");
        return builder.toString();
    }

    private String extractAssistantText(String responseBody) throws IOException {
        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
        if (json == null) {
            throw new IOException("Reponse OpenAI invalide.");
        }

        if (json.has("output_text") && !json.get("output_text").isJsonNull()) {
            String outputText = json.get("output_text").getAsString().trim();
            if (!outputText.isBlank()) {
                return outputText;
            }
        }

        if (!json.has("output")) {
            throw new IOException("Aucune sortie texte retournee par OpenAI.");
        }

        JsonArray output = json.getAsJsonArray("output");
        StringBuilder textBuilder = new StringBuilder();
        for (JsonElement outputElement : output) {
            JsonObject item = outputElement.getAsJsonObject();
            if (!item.has("content")) {
                continue;
            }
            JsonArray content = item.getAsJsonArray("content");
            for (JsonElement contentElement : content) {
                JsonObject part = contentElement.getAsJsonObject();
                if (part.has("type") && "output_text".equals(part.get("type").getAsString()) && part.has("text")) {
                    if (!textBuilder.isEmpty()) {
                        textBuilder.append('\n');
                    }
                    textBuilder.append(part.get("text").getAsString().trim());
                }
            }
        }

        if (textBuilder.isEmpty()) {
            throw new IOException("Aucune reponse texte n'a ete retournee par OpenAI.");
        }

        return textBuilder.toString();
    }

    private String requestText(String apiKey, String instructions, String input, int maxOutputTokens) throws IOException, InterruptedException {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", DEFAULT_MODEL);
        requestBody.put("instructions", instructions);
        requestBody.put("input", input);
        requestBody.put("max_output_tokens", maxOutputTokens);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey.trim())
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new IOException(extractErrorMessage(response.body(), response.statusCode()));
        }
        return extractAssistantText(response.body());
    }

    private String extractErrorMessage(String responseBody, int statusCode) {
        try {
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (json != null && json.has("error")) {
                JsonObject error = json.getAsJsonObject("error");
                if (error.has("message")) {
                    return "OpenAI API error " + statusCode + ": " + error.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return "OpenAI API error " + statusCode + ".";
    }

    private List<String> getAnswerChoices(Question currentQuestion) {
        List<String> choices = new ArrayList<>();
        if (currentQuestion.getType() == TypeQuestion.RELIER_FLECHE) {
            for (String[] pair : getMatchingPairs(currentQuestion)) {
                if (!pair[0].isBlank() && !pair[1].isBlank()) {
                    choices.add(pair[0] + " -> " + pair[1]);
                }
            }
            return choices;
        }
        for (Reponse reponse : getSafeResponses(currentQuestion)) {
            if (reponse != null && reponse.getTexte() != null && !reponse.getTexte().isBlank()) {
                choices.add(reponse.getTexte().trim());
            }
        }
        return choices;
    }

    private List<String> getCorrectAnswers(Question currentQuestion) {
        List<String> answers = new ArrayList<>();
        if (currentQuestion.getType() == TypeQuestion.RELIER_FLECHE) {
            for (String[] pair : getMatchingPairs(currentQuestion)) {
                if (!pair[0].isBlank() && !pair[1].isBlank()) {
                    answers.add(pair[0] + " -> " + pair[1]);
                }
            }
            return answers;
        }
        for (Reponse reponse : getSafeResponses(currentQuestion)) {
            if (reponse != null && reponse.isCorrecte() && reponse.getTexte() != null && !reponse.getTexte().isBlank()) {
                answers.add(reponse.getTexte().trim());
            }
        }
        return answers;
    }

    private List<String> getWrongAnswers(Question currentQuestion) {
        List<String> answers = new ArrayList<>();
        if (currentQuestion.getType() == TypeQuestion.RELIER_FLECHE) {
            return answers;
        }
        for (Reponse reponse : getSafeResponses(currentQuestion)) {
            if (reponse != null && !reponse.isCorrecte() && reponse.getTexte() != null && !reponse.getTexte().isBlank()) {
                answers.add(reponse.getTexte().trim());
            }
        }
        return answers;
    }

    private List<Reponse> getSafeResponses(Question currentQuestion) {
        if (currentQuestion == null || currentQuestion.getReponses() == null) {
            return List.of();
        }
        return currentQuestion.getReponses();
    }

    private List<String[]> getMatchingPairs(Question currentQuestion) {
        List<String[]> pairs = new ArrayList<>();
        List<String> legacyValues = new ArrayList<>();
        for (Reponse reponse : getSafeResponses(currentQuestion)) {
            String text = reponse == null || reponse.getTexte() == null ? "" : reponse.getTexte().trim();
            if (text.isBlank()) {
                continue;
            }
            if (text.contains("|||")) {
                String[] parts = text.split("\\|\\|\\|", 2);
                if (parts.length == 2) {
                    String left = parts[0].trim();
                    String right = parts[1].trim();
                    if (!left.isBlank() && !right.isBlank()) {
                        pairs.add(new String[]{left, right});
                    }
                }
            } else {
                legacyValues.add(text);
            }
        }
        if (!pairs.isEmpty()) {
            return pairs;
        }
        for (int index = 0; index + 1 < legacyValues.size(); index += 2) {
            pairs.add(new String[]{legacyValues.get(index), legacyValues.get(index + 1)});
        }
        return pairs;
    }

    private List<String> getMatchingLeftItems(Question currentQuestion) {
        List<String> items = new ArrayList<>();
        for (String[] pair : getMatchingPairs(currentQuestion)) {
            items.add(pair[0]);
        }
        return items;
    }

    private List<String> getMatchingRightItems(Question currentQuestion) {
        List<String> items = new ArrayList<>();
        for (String[] pair : getMatchingPairs(currentQuestion)) {
            items.add(pair[1]);
        }
        return items;
    }

    private String buildExplanation(Question currentQuestion, Quiz quiz) {
        String explanation = "cette reponse correspond le mieux a l'enonce \"" + safeValue(currentQuestion.getIntitule()) + "\"";
        if (quiz.getTitre() != null && !quiz.getTitre().isBlank()) {
            explanation += " dans le quiz \"" + quiz.getTitre() + "\"";
        }
        if (currentQuestion.getType() == null) {
            return explanation;
        }
        return switch (currentQuestion.getType()) {
            case QCM -> explanation + ", et pour un QCM plusieurs elements peuvent etre corrects";
            case QCU -> explanation + ", et pour un QCU une seule proposition doit etre retenue";
            case REPONSE_LIBRE -> explanation + ", et la reponse attendue doit reformuler l'idee juste";
            case VRAI_FAUX -> explanation + ", et il faut decider si l'affirmation est vraie ou fausse";
            case RELIER_FLECHE -> explanation + ", et il faut retrouver les bonnes associations";
            case PETIT_JEU -> explanation + ", et il faut choisir la bonne reponse dans un format ludique";
        };
    }

    private String buildMethod(Question currentQuestion) {
        if (currentQuestion.getType() == null) {
            return "Relis calmement l'enonce avant de choisir.";
        }
        return switch (currentQuestion.getType()) {
            case QCM -> "Pour un QCM, plusieurs propositions peuvent etre justes.";
            case QCU -> "Pour un QCU, une seule proposition est la bonne.";
            case REPONSE_LIBRE -> "Pour une reponse libre, formule l'idee correcte avec des mots simples.";
            case VRAI_FAUX -> "Lis l'affirmation puis decide si elle est vraie ou fausse.";
            case RELIER_FLECHE -> "Repere les elements qui vont bien ensemble avant de valider.";
            case PETIT_JEU -> "Observe bien les propositions du mini-jeu avant de choisir.";
        };
    }

    private String buildHint(Question currentQuestion, Quiz quiz, List<String> correctAnswers) {
        String keywordFocus = buildKeywordFocus(currentQuestion == null ? "" : currentQuestion.getIntitule());
        if (currentQuestion == null || currentQuestion.getType() == null) {
            return "Indice: " + keywordFocus + "relis tranquillement la question et cherche ce qu'on te demande exactement.";
        }
        return switch (currentQuestion.getType()) {
            case QCM -> "Indice: " + keywordFocus
                    + (correctAnswers.size() > 1
                    ? "plusieurs propositions peuvent marcher, alors elimine d'abord celles qui contredisent l'enonce."
                    : "commence par eliminer les propositions qui ne collent pas du tout a la question.");
            case QCU -> "Indice: " + keywordFocus + "une seule proposition est vraiment correcte.";
            case REPONSE_LIBRE -> "Indice: " + keywordFocus + "ecris l'idee essentielle avec des mots simples et une phrase courte.";
            case VRAI_FAUX -> "Indice: " + keywordFocus + "demande-toi si l'affirmation est totalement vraie, pas seulement presque vraie.";
            case RELIER_FLECHE -> "Indice: commence par l'association la plus evidente, puis utilise les elements restants pour finir les autres paires.";
            case PETIT_JEU -> "Indice: " + keywordFocus + "observe bien les indices du mini-jeu avant de choisir.";
        };
    }

    private String buildSimpleRephrase(Question currentQuestion) {
        if (currentQuestion == null || currentQuestion.getType() == null) {
            return "En plus simple: trouve exactement ce que la question te demande avant de repondre.";
        }
        return switch (currentQuestion.getType()) {
            case QCM -> "En plus simple: choisis toutes les bonnes propositions.";
            case QCU -> "En plus simple: choisis une seule bonne proposition.";
            case REPONSE_LIBRE -> "En plus simple: ecris la bonne idee avec tes propres mots.";
            case VRAI_FAUX -> "En plus simple: decide si la phrase est vraie ou fausse.";
            case RELIER_FLECHE -> "En plus simple: relie chaque element de gauche avec celui qui lui correspond a droite.";
            case PETIT_JEU -> "En plus simple: regarde bien puis choisis la meilleure reponse.";
        };
    }

    private String buildKeywordFocus(String questionText) {
        List<String> keywords = extractKeywords(questionText, 3);
        if (keywords.isEmpty()) {
            return "";
        }
        return "regarde surtout les mots " + String.join(", ", keywords) + ", puis ";
    }

    private List<String> extractKeywords(String text, int maxCount) {
        List<String> keywords = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return keywords;
        }
        for (String part : text.split("[^\\p{L}\\p{Nd}]+")) {
            String trimmed = part.trim();
            String normalized = normalize(trimmed);
            if (trimmed.length() < 4 || normalized.isBlank() || isIgnoredKeyword(normalized)) {
                continue;
            }
            boolean alreadyPresent = false;
            for (String keyword : keywords) {
                if (normalize(keyword).equals(normalized)) {
                    alreadyPresent = true;
                    break;
                }
            }
            if (!alreadyPresent) {
                keywords.add(trimmed);
                if (keywords.size() >= maxCount) {
                    break;
                }
            }
        }
        return keywords;
    }

    private boolean isIgnoredKeyword(String keyword) {
        return switch (keyword) {
            case "avec", "pour", "dans", "sous", "sans", "entre", "quelle", "quelles", "quel", "quels",
                    "comment", "pourquoi", "question", "bonne", "mauvaise", "reponse", "vrai", "faux",
                    "choisis", "choisir", "relie", "relier", "trouve", "faire", "plus" -> true;
            default -> false;
        };
    }

    private String formatList(List<String> values) {
        return values.isEmpty() ? "-" : String.join(" | ", values);
    }

    private void appendAnswer(StringBuilder builder, Question currentQuestion, List<String> correctAnswers) {
        if (!correctAnswers.isEmpty()) {
            if (currentQuestion.getType() == TypeQuestion.RELIER_FLECHE) {
                builder.append("Les bonnes associations sont: ");
            } else if (currentQuestion.getType() != null && currentQuestion.getType().isMultiChoice()) {
                builder.append("Les bonnes reponses sont: ");
            } else {
                builder.append("La bonne reponse est: ");
            }
            builder.append(String.join(currentQuestion.getType() != null && currentQuestion.getType().isMultiChoice() ? " | " : " / ", correctAnswers));
            builder.append(". ");
        } else {
            builder.append("Je n'ai pas de solution exacte memorisee pour cette question. ");
        }
    }

    private void appendStudentAnswerFeedback(
            StringBuilder builder,
            Question currentQuestion,
            String studentAnswer,
            List<String> correctAnswers
    ) {
        if (studentAnswer == null || studentAnswer.isBlank()) {
            builder.append("Je ne vois pas encore de reponse selectionnee pour toi. ");
            appendAnswer(builder, currentQuestion, correctAnswers);
            return;
        }

        boolean correct = isStudentAnswerCorrect(currentQuestion, studentAnswer, correctAnswers);
        builder.append("Ta reponse actuelle est: ").append(formatStudentAnswer(studentAnswer, currentQuestion)).append(". ");
        if (correct) {
            builder.append("Elle est correcte. ");
        } else {
            builder.append("Elle n'est pas correcte. ");
            appendAnswer(builder, currentQuestion, correctAnswers);
        }
    }

    private boolean isStudentAnswerCorrect(Question currentQuestion, String studentAnswer, List<String> correctAnswers) {
        if (correctAnswers.isEmpty()) {
            return false;
        }
        if (currentQuestion.getType() == null) {
            String normalizedStudentAnswer = normalize(studentAnswer);
            return correctAnswers.stream().anyMatch(answer -> normalize(answer).equals(normalizedStudentAnswer));
        }
        if (currentQuestion.getType() == TypeQuestion.QCM) {
            List<String> selectedAnswers = splitCompositeAnswer(studentAnswer).stream().map(this::normalize).sorted().toList();
            List<String> normalizedCorrectAnswers = correctAnswers.stream().map(this::normalize).sorted().toList();
            return selectedAnswers.equals(normalizedCorrectAnswers);
        }
        if (currentQuestion.getType() == TypeQuestion.RELIER_FLECHE) {
            List<String> selectedPairs = splitCompositeAnswer(studentAnswer).stream().map(this::normalizeMatchingPair).sorted().toList();
            List<String> normalizedCorrectAnswers = correctAnswers.stream().map(this::normalizeMatchingPair).sorted().toList();
            return selectedPairs.equals(normalizedCorrectAnswers);
        }
        String normalizedStudentAnswer = normalize(studentAnswer);
        return correctAnswers.stream().anyMatch(answer -> normalize(answer).equals(normalizedStudentAnswer));
    }

    private void appendChoicesOverview(StringBuilder builder, Question currentQuestion, List<String> answerChoices) {
        if (currentQuestion.getType() == TypeQuestion.RELIER_FLECHE) {
            builder.append("Elements a gauche: ").append(formatList(getMatchingLeftItems(currentQuestion))).append(". ");
            builder.append("Elements a droite: ").append(formatList(getMatchingRightItems(currentQuestion))).append(". ");
            return;
        }
        if (answerChoices.isEmpty()) {
            builder.append("Cette question n'a pas de liste de choix visible. ");
            return;
        }
        builder.append("Les choix proposes sont: ").append(String.join(" | ", answerChoices)).append(". ");
    }

    private String formatStudentAnswer(String studentAnswer, Question currentQuestion) {
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return "-";
        }
        List<String> pieces = splitCompositeAnswer(studentAnswer);
        if (pieces.isEmpty()) {
            return studentAnswer.trim();
        }
        return String.join(" | ", pieces);
    }

    private List<String> splitCompositeAnswer(String answer) {
        List<String> pieces = new ArrayList<>();
        if (answer == null || answer.isBlank()) {
            return pieces;
        }
        for (String piece : answer.split("\\|")) {
            String trimmed = piece.trim();
            if (!trimmed.isBlank()) {
                pieces.add(trimmed);
            }
        }
        return pieces;
    }

    private String normalizeMatchingPair(String pair) {
        if (pair == null || pair.isBlank()) {
            return "";
        }
        String[] pieces = pair.split("->", 2);
        if (pieces.length == 2) {
            return normalize(pieces[0]) + "->" + normalize(pieces[1]);
        }
        return normalize(pair);
    }

    private boolean containsAny(String message, String... values) {
        for (String value : values) {
            if (message.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").toLowerCase().trim();
    }

    private String normalizeApiKeyValue(String rawValue) {
        if (rawValue == null) {
            return "";
        }

        String value = rawValue.trim();
        if (value.length() >= 2) {
            boolean wrappedInDoubleQuotes = value.startsWith("\"") && value.endsWith("\"");
            boolean wrappedInSingleQuotes = value.startsWith("'") && value.endsWith("'");
            if (wrappedInDoubleQuotes || wrappedInSingleQuotes) {
                value = value.substring(1, value.length() - 1).trim();
            }
        }

        return isPlaceholderApiKey(value) ? "" : value;
    }

    private boolean isPlaceholderApiKey(String value) {
        String normalized = normalize(value).replace('_', ' ').replace('-', ' ');
        return normalized.equals("your openai api key here")
                || normalized.equals("replace with your openai api key")
                || normalized.equals("votre cle openai ici")
                || normalized.equals("remplacez par votre cle openai");
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String buildLocalQuizDescription(
            String title,
            String level,
            String ageCategory,
            int questionCount,
            int durationMinutes,
            String status
    ) {
        String safeTitle = safeValue(title).equals("-") ? "ce quiz" : "\"" + title.trim() + "\"";
        String audienceSentence = Quiz.CATEGORIE_AGE_FACILE.equalsIgnoreCase(ageCategory)
                ? "Il privilegie des consignes courtes, des exemples simples et des activites faciles a suivre pour les enfants."
                : "Il propose une progression claire pour consolider les notions importantes et gagner en autonomie.";
        String statusSentence = "Publie".equalsIgnoreCase(status)
                ? "Il peut etre utilise tout de suite en revision."
                : "Il peut encore etre ajuste avant publication si besoin.";
        return safeTitle + " propose " + Math.max(1, questionCount) + " question(s) de niveau "
                + safeValue(level).toLowerCase() + " a realiser en environ " + Math.max(1, durationMinutes)
                + " minute(s). " + audienceSentence + " " + statusSentence;
    }

    private String buildLocalImprovedQuestionText(
            String quizTitle,
            String level,
            String ageCategory,
            TypeQuestion typeQuestion,
            String currentQuestionText
    ) {
        TypeQuestion effectiveType = typeQuestion == null ? getDefaultQuestionType(ageCategory, 0) : typeQuestion;
        String cleaned = cleanSentence(currentQuestionText);
        if (cleaned.isBlank()) {
            cleaned = buildLocalQuestionText(quizTitle, level, ageCategory, effectiveType, 0);
        }
        if (effectiveType == TypeQuestion.REPONSE_LIBRE) {
            return ensureQuestionFormat("En quelques mots, " + decapitalize(stripQuestionMark(cleaned)));
        }
        if (effectiveType == TypeQuestion.RELIER_FLECHE) {
            return ensureQuestionFormat("Relie chaque element a la bonne idee sur le theme " + buildThemeLabel(quizTitle, currentQuestionText, level));
        }
        if (effectiveType == TypeQuestion.VRAI_FAUX && !normalize(cleaned).contains("vraie ou fausse")) {
            return ensureQuestionFormat("Cette affirmation est-elle vraie ou fausse : " + stripTrailingPunctuation(cleaned));
        }
        return ensureQuestionFormat(cleaned);
    }

    private QuestionDraft buildLocalQuestionDraft(
            String quizTitle,
            String level,
            String ageCategory,
            TypeQuestion typeQuestion,
            String questionText,
            int index
    ) {
        TypeQuestion effectiveType = typeQuestion == null ? getDefaultQuestionType(ageCategory, index) : typeQuestion;
        String finalQuestion = buildLocalQuestionText(quizTitle, level, ageCategory, effectiveType, index);
        if (questionText != null && !questionText.isBlank()) {
            finalQuestion = buildLocalImprovedQuestionText(quizTitle, level, ageCategory, effectiveType, questionText);
        }

        return switch (effectiveType) {
            case REPONSE_LIBRE -> new QuestionDraft(
                    finalQuestion,
                    effectiveType,
                    List.of("", "", "", ""),
                    List.of(1),
                    buildLocalFreeTextAnswer(quizTitle, questionText, level)
            );
            case VRAI_FAUX -> new QuestionDraft(
                    finalQuestion,
                    effectiveType,
                    List.of("Vrai", "Faux", "", ""),
                    List.of(1),
                    ""
            );
            case RELIER_FLECHE -> new QuestionDraft(
                    finalQuestion,
                    effectiveType,
                    buildLocalMatchingChoices(quizTitle, questionText, level, ageCategory),
                    List.of(1, 2, 3, 4),
                    ""
            );
            case QCM -> new QuestionDraft(
                    finalQuestion,
                    effectiveType,
                    buildLocalChoiceSet(quizTitle, questionText, level, true),
                    List.of(1, 2),
                    ""
            );
            case PETIT_JEU -> new QuestionDraft(
                    finalQuestion,
                    effectiveType,
                    buildLocalChoiceSet(quizTitle, questionText, level, false),
                    List.of(1),
                    ""
            );
            case QCU -> new QuestionDraft(
                    finalQuestion,
                    effectiveType,
                    buildLocalChoiceSet(quizTitle, questionText, level, false),
                    List.of(1),
                    ""
            );
        };
    }

    private List<QuestionDraft> buildLocalQuizPack(
            String quizTitle,
            String level,
            String ageCategory,
            String description,
            int durationMinutes,
            int questionCount
    ) {
        List<QuestionDraft> drafts = new ArrayList<>();
        for (int index = 0; index < Math.max(1, questionCount); index++) {
            TypeQuestion type = getDefaultQuestionType(ageCategory, index);
            String seedText = (quizTitle == null ? "" : quizTitle) + " " + (description == null ? "" : description) + " " + (index + 1);
            QuestionDraft draft = buildLocalQuestionDraft(quizTitle, level, ageCategory, type, "", index);
            String adjustedQuestion = buildLocalQuestionText(quizTitle, level, ageCategory, type, index);
            drafts.add(new QuestionDraft(
                    adjustedQuestion,
                    draft.type(),
                    draft.choices(),
                    draft.correctIndexes(),
                    draft.freeTextAnswer().isBlank() ? buildLocalFreeTextAnswer(quizTitle, seedText, level) : draft.freeTextAnswer()
            ));
        }
        return drafts;
    }

    private List<QuestionDraft> completeQuizPack(
            List<QuestionDraft> drafts,
            String quizTitle,
            String level,
            String ageCategory,
            String description,
            int durationMinutes,
            int questionCount
    ) {
        List<QuestionDraft> completed = new ArrayList<>();
        for (int index = 0; index < Math.max(1, questionCount); index++) {
            QuestionDraft current = index < drafts.size() ? drafts.get(index) : null;
            TypeQuestion requestedType = current == null ? getDefaultQuestionType(ageCategory, index) : current.type();
            completed.add(ensureUsableDraft(current, quizTitle, level, ageCategory, requestedType, "", index));
        }
        if (completed.isEmpty()) {
            return buildLocalQuizPack(quizTitle, level, ageCategory, description, durationMinutes, questionCount);
        }
        return completed;
    }

    private QuestionDraft ensureUsableDraft(
            QuestionDraft draft,
            String quizTitle,
            String level,
            String ageCategory,
            TypeQuestion requestedType,
            String fallbackQuestionText,
            int index
    ) {
        TypeQuestion effectiveType = requestedType == null ? getDefaultQuestionType(ageCategory, index) : requestedType;
        if (draft == null) {
            return buildLocalQuestionDraft(quizTitle, level, ageCategory, effectiveType, fallbackQuestionText, index);
        }

        TypeQuestion draftType = draft.type() == null ? effectiveType : draft.type();
        String question = cleanSentence(draft.question());
        List<String> choices = sanitizeChoices(draft.choices(), draftType);
        List<Integer> correctIndexes = sanitizeCorrectIndexes(draft.correctIndexes(), draftType);
        String freeText = draft.freeTextAnswer() == null ? "" : draft.freeTextAnswer().trim();

        boolean invalidDraft = question.isBlank();
        if (draftType.isTextAnswer() && freeText.isBlank()) {
            invalidDraft = true;
        }
        if (!draftType.isTextAnswer() && countNonBlankChoices(choices) < 2) {
            invalidDraft = true;
        }
        if (draftType == TypeQuestion.RELIER_FLECHE && countMatchingPairs(choices) < 2) {
            invalidDraft = true;
        }

        if (invalidDraft) {
            return buildLocalQuestionDraft(quizTitle, level, ageCategory, effectiveType, fallbackQuestionText, index);
        }

        return new QuestionDraft(question, draftType, choices, correctIndexes, freeText);
    }

    private TypeQuestion getDefaultQuestionType(String ageCategory, int index) {
        if (Quiz.CATEGORIE_AGE_FACILE.equalsIgnoreCase(ageCategory)) {
            return switch (index % 4) {
                case 0 -> TypeQuestion.VRAI_FAUX;
                case 1 -> TypeQuestion.RELIER_FLECHE;
                case 2 -> TypeQuestion.QCM;
                default -> TypeQuestion.PETIT_JEU;
            };
        }
        return switch (index % 3) {
            case 0 -> TypeQuestion.QCU;
            case 1 -> TypeQuestion.QCM;
            default -> TypeQuestion.REPONSE_LIBRE;
        };
    }

    private String buildLocalQuestionText(
            String quizTitle,
            String level,
            String ageCategory,
            TypeQuestion typeQuestion,
            int index
    ) {
        String theme = buildThemeLabel(quizTitle, quizTitle + " " + level, level);
        return switch (typeQuestion) {
            case VRAI_FAUX -> ensureQuestionFormat("Cette affirmation sur " + theme + " est-elle vraie ou fausse");
            case RELIER_FLECHE -> ensureQuestionFormat("Relie chaque element a la bonne idee sur le theme " + theme);
            case QCM -> ensureQuestionFormat("Quelles propositions aident a mieux comprendre " + theme);
            case REPONSE_LIBRE -> ensureQuestionFormat("En quelques mots, explique une idee importante sur " + theme);
            case PETIT_JEU -> ensureQuestionFormat("Dans ce petit jeu, quelle reponse correspond le mieux au theme " + theme);
            case QCU -> ensureQuestionFormat("Quelle proposition correspond le mieux au theme " + theme);
        };
    }

    private List<String> buildLocalChoiceSet(String quizTitle, String questionText, String level, boolean multiChoice) {
        String theme = buildThemeLabel(quizTitle, questionText, level);
        List<String> choices = new ArrayList<>();
        choices.add("Relire une idee importante sur " + theme);
        choices.add(multiChoice ? "Observer un exemple correct sur " + theme : "Choisir l'idee la plus claire sur " + theme);
        choices.add("Ignorer la consigne de l'exercice");
        choices.add("Repondre au hasard sans verifier");
        return choices;
    }

    private List<String> buildLocalMatchingChoices(String quizTitle, String questionText, String level, String ageCategory) {
        String theme = normalize(buildThemeLabel(quizTitle, questionText, level));
        if (theme.contains("java") || theme.contains("programm") || theme.contains("code")) {
            return List.of(
                    "variable|||stocker",
                    "boucle|||repeter",
                    "condition|||choisir",
                    "liste|||regrouper"
            );
        }
        if (theme.contains("math") || theme.contains("calcul") || theme.contains("nombre")) {
            return List.of(
                    "addition|||plus",
                    "soustraction|||moins",
                    "multiplication|||fois",
                    "division|||partager"
            );
        }
        return List.of(
                "livre|||lire",
                "crayon|||ecrire",
                "oreille|||ecouter",
                "oeil|||observer"
        );
    }

    private String buildLocalFreeTextAnswer(String quizTitle, String questionText, String level) {
        List<String> keywords = extractKeywords((quizTitle == null ? "" : quizTitle) + " " + (questionText == null ? "" : questionText), 1);
        if (!keywords.isEmpty()) {
            return keywords.get(0);
        }
        return buildThemeLabel(quizTitle, questionText, level);
    }

    private String buildThemeLabel(String quizTitle, String questionText, String level) {
        List<String> keywords = extractKeywords((quizTitle == null ? "" : quizTitle) + " " + (questionText == null ? "" : questionText), 1);
        if (!keywords.isEmpty()) {
            return keywords.get(0).toLowerCase();
        }
        if (level != null && !level.isBlank()) {
            return "le niveau " + level.toLowerCase();
        }
        return "ce cours";
    }

    private String cleanSentence(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String ensureQuestionFormat(String value) {
        String cleaned = cleanSentence(value);
        if (cleaned.isBlank()) {
            return "Question generee ?";
        }
        String capitalized = Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
        if (capitalized.endsWith("?")) {
            return capitalized;
        }
        return stripTrailingPunctuation(capitalized) + " ?";
    }

    private String stripQuestionMark(String value) {
        String cleaned = cleanSentence(value);
        if (cleaned.endsWith("?")) {
            return cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private String stripTrailingPunctuation(String value) {
        String cleaned = cleanSentence(value);
        while (!cleaned.isEmpty() && ".!?;:".indexOf(cleaned.charAt(cleaned.length() - 1)) >= 0) {
            cleaned = cleaned.substring(0, cleaned.length() - 1).trim();
        }
        return cleaned;
    }

    private String decapitalize(String value) {
        String cleaned = cleanSentence(value);
        if (cleaned.isEmpty()) {
            return cleaned;
        }
        return Character.toLowerCase(cleaned.charAt(0)) + cleaned.substring(1);
    }

    private int countNonBlankChoices(List<String> choices) {
        int count = 0;
        for (String choice : choices) {
            if (choice != null && !choice.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private int countMatchingPairs(List<String> choices) {
        int count = 0;
        for (String choice : choices) {
            if (choice != null && (choice.contains("|||") || choice.contains("->"))) {
                count++;
            }
        }
        return count;
    }

    private QuestionDraft parseQuestionDraft(String raw, TypeQuestion typeQuestion, String fallbackQuestion) {
        String question = fallbackQuestion == null ? "" : fallbackQuestion.trim();
        List<String> choices = new ArrayList<>(List.of("", "", "", ""));
        List<Integer> correctIndexes = new ArrayList<>();
        String freeText = "";
        TypeQuestion parsedType = typeQuestion == null ? TypeQuestion.QCU : typeQuestion;

        for (String line : raw.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("TYPE:")) {
                String rawType = trimmed.substring("TYPE:".length()).trim();
                try {
                    parsedType = TypeQuestion.valueOf(rawType);
                } catch (IllegalArgumentException ignored) {
                }
            } else if (trimmed.startsWith("QUESTION:")) {
                question = trimmed.substring("QUESTION:".length()).trim();
            } else if (trimmed.startsWith("CHOICE1:")) {
                choices.set(0, trimmed.substring("CHOICE1:".length()).trim());
            } else if (trimmed.startsWith("CHOICE2:")) {
                choices.set(1, trimmed.substring("CHOICE2:".length()).trim());
            } else if (trimmed.startsWith("CHOICE3:")) {
                choices.set(2, trimmed.substring("CHOICE3:".length()).trim());
            } else if (trimmed.startsWith("CHOICE4:")) {
                choices.set(3, trimmed.substring("CHOICE4:".length()).trim());
            } else if (trimmed.startsWith("CORRECT:")) {
                correctIndexes.clear();
                String payload = trimmed.substring("CORRECT:".length()).trim();
                for (String token : payload.split(",")) {
                    try {
                        int index = Integer.parseInt(token.trim());
                        if (index >= 1 && index <= 4 && !correctIndexes.contains(index)) {
                            correctIndexes.add(index);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            } else if (trimmed.startsWith("FREE_TEXT:")) {
                freeText = trimmed.substring("FREE_TEXT:".length()).trim();
            }
        }

        if (parsedType == TypeQuestion.VRAI_FAUX) {
            if (choices.get(0).isBlank()) {
                choices.set(0, "Vrai");
            }
            if (choices.get(1).isBlank()) {
                choices.set(1, "Faux");
            }
        }
        if (parsedType == TypeQuestion.REPONSE_LIBRE && freeText.isBlank()) {
            freeText = question;
        }
        if (parsedType.isSingleChoice() && correctIndexes.isEmpty()) {
            correctIndexes.add(1);
        }
        if (parsedType.isMultiChoice() && correctIndexes.isEmpty()) {
            correctIndexes.add(1);
        }

        return new QuestionDraft(
                question == null || question.isBlank() ? fallbackQuestion : question,
                parsedType,
                sanitizeChoices(choices, parsedType),
                sanitizeCorrectIndexes(correctIndexes, parsedType),
                freeText == null ? "" : freeText.trim()
        );
    }

    private List<QuestionDraft> parseQuizPack(String raw) {
        List<QuestionDraft> drafts = new ArrayList<>();
        for (String block : raw.split("(?m)^---\\s*$")) {
            String trimmed = block.trim();
            if (trimmed.isBlank()) {
                continue;
            }

            TypeQuestion typeQuestion = TypeQuestion.QCU;
            String fallbackQuestion = "";
            for (String line : trimmed.split("\\R")) {
                String current = line.trim();
                if (current.startsWith("TYPE:")) {
                    String rawType = current.substring("TYPE:".length()).trim();
                    try {
                        typeQuestion = TypeQuestion.valueOf(rawType);
                    } catch (IllegalArgumentException ignored) {
                        typeQuestion = TypeQuestion.QCU;
                    }
                } else if (current.startsWith("QUESTION:")) {
                    fallbackQuestion = current.substring("QUESTION:".length()).trim();
                }
            }
            QuestionDraft draft = parseQuestionDraft(trimmed, typeQuestion, fallbackQuestion);
            if (draft.question() != null && !draft.question().isBlank()) {
                drafts.add(draft);
            }
        }
        return drafts;
    }

    private List<String> sanitizeChoices(List<String> rawChoices, TypeQuestion typeQuestion) {
        List<String> sanitized = new ArrayList<>();
        if (rawChoices == null) {
            rawChoices = List.of();
        }
        for (String choice : rawChoices) {
            sanitized.add(choice == null ? "" : choice.trim());
        }
        while (sanitized.size() < 4) {
            sanitized.add("");
        }

        if (typeQuestion == TypeQuestion.VRAI_FAUX) {
            sanitized.set(0, sanitized.get(0).isBlank() ? "Vrai" : sanitized.get(0));
            sanitized.set(1, sanitized.get(1).isBlank() ? "Faux" : sanitized.get(1));
        }
        return sanitized;
    }

    private List<Integer> sanitizeCorrectIndexes(List<Integer> rawIndexes, TypeQuestion typeQuestion) {
        List<Integer> indexes = new ArrayList<>();
        if (rawIndexes != null) {
            for (Integer index : rawIndexes) {
                if (index != null && index >= 1 && index <= 4 && !indexes.contains(index)) {
                    indexes.add(index);
                }
            }
        }
        if (indexes.isEmpty()) {
            indexes.add(1);
        }
        if (typeQuestion != null && typeQuestion.isSingleChoice() && indexes.size() > 1) {
            return List.of(indexes.get(0));
        }
        return indexes;
    }

    public record ChatTurn(String role, String message) {
    }

    public record QuestionDraft(String question, TypeQuestion type, List<String> choices, List<Integer> correctIndexes, String freeTextAnswer) {
    }
}
