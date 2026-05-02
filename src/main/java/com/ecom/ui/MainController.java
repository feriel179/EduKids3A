package com.ecom.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import com.ecom.model.Category;
import com.ecom.model.Commande;
import com.ecom.model.CommandeStatut;
import com.ecom.model.Produit;
import com.ecom.service.CategoryService;
import com.ecom.service.ChatbotService;
import com.ecom.service.CommandeCreationResult;
import com.ecom.service.CommandeService;
import com.ecom.service.MailService;
import com.ecom.service.ProductAiService;
import com.ecom.service.ProduitService;
import com.ecom.validation.ValidationException;
import jakarta.mail.MessagingException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MainController {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Duration VIEW_FADE_DURATION = Duration.millis(280);
    private static final Duration VIEW_SLIDE_DURATION = Duration.millis(280);
    private static final Duration ITEM_FADE_DURATION = Duration.millis(260);

    @FXML private StackPane contentArea;
    @FXML private VBox frontOfficeView;
    @FXML private BorderPane backOfficeView;
    @FXML private VBox frontHomeSection;
    @FXML private VBox frontListSection;
    @FXML private VBox chatbotPopup;
    @FXML private VBox frontDetailSection;
    @FXML private VBox frontCategoryListSection;
    @FXML private VBox frontCategoryDetailSection;
    @FXML private VBox frontCommandeSection;
    @FXML private VBox produitListView;
    @FXML private VBox produitFormView;
    @FXML private VBox categoryListView;
    @FXML private VBox categoryFormView;
    @FXML private VBox commandeListView;
    @FXML private TextField nomField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField imageUrlField;
    @FXML private TextField prixField;
    @FXML private TextField stockField;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private TextField produitSearchField;
    @FXML private ComboBox<String> produitSortCombo;
    @FXML private TextField frontSearchField;
    @FXML private ComboBox<String> frontSortCombo;
    @FXML private TextField categoryNomField;
    @FXML private TextArea categoryDescriptionArea;
    @FXML private TextField categorySearchField;
    @FXML private ComboBox<String> categorySortCombo;
    @FXML private TextField commandeQuantiteField;
    @FXML private TextField commandePromoCodeField;
    @FXML private TextArea commandeCommentaireArea;
    @FXML private ComboBox<String> commandeStatusFilterCombo;
    @FXML private ComboBox<String> frontCommandeStatusFilterCombo;
    @FXML private Label commandeClientFeedbackLabel;
    @FXML private Label commandePromoFeedbackLabel;
    @FXML private Label frontPendingOrdersLabel;
    @FXML private Label frontValidatedOrdersLabel;
    @FXML private Label adminPendingOrdersLabel;
    @FXML private Label adminValidatedRevenueLabel;
    @FXML private Label adminLowStockLabel;
    @FXML private Button submitCommandeButton;
    @FXML private Button cancelCommandeButton;
    @FXML private Button generateDescriptionButton;
    @FXML private Button generateImageButton;
    @FXML private TableView<Produit> produitTable;
    @FXML private TableView<Category> categoryTable;
    @FXML private TableView<Commande> commandeTable;
    @FXML private TableView<Commande> frontCommandeTable;
    @FXML private TableColumn<Produit, Number> colId;
    @FXML private TableColumn<Produit, String> colNom;
    @FXML private TableColumn<Produit, String> colDescription;
    @FXML private TableColumn<Produit, Number> colPrix;
    @FXML private TableColumn<Produit, Number> colStock;
    @FXML private TableColumn<Produit, String> colCategorie;
    @FXML private TableColumn<Category, Number> categoryColId;
    @FXML private TableColumn<Category, String> categoryColNom;
    @FXML private TableColumn<Category, String> categoryColDescription;
    @FXML private TableColumn<Commande, Number> commandeColId;
    @FXML private TableColumn<Commande, String> commandeColProduit;
    @FXML private TableColumn<Commande, String> commandeColClient;
    @FXML private TableColumn<Commande, Number> commandeColQuantite;
    @FXML private TableColumn<Commande, String> commandeColTotalAvant;
    @FXML private TableColumn<Commande, String> commandeColTotal;
    @FXML private TableColumn<Commande, String> commandeColStatut;
    @FXML private TableColumn<Commande, String> commandeColDate;
    @FXML private TableColumn<Commande, Number> frontCommandeColId;
    @FXML private TableColumn<Commande, String> frontCommandeColProduit;
    @FXML private TableColumn<Commande, Number> frontCommandeColQuantite;
    @FXML private TableColumn<Commande, String> frontCommandeColTotalAvant;
    @FXML private TableColumn<Commande, String> frontCommandeColTotal;
    @FXML private TableColumn<Commande, String> frontCommandeColStatut;
    @FXML private TableColumn<Commande, String> frontCommandeColDate;
    @FXML private Label pageTitle;
    @FXML private Label formTitle;
    @FXML private Label categoryFormTitle;
    @FXML private Label sessionModeLabel;
    @FXML private Button sidebarListButton;
    @FXML private Button sidebarFormButton;
    @FXML private Button sidebarCategoryListButton;
    @FXML private Button sidebarCategoryFormButton;
    @FXML private Button sidebarCommandeListButton;
    @FXML private Button frontOfficeToggleButton;
    @FXML private Button backOfficeToggleButton;
    @FXML private Button frontAccueilNavButton;
    @FXML private Button frontProduitsNavButton;
    @FXML private Button frontCategoriesNavButton;
    @FXML private Button frontCommandesNavButton;
    @FXML private Button chatbotFloatingButton;
    @FXML private Label resultCountLabel;
    @FXML private Label categoryResultCountLabel;
    @FXML private Label commandeResultCountLabel;
    @FXML private Label frontResultCountLabel;
    @FXML private Label frontCommandeResultCountLabel;
    @FXML private FlowPane frontProductContainer;
    @FXML private Label frontDetailTitle;
    @FXML private Label frontDetailCategory;
    @FXML private Label frontDetailPrice;
    @FXML private Label frontDetailStock;
    @FXML private Label frontDetailDescription;
    @FXML private StackPane frontDetailImageContainer;
    @FXML private TextField frontCategorySearchField;
    @FXML private ComboBox<String> frontCategorySortCombo;
    @FXML private Label frontCategoryResultCountLabel;
    @FXML private FlowPane frontCategoryContainer;
    @FXML private Label frontCategoryDetailTitle;
    @FXML private Label frontCategoryDetailDescription;
    @FXML private Label frontCategoryDetailCount;
    @FXML private ScrollPane chatbotMessagesScroll;
    @FXML private VBox chatbotMessagesBox;
    @FXML private TextField chatbotInputField;
    @FXML private Button chatbotSendButton;

    private final ObservableList<Category> categories = FXCollections.observableArrayList();
    private final ObservableList<Produit> produits = FXCollections.observableArrayList();
    private final ObservableList<Commande> commandes = FXCollections.observableArrayList();
    private final ObservableList<Commande> frontCommandes = FXCollections.observableArrayList();
    private final CategoryService categoryService = new CategoryService();
    private final ProduitService produitService = new ProduitService();
    private final CommandeService commandeService = new CommandeService();
    private final MailService mailService = new MailService();
    private final ProductAiService productAiService = new ProductAiService();
    private final ChatbotService chatbotService = new ChatbotService();
    private final FilteredList<Produit> filteredProduits = new FilteredList<>(produits, produit -> true);
    private final FilteredList<Category> filteredCategories = new FilteredList<>(categories, category -> true);
    private final FilteredList<Commande> filteredCommandes = new FilteredList<>(commandes, commande -> true);
    private final FilteredList<Commande> filteredFrontCommandes = new FilteredList<>(frontCommandes, commande -> true);

    private Produit selectedProduit;
    private Category selectedCategory;
    private Produit selectedFrontProduit;
    private boolean uiAnimationsInitialized;

    @FXML
    public void initialize() {
        configureProduitTable();
        configureCategoryTable();
        configureCommandeTables();
        configureCategoryCombo();
        configureSearchAndSort();
        applyStandaloneAccess();
        loadData();
        showFrontOffice();
        if (chatbotMessagesBox != null) {
            chatbotMessagesBox.getChildren().clear();
            appendChatMessage(false, "Bonjour, je peux vous aider pour les produits, commandes et categories.");
        }
        Platform.runLater(this::initializeAnimations);
    }

    @FXML
    private void showFrontOffice() {
        frontOfficeView.setVisible(true);
        frontOfficeView.setManaged(true);
        backOfficeView.setVisible(false);
        backOfficeView.setManaged(false);
        chatbotFloatingButton.setVisible(true);
        chatbotFloatingButton.setManaged(true);
        hideChatbotPopup();
        backOfficeView.setOpacity(1);
        backOfficeView.setTranslateY(0);
        frontOfficeToggleButton.getStyleClass().setAll("primary-button");
        backOfficeToggleButton.getStyleClass().setAll("ghost-button");
        showFrontHome();
        renderFrontOfficeProducts();
        renderFrontCategories();
        refreshFrontCommandes();
        animateViewEntrance(frontOfficeView);
    }

    @FXML
    private void showBackOffice() {
        frontOfficeView.setVisible(false);
        frontOfficeView.setManaged(false);
        backOfficeView.setVisible(true);
        backOfficeView.setManaged(true);
        chatbotFloatingButton.setVisible(false);
        chatbotFloatingButton.setManaged(false);
        hideChatbotPopup();
        frontOfficeView.setOpacity(1);
        frontOfficeView.setTranslateY(0);
        frontOfficeToggleButton.getStyleClass().setAll("ghost-button");
        backOfficeToggleButton.getStyleClass().setAll("primary-button");
        showCommandeList();
        animateViewEntrance(backOfficeView);
    }

    public void openStudentMode() {
        setModeSwitchingVisible(false);
        showFrontOffice();
    }

    public void openAdminMode() {
        setModeSwitchingVisible(false);
        showBackOffice();
        if (backOfficeView != null) {
            backOfficeView.setLeft(null);
        }
    }

    private void setModeSwitchingVisible(boolean visible) {
        if (frontOfficeToggleButton != null) {
            frontOfficeToggleButton.setVisible(visible);
            frontOfficeToggleButton.setManaged(visible);
        }
        if (backOfficeToggleButton != null) {
            backOfficeToggleButton.setVisible(visible);
            backOfficeToggleButton.setManaged(visible);
        }
    }

    @FXML
    public void showProduitList() {
        setBackView(produitListView);
        clearProduitSidebar();
        clearCategorySidebar();
        clearCommandeSidebar();
        sidebarListButton.getStyleClass().setAll("menu-button", "active-menu-button");
    }

    @FXML
    public void showProduitForm() {
        selectedProduit = null;
        formTitle.setText("Creer ou modifier un produit");
        clearForm();
        setBackView(produitFormView);
        clearProduitSidebar();
        clearCategorySidebar();
        clearCommandeSidebar();
        sidebarFormButton.getStyleClass().setAll("menu-button", "active-menu-button");
    }

    @FXML
    public void showCategoryList() {
        setBackView(categoryListView);
        clearProduitSidebar();
        clearCategorySidebar();
        clearCommandeSidebar();
        sidebarCategoryListButton.getStyleClass().setAll("menu-button", "active-menu-button");
    }

    @FXML
    public void showCategoryForm() {
        selectedCategory = null;
        categoryFormTitle.setText("Creer ou modifier une categorie");
        clearCategoryForm();
        setBackView(categoryFormView);
        clearProduitSidebar();
        clearCategorySidebar();
        clearCommandeSidebar();
        sidebarCategoryFormButton.getStyleClass().setAll("menu-button", "active-menu-button");
    }

    @FXML
    public void showCommandeList() {
        pageTitle.setText("Liste des commandes");
        setBackView(commandeListView);
        clearProduitSidebar();
        clearCategorySidebar();
        clearCommandeSidebar();
        sidebarCommandeListButton.getStyleClass().setAll("menu-button", "active-menu-button");
    }

    @FXML
    private void showFrontList() {
        setFrontSection(frontListSection);
        frontAccueilNavButton.getStyleClass().setAll("front-nav-button");
        frontProduitsNavButton.getStyleClass().setAll("front-nav-button", "active-front-nav-button");
        frontCategoriesNavButton.getStyleClass().setAll("front-nav-button");
        frontCommandesNavButton.getStyleClass().setAll("front-nav-button");
    }

    @FXML
    private void showFrontCategoryList() {
        setFrontSection(frontCategoryListSection);
        frontAccueilNavButton.getStyleClass().setAll("front-nav-button");
        frontProduitsNavButton.getStyleClass().setAll("front-nav-button");
        frontCategoriesNavButton.getStyleClass().setAll("front-nav-button", "active-front-nav-button");
        frontCommandesNavButton.getStyleClass().setAll("front-nav-button");
    }

    @FXML
    private void showFrontHome() {
        setFrontSection(frontHomeSection);
        frontAccueilNavButton.getStyleClass().setAll("front-nav-button", "active-front-nav-button");
        frontProduitsNavButton.getStyleClass().setAll("front-nav-button");
        frontCategoriesNavButton.getStyleClass().setAll("front-nav-button");
        frontCommandesNavButton.getStyleClass().setAll("front-nav-button");
    }

    @FXML
    private void showFrontCommandes() {
        setFrontSection(frontCommandeSection);
        frontAccueilNavButton.getStyleClass().setAll("front-nav-button");
        frontProduitsNavButton.getStyleClass().setAll("front-nav-button");
        frontCategoriesNavButton.getStyleClass().setAll("front-nav-button");
        frontCommandesNavButton.getStyleClass().setAll("front-nav-button", "active-front-nav-button");
        refreshFrontCommandes();
    }

    @FXML
    private void showFrontChatbot() {
        boolean shouldShow = chatbotPopup == null || !chatbotPopup.isVisible();
        setChatbotPopupVisible(shouldShow);
    }

    @FXML
    private void hideChatbotPopup() {
        setChatbotPopupVisible(false);
    }

    private void setChatbotPopupVisible(boolean visible) {
        if (chatbotPopup == null) {
            return;
        }
        chatbotPopup.setVisible(visible);
        chatbotPopup.setManaged(visible);
        if (visible) {
            animateViewEntrance(chatbotPopup);
            if (chatbotInputField != null) {
                chatbotInputField.requestFocus();
            }
        }
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
    private void createCommande() {
        try {
            CommandeCreationResult result = commandeService.creerCommande(
                    selectedFrontProduit,
                    commandeQuantiteField.getText(),
                    commandeCommentaireArea.getText(),
                    commandePromoCodeField.getText()
            );

            StringBuilder feedback = new StringBuilder("Commande envoyee. Elle est en attente de validation.");
            if (result.remise() > 0) {
                feedback.append(String.format(
                        " Total: %.2f DT -> %.2f DT (remise: -%.2f DT via %s).",
                        result.montantInitial(),
                        result.montantFinal(),
                        result.remise(),
                        result.codePromoUtilise()
                ));
            } else {
                feedback.append(String.format(" Total: %.2f DT.", result.montantFinal()));
            }
            String nouveauCodePromoMessage = String.format(
                    "Nouveau code promo %s%% : %s",
                    formatPourcentage(result.nouveauCodePromoPourcentage()),
                    result.nouveauCodePromo()
            );

            commandeClientFeedbackLabel.setText(feedback.toString());
            showPromoFeedback(nouveauCodePromoMessage);
            showAlert(Alert.AlertType.INFORMATION, "Commande envoyee", feedback + "\n\n" + nouveauCodePromoMessage);
            clearCommandeForm();
            loadData();
            showFrontCommandes();
        } catch (ValidationException exception) {
            commandeClientFeedbackLabel.setText(exception.getMessage());
            hidePromoFeedback();
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Erreur base de donnees", exception.getMessage());
        }
    }

    @FXML
    private void validateSelectedCommande() {
        updateSelectedCommandeStatus(true);
    }

    @FXML
    private void refuseSelectedCommande() {
        updateSelectedCommandeStatus(false);
    }

    @FXML
    private void cancelSelectedCommande() {
        Commande commande = frontCommandeTable.getSelectionModel().getSelectedItem();
        if (commande == null) {
            showAlert(Alert.AlertType.WARNING, "Selection requise", "Veuillez selectionner une commande a annuler.");
            return;
        }
        if (commande.getStatut() != CommandeStatut.EN_ATTENTE) {
            showAlert(Alert.AlertType.WARNING, "Action impossible", "Seules les commandes en attente peuvent etre annulees.");
            return;
        }

        try {
            commandeService.annulerCommande(commande.getId());
            showAlert(Alert.AlertType.INFORMATION, "Commande annulee", "La commande a ete annulee avec succes.");
            loadData();
            showFrontCommandes();
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

        setBackView(produitFormView);
        clearProduitSidebar();
        clearCategorySidebar();
        clearCommandeSidebar();
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

        setBackView(categoryFormView);
        clearProduitSidebar();
        clearCategorySidebar();
        clearCommandeSidebar();
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

    @FXML
    private void generateDescriptionWithAi() {
        String productName = nomField.getText() == null ? "" : nomField.getText().trim();
        if (productName.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Nom requis", "Veuillez saisir le nom du produit d'abord.");
            return;
        }

        generateDescriptionButton.setDisable(true);
        generateDescriptionButton.setText("Generation...");

        String categoryName = categoryCombo.getValue() == null ? "" : categoryCombo.getValue().getNom();
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return productAiService.generateDescriptionFromName(productName, categoryName);
            }
        };

        task.setOnSucceeded(event -> {
            descriptionArea.setText(task.getValue());
            generateDescriptionButton.setDisable(false);
            generateDescriptionButton.setText("Generer description IA");
        });

        task.setOnFailed(event -> {
            generateDescriptionButton.setDisable(false);
            generateDescriptionButton.setText("Generer description IA");
            String message = task.getException() == null || task.getException().getMessage() == null
                    ? "Erreur inconnue lors de la generation de description."
                    : task.getException().getMessage();
            showAlert(Alert.AlertType.ERROR, "Generation description impossible", message);
        });

        Thread thread = new Thread(task, "ai-description-generation");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void generateImageWithAi() {
        String productName = nomField.getText() == null ? "" : nomField.getText().trim();
        if (productName.isBlank()) {
            showAlert(Alert.AlertType.WARNING, "Nom requis", "Veuillez saisir le nom du produit d'abord.");
            return;
        }

        generateImageButton.setDisable(true);
        generateImageButton.setText("Generation...");

        String categoryName = categoryCombo.getValue() == null ? "" : categoryCombo.getValue().getNom();
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return productAiService.generateImageFromName(productName, categoryName);
            }
        };

        task.setOnSucceeded(event -> {
            imageUrlField.setText(task.getValue());
            generateImageButton.setDisable(false);
            generateImageButton.setText("Generer image IA");
        });

        task.setOnFailed(event -> {
            generateImageButton.setDisable(false);
            generateImageButton.setText("Generer image IA");
            String message = task.getException() == null || task.getException().getMessage() == null
                    ? "Erreur inconnue lors de la generation d'image."
                    : task.getException().getMessage();
            showAlert(Alert.AlertType.ERROR, "Generation image impossible", message);
        });

        Thread thread = new Thread(task, "ai-image-generation");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void sendChatbotMessage() {
        String message = chatbotInputField == null || chatbotInputField.getText() == null
                ? ""
                : chatbotInputField.getText().trim();
        if (message.isBlank()) {
            return;
        }

        appendChatMessage(true, message);
        chatbotInputField.clear();
        chatbotSendButton.setDisable(true);
        chatbotSendButton.setText("...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return chatbotService.ask(message);
            }
        };

        task.setOnSucceeded(event -> {
            appendChatMessage(false, task.getValue());
            chatbotSendButton.setDisable(false);
            chatbotSendButton.setText("Envoyer");
        });

        task.setOnFailed(event -> {
            String error = task.getException() == null || task.getException().getMessage() == null
                    ? "Erreur chatbot inconnue."
                    : task.getException().getMessage();
            appendChatMessage(false, "Desole, je ne peux pas repondre maintenant. " + error);
            chatbotSendButton.setDisable(false);
            chatbotSendButton.setText("Envoyer");
        });

        Thread thread = new Thread(task, "chatbot-send");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void useChatbotQuickPrompt(ActionEvent event) {
        if (!(event.getSource() instanceof Button button) || chatbotInputField == null) {
            return;
        }
        String prompt = button.getUserData() == null ? button.getText() : button.getUserData().toString();
        chatbotInputField.setText(prompt);
        sendChatbotMessage();
    }

    private void appendChatMessage(boolean userMessage, String message) {
        if (chatbotMessagesBox == null) {
            return;
        }

        HBox row = new HBox();
        row.getStyleClass().add(userMessage ? "chatbot-message-row-user" : "chatbot-message-row-assistant");

        Label bubble = new Label(message);
        bubble.setWrapText(true);
        bubble.setMaxWidth(220);
        bubble.getStyleClass().add("chatbot-message-bubble");
        bubble.getStyleClass().add(userMessage ? "chatbot-message-bubble-user" : "chatbot-message-bubble-assistant");

        row.getChildren().add(bubble);
        chatbotMessagesBox.getChildren().add(row);

        if (chatbotMessagesScroll != null) {
            Platform.runLater(() -> chatbotMessagesScroll.setVvalue(1.0));
        }
    }

    private void configureProduitTable() {
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

    private void configureCategoryCombo() {
        categoryCombo.setCellFactory(comboBox -> new ListCell<>() {
            {
                getStyleClass().add("category-combo-cell");
            }

            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                setText(empty || category == null ? null : category.getNom());
            }
        });
        categoryCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Category category, boolean empty) {
                super.updateItem(category, empty);
                setText(empty || category == null ? null : category.getNom());
            }
        });
    }

    private void configureCommandeTables() {
        commandeColId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()));
        commandeColProduit.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getProduit().getNom()));
        commandeColClient.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getClientName()));
        commandeColQuantite.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getQuantite()));
        commandeColTotalAvant.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatCurrency(data.getValue().getMontantInitial())));
        commandeColTotal.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatCurrency(data.getValue().getMontantTotal())));
        commandeColStatut.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatut().getLabel()));
        commandeColDate.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatDateTime(data.getValue().getCreatedAt())));
        SortedList<Commande> sortedCommandes = new SortedList<>(filteredCommandes);
        sortedCommandes.comparatorProperty().bind(commandeTable.comparatorProperty());
        commandeTable.setItems(sortedCommandes);

        frontCommandeColId.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()));
        frontCommandeColProduit.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getProduit().getNom()));
        frontCommandeColQuantite.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getQuantite()));
        frontCommandeColTotalAvant.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatCurrency(data.getValue().getMontantInitial())));
        frontCommandeColTotal.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatCurrency(data.getValue().getMontantTotal())));
        frontCommandeColStatut.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getStatut().getLabel()));
        frontCommandeColDate.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(formatDateTime(data.getValue().getCreatedAt())));
        SortedList<Commande> sortedFrontCommandes = new SortedList<>(filteredFrontCommandes);
        sortedFrontCommandes.comparatorProperty().bind(frontCommandeTable.comparatorProperty());
        frontCommandeTable.setItems(sortedFrontCommandes);
    }

    private void configureSearchAndSort() {
        produitSortCombo.setItems(FXCollections.observableArrayList(
                "ID decroissant", "Nom A-Z", "Nom Z-A", "Prix croissant", "Prix decroissant", "Stock croissant", "Stock decroissant"
        ));
        produitSortCombo.setValue("ID decroissant");
        produitSearchField.textProperty().addListener((observable, oldValue, newValue) -> applyProduitFilterAndSort());
        produitSortCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyProduitFilterAndSort());

        frontSortCombo.setItems(FXCollections.observableArrayList(
                "Plus recent", "Nom A-Z", "Nom Z-A", "Prix croissant", "Prix decroissant"
        ));
        frontSortCombo.setValue("Plus recent");
        frontSearchField.textProperty().addListener((observable, oldValue, newValue) -> renderFrontOfficeProducts());
        frontSortCombo.valueProperty().addListener((observable, oldValue, newValue) -> renderFrontOfficeProducts());

        frontCategorySortCombo.setItems(FXCollections.observableArrayList("Nom A-Z", "Nom Z-A", "Plus de produits"));
        frontCategorySortCombo.setValue("Nom A-Z");
        frontCategorySearchField.textProperty().addListener((observable, oldValue, newValue) -> renderFrontCategories());
        frontCategorySortCombo.valueProperty().addListener((observable, oldValue, newValue) -> renderFrontCategories());

        categorySortCombo.setItems(FXCollections.observableArrayList("Nom A-Z", "Nom Z-A", "ID croissant", "ID decroissant"));
        categorySortCombo.setValue("Nom A-Z");
        categorySearchField.textProperty().addListener((observable, oldValue, newValue) -> applyCategoryFilterAndSort());
        categorySortCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyCategoryFilterAndSort());

        commandeStatusFilterCombo.setItems(FXCollections.observableArrayList(
                "Toutes", "En attente", "Validee", "Refusee", "Annulee"
        ));
        commandeStatusFilterCombo.setValue("Toutes");
        commandeStatusFilterCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyCommandeFilters());

        frontCommandeStatusFilterCombo.setItems(FXCollections.observableArrayList(
                "Toutes", "En attente", "Validee", "Refusee", "Annulee"
        ));
        frontCommandeStatusFilterCombo.setValue("Toutes");
        frontCommandeStatusFilterCombo.valueProperty().addListener((observable, oldValue, newValue) -> applyCommandeFilters());
    }

    private void applyProduitFilterAndSort() {
        String search = produitSearchField.getText() == null ? "" : produitSearchField.getText().trim().toLowerCase();
        filteredProduits.setPredicate(produit ->
                search.isEmpty()
                        || produit.getNom().toLowerCase().contains(search)
                        || produit.getDescription().toLowerCase().contains(search)
                        || produit.getCategory().getNom().toLowerCase().contains(search)
        );

        String sort = produitSortCombo.getValue();
        if ("Nom A-Z".equals(sort)) {
            produits.sort(Comparator.comparing(Produit::getNom, String.CASE_INSENSITIVE_ORDER));
        } else if ("Nom Z-A".equals(sort)) {
            produits.sort(Comparator.comparing(Produit::getNom, String.CASE_INSENSITIVE_ORDER).reversed());
        } else if ("Prix croissant".equals(sort)) {
            produits.sort(Comparator.comparingDouble(Produit::getPrix));
        } else if ("Prix decroissant".equals(sort)) {
            produits.sort(Comparator.comparingDouble(Produit::getPrix).reversed());
        } else if ("Stock croissant".equals(sort)) {
            produits.sort(Comparator.comparingInt(Produit::getStock));
        } else if ("Stock decroissant".equals(sort)) {
            produits.sort(Comparator.comparingInt(Produit::getStock).reversed());
        } else {
            produits.sort(Comparator.comparingInt(Produit::getId).reversed());
        }

        resultCountLabel.setText(filteredProduits.size() + " resultat(s)");
    }

    private void applyCategoryFilterAndSort() {
        String search = categorySearchField.getText() == null ? "" : categorySearchField.getText().trim().toLowerCase();
        filteredCategories.setPredicate(category ->
                search.isEmpty()
                        || category.getNom().toLowerCase().contains(search)
                        || category.getDescription().toLowerCase().contains(search)
        );

        String sort = categorySortCombo.getValue();
        if ("Nom Z-A".equals(sort)) {
            categories.sort(Comparator.comparing(Category::getNom, String.CASE_INSENSITIVE_ORDER).reversed());
        } else if ("ID croissant".equals(sort)) {
            categories.sort(Comparator.comparingInt(Category::getId));
        } else if ("ID decroissant".equals(sort)) {
            categories.sort(Comparator.comparingInt(Category::getId).reversed());
        } else {
            categories.sort(Comparator.comparing(Category::getNom, String.CASE_INSENSITIVE_ORDER));
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
        animateFlowItems(frontProductContainer);
        frontResultCountLabel.setText(displayList.size() + " produit(s)");
    }

    private Node createFrontProduitCard(Produit produit) {
        VBox card = new VBox(12);
        card.getStyleClass().add("front-card");
        card.setPrefWidth(280);

        StackPane imageHolder = new StackPane();
        imageHolder.getStyleClass().add("front-card-image-holder");
        imageHolder.setPrefSize(248, 180);
        imageHolder.getChildren().add(createImageNode(produit.getImageUrl(), 248, 180));

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

        Button detailsButton = new Button("Voir details");
        detailsButton.getStyleClass().add("ghost-button");
        detailsButton.setOnAction(event -> showFrontProduitDetails(produit));

        Button commanderButton = new Button("Commander");
        commanderButton.getStyleClass().add("primary-button");
        commanderButton.setDisable(produit.getStock() <= 0);
        commanderButton.setOnAction(event -> openCommandeForProduit(produit));

        VBox actionBox = new VBox(10, detailsButton, commanderButton);

        card.getChildren().addAll(imageHolder, categoryBadge, title, description, new VBox(10, price, stock), actionBox);
        attachCardHoverAnimation(card);
        return card;
    }

    private void showFrontProduitDetails(Produit produit) {
        selectedFrontProduit = produit;
        frontDetailTitle.setText(produit.getNom());
        frontDetailCategory.setText(produit.getCategory().getNom());
        frontDetailPrice.setText(String.format("%.2f DT", produit.getPrix()));
        frontDetailStock.setText("Stock disponible: " + produit.getStock());
        frontDetailDescription.setText(produit.getDescription());
        frontDetailImageContainer.getChildren().setAll(createImageNode(produit.getImageUrl(), 480, 340));

        commandeQuantiteField.setText("1");
        commandePromoCodeField.clear();
        commandeCommentaireArea.clear();
        commandeClientFeedbackLabel.setText("");
        hidePromoFeedback();
        submitCommandeButton.setDisable(produit.getStock() <= 0);
        if (produit.getStock() <= 0) {
            commandeClientFeedbackLabel.setText("Ce produit est actuellement en rupture de stock.");
        }

        setFrontSection(frontDetailSection);
        frontAccueilNavButton.getStyleClass().setAll("front-nav-button");
        frontProduitsNavButton.getStyleClass().setAll("front-nav-button", "active-front-nav-button");
        frontCategoriesNavButton.getStyleClass().setAll("front-nav-button");
        frontCommandesNavButton.getStyleClass().setAll("front-nav-button");
    }

    private void openCommandeForProduit(Produit produit) {
        showFrontProduitDetails(produit);
        commandeClientFeedbackLabel.setText("Saisissez la quantite puis cliquez sur \"Commander ce produit\".");
        if (produit.getStock() > 0 && (commandeQuantiteField.getText() == null || commandeQuantiteField.getText().isBlank())) {
            commandeQuantiteField.setText("1");
        }
        commandeQuantiteField.requestFocus();
        commandeQuantiteField.selectAll();
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
            displayList.sort(Comparator.comparing(Category::getNom, String.CASE_INSENSITIVE_ORDER).reversed());
        } else if ("Plus de produits".equals(sort)) {
            displayList.sort((a, b) -> Long.compare(countProduitsForCategory(b), countProduitsForCategory(a)));
        } else {
            displayList.sort(Comparator.comparing(Category::getNom, String.CASE_INSENSITIVE_ORDER));
        }

        frontCategoryContainer.getChildren().clear();
        for (Category category : displayList) {
            frontCategoryContainer.getChildren().add(createFrontCategoryCard(category));
        }
        animateFlowItems(frontCategoryContainer);
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
        attachCardHoverAnimation(card);
        return card;
    }

    private void showFrontCategoryDetails(Category category) {
        frontCategoryDetailTitle.setText(category.getNom());
        frontCategoryDetailDescription.setText(category.getDescription());
        frontCategoryDetailCount.setText(countProduitsForCategory(category) + " produit(s) dans cette categorie");
        setFrontSection(frontCategoryDetailSection);
        frontAccueilNavButton.getStyleClass().setAll("front-nav-button");
        frontProduitsNavButton.getStyleClass().setAll("front-nav-button");
        frontCategoriesNavButton.getStyleClass().setAll("front-nav-button", "active-front-nav-button");
        frontCommandesNavButton.getStyleClass().setAll("front-nav-button");
    }

    private long countProduitsForCategory(Category category) {
        return produits.stream().filter(produit -> produit.getCategory().getId() == category.getId()).count();
    }

    private void applyCommandeFilters() {
        String adminFilter = commandeStatusFilterCombo.getValue() == null ? "Toutes" : commandeStatusFilterCombo.getValue();
        String userFilter = frontCommandeStatusFilterCombo.getValue() == null ? "Toutes" : frontCommandeStatusFilterCombo.getValue();

        filteredCommandes.setPredicate(commande -> matchesCommandeFilter(commande, adminFilter));
        filteredFrontCommandes.setPredicate(commande -> matchesCommandeFilter(commande, userFilter));

        commandeResultCountLabel.setText(filteredCommandes.size() + " commande(s)");
        frontCommandeResultCountLabel.setText(filteredFrontCommandes.size() + " commande(s)");
    }

    private boolean matchesCommandeFilter(Commande commande, String filterValue) {
        if (filterValue == null || "Toutes".equals(filterValue)) {
            return true;
        }
        return commande.getStatut().getLabel().equalsIgnoreCase(filterValue);
    }

    private void updateDashboardStats() {
        long pendingAdminCount = commandes.stream()
                .filter(commande -> commande.getStatut() == CommandeStatut.EN_ATTENTE)
                .count();
        long lowStockCount = produits.stream()
                .filter(produit -> produit.getStock() > 0 && produit.getStock() <= 5)
                .count();
        double validatedRevenue = commandes.stream()
                .filter(commande -> commande.getStatut() == CommandeStatut.VALIDEE)
                .mapToDouble(Commande::getMontantTotal)
                .sum();

        adminPendingOrdersLabel.setText(pendingAdminCount + " en attente");
        adminLowStockLabel.setText(lowStockCount + " stock(s) faible(s)");
        adminValidatedRevenueLabel.setText(String.format("%.2f DT", validatedRevenue));
    }

    private Node createImageNode(String imagePath, double width, double height) {
        try {
            if (imagePath != null && !imagePath.isBlank()) {
                if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                    ImageView imageView = new ImageView(new Image(imagePath, width, height, false, true));
                    imageView.setFitWidth(width);
                    imageView.setFitHeight(height);
                    imageView.setPreserveRatio(false);
                    return imageView;
                }

                File file = new File(imagePath);
                if (file.exists()) {
                    ImageView imageView = new ImageView(new Image(file.toURI().toString(), width, height, false, true));
                    imageView.setFitWidth(width);
                    imageView.setFitHeight(height);
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

    private void refreshFrontCommandes() {
        frontCommandeResultCountLabel.setText(filteredFrontCommandes.size() + " commande(s)");
        long pendingCount = frontCommandes.stream()
                .filter(commande -> commande.getStatut() == CommandeStatut.EN_ATTENTE)
                .count();
        long validatedCount = frontCommandes.stream()
                .filter(commande -> commande.getStatut() == CommandeStatut.VALIDEE)
                .count();
        frontPendingOrdersLabel.setText(pendingCount + " en attente");
        frontValidatedOrdersLabel.setText(validatedCount + " validee(s)");
    }

    private void updateSelectedCommandeStatus(boolean approved) {
        Commande commande = commandeTable.getSelectionModel().getSelectedItem();
        if (commande == null) {
            showAlert(Alert.AlertType.WARNING, "Selection requise", "Veuillez selectionner une commande.");
            return;
        }

        try {
            CommandeStatut newStatus;
            if (approved) {
                commandeService.validerCommande(commande.getId());
                newStatus = CommandeStatut.VALIDEE;
            } else {
                commandeService.refuserCommande(commande.getId());
                newStatus = CommandeStatut.REFUSEE;
            }

            String successMessage = approved
                    ? "La commande a ete validee."
                    : "La commande a ete refusee.";

            try {
                mailService.sendCommandeStatusEmail(commande, newStatus);
                successMessage += "\n\nUn e-mail de notification a ete envoye a " + mailService.getNotificationRecipient() + ".";
            } catch (MessagingException exception) {
                successMessage += "\n\nLa commande a bien ete mise a jour, mais l'e-mail n'a pas pu etre envoye : " + exception.getMessage();
            }

            showAlert(
                    Alert.AlertType.INFORMATION,
                    approved ? "Commande validee" : "Commande refusee",
                    successMessage
            );
            loadData();
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Erreur base de donnees", exception.getMessage());
        }
    }

    private void loadData() {
        try {
            categories.setAll(categoryService.getAllCategories());
            produits.setAll(produitService.getAllProduits());
            categoryCombo.setItems(categories);
            commandes.clear();
            frontCommandes.clear();

            List<Commande> allCommandes = commandeService.getAllCommandes();
            commandes.setAll(allCommandes);
            frontCommandes.setAll(allCommandes);

            applyProduitFilterAndSort();
            applyCategoryFilterAndSort();
            applyCommandeFilters();
            renderFrontOfficeProducts();
            renderFrontCategories();
            updateDashboardStats();
            refreshFrontCommandes();
        } catch (SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "Connexion MySQL", "Impossible de charger les donnees.\n\n" + exception.getMessage());
        }
    }

    private void applyStandaloneAccess() {
        sessionModeLabel.setText("Mode sans connexion");
        backOfficeToggleButton.setVisible(true);
        backOfficeToggleButton.setManaged(true);
        sidebarCommandeListButton.setVisible(true);
        sidebarCommandeListButton.setManaged(true);
    }

    private void setFrontSection(VBox visibleSection) {
        switchSectionWithAnimation(List.of(
                frontHomeSection,
                frontListSection,
                frontDetailSection,
                frontCategoryListSection,
                frontCategoryDetailSection,
                frontCommandeSection
        ), visibleSection);
    }

    private void setBackView(VBox visibleView) {
        switchSectionWithAnimation(List.of(
                produitListView,
                produitFormView,
                categoryListView,
                categoryFormView,
                commandeListView
        ), visibleView);
    }

    private void clearCommandeForm() {
        commandeQuantiteField.setText("1");
        commandePromoCodeField.clear();
        commandeCommentaireArea.clear();
    }

    private void showPromoFeedback(String message) {
        commandePromoFeedbackLabel.setText(message);
        commandePromoFeedbackLabel.setManaged(true);
        commandePromoFeedbackLabel.setVisible(true);
    }

    private void hidePromoFeedback() {
        commandePromoFeedbackLabel.setText("");
        commandePromoFeedbackLabel.setManaged(false);
        commandePromoFeedbackLabel.setVisible(false);
    }

    private String formatPourcentage(double value) {
        if (Math.rint(value) == value) {
            return String.format("%.0f", value);
        }
        return String.format("%.2f", value);
    }

    private String formatCurrency(double value) {
        return String.format("%.2f DT", value);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_TIME_FORMATTER.format(dateTime);
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

    private void clearCommandeSidebar() {
        sidebarCommandeListButton.getStyleClass().setAll("menu-button");
    }

    private void initializeAnimations() {
        if (uiAnimationsInitialized) {
            return;
        }
        uiAnimationsInitialized = true;
        animateViewEntrance(contentArea);
        installButtonHoverAnimations(contentArea);
        installCardHoverAnimations(contentArea);
    }

    private void switchSectionWithAnimation(List<VBox> sections, VBox visibleSection) {
        for (VBox section : sections) {
            boolean mustShow = section == visibleSection;
            if (!mustShow) {
                section.setVisible(false);
                section.setManaged(false);
                section.setOpacity(1);
                section.setTranslateY(0);
            }
        }

        visibleSection.setVisible(true);
        visibleSection.setManaged(true);
        animateViewEntrance(visibleSection);
    }

    private void animateViewEntrance(Node node) {
        node.setOpacity(0);
        node.setTranslateY(18);

        FadeTransition fadeTransition = new FadeTransition(VIEW_FADE_DURATION, node);
        fadeTransition.setFromValue(0);
        fadeTransition.setToValue(1);

        TranslateTransition translateTransition = new TranslateTransition(VIEW_SLIDE_DURATION, node);
        translateTransition.setFromY(18);
        translateTransition.setToY(0);

        new ParallelTransition(fadeTransition, translateTransition).play();
    }

    private void animateFlowItems(FlowPane flowPane) {
        int index = 0;
        for (Node node : flowPane.getChildren()) {
            node.setOpacity(0);
            node.setTranslateY(14);

            FadeTransition fadeTransition = new FadeTransition(ITEM_FADE_DURATION, node);
            fadeTransition.setFromValue(0);
            fadeTransition.setToValue(1);
            fadeTransition.setDelay(Duration.millis(30L * index));

            TranslateTransition translateTransition = new TranslateTransition(ITEM_FADE_DURATION, node);
            translateTransition.setFromY(14);
            translateTransition.setToY(0);
            translateTransition.setDelay(Duration.millis(30L * index));

            fadeTransition.play();
            translateTransition.play();
            index++;
        }
    }

    private void installButtonHoverAnimations(Node node) {
        if (node instanceof Button button) {
            button.setOnMouseEntered(event -> animateScale(button, 1.03));
            button.setOnMouseExited(event -> animateScale(button, 1.0));
        }

        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                installButtonHoverAnimations(child);
            }
        }
    }

    private void attachCardHoverAnimation(Node node) {
        node.setOnMouseEntered(event -> {
            animateScale(node, 1.01);
            TranslateTransition upTransition = new TranslateTransition(Duration.millis(180), node);
            upTransition.setToY(-3);
            upTransition.play();
        });
        node.setOnMouseExited(event -> {
            animateScale(node, 1.0);
            TranslateTransition downTransition = new TranslateTransition(Duration.millis(180), node);
            downTransition.setToY(0);
            downTransition.play();
        });
    }

    private void installCardHoverAnimations(Node node) {
        if (node.getStyleClass().contains("front-card")
                || node.getStyleClass().contains("panel-card")
                || node.getStyleClass().contains("hero-panel")) {
            attachCardHoverAnimation(node);
        }

        if (node instanceof javafx.scene.Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                installCardHoverAnimations(child);
            }
        }
    }

    private void animateScale(Node node, double targetScale) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(150), node);
        transition.setToX(targetScale);
        transition.setToY(targetScale);
        transition.play();
    }
}
