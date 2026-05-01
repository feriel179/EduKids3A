package com.edukids.edukids3a.validation;

import com.edukids.edukids3a.model.Quiz;

public class QuizValidator {

    public void validate(Quiz quiz) {
        if (quiz.getTitre() == null || quiz.getTitre().trim().isEmpty()) {
            throw new ValidationException("Le titre est obligatoire.");
        }

        if (quiz.getTitre().trim().length() < 3) {
            throw new ValidationException("Le titre doit contenir au moins 3 caracteres.");
        }

        if (quiz.getTitre().trim().length() > 120) {
            throw new ValidationException("Le titre ne doit pas depasser 120 caracteres.");
        }

        if (quiz.getDescription() == null || quiz.getDescription().trim().length() < 10) {
            throw new ValidationException("La description doit contenir au moins 10 caracteres.");
        }

        if (quiz.getDescription().trim().length() > 1000) {
            throw new ValidationException("La description ne doit pas depasser 1000 caracteres.");
        }

        if (quiz.getImageUrl() != null && quiz.getImageUrl().trim().length() > 500) {
            throw new ValidationException("L'URL de l'image ne doit pas depasser 500 caracteres.");
        }

        if (quiz.getCategorieAge() == null || quiz.getCategorieAge().isBlank()) {
            throw new ValidationException("La categorie d'age est obligatoire.");
        }

        if (quiz.getDureeMinutes() <= 0) {
            throw new ValidationException("La duree doit etre superieure a 0.");
        }

        if (quiz.getScoreMinimum() < 0 || quiz.getScoreMinimum() > 100) {
            throw new ValidationException("Le score minimum doit etre entre 0 et 100.");
        }

        if (quiz.getNombreQuestions() < 0) {
            throw new ValidationException("Le nombre de questions ne peut pas etre negatif.");
        }
    }
}
