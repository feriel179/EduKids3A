package com.ecom.model;

import java.time.LocalDateTime;

public class Commande {
    private final int id;
    private final Produit produit;
    private final String clientName;
    private final int quantite;
    private final double montantInitial;
    private final double remise;
    private final double montantTotal;
    private final String codePromoUtilise;
    private final CommandeStatut statut;
    private final String commentaire;
    private final LocalDateTime createdAt;

    public Commande(int id, Produit produit, String clientName, int quantite, double montantTotal,
                    CommandeStatut statut, String commentaire, LocalDateTime createdAt) {
        this(id, produit, clientName, quantite, montantTotal, 0.0, montantTotal, null, statut, commentaire, createdAt);
    }

    public Commande(int id, Produit produit, String clientName, int quantite, double montantInitial, double remise,
                    double montantTotal, String codePromoUtilise, CommandeStatut statut, String commentaire,
                    LocalDateTime createdAt) {
        this.id = id;
        this.produit = produit;
        this.clientName = clientName;
        this.quantite = quantite;
        this.montantInitial = montantInitial;
        this.remise = remise;
        this.montantTotal = montantTotal;
        this.codePromoUtilise = codePromoUtilise;
        this.statut = statut;
        this.commentaire = commentaire;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public Produit getProduit() {
        return produit;
    }

    public String getClientName() {
        return clientName;
    }

    public int getQuantite() {
        return quantite;
    }

    public double getMontantInitial() {
        return montantInitial;
    }

    public double getRemise() {
        return remise;
    }

    public double getMontantTotal() {
        return montantTotal;
    }

    public String getCodePromoUtilise() {
        return codePromoUtilise;
    }

    public CommandeStatut getStatut() {
        return statut;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
