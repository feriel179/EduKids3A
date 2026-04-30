package com.edukids.edukids3a.model;

import java.util.List;

public class Question {
    private final Integer id;
    private final Quiz quiz;
    private final String intitule;
    private final TypeQuestion type;
    private final int points;
    private final List<Reponse> reponses;

    public Question(Quiz quiz, String intitule, TypeQuestion type, int points, List<Reponse> reponses) {
        this(null, quiz, intitule, type, points, reponses);
    }

    public Question(Integer id, Quiz quiz, String intitule, TypeQuestion type, int points, List<Reponse> reponses) {
        this.id = id;
        this.quiz = quiz;
        this.intitule = intitule;
        this.type = type;
        this.points = points;
        this.reponses = reponses;
    }

    public Integer getId() {
        return id;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public String getIntitule() {
        return intitule;
    }

    public TypeQuestion getType() {
        return type;
    }

    public int getPoints() {
        return points;
    }

    public List<Reponse> getReponses() {
        return reponses;
    }

    public String getQuizTitre() {
        return quiz.getTitre();
    }

    public String getTypeLabel() {
        return type.getLabel();
    }

    public String getResumeReponses() {
        if (type == TypeQuestion.RELIER_FLECHE) {
            return reponses.stream()
                    .map(reponse -> {
                        String[] parts = reponse.getTexte().split("\\|\\|\\|", 2);
                        if (parts.length == 2) {
                            return parts[0] + " -> " + parts[1];
                        }
                        return reponse.getTexte();
                    })
                    .reduce((left, right) -> left + " | " + right)
                    .orElse("");
        }
        return reponses.stream()
                .map(reponse -> reponse.getTexte() + (reponse.isCorrecte() ? " *" : ""))
                .reduce((left, right) -> left + " | " + right)
                .orElse("");
    }
}
