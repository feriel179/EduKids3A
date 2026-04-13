package com.edukids.edukids3a.model;

public class Reponse {
    private final Integer id;
    private final String texte;
    private final boolean correcte;

    public Reponse(String texte, boolean correcte) {
        this(null, texte, correcte);
    }

    public Reponse(Integer id, String texte, boolean correcte) {
        this.id = id;
        this.texte = texte;
        this.correcte = correcte;
    }

    public Integer getId() {
        return id;
    }

    public String getTexte() {
        return texte;
    }

    public boolean isCorrecte() {
        return correcte;
    }
}
