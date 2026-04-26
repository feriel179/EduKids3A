package com.edukids.edukids3a.ui;

import com.edukids.edukids3a.auth.SessionManager;
import com.edukids.edukids3a.controller.QuestionController;
import com.edukids.edukids3a.controller.QuizController;
import com.edukids.edukids3a.controller.QuizResultController;
import com.edukids.edukids3a.model.Quiz;
import com.edukids.edukids3a.model.Question;
import com.edukids.edukids3a.model.QuizResult;
import com.edukids.edukids3a.model.Reponse;
import com.edukids.edukids3a.model.TypeQuestion;
import com.edukids.edukids3a.service.MailingService;
import com.edukids.edukids3a.service.OpenAiChatService;
import com.edukids.edukids3a.service.QuizImageGeneratorService;
import com.edukids.edukids3a.validation.ValidationException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.collections.transformation.FilteredList;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuizBackOfficeView {

    private final SessionManager sessionManager = new SessionManager();
    private final QuizController quizController;
    private final QuestionController questionController;
    private final QuizResultController quizResultController;
    private final MailingService mailingService = new MailingService();
    private final OpenAiChatService openAiChatService = new OpenAiChatService();
    private final QuizImageGeneratorService quizImageGeneratorService = new QuizImageGeneratorService();
    private final BorderPane root = new BorderPane();
    private Button disconnectHeaderButton;
    private Button frontHeaderButton;
    private Button backHeaderButton;
    private Button listQuizButton;
    private Button addQuizButton;
    private Button listQuestionButton;
    private Button addQuestionButton;
    private Button statsButton;
    private TextField titreField;
    private TextField imageUrlField;
    private TextArea descriptionArea;
    private Label quizErrorLabel;
    private ComboBox<String> niveauCombo;
    private ComboBox<String> categorieAgeCombo;
    private Spinner<Integer> questionSpinner;
    private Spinner<Integer> dureeSpinner;
    private Spinner<Integer> scoreSpinner;
    private ComboBox<String> statutCombo;
    private Quiz currentQuiz;
    private ComboBox<Quiz> quizCombo;
    private TextArea questionIntituleArea;
    private Label questionErrorLabel;
    private ComboBox<TypeQuestion> typeQuestionCombo;
    private Spinner<Integer> pointsSpinner;
    private TextField reponseField1;
    private TextField reponseField2;
    private TextField reponseField3;
    private TextField reponseField4;
    private TextField relationRightField1;
    private TextField relationRightField2;
    private TextField relationRightField3;
    private TextField relationRightField4;
    private CheckBox correctCheck1;
    private CheckBox correctCheck2;
    private CheckBox correctCheck3;
    private CheckBox correctCheck4;
    private RadioButton correctRadio1;
    private RadioButton correctRadio2;
    private RadioButton correctRadio3;
    private RadioButton correctRadio4;
    private TextField reponseLibreField;
    private VBox reponsesContainer;
    private Question currentQuestion;
    private Quiz currentFrontQuiz;
    private Timeline activeQuizTimeline;

    public QuizBackOfficeView(
            QuizController quizController,
            QuestionController questionController,
            QuizResultController quizResultController
    ) {
        this.quizController = quizController;
        this.questionController = questionController;
        this.quizResultController = quizResultController;
    }

    public BorderPane build() {
        root.getStyleClass().add("app-root");
        root.setTop(buildHeader());
        root.setLeft(buildSidebar());
        showQuizList();
        return root;
    }

    private VBox buildHeader() {
        Label brand = new Label("EduKids");
        brand.getStyleClass().add("brand-title");

        disconnectHeaderButton = createHeaderButton("Deconnexion");
        frontHeaderButton = createHeaderButton("Front office");
        backHeaderButton = createHeaderButton("Back office");
        backHeaderButton.getStyleClass().add("header-button-primary");
        disconnectHeaderButton.setOnAction(event -> handleDisconnect());
        frontHeaderButton.setOnAction(event -> showFrontOfficePage());
        backHeaderButton.setOnAction(event -> showQuizList());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(18, brand, spacer, disconnectHeaderButton, frontHeaderButton, backHeaderButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(22, 26, 18, 26));
        header.getStyleClass().addAll("top-bar", "app-header-row");

        Region line = new Region();
        line.getStyleClass().add("accent-line");
        line.setPrefHeight(3);

        return new VBox(header, line);
    }

    private VBox buildSidebar() {
        Label adminLabel = new Label("Administration");
        adminLabel.getStyleClass().add("sidebar-title");

        Label pagesLabel = new Label("Pages");
        pagesLabel.getStyleClass().add("sidebar-subtitle");

        listQuizButton = createSidebarButton("Liste des quiz", true);
        addQuizButton = createSidebarButton("Creer ou modifier un quiz", false);
        listQuestionButton = createSidebarButton("Liste des questions", false);
        addQuestionButton = createSidebarButton("Creer ou modifier une question", false);
        listQuizButton.setOnAction(event -> showQuizList());
        addQuizButton.setOnAction(event -> showQuizForm());
        listQuestionButton.setOnAction(event -> showQuestionList());
        addQuestionButton.setOnAction(event -> showQuestionForm());

        statsButton = createSidebarButton("Statistiques", false);
        statsButton.setOnAction(event -> showStatisticsPage());

        Separator separator1 = new Separator();
        Separator separator2 = new Separator();
        separator1.getStyleClass().add("sidebar-separator");
        separator2.getStyleClass().add("sidebar-separator");

        VBox sidebar = new VBox(
                16,
                adminLabel,
                pagesLabel,
                listQuizButton,
                addQuizButton,
                separator1,
                listQuestionButton,
                addQuestionButton,
                separator2,
                statsButton,
                new Separator()
        );
        sidebar.setPadding(new Insets(22, 14, 22, 14));
        sidebar.setPrefWidth(245);
        sidebar.getStyleClass().addAll("sidebar", "sidebar-shell");
        return sidebar;
    }

    private void showQuizList() {
        stopActiveQuizTimer();
        setHeaderMode(false);
        ensureBackOfficeLayout();
        setSidebarSelection(listQuizButton);
        root.setCenter(buildQuizList());
    }

    private void showQuizForm() {
        stopActiveQuizTimer();
        setHeaderMode(false);
        ensureBackOfficeLayout();
        setSidebarSelection(addQuizButton);
        currentQuiz = null;
        root.setCenter(buildQuizForm());
    }

    private void showQuestionList() {
        stopActiveQuizTimer();
        setHeaderMode(false);
        ensureBackOfficeLayout();
        setSidebarSelection(listQuestionButton);
        root.setCenter(buildQuestionList());
    }

    private void showQuestionForm() {
        stopActiveQuizTimer();
        setHeaderMode(false);
        ensureBackOfficeLayout();
        setSidebarSelection(addQuestionButton);
        currentQuestion = null;
        root.setCenter(buildQuestionForm());
    }

    private void setSidebarSelection(Button selectedButton) {
        listQuizButton.getStyleClass().remove("sidebar-button-active");
        addQuizButton.getStyleClass().remove("sidebar-button-active");
        listQuestionButton.getStyleClass().remove("sidebar-button-active");
        addQuestionButton.getStyleClass().remove("sidebar-button-active");
        statsButton.getStyleClass().remove("sidebar-button-active");
        selectedButton.getStyleClass().add("sidebar-button-active");
    }

    private void showStatisticsPage() {
        stopActiveQuizTimer();
        setHeaderMode(false);
        ensureBackOfficeLayout();
        setSidebarSelection(statsButton);
        root.setCenter(buildStatisticsPage());
    }

    private void showFrontOfficePage() {
        stopActiveQuizTimer();
        setHeaderMode(true);
        root.setLeft(null);
        clearSidebarSelection();
        currentFrontQuiz = null;
        root.setCenter(buildFrontOfficePage());
    }

    private void showFrontQuizDetails(Quiz quiz) {
        stopActiveQuizTimer();
        setHeaderMode(true);
        root.setLeft(null);
        clearSidebarSelection();
        currentFrontQuiz = quiz;
        root.setCenter(buildFrontQuizDetailsPage(quiz));
    }

    private ScrollPane buildQuizList() {
        VBox page = new VBox(22);
        page.setPadding(new Insets(24, 28, 24, 28));
        page.getStyleClass().add("page-shell");

        Label title = new Label("Liste des quiz");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Lorsque vous cliquez sur Ajouter, le quiz saisi est enregistre dans cette liste.");
        subtitle.getStyleClass().add("page-subtitle");
        VBox headerText = new VBox(6, title, subtitle);
        headerText.getStyleClass().add("section-header");

        FilteredList<Quiz> filteredQuizzes = new FilteredList<>(quizController.getAllQuizzes(), quiz -> true);
        TableView<Quiz> table = new TableView<>(FXCollections.observableArrayList(filteredQuizzes));
        table.getStyleClass().add("quiz-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Quiz, String> titleCol = new TableColumn<>("Titre");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("titre"));

        TableColumn<Quiz, Integer> questionCol = new TableColumn<>("Questions");
        questionCol.setCellValueFactory(new PropertyValueFactory<>("nombreQuestions"));

        TableColumn<Quiz, String> categorieCol = new TableColumn<>("Categorie d'age");
        categorieCol.setCellValueFactory(new PropertyValueFactory<>("categorieAge"));

        TableColumn<Quiz, String> statusCol = new TableColumn<>("Statut");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("statut"));

        table.getColumns().addAll(titleCol, questionCol, categorieCol, statusCol);
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Quiz selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openQuizForEdit(selected);
                }
            }
        });

        Button addButton = new Button("Ajouter");
        Button editButton = new Button("Modifier");
        Button noteButton = new Button("Note");
        Button duplicateButton = new Button("Dupliquer");
        Button deleteButton = new Button("Supprimer");
        TextField searchField = createTextField("Recherche par titre ou statut");
        searchField.setPrefWidth(260);
        ComboBox<String> niveauFilter = createComboBox("Tous les niveaux", "Debutant", "Intermediaire", "Avance");
        niveauFilter.setPrefWidth(170);
        ComboBox<String> categorieFilter = createComboBox("Toutes les categories", Quiz.CATEGORIE_AGE_FACILE, Quiz.CATEGORIE_AGE_STANDARD);
        categorieFilter.setPrefWidth(190);
        ComboBox<String> statutFilter = createComboBox("Tous les statuts", "Brouillon", "Publie", "Archive");
        statutFilter.setPrefWidth(170);
        ComboBox<String> sortCombo = createComboBox("Titre A-Z", "Titre Z-A", "Plus de questions", "Moins de questions", "Statut");
        sortCombo.setPrefWidth(180);
        addButton.getStyleClass().add("primary-action");
        editButton.getStyleClass().add("secondary-action");
        noteButton.getStyleClass().add("secondary-action");
        duplicateButton.getStyleClass().add("secondary-action");
        deleteButton.getStyleClass().add("danger-action");
        applyListActionButtonStyle(addButton, editButton, noteButton, duplicateButton, deleteButton);
        addButton.setOnAction(event -> showQuizForm());
        editButton.setOnAction(event -> {
            Quiz selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openQuizForEdit(selected);
            } else {
                showInfo("Selection", "Selectionnez un quiz a modifier.");
            }
        });
        noteButton.setOnAction(event -> showQuizNote(table.getSelectionModel().getSelectedItem()));
        duplicateButton.setOnAction(event -> duplicateSelectedQuiz(table.getSelectionModel().getSelectedItem()));
        deleteButton.setOnAction(event -> {
            Quiz selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                quizController.deleteQuiz(selected);
            } else {
                showInfo("Selection", "Selectionnez un quiz a supprimer.");
            }
        });

        Runnable refreshQuizView = () -> {
            filteredQuizzes.setPredicate(quiz -> matchesQuizFilters(
                    quiz,
                    searchField.getText(),
                    niveauFilter.getValue(),
                    categorieFilter.getValue(),
                    statutFilter.getValue()
            ));
            refreshQuizTable(table, filteredQuizzes, sortCombo.getValue());
        };
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshQuizView.run());
        niveauFilter.setOnAction(event -> refreshQuizView.run());
        categorieFilter.setOnAction(event -> refreshQuizView.run());
        statutFilter.setOnAction(event -> refreshQuizView.run());
        sortCombo.setOnAction(event -> refreshQuizView.run());
        refreshQuizView.run();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox filtersBar = new HBox(12, searchField, niveauFilter, categorieFilter, statutFilter, spacer, sortCombo);
        filtersBar.setAlignment(Pos.CENTER_LEFT);
        filtersBar.getStyleClass().add("list-toolbar");

        FlowPane listActions = new FlowPane();
        listActions.setHgap(14);
        listActions.setVgap(14);
        listActions.getChildren().addAll(addButton, editButton, noteButton, duplicateButton, deleteButton);
        listActions.getStyleClass().add("list-actions-row");

        VBox listHeader = new VBox(16, headerText, filtersBar);
        listHeader.getStyleClass().add("list-header-block");

        VBox card = new VBox(18, listHeader, table, listActions);
        card.setPadding(new Insets(24));
        card.getStyleClass().addAll("card", "content-card");

        page.getChildren().add(card);

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private ScrollPane buildQuizForm() {
        VBox page = new VBox(22);
        page.setPadding(new Insets(24, 28, 24, 28));
        page.getStyleClass().add("page-shell");

        Label pageTitle = new Label("Fiche quiz");
        pageTitle.getStyleClass().add("page-title");
        Label pageSubtitle = new Label(currentQuiz == null
                ? "Remplissez les champs de votre entite Quiz puis cliquez sur Ajouter."
                : "Modifiez les champs puis cliquez sur Enregistrer.");
        pageSubtitle.getStyleClass().add("page-subtitle");
        VBox titleBox = new VBox(6, pageTitle, pageSubtitle);
        titleBox.getStyleClass().add("section-header");

        titreField = createTextField("Ex. Quiz Java debutant");
        imageUrlField = createTextField("https://... ou image locale");
        descriptionArea = createTextArea("Decrivez ici le quiz...", 5);
        quizErrorLabel = createErrorLabel();
        niveauCombo = createComboBox("Debutant", "Intermediaire", "Avance");
        categorieAgeCombo = createComboBox(Quiz.CATEGORIE_AGE_FACILE, Quiz.CATEGORIE_AGE_STANDARD);
        questionSpinner = createSpinner(1, 100, 10);
        dureeSpinner = createSpinner(1, 180, 20);
        scoreSpinner = createSpinner(0, 100, 60);
        statutCombo = createComboBox("Brouillon", "Publie", "Archive");
        installMaxLengthFormatter(titreField, 120);
        installMaxLengthFormatter(imageUrlField, 500);
        installMaxLengthFormatter(descriptionArea, 1000);

        Button generateDescriptionButton = new Button("Generer description IA");
        generateDescriptionButton.getStyleClass().add("secondary-action");
        generateDescriptionButton.setOnAction(event -> generateQuizDescriptionWithAi(generateDescriptionButton));
        VBox descriptionBox = new VBox(10, descriptionArea, generateDescriptionButton);

        Button generateImageButton = new Button("Generer image");
        generateImageButton.getStyleClass().add("secondary-action");
        generateImageButton.setOnAction(event -> generateQuizImageFromForm(generateImageButton));
        VBox imageBox = new VBox(10, imageUrlField, generateImageButton);

        VBox presentationCard = createCard(
                "Presentation",
                createFormRow("Titre *", titreField),
                createFormRow("Image", imageBox),
                createFormRow("Description *", descriptionBox),
                createFormRow("Niveau *", niveauCombo),
                createFormRow("Categorie d'age *", categorieAgeCombo)
        );

        VBox detailsCard = createCard(
                "Details du quiz",
                createFormRow("Nombre de questions *", questionSpinner),
                createFormRow("Duree (minutes) *", dureeSpinner),
                createFormRow("Score minimum *", scoreSpinner),
                createFormRow("Statut *", statutCombo)
        );

        VBox previewCard = buildQuizPreviewCard();

        Button backButton = new Button("<- Retour a la liste");
        Button saveButton = new Button("Enregistrer");
        Button deleteButton = new Button("Supprimer");
        Button addButton = new Button(currentQuiz == null ? "Ajouter" : "Nouveau");
        Button generateQuizPackButton = new Button("Generer quiz complet IA");

        backButton.getStyleClass().add("secondary-action");
        saveButton.getStyleClass().add("primary-action");
        deleteButton.getStyleClass().add("danger-action");
        addButton.getStyleClass().add("primary-action");
        generateQuizPackButton.getStyleClass().add("secondary-action");

        backButton.setOnAction(event -> showQuizList());
        addButton.setOnAction(event -> {
            currentQuiz = null;
            root.setCenter(buildQuizForm());
        });
        saveButton.setOnAction(event -> enregistrerQuiz());
        deleteButton.setOnAction(event -> supprimerQuizEnCours());
        generateQuizPackButton.setOnAction(event -> generateFullQuizWithAi(generateQuizPackButton));

        FlowPane actions = new FlowPane();
        actions.setHgap(14);
        actions.setVgap(14);
        actions.getChildren().addAll(backButton, addButton, saveButton, generateQuizPackButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(16));
        actions.getStyleClass().add("actions-card");

        page.getChildren().addAll(titleBox, quizErrorLabel, presentationCard, detailsCard, previewCard, actions);

        if (currentQuiz != null) {
            remplirQuizForm(currentQuiz);
        }

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private ScrollPane buildQuestionList() {
        VBox page = new VBox(22);
        page.setPadding(new Insets(24, 28, 24, 28));
        page.getStyleClass().add("page-shell");

        Label title = new Label("Liste des questions");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Chaque question s'adapte maintenant a la categorie d'age du quiz choisi.");
        subtitle.getStyleClass().add("page-subtitle");
        VBox headerText = new VBox(6, title, subtitle);
        headerText.getStyleClass().add("section-header");

        FilteredList<Question> filteredQuestions = new FilteredList<>(questionController.getAllQuestions(), question -> true);
        TableView<Question> table = new TableView<>(FXCollections.observableArrayList(filteredQuestions));
        table.getStyleClass().add("quiz-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Question, String> quizCol = new TableColumn<>("Quiz");
        quizCol.setCellValueFactory(new PropertyValueFactory<>("quizTitre"));

        TableColumn<Question, String> intituleCol = new TableColumn<>("Question");
        intituleCol.setCellValueFactory(new PropertyValueFactory<>("intitule"));

        TableColumn<Question, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("typeLabel"));

        TableColumn<Question, String> reponseCol = new TableColumn<>("Reponses");
        reponseCol.setCellValueFactory(new PropertyValueFactory<>("resumeReponses"));

        table.getColumns().addAll(quizCol, intituleCol, typeCol, reponseCol);
        table.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Question selected = table.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openQuestionForEdit(selected);
                }
            }
        });

        Button addButton = new Button("Ajouter");
        Button editButton = new Button("Modifier");
        Button deleteButton = new Button("Supprimer");
        TextField searchField = createTextField("Recherche question, quiz, type");
        searchField.setPrefWidth(260);
        ComboBox<String> quizFilter = createFilterComboBox(
                "Tous les quiz",
                questionController.getAllQuestions().stream()
                        .map(Question::getQuizTitre)
                        .distinct()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList()
        );
        quizFilter.setPrefWidth(210);
        ComboBox<String> typeFilter = createComboBox("Tous les types", "QCM", "QCU", "Reponse libre", "Vrai ou Faux", "Relier par une fleche", "Petit jeu");
        typeFilter.setPrefWidth(170);
        ComboBox<String> sortCombo = createComboBox("Question A-Z", "Question Z-A", "Quiz A-Z", "Type");
        sortCombo.setPrefWidth(180);
        addButton.getStyleClass().add("primary-action");
        editButton.getStyleClass().add("secondary-action");
        deleteButton.getStyleClass().add("danger-action");
        applyListActionButtonStyle(addButton, editButton, deleteButton);
        addButton.setOnAction(event -> showQuestionForm());
        editButton.setOnAction(event -> {
            Question selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openQuestionForEdit(selected);
            } else {
                showInfo("Selection", "Selectionnez une question a modifier.");
            }
        });
        deleteButton.setOnAction(event -> {
            Question selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                questionController.deleteQuestion(selected);
            } else {
                showInfo("Selection", "Selectionnez une question a supprimer.");
            }
        });

        Runnable refreshQuestionView = () -> {
            filteredQuestions.setPredicate(question -> matchesQuestionFilters(
                    question,
                    searchField.getText(),
                    quizFilter.getValue(),
                    typeFilter.getValue()
            ));
            refreshQuestionTable(table, filteredQuestions, sortCombo.getValue());
        };
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshQuestionView.run());
        quizFilter.setOnAction(event -> refreshQuestionView.run());
        typeFilter.setOnAction(event -> refreshQuestionView.run());
        sortCombo.setOnAction(event -> refreshQuestionView.run());
        refreshQuestionView.run();

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox filtersBar = new HBox(12, searchField, quizFilter, typeFilter, spacer, sortCombo);
        filtersBar.setAlignment(Pos.CENTER_LEFT);
        filtersBar.getStyleClass().add("list-toolbar");

        FlowPane listActions = new FlowPane();
        listActions.setHgap(14);
        listActions.setVgap(14);
        listActions.getChildren().addAll(addButton, editButton, deleteButton);
        listActions.getStyleClass().add("list-actions-row");

        VBox listHeader = new VBox(16, headerText, filtersBar);
        listHeader.getStyleClass().add("list-header-block");

        VBox card = new VBox(18, listHeader, table, listActions);
        card.setPadding(new Insets(24));
        card.getStyleClass().addAll("card", "content-card");

        page.getChildren().add(card);

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private ScrollPane buildQuestionForm() {
        VBox page = new VBox(22);
        page.setPadding(new Insets(24, 28, 24, 28));
        page.getStyleClass().add("page-shell");

        Label pageTitle = new Label("Fiche question");
        pageTitle.getStyleClass().add("page-title");
        Label pageSubtitle = new Label(currentQuestion == null
                ? "Selectionnez un quiz puis choisissez le type de question."
                : "Modifiez la question puis cliquez sur Enregistrer.");
        pageSubtitle.getStyleClass().add("page-subtitle");
        VBox titleBox = new VBox(6, pageTitle, pageSubtitle);
        titleBox.getStyleClass().add("section-header");

        quizCombo = new ComboBox<>(quizController.getAllQuizzes());
        quizCombo.getStyleClass().add("app-combo");
        quizCombo.setMaxWidth(Double.MAX_VALUE);
        if (!quizController.getAllQuizzes().isEmpty()) {
            quizCombo.getSelectionModel().selectFirst();
        }

        questionIntituleArea = createTextArea("Ex. Quel mot-cle permet d'iterer sur une liste en Java ?", 4);
        questionErrorLabel = createErrorLabel();
        typeQuestionCombo = new ComboBox<>();
        typeQuestionCombo.getStyleClass().add("app-combo");
        typeQuestionCombo.setMaxWidth(Double.MAX_VALUE);
        typeQuestionCombo.setOnAction(event -> refreshReponseSection());
        quizCombo.setOnAction(event -> refreshQuestionTypeOptions());
        pointsSpinner = createSpinner(1, 20, 1);
        installMaxLengthFormatter(questionIntituleArea, 500);
        refreshQuestionTypeOptions();

        Button improveQuestionButton = new Button("Ameliorer question IA");
        improveQuestionButton.getStyleClass().add("secondary-action");
        improveQuestionButton.setOnAction(event -> improveQuestionWithAi(improveQuestionButton));
        VBox questionTextBox = new VBox(10, questionIntituleArea, improveQuestionButton);

        VBox questionCard = createCard(
                "Question",
                createFormRow("Quiz *", quizCombo),
                createFormRow("Intitule *", questionTextBox),
                createFormRow("Type *", typeQuestionCombo),
                createFormRow("Points *", pointsSpinner)
        );

        reponsesContainer = new VBox(18);
        reponsesContainer.getStyleClass().add("card");
        reponsesContainer.setPadding(new Insets(24));
        refreshReponseSection();

        Button backButton = new Button("<- Retour a la liste");
        Button addButton = new Button(currentQuestion == null ? "Ajouter" : "Nouveau");
        Button clearButton = new Button("Vider");
        Button saveButton = new Button("Enregistrer");
        Button deleteButton = new Button("Supprimer");
        backButton.getStyleClass().add("secondary-action");
        addButton.getStyleClass().add("primary-action");
        clearButton.getStyleClass().add("danger-action");
        saveButton.getStyleClass().add("primary-action");
        deleteButton.getStyleClass().add("danger-action");

        backButton.setOnAction(event -> showQuestionList());
        addButton.setOnAction(event -> {
            currentQuestion = null;
            root.setCenter(buildQuestionForm());
        });
        clearButton.setOnAction(event -> clearQuestionForm());
        saveButton.setOnAction(event -> enregistrerQuestion());
        deleteButton.setOnAction(event -> supprimerQuestionEnCours());

        FlowPane actions = new FlowPane();
        actions.setHgap(14);
        actions.setVgap(14);
        actions.getChildren().addAll(backButton, addButton, saveButton, deleteButton, clearButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(16));
        actions.getStyleClass().add("actions-card");

        page.getChildren().addAll(titleBox, questionErrorLabel, questionCard, reponsesContainer, actions);

        if (currentQuestion != null) {
            remplirQuestionForm(currentQuestion);
        }

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private void refreshReponseSection() {
        if (reponsesContainer == null) {
            return;
        }

        reponsesContainer.getChildren().clear();

        Label titleLabel = new Label("Reponses");
        titleLabel.getStyleClass().add("card-title");
        Region accent = new Region();
        accent.getStyleClass().add("card-accent");
        accent.setPrefWidth(4);
        accent.setMinWidth(4);
        HBox heading = new HBox(14, accent, titleLabel);
        heading.setAlignment(Pos.CENTER_LEFT);
        Button generateAnswersButton = new Button("Generer reponses IA");
        generateAnswersButton.getStyleClass().add("secondary-action");
        generateAnswersButton.setOnAction(event -> generateQuestionDraftWithAi(generateAnswersButton));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerRow = new HBox(14, heading, spacer, generateAnswersButton);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        reponsesContainer.getChildren().add(headerRow);

        TypeQuestion type = typeQuestionCombo.getValue();
        if (type == null) {
            return;
        }

        Label helperLabel = new Label(buildQuestionTypeHelperText(type));
        helperLabel.getStyleClass().add("page-subtitle");
        helperLabel.setWrapText(true);
        reponsesContainer.getChildren().add(helperLabel);

        if (type.isTextAnswer()) {
            reponseLibreField = createTextField("Ex. for");
            installMaxLengthFormatter(reponseLibreField, 255);
            reponsesContainer.getChildren().add(createFormRow("Reponse attendue *", reponseLibreField));
            return;
        }

        reponseField1 = createTextField("Reponse 1");
        reponseField2 = createTextField("Reponse 2");
        reponseField3 = createTextField("Reponse 3");
        reponseField4 = createTextField("Reponse 4");
        installMaxLengthFormatter(reponseField1, 255);
        installMaxLengthFormatter(reponseField2, 255);
        installMaxLengthFormatter(reponseField3, 255);
        installMaxLengthFormatter(reponseField4, 255);

        if (type == TypeQuestion.RELIER_FLECHE) {
            relationRightField1 = createTextField("Correspondance 1");
            relationRightField2 = createTextField("Correspondance 2");
            relationRightField3 = createTextField("Correspondance 3");
            relationRightField4 = createTextField("Correspondance 4");
            installMaxLengthFormatter(relationRightField1, 255);
            installMaxLengthFormatter(relationRightField2, 255);
            installMaxLengthFormatter(relationRightField3, 255);
            installMaxLengthFormatter(relationRightField4, 255);

            reponsesContainer.getChildren().addAll(
                    createMatchingPairRow("Association 1 *", reponseField1, relationRightField1),
                    createMatchingPairRow("Association 2 *", reponseField2, relationRightField2),
                    createMatchingPairRow("Association 3", reponseField3, relationRightField3),
                    createMatchingPairRow("Association 4", reponseField4, relationRightField4)
            );
            return;
        }

        if (type == TypeQuestion.VRAI_FAUX) {
            reponseField1.setText("Vrai");
            reponseField2.setText("Faux");
            reponseField1.setEditable(false);
            reponseField2.setEditable(false);
            reponseField3.setText("");
            reponseField4.setText("");
            reponseField3.setDisable(true);
            reponseField4.setDisable(true);
        }

        if (type.isMultiChoice()) {
            correctCheck1 = new CheckBox("Correcte");
            correctCheck2 = new CheckBox("Correcte");
            correctCheck3 = new CheckBox("Correcte");
            correctCheck4 = new CheckBox("Correcte");
            reponsesContainer.getChildren().addAll(
                    createAnswerRow(buildAnswerLabel(type, 1), reponseField1, correctCheck1),
                    createAnswerRow(buildAnswerLabel(type, 2), reponseField2, correctCheck2),
                    createAnswerRow(buildAnswerLabel(type, 3), reponseField3, correctCheck3),
                    createAnswerRow(buildAnswerLabel(type, 4), reponseField4, correctCheck4)
            );
        } else {
            ToggleGroup toggleGroup = new ToggleGroup();
            correctRadio1 = new RadioButton("Bonne reponse");
            correctRadio2 = new RadioButton("Bonne reponse");
            correctRadio3 = new RadioButton("Bonne reponse");
            correctRadio4 = new RadioButton("Bonne reponse");
            correctRadio1.setToggleGroup(toggleGroup);
            correctRadio2.setToggleGroup(toggleGroup);
            correctRadio3.setToggleGroup(toggleGroup);
            correctRadio4.setToggleGroup(toggleGroup);
            correctRadio1.setSelected(true);

            reponsesContainer.getChildren().addAll(
                    createAnswerRow(buildAnswerLabel(type, 1), reponseField1, correctRadio1),
                    createAnswerRow(buildAnswerLabel(type, 2), reponseField2, correctRadio2),
                    createAnswerRow(buildAnswerLabel(type, 3), reponseField3, correctRadio3),
                    createAnswerRow(buildAnswerLabel(type, 4), reponseField4, correctRadio4)
            );
        }
    }

    private HBox createAnswerRow(String labelText, TextField field, Region selector) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        label.setPadding(Insets.EMPTY);

        HBox fieldLine = new HBox(12, field, selector);
        fieldLine.setAlignment(Pos.BASELINE_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);

        HBox row = new HBox(18, label, fieldLine);
        row.setAlignment(Pos.BASELINE_LEFT);
        HBox.setHgrow(fieldLine, Priority.ALWAYS);
        return row;
    }

    private HBox createMatchingPairRow(String labelText, TextField leftField, TextField rightField) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        label.setMinWidth(0);
        label.setPadding(Insets.EMPTY);
        label.setManaged(false);
        label.setVisible(false);

        Label arrow = new Label("→");
        arrow.getStyleClass().add("matching-arrow-label");

        leftField.setPromptText(labelText + " - gauche");
        leftField.setPrefWidth(240);
        leftField.setMaxWidth(320);
        rightField.setPromptText(labelText + " - droite");
        rightField.setPrefWidth(240);
        rightField.setMaxWidth(320);

        HBox fieldLine = new HBox(12, leftField, arrow, rightField);
        fieldLine.setAlignment(Pos.BASELINE_LEFT);
        fieldLine.setMaxWidth(700);

        VBox row = new VBox(0, label, fieldLine);
        row.setAlignment(Pos.CENTER_LEFT);
        return new HBox(row);
    }

    private void refreshQuestionTypeOptions() {
        if (typeQuestionCombo == null) {
            return;
        }
        TypeQuestion selectedType = typeQuestionCombo.getValue();
        List<TypeQuestion> allowedTypes = getAllowedQuestionTypes();
        typeQuestionCombo.getItems().setAll(allowedTypes);
        if (selectedType != null && allowedTypes.contains(selectedType)) {
            typeQuestionCombo.setValue(selectedType);
        } else if (!allowedTypes.isEmpty()) {
            typeQuestionCombo.setValue(allowedTypes.get(0));
        }
        refreshReponseSection();
    }

    private List<TypeQuestion> getAllowedQuestionTypes() {
        Quiz selectedQuiz = quizCombo == null ? null : quizCombo.getValue();
        if (selectedQuiz != null && selectedQuiz.isCategorieFacile()) {
            return List.of(
                    TypeQuestion.VRAI_FAUX,
                    TypeQuestion.RELIER_FLECHE,
                    TypeQuestion.QCM,
                    TypeQuestion.PETIT_JEU
            );
        }
        return List.of(TypeQuestion.QCM, TypeQuestion.QCU, TypeQuestion.REPONSE_LIBRE);
    }

    private String buildQuestionTypeHelperText(TypeQuestion type) {
        return switch (type) {
            case VRAI_FAUX -> "Format enfant: les choix Vrai et Faux sont proposes automatiquement.";
            case RELIER_FLECHE -> "Format enfant: saisissez de petites associations simples a reconnaitre.";
            case PETIT_JEU -> "Format enfant: proposez une petite consigne ludique avec une seule bonne reponse.";
            case REPONSE_LIBRE -> "L'eleve doit taper la bonne reponse.";
            case QCU -> "Une seule bonne reponse est attendue.";
            case QCM -> "Une ou plusieurs bonnes reponses peuvent etre cochees.";
        };
    }

    private String buildAnswerLabel(TypeQuestion type, int index) {
        return switch (type) {
            case VRAI_FAUX -> index <= 2 ? "Choix " + index + " *" : "Choix " + index;
            case RELIER_FLECHE -> index <= 2 ? "Association " + index + " *" : "Association " + index;
            case PETIT_JEU -> index <= 2 ? "Carte " + index + " *" : "Carte " + index;
            default -> index <= 2 ? "Choix " + index + " *" : "Choix " + index;
        };
    }

    private void enregistrerQuestion() {
        try {
            questionErrorLabel.setVisible(false);
            Question question = new Question(
                    quizCombo.getValue(),
                    questionIntituleArea.getText().trim(),
                    typeQuestionCombo.getValue(),
                    pointsSpinner.getValue(),
                    buildReponses()
            );
            if (currentQuestion == null) {
                questionController.saveQuestion(null, question);
            } else {
                questionController.saveQuestion(currentQuestion, question);
            }
            showQuestionList();
        } catch (ValidationException ex) {
            showInlineError(questionErrorLabel, ex.getMessage());
        }
    }

    private List<Reponse> buildReponses() {
        List<Reponse> reponses = new ArrayList<>();
        TypeQuestion type = typeQuestionCombo.getValue();
        if (type != null && type.isTextAnswer()) {
            reponses.add(new Reponse(reponseLibreField.getText().trim(), true));
            return reponses;
        }

        if (type == TypeQuestion.RELIER_FLECHE) {
            addMatchingReponseIfFilled(reponses, reponseField1, relationRightField1);
            addMatchingReponseIfFilled(reponses, reponseField2, relationRightField2);
            addMatchingReponseIfFilled(reponses, reponseField3, relationRightField3);
            addMatchingReponseIfFilled(reponses, reponseField4, relationRightField4);
            return reponses;
        }

        addReponseIfFilled(reponses, reponseField1, isCorrect(1));
        addReponseIfFilled(reponses, reponseField2, isCorrect(2));
        addReponseIfFilled(reponses, reponseField3, isCorrect(3));
        addReponseIfFilled(reponses, reponseField4, isCorrect(4));
        return reponses;
    }

    private void addReponseIfFilled(List<Reponse> reponses, TextField field, boolean correcte) {
        String texte = field.getText().trim();
        if (!texte.isEmpty()) {
            reponses.add(new Reponse(texte, correcte));
        }
    }

    private void addMatchingReponseIfFilled(List<Reponse> reponses, TextField leftField, TextField rightField) {
        String left = leftField == null ? "" : leftField.getText().trim();
        String right = rightField == null ? "" : rightField.getText().trim();
        if (!left.isEmpty() || !right.isEmpty()) {
            reponses.add(new Reponse(left + "|||" + right, true));
        }
    }

    private boolean isCorrect(int index) {
        if (typeQuestionCombo.getValue() != null && typeQuestionCombo.getValue().isMultiChoice()) {
            return switch (index) {
                case 1 -> correctCheck1.isSelected();
                case 2 -> correctCheck2.isSelected();
                case 3 -> correctCheck3.isSelected();
                default -> correctCheck4.isSelected();
            };
        }
        return switch (index) {
            case 1 -> correctRadio1.isSelected();
            case 2 -> correctRadio2.isSelected();
            case 3 -> correctRadio3.isSelected();
            default -> correctRadio4.isSelected();
        };
    }

    private void enregistrerQuiz() {
        try {
            quizErrorLabel.setVisible(false);
            currentQuiz = persistQuizFormChanges(currentQuiz != null, true);
            showQuizList();
        } catch (ValidationException ex) {
            showInlineError(quizErrorLabel, ex.getMessage());
        }
    }

    private void supprimerQuizEnCours() {
        if (currentQuiz != null) {
            quizController.deleteQuiz(currentQuiz);
            currentQuiz = null;
            showQuizList();
            return;
        }
        clearForm();
    }

    private void clearForm() {
        titreField.clear();
        imageUrlField.clear();
        descriptionArea.clear();
        quizErrorLabel.setVisible(false);
        niveauCombo.getSelectionModel().selectFirst();
        categorieAgeCombo.getSelectionModel().select(Quiz.CATEGORIE_AGE_STANDARD);
        questionSpinner.getValueFactory().setValue(10);
        dureeSpinner.getValueFactory().setValue(20);
        scoreSpinner.getValueFactory().setValue(60);
        statutCombo.getSelectionModel().selectFirst();
    }

    private void clearQuestionForm() {
        questionErrorLabel.setVisible(false);
        if (quizCombo != null && !quizController.getAllQuizzes().isEmpty()) {
            quizCombo.getSelectionModel().selectFirst();
        }
        if (questionIntituleArea != null) {
            questionIntituleArea.clear();
        }
        if (typeQuestionCombo != null) {
            refreshQuestionTypeOptions();
        }
        if (pointsSpinner != null) {
            pointsSpinner.getValueFactory().setValue(1);
        }
        if (typeQuestionCombo != null && typeQuestionCombo.getValue() != null) {
            refreshReponseSection();
        }
    }

    private void generateQuizDescriptionWithAi(Button triggerButton) {
        String apiKey = openAiChatService.resolveApiKey();
        if (titreField.getText().trim().isEmpty()) {
            showWarning("Saisissez d'abord un titre de quiz.");
            return;
        }

        triggerButton.setDisable(true);
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return openAiChatService.generateQuizDescription(
                        apiKey,
                        titreField.getText().trim(),
                        niveauCombo.getValue(),
                        categorieAgeCombo.getValue(),
                        questionSpinner.getValue(),
                        dureeSpinner.getValue(),
                        statutCombo.getValue()
                );
            }
        };

        task.setOnSucceeded(event -> {
            descriptionArea.setText(task.getValue());
            saveQuizDraftIfEditing();
            triggerButton.setDisable(false);
            showInfo("Description", "La description du quiz a ete generee.");
        });
        task.setOnFailed(event -> {
            triggerButton.setDisable(false);
            showError("IA indisponible", task.getException() == null ? "Impossible de generer la description." : task.getException().getMessage());
        });
        startBackgroundTask(task, "ai-generate-quiz-description");
    }

    private void generateQuizImageFromForm(Button triggerButton) {
        if (titreField.getText().trim().isEmpty()) {
            showWarning("Saisissez d'abord un titre de quiz.");
            return;
        }

        Quiz previewQuiz = buildPreviewQuizSnapshot();
        triggerButton.setDisable(true);
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return quizImageGeneratorService.generateQuizCover(previewQuiz);
            }
        };

        task.setOnSucceeded(event -> {
            imageUrlField.setText(task.getValue());
            saveQuizDraftIfEditing();
            triggerButton.setDisable(false);
            showInfo("Image", "La couverture du quiz a ete generee.");
        });
        task.setOnFailed(event -> {
            triggerButton.setDisable(false);
            showError("Image impossible", task.getException() == null ? "Impossible de generer l'image du quiz." : task.getException().getMessage());
        });
        startBackgroundTask(task, "quiz-generate-image");
    }

    private void generateFullQuizWithAi(Button triggerButton) {
        String apiKey = openAiChatService.resolveApiKey();
        if (titreField.getText().trim().isEmpty()) {
            showWarning("Saisissez d'abord un titre de quiz.");
            return;
        }

        Quiz draftQuiz = new Quiz(
                currentQuiz == null ? null : currentQuiz.getId(),
                titreField.getText().trim(),
                descriptionArea.getText().trim(),
                imageUrlField.getText().trim(),
                niveauCombo.getValue(),
                categorieAgeCombo.getValue(),
                questionSpinner.getValue(),
                dureeSpinner.getValue(),
                scoreSpinner.getValue(),
                statutCombo.getValue()
        );

        try {
            quizErrorLabel.setVisible(false);
            currentQuiz = quizController.saveQuiz(currentQuiz, draftQuiz);
        } catch (ValidationException ex) {
            showInlineError(quizErrorLabel, ex.getMessage());
            return;
        }

        triggerButton.setDisable(true);
        Quiz targetQuiz = currentQuiz;
        Task<List<OpenAiChatService.QuestionDraft>> task = new Task<>() {
            @Override
            protected List<OpenAiChatService.QuestionDraft> call() throws Exception {
                return openAiChatService.generateQuizPack(
                        apiKey,
                        targetQuiz.getTitre(),
                        targetQuiz.getNiveau(),
                        targetQuiz.getCategorieAge(),
                        targetQuiz.getDescription(),
                        targetQuiz.getDureeMinutes(),
                        Math.max(1, targetQuiz.getNombreQuestions())
                );
            }
        };

        task.setOnSucceeded(event -> {
            int created = 0;
            for (OpenAiChatService.QuestionDraft draft : task.getValue()) {
                try {
                    questionController.saveQuestion(null, buildQuestionFromDraft(targetQuiz, draft));
                    created++;
                } catch (Exception ignored) {
                }
            }
            triggerButton.setDisable(false);
            root.setCenter(buildQuizForm());
            showInfo("Quiz", created + " question(s) ont ete generees et ajoutees au quiz.");
        });
        task.setOnFailed(event -> {
            triggerButton.setDisable(false);
            showError("IA indisponible", task.getException() == null ? "Impossible de generer le quiz complet." : task.getException().getMessage());
        });
        startBackgroundTask(task, "ai-generate-full-quiz");
    }

    private void improveQuestionWithAi(Button triggerButton) {
        String apiKey = openAiChatService.resolveApiKey();
        if (questionIntituleArea.getText().trim().isEmpty()) {
            showWarning("Saisissez d'abord une question.");
            return;
        }

        triggerButton.setDisable(true);
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                Quiz selectedQuiz = quizCombo.getValue();
                return openAiChatService.improveQuestionText(
                        apiKey,
                        selectedQuiz == null ? "" : selectedQuiz.getTitre(),
                        selectedQuiz == null ? "" : selectedQuiz.getNiveau(),
                        selectedQuiz == null ? "" : selectedQuiz.getCategorieAge(),
                        typeQuestionCombo.getValue(),
                        questionIntituleArea.getText().trim()
                );
            }
        };

        task.setOnSucceeded(event -> {
            questionIntituleArea.setText(task.getValue());
            triggerButton.setDisable(false);
            showInfo("Question", "La formulation de la question a ete amelioree.");
        });
        task.setOnFailed(event -> {
            triggerButton.setDisable(false);
            showError("IA indisponible", task.getException() == null ? "Impossible d'ameliorer la question." : task.getException().getMessage());
        });
        startBackgroundTask(task, "ai-improve-question");
    }

    private void generateQuestionDraftWithAi(Button triggerButton) {
        String apiKey = openAiChatService.resolveApiKey();
        if (questionIntituleArea.getText().trim().isEmpty()) {
            showWarning("Saisissez d'abord une question.");
            return;
        }

        triggerButton.setDisable(true);
        Task<OpenAiChatService.QuestionDraft> task = new Task<>() {
            @Override
            protected OpenAiChatService.QuestionDraft call() throws Exception {
                Quiz selectedQuiz = quizCombo.getValue();
                return openAiChatService.generateQuestionDraft(
                        apiKey,
                        selectedQuiz == null ? "" : selectedQuiz.getTitre(),
                        selectedQuiz == null ? "" : selectedQuiz.getNiveau(),
                        selectedQuiz == null ? "" : selectedQuiz.getCategorieAge(),
                        typeQuestionCombo.getValue(),
                        questionIntituleArea.getText().trim()
                );
            }
        };

        task.setOnSucceeded(event -> {
            applyGeneratedQuestionDraft(task.getValue());
            triggerButton.setDisable(false);
            showInfo("Reponses", "Les reponses de la question ont ete generees.");
        });
        task.setOnFailed(event -> {
            triggerButton.setDisable(false);
            showError("IA indisponible", task.getException() == null ? "Impossible de generer les reponses." : task.getException().getMessage());
        });
        startBackgroundTask(task, "ai-generate-question-draft");
    }

    private void applyGeneratedQuestionDraft(OpenAiChatService.QuestionDraft draft) {
        if (draft == null) {
            return;
        }
        if (draft.question() != null && !draft.question().isBlank()) {
            questionIntituleArea.setText(draft.question().trim());
        }

        if (draft.type() != null && typeQuestionCombo.getItems().contains(draft.type()) && typeQuestionCombo.getValue() != draft.type()) {
            typeQuestionCombo.setValue(draft.type());
            refreshReponseSection();
        }

        TypeQuestion currentType = typeQuestionCombo.getValue();
        if (currentType == null) {
            currentType = draft.type();
        }
        if (currentType != null && currentType.isTextAnswer()) {
            if (reponseLibreField != null && draft.freeTextAnswer() != null && !draft.freeTextAnswer().isBlank()) {
                reponseLibreField.setText(draft.freeTextAnswer().trim());
            }
            return;
        }

        if (currentType == TypeQuestion.RELIER_FLECHE) {
            applyMatchingDraftChoices(draft.choices());
            return;
        }

        List<String> choices = draft.choices();
        if (choices.size() > 0) {
            reponseField1.setText(choices.get(0));
        }
        if (choices.size() > 1) {
            reponseField2.setText(choices.get(1));
        }
        if (choices.size() > 2) {
            reponseField3.setText(choices.get(2));
        }
        if (choices.size() > 3) {
            reponseField4.setText(choices.get(3));
        }

        clearCorrectSelections();
        for (Integer index : draft.correctIndexes()) {
            if (index != null) {
                setCorrectSelection(index, true);
            }
        }
    }

    private Question buildQuestionFromDraft(Quiz quiz, OpenAiChatService.QuestionDraft draft) {
        TypeQuestion typeQuestion = draft.type();
        if (typeQuestion == null) {
            typeQuestion = draft.freeTextAnswer() != null && !draft.freeTextAnswer().isBlank()
                    ? TypeQuestion.REPONSE_LIBRE
                    : TypeQuestion.QCU;
        }
        typeQuestion = normalizeDraftTypeForQuiz(quiz, typeQuestion);

        List<Reponse> reponses = new ArrayList<>();
        if (typeQuestion.isTextAnswer()) {
            String freeText = draft.freeTextAnswer() == null ? "" : draft.freeTextAnswer().trim();
            reponses.add(new Reponse(freeText.isBlank() ? "reponse attendue" : freeText, true));
        } else if (typeQuestion == TypeQuestion.RELIER_FLECHE) {
            for (String choice : draft.choices()) {
                String normalizedPair = normalizeGeneratedMatchingChoice(choice);
                if (!normalizedPair.isBlank()) {
                    reponses.add(new Reponse(normalizedPair, true));
                }
            }
        } else {
            for (int i = 0; i < draft.choices().size(); i++) {
                String choice = draft.choices().get(i);
                if (choice != null && !choice.isBlank()) {
                    reponses.add(new Reponse(choice.trim(), draft.correctIndexes().contains(i + 1)));
                }
            }
        }

        return new Question(
                quiz,
                draft.question() == null || draft.question().isBlank() ? "Question generee" : draft.question().trim(),
                typeQuestion,
                1,
                reponses
        );
    }

    private TypeQuestion normalizeDraftTypeForQuiz(Quiz quiz, TypeQuestion draftType) {
        if (draftType == null) {
            return quiz != null && quiz.isCategorieFacile() ? TypeQuestion.VRAI_FAUX : TypeQuestion.QCU;
        }
        if (quiz == null) {
            return draftType;
        }
        if (quiz.isCategorieFacile()) {
            if (draftType.isEasyModeType()) {
                return draftType;
            }
            return draftType == TypeQuestion.REPONSE_LIBRE ? TypeQuestion.PETIT_JEU : TypeQuestion.QCM;
        }
        if (draftType == TypeQuestion.VRAI_FAUX || draftType == TypeQuestion.PETIT_JEU) {
            return TypeQuestion.QCU;
        }
        if (draftType == TypeQuestion.RELIER_FLECHE) {
            return TypeQuestion.QCM;
        }
        return draftType;
    }

    private void applyMatchingDraftChoices(List<String> choices) {
        TextField[] leftFields = {reponseField1, reponseField2, reponseField3, reponseField4};
        TextField[] rightFields = {relationRightField1, relationRightField2, relationRightField3, relationRightField4};
        for (int index = 0; index < 4; index++) {
            String[] pair = splitGeneratedMatchingChoice(choices != null && index < choices.size() ? choices.get(index) : "");
            if (leftFields[index] != null) {
                leftFields[index].setText(pair[0]);
            }
            if (rightFields[index] != null) {
                rightFields[index].setText(pair[1]);
            }
        }
    }

    private String[] splitGeneratedMatchingChoice(String value) {
        if (value == null || value.isBlank()) {
            return new String[]{"", ""};
        }
        String normalized = value.trim().replace("->", "|||");
        if (normalized.contains("|||")) {
            String[] parts = normalized.split("\\|\\|\\|", 2);
            return new String[]{
                    parts.length > 0 ? parts[0].trim() : "",
                    parts.length > 1 ? parts[1].trim() : ""
            };
        }
        return new String[]{normalized, ""};
    }

    private String normalizeGeneratedMatchingChoice(String value) {
        String[] parts = splitGeneratedMatchingChoice(value);
        if (parts[0].isBlank() || parts[1].isBlank()) {
            return "";
        }
        return parts[0] + "|||" + parts[1];
    }

    private void clearCorrectSelections() {
        if (typeQuestionCombo.getValue() != null && typeQuestionCombo.getValue().isMultiChoice()) {
            if (correctCheck1 != null) correctCheck1.setSelected(false);
            if (correctCheck2 != null) correctCheck2.setSelected(false);
            if (correctCheck3 != null) correctCheck3.setSelected(false);
            if (correctCheck4 != null) correctCheck4.setSelected(false);
            return;
        }
        if (correctRadio1 != null) correctRadio1.setSelected(false);
        if (correctRadio2 != null) correctRadio2.setSelected(false);
        if (correctRadio3 != null) correctRadio3.setSelected(false);
        if (correctRadio4 != null) correctRadio4.setSelected(false);
    }

    private void startBackgroundTask(Task<?> task, String threadName) {
        Thread thread = new Thread(task, threadName);
        thread.setDaemon(true);
        thread.start();
    }

    private Quiz buildQuizFromForm() {
        return new Quiz(
                currentQuiz == null ? null : currentQuiz.getId(),
                titreField.getText().trim(),
                descriptionArea.getText().trim(),
                imageUrlField.getText().trim(),
                niveauCombo.getValue(),
                categorieAgeCombo.getValue(),
                questionSpinner.getValue(),
                dureeSpinner.getValue(),
                scoreSpinner.getValue(),
                statutCombo.getValue()
        );
    }

    private Quiz persistQuizFormChanges(boolean requireExistingQuiz, boolean validateFields) {
        if (requireExistingQuiz && currentQuiz == null) {
            return null;
        }

        Quiz quiz = buildQuizFromForm();
        if (!validateFields && currentQuiz != null) {
            if (quiz.getTitre() == null || quiz.getTitre().isBlank()) {
                return currentQuiz;
            }
            if (quiz.getDescription() == null || quiz.getDescription().trim().length() < 10) {
                return currentQuiz;
            }
        }
        return quizController.saveQuiz(currentQuiz, quiz);
    }

    private void saveQuizDraftIfEditing() {
        if (currentQuiz == null) {
            return;
        }
        try {
            quizErrorLabel.setVisible(false);
            currentQuiz = persistQuizFormChanges(true, false);
        } catch (ValidationException ignored) {
        }
    }

    private void supprimerQuestionEnCours() {
        if (currentQuestion != null) {
            questionController.deleteQuestion(currentQuestion);
            currentQuestion = null;
            showQuestionList();
            return;
        }
        clearQuestionForm();
    }

    private void openQuizForEdit(Quiz quiz) {
        currentQuiz = quiz;
        setSidebarSelection(addQuizButton);
        root.setCenter(buildQuizForm());
    }

    private void openQuestionForEdit(Question question) {
        currentQuestion = question;
        setSidebarSelection(addQuestionButton);
        root.setCenter(buildQuestionForm());
    }

    private void remplirQuizForm(Quiz quiz) {
        titreField.setText(quiz.getTitre());
        imageUrlField.setText(quiz.getImageUrl());
        descriptionArea.setText(quiz.getDescription());
        niveauCombo.setValue(quiz.getNiveau());
        categorieAgeCombo.setValue(quiz.getCategorieAge());
        questionSpinner.getValueFactory().setValue(quiz.getNombreQuestions());
        dureeSpinner.getValueFactory().setValue(quiz.getDureeMinutes());
        scoreSpinner.getValueFactory().setValue(quiz.getScoreMinimum());
        statutCombo.setValue(quiz.getStatut());
    }

    private void remplirQuestionForm(Question question) {
        quizCombo.setValue(question.getQuiz());
        refreshQuestionTypeOptions();
        questionIntituleArea.setText(question.getIntitule());
        typeQuestionCombo.setValue(question.getType());
        pointsSpinner.getValueFactory().setValue(question.getPoints());
        refreshReponseSection();

        if (question.getType().isTextAnswer()) {
            if (!question.getReponses().isEmpty()) {
                reponseLibreField.setText(question.getReponses().get(0).getTexte());
            }
            return;
        }

        if (question.getType() == TypeQuestion.RELIER_FLECHE) {
            fillMatchingChoice(0, question.getReponses());
            fillMatchingChoice(1, question.getReponses());
            fillMatchingChoice(2, question.getReponses());
            fillMatchingChoice(3, question.getReponses());
            return;
        }

        List<Reponse> reponses = question.getReponses();
        fillChoice(0, reponses);
        fillChoice(1, reponses);
        fillChoice(2, reponses);
        fillChoice(3, reponses);
    }

    private void fillChoice(int index, List<Reponse> reponses) {
        if (index >= reponses.size()) {
            return;
        }
        Reponse reponse = reponses.get(index);
        switch (index) {
            case 0 -> {
                reponseField1.setText(reponse.getTexte());
                setCorrectSelection(1, reponse.isCorrecte());
            }
            case 1 -> {
                reponseField2.setText(reponse.getTexte());
                setCorrectSelection(2, reponse.isCorrecte());
            }
            case 2 -> {
                reponseField3.setText(reponse.getTexte());
                setCorrectSelection(3, reponse.isCorrecte());
            }
            default -> {
                reponseField4.setText(reponse.getTexte());
                setCorrectSelection(4, reponse.isCorrecte());
            }
        }
    }

    private void fillMatchingChoice(int index, List<Reponse> reponses) {
        List<String[]> pairs = extractMatchingPairs(reponses);
        if (index >= pairs.size()) {
            return;
        }
        String[] parts = pairs.get(index);
        String left = parts[0];
        String right = parts[1];
        switch (index) {
            case 0 -> {
                reponseField1.setText(left);
                relationRightField1.setText(right);
            }
            case 1 -> {
                reponseField2.setText(left);
                relationRightField2.setText(right);
            }
            case 2 -> {
                reponseField3.setText(left);
                relationRightField3.setText(right);
            }
            default -> {
                reponseField4.setText(left);
                relationRightField4.setText(right);
            }
        }
    }

    private void setCorrectSelection(int index, boolean selected) {
        if (typeQuestionCombo.getValue() != null && typeQuestionCombo.getValue().isMultiChoice()) {
            switch (index) {
                case 1 -> correctCheck1.setSelected(selected);
                case 2 -> correctCheck2.setSelected(selected);
                case 3 -> correctCheck3.setSelected(selected);
                default -> correctCheck4.setSelected(selected);
            }
            return;
        }

        if (!selected) {
            return;
        }
        switch (index) {
            case 1 -> correctRadio1.setSelected(true);
            case 2 -> correctRadio2.setSelected(true);
            case 3 -> correctRadio3.setSelected(true);
            default -> correctRadio4.setSelected(true);
        }
    }

    private void showWarning(String message) {
        showStyledMessage("Verification", message, "warning");
    }

    private void showInlineError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void showInfo(String header, String message) {
        showStyledMessage(header, message, "info");
    }

    private void showError(String header, String message) {
        showStyledMessage(header, message, "error");
    }

    private void showStyledMessage(String header, String message, String variant) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (root.getScene() != null && root.getScene().getWindow() != null) {
            dialog.initOwner(root.getScene().getWindow());
        }
        dialog.setTitle(header);

        Label badge = new Label(switch (variant) {
            case "error" -> "Erreur";
            case "warning" -> "Attention";
            default -> "Info";
        });
        badge.getStyleClass().addAll("message-badge", "message-badge-" + variant);

        Label title = new Label(header);
        title.getStyleClass().add("page-title");

        Label body = new Label(message);
        body.getStyleClass().add("message-body");
        body.setWrapText(true);

        Button closeButton = new Button("Fermer");
        closeButton.getStyleClass().add("front-card-button");
        closeButton.setOnAction(event -> dialog.close());

        VBox content = new VBox(16, badge, title, body, closeButton);
        content.setPadding(new Insets(24));
        content.getStyleClass().addAll("score-dialog-card", "message-dialog-card");

        Scene scene = new Scene(content, 430, 250);
        if (root.getScene() != null && !root.getScene().getStylesheets().isEmpty()) {
            scene.getStylesheets().addAll(root.getScene().getStylesheets());
        }
        scene.setFill(Color.TRANSPARENT);

        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private void handleDisconnect() {
        sessionManager.disconnect();
        setHeaderMode(false);
        ensureBackOfficeLayout();
        clearSidebarSelection();

        Label title = new Label("Session deconnectee");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Cliquez sur Reconnexion pour revenir au back office.");
        subtitle.getStyleClass().add("page-subtitle");
        Button reconnectButton = new Button("Reconnexion");
        reconnectButton.getStyleClass().add("primary-action");
        reconnectButton.setOnAction(event -> {
            sessionManager.reconnect();
            showQuizList();
        });

        VBox card = new VBox(18, title, subtitle, reconnectButton);
        card.setPadding(new Insets(28));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("card");

        VBox page = new VBox(card);
        page.setPadding(new Insets(24, 28, 24, 28));
        root.setCenter(page);
    }

    private ScrollPane buildStatisticsPage() {
        VBox page = new VBox(22);
        page.setPadding(new Insets(24, 28, 24, 28));
        page.getStyleClass().add("page-shell");

        Label title = new Label("Statistiques");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Vue resumee du contenu enregistre dans la base de donnees.");
        subtitle.getStyleClass().add("page-subtitle");

        int nombreQuiz = quizController.getAllQuizzes().size();
        int nombreQuestions = questionController.getAllQuestions().size();
        long nombreReponses = questionController.getAllQuestions().stream()
                .mapToLong(question -> question.getReponses().size())
                .sum();
        long nombrePublies = quizController.getAllQuizzes().stream()
                .filter(quiz -> "Publie".equalsIgnoreCase(quiz.getStatut()))
                .count();
        long nombreBrouillons = quizController.getAllQuizzes().stream()
                .filter(quiz -> "Brouillon".equalsIgnoreCase(quiz.getStatut()))
                .count();
        long nombreArchives = quizController.getAllQuizzes().stream()
                .filter(quiz -> "Archive".equalsIgnoreCase(quiz.getStatut()))
                .count();
        int scoreMoyen = (int) Math.round(quizController.getAllQuizzes().stream()
                .mapToInt(Quiz::getScoreMinimum)
                .average()
                .orElse(0));
        Quiz quizLePlusRiche = quizController.getAllQuizzes().stream()
                .max(Comparator.comparingInt(Quiz::getNombreQuestions))
                .orElse(null);

        PieChart chart = createStatisticsPieChart(
                new String[]{"Brouillon", "Publie", "Archive"},
                new double[]{nombreBrouillons, nombrePublies, nombreArchives},
                new String[]{"#7fd14b", "#f7f34a", "#8ec9d3"}
        );
        VBox legend = createStatisticsLegend(
                "Legende",
                new String[]{"Brouillon", "Publie", "Archive"},
                new String[]{"#7fd14b", "#f7f34a", "#8ec9d3"}
        );

        HBox chartRow = new HBox(26, chart, legend);
        chartRow.setAlignment(Pos.CENTER);
        chartRow.setPadding(new Insets(10, 0, 6, 0));

        VBox chartCard = new VBox(18);
        chartCard.setPadding(new Insets(10, 0, 10, 0));
        Label chartTitle = new Label("Repartition des quiz par statut");
        chartTitle.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: 800;");
        chartCard.getChildren().addAll(chartTitle, chartRow);

        FlowPane summaryRow = new FlowPane();
        summaryRow.setHgap(18);
        summaryRow.setVgap(18);
        summaryRow.getChildren().addAll(
                createLightStatCard("Quiz", String.valueOf(nombreQuiz)),
                createLightStatCard("Questions", String.valueOf(nombreQuestions)),
                createLightStatCard("Reponses", String.valueOf(nombreReponses)),
                createLightStatCard("Score min moyen", scoreMoyen + "%")
        );
        summaryRow.getStyleClass().add("stats-summary-grid");

        Label insightsTitle = new Label("Analyse rapide");
        insightsTitle.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 800;");
        Label insightsText = new Label(buildStatisticsInsight(quizLePlusRiche, nombreQuestions));
        insightsText.setStyle("-fx-text-fill: #c9c9c9; -fx-font-size: 14px;");
        insightsText.setWrapText(true);
        Button exportButton = new Button("Exporter en PDF");
        exportButton.getStyleClass().add("primary-action");
        exportButton.setOnAction(event -> exportAnalyticsReport());
        Button mailButton = new Button("Envoyer par email");
        mailButton.getStyleClass().add("secondary-action");
        mailButton.setOnAction(event -> openAnalyticsMailDialog());
        Label mailStatus = new Label(mailingService.hasConfiguration()
                ? "Mailing configure"
                : "Mailing non configure");
        mailStatus.setStyle("-fx-text-fill: #9cb3c9; -fx-font-size: 12px;");

        HBox actionsRow = new HBox(12, exportButton, mailButton);
        actionsRow.setAlignment(Pos.CENTER_LEFT);
        VBox insightsCard = new VBox(14, insightsTitle, insightsText, actionsRow, mailStatus);
        insightsCard.setPadding(new Insets(18, 0, 0, 0));

        VBox pageBox = new VBox(18, title, subtitle, chartCard, summaryRow, insightsCard);

        ScrollPane scrollPane = new ScrollPane(pageBox);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private ScrollPane buildFrontOfficePage() {
        VBox page = new VBox();
        page.getStyleClass().addAll("front-page", "front-page-shell");

        HBox navBar = new HBox(18);
        navBar.setPadding(new Insets(18, 32, 18, 32));
        navBar.getStyleClass().addAll("front-nav", "front-nav-shell");

        Button accueilButton = new Button("Accueil");
        Button quizButton = new Button("Quiz");
        Button programmesButton = new Button("Programmes");
        accueilButton.getStyleClass().add("front-tab");
        quizButton.getStyleClass().addAll("front-tab", "front-tab-active");
        programmesButton.getStyleClass().add("front-tab");
        accueilButton.setOnAction(event -> showFrontOfficePage());
        quizButton.setOnAction(event -> showFrontOfficePage());
        programmesButton.setOnAction(event -> showInfo("Programmes", "La page Programmes peut etre ajoutee ensuite."));
        navBar.getChildren().addAll(accueilButton, quizButton, programmesButton);

        StackPane hero = new StackPane();
        hero.setPadding(new Insets(42, 20, 42, 20));
        hero.setMinHeight(250);
        hero.getStyleClass().addAll("front-hero", "front-detail-hero");
        decorateKidHero(hero);

        VBox heroContent = new VBox(10);
        heroContent.setAlignment(Pos.CENTER);
        Label heroTitle = new Label("Nos quiz");
        heroTitle.getStyleClass().add("front-hero-title");
        Label heroSubtitle = new Label("Explorez, recherchez et lancez les quiz disponibles.");
        heroSubtitle.getStyleClass().add("front-hero-subtitle");
        Label heroStats = new Label(buildFrontHeroStatsText());
        heroStats.getStyleClass().add("front-hero-stats");
        Button surpriseButton = new Button("Quiz surprise");
        surpriseButton.getStyleClass().add("front-card-button");
        surpriseButton.setOnAction(event -> openRandomFrontQuiz());
        heroContent.getChildren().addAll(heroTitle, heroSubtitle, heroStats, surpriseButton);
        hero.getChildren().add(heroContent);

        FlowPane facileContainer = new FlowPane();
        facileContainer.setHgap(22);
        facileContainer.setVgap(22);
        facileContainer.getStyleClass().add("front-cards-grid");

        FlowPane standardContainer = new FlowPane();
        standardContainer.setHgap(22);
        standardContainer.setVgap(22);
        standardContainer.getStyleClass().add("front-cards-grid");

        FilteredList<Quiz> filteredQuizzes = new FilteredList<>(quizController.getAllQuizzes(), quiz -> true);
        TextField searchField = createTextField("Rechercher un quiz...");
        searchField.setPrefWidth(340);
        ComboBox<String> sortCombo = createComboBox("Plus recent", "Titre A-Z", "Titre Z-A", "Niveau");
        sortCombo.setPrefWidth(180);
        Button searchButton = new Button("⌕");
        searchButton.getStyleClass().add("front-search-button");
        Button resetButton = new Button("Reinitialiser");
        resetButton.getStyleClass().add("front-reset-button");

        Label easyCountLabel = new Label();
        easyCountLabel.getStyleClass().add("front-count-label");
        Label standardCountLabel = new Label();
        standardCountLabel.getStyleClass().add("front-count-label");

        HBox toolsBar = new HBox(14);
        toolsBar.setAlignment(Pos.CENTER_LEFT);
        toolsBar.setPadding(new Insets(26, 32, 18, 32));
        toolsBar.getStyleClass().add("front-tools-bar");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolsBar.getChildren().addAll(searchField, searchButton, sortCombo, resetButton, spacer);

        installPlayfulButtonAnimations(accueilButton);
        installPlayfulButtonAnimations(quizButton);
        installPlayfulButtonAnimations(programmesButton);
        installPlayfulButtonAnimations(surpriseButton);
        installPlayfulButtonAnimations(searchButton);
        installPlayfulButtonAnimations(resetButton);

        VBox easySection = createFrontAudienceSection(
                "Quiz faciles 8-10 ans",
                "Des activites simples a comprendre, ideales pour vrai ou faux, relier ou choisir la bonne reponse.",
                facileContainer,
                easyCountLabel,
                "front-audience-section-easy"
        );
        VBox standardSection = createFrontAudienceSection(
                "Quiz 10 ans et plus",
                "Les quiz actuels complets restent disponibles ici pour les eleves plus avances.",
                standardContainer,
                standardCountLabel,
                "front-audience-section-standard"
        );

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredQuizzes.setPredicate(quiz -> quizController.matchesSearch(quiz, newValue));
            refreshFrontAudienceSections(facileContainer, standardContainer, filteredQuizzes, sortCombo.getValue(), easyCountLabel, standardCountLabel);
        });
        searchButton.setOnAction(event -> {
            filteredQuizzes.setPredicate(quiz -> quizController.matchesSearch(quiz, searchField.getText()));
            refreshFrontAudienceSections(facileContainer, standardContainer, filteredQuizzes, sortCombo.getValue(), easyCountLabel, standardCountLabel);
        });
        resetButton.setOnAction(event -> {
            searchField.clear();
            sortCombo.getSelectionModel().selectFirst();
            filteredQuizzes.setPredicate(quiz -> true);
            refreshFrontAudienceSections(facileContainer, standardContainer, filteredQuizzes, sortCombo.getValue(), easyCountLabel, standardCountLabel);
        });
        sortCombo.setOnAction(event -> refreshFrontAudienceSections(facileContainer, standardContainer, filteredQuizzes, sortCombo.getValue(), easyCountLabel, standardCountLabel));
        refreshFrontAudienceSections(facileContainer, standardContainer, filteredQuizzes, sortCombo.getValue(), easyCountLabel, standardCountLabel);

        FlowPane frontStatsRow = new FlowPane();
        frontStatsRow.setHgap(18);
        frontStatsRow.setVgap(18);
        frontStatsRow.getChildren().addAll(
                createFrontMetricCard("Quiz 8-10 ans", String.valueOf(quizController.getAllQuizzes().stream().filter(Quiz::isCategorieFacile).count())),
                createFrontMetricCard("Quiz 10+", String.valueOf(quizController.getAllQuizzes().stream().filter(quiz -> !quiz.isCategorieFacile()).count())),
                createFrontMetricCard("Questions", String.valueOf(questionController.getAllQuestions().size())),
                createFrontMetricCard("Niveaux", String.valueOf(quizController.getAllQuizzes().stream().map(Quiz::getNiveau).distinct().count()))
        );
        frontStatsRow.setPadding(new Insets(26, 32, 10, 32));
        frontStatsRow.getStyleClass().add("front-stats-grid");

        VBox audienceSections = new VBox(22, easySection, standardSection);
        audienceSections.setPadding(new Insets(0, 32, 32, 32));

        page.getChildren().addAll(navBar, hero, frontStatsRow, toolsBar, audienceSections);
        playStaggeredEntrance(navBar.getChildren(), 0, 70, -16);
        playStaggeredEntrance(heroContent.getChildren(), 120, 90, 22);
        playStaggeredEntrance(frontStatsRow.getChildren(), 220, 85, 28);
        playEntranceAnimation(toolsBar, 320, 0, 24, 0.97);
        playEntranceAnimation(easySection, 380, 0, 26, 0.97);
        playEntranceAnimation(standardSection, 470, 0, 26, 0.97);

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private VBox createSimpleStatCard(String label, String value) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("page-title");
        Label labelText = new Label(label);
        labelText.getStyleClass().add("page-subtitle");

        VBox box = new VBox(8, valueLabel, labelText);
        box.setPadding(new Insets(24));
        box.setPrefWidth(220);
        box.getStyleClass().add("card");
        return box;
    }

    private VBox createLightStatCard(String label, String value) {
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 26px; -fx-font-weight: 800;");
        Label labelText = new Label(label);
        labelText.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        VBox box = new VBox(8, valueLabel, labelText);
        box.setPadding(new Insets(16, 18, 16, 18));
        box.setPrefWidth(220);
        box.setStyle("""
                -fx-background-color: transparent;
                -fx-border-color: #343434;
                -fx-border-radius: 16px;
                -fx-background-radius: 16px;
                """);
        return box;
    }

    private PieChart createStatisticsPieChart(String[] labels, double[] values, String[] colors) {
        PieChart chart = new PieChart();
        chart.setLegendVisible(false);
        chart.setLabelsVisible(true);
        chart.setClockwise(true);
        chart.setStartAngle(90);
        chart.setPrefSize(380, 380);
        chart.setMinSize(380, 380);
        chart.setStyle("-fx-background-color: transparent;");

        double total = 0;
        for (double value : values) {
            total += value;
        }

        for (int i = 0; i < labels.length; i++) {
            double percent = total == 0 ? 0 : Math.round((values[i] * 100.0) / total);
            PieChart.Data data = new PieChart.Data(percent + " %", values[i]);
            final String color = colors[i];
            data.nodeProperty().addListener((observable, oldNode, node) -> {
                if (node != null) {
                    node.setStyle("-fx-pie-color: " + color + ";");
                }
            });
            chart.getData().add(data);
        }

        return chart;
    }

    private VBox createStatisticsLegend(String title, String[] labels, String[] colors) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: 800;");

        VBox items = new VBox(14);
        for (int i = 0; i < labels.length; i++) {
            Region swatch = new Region();
            swatch.setPrefSize(38, 38);
            swatch.setMinSize(38, 38);
            swatch.setStyle("-fx-background-color: " + colors[i] + ";");

            Label text = new Label(labels[i]);
            text.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: 600;");

            HBox row = new HBox(14, swatch, text);
            row.setAlignment(Pos.CENTER_LEFT);
            items.getChildren().add(row);
        }

        VBox legendBox = new VBox(18, titleLabel, items);
        legendBox.setAlignment(Pos.TOP_CENTER);
        legendBox.setPadding(new Insets(18, 20, 18, 20));
        legendBox.setPrefWidth(230);
        legendBox.setStyle("""
                -fx-background-color: transparent;
                -fx-border-color: #4a4a4a;
                -fx-border-width: 2px;
                -fx-padding: 18px;
                """);
        return legendBox;
    }

    private void clearSidebarSelection() {
        listQuizButton.getStyleClass().remove("sidebar-button-active");
        addQuizButton.getStyleClass().remove("sidebar-button-active");
        listQuestionButton.getStyleClass().remove("sidebar-button-active");
        addQuestionButton.getStyleClass().remove("sidebar-button-active");
        statsButton.getStyleClass().remove("sidebar-button-active");
    }

    private void refreshQuizTable(TableView<Quiz> table, FilteredList<Quiz> filteredQuizzes, String sortValue) {
        List<Quiz> items = new ArrayList<>(filteredQuizzes);
        items.sort(quizController.getBackOfficeComparator(sortValue));
        table.setItems(FXCollections.observableArrayList(items));
    }

    private void refreshQuestionTable(TableView<Question> table, FilteredList<Question> filteredQuestions, String sortValue) {
        List<Question> items = new ArrayList<>(filteredQuestions);
        items.sort(questionController.getComparator(sortValue));
        table.setItems(FXCollections.observableArrayList(items));
    }

    private boolean matchesQuizFilters(Quiz quiz, String searchValue, String niveauValue, String categorieAgeValue, String statutValue) {
        boolean searchMatches = quizController.matchesSearch(quiz, searchValue);
        boolean niveauMatches = niveauValue == null
                || "Tous les niveaux".equalsIgnoreCase(niveauValue)
                || quiz.getNiveau().equalsIgnoreCase(niveauValue);
        boolean categorieMatches = categorieAgeValue == null
                || "Toutes les categories".equalsIgnoreCase(categorieAgeValue)
                || quiz.getCategorieAge().equalsIgnoreCase(categorieAgeValue);
        boolean statutMatches = statutValue == null
                || "Tous les statuts".equalsIgnoreCase(statutValue)
                || quiz.getStatut().equalsIgnoreCase(statutValue);
        return searchMatches && niveauMatches && categorieMatches && statutMatches;
    }

    private boolean matchesQuestionFilters(Question question, String searchValue, String quizValue, String typeValue) {
        boolean searchMatches = questionController.matchesSearch(question, searchValue);
        boolean quizMatches = quizValue == null
                || "Tous les quiz".equalsIgnoreCase(quizValue)
                || question.getQuizTitre().equalsIgnoreCase(quizValue);
        boolean typeMatches = typeValue == null
                || "Tous les types".equalsIgnoreCase(typeValue)
                || question.getTypeLabel().equalsIgnoreCase(typeValue);
        return searchMatches && quizMatches && typeMatches;
    }

    private VBox createCard(String title, Region... rows) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        Region accent = new Region();
        accent.getStyleClass().add("card-accent");
        accent.setPrefWidth(4);
        accent.setMinWidth(4);

        HBox heading = new HBox(14, accent, titleLabel);
        heading.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(18);
        content.getChildren().add(heading);
        content.getChildren().addAll(rows);
        content.setPadding(new Insets(24));
        content.getStyleClass().add("card");
        return content;
    }

    private HBox createFormRow(String labelText, Region field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        label.setMinWidth(190);

        VBox fieldBox = new VBox(6, field);
        if (field instanceof TextArea) {
            Label helper = new Label("Minimum 10 caracteres");
            helper.getStyleClass().add("helper-label");
            fieldBox.getChildren().add(helper);
        }

        HBox row = new HBox(18, label, fieldBox);
        row.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(fieldBox, Priority.ALWAYS);
        return row;
    }

    private TextField createTextField(String prompt) {
        TextField textField = new TextField();
        textField.setPromptText(prompt);
        textField.getStyleClass().add("app-field");
        return textField;
    }

    private TextArea createTextArea(String prompt, int rows) {
        TextArea textArea = new TextArea();
        textArea.setPromptText(prompt);
        textArea.setPrefRowCount(rows);
        textArea.setWrapText(true);
        textArea.getStyleClass().add("app-field");
        return textArea;
    }

    private Label createErrorLabel() {
        Label label = new Label();
        label.getStyleClass().add("error-label");
        label.setVisible(false);
        label.setManaged(false);
        return label;
    }

    private void installMaxLengthFormatter(TextField textField, int maxLength) {
        textField.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= maxLength ? change : null));
    }

    private void installMaxLengthFormatter(TextArea textArea, int maxLength) {
        textArea.setTextFormatter(new TextFormatter<String>(change ->
                change.getControlNewText().length() <= maxLength ? change : null));
    }

    private ComboBox<String> createComboBox(String... values) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(values);
        comboBox.getSelectionModel().selectFirst();
        comboBox.getStyleClass().add("app-combo");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        return comboBox;
    }

    private ComboBox<String> createFilterComboBox(String defaultValue, List<String> values) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.getItems().add(defaultValue);
        comboBox.getItems().addAll(values);
        comboBox.getSelectionModel().selectFirst();
        comboBox.getStyleClass().add("app-combo");
        comboBox.setMaxWidth(Double.MAX_VALUE);
        return comboBox;
    }

    private VBox createFrontAudienceSection(String title, String subtitle, FlowPane container, Label countLabel, String styleClass) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-title");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("page-subtitle");
        subtitleLabel.setWrapText(true);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(14, new VBox(6, titleLabel, subtitleLabel), spacer, countLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox section = new VBox(18, header, container);
        section.setPadding(new Insets(24));
        section.getStyleClass().addAll("card", "front-audience-section", styleClass);
        return section;
    }

    private void refreshFrontAudienceSections(
            FlowPane facileContainer,
            FlowPane standardContainer,
            FilteredList<Quiz> filteredQuizzes,
            String sortValue,
            Label easyCountLabel,
            Label standardCountLabel
    ) {
        List<Quiz> items = new ArrayList<>(filteredQuizzes);
        items.sort(quizController.getFrontOfficeComparator(sortValue));
        List<Quiz> easyItems = items.stream().filter(Quiz::isCategorieFacile).toList();
        List<Quiz> standardItems = items.stream().filter(quiz -> !quiz.isCategorieFacile()).toList();

        refreshFrontQuizCardsForCategory(facileContainer, easyItems, easyCountLabel, "Aucun quiz facile 8-10 ans ne correspond a votre recherche.");
        refreshFrontQuizCardsForCategory(standardContainer, standardItems, standardCountLabel, "Aucun quiz 10 ans et plus ne correspond a votre recherche.");
    }

    private void refreshFrontQuizCardsForCategory(FlowPane container, List<Quiz> items, Label countLabel, String emptyMessage) {
        container.getChildren().clear();
        if (items.isEmpty()) {
            VBox emptyState = createFrontEmptyState(emptyMessage);
            container.getChildren().add(emptyState);
            playEntranceAnimation(emptyState, 60, 0, 24, 0.96);
            countLabel.setText("0 quiz");
            return;
        }

        for (int index = 0; index < items.size(); index++) {
            VBox card = createFrontQuizCard(items.get(index));
            container.getChildren().add(card);
            playEntranceAnimation(card, index * 90.0, 0, 30, 0.94);
        }
        countLabel.setText(items.size() + " quiz");
    }

    private String buildFrontHeroStatsText() {
        return quizController.buildFrontHeroStatsText(questionController.getAllQuestions().size());
    }

    private void openRandomFrontQuiz() {
        List<Quiz> publishedQuizzes = quizController.getAllQuizzes().stream()
                .filter(quiz -> "Publie".equalsIgnoreCase(quiz.getStatut()))
                .toList();
        List<Quiz> source = publishedQuizzes.isEmpty() ? new ArrayList<>(quizController.getAllQuizzes()) : publishedQuizzes;
        if (source.isEmpty()) {
            showInfo("Quiz surprise", "Aucun quiz n'est disponible pour le moment.");
            return;
        }

        int randomIndex = (int) (Math.random() * source.size());
        showFrontQuizDetails(source.get(randomIndex));
    }

    private Node createQuizImageNode(Quiz quiz) {
        if (quiz.getImageUrl() != null && !quiz.getImageUrl().isBlank()) {
            try {
                Image image = new Image(resolveQuizImageSource(quiz.getImageUrl()), true);
                if (!image.isError()) {
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(268);
                    imageView.setFitHeight(160);
                    imageView.setPreserveRatio(false);
                    imageView.setSmooth(true);

                    StackPane wrapper = new StackPane(imageView);
                    wrapper.setPrefSize(268, 160);
                    wrapper.setMaxSize(268, 160);
                    wrapper.setStyle("-fx-background-radius: 14px; -fx-border-radius: 14px;");
                    return wrapper;
                }
            } catch (Exception ignored) {
            }
        }

        Region imagePlaceholder = new Region();
        imagePlaceholder.getStyleClass().add("front-card-image");
        imagePlaceholder.setPrefSize(268, 160);
        return imagePlaceholder;
    }

    private String resolveQuizImageSource(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }

        String value = rawValue.trim();
        String normalizedValue = value.toLowerCase();
        if (normalizedValue.startsWith("http://")
                || normalizedValue.startsWith("https://")
                || normalizedValue.startsWith("file:/")
                || normalizedValue.startsWith("jar:")
                || normalizedValue.startsWith("data:")) {
            return value;
        }

        try {
            Path path = Path.of(value);
            if (Files.exists(path)) {
                return path.toAbsolutePath().toUri().toString();
            }
        } catch (Exception ignored) {
        }
        return value;
    }

    private VBox createFrontQuizCard(Quiz quiz) {
        VBox card = new VBox(12);
        card.setPrefWidth(300);
        card.getStyleClass().add("front-quiz-card");
        installCardHoverAnimation(card);

        Node imageNode = createQuizImageNode(quiz);

        Label meta = new Label(quiz.getNiveau() + " • " + quiz.getNombreQuestions() + " questions");
        meta.getStyleClass().add("front-card-meta");

        Label title = new Label(quiz.getTitre());
        title.getStyleClass().add("front-card-title");
        title.setWrapText(true);

        Label description = new Label(quiz.getDescription());
        description.getStyleClass().add("front-card-description");
        description.setWrapText(true);

        Label status = new Label(quiz.getStatut());
        status.getStyleClass().add("front-card-badge");

        Label extra = new Label("Duree: " + quiz.getDureeMinutes() + " min • Score minimum: " + quiz.getScoreMinimum() + "%");
        extra.getStyleClass().add("front-card-extra");

        Button detailButton = new Button("Voir le quiz");
        detailButton.getStyleClass().add("front-card-button");
        detailButton.setOnAction(event -> showFrontQuizDetails(quiz));
        Button noteButton = new Button("Note");
        noteButton.getStyleClass().add("front-card-button");
        noteButton.setOnAction(event -> {
            event.consume();
            showQuizNote(quiz);
        });
        HBox actionRow = new HBox(10, detailButton, noteButton);
        installPlayfulButtonAnimations(detailButton);
        installPlayfulButtonAnimations(noteButton);
        card.setOnMouseClicked(event -> showFrontQuizDetails(quiz));

        card.getChildren().addAll(imageNode, meta, title, description, status, extra, actionRow);
        return card;
    }

    private VBox buildQuizPreviewCard() {
        Label sectionTitle = new Label("Apercu en direct");
        sectionTitle.getStyleClass().add("card-title");
        Label sectionSubtitle = new Label("Visualisez tout de suite l'apparence generale du quiz dans le front office.");
        sectionSubtitle.getStyleClass().add("page-subtitle");

        StackPane imageHolder = new StackPane();
        Label metaLabel = new Label();
        metaLabel.getStyleClass().add("front-card-meta");
        Label titleLabel = new Label();
        titleLabel.getStyleClass().add("front-card-title");
        titleLabel.setWrapText(true);
        Label descriptionLabel = new Label();
        descriptionLabel.getStyleClass().add("front-card-description");
        descriptionLabel.setWrapText(true);
        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("front-card-badge");
        Label extraLabel = new Label();
        extraLabel.getStyleClass().add("front-card-extra");
        Label teaserLabel = new Label();
        teaserLabel.getStyleClass().add("page-subtitle");
        teaserLabel.setWrapText(true);

        VBox previewBody = new VBox(12, imageHolder, metaLabel, titleLabel, descriptionLabel, statusLabel, extraLabel, teaserLabel);
        previewBody.getStyleClass().add("front-quiz-card");

        Runnable refreshPreview = () -> {
            Quiz previewQuiz = buildPreviewQuizSnapshot();
            imageHolder.getChildren().setAll(createQuizImageNode(previewQuiz));
            metaLabel.setText(previewQuiz.getNiveau() + " • " + previewQuiz.getNombreQuestions() + " questions");
            titleLabel.setText(previewQuiz.getTitre().isBlank() ? "Titre du quiz" : previewQuiz.getTitre());
            descriptionLabel.setText(previewQuiz.getDescription().isBlank() ? "La description du quiz apparaitra ici." : previewQuiz.getDescription());
            statusLabel.setText(previewQuiz.getStatut());
            extraLabel.setText("Duree: " + previewQuiz.getDureeMinutes() + " min • Score minimum: " + previewQuiz.getScoreMinimum() + "%");
            teaserLabel.setText(buildPreviewTeaser(previewQuiz));
        };

        titreField.textProperty().addListener((observable, oldValue, newValue) -> refreshPreview.run());
        imageUrlField.textProperty().addListener((observable, oldValue, newValue) -> refreshPreview.run());
        descriptionArea.textProperty().addListener((observable, oldValue, newValue) -> refreshPreview.run());
        niveauCombo.valueProperty().addListener((observable, oldValue, newValue) -> refreshPreview.run());
        categorieAgeCombo.valueProperty().addListener((observable, oldValue, newValue) -> refreshPreview.run());
        questionSpinner.valueProperty().addListener((observable, oldValue, newValue) -> refreshPreview.run());
        dureeSpinner.valueProperty().addListener((observable, oldValue, newValue) -> refreshPreview.run());
        scoreSpinner.valueProperty().addListener((observable, oldValue, newValue) -> refreshPreview.run());
        statutCombo.valueProperty().addListener((observable, oldValue, newValue) -> refreshPreview.run());
        refreshPreview.run();

        VBox card = new VBox(16, sectionTitle, sectionSubtitle, previewBody);
        card.setPadding(new Insets(24));
        card.getStyleClass().addAll("card", "content-card");
        return card;
    }

    private Quiz buildPreviewQuizSnapshot() {
        return new Quiz(
                titreField == null ? "" : titreField.getText().trim(),
                descriptionArea == null ? "" : descriptionArea.getText().trim(),
                imageUrlField == null ? "" : imageUrlField.getText().trim(),
                niveauCombo == null || niveauCombo.getValue() == null ? "Debutant" : niveauCombo.getValue(),
                categorieAgeCombo == null || categorieAgeCombo.getValue() == null ? Quiz.CATEGORIE_AGE_STANDARD : categorieAgeCombo.getValue(),
                questionSpinner == null ? 0 : questionSpinner.getValue(),
                dureeSpinner == null ? 20 : dureeSpinner.getValue(),
                scoreSpinner == null ? 60 : scoreSpinner.getValue(),
                statutCombo == null || statutCombo.getValue() == null ? "Brouillon" : statutCombo.getValue()
        );
    }

    private String buildPreviewTeaser(Quiz quiz) {
        if (quiz.isCategorieFacile()) {
            return "Ce quiz sera range dans l'espace 8-10 ans avec des formats simples comme vrai ou faux.";
        }
        if ("Publie".equalsIgnoreCase(quiz.getStatut())) {
            return "Ce quiz est pret a etre visible immediatement dans le front office.";
        }
        if ("Archive".equalsIgnoreCase(quiz.getStatut())) {
            return "Le quiz sera conserve en archive et non propose en priorite aux eleves.";
        }
        return "Le quiz reste en brouillon tant que vous ne le publiez pas.";
    }

    private ScrollPane buildFrontQuizDetailsPage(Quiz quiz) {
        VBox page = new VBox();
        page.getStyleClass().add("front-page");

        HBox navBar = new HBox(18);
        navBar.setPadding(new Insets(18, 32, 18, 32));
        navBar.getStyleClass().add("front-nav");

        Button accueilButton = new Button("Accueil");
        Button quizButton = new Button("Quiz");
        Button programmesButton = new Button("Programmes");
        accueilButton.getStyleClass().add("front-tab");
        quizButton.getStyleClass().addAll("front-tab", "front-tab-active");
        programmesButton.getStyleClass().add("front-tab");
        accueilButton.setOnAction(event -> showFrontOfficePage());
        quizButton.setOnAction(event -> showFrontOfficePage());
        programmesButton.setOnAction(event -> showInfo("Programmes", "La page Programmes peut etre ajoutee ensuite."));
        navBar.getChildren().addAll(accueilButton, quizButton, programmesButton);
        installPlayfulButtonAnimations(accueilButton);
        installPlayfulButtonAnimations(quizButton);
        installPlayfulButtonAnimations(programmesButton);

        StackPane hero = new StackPane();
        hero.setPadding(new Insets(36, 32, 36, 32));
        hero.setMinHeight(238);
        hero.getStyleClass().addAll("front-hero", "front-detail-hero");
        decorateKidHero(hero);
        Label backBreadcrumb = new Label("Accueil • Quiz • " + quiz.getTitre());
        backBreadcrumb.getStyleClass().add("front-hero-subtitle");
        Label heroTitle = new Label(quiz.getTitre());
        heroTitle.getStyleClass().add("front-hero-title");
        Label heroSubtitle = new Label(quiz.getDescription());
        heroSubtitle.getStyleClass().add("front-detail-description");
        ProgressIndicator timerProgress = new ProgressIndicator(1.0);
        Label timerValueLabel = new Label(formatRemainingTime(quiz.getDureeMinutes() * 60));
        StackPane timerCircle = createTimerCircle(timerProgress, timerValueLabel);
        Button backButton = new Button("Retour aux quiz");
        backButton.getStyleClass().add("front-card-button");
        backButton.setOnAction(event -> showFrontOfficePage());
        Button noteButton = new Button("Note");
        noteButton.getStyleClass().add("front-card-button");
        noteButton.setOnAction(event -> showQuizNote(quiz));

        HBox heroActionRow = new HBox(10, backButton, noteButton);
        heroActionRow.getStyleClass().add("front-detail-action-row");
        VBox heroTextBox = new VBox(10, backBreadcrumb, heroTitle, heroSubtitle, heroActionRow);
        HBox heroRow = new HBox(30, heroTextBox, timerCircle);
        HBox.setHgrow(heroTextBox, Priority.ALWAYS);
        heroRow.setAlignment(Pos.CENTER_LEFT);
        hero.getChildren().add(heroRow);
        StackPane.setAlignment(heroRow, Pos.CENTER_LEFT);
        installPlayfulButtonAnimations(backButton);
        installPlayfulButtonAnimations(noteButton);
        installBreathingPulse(timerCircle, 1.0, 1.04, 2.3);

        HBox infoRow = new HBox(
                18,
                createFrontMetricCard("Categorie", quiz.getCategorieAge()),
                createFrontMetricCard("Niveau", quiz.getNiveau()),
                createFrontMetricCard("Questions", String.valueOf(quiz.getNombreQuestions())),
                createFrontMetricCard("Duree", quiz.getDureeMinutes() + " min"),
                createFrontMetricCard("Score", quiz.getScoreMinimum() + "%")
        );
        infoRow.setPadding(new Insets(26, 32, 8, 32));
        infoRow.getStyleClass().add("front-detail-metrics");

        VBox detailsCard = new VBox(16);
        detailsCard.setPadding(new Insets(24));
        detailsCard.getStyleClass().addAll("card", "front-detail-card");
        Label detailsTitle = new Label("Questions du quiz");
        detailsTitle.getStyleClass().add("page-title");
        Label detailsText = new Label("Statut: " + quiz.getStatut() + " • Parcourez les questions disponibles ci-dessous.");
        detailsText.getStyleClass().add("page-subtitle");
        VBox detailHeader = new VBox(6, detailsTitle, detailsText);
        detailHeader.getStyleClass().add("front-detail-header");
        detailsCard.getChildren().add(detailHeader);

        List<Question> quizQuestions = questionController.getQuestionsForQuiz(quiz);

        Map<Question, TextField> freeTextAnswers = new HashMap<>();
        Map<Question, List<CheckBox>> qcmAnswers = new HashMap<>();
        Map<Question, ToggleGroup> qcuAnswers = new HashMap<>();
        Map<Question, Map<String, String>> matchingAnswers = new HashMap<>();

        if (quizQuestions.isEmpty()) {
            Label emptyLabel = new Label("Aucune question n'est encore associee a ce quiz.");
            emptyLabel.getStyleClass().add("front-empty-title");
            detailsCard.getChildren().add(emptyLabel);
        } else {
            Map<Question, VBox> questionViews = new HashMap<>();
            Runnable[] refreshProgressRef = new Runnable[1];
            for (int i = 0; i < quizQuestions.size(); i++) {
                Question question = quizQuestions.get(i);
                questionViews.put(question, createFrontQuestionPreview(
                        question,
                        i + 1,
                        quizQuestions.size(),
                        freeTextAnswers,
                        qcmAnswers,
                        qcuAnswers,
                        matchingAnswers,
                        () -> {
                            if (refreshProgressRef[0] != null) {
                                refreshProgressRef[0].run();
                            }
                        }
                ));
            }

            Label currentQuestionLabel = new Label();
            currentQuestionLabel.getStyleClass().add("front-question-stage-label");
            Label progressSummaryLabel = new Label();
            progressSummaryLabel.getStyleClass().add("front-progress-summary");
            FlowPane progressTrack = new FlowPane();
            progressTrack.setHgap(10);
            progressTrack.setVgap(10);
            progressTrack.getStyleClass().add("front-progress-track");
            VBox questionHost = new VBox();
            questionHost.getStyleClass().add("front-question-host");
            HBox navigationBar = new HBox(14);
            navigationBar.setAlignment(Pos.CENTER_LEFT);
            navigationBar.getStyleClass().add("front-question-navigation");
            List<OpenAiChatService.ChatTurn> chatHistory = new ArrayList<>();
            int[] lastChatQuestionIndex = {-1};
            VBox[] chatbotCardRef = new VBox[1];

            Button previousButton = new Button("Précédent");
            previousButton.getStyleClass().add("secondary-action");
            Button nextButton = new Button("Suivant");
            nextButton.getStyleClass().add("front-card-button");
            Button validateQuizButton = new Button("Valider le quiz");
            validateQuizButton.getStyleClass().add("front-validate-button");
            validateQuizButton.setVisible(false);
            validateQuizButton.setManaged(false);
            installPlayfulButtonAnimations(previousButton);
            installPlayfulButtonAnimations(nextButton);
            installPlayfulButtonAnimations(validateQuizButton);

            int[] currentQuestionIndex = {0};
            Runnable[] renderCurrentQuestionRef = new Runnable[1];
            renderCurrentQuestionRef[0] = () -> {
                Question currentQuestionView = quizQuestions.get(currentQuestionIndex[0]);
                VBox currentView = questionViews.get(currentQuestionView);
                questionHost.getChildren().setAll(currentView);
                playEntranceAnimation(currentView, 0, 24, 0, 0.98);
                currentQuestionLabel.setText("Question " + (currentQuestionIndex[0] + 1) + " / " + quizQuestions.size());
                previousButton.setDisable(currentQuestionIndex[0] == 0);
                boolean isLastQuestion = currentQuestionIndex[0] == quizQuestions.size() - 1;
                nextButton.setVisible(!isLastQuestion);
                nextButton.setManaged(!isLastQuestion);
                validateQuizButton.setVisible(isLastQuestion);
                validateQuizButton.setManaged(isLastQuestion);
                if (refreshProgressRef[0] != null) {
                    refreshProgressRef[0].run();
                }
                if (chatbotCardRef[0] != null && lastChatQuestionIndex[0] != currentQuestionIndex[0]) {
                    resetChatbotForQuestion(
                            chatbotCardRef[0],
                            chatHistory,
                            currentQuestionView,
                            currentQuestionIndex[0] + 1,
                            quizQuestions.size()
                    );
                    lastChatQuestionIndex[0] = currentQuestionIndex[0];
                }
            };
            refreshProgressRef[0] = () -> refreshQuizProgressTrack(
                    progressTrack,
                    progressSummaryLabel,
                    quizQuestions,
                    currentQuestionIndex,
                    renderCurrentQuestionRef[0],
                    freeTextAnswers,
                    qcmAnswers,
                    qcuAnswers,
                    matchingAnswers
            );

            Button chatbotToggleButton = new Button("AI");
            chatbotToggleButton.getStyleClass().add("chatbot-toggle-button");
            installPlayfulButtonAnimations(chatbotToggleButton);
            Runnable[] hideChatbotRef = new Runnable[1];
            VBox chatbotCard = buildQuizChatbotCard(
                    quiz,
                    quizQuestions,
                    currentQuestionIndex,
                    chatHistory,
                    freeTextAnswers,
                    qcmAnswers,
                    qcuAnswers,
                    matchingAnswers,
                    () -> {
                        if (hideChatbotRef[0] != null) {
                            hideChatbotRef[0].run();
                        }
                    }
            );
            chatbotCardRef[0] = chatbotCard;
            chatbotCard.setVisible(false);
            chatbotCard.setManaged(false);
            hideChatbotRef[0] = () -> {
                chatbotCard.setVisible(false);
                chatbotCard.setManaged(false);
                chatbotToggleButton.setVisible(true);
                chatbotToggleButton.setManaged(true);
                chatbotToggleButton.setDisable(false);
                chatbotToggleButton.toFront();
            };
            chatbotToggleButton.setOnAction(event -> {
                boolean showChatbot = !chatbotCard.isVisible();
                chatbotCard.setVisible(showChatbot);
                chatbotCard.setManaged(showChatbot);
                if (showChatbot) {
                    chatbotToggleButton.setVisible(false);
                    chatbotToggleButton.setManaged(false);
                    chatbotToggleButton.setDisable(true);
                    chatbotCard.toFront();
                    playEntranceAnimation(chatbotCard, 0, 26, 0, 0.96);
                } else if (hideChatbotRef[0] != null) {
                    hideChatbotRef[0].run();
                }
            });

            previousButton.setOnAction(event -> {
                if (currentQuestionIndex[0] > 0) {
                    currentQuestionIndex[0]--;
                    renderCurrentQuestionRef[0].run();
                }
            });
            nextButton.setOnAction(event -> {
                if (currentQuestionIndex[0] < quizQuestions.size() - 1) {
                    currentQuestionIndex[0]++;
                    renderCurrentQuestionRef[0].run();
                }
            });
            validateQuizButton.setOnAction(event -> finishQuizAttempt(
                    quiz,
                    quizQuestions,
                    freeTextAnswers,
                    qcmAnswers,
                    qcuAnswers,
                    matchingAnswers,
                    validateQuizButton,
                    navigationBar,
                    questionHost,
                    chatbotCard,
                    false
            ));

            navigationBar.getChildren().addAll(previousButton, nextButton, validateQuizButton);
            renderCurrentQuestionRef[0].run();

            VBox stageHeader = new VBox(8, currentQuestionLabel, progressSummaryLabel, progressTrack);
            stageHeader.getStyleClass().add("front-question-stage-header");

            VBox quizFlow = new VBox(18, stageHeader, questionHost, navigationBar);
            quizFlow.getStyleClass().add("front-quiz-flow");
            HBox.setHgrow(quizFlow, Priority.ALWAYS);
            quizFlow.setMaxWidth(Double.MAX_VALUE);

            StackPane quizArea = new StackPane(quizFlow, chatbotCard, chatbotToggleButton);
            quizArea.getStyleClass().add("front-quiz-area");
            StackPane.setAlignment(chatbotCard, Pos.TOP_RIGHT);
            StackPane.setMargin(chatbotCard, new Insets(0, 0, 0, 24));
            StackPane.setAlignment(chatbotToggleButton, Pos.BOTTOM_RIGHT);
            StackPane.setMargin(chatbotToggleButton, new Insets(0, 0, 6, 0));
            detailsCard.getChildren().add(quizArea);
            startQuizTimer(
                    quiz,
                    timerProgress,
                    timerValueLabel,
                    validateQuizButton,
                    navigationBar,
                    questionHost,
            chatbotCard,
            quizQuestions,
            freeTextAnswers,
            qcmAnswers,
            qcuAnswers,
            matchingAnswers
            );
        }

        VBox wrapper = new VBox(navBar, hero, infoRow, detailsCard);
        VBox.setMargin(detailsCard, new Insets(18, 32, 32, 32));
        playStaggeredEntrance(navBar.getChildren(), 0, 70, -16);
        playStaggeredEntrance(List.of(backBreadcrumb, heroTitle, heroSubtitle, heroActionRow, timerCircle), 110, 85, 20);
        playStaggeredEntrance(infoRow.getChildren(), 220, 70, 24);
        playEntranceAnimation(detailsCard, 320, 0, 28, 0.97);

        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private VBox buildQuizChatbotCard(
            Quiz quiz,
            List<Question> quizQuestions,
            int[] currentQuestionIndex,
            List<OpenAiChatService.ChatTurn> chatHistory,
            Map<Question, TextField> freeTextAnswers,
            Map<Question, List<CheckBox>> qcmAnswers,
            Map<Question, ToggleGroup> qcuAnswers,
            Map<Question, Map<String, String>> matchingAnswers,
            Runnable onCloseChatbot
    ) {
        Label title = new Label("Chatbot quiz");
        title.getStyleClass().add("chatbot-title");

        Label subtitle = new Label("Pose une question pendant le quiz pour recevoir une reponse verifiee a partir de la correction enregistree.");
        subtitle.getStyleClass().add("chatbot-subtitle");
        subtitle.setWrapText(true);

        Label chatHint = new Label("Tu peux demander la bonne reponse, une explication simple, une methode ou verifier ta reponse actuelle.");
        chatHint.getStyleClass().add("chatbot-helper");
        chatHint.setWrapText(true);

        Label apiStatusLabel = new Label("Reponses verifiees par le quiz");
        apiStatusLabel.getStyleClass().addAll(
                "chatbot-api-status",
                "chatbot-api-status-ready"
        );

        TextArea transcriptArea = new TextArea();
        transcriptArea.setEditable(false);
        transcriptArea.setWrapText(true);
        transcriptArea.setPrefRowCount(12);
        transcriptArea.getStyleClass().addAll("app-field", "chatbot-transcript");
        transcriptArea.setText("EduKids Coach: Bonjour, ecris ta question et je te repondrai avec la correction de la question actuelle du quiz.");

        TextArea inputArea = new TextArea();
        inputArea.setPromptText("Exemple: pourquoi cette reponse est correcte ?");
        inputArea.setWrapText(true);
        inputArea.setPrefRowCount(3);
        inputArea.getStyleClass().addAll("app-field", "chatbot-input");

        Label statusLabel = new Label("Le coach repond a partir de la question affichee et de sa correction.");
        statusLabel.getStyleClass().add("chatbot-status");
        statusLabel.setWrapText(true);

        Button sendButton = new Button("Envoyer");
        sendButton.getStyleClass().add("front-card-button");

        Button hintButton = new Button("Indice");
        hintButton.getStyleClass().add("chatbot-quick-button");
        Button explainButton = new Button("Expliquer");
        explainButton.getStyleClass().add("chatbot-quick-button");
        Button methodButton = new Button("Methode");
        methodButton.getStyleClass().add("chatbot-quick-button");

        Runnable unlockChatControls = () -> {
            sendButton.setDisable(false);
            inputArea.setDisable(false);
            hintButton.setDisable(false);
            explainButton.setDisable(false);
            methodButton.setDisable(false);
            inputArea.requestFocus();
            inputArea.positionCaret(inputArea.getLength());
        };

        Runnable sendMessage = () -> {
            String userMessage = inputArea.getText() == null ? "" : inputArea.getText().trim();
            if (userMessage.isBlank()) {
                statusLabel.setText("Ecris un message avant d'envoyer.");
                return;
            }
            if (quizQuestions.isEmpty() || currentQuestionIndex[0] < 0 || currentQuestionIndex[0] >= quizQuestions.size()) {
                statusLabel.setText("Aucune question active n'est disponible pour le moment.");
                inputArea.clear();
                return;
            }

            Question currentQuestion = quizQuestions.get(currentQuestionIndex[0]);
            String studentAnswer = getCurrentStudentAnswer(currentQuestion, freeTextAnswers, qcmAnswers, qcuAnswers, matchingAnswers);
            transcriptArea.appendText("\n\nToi: " + userMessage);
            inputArea.clear();
            String verifiedResponse = openAiChatService.buildFallbackAssistantReply(
                    quiz,
                    currentQuestion,
                    currentQuestionIndex[0] + 1,
                    quizQuestions.size(),
                    studentAnswer,
                    userMessage
            );
            chatHistory.add(new OpenAiChatService.ChatTurn("User", userMessage));
            chatHistory.add(new OpenAiChatService.ChatTurn("Assistant", verifiedResponse));
            transcriptArea.appendText("\n\nEduKids Coach: " + verifiedResponse);
            statusLabel.setText("Reponse verifiee affichee pour la question " + (currentQuestionIndex[0] + 1) + ".");
            unlockChatControls.run();
        };

        sendButton.setOnAction(event -> sendMessage.run());
        hintButton.setOnAction(event -> {
            inputArea.setText("Donne-moi un indice sans me donner toute la reponse.");
            sendMessage.run();
        });
        explainButton.setOnAction(event -> {
            inputArea.setText("Explique-moi cette question simplement.");
            sendMessage.run();
        });
        methodButton.setOnAction(event -> {
            inputArea.setText("Quelle methode je dois utiliser pour trouver la reponse ?");
            sendMessage.run();
        });

        HBox actionRow = new HBox(sendButton);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        FlowPane quickActions = new FlowPane();
        quickActions.setHgap(8);
        quickActions.setVgap(8);
        quickActions.getChildren().addAll(hintButton, explainButton, methodButton);

        Button closeButton = new Button("Fermer");
        closeButton.getStyleClass().add("chatbot-close-button");
        closeButton.setOnAction(event -> {
            if (onCloseChatbot != null) {
                onCloseChatbot.run();
            }
        });
        installPlayfulButtonAnimations(sendButton);
        installPlayfulButtonAnimations(hintButton);
        installPlayfulButtonAnimations(explainButton);
        installPlayfulButtonAnimations(methodButton);
        installPlayfulButtonAnimations(closeButton);

        HBox headerRow = new HBox(title, new Region(), closeButton);
        HBox.setHgrow(headerRow.getChildren().get(1), Priority.ALWAYS);

        VBox card = new VBox(12, headerRow, subtitle, apiStatusLabel, chatHint, quickActions, transcriptArea, inputArea, actionRow, statusLabel);
        card.getStyleClass().add("chatbot-card");
        card.setPrefWidth(350);
        card.setMinWidth(320);
        card.getProperties().put("chatTranscriptArea", transcriptArea);
        card.getProperties().put("chatStatusLabel", statusLabel);
        card.getProperties().put("chatInputArea", inputArea);
        return card;
    }

    private void resetChatbotForQuestion(
            VBox chatbotCard,
            List<OpenAiChatService.ChatTurn> chatHistory,
            Question currentQuestion,
            int questionNumber,
            int totalQuestions
    ) {
        if (chatbotCard == null || currentQuestion == null) {
            return;
        }
        chatHistory.clear();

        Object transcriptNode = chatbotCard.getProperties().get("chatTranscriptArea");
        if (transcriptNode instanceof TextArea transcriptArea) {
            transcriptArea.setText(
                    "EduKids Coach: Bonjour, je suis maintenant synchronise sur la question "
                            + questionNumber
                            + " / "
                            + totalQuestions
                            + ". Pose-moi ta question sur : "
                            + currentQuestion.getIntitule()
            );
            transcriptArea.positionCaret(transcriptArea.getLength());
        }

        Object statusNode = chatbotCard.getProperties().get("chatStatusLabel");
        if (statusNode instanceof Label statusLabel) {
            statusLabel.setText("Le coach IA suit maintenant la question " + questionNumber + " / " + totalQuestions + ".");
        }

        Object inputNode = chatbotCard.getProperties().get("chatInputArea");
        if (inputNode instanceof TextArea inputArea) {
            inputArea.clear();
        }
    }

    private VBox createFrontMetricCard(String label, String value) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("front-metric-value");
        Label labelLabel = new Label(label);
        labelLabel.getStyleClass().add("front-metric-label");
        VBox card = new VBox(8, valueLabel, labelLabel);
        card.setPrefWidth(200);
        card.getStyleClass().add("front-metric-card");
        installCardHoverAnimation(card);
        return card;
    }

    private void refreshQuizProgressTrack(
            FlowPane progressTrack,
            Label progressSummaryLabel,
            List<Question> quizQuestions,
            int[] currentQuestionIndex,
            Runnable renderCurrentQuestion,
            Map<Question, TextField> freeTextAnswers,
            Map<Question, List<CheckBox>> qcmAnswers,
            Map<Question, ToggleGroup> qcuAnswers,
            Map<Question, Map<String, String>> matchingAnswers
    ) {
        progressTrack.getChildren().clear();
        int startedCount = 0;
        int completedCount = 0;

        for (int index = 0; index < quizQuestions.size(); index++) {
            Question question = quizQuestions.get(index);
            String state = getQuestionProgressState(question, freeTextAnswers, qcmAnswers, qcuAnswers, matchingAnswers);
            if (!"empty".equals(state)) {
                startedCount++;
            }
            if ("done".equals(state)) {
                completedCount++;
            }

            Button chip = new Button(String.valueOf(index + 1));
            chip.getStyleClass().add("front-progress-chip");
            if (index == currentQuestionIndex[0]) {
                chip.getStyleClass().add("front-progress-chip-active");
            }
            if ("done".equals(state)) {
                chip.getStyleClass().add("front-progress-chip-done");
            } else if ("partial".equals(state)) {
                chip.getStyleClass().add("front-progress-chip-partial");
            }

            final int targetIndex = index;
            chip.setOnAction(event -> {
                currentQuestionIndex[0] = targetIndex;
                renderCurrentQuestion.run();
                playQuickPop(chip, 1.12);
            });
            installPlayfulButtonAnimations(chip);
            progressTrack.getChildren().add(chip);
        }

        progressSummaryLabel.setText(startedCount + " / " + quizQuestions.size() + " commencees • " + completedCount + " completes");
    }

    private String getQuestionProgressState(
            Question question,
            Map<Question, TextField> freeTextAnswers,
            Map<Question, List<CheckBox>> qcmAnswers,
            Map<Question, ToggleGroup> qcuAnswers,
            Map<Question, Map<String, String>> matchingAnswers
    ) {
        if (question.getType().isTextAnswer()) {
            TextField field = freeTextAnswers.get(question);
            return field != null && field.getText() != null && !field.getText().trim().isBlank() ? "done" : "empty";
        }

        if (question.getType().isSingleChoice()) {
            ToggleGroup toggleGroup = qcuAnswers.get(question);
            return toggleGroup != null && toggleGroup.getSelectedToggle() != null ? "done" : "empty";
        }

        if (question.getType() == TypeQuestion.RELIER_FLECHE) {
            Map<String, String> selections = matchingAnswers.get(question);
            int selectedCount = selections == null ? 0 : selections.size();
            int expectedCount = extractMatchingPairs(question.getReponses()).size();
            if (selectedCount == 0) {
                return "empty";
            }
            return selectedCount >= expectedCount && expectedCount > 0 ? "done" : "partial";
        }

        List<CheckBox> boxes = qcmAnswers.get(question);
        if (boxes == null || boxes.isEmpty()) {
            return "empty";
        }
        boolean hasSelection = boxes.stream().anyMatch(CheckBox::isSelected);
        return hasSelection ? "done" : "empty";
    }

    private void playEntranceAnimation(Node node, double delayMillis, double fromTranslateX, double fromTranslateY, double fromScale) {
        if (node == null) {
            return;
        }

        node.setOpacity(0);
        node.setTranslateX(fromTranslateX);
        node.setTranslateY(fromTranslateY);
        node.setScaleX(fromScale);
        node.setScaleY(fromScale);

        FadeTransition fade = new FadeTransition(Duration.millis(420), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition slide = new TranslateTransition(Duration.millis(460), node);
        slide.setFromX(fromTranslateX);
        slide.setFromY(fromTranslateY);
        slide.setToX(0);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(Duration.millis(460), node);
        scale.setFromX(fromScale);
        scale.setFromY(fromScale);
        scale.setToX(1);
        scale.setToY(1);
        scale.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition reveal = new ParallelTransition(fade, slide, scale);
        reveal.setDelay(Duration.millis(delayMillis));
        reveal.play();
    }

    private void playStaggeredEntrance(List<? extends Node> nodes, double initialDelayMillis, double stepMillis, double fromTranslateY) {
        for (int index = 0; index < nodes.size(); index++) {
            playEntranceAnimation(nodes.get(index), initialDelayMillis + (index * stepMillis), 0, fromTranslateY, 0.95);
        }
    }

    private void animateNodeTransform(Node node, double targetScale, double targetTranslateY, double durationMillis) {
        ScaleTransition scale = new ScaleTransition(Duration.millis(durationMillis), node);
        scale.setToX(targetScale);
        scale.setToY(targetScale);
        scale.setInterpolator(Interpolator.EASE_BOTH);

        TranslateTransition move = new TranslateTransition(Duration.millis(durationMillis), node);
        move.setToY(targetTranslateY);
        move.setInterpolator(Interpolator.EASE_BOTH);

        new ParallelTransition(scale, move).play();
    }

    private void installHoverLiftAnimation(Node node, double hoverScale, double hoverLift) {
        node.setOnMouseEntered(event -> animateNodeTransform(node, hoverScale, -hoverLift, 180));
        node.setOnMouseExited(event -> animateNodeTransform(node, 1, 0, 220));
    }

    private void installCardHoverAnimation(Node node) {
        installHoverLiftAnimation(node, 1.03, 6);
    }

    private void installPlayfulButtonAnimations(Button button) {
        installHoverLiftAnimation(button, 1.06, 4);
        button.setOnMousePressed(event -> animateNodeTransform(button, 0.97, 0, 90));
        button.setOnMouseReleased(event -> animateNodeTransform(button, 1.04, -2, 120));
    }

    private void playQuickPop(Node node, double targetScale) {
        ScaleTransition pop = new ScaleTransition(Duration.millis(160), node);
        pop.setFromX(1);
        pop.setFromY(1);
        pop.setToX(targetScale);
        pop.setToY(targetScale);
        pop.setCycleCount(2);
        pop.setAutoReverse(true);
        pop.setInterpolator(Interpolator.EASE_OUT);
        pop.play();
    }

    private void installLoopingAnimation(Node owner, Animation animation) {
        owner.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                animation.stop();
            } else if (animation.getStatus() != Animation.Status.RUNNING) {
                animation.play();
            }
        });
        animation.play();
    }

    private void installBreathingPulse(Node node, double minimumScale, double maximumScale, double durationSeconds) {
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(durationSeconds), node);
        pulse.setFromX(minimumScale);
        pulse.setFromY(minimumScale);
        pulse.setToX(maximumScale);
        pulse.setToY(maximumScale);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        installLoopingAnimation(node, pulse);
    }

    private void addHeroOrb(
            StackPane hero,
            double radius,
            String color,
            double opacity,
            Pos alignment,
            double translateX,
            double translateY,
            double driftX,
            double driftY,
            double durationSeconds
    ) {
        Circle orb = new Circle(radius, Color.web(color, opacity));
        orb.setMouseTransparent(true);
        orb.setTranslateX(translateX);
        orb.setTranslateY(translateY);
        hero.getChildren().add(orb);
        StackPane.setAlignment(orb, alignment);

        TranslateTransition drift = new TranslateTransition(Duration.seconds(durationSeconds), orb);
        drift.setFromX(translateX);
        drift.setFromY(translateY);
        drift.setToX(translateX + driftX);
        drift.setToY(translateY + driftY);
        drift.setAutoReverse(true);

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(Math.max(2.4, durationSeconds - 0.8)), orb);
        pulse.setFromX(0.92);
        pulse.setFromY(0.92);
        pulse.setToX(1.08);
        pulse.setToY(1.08);
        pulse.setAutoReverse(true);

        FadeTransition twinkle = new FadeTransition(Duration.seconds(Math.max(2.0, durationSeconds - 1.4)), orb);
        twinkle.setFromValue(opacity * 0.62);
        twinkle.setToValue(Math.min(0.92, opacity + 0.18));
        twinkle.setAutoReverse(true);

        ParallelTransition loop = new ParallelTransition(drift, pulse, twinkle);
        loop.setCycleCount(Animation.INDEFINITE);
        loop.setInterpolator(Interpolator.EASE_BOTH);
        installLoopingAnimation(orb, loop);
    }

    private void addHeroSpark(
            StackPane hero,
            String color,
            Pos alignment,
            double translateX,
            double translateY,
            double radius,
            double durationSeconds
    ) {
        Circle spark = new Circle(radius, Color.web(color));
        spark.setOpacity(0.35);
        spark.setMouseTransparent(true);
        spark.setTranslateX(translateX);
        spark.setTranslateY(translateY);
        hero.getChildren().add(spark);
        StackPane.setAlignment(spark, alignment);

        FadeTransition glow = new FadeTransition(Duration.seconds(durationSeconds), spark);
        glow.setFromValue(0.18);
        glow.setToValue(0.85);
        glow.setAutoReverse(true);

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(durationSeconds), spark);
        pulse.setFromX(0.72);
        pulse.setFromY(0.72);
        pulse.setToX(1.45);
        pulse.setToY(1.45);
        pulse.setAutoReverse(true);

        ParallelTransition loop = new ParallelTransition(glow, pulse);
        loop.setCycleCount(Animation.INDEFINITE);
        loop.setInterpolator(Interpolator.EASE_BOTH);
        installLoopingAnimation(spark, loop);
    }

    private void decorateKidHero(StackPane hero) {
        addHeroOrb(hero, 86, "#56c6ff", 0.14, Pos.TOP_LEFT, -180, -46, 30, 18, 7.4);
        addHeroOrb(hero, 58, "#ffd54f", 0.15, Pos.TOP_RIGHT, 118, -32, -26, 14, 6.6);
        addHeroOrb(hero, 46, "#7df1b8", 0.12, Pos.BOTTOM_LEFT, -110, 34, 22, -16, 6.2);
        addHeroOrb(hero, 34, "#ff8cc6", 0.14, Pos.BOTTOM_RIGHT, 90, 38, -18, -12, 5.8);
        addHeroSpark(hero, "#fff3a3", Pos.TOP_LEFT, -34, 24, 5, 2.4);
        addHeroSpark(hero, "#a8f0ff", Pos.TOP_RIGHT, -70, 38, 4, 2.9);
        addHeroSpark(hero, "#ffd6f0", Pos.CENTER_LEFT, -220, 12, 3.5, 2.1);
        addHeroSpark(hero, "#c4ffdb", Pos.BOTTOM_RIGHT, 30, 18, 4.5, 2.7);
    }

    private String getCurrentStudentAnswer(
            Question question,
            Map<Question, TextField> freeTextAnswers,
            Map<Question, List<CheckBox>> qcmAnswers,
            Map<Question, ToggleGroup> qcuAnswers,
            Map<Question, Map<String, String>> matchingAnswers
    ) {
        if (question.getType().isTextAnswer()) {
            TextField field = freeTextAnswers.get(question);
            return field == null ? "" : field.getText().trim();
        }

        if (question.getType().isSingleChoice()) {
            ToggleGroup toggleGroup = qcuAnswers.get(question);
            if (toggleGroup == null || toggleGroup.getSelectedToggle() == null) {
                return "";
            }
            RadioButton selected = (RadioButton) toggleGroup.getSelectedToggle();
            return selected.getText() == null ? "" : selected.getText().trim();
        }

        if (question.getType() == TypeQuestion.RELIER_FLECHE) {
            Map<String, String> pairs = matchingAnswers.get(question);
            if (pairs == null || pairs.isEmpty()) {
                return "";
            }
            return pairs.entrySet().stream()
                    .map(entry -> entry.getKey() + " -> " + entry.getValue())
                    .reduce((left, right) -> left + " | " + right)
                    .orElse("");
        }

        List<CheckBox> boxes = qcmAnswers.get(question);
        if (boxes == null || boxes.isEmpty()) {
            return "";
        }

        List<String> selectedAnswers = new ArrayList<>();
        for (CheckBox box : boxes) {
            if (box.isSelected() && box.getText() != null && !box.getText().isBlank()) {
                selectedAnswers.add(box.getText().trim());
            }
        }
        return String.join(" | ", selectedAnswers);
    }

    private VBox createFrontQuestionPreview(
            Question question,
            int questionNumber,
            int totalQuestions,
            Map<Question, TextField> freeTextAnswers,
            Map<Question, List<CheckBox>> qcmAnswers,
            Map<Question, ToggleGroup> qcuAnswers,
            Map<Question, Map<String, String>> matchingAnswers,
            Runnable onAnswerUpdated
    ) {
        VBox box = new VBox(8);
        box.getStyleClass().add("front-question-preview");

        Label order = new Label("Question " + questionNumber + " / " + totalQuestions);
        order.getStyleClass().add("front-question-order");
        Label title = new Label(question.getIntitule());
        title.getStyleClass().add("front-question-title");
        Label meta = new Label(question.getTypeLabel() + " • " + question.getPoints() + " point(s)");
        meta.getStyleClass().add("front-question-meta");
        if (question.getType() == TypeQuestion.RELIER_FLECHE) {
            VBox matchingBox = createFrontMatchingPreview(question, matchingAnswers, onAnswerUpdated);
            box.getChildren().addAll(order, title, meta, matchingBox);
            return box;
        }

        VBox answersBox = new VBox(10);
        answersBox.getStyleClass().add("front-question-answers-box");
        if (question.getType().isTextAnswer()) {
            TextField freeAnswerField = createTextField("Tapez votre reponse...");
            freeAnswerField.getStyleClass().add("front-free-answer-field");
            freeTextAnswers.put(question, freeAnswerField);
            installHoverLiftAnimation(freeAnswerField, 1.01, 2);
            freeAnswerField.textProperty().addListener((observable, oldValue, newValue) -> onAnswerUpdated.run());
            answersBox.getChildren().add(freeAnswerField);
        } else if (question.getType().isSingleChoice()) {
            ToggleGroup toggleGroup = new ToggleGroup();
            qcuAnswers.put(question, toggleGroup);
            for (Reponse reponse : question.getReponses()) {
                RadioButton radioButton = new RadioButton(reponse.getTexte());
                radioButton.setToggleGroup(toggleGroup);
                radioButton.getStyleClass().add("front-radio-answer");
                radioButton.setWrapText(true);
                installHoverLiftAnimation(radioButton, 1.02, 2);
                radioButton.selectedProperty().addListener((observable, oldValue, newValue) -> onAnswerUpdated.run());
                answersBox.getChildren().add(radioButton);
            }
        } else {
            List<CheckBox> boxes = new ArrayList<>();
            for (Reponse reponse : question.getReponses()) {
                CheckBox checkBox = new CheckBox(reponse.getTexte());
                checkBox.getStyleClass().add("front-check-answer");
                checkBox.setWrapText(true);
                installHoverLiftAnimation(checkBox, 1.02, 2);
                checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> onAnswerUpdated.run());
                boxes.add(checkBox);
                answersBox.getChildren().add(checkBox);
            }
            qcmAnswers.put(question, boxes);
        }

        box.getChildren().addAll(order, title, meta, answersBox);
        return box;
    }

    private VBox createFrontMatchingPreview(Question question, Map<Question, Map<String, String>> matchingAnswers, Runnable onAnswerUpdated) {
        List<String[]> pairs = extractMatchingPairs(question.getReponses());

        Label helper = new Label("Relie chaque element de gauche avec la bonne reponse de droite.");
        helper.getStyleClass().add("page-subtitle");
        helper.setWrapText(true);

        if (pairs.isEmpty()) {
            Label emptyLabel = new Label("Aucune association n'est encore configuree pour cette question.");
            emptyLabel.getStyleClass().add("front-empty-title");
            return new VBox(14, helper, emptyLabel);
        }

        Pane linesPane = new Pane();
        linesPane.setMouseTransparent(true);
        linesPane.setPickOnBounds(false);

        VBox leftColumn = new VBox(18);
        leftColumn.setAlignment(Pos.TOP_LEFT);
        VBox rightColumn = new VBox(18);
        rightColumn.setAlignment(Pos.TOP_RIGHT);

        HBox columns = new HBox(72, leftColumn, rightColumn);
        columns.setAlignment(Pos.TOP_CENTER);

        StackPane matchingBoard = new StackPane(linesPane, columns);
        matchingBoard.getStyleClass().add("matching-board");
        matchingBoard.setPadding(new Insets(18));
        double boardHeight = Math.max(170, 40 + (pairs.size() * 62.0));
        matchingBoard.setMinHeight(boardHeight);
        matchingBoard.setPrefHeight(boardHeight);
        StackPane.setAlignment(columns, Pos.TOP_CENTER);

        List<String[]> shuffledPairs = new ArrayList<>(pairs);
        Collections.shuffle(shuffledPairs);

        Map<String, Label> leftNodes = new LinkedHashMap<>();
        Map<String, Label> rightNodes = new LinkedHashMap<>();
        Map<String, String> selections = matchingAnswers.computeIfAbsent(question, key -> new LinkedHashMap<>());
        Label[] activeLeftLabel = {null};

        for (String[] pair : pairs) {
            Label label = createMatchingSideLabel(pair[0], "matching-left-item");
            label.setOnMouseClicked(event -> {
                activeLeftLabel[0] = label;
                highlightMatchingSelection(leftNodes.values(), label);
                playQuickPop(label, 1.08);
            });
            leftNodes.put(pair[0], label);
            leftColumn.getChildren().add(label);
        }

        for (String[] pair : shuffledPairs) {
            Label label = createMatchingSideLabel(pair[1], "matching-right-item");
            label.setOnMouseClicked(event -> {
                if (activeLeftLabel[0] == null) {
                    return;
                }
                String leftValue = activeLeftLabel[0].getText();
                selections.put(leftValue, label.getText());
                playQuickPop(activeLeftLabel[0], 1.06);
                playQuickPop(label, 1.08);
                highlightMatchingSelection(leftNodes.values(), null);
                activeLeftLabel[0] = null;
                redrawMatchingLines(linesPane, matchingBoard, leftNodes, rightNodes, selections);
                onAnswerUpdated.run();
            });
            rightNodes.put(pair[1], label);
            rightColumn.getChildren().add(label);
        }

        matchingBoard.layoutBoundsProperty().addListener((observable, oldValue, newValue) ->
                redrawMatchingLines(linesPane, matchingBoard, leftNodes, rightNodes, selections));
        leftColumn.heightProperty().addListener((observable, oldValue, newValue) ->
                redrawMatchingLines(linesPane, matchingBoard, leftNodes, rightNodes, selections));
        rightColumn.heightProperty().addListener((observable, oldValue, newValue) ->
                redrawMatchingLines(linesPane, matchingBoard, leftNodes, rightNodes, selections));

        redrawMatchingLines(linesPane, matchingBoard, leftNodes, rightNodes, selections);
        return new VBox(14, helper, matchingBoard);
    }

    private List<String[]> extractMatchingPairs(List<Reponse> reponses) {
        List<String[]> pairs = new ArrayList<>();
        for (Reponse reponse : reponses) {
            String texte = reponse.getTexte() == null ? "" : reponse.getTexte().trim();
            if (texte.contains("|||")) {
                String[] parts = texte.split("\\|\\|\\|", 2);
                if (parts.length == 2 && !parts[0].trim().isEmpty() && !parts[1].trim().isEmpty()) {
                    pairs.add(new String[]{parts[0].trim(), parts[1].trim()});
                }
            }
        }
        if (!pairs.isEmpty()) {
            return pairs;
        }

        List<String> legacyValues = reponses.stream()
                .map(Reponse::getTexte)
                .filter(text -> text != null && !text.trim().isEmpty())
                .map(String::trim)
                .toList();
        for (int i = 0; i + 1 < legacyValues.size(); i += 2) {
            pairs.add(new String[]{legacyValues.get(i), legacyValues.get(i + 1)});
        }
        return pairs;
    }

    private Label createMatchingSideLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().addAll("matching-side-item", styleClass);
        label.setWrapText(true);
        label.setMinWidth(180);
        label.setPrefWidth(220);
        installHoverLiftAnimation(label, 1.03, 3);
        return label;
    }

    private void highlightMatchingSelection(Iterable<Label> labels, Label selectedLabel) {
        for (Label label : labels) {
            label.getStyleClass().remove("matching-side-item-active");
            if (selectedLabel != null && label == selectedLabel) {
                label.getStyleClass().add("matching-side-item-active");
            }
        }
    }

    private void redrawMatchingLines(
            Pane linesPane,
            StackPane matchingBoard,
            Map<String, Label> leftNodes,
            Map<String, Label> rightNodes,
            Map<String, String> selections
    ) {
        linesPane.getChildren().clear();
        linesPane.setPrefSize(matchingBoard.getWidth(), matchingBoard.getHeight());
        for (Map.Entry<String, String> entry : selections.entrySet()) {
            Label left = leftNodes.get(entry.getKey());
            Label right = rightNodes.get(entry.getValue());
            if (left == null || right == null) {
                continue;
            }

            var leftSceneBounds = left.localToScene(left.getBoundsInLocal());
            var rightSceneBounds = right.localToScene(right.getBoundsInLocal());
            if (leftSceneBounds == null || rightSceneBounds == null) {
                continue;
            }
            var leftBounds = matchingBoard.sceneToLocal(leftSceneBounds);
            var rightBounds = matchingBoard.sceneToLocal(rightSceneBounds);

            double startX = leftBounds.getMaxX();
            double startY = leftBounds.getMinY() + leftBounds.getHeight() / 2.0;
            double endX = rightBounds.getMinX();
            double endY = rightBounds.getMinY() + rightBounds.getHeight() / 2.0;

            Line line = new Line(startX, startY, endX, endY);
            line.setStroke(Color.web("#2ad16f"));
            line.setStrokeWidth(4);

            Polygon arrowHead = new Polygon(
                    endX, endY,
                    endX - 14, endY - 8,
                    endX - 14, endY + 8
            );
            arrowHead.setFill(Color.web("#2ad16f"));

            Circle startDot = new Circle(startX, startY, 5, Color.web("#4f647c"));
            Circle endDot = new Circle(endX, endY, 5, Color.web("#4f647c"));
            linesPane.getChildren().addAll(line, arrowHead, startDot, endDot);
        }
    }

    private VBox createFrontEmptyState(String message) {
        VBox box = new VBox(8);
        box.setPrefWidth(420);
        box.setPadding(new Insets(28));
        box.getStyleClass().add("front-empty-card");
        Label title = new Label(message);
        title.getStyleClass().add("front-empty-title");
        Label subtitle = new Label("Essayez un autre mot-cle ou reinitialisez les filtres.");
        subtitle.getStyleClass().add("front-empty-subtitle");
        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private Spinner<Integer> createSpinner(int min, int max, int initialValue) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initialValue);
        spinner.setEditable(true);
        spinner.getStyleClass().add("app-spinner");
        spinner.setMaxWidth(Double.MAX_VALUE);
        return spinner;
    }

    private void duplicateSelectedQuiz(Quiz selectedQuiz) {
        if (selectedQuiz == null) {
            showInfo("Selection", "Selectionnez un quiz a dupliquer.");
            return;
        }

        try {
            Quiz duplicatedQuiz = quizController.saveQuiz(null, new Quiz(
                    selectedQuiz.getTitre() + " (copie)",
                    selectedQuiz.getDescription(),
                    selectedQuiz.getImageUrl(),
                    selectedQuiz.getNiveau(),
                    selectedQuiz.getCategorieAge(),
                    0,
                    selectedQuiz.getDureeMinutes(),
                    selectedQuiz.getScoreMinimum(),
                    "Brouillon"
            ));

            for (Question question : questionController.getQuestionsForQuiz(selectedQuiz)) {
                List<Reponse> duplicatedReponses = question.getReponses().stream()
                        .map(reponse -> new Reponse(reponse.getTexte(), reponse.isCorrecte()))
                        .toList();

                questionController.saveQuestion(null, new Question(
                        duplicatedQuiz,
                        question.getIntitule(),
                        question.getType(),
                        question.getPoints(),
                        duplicatedReponses
                ));
            }

            showInfo("Duplication", "Le quiz a ete duplique avec ses questions dans une nouvelle version brouillon.");
            showQuizList();
        } catch (ValidationException ex) {
            showError("Duplication impossible", ex.getMessage());
        } catch (Exception ex) {
            showError("Duplication impossible", "Une erreur est survenue pendant la duplication du quiz.");
        }
    }

    private String buildStatisticsInsight(Quiz quizLePlusRiche, int nombreQuestions) {
        if (quizLePlusRiche == null) {
            return "Aucun quiz n'est disponible pour produire une analyse avancee.";
        }

        return "Quiz le plus complet: " + quizLePlusRiche.getTitre()
                + " avec " + quizLePlusRiche.getNombreQuestions() + " question(s). "
                + "La base contient actuellement " + nombreQuestions + " question(s) au total, "
                + "ce qui permet de suivre rapidement la progression du contenu pedagogique.";
    }

    private void exportAnalyticsReport() {
        try {
            Path reportPath = generateAnalyticsReportPdf();
            showInfo("Export termine", "Le rapport PDF a ete enregistre dans:\n" + reportPath.toAbsolutePath());
        } catch (IOException ex) {
            showError("Export impossible", "Le rapport PDF n'a pas pu etre ecrit sur le disque.");
        }
    }

    private void openAnalyticsMailDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (root.getScene() != null && root.getScene().getWindow() != null) {
            dialog.initOwner(root.getScene().getWindow());
        }
        dialog.setTitle("Envoyer le rapport");

        Label badge = new Label("Mailing");
        badge.getStyleClass().addAll("message-badge", "message-badge-info");

        Label title = new Label("Envoyer le rapport statistique");
        title.getStyleClass().add("page-title");

        Label body = new Label("Le rapport PDF sera genere automatiquement puis envoye en piece jointe.");
        body.getStyleClass().add("page-subtitle");
        body.setWrapText(true);

        TextField recipientField = createTextField("destinataire@example.com");
        TextField subjectField = createTextField("Rapport statistique EduKids");
        subjectField.setText("Rapport statistique EduKids");
        TextArea messageArea = createTextArea(
                "Bonjour,\n\nVeuillez trouver ci-joint le rapport statistique EduKids.\n",
                5
        );

        Label configLabel = new Label(mailingService.hasConfiguration()
                ? "Configuration SMTP detectee."
                : "Configuration SMTP manquante. Remplissez d'abord data/mail.properties a partir de mail.properties.example.");
        configLabel.getStyleClass().add("message-body");
        configLabel.setWrapText(true);

        Label statusLabel = new Label("Pret a envoyer.");
        statusLabel.getStyleClass().add("message-body");
        statusLabel.setWrapText(true);

        Button cancelButton = new Button("Annuler");
        cancelButton.getStyleClass().add("secondary-action");
        cancelButton.setOnAction(event -> dialog.close());

        Button sendButton = new Button("Envoyer");
        sendButton.getStyleClass().add("primary-action");
        sendButton.setDisable(!mailingService.hasConfiguration());
        sendButton.setOnAction(event -> {
            String recipient = recipientField.getText() == null ? "" : recipientField.getText().trim();
            String subject = subjectField.getText() == null ? "" : subjectField.getText().trim();
            String message = messageArea.getText() == null ? "" : messageArea.getText();
            if (recipient.isBlank() || !recipient.contains("@")) {
                statusLabel.setText("Saisissez une adresse email valide.");
                return;
            }

            sendButton.setDisable(true);
            cancelButton.setDisable(true);
            recipientField.setDisable(true);
            subjectField.setDisable(true);
            messageArea.setDisable(true);
            statusLabel.setText("Generation du rapport et envoi en cours...");

            Task<Path> task = new Task<>() {
                @Override
                protected Path call() throws Exception {
                    Path reportPath = generateAnalyticsReportPdf();
                    mailingService.sendEmailWithAttachment(
                            recipient,
                            subject,
                            message,
                            reportPath
                    );
                    return reportPath;
                }
            };

            task.setOnSucceeded(successEvent -> {
                Path reportPath = task.getValue();
                dialog.close();
                showInfo(
                        "Email envoye",
                        "Le rapport a ete envoye a " + recipient + ".\n\nPiece jointe:\n" + reportPath.toAbsolutePath()
                );
            });

            task.setOnFailed(failureEvent -> {
                Throwable error = task.getException();
                statusLabel.setText(error == null || error.getMessage() == null || error.getMessage().isBlank()
                        ? "L'envoi a echoue."
                        : error.getMessage());
                sendButton.setDisable(!mailingService.hasConfiguration());
                cancelButton.setDisable(false);
                recipientField.setDisable(false);
                subjectField.setDisable(false);
                messageArea.setDisable(false);
            });

            Thread thread = new Thread(task, "mailing-report");
            thread.setDaemon(true);
            thread.start();
        });

        HBox actions = new HBox(12, cancelButton, sendButton);
        actions.setAlignment(Pos.CENTER_RIGHT);

        VBox content = new VBox(
                16,
                badge,
                title,
                body,
                createFormRow("Destinataire *", recipientField),
                createFormRow("Sujet *", subjectField),
                createFormRow("Message", messageArea),
                configLabel,
                statusLabel,
                actions
        );
        content.setPadding(new Insets(24));
        content.getStyleClass().addAll("score-dialog-card", "message-dialog-card");

        Scene scene = new Scene(content, 560, 470);
        if (root.getScene() != null && !root.getScene().getStylesheets().isEmpty()) {
            scene.getStylesheets().addAll(root.getScene().getStylesheets());
        }
        scene.setFill(Color.TRANSPARENT);

        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private Path generateAnalyticsReportPdf() throws IOException {
        Path exportDirectory = Path.of("data", "exports");
        Files.createDirectories(exportDirectory);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path reportPath = exportDirectory.resolve("quiz-report-" + timestamp + ".pdf");
        writeAnalyticsPdf(reportPath);
        return reportPath;
    }

    private void writeAnalyticsPdf(Path reportPath) throws IOException {
        List<Quiz> quizzes = quizController.getAllQuizzes().stream()
                .sorted(Comparator.comparing(Quiz::getTitre, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int quizCount = quizzes.size();
        int questionCount = questionController.getAllQuestions().size();
        int answerCount = questionController.getAllQuestions().stream()
                .mapToInt(question -> question.getReponses().size())
                .sum();
        long publishedCount = quizzes.stream().filter(quiz -> "Publie".equalsIgnoreCase(quiz.getStatut())).count();
        long draftCount = quizzes.stream().filter(quiz -> "Brouillon".equalsIgnoreCase(quiz.getStatut())).count();
        long archivedCount = quizzes.stream().filter(quiz -> "Archive".equalsIgnoreCase(quiz.getStatut())).count();
        int averageDuration = quizCount == 0 ? 0 : (int) Math.round(quizzes.stream().mapToInt(Quiz::getDureeMinutes).average().orElse(0));
        int averageScoreMinimum = quizCount == 0 ? 0 : (int) Math.round(quizzes.stream().mapToInt(Quiz::getScoreMinimum).average().orElse(0));

        PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDFont regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        java.awt.Color navy = new java.awt.Color(30, 55, 90);
        java.awt.Color sky = new java.awt.Color(142, 201, 211);
        java.awt.Color lime = new java.awt.Color(127, 209, 75);
        java.awt.Color yellow = new java.awt.Color(247, 243, 74);
        java.awt.Color orange = new java.awt.Color(255, 153, 43);
        java.awt.Color softBlue = new java.awt.Color(231, 242, 249);
        java.awt.Color warmPanel = new java.awt.Color(247, 241, 233);
        java.awt.Color lineColor = new java.awt.Color(220, 229, 236);
        java.awt.Color textColor = new java.awt.Color(46, 58, 74);
        java.awt.Color mutedText = new java.awt.Color(108, 122, 137);

        try (PDDocument document = new PDDocument()) {
            PDPage overviewPage = new PDPage(PDRectangle.A4);
            document.addPage(overviewPage);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, overviewPage)) {
                float pageWidth = overviewPage.getMediaBox().getWidth();
                float pageHeight = overviewPage.getMediaBox().getHeight();
                float margin = 38;

                drawFilledRect(contentStream, 0, pageHeight - 110, pageWidth, 110, navy);
                writePdfText(contentStream, "Rapport statistique EduKids", margin, pageHeight - 48, boldFont, 24, java.awt.Color.WHITE);
                writePdfText(
                        contentStream,
                        "Genere le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        margin,
                        pageHeight - 72,
                        regularFont,
                        11,
                        new java.awt.Color(232, 240, 247)
                );
                writePdfText(contentStream, "Vue d'ensemble des quiz, des volumes et des statuts.", margin, pageHeight - 90, regularFont, 11, new java.awt.Color(219, 231, 242));

                float summaryTop = pageHeight - 145;
                float cardWidth = 165;
                float cardHeight = 72;
                float cardGap = 14;
                float firstRowY = summaryTop - cardHeight;
                float secondRowY = firstRowY - cardHeight - 12;

                drawSummaryCard(contentStream, margin, firstRowY, cardWidth, cardHeight, softBlue, "Quiz", String.valueOf(quizCount), textColor, mutedText, boldFont, regularFont);
                drawSummaryCard(contentStream, margin + cardWidth + cardGap, firstRowY, cardWidth, cardHeight, softBlue, "Questions", String.valueOf(questionCount), textColor, mutedText, boldFont, regularFont);
                drawSummaryCard(contentStream, margin + (cardWidth + cardGap) * 2, firstRowY, cardWidth, cardHeight, softBlue, "Reponses", String.valueOf(answerCount), textColor, mutedText, boldFont, regularFont);
                drawSummaryCard(contentStream, margin, secondRowY, cardWidth, cardHeight, warmPanel, "Publies", String.valueOf(publishedCount), textColor, mutedText, boldFont, regularFont);
                drawSummaryCard(contentStream, margin + cardWidth + cardGap, secondRowY, cardWidth, cardHeight, warmPanel, "Duree moyenne", averageDuration + " min", textColor, mutedText, boldFont, regularFont);
                drawSummaryCard(contentStream, margin + (cardWidth + cardGap) * 2, secondRowY, cardWidth, cardHeight, warmPanel, "Score minimum moyen", averageScoreMinimum + "%", textColor, mutedText, boldFont, regularFont);

                float chartPanelX = margin;
                float chartPanelY = 170;
                float chartPanelWidth = pageWidth - (margin * 2);
                float chartPanelHeight = 235;
                drawFilledRect(contentStream, chartPanelX, chartPanelY, chartPanelWidth, chartPanelHeight, new java.awt.Color(252, 252, 250));
                drawStrokedRect(contentStream, chartPanelX, chartPanelY, chartPanelWidth, chartPanelHeight, lineColor, 1.1f);
                writePdfText(contentStream, "Repartition des quiz par statut", chartPanelX + 18, chartPanelY + chartPanelHeight - 28, boldFont, 16, textColor);
                writePdfText(contentStream, "Le cercle reprend le meme principe visuel que la page statistiques.", chartPanelX + 18, chartPanelY + chartPanelHeight - 46, regularFont, 10, mutedText);

                Map<String, Integer> chartValues = new HashMap<>();
                chartValues.put("Publie", (int) publishedCount);
                chartValues.put("Brouillon", (int) draftCount);
                chartValues.put("Archive", (int) archivedCount);

                float centerX = chartPanelX + 150;
                float centerY = chartPanelY + 92;
                float radius = 78;
                drawPieChart(
                        contentStream,
                        centerX,
                        centerY,
                        radius,
                        chartValues,
                        List.of(lime, yellow, sky),
                        boldFont,
                        regularFont
                );

                float legendX = chartPanelX + 320;
                float legendY = chartPanelY + 142;
                drawFilledRect(contentStream, legendX, legendY - 98, 160, 110, warmPanel);
                drawStrokedRect(contentStream, legendX, legendY - 98, 160, 110, lineColor, 1f);
                writePdfText(contentStream, "Legende", legendX + 50, legendY - 10, boldFont, 14, textColor);
                drawLegendItem(contentStream, legendX + 16, legendY - 34, lime, "Publie", regularFont, textColor);
                drawLegendItem(contentStream, legendX + 16, legendY - 58, yellow, "Brouillon", regularFont, textColor);
                drawLegendItem(contentStream, legendX + 16, legendY - 82, sky, "Archive", regularFont, textColor);

                writePdfText(
                        contentStream,
                        "Astuce: plus la part jaune est elevee, plus il reste de quiz a publier.",
                        chartPanelX + 18,
                        chartPanelY + 16,
                        regularFont,
                        10,
                        mutedText
                );
            }

            int itemsPerPage = 18;
            int totalPages = Math.max(1, (int) Math.ceil(quizzes.size() / (double) itemsPerPage));
            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                PDPage detailsPage = new PDPage(PDRectangle.A4);
                document.addPage(detailsPage);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, detailsPage)) {
                    float pageWidth = detailsPage.getMediaBox().getWidth();
                    float pageHeight = detailsPage.getMediaBox().getHeight();
                    float margin = 38;

                    drawFilledRect(contentStream, 0, pageHeight - 76, pageWidth, 76, navy);
                    writePdfText(contentStream, "Details des quiz", margin, pageHeight - 42, boldFont, 20, java.awt.Color.WHITE);
                    writePdfText(contentStream, "Page " + (pageIndex + 1) + " / " + totalPages, pageWidth - 105, pageHeight - 42, regularFont, 10, new java.awt.Color(223, 235, 245));

                    float tableX = margin;
                    float tableY = pageHeight - 132;
                    float rowHeight = 28;
                    float[] columnWidths = {175, 78, 88, 66, 58, 58};
                    String[] headers = {"Titre", "Niveau", "Statut", "Questions", "Duree", "Score"};

                    float currentX = tableX;
                    for (int i = 0; i < headers.length; i++) {
                        drawFilledRect(contentStream, currentX, tableY, columnWidths[i], rowHeight, sky);
                        drawStrokedRect(contentStream, currentX, tableY, columnWidths[i], rowHeight, lineColor, 0.8f);
                        writePdfText(contentStream, headers[i], currentX + 8, tableY + 10, boldFont, 10, textColor);
                        currentX += columnWidths[i];
                    }

                    int startIndex = pageIndex * itemsPerPage;
                    int endIndex = Math.min(startIndex + itemsPerPage, quizzes.size());
                    float rowY = tableY - rowHeight;

                    for (int index = startIndex; index < endIndex; index++) {
                        Quiz quiz = quizzes.get(index);
                        java.awt.Color rowColor = (index % 2 == 0) ? new java.awt.Color(250, 252, 253) : warmPanel;
                        currentX = tableX;
                        String[] values = {
                                truncatePdfText(quiz.getTitre(), 28),
                                truncatePdfText(quiz.getNiveau(), 12),
                                truncatePdfText(quiz.getStatut(), 12),
                                String.valueOf(quiz.getNombreQuestions()),
                                quiz.getDureeMinutes() + " min",
                                quiz.getScoreMinimum() + "%"
                        };

                        for (int i = 0; i < values.length; i++) {
                            drawFilledRect(contentStream, currentX, rowY, columnWidths[i], rowHeight, rowColor);
                            drawStrokedRect(contentStream, currentX, rowY, columnWidths[i], rowHeight, lineColor, 0.8f);
                            writePdfText(contentStream, values[i], currentX + 8, rowY + 10, regularFont, 9.5f, textColor);
                            currentX += columnWidths[i];
                        }
                        rowY -= rowHeight;
                    }
                }
            }

            document.save(reportPath.toFile());
        }
    }

    private float writePdfLine(
            PDPageContentStream contentStream,
            String text,
            float x,
            float y,
            PDFont font,
            float fontSize
    ) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
        return y;
    }

    private void writePdfText(
            PDPageContentStream contentStream,
            String text,
            float x,
            float y,
            PDFont font,
            float fontSize,
            java.awt.Color color
    ) throws IOException {
        contentStream.beginText();
        contentStream.setNonStrokingColor(color);
        contentStream.setFont(font, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }

    private void writePdfCenteredText(
            PDPageContentStream contentStream,
            String text,
            float centerX,
            float y,
            PDFont font,
            float fontSize,
            java.awt.Color color
    ) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        writePdfText(contentStream, text, centerX - (textWidth / 2), y, font, fontSize, color);
    }

    private void drawFilledRect(
            PDPageContentStream contentStream,
            float x,
            float y,
            float width,
            float height,
            java.awt.Color color
    ) throws IOException {
        contentStream.setNonStrokingColor(color);
        contentStream.addRect(x, y, width, height);
        contentStream.fill();
    }

    private void drawStrokedRect(
            PDPageContentStream contentStream,
            float x,
            float y,
            float width,
            float height,
            java.awt.Color color,
            float lineWidth
    ) throws IOException {
        contentStream.setLineWidth(lineWidth);
        contentStream.setStrokingColor(color);
        contentStream.addRect(x, y, width, height);
        contentStream.stroke();
    }

    private void drawSummaryCard(
            PDPageContentStream contentStream,
            float x,
            float y,
            float width,
            float height,
            java.awt.Color background,
            String label,
            String value,
            java.awt.Color titleColor,
            java.awt.Color subtitleColor,
            PDFont boldFont,
            PDFont regularFont
    ) throws IOException {
        drawFilledRect(contentStream, x, y, width, height, background);
        drawStrokedRect(contentStream, x, y, width, height, new java.awt.Color(220, 229, 236), 0.8f);
        writePdfText(contentStream, value, x + 14, y + height - 28, boldFont, 20, titleColor);
        writePdfText(contentStream, label, x + 14, y + 16, regularFont, 10, subtitleColor);
    }

    private void drawLegendItem(
            PDPageContentStream contentStream,
            float x,
            float y,
            java.awt.Color color,
            String label,
            PDFont font,
            java.awt.Color textColor
    ) throws IOException {
        drawFilledRect(contentStream, x, y, 16, 16, color);
        writePdfText(contentStream, label, x + 24, y + 4, font, 11, textColor);
    }

    private void drawPieChart(
            PDPageContentStream contentStream,
            float centerX,
            float centerY,
            float radius,
            Map<String, Integer> values,
            List<java.awt.Color> colors,
            PDFont boldFont,
            PDFont regularFont
    ) throws IOException {
        List<Map.Entry<String, Integer>> entries = values.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.comparingByKey())
                .toList();

        if (entries.isEmpty()) {
            drawFilledRect(contentStream, centerX - radius, centerY - radius, radius * 2, radius * 2, new java.awt.Color(240, 244, 247));
            writePdfCenteredText(contentStream, "0 quiz", centerX, centerY - 4, boldFont, 16, new java.awt.Color(108, 122, 137));
            writePdfCenteredText(contentStream, "Aucune donnee", centerX, centerY - 22, regularFont, 10, new java.awt.Color(132, 145, 157));
            return;
        }

        int total = entries.stream().mapToInt(Map.Entry::getValue).sum();
        double startAngle = 90.0;

        for (int index = 0; index < entries.size(); index++) {
            Map.Entry<String, Integer> entry = entries.get(index);
            double extent = 360.0 * entry.getValue() / total;
            drawPieSlice(contentStream, centerX, centerY, radius, startAngle, startAngle - extent, colors.get(index % colors.size()));

            double midAngle = Math.toRadians(startAngle - (extent / 2.0));
            String percentage = Math.round((entry.getValue() * 100f) / total) + "%";
            float labelX = centerX + (float) (Math.cos(midAngle) * radius * 0.58f);
            float labelY = centerY + (float) (Math.sin(midAngle) * radius * 0.58f);
            writePdfCenteredText(contentStream, percentage, labelX, labelY, boldFont, 14, new java.awt.Color(34, 45, 58));

            startAngle -= extent;
        }
    }

    private void drawPieSlice(
            PDPageContentStream contentStream,
            float centerX,
            float centerY,
            float radius,
            double startAngle,
            double endAngle,
            java.awt.Color color
    ) throws IOException {
        contentStream.setNonStrokingColor(color);
        contentStream.moveTo(centerX, centerY);

        double angleStep = 4.0;
        double direction = endAngle < startAngle ? -angleStep : angleStep;
        double angle = startAngle;

        while ((direction < 0 && angle >= endAngle) || (direction > 0 && angle <= endAngle)) {
            double radian = Math.toRadians(angle);
            float x = centerX + (float) (Math.cos(radian) * radius);
            float y = centerY + (float) (Math.sin(radian) * radius);
            contentStream.lineTo(x, y);
            angle += direction;
        }

        double endRadian = Math.toRadians(endAngle);
        contentStream.lineTo(centerX + (float) (Math.cos(endRadian) * radius), centerY + (float) (Math.sin(endRadian) * radius));
        contentStream.closePath();
        contentStream.fill();
    }

    private String truncatePdfText(String value, int maxLength) {
        if (value == null) {
            return "-";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private void showQuizNote(Quiz quiz) {
        if (quiz == null) {
            showInfo("Selection", "Selectionnez un quiz pour afficher sa note.");
            return;
        }

        List<QuizResult> results = quizResultController.getResultsForQuiz(quiz);
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (root.getScene() != null && root.getScene().getWindow() != null) {
            dialog.initOwner(root.getScene().getWindow());
        }
        dialog.setTitle("Historique des notes");

        Label title = new Label("Historique des notes");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Quiz: " + quiz.getTitre());
        subtitle.getStyleClass().add("page-subtitle");

        VBox content = new VBox(18);
        content.setPadding(new Insets(24));
        content.getStyleClass().add("score-dialog-card");

        if (results.isEmpty()) {
            Label emptyLabel = new Label("Aucun score final n'a encore ete enregistre pour ce quiz.");
            emptyLabel.getStyleClass().add("page-subtitle");
            Button closeButton = new Button("Fermer");
            closeButton.getStyleClass().add("front-card-button");
            closeButton.setOnAction(event -> dialog.close());
            content.getChildren().addAll(title, subtitle, emptyLabel, closeButton);
        } else {
            int bestScore = results.stream().mapToInt(QuizResult::getFinalScore).max().orElse(0);
            int averageScore = (int) Math.round(results.stream().mapToInt(QuizResult::getFinalScore).average().orElse(0));
            QuizResult latestResult = results.get(0);

            FlowPane summaryRow = new FlowPane();
            summaryRow.setHgap(14);
            summaryRow.setVgap(14);
            summaryRow.getChildren().addAll(
                    createHistorySummaryCard("Meilleure note", bestScore + "%"),
                    createHistorySummaryCard("Moyenne", averageScore + "%"),
                    createHistorySummaryCard("Derniere tentative", latestResult.getFinalScore() + "%")
            );

            Label insightBadge = new Label(buildPerformanceBadge(bestScore, averageScore));
            insightBadge.getStyleClass().add("history-badge");

            TableView<QuizResult> table = new TableView<>(FXCollections.observableArrayList(results));
            table.getStyleClass().add("quiz-table");
            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
            table.setPrefHeight(320);

            TableColumn<QuizResult, String> dateCol = new TableColumn<>("Date");
            dateCol.setCellValueFactory(new PropertyValueFactory<>("completedAt"));

            TableColumn<QuizResult, Integer> scoreCol = new TableColumn<>("Score final");
            scoreCol.setCellValueFactory(new PropertyValueFactory<>("finalScore"));

            TableColumn<QuizResult, Integer> earnedCol = new TableColumn<>("Points obtenus");
            earnedCol.setCellValueFactory(new PropertyValueFactory<>("earnedPoints"));

            TableColumn<QuizResult, Integer> totalCol = new TableColumn<>("Points totaux");
            totalCol.setCellValueFactory(new PropertyValueFactory<>("totalPoints"));

            table.getColumns().addAll(dateCol, scoreCol, earnedCol, totalCol);

            Button closeButton = new Button("Fermer");
            closeButton.getStyleClass().add("front-card-button");
            closeButton.setOnAction(event -> dialog.close());

            content.getChildren().addAll(title, subtitle, summaryRow, insightBadge, table, closeButton);
        }

        Scene scene = new Scene(content, 760, results.isEmpty() ? 220 : 460);
        if (root.getScene() != null && !root.getScene().getStylesheets().isEmpty()) {
            scene.getStylesheets().addAll(root.getScene().getStylesheets());
        }
        scene.setFill(Color.TRANSPARENT);

        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }

    private String buildAnalyticsReport() {
        StringBuilder builder = new StringBuilder();
        builder.append("Rapport EduKids").append(System.lineSeparator());
        builder.append("Genere le ")
                .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append(System.lineSeparator()).append(System.lineSeparator());

        builder.append("Resume global").append(System.lineSeparator());
        builder.append("- Quiz: ").append(quizController.getAllQuizzes().size()).append(System.lineSeparator());
        builder.append("- Questions: ").append(questionController.getAllQuestions().size()).append(System.lineSeparator());
        builder.append("- Reponses: ").append(questionController.getAllQuestions().stream()
                .mapToLong(question -> question.getReponses().size())
                .sum()).append(System.lineSeparator()).append(System.lineSeparator());

        builder.append("Details des quiz").append(System.lineSeparator());
        for (Quiz quiz : quizController.getAllQuizzes().stream()
                .sorted(Comparator.comparing(Quiz::getTitre, String.CASE_INSENSITIVE_ORDER))
                .toList()) {
            builder.append("- ")
                    .append(quiz.getTitre())
                    .append(" | niveau=")
                    .append(quiz.getNiveau())
                    .append(" | statut=")
                    .append(quiz.getStatut())
                    .append(" | questions=")
                    .append(quiz.getNombreQuestions())
                    .append(" | duree=")
                    .append(quiz.getDureeMinutes())
                    .append(" min | score min=")
                    .append(quiz.getScoreMinimum())
                    .append("%")
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    private VBox createHistorySummaryCard(String label, String value) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("history-summary-value");
        Label labelLabel = new Label(label);
        labelLabel.getStyleClass().add("history-summary-label");

        VBox card = new VBox(6, valueLabel, labelLabel);
        card.getStyleClass().add("history-summary-card");
        return card;
    }

    private String buildPerformanceBadge(int bestScore, int averageScore) {
        if (bestScore >= 90) {
            return "Badge: Expert en progression • Excellents resultats repetes";
        }
        if (averageScore >= 70) {
            return "Badge: Bonne dynamique • Les tentatives montrent une progression solide";
        }
        if (bestScore >= 50) {
            return "Badge: En progression • Encore quelques essais pour stabiliser les resultats";
        }
        return "Badge: Decouverte • Ce quiz merite encore quelques tentatives";
    }

    private String buildScoreCelebrationBadge(int finalScore) {
        if (finalScore >= 90) {
            return "Mode super champion active";
        }
        if (finalScore >= 75) {
            return "Mission reussie avec style";
        }
        if (finalScore >= 50) {
            return "Belle progression en cours";
        }
        return "Bravo pour cette tentative";
    }

    private String buildScoreCelebrationMessage(int finalScore) {
        if (finalScore >= 90) {
            return "Tu as presque tout reussi. Continue comme ca, tu es en pleine forme.";
        }
        if (finalScore >= 75) {
            return "Tres bon score. Encore un petit effort et tu touches le niveau expert.";
        }
        if (finalScore >= 50) {
            return "Tu avances bien. Quelques essais de plus et tu vas grimper tres vite.";
        }
        return "Chaque tentative aide a apprendre. Rejoue pour voir tes progres question par question.";
    }

    private void launchCelebrationConfetti(Pane layer, int intensity) {
        double width = layer.getPrefWidth();
        double height = layer.getPrefHeight();
        String[] colors = {"#49a2ff", "#7df1b8", "#ffd54f", "#ff8cc6", "#ff7b7b"};

        for (int index = 0; index < intensity; index++) {
            Circle piece = new Circle(3 + (Math.random() * 5), Color.web(colors[index % colors.length]));
            piece.setOpacity(0.95);
            piece.setLayoutX(24 + Math.random() * Math.max(40, width - 48));
            piece.setLayoutY(-20 - Math.random() * 50);
            layer.getChildren().add(piece);

            TranslateTransition fall = new TranslateTransition(Duration.seconds(1.6 + Math.random() * 1.2), piece);
            fall.setByY(height + 110);
            fall.setByX(-45 + Math.random() * 90);
            fall.setInterpolator(Interpolator.EASE_IN);

            FadeTransition fade = new FadeTransition(Duration.seconds(2.1 + Math.random() * 0.6), piece);
            fade.setFromValue(0.95);
            fade.setToValue(0);

            ScaleTransition spinBounce = new ScaleTransition(Duration.seconds(0.7 + Math.random() * 0.4), piece);
            spinBounce.setFromX(0.8);
            spinBounce.setFromY(0.8);
            spinBounce.setToX(1.35);
            spinBounce.setToY(1.35);
            spinBounce.setCycleCount(Animation.INDEFINITE);
            spinBounce.setAutoReverse(true);

            ParallelTransition confetti = new ParallelTransition(fall, fade, spinBounce);
            confetti.setOnFinished(event -> layer.getChildren().remove(piece));
            confetti.play();
        }
    }

    private StackPane createTimerCircle(ProgressIndicator timerProgress, Label timerValueLabel) {
        timerProgress.setPrefSize(110, 110);
        timerProgress.setMinSize(110, 110);
        timerProgress.setStyle("-fx-progress-color: #49a2ff;");

        Label timerTitleLabel = new Label("Temps");
        timerTitleLabel.setStyle("-fx-text-fill: #d8e7ff; -fx-font-size: 12px; -fx-font-weight: 700;");
        timerValueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 800;");

        VBox timerTextBox = new VBox(2, timerTitleLabel, timerValueLabel);
        timerTextBox.setAlignment(Pos.CENTER);

        StackPane timerCircle = new StackPane(timerProgress, timerTextBox);
        timerCircle.setPrefSize(126, 126);
        timerCircle.setMinSize(126, 126);
        return timerCircle;
    }

    private void startQuizTimer(
            Quiz quiz,
            ProgressIndicator timerProgress,
            Label timerValueLabel,
            Button validateQuizButton,
            Node navigationContainer,
            Node questionContainer,
            Node chatbotContainer,
            List<Question> quizQuestions,
            Map<Question, TextField> freeTextAnswers,
            Map<Question, List<CheckBox>> qcmAnswers,
            Map<Question, ToggleGroup> qcuAnswers,
            Map<Question, Map<String, String>> matchingAnswers
    ) {
        stopActiveQuizTimer();
        final int totalSeconds = Math.max(quiz.getDureeMinutes(), 1) * 60;
        final int[] remainingSeconds = {totalSeconds};
        updateTimerDisplay(timerProgress, timerValueLabel, remainingSeconds[0], totalSeconds);

        activeQuizTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            remainingSeconds[0]--;
            updateTimerDisplay(timerProgress, timerValueLabel, Math.max(remainingSeconds[0], 0), totalSeconds);

            if (remainingSeconds[0] <= 0) {
                finishQuizAttempt(
                        quiz,
                        quizQuestions,
                        freeTextAnswers,
                        qcmAnswers,
                        qcuAnswers,
                        matchingAnswers,
                        validateQuizButton,
                        navigationContainer,
                        questionContainer,
                        chatbotContainer,
                        true
                );
            }
        }));
        activeQuizTimeline.setCycleCount(Timeline.INDEFINITE);
        activeQuizTimeline.play();
    }

    private void finishQuizAttempt(
            Quiz quiz,
            List<Question> quizQuestions,
            Map<Question, TextField> freeTextAnswers,
            Map<Question, List<CheckBox>> qcmAnswers,
            Map<Question, ToggleGroup> qcuAnswers,
            Map<Question, Map<String, String>> matchingAnswers,
            Button validateQuizButton,
            Node navigationContainer,
            Node questionContainer,
            Node chatbotContainer,
            boolean timeElapsed
    ) {
        if (validateQuizButton.isDisabled()) {
            return;
        }

        stopActiveQuizTimer();
        validateQuizButton.setDisable(true);
        navigationContainer.setDisable(true);
        questionContainer.setDisable(true);
        chatbotContainer.setDisable(true);

        QuestionController.QuizScore score = questionController.computeScore(
                quizQuestions,
                freeTextAnswers,
                qcmAnswers,
                qcuAnswers,
                matchingAnswers
        );
        quizResultController.saveResult(
                quiz,
                score.percent(),
                score.earnedPoints(),
                score.totalPoints(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        );

        if (timeElapsed) {
            showInfo("Temps ecoule", "Le temps du quiz est termine. Le quiz a ete arrete automatiquement.");
        }

        showFinalScore(score.percent(), score.earnedPoints(), score.totalPoints());
    }

    private void updateTimerDisplay(ProgressIndicator timerProgress, Label timerValueLabel, int remainingSeconds, int totalSeconds) {
        timerValueLabel.setText(formatRemainingTime(remainingSeconds));
        timerProgress.setProgress(totalSeconds == 0 ? 0 : (double) remainingSeconds / totalSeconds);
        if (remainingSeconds <= 60) {
            timerProgress.setStyle("-fx-progress-color: #ff6b6b;");
            timerValueLabel.setStyle("-fx-text-fill: #ffb2b2; -fx-font-size: 20px; -fx-font-weight: 800;");
        } else {
            timerProgress.setStyle("-fx-progress-color: #49a2ff;");
            timerValueLabel.setStyle("-fx-text-fill: white; -fx-font-size: 20px; -fx-font-weight: 800;");
        }
    }

    private void stopActiveQuizTimer() {
        if (activeQuizTimeline != null) {
            activeQuizTimeline.stop();
            activeQuizTimeline = null;
        }
    }

    private String formatRemainingTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private Button createHeaderButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("header-button");
        return button;
    }

    private Button createSidebarButton(String text, boolean selected) {
        Button button = new Button(text);
        button.setWrapText(true);
        button.setMaxWidth(Double.MAX_VALUE);
        button.getStyleClass().add("sidebar-button");
        if (selected) {
            button.getStyleClass().add("sidebar-button-active");
        }
        return button;
    }

    private void applyListActionButtonStyle(Button... buttons) {
        for (Button button : buttons) {
            button.setPrefWidth(120);
            button.setMinWidth(120);
            button.setPrefHeight(44);
        }
    }

    private void setHeaderMode(boolean frontOfficeActive) {
        frontHeaderButton.getStyleClass().remove("header-button-primary");
        backHeaderButton.getStyleClass().remove("header-button-primary");
        if (frontOfficeActive) {
            frontHeaderButton.getStyleClass().add("header-button-primary");
        } else {
            backHeaderButton.getStyleClass().add("header-button-primary");
        }
    }

    private void ensureBackOfficeLayout() {
        if (root.getLeft() == null) {
            root.setLeft(buildSidebar());
        }
    }

    private void showFinalScore(int finalScore, int earnedPoints, int totalPoints) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (root.getScene() != null && root.getScene().getWindow() != null) {
            dialog.initOwner(root.getScene().getWindow());
        }
        dialog.setTitle("Score final");

        Label title = new Label("Score final");
        title.getStyleClass().add("score-dialog-title");

        Label score = new Label(finalScore + "%");
        score.getStyleClass().add("score-dialog-score");

        Label details = new Label("Vous avez obtenu " + earnedPoints + " / " + totalPoints + " points.");
        details.getStyleClass().add("score-dialog-details");
        details.setWrapText(true);

        Label rewardBadge = new Label(buildScoreCelebrationBadge(finalScore));
        rewardBadge.getStyleClass().add("score-dialog-badge");

        Label encouragement = new Label(buildScoreCelebrationMessage(finalScore));
        encouragement.getStyleClass().add("score-dialog-message");
        encouragement.setWrapText(true);

        Button closeButton = new Button("Fermer");
        closeButton.getStyleClass().add("front-card-button");
        closeButton.setOnAction(event -> dialog.close());
        installPlayfulButtonAnimations(closeButton);

        VBox content = new VBox(16, title, score, rewardBadge, details, encouragement, closeButton);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(26));
        content.getStyleClass().add("score-dialog-card");
        playEntranceAnimation(content, 0, 0, 18, 0.95);
        installBreathingPulse(score, 1.0, 1.06, 1.6);

        Pane confettiLayer = new Pane();
        confettiLayer.setMouseTransparent(true);
        confettiLayer.setPickOnBounds(false);
        confettiLayer.setPrefSize(420, 320);

        StackPane rootLayer = new StackPane(confettiLayer, content);
        rootLayer.setPadding(new Insets(10));
        rootLayer.setPrefSize(420, 320);

        Scene scene = new Scene(rootLayer, 420, 320);
        if (root.getScene() != null && !root.getScene().getStylesheets().isEmpty()) {
            scene.getStylesheets().addAll(root.getScene().getStylesheets());
        }
        scene.setFill(Color.TRANSPARENT);

        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.setOnShown(event -> launchCelebrationConfetti(confettiLayer, finalScore >= 75 ? 36 : 22));
        dialog.showAndWait();
    }
}
