package com.edukids.edukids3a.model;

public class Quiz {
    public static final String CATEGORIE_AGE_FACILE = "8-10 ans";
    public static final String CATEGORIE_AGE_STANDARD = "10 ans et plus";

    private final Integer id;
    private final String titre;
    private final String description;
    private final String imageUrl;
    private final String niveau;
    private final String categorieAge;
    private int nombreQuestions;
    private final int dureeMinutes;
    private final int scoreMinimum;
    private final String statut;

    public Quiz(String titre, String description, String imageUrl, String niveau, int nombreQuestions, int dureeMinutes, int scoreMinimum, String statut) {
        this(null, titre, description, imageUrl, niveau, CATEGORIE_AGE_STANDARD, nombreQuestions, dureeMinutes, scoreMinimum, statut);
    }

    public Quiz(Integer id, String titre, String description, String imageUrl, String niveau, int nombreQuestions, int dureeMinutes, int scoreMinimum, String statut) {
        this(id, titre, description, imageUrl, niveau, CATEGORIE_AGE_STANDARD, nombreQuestions, dureeMinutes, scoreMinimum, statut);
    }

    public Quiz(String titre, String description, String imageUrl, String niveau, String categorieAge, int nombreQuestions, int dureeMinutes, int scoreMinimum, String statut) {
        this(null, titre, description, imageUrl, niveau, categorieAge, nombreQuestions, dureeMinutes, scoreMinimum, statut);
    }

    public Quiz(Integer id, String titre, String description, String imageUrl, String niveau, String categorieAge, int nombreQuestions, int dureeMinutes, int scoreMinimum, String statut) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.imageUrl = imageUrl;
        this.niveau = niveau;
        this.categorieAge = categorieAge == null || categorieAge.isBlank() ? CATEGORIE_AGE_STANDARD : categorieAge;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public String getNiveau() {
        return niveau;
    }

    public String getCategorieAge() {
        return categorieAge;
    }

    public int getNombreQuestions() {
        return nombreQuestions;
    }

    public void setNombreQuestions(int nombreQuestions) {
        this.nombreQuestions = Math.max(0, nombreQuestions);
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

    public boolean isCategorieFacile() {
        return CATEGORIE_AGE_FACILE.equalsIgnoreCase(categorieAge);
    }

    public String getAudienceHint() {
        if (isCategorieFacile()) {
            return "Quiz faciles pour 8-10 ans: vrai ou faux, relier, choix simples.";
        }
        return "Quiz complets pour les eleves de plus de 10 ans.";
    }

    @Override
    public String toString() {
        return titre;
    }
}
