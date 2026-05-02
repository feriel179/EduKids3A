package com.ecom.validation;

import com.ecom.model.Category;
import com.ecom.model.Produit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EcommerceValidatorTest {

    private final CategoryValidator categoryValidator = new CategoryValidator();
    private final ProduitValidator produitValidator = new ProduitValidator();
    private final CommandeValidator commandeValidator = new CommandeValidator();

    @Test
    void categoryValidator_acceptsValidCategory() {
        assertDoesNotThrow(() -> categoryValidator.validate("Livres", "Livres educatifs pour enfants"));
    }

    @Test
    void categoryValidator_rejectsInvalidNameCharacters() {
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> categoryValidator.validate("Cat<script>", "Description correcte")
        );

        assertEquals("Le nom de la categorie contient des caracteres invalides.", exception.getMessage());
    }

    @Test
    void produitValidator_acceptsValidProduct() {
        Category category = new Category(1, "Livres", "Livres educatifs");

        assertDoesNotThrow(() -> produitValidator.validate(
                "Livre Java",
                "Livre pratique",
                "java.png",
                "29.5",
                "12",
                category
        ));
    }

    @Test
    void produitValidator_rejectsInvalidImageExtension() {
        Category category = new Category(1, "Livres", "Livres educatifs");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> produitValidator.validate("Livre Java", "Livre pratique", "java.pdf", "29.5", "12", category)
        );

        assertEquals("L'image doit etre un fichier png, jpg, jpeg, gif ou webp.", exception.getMessage());
    }

    @Test
    void produitValidator_rejectsNegativeStock() {
        Category category = new Category(1, "Livres", "Livres educatifs");

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> produitValidator.validate("Livre Java", "Livre pratique", "java.png", "29.5", "-1", category)
        );

        assertEquals("Le stock doit etre positif ou nul.", exception.getMessage());
    }

    @Test
    void commandeValidator_returnsParsedQuantityForValidOrder() {
        Produit produit = new Produit(1, "Livre Java", "Livre pratique", "java.png", 29.5, 5,
                new Category(1, "Livres", "Livres educatifs"));

        int quantity = assertDoesNotThrow(() -> commandeValidator.validateCreation(produit, "3", "Merci"));

        assertEquals(3, quantity);
    }

    @Test
    void commandeValidator_rejectsQuantityAboveStock() {
        Produit produit = new Produit(1, "Livre Java", "Livre pratique", "java.png", 29.5, 2,
                new Category(1, "Livres", "Livres educatifs"));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> commandeValidator.validateCreation(produit, "3", null)
        );

        assertEquals("La quantite demandee depasse le stock disponible.", exception.getMessage());
    }
}
