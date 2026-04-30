package com.edukids.edukids3a.service;

import com.edukids.edukids3a.model.Question;
import com.edukids.edukids3a.persistence.QuestionRepository;
import com.edukids.edukids3a.validation.QuestionValidator;
import javafx.collections.ObservableList;

public class    QuestionService {
    private final QuestionRepository repository;
    private final QuestionValidator validator = new QuestionValidator();

    public QuestionService(QuizService quizService) {
        this.repository = new QuestionRepository(quizService.getRepository());
    }

    public ObservableList<Question> getAllQuestions() {
        return repository.findAll();
    }

    public void ajouterQuestion(Question question) {
        validator.validate(question);
        repository.save(question);
    }

    public void modifierQuestion(Question originalQuestion, Question updatedQuestion) {
        validator.validate(updatedQuestion);
        repository.update(originalQuestion, updatedQuestion);
    }

    public void supprimerQuestion(Question question) {
        repository.delete(question);
    }
}
