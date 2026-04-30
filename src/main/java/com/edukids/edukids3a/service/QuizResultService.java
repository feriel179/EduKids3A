package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.Quiz;
import com.edukids.edukids3a.model.QuizResult;
import com.edukids.edukids3a.persistence.QuizResultRepository;

import java.util.List;

public class QuizResultService {
    private final QuizResultRepository repository;

    public QuizResultService() {
        this(new QuizResultRepository());
    }

    public QuizResultService(QuizResultRepository repository) {
        this.repository = repository;
    }

    public void saveResult(Quiz quiz, int finalScore, int earnedPoints, int totalPoints, String completedAt) {
        repository.save(quiz, finalScore, earnedPoints, totalPoints, completedAt);
    }

    public List<QuizResult> getResultsForQuiz(Quiz quiz) {
        return repository.findByQuiz(quiz);
    }
}
