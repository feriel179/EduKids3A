package com.edukids.edukids3a.model;

public enum TypeQuestion {
    QCM("QCM"),
    QCU("QCU"),
    REPONSE_LIBRE("Reponse libre"),
    VRAI_FAUX("Vrai ou Faux"),
    RELIER_FLECHE("Relier par une fleche"),
    PETIT_JEU("Petit jeu");

    private final String label;

    TypeQuestion(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isTextAnswer() {
        return this == REPONSE_LIBRE;
    }

    public boolean isSingleChoice() {
        return this == QCU || this == VRAI_FAUX || this == PETIT_JEU;
    }

    public boolean isMultiChoice() {
        return this == QCM || this == RELIER_FLECHE;
    }

    public boolean isEasyModeType() {
        return this == QCM || this == VRAI_FAUX || this == RELIER_FLECHE || this == PETIT_JEU;
    }

    @Override
    public String toString() {
        return label;
    }
}
