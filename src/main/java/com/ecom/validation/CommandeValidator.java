package com.ecom.validation;

import com.ecom.model.Produit;

public class CommandeValidator {

    public int validateCreation(Produit produit, String quantiteText, String commentaire) throws ValidationException {
        if (produit == null) {
            throw new ValidationException("Veuillez selectionner un produit.");
        }

        if (quantiteText == null || quantiteText.isBlank()) {
            throw new ValidationException("Veuillez saisir une quantite.");
        }

        final int quantite;
        try {
            quantite = Integer.parseInt(quantiteText.trim());
        } catch (NumberFormatException exception) {
            throw new ValidationException("La quantite doit etre numerique.");
        }

        if (quantite <= 0) {
            throw new ValidationException("La quantite doit etre superieure a 0.");
        }

        if (quantite > produit.getStock()) {
            throw new ValidationException("La quantite demandee depasse le stock disponible.");
        }

        if (commentaire != null && commentaire.trim().length() > 255) {
            throw new ValidationException("Le commentaire ne doit pas depasser 255 caracteres.");
        }

        return quantite;
    }
}
