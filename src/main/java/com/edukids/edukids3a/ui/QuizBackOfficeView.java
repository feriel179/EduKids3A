package com.edukids.edukids3a.ui;

import com.edukids.edukids3a.auth.SessionManager;
import com.edukids.edukids3a.model.Quiz;
import com.edukids.edukids3a.model.Question;
import com.edukids.edukids3a.model.Reponse;
import com.edukids.edukids3a.model.TypeQuestion;
import com.edukids.edukids3a.service.QuestionService;
import com.edukids.edukids3a.service.QuizService;
import com.edukids.edukids3a.validation.ValidationException;
import javafx.collections.transformation.FilteredList;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
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
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizBackOfficeView {

    private final SessionManager sessionManager = new SessionManager();
    private final QuizService quizService;
    private final QuestionService questionService;
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
    private TextArea descriptionArea;
    private Label quizErrorLabel;
    private ComboBox<String> niveauCombo;
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

    public QuizBackOfficeView(QuizService quizService, QuestionService questionService) {
        this.quizService = quizService;
        this.questionService = questionService;
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
        header.getStyleClass().add("top-bar");

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
        sidebar.getStyleClass().add("sidebar");
        return sidebar;
    }

    private void showQuizList() {
        setHeaderMode(false);
        ensureBackOfficeLayout();
        setSidebarSelection(listQuizButton);
        root.setCenter(buildQuizList());
    }

    private void showQuizForm() {
        setHeaderMode(false);
        ensureBackOfficeLayout();
        setSidebarSelection(addQuizButton);
        currentQuiz = null;
        root.setCenter(buildQuizForm());
    }

    private void showQuestionList() {
        setHeaderMode(false);
        ensureBackOfficeLayout();
        setSidebarSelection(listQuestionButton);
        root.setCenter(buildQuestionList());
    }

    private void showQuestionForm() {
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
        setHeaderMode(false);
        ensureBackOfficeLayout();
        setSidebarSelection(statsButton);
        root.setCenter(buildStatisticsPage());
    }

    private void showFrontOfficePage() {
        setHeaderMode(true);
        root.setLeft(null);
        clearSidebarSelection();
        currentFrontQuiz = null;
        root.setCenter(buildFrontOfficePage());
    }

    private void showFrontQuizDetails(Quiz quiz) {
        setHeaderMode(true);
        root.setLeft(null);
        clearSidebarSelection();
        currentFrontQuiz = quiz;
        root.setCenter(buildFrontQuizDetailsPage(quiz));
    }

    private ScrollPane buildQuizList() {
        VBox page = new VBox(22);
        page.setPadding(new Insets(24, 28, 24, 28));

        Label title = new Label("Liste des quiz");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Lorsque vous cliquez sur Ajouter, le quiz saisi est enregistre dans cette liste.");
        subtitle.getStyleClass().add("page-subtitle");

        FilteredList<Quiz> filteredQuizzes = new FilteredList<>(quizService.getAllQuizzes(), quiz -> true);
        TableView<Quiz> table = new TableView<>(FXCollections.observableArrayList(filteredQuizzes));
        table.getStyleClass().add("quiz-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Quiz, String> titleCol = new TableColumn<>("Titre");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("titre"));

        TableColumn<Quiz, Integer> questionCol = new TableColumn<>("Questions");
        questionCol.setCellValueFactory(new PropertyValueFactory<>("nombreQuestions"));

        TableColumn<Quiz, String> statusCol = new TableColumn<>("Statut");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("statut"));

        table.getColumns().addAll(titleCol, questionCol, statusCol);
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
        Button deleteButton = new Button("Supprimer");
        TextField searchField = createTextField("Recherche par titre ou statut");
        searchField.setPrefWidth(260);
        ComboBox<String> sortCombo = createComboBox("Titre A-Z", "Titre Z-A", "Plus de questions", "Moins de questions", "Statut");
        sortCombo.setPrefWidth(180);
        addButton.getStyleClass().add("primary-action");
        editButton.getStyleClass().add("secondary-action");
        deleteButton.getStyleClass().add("danger-action");
        applyListActionButtonStyle(addButton, editButton, deleteButton);
        addButton.setOnAction(event -> showQuizForm());
        editButton.setOnAction(event -> {
            Quiz selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openQuizForEdit(selected);
            } else {
                showInfo("Selection", "Selectionnez un quiz a modifier.");
            }
        });
        deleteButton.setOnAction(event -> {
            Quiz selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                quizService.supprimerQuiz(selected);
            } else {
                showInfo("Selection", "Selectionnez un quiz a supprimer.");
            }
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredQuizzes.setPredicate(quiz -> matchesQuizSearch(quiz, newValue));
            refreshQuizTable(table, filteredQuizzes, sortCombo.getValue());
        });
        sortCombo.setOnAction(event -> refreshQuizTable(table, filteredQuizzes, sortCombo.getValue()));
        refreshQuizTable(table, filteredQuizzes, sortCombo.getValue());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox listHeader = new HBox(12, new VBox(6, title, subtitle), spacer, searchField, sortCombo);
        listHeader.setAlignment(Pos.CENTER_LEFT);

        HBox listActions = new HBox(14, addButton, editButton, deleteButton);
        listActions.getStyleClass().add("list-actions-row");

        VBox card = new VBox(18, listHeader, table, listActions);
        card.setPadding(new Insets(24));
        card.getStyleClass().add("card");

        page.getChildren().add(card);

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private ScrollPane buildQuizForm() {
        VBox page = new VBox(22);
        page.setPadding(new Insets(24, 28, 24, 28));

        Label pageTitle = new Label("Fiche quiz");
        pageTitle.getStyleClass().add("page-title");
        Label pageSubtitle = new Label(currentQuiz == null
                ? "Remplissez les champs de votre entite Quiz puis cliquez sur Ajouter."
                : "Modifiez les champs puis cliquez sur Enregistrer.");
        pageSubtitle.getStyleClass().add("page-subtitle");
        VBox titleBox = new VBox(6, pageTitle, pageSubtitle);

        titreField = createTextField("Ex. Quiz Java debutant");
        descriptionArea = createTextArea("Decrivez ici le quiz...", 5);
        quizErrorLabel = createErrorLabel();
        niveauCombo = createComboBox("Debutant", "Intermediaire", "Avance");
        questionSpinner = createSpinner(1, 100, 10);
        dureeSpinner = createSpinner(1, 180, 20);
        scoreSpinner = createSpinner(0, 100, 60);
        statutCombo = createComboBox("Brouillon", "Publie", "Archive");
        installMaxLengthFormatter(titreField, 120);
        installMaxLengthFormatter(descriptionArea, 1000);

        VBox presentationCard = createCard(
                "Presentation",
                createFormRow("Titre *", titreField),
                createFormRow("Description *", descriptionArea),
                createFormRow("Niveau *", niveauCombo)
        );

        VBox detailsCard = createCard(
                "Details du quiz",
                createFormRow("Nombre de questions *", questionSpinner),
                createFormRow("Duree (minutes) *", dureeSpinner),
                createFormRow("Score minimum *", scoreSpinner),
                createFormRow("Statut *", statutCombo)
        );

        Button backButton = new Button("<- Retour a la liste");
        Button saveButton = new Button("Enregistrer");
        Button deleteButton = new Button("Supprimer");
        Button addButton = new Button(currentQuiz == null ? "Ajouter" : "Nouveau");

        backButton.getStyleClass().add("secondary-action");
        saveButton.getStyleClass().add("primary-action");
        deleteButton.getStyleClass().add("danger-action");
        addButton.getStyleClass().add("primary-action");

        backButton.setOnAction(event -> showQuizList());
        addButton.setOnAction(event -> {
            currentQuiz = null;
            root.setCenter(buildQuizForm());
        });
        saveButton.setOnAction(event -> enregistrerQuiz());
        deleteButton.setOnAction(event -> supprimerQuizEnCours());

        HBox actions = new HBox(14, backButton, addButton, saveButton, deleteButton);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(16));
        actions.getStyleClass().add("actions-card");

        page.getChildren().addAll(titleBox, quizErrorLabel, presentationCard, detailsCard, actions);

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

        Label title = new Label("Liste des questions");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Chaque question doit etre liee a un quiz et peut etre QCM, QCU ou reponse libre.");
        subtitle.getStyleClass().add("page-subtitle");

        FilteredList<Question> filteredQuestions = new FilteredList<>(questionService.getAllQuestions(), question -> true);
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
                questionService.supprimerQuestion(selected);
            } else {
                showInfo("Selection", "Selectionnez une question a supprimer.");
            }
        });

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredQuestions.setPredicate(question -> matchesQuestionSearch(question, newValue));
            refreshQuestionTable(table, filteredQuestions, sortCombo.getValue());
        });
        sortCombo.setOnAction(event -> refreshQuestionTable(table, filteredQuestions, sortCombo.getValue()));
        refreshQuestionTable(table, filteredQuestions, sortCombo.getValue());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox listHeader = new HBox(12, new VBox(6, title, subtitle), spacer, searchField, sortCombo);
        listHeader.setAlignment(Pos.CENTER_LEFT);

        HBox listActions = new HBox(14, addButton, editButton, deleteButton);
        listActions.getStyleClass().add("list-actions-row");

        VBox card = new VBox(18, listHeader, table, listActions);
        card.setPadding(new Insets(24));
        card.getStyleClass().add("card");

        page.getChildren().add(card);

        ScrollPane scrollPane = new ScrollPane(page);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private ScrollPane buildQuestionForm() {
        VBox page = new VBox(22);
        page.setPadding(new Insets(24, 28, 24, 28));

        Label pageTitle = new Label("Fiche question");
        pageTitle.getStyleClass().add("page-title");
        Label pageSubtitle = new Label(currentQuestion == null
                ? "Selectionnez un quiz puis choisissez le type de question."
                : "Modifiez la question puis cliquez sur Enregistrer.");
        pageSubtitle.getStyleClass().add("page-subtitle");
        VBox titleBox = new VBox(6, pageTitle, pageSubtitle);

        quizCombo = new ComboBox<>(quizService.getAllQuizzes());
        quizCombo.getStyleClass().add("app-combo");
        quizCombo.setMaxWidth(Double.MAX_VALUE);
        if (!quizService.getAllQuizzes().isEmpty()) {
            quizCombo.getSelectionModel().selectFirst();
        }

        questionIntituleArea = createTextArea("Ex. Quel mot-cle permet d'iterer sur une liste en Java ?", 4);
        questionErrorLabel = createErrorLabel();
        typeQuestionCombo = new ComboBox<>();
        typeQuestionCombo.getItems().addAll(TypeQuestion.QCM, TypeQuestion.QCU, TypeQuestion.REPONSE_LIBRE);
        typeQuestionCombo.getSelectionModel().select(TypeQuestion.QCM);
        typeQuestionCombo.getStyleClass().add("app-combo");
        typeQuestionCombo.setMaxWidth(Double.MAX_VALUE);
        typeQuestionCombo.setOnAction(event -> refreshReponseSection());
        pointsSpinner = createSpinner(1, 20, 1);
        installMaxLengthFormatter(questionIntituleArea, 500);

        VBox questionCard = createCard(
                "Question",
                createFormRow("Quiz *", quizCombo),
                createFormRow("Intitule *", questionIntituleArea),
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

        HBox actions = new HBox(14, backButton, addButton, saveButton, deleteButton, clearButton);
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
        reponsesContainer.getChildren().add(heading);

        TypeQuestion type = typeQuestionCombo.getValue();
        if (type == TypeQuestion.REPONSE_LIBRE) {
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

        if (type == TypeQuestion.QCM) {
            correctCheck1 = new CheckBox("Correcte");
            correctCheck2 = new CheckBox("Correcte");
            correctCheck3 = new CheckBox("Correcte");
            correctCheck4 = new CheckBox("Correcte");
            reponsesContainer.getChildren().addAll(
                    createAnswerRow("Choix 1 *", reponseField1, correctCheck1),
                    createAnswerRow("Choix 2 *", reponseField2, correctCheck2),
                    createAnswerRow("Choix 3", reponseField3, correctCheck3),
                    createAnswerRow("Choix 4", reponseField4, correctCheck4)
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
                    createAnswerRow("Choix 1 *", reponseField1, correctRadio1),
                    createAnswerRow("Choix 2 *", reponseField2, correctRadio2),
                    createAnswerRow("Choix 3", reponseField3, correctRadio3),
                    createAnswerRow("Choix 4", reponseField4, correctRadio4)
            );
        }
    }

    private HBox createAnswerRow(String labelText, TextField field, Region selector) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        label.setMinWidth(190);

        HBox fieldLine = new HBox(12, field, selector);
        fieldLine.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(field, Priority.ALWAYS);

        HBox row = new HBox(18, label, fieldLine);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(fieldLine, Priority.ALWAYS);
        return row;
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
                questionService.ajouterQuestion(question);
            } else {
                questionService.modifierQuestion(currentQuestion, question);
            }
            showQuestionList();
        } catch (ValidationException ex) {
            showInlineError(questionErrorLabel, ex.getMessage());
        }
    }

    private List<Reponse> buildReponses() {
        List<Reponse> reponses = new ArrayList<>();
        TypeQuestion type = typeQuestionCombo.getValue();
        if (type == TypeQuestion.REPONSE_LIBRE) {
            reponses.add(new Reponse(reponseLibreField.getText().trim(), true));
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

    private boolean isCorrect(int index) {
        if (typeQuestionCombo.getValue() == TypeQuestion.QCM) {
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
        Quiz quiz = new Quiz(
                currentQuiz == null ? null : currentQuiz.getId(),
                titreField.getText().trim(),
                descriptionArea.getText().trim(),
                niveauCombo.getValue(),
                questionSpinner.getValue(),
                dureeSpinner.getValue(),
                scoreSpinner.getValue(),
                statutCombo.getValue()
        );

        try {
            quizErrorLabel.setVisible(false);
            if (currentQuiz == null) {
                quizService.ajouterQuiz(quiz);
            } else {
                quizService.modifierQuiz(quiz);
            }
            showQuizList();
        } catch (ValidationException ex) {
            showInlineError(quizErrorLabel, ex.getMessage());
        }
    }

    private void supprimerQuizEnCours() {
        if (currentQuiz != null) {
            quizService.supprimerQuiz(currentQuiz);
            currentQuiz = null;
            showQuizList();
            return;
        }
        clearForm();
    }

    private void clearForm() {
        titreField.clear();
        descriptionArea.clear();
        quizErrorLabel.setVisible(false);
        niveauCombo.getSelectionModel().selectFirst();
        questionSpinner.getValueFactory().setValue(10);
        dureeSpinner.getValueFactory().setValue(20);
        scoreSpinner.getValueFactory().setValue(60);
        statutCombo.getSelectionModel().selectFirst();
    }

    private void clearQuestionForm() {
        questionErrorLabel.setVisible(false);
        if (quizCombo != null && !quizService.getAllQuizzes().isEmpty()) {
            quizCombo.getSelectionModel().selectFirst();
        }
        if (questionIntituleArea != null) {
            questionIntituleArea.clear();
        }
        if (typeQuestionCombo != null) {
            typeQuestionCombo.getSelectionModel().select(TypeQuestion.QCM);
        }
        if (pointsSpinner != null) {
            pointsSpinner.getValueFactory().setValue(1);
        }
        refreshReponseSection();
    }

    private void supprimerQuestionEnCours() {
        if (currentQuestion != null) {
            questionService.supprimerQuestion(currentQuestion);
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
        descriptionArea.setText(quiz.getDescription());
        niveauCombo.setValue(quiz.getNiveau());
        questionSpinner.getValueFactory().setValue(quiz.getNombreQuestions());
        dureeSpinner.getValueFactory().setValue(quiz.getDureeMinutes());
        scoreSpinner.getValueFactory().setValue(quiz.getScoreMinimum());
        statutCombo.setValue(quiz.getStatut());
    }

    private void remplirQuestionForm(Question question) {
        quizCombo.setValue(question.getQuiz());
        questionIntituleArea.setText(question.getIntitule());
        typeQuestionCombo.setValue(question.getType());
        pointsSpinner.getValueFactory().setValue(question.getPoints());
        refreshReponseSection();

        if (question.getType() == TypeQuestion.REPONSE_LIBRE) {
            if (!question.getReponses().isEmpty()) {
                reponseLibreField.setText(question.getReponses().get(0).getTexte());
            }
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

    private void setCorrectSelection(int index, boolean selected) {
        if (typeQuestionCombo.getValue() == TypeQuestion.QCM) {
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
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Verification");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInlineError(Label label, String message) {
        label.setText(message);
        label.setVisible(true);
        label.setManaged(true);
    }

    private void showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
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

        Label title = new Label("Statistiques");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Vue resumee du contenu enregistre dans la base de donnees.");
        subtitle.getStyleClass().add("page-subtitle");

        int nombreQuiz = quizService.getAllQuizzes().size();
        int nombreQuestions = questionService.getAllQuestions().size();
        long nombreReponses = questionService.getAllQuestions().stream()
                .mapToLong(question -> question.getReponses().size())
                .sum();

        VBox quizCard = createSimpleStatCard("Quiz", String.valueOf(nombreQuiz));
        VBox questionCard = createSimpleStatCard("Questions", String.valueOf(nombreQuestions));
        VBox reponseCard = createSimpleStatCard("Reponses", String.valueOf(nombreReponses));

        HBox statsRow = new HBox(18, quizCard, questionCard, reponseCard);
        VBox pageBox = new VBox(18, title, subtitle, statsRow);

        ScrollPane scrollPane = new ScrollPane(pageBox);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private ScrollPane buildFrontOfficePage() {
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

        VBox hero = new VBox(10);
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(42, 20, 42, 20));
        hero.getStyleClass().add("front-hero");
        Label heroTitle = new Label("Nos quiz");
        heroTitle.getStyleClass().add("front-hero-title");
        Label heroSubtitle = new Label("Explorez, recherchez et lancez les quiz disponibles.");
        heroSubtitle.getStyleClass().add("front-hero-subtitle");
        Label heroStats = new Label(buildFrontHeroStatsText());
        heroStats.getStyleClass().add("front-hero-stats");
        hero.getChildren().addAll(heroTitle, heroSubtitle, heroStats);

        FlowPane cardsContainer = new FlowPane();
        cardsContainer.setHgap(22);
        cardsContainer.setVgap(22);
        cardsContainer.setPadding(new Insets(0, 32, 32, 32));

        FilteredList<Quiz> filteredQuizzes = new FilteredList<>(quizService.getAllQuizzes(), quiz -> true);
        TextField searchField = createTextField("Rechercher un quiz...");
        searchField.setPrefWidth(340);
        ComboBox<String> sortCombo = createComboBox("Plus recent", "Titre A-Z", "Titre Z-A", "Niveau");
        sortCombo.setPrefWidth(180);
        Button searchButton = new Button("⌕");
        searchButton.getStyleClass().add("front-search-button");
        Button resetButton = new Button("Reinitialiser");
        resetButton.getStyleClass().add("front-reset-button");

        Label countLabel = new Label();
        countLabel.getStyleClass().add("front-count-label");

        HBox toolsBar = new HBox(14);
        toolsBar.setAlignment(Pos.CENTER_LEFT);
        toolsBar.setPadding(new Insets(26, 32, 18, 32));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolsBar.getChildren().addAll(searchField, searchButton, sortCombo, resetButton, spacer, countLabel);

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredQuizzes.setPredicate(quiz -> matchesQuizSearch(quiz, newValue));
            refreshFrontQuizCards(cardsContainer, filteredQuizzes, sortCombo.getValue(), countLabel);
        });
        searchButton.setOnAction(event -> {
            filteredQuizzes.setPredicate(quiz -> matchesQuizSearch(quiz, searchField.getText()));
            refreshFrontQuizCards(cardsContainer, filteredQuizzes, sortCombo.getValue(), countLabel);
        });
        resetButton.setOnAction(event -> {
            searchField.clear();
            sortCombo.getSelectionModel().selectFirst();
            filteredQuizzes.setPredicate(quiz -> true);
            refreshFrontQuizCards(cardsContainer, filteredQuizzes, sortCombo.getValue(), countLabel);
        });
        sortCombo.setOnAction(event -> refreshFrontQuizCards(cardsContainer, filteredQuizzes, sortCombo.getValue(), countLabel));
        refreshFrontQuizCards(cardsContainer, filteredQuizzes, sortCombo.getValue(), countLabel);

        HBox frontStatsRow = new HBox(
                18,
                createFrontMetricCard("Quiz disponibles", String.valueOf(quizService.getAllQuizzes().size())),
                createFrontMetricCard("Questions", String.valueOf(questionService.getAllQuestions().size())),
                createFrontMetricCard("Niveaux", String.valueOf(quizService.getAllQuizzes().stream().map(Quiz::getNiveau).distinct().count()))
        );
        frontStatsRow.setPadding(new Insets(26, 32, 10, 32));

        page.getChildren().addAll(navBar, hero, frontStatsRow, toolsBar, cardsContainer);

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

    private void clearSidebarSelection() {
        listQuizButton.getStyleClass().remove("sidebar-button-active");
        addQuizButton.getStyleClass().remove("sidebar-button-active");
        listQuestionButton.getStyleClass().remove("sidebar-button-active");
        addQuestionButton.getStyleClass().remove("sidebar-button-active");
        statsButton.getStyleClass().remove("sidebar-button-active");
    }

    private boolean matchesQuizSearch(Quiz quiz, String searchValue) {
        if (searchValue == null || searchValue.isBlank()) {
            return true;
        }
        String query = searchValue.toLowerCase();
        return quiz.getTitre().toLowerCase().contains(query)
                || quiz.getStatut().toLowerCase().contains(query)
                || quiz.getNiveau().toLowerCase().contains(query);
    }

    private boolean matchesQuestionSearch(Question question, String searchValue) {
        if (searchValue == null || searchValue.isBlank()) {
            return true;
        }
        String query = searchValue.toLowerCase();
        return question.getIntitule().toLowerCase().contains(query)
                || question.getQuizTitre().toLowerCase().contains(query)
                || question.getTypeLabel().toLowerCase().contains(query)
                || question.getResumeReponses().toLowerCase().contains(query);
    }

    private void refreshQuizTable(TableView<Quiz> table, FilteredList<Quiz> filteredQuizzes, String sortValue) {
        List<Quiz> items = new ArrayList<>(filteredQuizzes);
        items.sort(getQuizComparator(sortValue));
        table.setItems(FXCollections.observableArrayList(items));
    }

    private Comparator<Quiz> getQuizComparator(String sortValue) {
        return switch (sortValue) {
            case "Titre Z-A" -> Comparator.comparing(Quiz::getTitre, String.CASE_INSENSITIVE_ORDER).reversed();
            case "Plus de questions" -> Comparator.comparingInt(Quiz::getNombreQuestions).reversed();
            case "Moins de questions" -> Comparator.comparingInt(Quiz::getNombreQuestions);
            case "Statut" -> Comparator.comparing(Quiz::getStatut, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Quiz::getTitre, String.CASE_INSENSITIVE_ORDER);
        };
    }

    private Comparator<Quiz> getFrontQuizComparator(String sortValue) {
        return switch (sortValue) {
            case "Titre A-Z" -> Comparator.comparing(Quiz::getTitre, String.CASE_INSENSITIVE_ORDER);
            case "Titre Z-A" -> Comparator.comparing(Quiz::getTitre, String.CASE_INSENSITIVE_ORDER).reversed();
            case "Niveau" -> Comparator.comparing(Quiz::getNiveau, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(quiz -> quiz.getId() == null ? 0 : quiz.getId(), Comparator.reverseOrder());
        };
    }

    private void refreshQuestionTable(TableView<Question> table, FilteredList<Question> filteredQuestions, String sortValue) {
        List<Question> items = new ArrayList<>(filteredQuestions);
        items.sort(getQuestionComparator(sortValue));
        table.setItems(FXCollections.observableArrayList(items));
    }

    private Comparator<Question> getQuestionComparator(String sortValue) {
        return switch (sortValue) {
            case "Question Z-A" -> Comparator.comparing(Question::getIntitule, String.CASE_INSENSITIVE_ORDER).reversed();
            case "Quiz A-Z" -> Comparator.comparing(Question::getQuizTitre, String.CASE_INSENSITIVE_ORDER);
            case "Type" -> Comparator.comparing(Question::getTypeLabel, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Question::getIntitule, String.CASE_INSENSITIVE_ORDER);
        };
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

    private void refreshFrontQuizCards(FlowPane container, FilteredList<Quiz> filteredQuizzes, String sortValue, Label countLabel) {
        List<Quiz> items = new ArrayList<>(filteredQuizzes);
        items.sort(getFrontQuizComparator(sortValue));
        container.getChildren().clear();

        if (items.isEmpty()) {
            container.getChildren().add(createFrontEmptyState());
            countLabel.setText("0 quiz");
            return;
        }

        for (Quiz quiz : items) {
            container.getChildren().add(createFrontQuizCard(quiz));
        }

        countLabel.setText(items.size() + " quiz");
    }

    private VBox createFrontQuizCard(Quiz quiz) {
        VBox card = new VBox(12);
        card.setPrefWidth(300);
        card.getStyleClass().add("front-quiz-card");

        Region image = new Region();
        image.getStyleClass().add("front-card-image");
        image.setPrefSize(268, 160);

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
        card.setOnMouseClicked(event -> showFrontQuizDetails(quiz));

        card.getChildren().addAll(image, meta, title, description, status, extra, detailButton);
        return card;
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

        VBox hero = new VBox(10);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.setPadding(new Insets(36, 32, 36, 32));
        hero.getStyleClass().add("front-hero");
        Label backBreadcrumb = new Label("Accueil • Quiz • " + quiz.getTitre());
        backBreadcrumb.getStyleClass().add("front-hero-subtitle");
        Label heroTitle = new Label(quiz.getTitre());
        heroTitle.getStyleClass().add("front-hero-title");
        Label heroSubtitle = new Label(quiz.getDescription());
        heroSubtitle.getStyleClass().add("front-detail-description");
        Button backButton = new Button("Retour aux quiz");
        backButton.getStyleClass().add("front-card-button");
        backButton.setOnAction(event -> showFrontOfficePage());
        hero.getChildren().addAll(backBreadcrumb, heroTitle, heroSubtitle, backButton);

        HBox infoRow = new HBox(
                18,
                createFrontMetricCard("Niveau", quiz.getNiveau()),
                createFrontMetricCard("Questions", String.valueOf(quiz.getNombreQuestions())),
                createFrontMetricCard("Duree", quiz.getDureeMinutes() + " min"),
                createFrontMetricCard("Score", quiz.getScoreMinimum() + "%")
        );
        infoRow.setPadding(new Insets(26, 32, 8, 32));

        VBox detailsCard = new VBox(16);
        detailsCard.setPadding(new Insets(24));
        detailsCard.getStyleClass().add("card");
        Label detailsTitle = new Label("Questions du quiz");
        detailsTitle.getStyleClass().add("page-title");
        Label detailsText = new Label("Statut: " + quiz.getStatut() + " • Parcourez les questions disponibles ci-dessous.");
        detailsText.getStyleClass().add("page-subtitle");
        detailsCard.getChildren().addAll(detailsTitle, detailsText);

        List<Question> quizQuestions = questionService.getAllQuestions().stream()
                .filter(question -> question.getQuiz().getId() != null
                        && quiz.getId() != null
                        && question.getQuiz().getId().equals(quiz.getId()))
                .toList();

        Map<Question, TextField> freeTextAnswers = new HashMap<>();
        Map<Question, List<CheckBox>> qcmAnswers = new HashMap<>();
        Map<Question, ToggleGroup> qcuAnswers = new HashMap<>();

        if (quizQuestions.isEmpty()) {
            Label emptyLabel = new Label("Aucune question n'est encore associee a ce quiz.");
            emptyLabel.getStyleClass().add("front-empty-title");
            detailsCard.getChildren().add(emptyLabel);
        } else {
            for (Question question : quizQuestions) {
                detailsCard.getChildren().add(createFrontQuestionPreview(question, freeTextAnswers, qcmAnswers, qcuAnswers));
            }

            Button validateQuizButton = new Button("Valider le quiz");
            validateQuizButton.getStyleClass().add("front-validate-button");
            validateQuizButton.setOnAction(event -> {
                int totalPoints = quizQuestions.stream().mapToInt(Question::getPoints).sum();
                int earnedPoints = 0;

                for (Question question : quizQuestions) {
                    if (question.getType() == TypeQuestion.REPONSE_LIBRE) {
                        TextField answerField = freeTextAnswers.get(question);
                        String userAnswer = answerField == null ? "" : answerField.getText().trim();
                        String expected = question.getReponses().isEmpty() ? "" : question.getReponses().get(0).getTexte().trim();
                        if (userAnswer.equalsIgnoreCase(expected)) {
                            earnedPoints += question.getPoints();
                        }
                        continue;
                    }

                    if (question.getType() == TypeQuestion.QCU) {
                        ToggleGroup toggleGroup = qcuAnswers.get(question);
                        if (toggleGroup != null && toggleGroup.getSelectedToggle() instanceof RadioButton selectedButton) {
                            String selectedText = selectedButton.getText();
                            boolean isCorrect = question.getReponses().stream()
                                    .anyMatch(reponse -> reponse.isCorrecte() && reponse.getTexte().equals(selectedText));
                            if (isCorrect) {
                                earnedPoints += question.getPoints();
                            }
                        }
                        continue;
                    }

                    List<CheckBox> selectedBoxes = qcmAnswers.get(question);
                    if (selectedBoxes != null) {
                        List<String> selectedTexts = selectedBoxes.stream()
                                .filter(CheckBox::isSelected)
                                .map(CheckBox::getText)
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList();

                        List<String> correctTexts = question.getReponses().stream()
                                .filter(Reponse::isCorrecte)
                                .map(Reponse::getTexte)
                                .sorted(String.CASE_INSENSITIVE_ORDER)
                                .toList();

                        if (selectedTexts.equals(correctTexts)) {
                            earnedPoints += question.getPoints();
                        }
                    }
                }

                int finalScore = totalPoints == 0 ? 0 : (earnedPoints * 100) / totalPoints;
                showFinalScore(finalScore, earnedPoints, totalPoints);
            });

            detailsCard.getChildren().add(validateQuizButton);
        }

        VBox wrapper = new VBox(navBar, hero, infoRow, detailsCard);
        VBox.setMargin(detailsCard, new Insets(18, 32, 32, 32));

        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("content-scroll");
        return scrollPane;
    }

    private VBox createFrontMetricCard(String label, String value) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("front-metric-value");
        Label labelLabel = new Label(label);
        labelLabel.getStyleClass().add("front-metric-label");
        VBox card = new VBox(8, valueLabel, labelLabel);
        card.setPrefWidth(200);
        card.getStyleClass().add("front-metric-card");
        return card;
    }

    private VBox createFrontQuestionPreview(
            Question question,
            Map<Question, TextField> freeTextAnswers,
            Map<Question, List<CheckBox>> qcmAnswers,
            Map<Question, ToggleGroup> qcuAnswers
    ) {
        VBox box = new VBox(8);
        box.getStyleClass().add("front-question-preview");

        Label order = new Label("Question");
        order.getStyleClass().add("front-question-order");
        Label title = new Label(question.getIntitule());
        title.getStyleClass().add("front-question-title");
        Label meta = new Label(question.getTypeLabel() + " • " + question.getPoints() + " point(s)");
        meta.getStyleClass().add("front-question-meta");

        VBox answersBox = new VBox(10);
        answersBox.getStyleClass().add("front-question-answers-box");
        if (question.getType() == TypeQuestion.REPONSE_LIBRE) {
            TextField freeAnswerField = createTextField("Tapez votre reponse...");
            freeAnswerField.getStyleClass().add("front-free-answer-field");
            freeTextAnswers.put(question, freeAnswerField);
            answersBox.getChildren().add(freeAnswerField);
        } else if (question.getType() == TypeQuestion.QCU) {
            ToggleGroup toggleGroup = new ToggleGroup();
            qcuAnswers.put(question, toggleGroup);
            for (Reponse reponse : question.getReponses()) {
                RadioButton radioButton = new RadioButton(reponse.getTexte());
                radioButton.setToggleGroup(toggleGroup);
                radioButton.getStyleClass().add("front-radio-answer");
                radioButton.setWrapText(true);
                answersBox.getChildren().add(radioButton);
            }
        } else {
            List<CheckBox> boxes = new ArrayList<>();
            for (Reponse reponse : question.getReponses()) {
                CheckBox checkBox = new CheckBox(reponse.getTexte());
                checkBox.getStyleClass().add("front-check-answer");
                checkBox.setWrapText(true);
                boxes.add(checkBox);
                answersBox.getChildren().add(checkBox);
            }
            qcmAnswers.put(question, boxes);
        }

        box.getChildren().addAll(order, title, meta, answersBox);
        return box;
    }

    private VBox createFrontEmptyState() {
        VBox box = new VBox(8);
        box.setPrefWidth(420);
        box.setPadding(new Insets(28));
        box.getStyleClass().add("front-empty-card");
        Label title = new Label("Aucun quiz ne correspond a votre recherche");
        title.getStyleClass().add("front-empty-title");
        Label subtitle = new Label("Essayez un autre mot-cle ou reinitialisez les filtres.");
        subtitle.getStyleClass().add("front-empty-subtitle");
        box.getChildren().addAll(title, subtitle);
        return box;
    }

    private String buildFrontHeroStatsText() {
        return quizService.getAllQuizzes().size() + " quiz • "
                + questionService.getAllQuestions().size() + " questions • experience interactive";
    }

    private Spinner<Integer> createSpinner(int min, int max, int initialValue) {
        Spinner<Integer> spinner = new Spinner<>(min, max, initialValue);
        spinner.setEditable(true);
        spinner.getStyleClass().add("app-spinner");
        spinner.setMaxWidth(Double.MAX_VALUE);
        return spinner;
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

        Button closeButton = new Button("Fermer");
        closeButton.getStyleClass().add("front-card-button");
        closeButton.setOnAction(event -> dialog.close());

        VBox content = new VBox(16, title, score, details, closeButton);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(26));
        content.getStyleClass().add("score-dialog-card");

        Scene scene = new Scene(content, 360, 250);
        if (root.getScene() != null && !root.getScene().getStylesheets().isEmpty()) {
            scene.getStylesheets().addAll(root.getScene().getStylesheets());
        }
        scene.setFill(Color.TRANSPARENT);

        dialog.setScene(scene);
        dialog.setResizable(false);
        dialog.showAndWait();
    }
}
