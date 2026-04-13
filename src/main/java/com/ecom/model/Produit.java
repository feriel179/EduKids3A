package com.ecom.model;

public class Produit {
    private final int id;
    private final String nom;
    private final String description;
    private final String imageUrl;
    private final double prix;
    private final int stock;
    private final Category category;

    public Produit(int id, String nom, String description, String imageUrl, double prix, int stock, Category category) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.imageUrl = imageUrl;
        this.prix = prix;
        this.stock = stock;
        this.category = category;
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

    public String getImageUrl() {
        return imageUrl;
    }

    public double getPrix() {
        return prix;
    }

    public int getStock() {
        return stock;
    }

    public Category getCategory() {
        return category;
    }
}
