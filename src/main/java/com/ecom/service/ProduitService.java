package com.ecom.service;

import com.ecom.model.Category;
import com.ecom.model.Produit;
import com.ecom.persistence.ProduitRepository;
import com.ecom.validation.ProduitValidator;
import com.ecom.validation.ValidationException;

import java.sql.SQLException;
import java.util.List;

public class ProduitService {

    private final ProduitRepository produitRepository;
    private final ProduitValidator produitValidator;

    public ProduitService() {
        this.produitRepository = new ProduitRepository();
        this.produitValidator = new ProduitValidator();
    }

    public List<Produit> getAllProduits() throws SQLException {
        return produitRepository.findAll();
    }

    public void ajouterProduit(String nom, String description, String imagePath, String prixText, String stockText, Category category)
            throws ValidationException, SQLException {
        produitValidator.validate(nom, description, imagePath, prixText, stockText, category);

        Produit produit = new Produit(
                0,
                nom.trim(),
                description.trim(),
                imagePath.trim(),
                Double.parseDouble(prixText.trim()),
                Integer.parseInt(stockText.trim()),
                category
        );

        produitRepository.save(produit);
    }

    public void modifierProduit(int id, String nom, String description, String imagePath, String prixText, String stockText, Category category)
            throws ValidationException, SQLException {
        produitValidator.validate(nom, description, imagePath, prixText, stockText, category);

        Produit produit = new Produit(
                id,
                nom.trim(),
                description.trim(),
                imagePath.trim(),
                Double.parseDouble(prixText.trim()),
                Integer.parseInt(stockText.trim()),
                category
        );

        produitRepository.update(produit);
    }

    public void supprimerProduit(int id) throws SQLException {
        produitRepository.deleteById(id);
    }
}
