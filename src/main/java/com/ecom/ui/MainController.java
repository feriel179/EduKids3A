package com.ecom.ui;

import com.ecom.model.Category;
import com.ecom.model.Produit;
import com.ecom.service.CategoryService;
import com.ecom.service.ProduitService;
import com.ecom.validation.ValidationException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainController {

    @FXML
    private StackPane contentArea;
    @FXML
    private VBox frontOfficeView;
    @FXML
    private BorderPane backOfficeView;
    @FXML
    private VBox frontHomeSection;
    @FXML
    private VBox frontListSection;
    @FXML
    private VBox frontDetailSection;
    @FXML
    private VBox frontCategoryListSection;
    @FXML
    private VBox frontCategoryDetailSection;
    @FXML
    private VBox produitListView;
    @FXML
    private VBox produitFormView;
    @FXML
    private VBox categoryListView;
    @FXML
    private VBox categoryFormView;
    @FXML
    private TextField nomField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField imageUrlField;
    @FXML
    private TextField prixField;
    @FXML
    private TextField stockField;
    @FXML
    private ComboBox<Category> categoryCombo;
    @FXML
    private TextField produitSearchField;
    @FXML
    private ComboBox<String> produitSortCombo;
    @FXML
    private TextField frontSearchField;
    @FXML
    private ComboBox<String> frontSortCombo;
    @FXML
    private TextField categoryNomField;
    @FXML
    private TextArea categoryDescriptionArea;
    @FXML
    private TextField categorySearchField;
    @FXML
    private ComboBox<String> categorySortCombo;
    @FXML
    private TableView<Produit> produitTable;
    @FXML
    private TableView<Category> categoryTable;
    @FXML
    private TableColumn<Produit, Number> colId;
    @FXML
    private TableColumn<Produit, String> colNom;
    @FXML
    private TableColumn<Produit, String> colDescription;
    @FXML
    private TableColumn<Produit, Number> colPrix;
    @FXML
    private TableColumn<Produit, Number> colStock;
    @FXML
    private TableColumn<Produit, String> colCategorie;
    @FXML
    private TableColumn<Category, Number> categoryColId;
    @FXML
    private TableColumn<Category, String> categoryColNom;
    @FXML
    private TableColumn<Category, String> categoryColDescription;
    @FXML
    private Label pageTitle;
    @FXML
    private Label formTitle;
    @FXML
    private Button sidebarListButton;
    @FXML
    private Button sidebarFormButton;
    @FXML
    private Button sidebarCategoryListButton;
    @FXML
    private Button sidebarCategoryFormButton;
    @FXML
    private Button frontOfficeToggleButton;
    @FXML
    private Button backOfficeToggleButton;
    @FXML
    private Button frontAccueilNavButton;
    @FXML
    private Button frontProduitsNavButton;
    @FXML
    private Button frontCategoriesNavButton;
    @FXML
    private Label resultCountLabel;
    @FXML
    private Label categoryResultCountLabel;
    @FXML
    private Label categoryFormTitle;
    @FXML
    private Label frontResultCountLabel;
    @FXML
    private FlowPane frontProductContainer;
    @FXML
    private Label frontDetailTitle;
    @FXML
    private Label frontDetailCategory;
    @FXML
    private Label frontDetailPrice;
    @FXML
    private Label frontDetailStock;
    @FXML
    private Label frontDetailDescription;
    @FXML
    private StackPane frontDetailImageContainer;
    @FXML
    private TextField frontCategorySearchField;
    @FXML
    private ComboBox<String> frontCategorySortCombo;
    @FXML
    private Label frontCategoryResultCountLabel;
    @FXML
    private FlowPane frontCategoryContainer;
    @FXML
    private Label frontCategoryDetailTitle;
    @FXML
    private Label frontCategoryDetailDescription;
    @FXML
    private Label frontCategoryDetailCount;
    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private final ObservableList<Produit> produits = FXCollections.observableArrayList();
    private final CategoryService categoryService = new CategoryService();
    private final ProduitService produitService = new ProduitService();
    private final FilteredList<Produit> filteredProduits = new FilteredList<>(produits, produit -> true);
    private final FilteredList<Category> filteredCategories = new FilteredList<>(categories, category -> true);
    private Produit selectedProduit;
    private Category selectedCategory;
    private Produit selectedFrontProduit;
    private Category selectedFrontCategory;

    @FXML
    public void initialize() {
        configureTable();
        configureCategoryTable();
        configureSearchAndSort();
        loadData();
        showFrontOffice();
    }

    @FXML
    private void showFrontOffice() {
        frontOfficeView.setVisible(true);
        frontOfficeView.setManaged(true);
        backOfficeView.setVisible(false);
        backOfficeView.setManaged(false);
        frontOfficeToggleButton.getStyleClass().setAll("primary-button");
        backOfficeToggleButton.getStyleClass().setAll("ghost-button");
        showFrontHome();
        renderFrontOfficeProducts();
        renderFrontCategories();
    }

    @FXML
    private void showBackOffice() {
        frontOfficeView.setVisible(false);
        frontOfficeView.setManaged(false);
        backOfficeView.setVisible(true);
        backOfficeView.setManaged(true);
        frontOfficeToggleButton.getStyleClass().setAll("ghost-button");
        backOfficeToggleButton.getStyleClass().setAll("primary-button");
        showProduitList();
    }

    @FXML
    private void showProduitList() {
        pageTitle.setText("Liste des produits");
        produitListView.setVisible(true);
        produitListView.setManaged(true);
        produitFormView.setVisible(false);
        produitFormView.setManaged(false);
        categoryListView.setVisible(false);
        categoryListView.setManaged(false);
        categoryFormView.setVisible(false);
        categoryFormView.setManaged(false);
        clearProduitSidebar();
        clearCategorySidebar();
        sidebarListButton.getStyleClass().setAll("menu-button", "active-menu-button");
    }

    @FXML
    private void showProduitForm() {
        selectedProduit = null;
        formTitle.setText("Creer ou modifier un produit");
        pageTitle.setText("Fiche produit");
        produitListView.setVisible(false);
        produitListView.setManaged(false);
        produitFormView.setVisible(true);
        produitFormView.setManaged(true);
        categoryListView.setVisible(false);
        categoryListView.setManaged(false);
        categoryFormView.setVisible(false);
        categoryFormView.setManaged(false);
        clearForm();
        clearProduitSidebar();
        clearCategorySidebar();
        sidebarFormButton.getStyleClass().setAll("menu-button", "active-menu-button");
    }

    @FXML
    private void showCategoryList() {
        pageTitle.setText("Liste des categories");
        produitListView.setVisible(false);
        produitListView.setManaged(false);
        produitFormView.setVisible(false);
        produitFormView.setManaged(false);
        categoryListView.setVisible(true);
        categoryListView.setManaged(true);
        categoryFormView.setVisible(false);
        categoryFormView.setManaged(false);
        clearProduitSidebar();
        clearCategorySidebar();
        sidebarCategoryListButton.getStyleClass().setAll("menu-button", "active-menu-button");
    }

    @FXML
    private void showCategoryForm() {
        selectedCategory = null;
        categoryFormTitle.setText("Creer ou modifier une categorie");
        pageTitle.setText("Fiche categorie");
        produitListView.setVisible(false);
        produitListView.setManaged(false);
        produitFormView.setVisible(false);
        produitFormView.setManaged(false);
        categoryListView.setVisible(false);
        categoryListView.setManaged(false);
        categoryFormView.setVisible(true);
        categoryFormView.setManaged(true);
        clearCategoryForm();
        clearProduitSidebar();
        clearCategorySidebar();
        sidebarCategoryFormButton.getStyleClass().setAll("menu-button", "active-menu-button");
    }

    @FXML
    private void saveProduit() {
        try {
            if (selectedProduit == null) {
                produitService.ajouterProduit(
                        nomField.getText(),
                        descriptionArea.getText(),
                        imageUrlField.getText(),
                        prixField.getText(),
                        stockField.getText(),
                        categoryCombo.getValue()
                );
                showAlert(Alert.AlertType.INFORMATION, "Produit ajoute", "Le produit a ete enregistre avec succes.");
            } else {
                produitService.modifierProduit(
                        selectedProduit.getId(),
                        nomField.getText(),
                        descriptionArea.getText(),
                        imageUrlField.getText(),
                        prixField.getText(),
                        stockField.getText(),
                        categoryCombo.getValue()
                );
                showAlert(Alert.AlertType.INFORMATION, "Produit modifie", "Le produit a ete modifie avec succes.");
            }
            loadData();
            clearForm();
            showProduitList();
        } catch (ValidationException exception) {
            showAlert(Alert.AlertType.ERROR, "Validation", exception.getMessage());
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Erreur base de donnees", exception.getMessage());
        }
    }

    @FXML
    private void saveCategory() {
        try {
            if (selectedCategory == null) {
                categoryService.ajouterCategory(categoryNomField.getText(), categoryDescriptionArea.getText());
                showAlert(Alert.AlertType.INFORMATION, "Categorie ajoutee", "La categorie a ete enregistree avec succes.");
            } else {
                categoryService.modifierCategory(selectedCategory.getId(), categoryNomField.getText(), categoryDescriptionArea.getText());
                showAlert(Alert.AlertType.INFORMATION, "Categorie modifiee", "La categorie a ete modifiee avec succes.");
            }
            loadData();
            clearCategoryForm();
            showCategoryList();
        } catch (ValidationException exception) {
            showAlert(Alert.AlertType.ERROR, "Validation", exception.getMessage());
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Erreur base de donnees", exception.getMessage());
        }
    }

    @FXML
    private void clearForm() {
        selectedProduit = null;
        formTitle.setText("Creer ou modifier un produit");
        nomField.clear();
        descriptionArea.clear();
        imageUrlField.clear();
        prixField.clear();
        stockField.clear();
        categoryCombo.getSelectionModel().clearSelection();
    }

    @FXML
    private void clearCategoryForm() {
        selectedCategory = null;
        categoryFormTitle.setText("Creer ou modifier une categorie");
        categoryNomField.clear();
        categoryDescriptionArea.clear();
    }

    @FXML
    private void resetProduitFilters() {
        produitSearchField.clear();
        produitSortCombo.setValue("ID decroissant");
        applyProduitFilterAndSort();
    }

    @FXML
    private void resetCategoryFilters() {
        categorySearchField.clear();
        categorySortCombo.setValue("Nom A-Z");
        applyCategoryFilterAndSort();
    }

    @FXML
    private void resetFrontFilters() {
        frontSearchField.clear();
        frontSortCombo.setValue("Plus recent");
        showFrontList();
        renderFrontOfficeProducts();
    }

    @FXML
    private void resetFrontCategoryFilters() {
        frontCategorySearchField.clear();
        frontCategorySortCombo.setValue("Nom A-Z");
        showFrontCategoryList();
        renderFrontCategories();
    }

    @FXML
    private void showFrontList() {
        frontHomeSection.setVisible(false);
        frontHomeSection.setManaged(false);
        frontListSection.setVisible(true);
        frontListSection.setManaged(true);
        frontDetailSection.setVisible(false);
        frontDetailSection.setManaged(false);
        frontCategoryListSection.setVisible(false);
        frontCategoryListSection.setManaged(false);
        frontCategoryDetailSection.setVisible(false);
        frontCategoryDetailSection.setManaged(false);
        frontAccueilNavButton.getStyleClass().setAll("front-nav-button");
        frontProduitsNavButton.getStyleClass().setAll("front-nav-button", "active-front-nav-button");
        frontCategoriesNavButton.getStyleClass().setAll("front-nav-button");
    }

    @FXML
    private void showFrontCategoryList() {
        frontHomeSection.setVisible(false);
        frontHomeSection.setManaged(false);
        frontListSection.setVisible(false);
        frontListSection.setManaged(false);
        frontDetailSection.setVisible(false);
        frontDetailSection.setManaged(false);
        frontCategoryListSection.setVisible(true);
        frontCategoryListSection.setManaged(true);
        frontCategoryDetailSection.setVisible(false);
        frontCategoryDetailSection.setManaged(false);
        frontAccueilNavButton.getStyleClass().setAll("front-nav-button");
        frontProduitsNavButton.getStyleClass().setAll("front-nav-button");
        frontCategoriesNavButton.getStyleClass().setAll("front-nav-button", "active-front-nav-button");
    }

    @FXML
    private void showFrontHome() {
        frontHomeSection.setVisible(true);
        frontHomeSection.setManaged(true);
        frontListSection.setVisible(false);
        frontListSection.setManaged(false);
        frontDetailSection.setVisible(false);
        frontDetailSection.setManaged(false);
        frontCategoryListSection.setVisible(false);
        frontCategoryListSection.setManaged(false);
        frontCategoryDetailSection.setVisible(false);
        frontCategoryDetailSection.setManaged(false);
        frontAccueilNavButton.getStyleClass().setAll("front-nav-button", "active-front-nav-button");
        frontProduitsNavButton.getStyleClass().setAll("front-nav-button");
        frontCategoriesNavButton.getStyleClass().setAll("front-nav-button");
    }

    @FXML
    private void editSelectedProduit() {
        Produit produit = produitTable.getSelectionModel().getSelectedItem();
        if (produit == null) {
            showAlert(Alert.AlertType.WARNING, "Selection requise", "Veuillez selectionner un produit a modifier.");
            return;
        }

        selectedProduit = produit;
        formTitle.setText("Modifier le produit");
        nomField.setText(produit.getNom());
        descriptionArea.setText(produit.getDescription());
        imageUrlField.setText(produit.getImageUrl());
        prixField.setText(String.valueOf(produit.getPrix()));
        stockField.setText(String.valueOf(produit.getStock()));
        categoryCombo.setValue(produit.getCategory());

        produitListView.setVisible(false);
        produitListView.setManaged(false);
        produitFormView.setVisible(true);
        produitFormView.setManaged(true);
        clearProduitSidebar();
        clearCategorySidebar();
        sidebarFormButton.getStyleClass().setAll("menu-button", "active-menu-button");
    }

    @FXML
    private void deleteSelectedProduit() {
        Produit produit = produitTable.getSelectionModel().getSelectedItem();
        if (produit == null) {
            showAlert(Alert.AlertType.WARNING, "Selection requise", "Veuillez selectionner un produit a supprimer.");
            return;
        }

        try {
            produitService.supprimerProduit(produit.getId());
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Produit supprime", "Le produit a ete supprime avec succes.");
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Erreur base de donnees", exception.getMessage());
        }
    }

    @FXML
    private void editSelectedCategory() {
        Category category = categoryTable.getSelectionModel().getSelectedItem();
        if (category == null) {
            showAlert(Alert.AlertType.WARNING, "Selection requise", "Veuillez selectionner une categorie a modifier.");
            return;
        }

        selectedCategory = category;
        categoryFormTitle.setText("Modifier la categorie");
        categoryNomField.setText(category.getNom());
        categoryDescriptionArea.setText(category.getDescription());

        categoryListView.setVisible(false);
        categoryListView.setManaged(false);
        categoryFormView.setVisible(true);
        categoryFormView.setManaged(true);
        clearProduitSidebar();
        clearCategorySidebar();
        sidebarCategoryFormButton.getStyleClass().setAll("menu-button", "active-menu-button");
    }

    @FXML
    private void deleteSelectedCategory() {
        Category category = categoryTable.getSelectionModel().getSelectedItem();
        if (category == null) {
            showAlert(Alert.AlertType.WARNING, "Selection requise", "Veuillez selectionner une categorie a supprimer.");
            return;
        }

        try {
            categoryService.supprimerCategory(category.getId());
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Categorie supprimee", "La categorie a ete supprimee avec succes.");
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Erreur base de donnees", "Suppression impossible. Verifiez si des produits utilisent encore cette categorie.\n\n" + exception.getMessage());
        }
    }

    @FXML
    private void chooseImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image produit");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File selectedFile = fileChooser.showOpenDialog(contentArea.getScene().getWindow());
        if (selectedFile != null) {
            imageUrlField.setText(selectedFile.getAbsolutePath());
        }
    }

    private void configureTable() {
        colId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()));
        colNom.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNom()));
        colDescription.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
        colPrix.setCellValueFactory(data -> new javafx.beans.property.SimpleDoubleProperty(data.getValue().getPrix()));
        colStock.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getStock()));
        colCategorie.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCategory().getNom()));
        SortedList<Produit> sortedProduits = new SortedList<>(filteredProduits);
        sortedProduits.comparatorProperty().bind(produitTable.comparatorProperty());
        produitTable.setItems(sortedProduits);
    }

    private void configureCategoryTable() {
        categoryColId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()));
        categoryColNom.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getNom()));
        categoryColDescription.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getDescription()));
        SortedList<Category> sortedCategories = new SortedList<>(filteredCategories);
        sortedCategories.comparatorProperty().bind(categoryTable.comparatorProperty());
        categoryTable.setItems(sortedCategories);
    }

    private void configureSearchAndSort() {
        produitSortCombo.setItems(FXCollections.observableArrayList(
                "ID decroissant",
                "Nom A-Z",
                "Nom Z-A",
                "Prix croissant",
                "Prix decroissant",
                "Stock croissant",
                "Stock decroissant"
        ));
        produitSortCombo.setValue("ID decroissant");
        produitSearchField.textProperty().addListener((observable, oldValue, newValue) -> applyProduitFilterAndSort());
        produitSortCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyProduitFilterAndSort());

        frontSortCombo.setItems(FXCollections.observableArrayList(
                "Plus recent",
                "Nom A-Z",
                "Nom Z-A",
                "Prix croissant",
                "Prix decroissant"
        ));
        frontSortCombo.setValue("Plus recent");
        frontSearchField.textProperty().addListener((observable, oldValue, newValue) -> renderFrontOfficeProducts());
        frontSortCombo.valueProperty().addListener((observable, oldValue, newValue) -> renderFrontOfficeProducts());

        frontCategorySortCombo.setItems(FXCollections.observableArrayList(
                "Nom A-Z",
                "Nom Z-A",
                "Plus de produits"
        ));
        frontCategorySortCombo.setValue("Nom A-Z");
        frontCategorySearchField.textProperty().addListener((observable, oldValue, newValue) -> renderFrontCategories());
        frontCategorySortCombo.valueProperty().addListener((observable, oldValue, newValue) -> renderFrontCategories());

        categorySortCombo.setItems(FXCollections.observableArrayList(
                "Nom A-Z",
                "Nom Z-A",
                "ID croissant",
                "ID decroissant"
        ));
        categorySortCombo.setValue("Nom A-Z");
        categorySearchField.textProperty().addListener((observable, oldValue, newValue) -> applyCategoryFilterAndSort());
        categorySortCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyCategoryFilterAndSort());
    }

    private void applyProduitFilterAndSort() {
        String search = produitSearchField.getText() == null ? "" : produitSearchField.getText().trim().toLowerCase();
        filteredProduits.setPredicate(produit -> {
            if (search.isEmpty()) {
                return true;
            }

            return produit.getNom().toLowerCase().contains(search)
                    || produit.getDescription().toLowerCase().contains(search)
                    || produit.getCategory().getNom().toLowerCase().contains(search);
        });

        String sort = produitSortCombo.getValue();
        if ("Nom A-Z".equals(sort)) {
            produits.sort((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()));
        } else if ("Nom Z-A".equals(sort)) {
            produits.sort((a, b) -> b.getNom().compareToIgnoreCase(a.getNom()));
        } else if ("Prix croissant".equals(sort)) {
            produits.sort((a, b) -> Double.compare(a.getPrix(), b.getPrix()));
        } else if ("Prix decroissant".equals(sort)) {
            produits.sort((a, b) -> Double.compare(b.getPrix(), a.getPrix()));
        } else if ("Stock croissant".equals(sort)) {
            produits.sort((a, b) -> Integer.compare(a.getStock(), b.getStock()));
        } else if ("Stock decroissant".equals(sort)) {
            produits.sort((a, b) -> Integer.compare(b.getStock(), a.getStock()));
        } else {
            produits.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        }

        resultCountLabel.setText(filteredProduits.size() + " resultat(s)");
    }

    private void applyCategoryFilterAndSort() {
        String search = categorySearchField.getText() == null ? "" : categorySearchField.getText().trim().toLowerCase();
        filteredCategories.setPredicate(category -> {
            if (search.isEmpty()) {
                return true;
            }

            return category.getNom().toLowerCase().contains(search)
                    || category.getDescription().toLowerCase().contains(search);
        });

        String sort = categorySortCombo.getValue();
        if ("Nom Z-A".equals(sort)) {
            categories.sort((a, b) -> b.getNom().compareToIgnoreCase(a.getNom()));
        } else if ("ID croissant".equals(sort)) {
            categories.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        } else if ("ID decroissant".equals(sort)) {
            categories.sort((a, b) -> Integer.compare(b.getId(), a.getId()));
        } else {
            categories.sort((a, b) -> a.getNom().compareToIgnoreCase(b.getNom()));
        }

        categoryResultCountLabel.setText(filteredCategories.size() + " resultat(s)");
    }

    private void renderFrontOfficeProducts() {
        if (frontProductContainer == null) {
            return;
        }

        String search = frontSearchField.getText() == null ? "" : frontSearchField.getText().trim().toLowerCase();
        List<Produit> displayList = new ArrayList<>();

        for (Produit produit : produits) {
            if (search.isEmpty()
                    || produit.getNom().toLowerCase().contains(search)
                    || produit.getDescription().toLowerCase().contains(search)
                    || produit.getCategory().getNom().toLowerCase().contains(search)) {
                displayList.add(produit);
            }
        }

        String sort = frontSortCombo.getValue();
        if ("Nom A-Z".equals(sort)) {
            displayList.sort(Comparator.comparing(Produit::getNom, String.CASE_INSENSITIVE_ORDER));
        } else if ("Nom Z-A".equals(sort)) {
            displayList.sort(Comparator.comparing(Produit::getNom, String.CASE_INSENSITIVE_ORDER).reversed());
        } else if ("Prix croissant".equals(sort)) {
            displayList.sort(Comparator.comparingDouble(Produit::getPrix));
        } else if ("Prix decroissant".equals(sort)) {
            displayList.sort(Comparator.comparingDouble(Produit::getPrix).reversed());
        } else {
            displayList.sort(Comparator.comparingInt(Produit::getId).reversed());
        }

        frontProductContainer.getChildren().clear();
        for (Produit produit : displayList) {
            frontProductContainer.getChildren().add(createFrontProduitCard(produit));
        }
        frontResultCountLabel.setText(displayList.size() + " produit(s)");
    }

    private Node createFrontProduitCard(Produit produit) {
        VBox card = new VBox(12);
        card.getStyleClass().add("front-card");
        card.setPrefWidth(280);

        StackPane imageHolder = new StackPane();
        imageHolder.getStyleClass().add("front-card-image-holder");
        imageHolder.setPrefSize(248, 180);
        imageHolder.getChildren().add(createImageNode(produit.getImageUrl()));

        Label categoryBadge = new Label(produit.getCategory().getNom());
        categoryBadge.getStyleClass().add("front-category-badge");

        Label title = new Label(produit.getNom());
        title.getStyleClass().add("front-card-title");
        title.setWrapText(true);

        Label description = new Label(produit.getDescription());
        description.getStyleClass().add("front-card-description");
        description.setWrapText(true);

        Label price = new Label(String.format("%.2f DT", produit.getPrix()));
        price.getStyleClass().add("front-price-badge");

        Label stock = new Label("Stock: " + produit.getStock());
        stock.getStyleClass().add("front-stock-badge");

        VBox metaBox = new VBox(10, price, stock);
        Button button = new Button("Voir details");
        button.getStyleClass().add("primary-button");
        button.setOnAction(event -> showFrontProduitDetails(produit));

        card.getChildren().addAll(imageHolder, categoryBadge, title, description, metaBox, button);
        return card;
    }

    private void showFrontProduitDetails(Produit produit) {
        selectedFrontProduit = produit;
        frontDetailTitle.setText(produit.getNom());
        frontDetailCategory.setText(produit.getCategory().getNom());
        frontDetailPrice.setText(String.format("%.2f DT", produit.getPrix()));
        frontDetailStock.setText("Stock disponible: " + produit.getStock());
        frontDetailDescription.setText(produit.getDescription());
        frontDetailImageContainer.getChildren().setAll(createImageNode(produit.getImageUrl()));

        frontListSection.setVisible(false);
        frontListSection.setManaged(false);
        frontDetailSection.setVisible(true);
        frontDetailSection.setManaged(true);
        frontHomeSection.setVisible(false);
        frontHomeSection.setManaged(false);
    }

    private void showFrontCategoryDetails(Category category) {
        selectedFrontCategory = category;
        frontCategoryDetailTitle.setText(category.getNom());
        frontCategoryDetailDescription.setText(category.getDescription());
        long count = produits.stream().filter(produit -> produit.getCategory().getId() == category.getId()).count();
        frontCategoryDetailCount.setText(count + " produit(s) dans cette categorie");

        frontCategoryListSection.setVisible(false);
        frontCategoryListSection.setManaged(false);
        frontCategoryDetailSection.setVisible(true);
        frontCategoryDetailSection.setManaged(true);
        frontHomeSection.setVisible(false);
        frontHomeSection.setManaged(false);
    }

    private Node createImageNode(String imagePath) {
        try {
            if (imagePath != null && !imagePath.isBlank()) {
                File file = new File(imagePath);
                if (file.exists()) {
                    ImageView imageView = new ImageView(new Image(file.toURI().toString(), 248, 180, false, true));
                    imageView.setFitWidth(248);
                    imageView.setFitHeight(180);
                    imageView.setPreserveRatio(false);
                    return imageView;
                }
            }
        } catch (Exception ignored) {
        }

        VBox placeholder = new VBox(8);
        placeholder.setAlignment(Pos.CENTER);
        placeholder.getStyleClass().add("front-image-placeholder");
        Label title = new Label("Image");
        title.getStyleClass().add("front-image-placeholder-title");
        Label subtitle = new Label("Apercu indisponible");
        subtitle.getStyleClass().add("front-image-placeholder-text");
        placeholder.getChildren().addAll(title, subtitle);
        return placeholder;
    }

    private void renderFrontCategories() {
        if (frontCategoryContainer == null) {
            return;
        }

        String search = frontCategorySearchField.getText() == null ? "" : frontCategorySearchField.getText().trim().toLowerCase();
        List<Category> displayList = new ArrayList<>();
        for (Category category : categories) {
            if (search.isEmpty()
                    || category.getNom().toLowerCase().contains(search)
                    || category.getDescription().toLowerCase().contains(search)) {
                displayList.add(category);
            }
        }

        String sort = frontCategorySortCombo.getValue();
        if ("Nom Z-A".equals(sort)) {
            displayList.sort((a, b) -> b.getNom().compareToIgnoreCase(a.getNom()));
        } else if ("Plus de produits".equals(sort)) {
            displayList.sort((a, b) -> Long.compare(countProduitsForCategory(b), countProduitsForCategory(a)));
        } else {
            displayList.sort(Comparator.comparing(Category::getNom, String.CASE_INSENSITIVE_ORDER));
        }

        frontCategoryContainer.getChildren().clear();
        for (Category category : displayList) {
            frontCategoryContainer.getChildren().add(createFrontCategoryCard(category));
        }
        frontCategoryResultCountLabel.setText(displayList.size() + " categorie(s)");
    }

    private Node createFrontCategoryCard(Category category) {
        VBox card = new VBox(12);
        card.getStyleClass().add("front-card");
        card.setPrefWidth(300);

        Label badge = new Label("Categorie");
        badge.getStyleClass().add("front-category-badge");

        Label title = new Label(category.getNom());
        title.getStyleClass().add("front-card-title");
        title.setWrapText(true);

        Label description = new Label(category.getDescription());
        description.getStyleClass().add("front-card-description");
        description.setWrapText(true);

        Label count = new Label(countProduitsForCategory(category) + " produit(s)");
        count.getStyleClass().add("front-stock-badge");

        Button button = new Button("Voir details");
        button.getStyleClass().add("primary-button");
        button.setOnAction(event -> showFrontCategoryDetails(category));

        card.getChildren().addAll(badge, title, description, count, button);
        return card;
    }

    private long countProduitsForCategory(Category category) {
        return produits.stream().filter(produit -> produit.getCategory().getId() == category.getId()).count();
    }

    private void loadData() {
        try {
            categories.setAll(categoryService.getAllCategories());
            produits.setAll(produitService.getAllProduits());
            categoryCombo.setItems(categories);
            applyProduitFilterAndSort();
            applyCategoryFilterAndSort();
            renderFrontOfficeProducts();
            renderFrontCategories();
        } catch (SQLException exception) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Connexion MySQL",
                    "Impossible de charger les donnees. Verifiez la base 'javafx' et vos identifiants.\n\n" + exception.getMessage()
            );
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void clearProduitSidebar() {
        sidebarListButton.getStyleClass().setAll("menu-button");
        sidebarFormButton.getStyleClass().setAll("menu-button");
    }

    private void clearCategorySidebar() {
        sidebarCategoryListButton.getStyleClass().setAll("menu-button");
        sidebarCategoryFormButton.getStyleClass().setAll("menu-button");
    }
}
