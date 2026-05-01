package com.edukids.edukids3a.controller;

import com.edukids.edukids3a.model.Question;
import com.edukids.edukids3a.model.Quiz;
import com.edukids.edukids3a.model.Reponse;
import com.edukids.edukids3a.model.TypeQuestion;
import com.edukids.edukids3a.service.QuestionService;
import javafx.collections.ObservableList;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuestionController {
    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    public ObservableList<Question> getAllQuestions() {
        return questionService.getAllQuestions();
    }

    public void saveQuestion(Question currentQuestion, Question question) {
        if (currentQuestion == null) {
            questionService.ajouterQuestion(question);
        } else {
            questionService.modifierQuestion(currentQuestion, question);
        }
    }

    public void deleteQuestion(Question question) {
        questionService.supprimerQuestion(question);
    }

    public List<Question> getQuestionsForQuiz(Quiz quiz) {
        return getAllQuestions().stream()
                .filter(question -> question.getQuiz().getId() != null
                        && quiz.getId() != null
                        && question.getQuiz().getId().equals(quiz.getId()))
                .toList();
    }

    public boolean matchesSearch(Question question, String searchValue) {
        if (searchValue == null || searchValue.isBlank()) {
            return true;
        }
        String query = searchValue.toLowerCase();
        return question.getIntitule().toLowerCase().contains(query)
                || question.getQuizTitre().toLowerCase().contains(query)
                || question.getTypeLabel().toLowerCase().contains(query)
                || question.getResumeReponses().toLowerCase().contains(query);
    }

    public Comparator<Question> getComparator(String sortValue) {
        return switch (sortValue) {
            case "Question Z-A" -> Comparator.comparing(Question::getIntitule, String.CASE_INSENSITIVE_ORDER).reversed();
            case "Quiz A-Z" -> Comparator.comparing(Question::getQuizTitre, String.CASE_INSENSITIVE_ORDER);
            case "Type" -> Comparator.comparing(Question::getTypeLabel, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Question::getIntitule, String.CASE_INSENSITIVE_ORDER);
        };
    }

    public QuizScore computeScore(
            List<Question> quizQuestions,
            Map<Question, TextField> freeTextAnswers,
            Map<Question, List<CheckBox>> qcmAnswers,
            Map<Question, ToggleGroup> qcuAnswers,
            Map<Question, Map<String, String>> matchingAnswers
    ) {
        int totalPoints = quizQuestions.stream().mapToInt(Question::getPoints).sum();
        int earnedPoints = 0;

        for (Question question : quizQuestions) {
            if (question.getType().isTextAnswer()) {
                TextField answerField = freeTextAnswers.get(question);
                String userAnswer = answerField == null ? "" : answerField.getText().trim();
                String expected = question.getReponses().isEmpty() ? "" : question.getReponses().get(0).getTexte().trim();
                if (userAnswer.equalsIgnoreCase(expected)) {
                    earnedPoints += question.getPoints();
                }
                continue;
            }

            if (question.getType().isSingleChoice()) {
                ToggleGroup toggleGroup = qcuAnswers.get(question);
                if (toggleGroup != null && toggleGroup.getSelectedToggle() instanceof RadioButton selectedButton) {
                    String selectedText = selectedButton.getText();
                    boolean isCorrect = question.getReponses().stream()
                            .anyMatch(reponse -> reponse.isCorrecte() && reponse.getTexte().equals(selectedText));
                    if (isCorrect) {
                        earnedPoints += question.getPoints();
                    }
                }
                continue;
            }

            if (question.getType() == TypeQuestion.RELIER_FLECHE) {
                Map<String, String> selectedPairs = matchingAnswers.get(question);
                if (selectedPairs != null) {
                    Map<String, String> expectedPairs = new LinkedHashMap<>();
                    for (Reponse reponse : question.getReponses()) {
                        String text = reponse.getTexte() == null ? "" : reponse.getTexte().trim();
                        if (text.contains("|||")) {
                            String[] parts = text.split("\\|\\|\\|", 2);
                            if (parts.length == 2) {
                                expectedPairs.put(parts[0].trim(), parts[1].trim());
                            }
                        }
                    }
                    if (expectedPairs.isEmpty()) {
                        List<String> legacyValues = question.getReponses().stream()
                                .map(Reponse::getTexte)
                                .filter(text -> text != null && !text.trim().isEmpty())
                                .map(String::trim)
                                .toList();
                        for (int i = 0; i + 1 < legacyValues.size(); i += 2) {
                            expectedPairs.put(legacyValues.get(i), legacyValues.get(i + 1));
                        }
                    }
                    if (selectedPairs.equals(expectedPairs)) {
                        earnedPoints += question.getPoints();
                    }
                }
                continue;
            }

            List<CheckBox> selectedBoxes = qcmAnswers.get(question);
            if (selectedBoxes != null) {
                List<String> selectedTexts = selectedBoxes.stream()
                        .filter(CheckBox::isSelected)
                        .map(CheckBox::getText)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();

                List<String> correctTexts = question.getReponses().stream()
                        .filter(Reponse::isCorrecte)
                        .map(Reponse::getTexte)
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList();

                if (selectedTexts.equals(correctTexts)) {
                    earnedPoints += question.getPoints();
                }
            }
        }

        int finalScore = totalPoints == 0 ? 0 : (earnedPoints * 100) / totalPoints;
        return new QuizScore(finalScore, earnedPoints, totalPoints);
    }

    public record QuizScore(int percent, int earnedPoints, int totalPoints) {
    }
}
