package com.ecom.validation;

import com.ecom.model.Category;

public class ProduitValidator {

    public void validate(String nom, String description, String imagePath, String prixText, String stockText, Category category)
            throws ValidationException {
        if (nom == null || nom.isBlank()
                || description == null || description.isBlank()
                || imagePath == null || imagePath.isBlank()
                || prixText == null || prixText.isBlank()
                || stockText == null || stockText.isBlank()
                || category == null) {
            throw new ValidationException("Veuillez remplir tous les champs du produit.");
        }

        String trimmedNom = nom.trim();
        String trimmedDescription = description.trim();
        String trimmedImagePath = imagePath.trim();

        if (trimmedNom.length() < 3) {
            throw new ValidationException("Le nom du produit doit contenir au moins 3 caracteres.");
        }

        if (!trimmedNom.matches("[\\p{L}0-9\\s\\-']+")) {
            throw new ValidationException("Le nom du produit contient des caracteres invalides.");
        }

        if (trimmedDescription.length() < 5) {
            throw new ValidationException("La description du produit doit contenir au moins 5 caracteres.");
        }

        if (!(trimmedImagePath.endsWith(".png")
                || trimmedImagePath.endsWith(".jpg")
                || trimmedImagePath.endsWith(".jpeg")
                || trimmedImagePath.endsWith(".gif")
                || trimmedImagePath.endsWith(".webp"))) {
            throw new ValidationException("L'image doit etre un fichier png, jpg, jpeg, gif ou webp.");
        }

        try {
            double prix = Double.parseDouble(prixText);
            if (prix <= 0) {
                throw new ValidationException("Le prix doit etre superieur a 0.");
            }
        } catch (NumberFormatException exception) {
            throw new ValidationException("Le prix doit etre numerique.");
        }

        try {
            int stock = Integer.parseInt(stockText);
            if (stock < 0) {
                throw new ValidationException("Le stock doit etre positif ou nul.");
            }
        } catch (NumberFormatException exception) {
            throw new ValidationException("Le stock doit etre numerique.");
        }
    }
}
