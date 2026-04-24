package com.edukids.edukids3a.models;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Types d'événement alignés sur la partie web (Evenement::TYPES).
 */
public enum TypeEvenement {
    SPORT("Sport", "sport"),
    ART("Art", "art"),
    MUSIQUE("Musique", "musique"),
    EDUCATION("Éducation", "education"),
    SCIENCE("Science", "science"),
    TECHNOLOGIE("Technologie", "technologie"),
    CULTURE("Culture", "culture"),
    SORTIE("Sortie", "sortie"),
    ATELIER("Atelier", "atelier"),
    FETE("Fête", "fete");

    private final String libelle;
    private final String code;

    TypeEvenement(String libelle, String code) {
        this.libelle = libelle;
        this.code = code;
    }

    public String getLibelle() {
        return libelle;
    }

    public String getCode() {
        return code;
    }

    public static TypeEvenement fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElse(null);
    }

    public static Map<String, String> libellesParCode() {
        Map<String, String> m = new LinkedHashMap<>();
        for (TypeEvenement t : values()) {
            m.put(t.code, t.libelle);
        }
        return m;
    }
}
