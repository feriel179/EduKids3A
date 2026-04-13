package com.edukids.edukids3a.model;

public class Quiz {
    private final Integer id;
    private final String titre;
    private final String description;
    private final String niveau;
    private int nombreQuestions;
    private final int dureeMinutes;
    private final int scoreMinimum;
    private final String statut;

    public Quiz(String titre, String description, String niveau, int nombreQuestions, int dureeMinutes, int scoreMinimum, String statut) {
        this(null, titre, description, niveau, nombreQuestions, dureeMinutes, scoreMinimum, statut);
    }

    public Quiz(Integer id, String titre, String description, String niveau, int nombreQuestions, int dureeMinutes, int scoreMinimum, String statut) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.niveau = niveau;
        this.nombreQuestions = nombreQuestions;
        this.dureeMinutes = dureeMinutes;
        this.scoreMinimum = scoreMinimum;
        this.statut = statut;
    }

    public Integer getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    public String getDescription() {
        return description;
    }

    public String getNiveau() {
        return niveau;
    }

    public int getNombreQuestions() {
        return nombreQuestions;
    }

    public void incrementNombreQuestions() {
        this.nombreQuestions++;
    }

    public void decrementNombreQuestions() {
        if (this.nombreQuestions > 0) {
            this.nombreQuestions--;
        }
    }

    public int getDureeMinutes() {
        return dureeMinutes;
    }

    public int getScoreMinimum() {
        return scoreMinimum;
    }

    public String getStatut() {
        return statut;
    }

    @Override
    public String toString() {
        return titre;
    }
}
