package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.Quiz;
import com.edukids.edukids3a.persistence.QuizRepository;
import com.edukids.edukids3a.validation.QuizValidator;
import javafx.collections.ObservableList;

public class QuizService {
    private final QuizRepository repository;
    private final QuizValidator validator = new QuizValidator();

    public QuizService() {
        this(new QuizRepository());
    }

    public QuizService(QuizRepository repository) {
        this.repository = repository;
    }

    public ObservableList<Quiz> getAllQuizzes() {
        return repository.findAll();
    }

    public void ajouterQuiz(Quiz quiz) {
        validator.validate(quiz);
        repository.save(quiz);
    }

    public void modifierQuiz(Quiz quiz) {
        validator.validate(quiz);
        repository.update(quiz);
    }

    public void supprimerQuiz(Quiz quiz) {
        repository.delete(quiz);
    }

    public QuizRepository getRepository() {
        return repository;
    }
}
