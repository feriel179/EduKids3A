package com.ecom.validation;

public class CategoryValidator {

    public void validate(String nom, String description) throws ValidationException {
        if (nom == null || nom.isBlank() || description == null || description.isBlank()) {
            throw new ValidationException("Veuillez remplir tous les champs de la categorie.");
        }

        String trimmedNom = nom.trim();
        String trimmedDescription = description.trim();

        if (trimmedNom.length() < 3) {
            throw new ValidationException("Le nom de la categorie doit contenir au moins 3 caracteres.");
        }

        if (!trimmedNom.matches("[\\p{L}0-9\\s\\-']+")) {
            throw new ValidationException("Le nom de la categorie contient des caracteres invalides.");
        }

        if (trimmedDescription.length() < 5) {
            throw new ValidationException("La description de la categorie doit contenir au moins 5 caracteres.");
        }
    }
}
