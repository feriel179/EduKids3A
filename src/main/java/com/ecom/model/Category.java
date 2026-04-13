package com.ecom.model;

public class Category {
    private final int id;
    private final String nom;
    private final String description;

    public Category(int id, String nom, String description) {
        this.id = id;
        this.nom = nom;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return nom;
    }
}
