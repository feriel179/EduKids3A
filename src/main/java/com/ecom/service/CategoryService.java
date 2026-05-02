package com.ecom.service;

import com.ecom.model.Category;
import com.ecom.persistence.CategoryRepository;
import com.ecom.validation.CategoryValidator;
import com.ecom.validation.ValidationException;

import java.sql.SQLException;
import java.util.List;

public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryValidator categoryValidator;

    public CategoryService() {
        this.categoryRepository = new CategoryRepository();
        this.categoryValidator = new CategoryValidator();
    }

    public List<Category> getAllCategories() throws SQLException {
        return categoryRepository.findAll();
    }

    public void ajouterCategory(String nom, String description) throws SQLException, ValidationException {
        categoryValidator.validate(nom, description);
        categoryRepository.save(new Category(0, nom.trim(), description.trim()));
    }

    public void modifierCategory(int id, String nom, String description) throws SQLException, ValidationException {
        categoryValidator.validate(nom, description);
        categoryRepository.update(new Category(id, nom.trim(), description.trim()));
    }

    public void supprimerCategory(int id) throws SQLException {
        categoryRepository.deleteById(id);
    }
}
