package com.edukids.edukids3a.controller;

import com.edukids.edukids3a.model.Quiz;
import com.edukids.edukids3a.service.QuizService;
import javafx.collections.ObservableList;

import java.util.Comparator;

public class QuizController {
    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    public ObservableList<Quiz> getAllQuizzes() {
        return quizService.getAllQuizzes();
    }

    public Quiz saveQuiz(Quiz currentQuiz, Quiz quiz) {
        if (currentQuiz == null) {
            return quizService.ajouterQuiz(quiz);
        }
        quizService.modifierQuiz(quiz);
        return quiz;
    }

    public void deleteQuiz(Quiz quiz) {
        quizService.supprimerQuiz(quiz);
    }

    public boolean matchesSearch(Quiz quiz, String searchValue) {
        if (searchValue == null || searchValue.isBlank()) {
            return true;
        }
        String query = searchValue.toLowerCase();
        return quiz.getTitre().toLowerCase().contains(query)
                || quiz.getStatut().toLowerCase().contains(query)
                || quiz.getNiveau().toLowerCase().contains(query)
                || quiz.getCategorieAge().toLowerCase().contains(query);
    }

    public Comparator<Quiz> getBackOfficeComparator(String sortValue) {
        return switch (sortValue) {
            case "Titre Z-A" -> Comparator.comparing(Quiz::getTitre, String.CASE_INSENSITIVE_ORDER).reversed();
            case "Plus de questions" -> Comparator.comparingInt(Quiz::getNombreQuestions).reversed();
            case "Moins de questions" -> Comparator.comparingInt(Quiz::getNombreQuestions);
            case "Statut" -> Comparator.comparing(Quiz::getStatut, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Quiz::getTitre, String.CASE_INSENSITIVE_ORDER);
        };
    }

    public Comparator<Quiz> getFrontOfficeComparator(String sortValue) {
        return switch (sortValue) {
            case "Titre A-Z" -> Comparator.comparing(Quiz::getTitre, String.CASE_INSENSITIVE_ORDER);
            case "Titre Z-A" -> Comparator.comparing(Quiz::getTitre, String.CASE_INSENSITIVE_ORDER).reversed();
            case "Niveau" -> Comparator.comparing(Quiz::getNiveau, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(quiz -> quiz.getId() == null ? 0 : quiz.getId(), Comparator.reverseOrder());
        };
    }

    public String buildFrontHeroStatsText(int questionCount) {
        long quizFaciles = getAllQuizzes().stream().filter(Quiz::isCategorieFacile).count();
        long quizStandard = getAllQuizzes().size() - quizFaciles;
        return getAllQuizzes().size() + " quiz • " + questionCount + " questions • "
                + quizFaciles + " faciles • " + quizStandard + " standards";
    }
}
