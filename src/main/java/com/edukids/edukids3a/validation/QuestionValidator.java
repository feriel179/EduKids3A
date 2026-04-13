package com.edukids.edukids3a.validation;

import com.edukids.edukids3a.model.Question;
import com.edukids.edukids3a.model.Reponse;
import com.edukids.edukids3a.model.TypeQuestion;

public class QuestionValidator {

    public void validate(Question question) {
        if (question.getQuiz() == null) {
            throw new ValidationException("Vous devez selectionner un quiz.");
        }

        if (question.getIntitule() == null || question.getIntitule().trim().length() < 5) {
            throw new ValidationException("L'intitule de la question doit contenir au moins 5 caracteres.");
        }

        if (question.getIntitule().trim().length() > 500) {
            throw new ValidationException("L'intitule de la question ne doit pas depasser 500 caracteres.");
        }

        if (question.getPoints() <= 0) {
            throw new ValidationException("Les points doivent etre superieurs a 0.");
        }

        if (question.getType() == TypeQuestion.REPONSE_LIBRE) {
            if (question.getReponses().isEmpty() || question.getReponses().get(0).getTexte().trim().isEmpty()) {
                throw new ValidationException("La reponse attendue est obligatoire pour une question libre.");
            }
            if (question.getReponses().get(0).getTexte().trim().length() > 255) {
                throw new ValidationException("La reponse libre ne doit pas depasser 255 caracteres.");
            }
            return;
        }

        long filledAnswers = question.getReponses().stream()
                .map(Reponse::getTexte)
                .filter(text -> text != null && !text.trim().isEmpty())
                .count();

        if (filledAnswers < 2) {
            throw new ValidationException("Ajoutez au moins deux reponses pour une question QCM ou QCU.");
        }

        long correctAnswers = question.getReponses().stream().filter(Reponse::isCorrecte).count();
        if (correctAnswers == 0) {
            throw new ValidationException("Selectionnez au moins une bonne reponse.");
        }

        if (question.getType() == TypeQuestion.QCU && correctAnswers > 1) {
            throw new ValidationException("Une question QCU doit avoir une seule bonne reponse.");
        }

        boolean tooLongAnswer = question.getReponses().stream()
                .map(Reponse::getTexte)
                .filter(text -> text != null && !text.trim().isEmpty())
                .anyMatch(text -> text.trim().length() > 255);

        if (tooLongAnswer) {
            throw new ValidationException("Chaque reponse doit contenir au maximum 255 caracteres.");
        }
    }
}
