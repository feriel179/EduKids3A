package com.edukids.edukids3a.ui;

import com.edukids.edukids3a.model.Evenement;
import com.edukids.edukids3a.model.Programme;
import com.edukids.edukids3a.model.TypeEvenement;
import com.edukids.edukids3a.service.EvenementService;
import com.edukids.edukids3a.service.ProgrammeService;
import com.edukids.edukids3a.validation.EvenementValidator;
import com.edukids.edukids3a.validation.ProgrammeValidator;
import com.edukids.edukids3a.validation.ValidationException;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.util.Duration;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy").withLocale(Locale.FRENCH);

    private static final String SORT_RECENT = "Plus récent";
    private static final String SORT_ANCIEN = "Plus ancien";
    private static final String SORT_TITRE = "Titre (A-Z)";

    /** Taille demandée au décodage pour les cartes (px) */
    private static final double IMG_CARD_W = 600;
    private static final double IMG_CARD_H = 320;
    /** Taille demandée au décodage pour la vue détail (meilleure netteté à l’agrandissement) */
    private static final double IMG_DETAIL_W = 1200;
    private static final double IMG_DETAIL_H = 560;
    /** Aperçu dans le formulaire back-office */
    private static final double IMG_PREVIEW_W = 720;
    private static final double IMG_PREVIEW_H = 360;
    /** Miniature dans le tableau back-office */
    private static final double IMG_TABLE_THUMB_W = 96;
    private static final double IMG_TABLE_THUMB_H = 96;

    private final EvenementService evenementService = new EvenementService();
    private final ProgrammeService programmeService = new ProgrammeService();

    private final ObservableList<Evenement> evenementsData = FXCollections.observableArrayList();
    private final ObservableList<Programme> programmesData = FXCollections.observableArrayList();
    private FilteredList<Evenement> evenementsFiltresBack;

    @FXML
    private VBox paneFrontOffice;
    @FXML
    private VBox paneBackOffice;
    private final ToggleGroup modeGroup = new ToggleGroup();
    @FXML
    private ToggleButton toggleFrontOffice;
    @FXML
    private ToggleButton toggleBackOffice;
    @FXML
    private TabPane tabPaneBack;
    private final ToggleGroup backEvViewGroup = new ToggleGroup();
    @FXML
    private ToggleButton toggleBackEvListe;
    @FXML
    private ToggleButton toggleBackEvForm;
    @FXML
    private ToggleButton toggleBackEvDetail;
    @FXML
    private VBox paneBackEvListe;
    @FXML
    private VBox paneBackEvFormulaire;
    @FXML
    private VBox paneBackEvDetail;
    @FXML
    private Label lblBackEvDetailTitre;
    @FXML
    private Label lblBackEvDetailDescription;
    @FXML
    private Label lblBackEvDetailDate;
    @FXML
    private Label lblBackEvDetailHoraires;
    @FXML
    private Label lblBackEvDetailType;
    @FXML
    private Label lblBackEvDetailLieu;
    @FXML
    private Label lblBackEvDetailPlaces;
    @FXML
    private Label lblBackEvDetailProgramme;
    @FXML
    private Label lblBackEvDetailImageChemin;
    @FXML
    private Label lblBackEvDetailImagePlaceholder;
    @FXML
    private StackPane backEvDetailImgWrap;
    @FXML
    private ImageView imgBackEvDetailEvenement;
    private final ToggleGroup backPrViewGroup = new ToggleGroup();
    @FXML
    private ToggleButton toggleBackPrListe;
    @FXML
    private ToggleButton toggleBackPrForm;
    @FXML
    private VBox paneBackPrListe;
    @FXML
    private VBox paneBackPrFormulaire;

    @FXML
    private FlowPane flowEvenementsCards;
    @FXML
    private FlowPane flowProgrammesCards;
    @FXML
    private TextField tfFrontSearchEvenements;
    @FXML
    private ComboBox<String> cbFrontSortEvenements;
    @FXML
    private Label lblFrontEvenementsCount;
    @FXML
    private TextField tfFrontSearchProgrammes;
    @FXML
    private Label lblFrontProgrammesCount;

    @FXML
    private VBox boxFrontEvList;
    @FXML
    private VBox boxFrontEvDetail;
    @FXML
    private ScrollPane spFrontEvDetail;
    @FXML
    private VBox boxFrontDetailScrollContent;
    @FXML
    private StackPane detailHeroImgWrap;
    @FXML
    private ImageView imgFrontDetailEvenement;
    @FXML
    private Label lblFrontDetailImagePlaceholder;
    @FXML
    private Label lblFrontDetailTitre;
    @FXML
    private FlowPane boxFrontDetailBadges;
    @FXML
    private Label lblFrontDetailDescription;
    @FXML
    private Label lblFrontDetailProgrammeInfo;
    @FXML
    private VBox boxFrontDetailProgramme;
    @FXML
    private Label lblFrontDetailPause;
    @FXML
    private Label lblFrontDetailActivites;
    @FXML
    private Label lblFrontDetailDocs;
    @FXML
    private Label lblFrontDetailMateriels;

    @FXML
    private TableView<Evenement> tableEvenements;
    @FXML
    private TableColumn<Evenement, Evenement> colEvEvenement;
    @FXML
    private TableColumn<Evenement, Evenement> colEvDescription;
    @FXML
    private TableColumn<Evenement, Evenement> colEvDateAff;
    @FXML
    private TableColumn<Evenement, Evenement> colEvHorairesAff;
    @FXML
    private TableColumn<Evenement, Evenement> colEvProgrammeAff;
    @FXML
    private TableColumn<Evenement, Evenement> colEvActions;
    @FXML
    private TextField tfBackEvSearch;
    @FXML
    private Label lblBackEvResultCount;
    @FXML
    private Label lblEmptyBackEvenements;
    @FXML
    private Label lblEmptyBackProgrammes;
    @FXML
    private Label lblEmptyFrontEvenements;
    @FXML
    private Label lblEmptyFrontProgrammes;

    @FXML
    private TextField tfEvTitre;
    @FXML
    private TextArea taEvDescription;
    @FXML
    private DatePicker dpEvDate;
    @FXML
    private Spinner<Integer> spEvDebutHeure;
    @FXML
    private Spinner<Integer> spEvDebutMinute;
    @FXML
    private Spinner<Integer> spEvFinHeure;
    @FXML
    private Spinner<Integer> spEvFinMinute;
    @FXML
    private ComboBox<TypeEvenement> cbEvType;
    @FXML
    private TextField tfEvImage;
    @FXML
    private ImageView imgBackOfficeEvenementPreview;
    @FXML
    private Label lblBackOfficeImagePlaceholder;

    private PauseTransition apercuImageDebounce;
    @FXML
    private TextField tfEvLocalisation;
    @FXML
    private TextField tfEvPlaces;

    @FXML
    private TableView<Programme> tableProgrammes;
    @FXML
    private TableColumn<Programme, String> colPrEvenement;
    @FXML
    private TableColumn<Programme, LocalTime> colPrPauseDebut;
    @FXML
    private TableColumn<Programme, LocalTime> colPrPauseFin;

    @FXML
    private ComboBox<Evenement> cbPrEvenement;
    @FXML
    private Spinner<Integer> spPrPauseDebutHeure;
    @FXML
    private Spinner<Integer> spPrPauseDebutMinute;
    @FXML
    private Spinner<Integer> spPrPauseFinHeure;
    @FXML
    private Spinner<Integer> spPrPauseFinMinute;
    @FXML
    private TextArea taPrActivites;
    @FXML
    private TextArea taPrDocuments;
    @FXML
    private TextArea taPrMateriels;

    private Programme programmeEnEdition;
    private Evenement evenementEnEdition;

    @FXML
    private void initialize() {
        toggleFrontOffice.setToggleGroup(modeGroup);
        toggleBackOffice.setToggleGroup(modeGroup);
        toggleBackEvListe.setToggleGroup(backEvViewGroup);
        toggleBackEvForm.setToggleGroup(backEvViewGroup);
        toggleBackEvDetail.setToggleGroup(backEvViewGroup);
        toggleBackPrListe.setToggleGroup(backPrViewGroup);
        toggleBackPrForm.setToggleGroup(backPrViewGroup);

        cbEvType.setItems(FXCollections.observableArrayList(TypeEvenement.values()));
        cbEvType.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(TypeEvenement object) {
                return object == null ? "" : object.getLibelle();
            }

            @Override
            public TypeEvenement fromString(String string) {
                return null;
            }
        });

        if (spEvDebutHeure != null) {
            spEvDebutHeure.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9));
            spEvDebutMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
            spEvFinHeure.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12));
            spEvFinMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        }
        if (spPrPauseDebutHeure != null) {
            spPrPauseDebutHeure.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 12));
            spPrPauseDebutMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
            spPrPauseFinHeure.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 13));
            spPrPauseFinMinute.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0));
        }

        if (cbPrEvenement != null) {
            cbPrEvenement.setConverter(new javafx.util.StringConverter<>() {
                @Override
                public String toString(Evenement object) {
                    if (object == null) {
                        return "";
                    }
                    if (object.getTitre() != null && !object.getTitre().isBlank()) {
                        return object.getTitre();
                    }
                    return object.getId() != null ? "Événement #" + object.getId() : "Événement";
                }

                @Override
                public Evenement fromString(String string) {
                    return null;
                }
            });
        }

        cbFrontSortEvenements.setItems(FXCollections.observableArrayList(SORT_RECENT, SORT_ANCIEN, SORT_TITRE));
        cbFrontSortEvenements.getSelectionModel().selectFirst();
        cbFrontSortEvenements.setOnAction(e -> rafraichirCartesEvenementsFront());

        evenementsFiltresBack = new FilteredList<>(evenementsData, e -> true);
        tableEvenements.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableEvenements.setItems(evenementsFiltresBack);
        configurerTableEvenementsBackOffice();
        if (tfBackEvSearch != null) {
            tfBackEvSearch.textProperty().addListener((o, ov, nv) -> {
                String q = nv == null ? "" : nv.trim().toLowerCase(Locale.ROOT);
                evenementsFiltresBack.setPredicate(ev -> {
                    if (q.isEmpty()) {
                        return true;
                    }
                    return contient(ev.getTitre(), q)
                            || contient(ev.getDescription(), q)
                            || contient(ev.getLocalisation(), q);
                });
                mettreAJourCompteurEvenementsBack();
            });
        }
        mettreAJourCompteurEvenementsBack();
        tableEvenements.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                remplirFormulaireEvenement(sel);
            } else {
                evenementEnEdition = null;
                viderFormulaireEvenement();
            }
        });

        colPrEvenement.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getEvenement() != null ? c.getValue().getEvenement().getTitre() : ""));
        colPrPauseDebut.setCellValueFactory(new PropertyValueFactory<>("pauseDebut"));
        colPrPauseFin.setCellValueFactory(new PropertyValueFactory<>("pauseFin"));

        tableProgrammes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableProgrammes.setFixedCellSize(-1);
        tableProgrammes.setItems(programmesData);
        tableProgrammes.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) {
                remplirFormulaireProgramme(sel);
            } else {
                programmeEnEdition = null;
                viderFormulaireProgramme();
                rafraichirComboEvenementsPourProgramme();
            }
        });

        modeGroup.selectedToggleProperty().addListener((obs, old, t) -> appliquerMode(t));
        appliquerMode(modeGroup.getSelectedToggle());

        backEvViewGroup.selectedToggleProperty().addListener((obs, o, n) -> syncVueBackEvenements(n));
        backPrViewGroup.selectedToggleProperty().addListener((obs, o, n) -> syncVueBackProgrammes(n));

        tfFrontSearchEvenements.textProperty().addListener((o, a, b) -> rafraichirCartesEvenementsFront());
        tfFrontSearchProgrammes.textProperty().addListener((o, a, b) -> rafraichirCartesProgrammesFront());

        if (detailHeroImgWrap != null) {
            Rectangle detailClip = new Rectangle();
            detailClip.setArcWidth(20);
            detailClip.setArcHeight(20);
            detailClip.widthProperty().bind(detailHeroImgWrap.widthProperty());
            detailClip.heightProperty().bind(detailHeroImgWrap.heightProperty());
            detailHeroImgWrap.setClip(detailClip);
        }
        if (backEvDetailImgWrap != null) {
            Rectangle clipBackDetail = new Rectangle();
            clipBackDetail.setArcWidth(10);
            clipBackDetail.setArcHeight(10);
            clipBackDetail.widthProperty().bind(backEvDetailImgWrap.widthProperty());
            clipBackDetail.heightProperty().bind(backEvDetailImgWrap.heightProperty());
            backEvDetailImgWrap.setClip(clipBackDetail);
        }
        if (spFrontEvDetail != null && boxFrontDetailScrollContent != null) {
            spFrontEvDetail.setFitToWidth(true);
            boxFrontDetailScrollContent.prefWidthProperty().bind(spFrontEvDetail.widthProperty().subtract(12));
        }

        apercuImageDebounce = new PauseTransition(Duration.millis(350));
        apercuImageDebounce.setOnFinished(e -> actualiserApercuImageBackOffice());
        tfEvImage.textProperty().addListener((obs, oldV, newV) -> {
            apercuImageDebounce.stop();
            apercuImageDebounce.playFromStart();
        });

        onRafraichirEvenements();
        onRafraichirProgrammes();

        Platform.runLater(this::installerRaccourcisScene);
    }

    private void appliquerMode(Toggle t) {
        boolean front = t == toggleFrontOffice;
        paneFrontOffice.setVisible(front);
        paneFrontOffice.setManaged(front);
        paneBackOffice.setVisible(!front);
        paneBackOffice.setManaged(!front);
        if (front) {
            rafraichirCartesEvenementsFront();
            rafraichirCartesProgrammesFront();
        } else {
            allerVueListeEvenements();
            allerVueListeProgrammes();
        }
    }

    private void syncVueBackEvenements(Toggle t) {
        if (paneBackEvListe == null || paneBackEvFormulaire == null || paneBackEvDetail == null) {
            return;
        }
        boolean liste = t == toggleBackEvListe;
        boolean form = t == toggleBackEvForm;
        boolean detail = t == toggleBackEvDetail;
        paneBackEvListe.setVisible(liste);
        paneBackEvListe.setManaged(liste);
        paneBackEvFormulaire.setVisible(form);
        paneBackEvFormulaire.setManaged(form);
        paneBackEvDetail.setVisible(detail);
        paneBackEvDetail.setManaged(detail);
        if (detail) {
            Evenement sel = tableEvenements.getSelectionModel().getSelectedItem();
            if (sel != null) {
                remplirVueDetailEvenementBack(sel);
            } else {
                viderVueDetailEvenementBack();
            }
        }
    }

    private void syncVueBackProgrammes(Toggle t) {
        if (paneBackPrListe == null || paneBackPrFormulaire == null) {
            return;
        }
        boolean liste = t == toggleBackPrListe;
        paneBackPrListe.setVisible(liste);
        paneBackPrListe.setManaged(liste);
        paneBackPrFormulaire.setVisible(!liste);
        paneBackPrFormulaire.setManaged(!liste);
    }

    private void allerVueListeEvenements() {
        if (toggleBackEvListe != null) {
            toggleBackEvListe.setSelected(true);
        }
    }

    private void allerVueListeProgrammes() {
        if (toggleBackPrListe != null) {
            toggleBackPrListe.setSelected(true);
        }
    }

    private void mettreAJourCompteurEvenementsBack() {
        if (lblBackEvResultCount == null) {
            return;
        }
        int n = evenementsFiltresBack != null ? evenementsFiltresBack.size() : evenementsData.size();
        lblBackEvResultCount.setText(n + " résultat(s)");
        majEtatVideBackEvenements();
    }

    private void majEtatVideBackEvenements() {
        if (lblEmptyBackEvenements == null) {
            return;
        }
        boolean vide = evenementsFiltresBack == null || evenementsFiltresBack.isEmpty();
        lblEmptyBackEvenements.setManaged(vide);
        lblEmptyBackEvenements.setVisible(vide);
        if (vide) {
            boolean baseVide = evenementsData.isEmpty();
            lblEmptyBackEvenements.setText(baseVide
                    ? "Aucun événement en base. Utilisez « + Nouvel événement » pour en créer un."
                    : "Aucun événement ne correspond au filtre actuel. Modifiez la recherche ou cliquez sur « Réinitialiser ».");
        }
    }

    private void majEtatVideBackProgrammes() {
        if (lblEmptyBackProgrammes == null) {
            return;
        }
        boolean vide = programmesData.isEmpty();
        lblEmptyBackProgrammes.setManaged(vide);
        lblEmptyBackProgrammes.setVisible(vide);
        if (vide) {
            lblEmptyBackProgrammes.setText(
                    "Aucun programme en base. Utilisez « + Nouveau programme » pour en ajouter un.");
        }
    }

    private void majEtatVideFrontEvenements() {
        if (lblEmptyFrontEvenements == null) {
            return;
        }
        int n = flowEvenementsCards.getChildren().size();
        boolean show = n == 0;
        lblEmptyFrontEvenements.setManaged(show);
        lblEmptyFrontEvenements.setVisible(show);
        if (show) {
            boolean baseVide = evenementsData.isEmpty();
            lblEmptyFrontEvenements.setText(baseVide
                    ? "Aucun événement pour le moment."
                    : "Aucun événement ne correspond à votre recherche ou au tri actuel.");
        }
    }

    private void majEtatVideFrontProgrammes() {
        if (lblEmptyFrontProgrammes == null) {
            return;
        }
        int n = flowProgrammesCards.getChildren().size();
        boolean show = n == 0;
        lblEmptyFrontProgrammes.setManaged(show);
        lblEmptyFrontProgrammes.setVisible(show);
        if (show) {
            boolean baseVide = programmesData.isEmpty();
            lblEmptyFrontProgrammes.setText(baseVide
                    ? "Aucun programme pour le moment."
                    : "Aucun programme ne correspond à votre recherche.");
        }
    }

    private void installerRaccourcisScene() {
        if (paneFrontOffice == null || paneFrontOffice.getScene() == null) {
            return;
        }
        paneFrontOffice.getScene().addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F5) {
                onRafraichirEvenements();
                onRafraichirProgrammes();
                e.consume();
                return;
            }
            if (e.getCode() == KeyCode.ESCAPE) {
                if (boxFrontEvDetail != null && boxFrontEvDetail.isVisible()) {
                    onFrontRetourListeEvenements();
                    e.consume();
                    return;
                }
                if (paneBackEvDetail != null && paneBackEvDetail.isVisible()) {
                    allerVueListeEvenements();
                    e.consume();
                }
            }
        });
    }

    private void retirerClasseErreur(Node n) {
        if (n != null) {
            n.getStyleClass().remove("form-field-error");
        }
    }

    private void ajouterClasseErreur(Node n) {
        if (n != null && !n.getStyleClass().contains("form-field-error")) {
            n.getStyleClass().add("form-field-error");
        }
    }

    private void nettoyerErreursVisuellesEvenement() {
        retirerClasseErreur(tfEvTitre);
        retirerClasseErreur(taEvDescription);
        retirerClasseErreur(tfEvPlaces);
        retirerClasseErreur(dpEvDate);
        for (Spinner<Integer> sp : List.of(spEvDebutHeure, spEvDebutMinute, spEvFinHeure, spEvFinMinute)) {
            retirerClasseErreur(sp);
        }
    }

    private void marquerErreursVisuellesEvenement(String msg) {
        nettoyerErreursVisuellesEvenement();
        if (msg == null) {
            return;
        }
        String m = msg.toLowerCase(Locale.FRENCH);
        if (m.contains("titre")) {
            ajouterClasseErreur(tfEvTitre);
        }
        if (m.contains("description")) {
            ajouterClasseErreur(taEvDescription);
        }
        if (m.contains("date")) {
            ajouterClasseErreur(dpEvDate);
        }
        if (m.contains("heure") || m.contains("fin doit") || m.contains("début")) {
            ajouterClasseErreur(spEvDebutHeure);
            ajouterClasseErreur(spEvDebutMinute);
            ajouterClasseErreur(spEvFinHeure);
            ajouterClasseErreur(spEvFinMinute);
        }
        if (m.contains("places") || m.contains("entier invalide")) {
            ajouterClasseErreur(tfEvPlaces);
        }
    }

    private void nettoyerErreursVisuellesProgramme() {
        retirerClasseErreur(cbPrEvenement);
        retirerClasseErreur(taPrActivites);
        retirerClasseErreur(taPrDocuments);
        retirerClasseErreur(taPrMateriels);
        for (Spinner<Integer> sp : List.of(spPrPauseDebutHeure, spPrPauseDebutMinute, spPrPauseFinHeure,
                spPrPauseFinMinute)) {
            retirerClasseErreur(sp);
        }
    }

    private void marquerErreursVisuellesProgramme(String msg) {
        nettoyerErreursVisuellesProgramme();
        if (msg == null) {
            return;
        }
        String m = msg.toLowerCase(Locale.FRENCH);
        if (m.contains("événement") || m.contains("evenement")) {
            ajouterClasseErreur(cbPrEvenement);
        }
        if (m.contains("pause")) {
            ajouterClasseErreur(spPrPauseDebutHeure);
            ajouterClasseErreur(spPrPauseDebutMinute);
            ajouterClasseErreur(spPrPauseFinHeure);
            ajouterClasseErreur(spPrPauseFinMinute);
        }
        if (m.contains("activités") || m.contains("activites")) {
            ajouterClasseErreur(taPrActivites);
        }
        if (m.contains("documents")) {
            ajouterClasseErreur(taPrDocuments);
        }
        if (m.contains("matériels") || m.contains("materiels")) {
            ajouterClasseErreur(taPrMateriels);
        }
    }

    @FXML
    private void onBackEvResetFiltre() {
        if (tfBackEvSearch != null) {
            tfBackEvSearch.clear();
        }
        if (evenementsFiltresBack != null) {
            evenementsFiltresBack.setPredicate(e -> true);
        }
        mettreAJourCompteurEvenementsBack();
    }

    private void appliquerHeureAuxSpinners(LocalTime debut, LocalTime fin) {
        if (spEvDebutHeure == null) {
            return;
        }
        LocalTime d = debut != null ? debut : LocalTime.of(9, 0);
        LocalTime f = fin != null ? fin : LocalTime.of(12, 0);
        spEvDebutHeure.getValueFactory().setValue(d.getHour());
        spEvDebutMinute.getValueFactory().setValue(d.getMinute());
        spEvFinHeure.getValueFactory().setValue(f.getHour());
        spEvFinMinute.getValueFactory().setValue(f.getMinute());
    }

    private void appliquerPauseAuxSpinnersProgramme(LocalTime debut, LocalTime fin) {
        if (spPrPauseDebutHeure == null) {
            return;
        }
        LocalTime d = debut != null ? debut : LocalTime.of(12, 0);
        LocalTime f = fin != null ? fin : LocalTime.of(13, 0);
        spPrPauseDebutHeure.getValueFactory().setValue(d.getHour());
        spPrPauseDebutMinute.getValueFactory().setValue(d.getMinute());
        spPrPauseFinHeure.getValueFactory().setValue(f.getHour());
        spPrPauseFinMinute.getValueFactory().setValue(f.getMinute());
    }

    private void afficherDetailEvenementBack(Evenement e) {
        if (e == null) {
            return;
        }
        tableEvenements.getSelectionModel().select(e);
        remplirVueDetailEvenementBack(e);
        if (toggleBackEvDetail != null) {
            toggleBackEvDetail.setSelected(true);
        }
    }

    private void remplirVueDetailEvenementBack(Evenement e) {
        if (lblBackEvDetailTitre == null || e == null) {
            return;
        }
        lblBackEvDetailTitre.setText(nvl(e.getTitre()));
        String desc = e.getDescription();
        lblBackEvDetailDescription.setText(desc != null && !desc.isBlank() ? desc.trim() : "—");
        lblBackEvDetailDate.setText(e.getDateEvenement() != null ? e.getDateEvenement().format(DATE_FR) : "—");
        if (e.getHeureDebut() != null && e.getHeureFin() != null) {
            lblBackEvDetailHoraires.setText(TIME_FMT.format(e.getHeureDebut()) + " – "
                    + TIME_FMT.format(e.getHeureFin()));
        } else {
            lblBackEvDetailHoraires.setText("—");
        }
        TypeEvenement te = TypeEvenement.fromCode(e.getTypeEvenement());
        if (te != null) {
            lblBackEvDetailType.setText(te.getLibelle());
        } else if (e.getTypeEvenement() != null && !e.getTypeEvenement().isBlank()) {
            lblBackEvDetailType.setText(e.getTypeEvenement());
        } else {
            lblBackEvDetailType.setText("—");
        }
        String lieu = e.getLocalisation();
        lblBackEvDetailLieu.setText(lieu != null && !lieu.isBlank() ? lieu.trim() : "—");
        lblBackEvDetailPlaces.setText(e.getNbPlacesDisponibles() != null
                ? String.valueOf(e.getNbPlacesDisponibles()) : "—");
        lblBackEvDetailProgramme.setText(e.getProgramme() != null ? "Défini" : "Non défini");
        String imgRaw = blankToNull(e.getImage());
        lblBackEvDetailImageChemin.setText(imgRaw != null ? imgRaw : "—");
        if (imgBackEvDetailEvenement == null) {
            return;
        }
        if (imgRaw == null) {
            imgBackEvDetailEvenement.setImage(null);
            mettreAJourPlaceholderBackEvDetailImage(null);
        } else {
            chargerImageEvenement(imgBackEvDetailEvenement, imgRaw, IMG_PREVIEW_W, IMG_PREVIEW_H);
            StackPane.setAlignment(imgBackEvDetailEvenement, Pos.CENTER);
            Image im = imgBackEvDetailEvenement.getImage();
            if (im != null) {
                im.progressProperty().addListener((o, ov, nv) -> mettreAJourPlaceholderBackEvDetailImage(imgRaw));
                im.errorProperty().addListener((o, ov, nv) -> mettreAJourPlaceholderBackEvDetailImage(imgRaw));
            }
            mettreAJourPlaceholderBackEvDetailImage(imgRaw);
        }
    }

    private void viderVueDetailEvenementBack() {
        if (lblBackEvDetailTitre == null) {
            return;
        }
        lblBackEvDetailTitre.setText("—");
        lblBackEvDetailDescription.setText(
                "Sélectionnez un événement dans la liste, ou cliquez sur « Détail » dans le tableau.");
        lblBackEvDetailDate.setText("—");
        lblBackEvDetailHoraires.setText("—");
        lblBackEvDetailType.setText("—");
        lblBackEvDetailLieu.setText("—");
        lblBackEvDetailPlaces.setText("—");
        lblBackEvDetailProgramme.setText("—");
        lblBackEvDetailImageChemin.setText("—");
        if (imgBackEvDetailEvenement != null) {
            imgBackEvDetailEvenement.setImage(null);
        }
        mettreAJourPlaceholderBackEvDetailImage(null);
    }

    private void mettreAJourPlaceholderBackEvDetailImage(String rawImageRef) {
        if (lblBackEvDetailImagePlaceholder == null || imgBackEvDetailEvenement == null) {
            return;
        }
        if (rawImageRef == null || rawImageRef.isBlank()) {
            lblBackEvDetailImagePlaceholder.setText("Pas d'image renseignée");
            lblBackEvDetailImagePlaceholder.setVisible(true);
            lblBackEvDetailImagePlaceholder.setManaged(true);
            return;
        }
        Image im = imgBackEvDetailEvenement.getImage();
        if (im != null && !im.isError() && im.getProgress() >= 1.0 && im.getWidth() > 0) {
            lblBackEvDetailImagePlaceholder.setVisible(false);
            lblBackEvDetailImagePlaceholder.setManaged(false);
            return;
        }
        if (im != null && im.isError()) {
            lblBackEvDetailImagePlaceholder.setText("Impossible d'afficher cette image.");
        } else {
            lblBackEvDetailImagePlaceholder.setText("Chargement de l'aperçu…");
        }
        lblBackEvDetailImagePlaceholder.setVisible(true);
        lblBackEvDetailImagePlaceholder.setManaged(true);
    }

    private void configurerTableEvenementsBackOffice() {
        tableEvenements.setFixedCellSize(-1);

        colEvEvenement.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        colEvEvenement.setCellFactory(c -> new TableCell<Evenement, Evenement>() {
            @Override
            protected void updateItem(Evenement ev, boolean empty) {
                super.updateItem(ev, empty);
                setAlignment(Pos.CENTER_LEFT);
                if (empty || ev == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                ImageView iv = new ImageView();
                iv.setFitWidth(44);
                iv.setFitHeight(44);
                iv.setPreserveRatio(true);
                iv.setSmooth(true);
                chargerImageEvenement(iv, ev.getImage(), IMG_TABLE_THUMB_W, IMG_TABLE_THUMB_H);
                Label t = new Label(nvl(ev.getTitre()));
                t.getStyleClass().add("back-table-ev-title");
                t.setWrapText(true);
                t.setMaxWidth(220);
                HBox row = new HBox(10);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setMaxHeight(Region.USE_PREF_SIZE);
                row.getChildren().addAll(iv, t);
                setGraphic(row);
                setText(null);
            }
        });

        colEvDescription.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        colEvDescription.setCellFactory(c -> new TableCell<Evenement, Evenement>() {
            @Override
            protected void updateItem(Evenement ev, boolean empty) {
                super.updateItem(ev, empty);
                setAlignment(Pos.CENTER_LEFT);
                if (empty || ev == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label l = new Label(tronquer(ev.getDescription(), 200));
                l.getStyleClass().add("back-table-ev-desc");
                l.setWrapText(true);
                l.setMaxWidth(280);
                l.setMaxHeight(Region.USE_PREF_SIZE);
                setGraphic(l);
                setText(null);
            }
        });

        colEvDateAff.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        colEvDateAff.setCellFactory(c -> new TableCell<Evenement, Evenement>() {
            @Override
            protected void updateItem(Evenement ev, boolean empty) {
                super.updateItem(ev, empty);
                setAlignment(Pos.CENTER_LEFT);
                if (empty || ev == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                if (ev.getDateEvenement() == null) {
                    setGraphic(null);
                    setText("—");
                    return;
                }
                Label badge = new Label(ev.getDateEvenement().format(DATE_FR));
                badge.getStyleClass().add("cell-badge-date");
                badge.setMaxHeight(Region.USE_PREF_SIZE);
                setGraphic(badge);
                setText(null);
            }
        });

        colEvHorairesAff.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        colEvHorairesAff.setCellFactory(c -> new TableCell<Evenement, Evenement>() {
            @Override
            protected void updateItem(Evenement ev, boolean empty) {
                super.updateItem(ev, empty);
                setAlignment(Pos.CENTER_LEFT);
                if (empty || ev == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                LocalTime d = ev.getHeureDebut();
                LocalTime f = ev.getHeureFin();
                if (d == null || f == null) {
                    setText("—");
                    setGraphic(null);
                    return;
                }
                Label startPill = new Label(TIME_FMT.format(d));
                startPill.getStyleClass().add("cell-time-pill-start");
                Label endPill = new Label(TIME_FMT.format(f));
                endPill.getStyleClass().add("cell-time-pill-end");
                HBox times = new HBox(6, startPill, endPill);
                times.setAlignment(Pos.CENTER_LEFT);
                times.setMaxHeight(Region.USE_PREF_SIZE);
                long mins = java.time.Duration.between(d, f).toMinutes();
                if (mins < 0) {
                    mins = 0;
                }
                Label dur = new Label(mins + " min");
                dur.getStyleClass().add("cell-duration-hint");
                dur.setMaxHeight(Region.USE_PREF_SIZE);
                VBox box = new VBox(4, times, dur);
                box.setAlignment(Pos.CENTER_LEFT);
                box.setFillWidth(true);
                box.setMaxHeight(Region.USE_PREF_SIZE);
                setGraphic(box);
                setText(null);
            }
        });

        colEvProgrammeAff.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        colEvProgrammeAff.setCellFactory(c -> new TableCell<Evenement, Evenement>() {
            @Override
            protected void updateItem(Evenement ev, boolean empty) {
                super.updateItem(ev, empty);
                setAlignment(Pos.CENTER_LEFT);
                if (empty || ev == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                boolean def = ev.getProgramme() != null;
                Label b = new Label(def ? "Défini" : "Non défini");
                b.getStyleClass().add(def ? "cell-badge-prog-oui" : "cell-badge-prog-non");
                b.setMaxHeight(Region.USE_PREF_SIZE);
                setGraphic(b);
                setText(null);
            }
        });

        colEvActions.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        colEvActions.setCellFactory(c -> new TableCell<Evenement, Evenement>() {
            @Override
            protected void updateItem(Evenement ev, boolean empty) {
                super.updateItem(ev, empty);
                setAlignment(Pos.CENTER_LEFT);
                if (empty || ev == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Button detail = new Button("Détail");
                detail.getStyleClass().add("back-table-action-link");
                detail.setOnAction(ae -> afficherDetailEvenementBack(ev));
                Button mod = new Button("Modifier");
                mod.getStyleClass().addAll("back-table-action-link", "back-table-action-mod");
                mod.setOnAction(ae -> {
                    tableEvenements.getSelectionModel().select(ev);
                    onBackEvModifierVersFormulaire();
                });
                Button del = new Button("Supprimer");
                del.getStyleClass().addAll("back-table-action-link", "back-table-action-del");
                del.setOnAction(ae -> {
                    tableEvenements.getSelectionModel().select(ev);
                    onSupprimerEvenement();
                });
                HBox bar = new HBox(8, detail, mod, del);
                bar.setAlignment(Pos.CENTER_LEFT);
                bar.setMaxHeight(Region.USE_PREF_SIZE);
                setGraphic(bar);
                setText(null);
            }
        });
    }

    @FXML
    private void onBackEvNouveauVersFormulaire() {
        if (toggleBackEvForm != null) {
            toggleBackEvForm.setSelected(true);
        }
        onNouveauEvenement();
    }

    @FXML
    private void onBackEvModifierVersFormulaire() {
        if (tableEvenements.getSelectionModel().getSelectedItem() == null) {
            erreur("Sélectionnez un événement dans la liste pour le modifier.");
            return;
        }
        if (toggleBackEvForm != null) {
            toggleBackEvForm.setSelected(true);
        }
    }

    @FXML
    private void onBackEvRetourListe() {
        allerVueListeEvenements();
    }

    @FXML
    private void onBackPrNouveauVersFormulaire() {
        if (toggleBackPrForm != null) {
            toggleBackPrForm.setSelected(true);
        }
        onNouveauProgramme();
    }

    @FXML
    private void onBackPrModifierVersFormulaire() {
        if (tableProgrammes.getSelectionModel().getSelectedItem() == null) {
            erreur("Sélectionnez un programme dans la liste pour le modifier.");
            return;
        }
        if (toggleBackPrForm != null) {
            toggleBackPrForm.setSelected(true);
        }
    }

    @FXML
    private void onBackPrRetourListe() {
        allerVueListeProgrammes();
    }

    @FXML
    private void onParcourirImageEvenement() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir une image");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp", "*.bmp"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        Window owner = tfEvImage.getScene() != null ? tfEvImage.getScene().getWindow() : null;
        File f = chooser.showOpenDialog(owner);
        if (f != null) {
            if (apercuImageDebounce != null) {
                apercuImageDebounce.stop();
            }
            tfEvImage.setText(f.getAbsolutePath());
            actualiserApercuImageBackOffice();
        }
    }

    private void actualiserApercuImageBackOffice() {
        if (imgBackOfficeEvenementPreview == null || tfEvImage == null) {
            return;
        }
        String raw = tfEvImage.getText();
        if (raw == null || raw.isBlank()) {
            imgBackOfficeEvenementPreview.setImage(null);
            mettreAJourPlaceholderApercuBackOffice();
            return;
        }
        chargerImageEvenement(imgBackOfficeEvenementPreview, raw.trim(), IMG_PREVIEW_W, IMG_PREVIEW_H);
        StackPane.setAlignment(imgBackOfficeEvenementPreview, Pos.CENTER);
        Image im = imgBackOfficeEvenementPreview.getImage();
        if (im != null) {
            im.progressProperty().addListener((o, ov, nv) -> mettreAJourPlaceholderApercuBackOffice());
            im.errorProperty().addListener((o, ov, nv) -> mettreAJourPlaceholderApercuBackOffice());
        }
        mettreAJourPlaceholderApercuBackOffice();
    }

    private void mettreAJourPlaceholderApercuBackOffice() {
        if (lblBackOfficeImagePlaceholder == null || tfEvImage == null || imgBackOfficeEvenementPreview == null) {
            return;
        }
        String t = tfEvImage.getText();
        if (t == null || t.isBlank()) {
            lblBackOfficeImagePlaceholder.setText("Aucun aperçu — saisissez une URL ou choisissez un fichier");
            lblBackOfficeImagePlaceholder.setVisible(true);
            lblBackOfficeImagePlaceholder.setManaged(true);
            return;
        }
        Image im = imgBackOfficeEvenementPreview.getImage();
        if (im != null && !im.isError() && im.getProgress() >= 1.0 && im.getWidth() > 0) {
            lblBackOfficeImagePlaceholder.setVisible(false);
            lblBackOfficeImagePlaceholder.setManaged(false);
            return;
        }
        if (im != null && im.isError()) {
            lblBackOfficeImagePlaceholder.setText("Impossible d'afficher cette image (URL ou fichier invalide).");
        } else {
            lblBackOfficeImagePlaceholder.setText("Chargement de l'aperçu…");
        }
        lblBackOfficeImagePlaceholder.setVisible(true);
        lblBackOfficeImagePlaceholder.setManaged(true);
    }

    @FXML
    private void onFrontRechercheEvenements() {
        rafraichirCartesEvenementsFront();
    }

    @FXML
    private void onFrontRechercheProgrammes() {
        rafraichirCartesProgrammesFront();
    }

    @FXML
    private void onFrontRetourListeEvenements() {
        boxFrontEvDetail.setVisible(false);
        boxFrontEvDetail.setManaged(false);
        boxFrontEvList.setVisible(true);
        boxFrontEvList.setManaged(true);
    }

    private void rafraichirCartesEvenementsFront() {
        flowEvenementsCards.getChildren().clear();
        List<Evenement> base = new ArrayList<>(evenementsData);
        String q = tfFrontSearchEvenements.getText() == null ? "" : tfFrontSearchEvenements.getText().trim().toLowerCase(Locale.ROOT);
        if (!q.isEmpty()) {
            base.removeIf(e -> !matcheRechercheEvenement(e, q));
        }
        String sort = cbFrontSortEvenements.getSelectionModel().getSelectedItem();
        if (SORT_ANCIEN.equals(sort)) {
            base.sort(Comparator.comparing(Evenement::getDateEvenement, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Evenement::getHeureDebut, Comparator.nullsLast(Comparator.naturalOrder())));
        } else if (SORT_TITRE.equals(sort)) {
            base.sort(Comparator.comparing(Evenement::getTitre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        } else {
            base.sort(Comparator.comparing(Evenement::getDateEvenement, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Evenement::getHeureDebut, Comparator.nullsLast(Comparator.naturalOrder())));
        }
        for (Evenement e : base) {
            flowEvenementsCards.getChildren().add(creerCarteEvenement(e));
        }
        lblFrontEvenementsCount.setText(base.size() + " événement(s)");
        majEtatVideFrontEvenements();
    }

    private static boolean matcheRechercheEvenement(Evenement e, String q) {
        return contient(e.getTitre(), q) || contient(e.getDescription(), q) || contient(e.getLocalisation(), q);
    }

    private static boolean contient(String s, String q) {
        return s != null && s.toLowerCase(Locale.ROOT).contains(q);
    }

    private VBox creerCarteEvenement(Evenement e) {
        VBox card = new VBox();
        card.getStyleClass().add("event-card");
        card.setMaxWidth(300);

        StackPane imgWrap = new StackPane();
        imgWrap.getStyleClass().add("event-card-image-wrap");
        imgWrap.setPrefSize(300, 160);
        Rectangle cardImgClip = new Rectangle();
        cardImgClip.setArcWidth(24);
        cardImgClip.setArcHeight(24);
        cardImgClip.widthProperty().bind(imgWrap.widthProperty());
        cardImgClip.heightProperty().bind(imgWrap.heightProperty());
        imgWrap.setClip(cardImgClip);
        ImageView iv = new ImageView();
        iv.setFitWidth(300);
        iv.setFitHeight(160);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        chargerImageEvenement(iv, e.getImage(), IMG_CARD_W, IMG_CARD_H);
        StackPane.setAlignment(iv, Pos.CENTER);
        imgWrap.getChildren().add(iv);

        VBox body = new VBox(8);
        body.getStyleClass().add("event-card-body");

        String dateStr = e.getDateEvenement() != null ? e.getDateEvenement().format(DATE_FR) : "—";
        String hStr = (e.getHeureDebut() != null && e.getHeureFin() != null)
                ? TIME_FMT.format(e.getHeureDebut()) + " - " + TIME_FMT.format(e.getHeureFin())
                : "";
        Label meta = new Label(dateStr + (hStr.isEmpty() ? "" : "  •  " + hStr));
        meta.getStyleClass().add("event-card-meta");

        Label titre = new Label(nvl(e.getTitre()));
        titre.getStyleClass().add("event-card-title");
        titre.setWrapText(true);

        Label desc = new Label(tronquer(e.getDescription(), 160));
        desc.getStyleClass().add("event-card-desc");
        desc.setWrapText(true);

        Label badge = new Label(e.getProgramme() != null ? "Programme publié" : "Programme à venir");
        badge.getStyleClass().add("badge-status");
        if (e.getProgramme() != null) {
            badge.getStyleClass().add("badge-status-programme");
        }

        HBox actions = new HBox(10);
        actions.getStyleClass().add("event-card-actions");
        Button btnParticiper = new Button("Participer");
        btnParticiper.getStyleClass().addAll("btn-participer");
        btnParticiper.setOnAction(ev -> info("Merci pour votre intérêt !\n(Inscription simplifiée — démo locale.)"));
        Button btnDetails = new Button("Détails");
        btnDetails.getStyleClass().addAll("btn-details");
        btnDetails.setOnAction(ev -> ouvrirDetailEvenementFront(e));
        actions.getChildren().addAll(btnParticiper, btnDetails);

        body.getChildren().addAll(meta, titre, desc, badge, actions);
        card.getChildren().addAll(imgWrap, body);
        return card;
    }

    /**
     * Affiche une image dans un {@link ImageView} : fichiers locaux (chemin ou {@code file:}),
     * ou URL http(s). Les fichiers sont lus en synchrone (fiabilité sous Windows) ;
     * les URL sont chargées en arrière-plan.
     */
    private void chargerImageEvenement(ImageView iv, String raw, double decodeW, double decodeH) {
        iv.setImage(null);
        if (raw == null || raw.isBlank()) {
            return;
        }
        String s = raw.trim();
        try {
            if (s.startsWith("http://") || s.startsWith("https://")) {
                Image img = new Image(s, decodeW, decodeH, true, true, true);
                iv.setImage(img);
                img.errorProperty().addListener((obs, wasErr, isErr) -> {
                    if (Boolean.TRUE.equals(isErr)) {
                        Platform.runLater(() -> {
                            if (iv.getImage() == img) {
                                iv.setImage(null);
                            }
                        });
                    }
                });
                return;
            }

            Path path = null;
            if (s.startsWith("file:")) {
                try {
                    path = Path.of(URI.create(s)).toAbsolutePath().normalize();
                } catch (Exception e) {
                    Image img = new Image(s, decodeW, decodeH, true, true, false);
                    if (!img.isError()) {
                        iv.setImage(img);
                    }
                    return;
                }
            } else {
                path = Path.of(s).toAbsolutePath().normalize();
            }

            if (path == null || !Files.isRegularFile(path)) {
                return;
            }

            try (InputStream in = Files.newInputStream(path)) {
                Image img = new Image(in, decodeW, decodeH, true, true);
                if (!img.isError()) {
                    iv.setImage(img);
                }
            }
        } catch (Exception ignored) {
            // image optionnelle
        }
    }

    private void ouvrirDetailEvenementFront(Evenement e) {
        lblFrontDetailTitre.setText(nvl(e.getTitre()));
        lblFrontDetailDescription.setText(nvl(e.getDescription()));

        imgFrontDetailEvenement.setFitWidth(900);
        imgFrontDetailEvenement.setFitHeight(280);
        StackPane.setAlignment(imgFrontDetailEvenement, Pos.CENTER);

        String rawImg = blankToNull(e.getImage());
        if (rawImg == null) {
            imgFrontDetailEvenement.setImage(null);
            mettreAJourPlaceholderDetailHero(null);
        } else {
            chargerImageEvenement(imgFrontDetailEvenement, rawImg, IMG_DETAIL_W, IMG_DETAIL_H);
            Image im = imgFrontDetailEvenement.getImage();
            if (im != null) {
                im.progressProperty().addListener((o, ov, nv) -> mettreAJourPlaceholderDetailHero(rawImg));
                im.errorProperty().addListener((o, ov, nv) -> mettreAJourPlaceholderDetailHero(rawImg));
            }
            mettreAJourPlaceholderDetailHero(rawImg);
        }

        boxFrontDetailBadges.getChildren().clear();
        if (e.getDateEvenement() != null) {
            Label l = new Label("📅  " + e.getDateEvenement().format(DATE_FR));
            l.getStyleClass().add("badge-pill");
            l.setWrapText(true);
            boxFrontDetailBadges.getChildren().add(l);
        }
        TypeEvenement te = TypeEvenement.fromCode(e.getTypeEvenement());
        if (te != null) {
            Label l = new Label(te.getLibelle());
            l.getStyleClass().addAll("badge-pill", "badge-pill-type");
            l.setWrapText(true);
            boxFrontDetailBadges.getChildren().add(l);
        }
        String lieu = e.getLocalisation();
        if (lieu != null && !lieu.isBlank()) {
            Label l = new Label("📍  " + lieu.trim());
            l.getStyleClass().addAll("badge-pill", "badge-pill-lieu");
            l.setWrapText(true);
            boxFrontDetailBadges.getChildren().add(l);
        }
        if (e.getHeureDebut() != null && e.getHeureFin() != null) {
            Label l = new Label("🕐  " + TIME_FMT.format(e.getHeureDebut()) + " – " + TIME_FMT.format(e.getHeureFin()));
            l.getStyleClass().addAll("badge-pill", "badge-pill-time");
            l.setWrapText(true);
            boxFrontDetailBadges.getChildren().add(l);
        }
        if (e.getNbPlacesDisponibles() != null) {
            Label l = new Label("Places : " + e.getNbPlacesDisponibles());
            l.getStyleClass().addAll("badge-pill", "badge-pill-places");
            l.setWrapText(true);
            boxFrontDetailBadges.getChildren().add(l);
        }

        Programme p = e.getProgramme();
        if (p != null) {
            lblFrontDetailProgrammeInfo.setText("Informations programme disponibles ci-dessous.");
            boxFrontDetailProgramme.setVisible(true);
            boxFrontDetailProgramme.setManaged(true);
            if (lblFrontDetailPause != null) {
                if (p.getPauseDebut() != null && p.getPauseFin() != null) {
                    lblFrontDetailPause.setText("Pause : " + TIME_FMT.format(p.getPauseDebut()) + " – "
                            + TIME_FMT.format(p.getPauseFin()));
                    lblFrontDetailPause.setVisible(true);
                    lblFrontDetailPause.setManaged(true);
                } else {
                    lblFrontDetailPause.setVisible(false);
                    lblFrontDetailPause.setManaged(false);
                }
            }
            lblFrontDetailActivites.setText(nvl(p.getActivites()));
            lblFrontDetailDocs.setText(nvl(p.getDocumentsRequis()));
            lblFrontDetailMateriels.setText(nvl(p.getMaterielsRequis()));
        } else {
            lblFrontDetailProgrammeInfo.setText("Le programme de cet événement sera bientôt disponible.");
            boxFrontDetailProgramme.setVisible(false);
            boxFrontDetailProgramme.setManaged(false);
        }

        boxFrontEvList.setVisible(false);
        boxFrontEvList.setManaged(false);
        boxFrontEvDetail.setVisible(true);
        boxFrontEvDetail.setManaged(true);
        if (spFrontEvDetail != null) {
            spFrontEvDetail.layout();
            Platform.runLater(() -> spFrontEvDetail.setVvalue(0));
        }
    }

    private void mettreAJourPlaceholderDetailHero(String rawImageRef) {
        if (lblFrontDetailImagePlaceholder == null || imgFrontDetailEvenement == null) {
            return;
        }
        if (rawImageRef == null || rawImageRef.isBlank()) {
            lblFrontDetailImagePlaceholder.setText("Pas d'image pour cet événement");
            lblFrontDetailImagePlaceholder.setVisible(true);
            lblFrontDetailImagePlaceholder.setManaged(true);
            return;
        }
        Image im = imgFrontDetailEvenement.getImage();
        if (im != null && !im.isError() && im.getProgress() >= 1.0 && im.getWidth() > 0) {
            lblFrontDetailImagePlaceholder.setVisible(false);
            lblFrontDetailImagePlaceholder.setManaged(false);
            return;
        }
        if (im != null && im.isError()) {
            lblFrontDetailImagePlaceholder.setText("Impossible d'afficher cette image (URL ou fichier invalide).");
        } else {
            lblFrontDetailImagePlaceholder.setText("Chargement de l'image…");
        }
        lblFrontDetailImagePlaceholder.setVisible(true);
        lblFrontDetailImagePlaceholder.setManaged(true);
    }

    private void rafraichirCartesProgrammesFront() {
        flowProgrammesCards.getChildren().clear();
        String q = tfFrontSearchProgrammes.getText() == null ? "" : tfFrontSearchProgrammes.getText().trim().toLowerCase(Locale.ROOT);
        List<Programme> list = new ArrayList<>(programmesData);
        if (!q.isEmpty()) {
            list.removeIf(p -> {
                Evenement ev = p.getEvenement();
                String titre = ev != null ? ev.getTitre() : "";
                return !contient(titre, q) && !contient(p.getActivites(), q);
            });
        }
        for (Programme p : list) {
            flowProgrammesCards.getChildren().add(creerCarteProgramme(p));
        }
        lblFrontProgrammesCount.setText(list.size() + " programme(s)");
        majEtatVideFrontProgrammes();
    }

    private VBox creerCarteProgramme(Programme p) {
        VBox card = new VBox(10);
        card.getStyleClass().add("programme-card-front");
        Evenement ev = p.getEvenement();
        Label t = new Label(ev != null ? nvl(ev.getTitre()) : "Événement");
        t.getStyleClass().add("title");
        String meta = (p.getPauseDebut() != null && p.getPauseFin() != null)
                ? "Pause : " + TIME_FMT.format(p.getPauseDebut()) + " – " + TIME_FMT.format(p.getPauseFin())
                : "";
        Label m = new Label(meta);
        m.getStyleClass().add("meta");
        Label sn = new Label(tronquer(p.getActivites(), 200));
        sn.getStyleClass().add("snippet");
        sn.setWrapText(true);
        card.getChildren().addAll(t, m, sn);
        return card;
    }

    private static String tronquer(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.trim().replaceAll("\\s+", " ");
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    @FXML
    private void onNouveauEvenement() {
        evenementEnEdition = null;
        viderFormulaireEvenement();
        tableEvenements.getSelectionModel().clearSelection();
        nettoyerErreursVisuellesEvenement();
    }

    @FXML
    private void onEnregistrerEvenement() {
        nettoyerErreursVisuellesEvenement();
        try {
            Evenement e = lireFormulaireEvenement();
            EvenementValidator.valider(e);
            evenementService.enregistrer(e);
            onRafraichirEvenements();
            allerVueListeEvenements();
            info("Événement enregistré.");
        } catch (ValidationException ex) {
            marquerErreursVisuellesEvenement(ex.getMessage());
            erreur(ex.getMessage());
        } catch (Exception ex) {
            LOG.error("Enregistrement événement", ex);
            erreur("Erreur : " + ex.getMessage());
        }
    }

    @FXML
    private void onSupprimerEvenement() {
        Evenement sel = tableEvenements.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getId() == null) {
            erreur("Sélectionnez un événement existant à supprimer.");
            return;
        }
        if (!confirmer("Supprimer cet événement et son programme éventuel ?")) {
            return;
        }
        try {
            evenementService.supprimer(sel);
            evenementEnEdition = null;
            viderFormulaireEvenement();
            onRafraichirEvenements();
            onRafraichirProgrammes();
            allerVueListeEvenements();
            info("Événement supprimé.");
        } catch (Exception ex) {
            LOG.error("Suppression événement", ex);
            erreur("Erreur : " + ex.getMessage());
        }
    }

    @FXML
    private void onRafraichirEvenements() {
        Integer keepId = Optional.ofNullable(tableEvenements.getSelectionModel().getSelectedItem())
                .map(Evenement::getId)
                .orElse(null);
        List<Evenement> list = evenementService.listerTous();
        evenementsData.setAll(list);
        if (keepId != null) {
            list.stream()
                    .filter(ev -> keepId.equals(ev.getId()))
                    .findFirst()
                    .ifPresent(ev -> tableEvenements.getSelectionModel().select(ev));
        }
        rafraichirComboEvenementsPourProgramme();
        rafraichirCartesEvenementsFront();
        mettreAJourCompteurEvenementsBack();
    }

    @FXML
    private void onNouveauProgramme() {
        programmeEnEdition = null;
        viderFormulaireProgramme();
        cbPrEvenement.setDisable(false);
        rafraichirComboEvenementsPourProgramme();
        tableProgrammes.getSelectionModel().clearSelection();
        nettoyerErreursVisuellesProgramme();
    }

    @FXML
    private void onEnregistrerProgramme() {
        nettoyerErreursVisuellesProgramme();
        try {
            Programme p = lireFormulaireProgramme();
            ProgrammeValidator.valider(p);
            programmeService.enregistrer(p);
            onRafraichirProgrammes();
            onRafraichirEvenements();
            programmeEnEdition = null;
            viderFormulaireProgramme();
            cbPrEvenement.setDisable(false);
            rafraichirComboEvenementsPourProgramme();
            tableProgrammes.getSelectionModel().clearSelection();
            allerVueListeProgrammes();
            info("Programme enregistré.");
        } catch (ValidationException ex) {
            marquerErreursVisuellesProgramme(ex.getMessage());
            erreur(ex.getMessage());
        } catch (Exception ex) {
            LOG.error("Enregistrement programme", ex);
            erreur("Erreur : " + ex.getMessage());
        }
    }

    @FXML
    private void onSupprimerProgramme() {
        Programme sel = tableProgrammes.getSelectionModel().getSelectedItem();
        if (sel == null || sel.getId() == null) {
            erreur("Sélectionnez un programme existant à supprimer.");
            return;
        }
        if (!confirmer("Supprimer ce programme ?")) {
            return;
        }
        try {
            programmeService.supprimer(sel);
            onNouveauProgramme();
            onRafraichirProgrammes();
            onRafraichirEvenements();
            allerVueListeProgrammes();
            info("Programme supprimé.");
        } catch (Exception ex) {
            LOG.error("Suppression programme", ex);
            erreur("Erreur : " + ex.getMessage());
        }
    }

    @FXML
    private void onRafraichirProgrammes() {
        Integer keepId = Optional.ofNullable(tableProgrammes.getSelectionModel().getSelectedItem())
                .map(Programme::getId)
                .orElse(null);
        List<Programme> list = programmeService.listerTous();
        programmesData.setAll(list);
        if (keepId != null) {
            list.stream()
                    .filter(p -> keepId.equals(p.getId()))
                    .findFirst()
                    .ifPresent(p -> tableProgrammes.getSelectionModel().select(p));
        }
        rafraichirComboEvenementsPourProgramme();
        rafraichirCartesProgrammesFront();
        majEtatVideBackProgrammes();
    }

    private void remplirFormulaireEvenement(Evenement e) {
        nettoyerErreursVisuellesEvenement();
        evenementEnEdition = e;
        tfEvTitre.setText(nvl(e.getTitre()));
        taEvDescription.setText(nvl(e.getDescription()));
        dpEvDate.setValue(e.getDateEvenement());
        appliquerHeureAuxSpinners(e.getHeureDebut(), e.getHeureFin());
        TypeEvenement te = TypeEvenement.fromCode(e.getTypeEvenement());
        if (te != null) {
            cbEvType.getSelectionModel().select(te);
        } else {
            cbEvType.getSelectionModel().clearSelection();
        }
        tfEvImage.setText(nvl(e.getImage()));
        tfEvLocalisation.setText(nvl(e.getLocalisation()));
        tfEvPlaces.setText(e.getNbPlacesDisponibles() != null ? e.getNbPlacesDisponibles().toString() : "");
    }

    private void viderFormulaireEvenement() {
        evenementEnEdition = null;
        nettoyerErreursVisuellesEvenement();
        tfEvTitre.clear();
        taEvDescription.clear();
        dpEvDate.setValue(null);
        appliquerHeureAuxSpinners(null, null);
        cbEvType.getSelectionModel().clearSelection();
        tfEvImage.clear();
        tfEvLocalisation.clear();
        tfEvPlaces.clear();
    }

    private Evenement lireFormulaireEvenement() {
        Evenement e = new Evenement();
        if (evenementEnEdition != null && evenementEnEdition.getId() != null) {
            e.setId(evenementEnEdition.getId());
        }
        e.setTitre(tfEvTitre.getText().trim());
        e.setDescription(taEvDescription.getText().trim());
        e.setDateEvenement(dpEvDate.getValue());
        if (spEvDebutHeure != null) {
            e.setHeureDebut(LocalTime.of(spEvDebutHeure.getValue(), spEvDebutMinute.getValue()));
            e.setHeureFin(LocalTime.of(spEvFinHeure.getValue(), spEvFinMinute.getValue()));
        }
        TypeEvenement sel = cbEvType.getSelectionModel().getSelectedItem();
        e.setTypeEvenement(sel != null ? sel.getCode() : null);
        e.setImage(blankToNull(tfEvImage.getText()));
        e.setLocalisation(blankToNull(tfEvLocalisation.getText()));
        String pl = tfEvPlaces.getText().trim();
        if (pl.isEmpty()) {
            e.setNbPlacesDisponibles(null);
        } else {
            try {
                e.setNbPlacesDisponibles(Integer.parseInt(pl));
            } catch (NumberFormatException ex) {
                throw new ValidationException("Places disponibles : nombre entier invalide.");
            }
        }
        return e;
    }

    private void remplirFormulaireProgramme(Programme p) {
        nettoyerErreursVisuellesProgramme();
        programmeEnEdition = p;
        appliquerPauseAuxSpinnersProgramme(p.getPauseDebut(), p.getPauseFin());
        taPrActivites.setText(nvl(p.getActivites()));
        taPrDocuments.setText(nvl(p.getDocumentsRequis()));
        taPrMateriels.setText(nvl(p.getMaterielsRequis()));

        cbPrEvenement.setDisable(true);
        List<Evenement> tous = evenementService.listerTous();
        cbPrEvenement.setItems(FXCollections.observableArrayList(tous));
        if (p.getEvenement() != null) {
            tous.stream()
                    .filter(ev -> ev.getId() != null && ev.getId().equals(p.getEvenement().getId()))
                    .findFirst()
                    .ifPresent(ev -> cbPrEvenement.getSelectionModel().select(ev));
        }
    }

    private void viderFormulaireProgramme() {
        programmeEnEdition = null;
        nettoyerErreursVisuellesProgramme();
        cbPrEvenement.getSelectionModel().clearSelection();
        appliquerPauseAuxSpinnersProgramme(null, null);
        taPrActivites.clear();
        taPrDocuments.clear();
        taPrMateriels.clear();
    }

    private Programme lireFormulaireProgramme() {
        Programme p = new Programme();
        if (programmeEnEdition != null && programmeEnEdition.getId() != null) {
            p.setId(programmeEnEdition.getId());
        }
        Evenement ev = cbPrEvenement.getSelectionModel().getSelectedItem();
        p.setEvenement(ev);
        if (spPrPauseDebutHeure != null) {
            p.setPauseDebut(LocalTime.of(spPrPauseDebutHeure.getValue(), spPrPauseDebutMinute.getValue()));
            p.setPauseFin(LocalTime.of(spPrPauseFinHeure.getValue(), spPrPauseFinMinute.getValue()));
        }
        p.setActivites(taPrActivites.getText().trim());
        p.setDocumentsRequis(taPrDocuments.getText().trim());
        p.setMaterielsRequis(taPrMateriels.getText().trim());
        return p;
    }

    private void rafraichirComboEvenementsPourProgramme() {
        if (programmeEnEdition != null) {
            return;
        }
        List<Evenement> tous = evenementService.listerTous();
        List<Evenement> sansProgramme = tous.stream()
                .filter(ev -> ev.getProgramme() == null)
                .toList();
        cbPrEvenement.setItems(FXCollections.observableArrayList(sansProgramme));
        cbPrEvenement.setDisable(false);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private static void erreur(String msg) {
        LOG.warn("Erreur UI: {}", msg);
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private static boolean confirmer(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.YES;
    }
}
