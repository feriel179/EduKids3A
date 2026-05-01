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

        if (question.getType().isTextAnswer()) {
            if (question.getReponses().isEmpty() || question.getReponses().get(0).getTexte().trim().isEmpty()) {
                throw new ValidationException("La reponse attendue est obligatoire pour une question libre.");
            }
            if (question.getReponses().get(0).getTexte().trim().length() > 255) {
                throw new ValidationException("La reponse libre ne doit pas depasser 255 caracteres.");
            }
            return;
        }

        if (question.getType() == TypeQuestion.RELIER_FLECHE) {
            long validPairs = question.getReponses().stream()
                    .map(Reponse::getTexte)
                    .filter(text -> text != null && text.contains("|||"))
                    .map(text -> text.split("\\|\\|\\|", 2))
                    .filter(parts -> parts.length == 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty())
                    .count();
            if (validPairs < 2) {
                throw new ValidationException("Ajoutez au moins deux associations pour une question Relier par une fleche.");
            }
            return;
        }

        long filledAnswers = question.getReponses().stream()
                .map(Reponse::getTexte)
                .filter(text -> text != null && !text.trim().isEmpty())
                .count();

        if (filledAnswers < 2) {
            throw new ValidationException("Ajoutez au moins deux reponses pour cette question.");
        }

        long correctAnswers = question.getReponses().stream().filter(Reponse::isCorrecte).count();
        if (correctAnswers == 0) {
            throw new ValidationException("Selectionnez au moins une bonne reponse.");
        }

        if (question.getType().isSingleChoice() && correctAnswers > 1) {
            throw new ValidationException("Cette question doit avoir une seule bonne reponse.");
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
