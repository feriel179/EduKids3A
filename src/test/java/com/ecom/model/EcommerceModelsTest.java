package com.ecom.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EcommerceModelsTest {

    @Test
    void categoryAndProductExposeConstructorValues() {
        Category category = new Category(1, "Livres", "Livres educatifs");
        Produit produit = new Produit(2, "Livre Java", "Description", "java.png", 29.5, 7, category);

        assertEquals("Livres", category.toString());
        assertEquals(2, produit.getId());
        assertEquals("Livre Java", produit.getNom());
        assertEquals(29.5, produit.getPrix());
        assertEquals(7, produit.getStock());
        assertSame(category, produit.getCategory());
    }

    @Test
    void commandeExposesFinancialFieldsAndStatusLabels() {
        Produit produit = new Produit(2, "Livre Java", "Description", "java.png", 29.5, 7,
                new Category(1, "Livres", "Livres educatifs"));
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 2, 10, 0);
        Commande commande = new Commande(3, produit, "Ada", 2, 59.0, 10.0,
                49.0, "PROMO10", CommandeStatut.VALIDEE, "Merci", createdAt);

        assertEquals(59.0, commande.getMontantInitial());
        assertEquals(10.0, commande.getRemise());
        assertEquals(49.0, commande.getMontantTotal());
        assertEquals("PROMO10", commande.getCodePromoUtilise());
        assertEquals("Validee", commande.getStatut().getLabel());
        assertEquals(createdAt, commande.getCreatedAt());
    }
}
