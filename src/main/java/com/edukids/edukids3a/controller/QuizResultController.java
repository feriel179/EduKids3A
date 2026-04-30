package com.edukids.edukids3a.controller;

import com.edukids.edukids3a.model.Quiz;
import com.edukids.edukids3a.model.QuizResult;
import com.edukids.edukids3a.service.QuizResultService;

import java.util.List;

public class QuizResultController {
    private final QuizResultService quizResultService;

    public QuizResultController(QuizResultService quizResultService) {
        this.quizResultService = quizResultService;
    }

    public void saveResult(Quiz quiz, int finalScore, int earnedPoints, int totalPoints, String completedAt) {
        quizResultService.saveResult(quiz, finalScore, earnedPoints, totalPoints, completedAt);
    }

    public List<QuizResult> getResultsForQuiz(Quiz quiz) {
        return quizResultService.getResultsForQuiz(quiz);
    }
}
