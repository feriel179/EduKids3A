package com.edukids.edukids3a.controller;


import com.edukids.enums.Role;
import com.edukids.utils.SessionManager;
import tn.esprit.MainFX;
import com.edukids.edukids3a.model.Evenement;
import com.edukids.edukids3a.model.Programme;
import com.edukids.edukids3a.model.Reservation;
import com.edukids.edukids3a.model.UserEvenementInteraction;
import com.edukids.edukids3a.model.TypeEvenement;
import com.edukids.edukids3a.service.EvenementImageAiService;
import com.edukids.edukids3a.service.EvenementNotificationMailService;
import com.edukids.edukids3a.service.EvenementService;
import com.edukids.edukids3a.service.InteractionService;
import com.edukids.edukids3a.service.ProgrammeService;
import com.edukids.edukids3a.service.ProgrammeActivitesAiService;
import com.edukids.edukids3a.service.ReservationPassPdfService;
import com.edukids.edukids3a.service.ReservationService;
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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.util.Duration;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
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
import javafx.scene.control.Tooltip;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.scene.web.WebView;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.ByteArrayInputStream;

public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    /** Téléchargement d’images HTTP(S) : User-Agent « navigateur » pour APIs (ex. Pollinations) qui refusent le client JavaFX par défaut. */
    private static final HttpClient HTTP_CLIENT_IMAGE = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy").withLocale(Locale.FRENCH);
    private static final DateTimeFormatter MONTH_TITLE_FR = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);
    private static final DateTimeFormatter RESA_DATETIME_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);
    private static final String[] CAL_WEEK_HEADERS = {"lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim."};

    private static final String SORT_RECENT = "Plus récent";
    private static final String SORT_ANCIEN = "Plus ancien";
    private static final String SORT_TITRE = "Titre (A-Z)";

    private YearMonth frontEvenementsCalMonth = YearMonth.now();
    private boolean frontEvVueListe = true;

    /** Nav front sélectionnée avant d'ouvrir la page « Participer » (pour Retour). */
    private Toggle lastFrontNavBeforeParticiper;
    /** Événement ciblé par le formulaire Participer. */
    private Integer partenaireEvenementId;

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

    private static final int PG_BACK_EV_LISTE = 0;
    private static final int PG_BACK_EV_FORM = 1;
    private static final int PG_BACK_EV_DETAIL = 2;
    private static final int PG_BACK_PR_LISTE = 3;
    private static final int PG_BACK_PR_FORM = 4;
    private static final int PG_BACK_STATS = 5;
    private static final int PG_BACK_RES_LISTE = 6;

    private final EvenementService evenementService = new EvenementService();
    private final ProgrammeService programmeService = new ProgrammeService();
    private final InteractionService interactionService = new InteractionService();
    private final ReservationService reservationService = new ReservationService();
    private final ReservationPassPdfService reservationPassPdfService = new ReservationPassPdfService();
    private final EvenementImageAiService evenementImageAiService = new EvenementImageAiService();
    private final ProgrammeActivitesAiService programmeActivitesAiService = new ProgrammeActivitesAiService();
    private final EvenementNotificationMailService evenementNotificationMailService = new EvenementNotificationMailService();

    /** Événement affiché en détail front (référence pour rechargement après actions). */
    private Integer frontDetailEvenementId;

    private final ObservableList<Evenement> evenementsData = FXCollections.observableArrayList();
    private final ObservableList<Programme> programmesData = FXCollections.observableArrayList();
    private final ObservableList<Reservation> reservationsData = FXCollections.observableArrayList();
    private FilteredList<Reservation> reservationsFiltresBack;
    private final ObservableList<Reservation> reservationsFrontData = FXCollections.observableArrayList();
    private FilteredList<Evenement> evenementsFiltresBack;

    @FXML
    private VBox paneFrontOffice;
    @FXML
    private VBox paneBackOffice;
    private final ToggleGroup backNavGroup = new ToggleGroup();
    @FXML
    private ToggleButton toggleBackNavEvListe;
    @FXML
    private ToggleButton toggleBackNavEvForm;
    @FXML
    private ToggleButton toggleBackNavPrListe;
    @FXML
    private ToggleButton toggleBackNavPrForm;
    @FXML
    private ToggleButton toggleBackNavStats;
    @FXML
    private ToggleButton toggleBackNavResListe;
    @FXML
    private VBox paneBackPageEvListe;
    @FXML
    private VBox paneBackPageEvForm;
    @FXML
    private VBox paneBackPageEvDetail;
    @FXML
    private VBox paneBackPagePrListe;
    @FXML
    private VBox paneBackPagePrForm;
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
    @FXML
    private ScrollPane paneBackPageStats;
    @FXML
    private VBox paneBackPageResListe;
    @FXML
    private TableView<Reservation> tableReservations;
    @FXML
    private TableColumn<Reservation, Integer> colResId;
    @FXML
    private TableColumn<Reservation, String> colResEvenement;
    @FXML
    private TableColumn<Reservation, String> colResUserEmail;
    @FXML
    private TableColumn<Reservation, String> colResNom;
    @FXML
    private TableColumn<Reservation, String> colResEmail;
    @FXML
    private TableColumn<Reservation, String> colResTelephone;
    @FXML
    private TableColumn<Reservation, String> colResPlaces;
    @FXML
    private TableColumn<Reservation, String> colResDate;
    @FXML
    private TableColumn<Reservation, Reservation> colResActions;
    @FXML
    private Label lblBackResResultCount;
    @FXML
    private TextField tfBackResSearch;
    @FXML
    private Label lblEmptyBackReservations;
    @FXML
    private Label lblStatsTotalEvenements;
    @FXML
    private Label lblStatsTotalProgrammes;
    @FXML
    private Label lblStatsEvenementsAvecProgramme;
    @FXML
    private Label lblStatsSansProgramme;
    @FXML
    private Label lblStatsCouvertureProgrammes;
    @FXML
    private Label lblStatsTotalLikes;
    @FXML
    private Label lblStatsTotalDislikes;
    @FXML
    private Label lblStatsTotalFavorites;
    @FXML
    private VBox boxStatsCharts;
    @FXML
    private TableView<LigneStatsParType> tableStatsParType;
    @FXML
    private TableColumn<LigneStatsParType, String> colStatsType;
    @FXML
    private TableColumn<LigneStatsParType, Integer> colStatsNombre;
    @FXML
    private TableColumn<LigneStatsParType, String> colStatsPourcent;

    private final ToggleGroup frontNavGroup = new ToggleGroup();
    private final ToggleGroup frontEvViewGroup = new ToggleGroup();
    @FXML
    private ToggleButton toggleFrontNavAccueil;
    @FXML
    private ToggleButton toggleFrontNavEv;
    @FXML
    private ToggleButton toggleFrontNavPr;
    @FXML
    private ToggleButton toggleFrontNavFavoris;
    @FXML
    private ToggleButton toggleFrontNavResa;
    @FXML
    private Label lblFrontNavFavorisBadge;
    @FXML
    private VBox paneFrontPageAccueil;
    @FXML
    private Label lblAccueilNbEvenements;
    @FXML
    private Label lblAccueilNbProgrammes;
    @FXML
    private Label lblAccueilNbAvecProgramme;
    @FXML
    private VBox paneFrontPageEv;
    @FXML
    private VBox paneFrontPagePr;
    @FXML
    private VBox paneFrontPageFavoris;
    @FXML
    private VBox paneFrontPageResa;
    @FXML
    private TableView<Reservation> tableFrontMesReservations;
    @FXML
    private TableColumn<Reservation, String> colFrontResEv;
    @FXML
    private TableColumn<Reservation, String> colFrontResPlaces;
    @FXML
    private TableColumn<Reservation, String> colFrontResDate;
    @FXML
    private TableColumn<Reservation, Reservation> colFrontResPass;
    @FXML
    private Label lblFrontResaVide;
    @FXML
    private VBox paneFrontPageParticiper;
    @FXML
    private Label lblParticiperBreadcrumb;
    @FXML
    private Label lblParticiperEvTitre;
    @FXML
    private Label lblParticiperEvMeta;
    @FXML
    private Label lblParticiperEvLieu;
    @FXML
    private Label lblParticiperEvPlaces;
    @FXML
    private TextField tfParticiperNom;
    @FXML
    private TextField tfParticiperPrenom;
    @FXML
    private TextField tfParticiperEmail;
    @FXML
    private TextField tfParticiperTel;
    @FXML
    private Spinner<Integer> spParticiperAdultes;
    @FXML
    private Spinner<Integer> spParticiperEnfants;
    @FXML
    private Label lblParticiperTotal;
    @FXML
    private FlowPane flowFrontFavorisCards;
    @FXML
    private Label lblFrontFavorisVide;

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
    private VBox boxFrontEvCalendar;
    @FXML
    private GridPane gridFrontEvenementsCalendar;
    @FXML
    private Label lblFrontCalMonthTitle;
    @FXML
    private ToggleButton toggleFrontEvListe;
    @FXML
    private ToggleButton toggleFrontEvCalendrier;
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
    private Label lblFrontDetailPlacesRestantes;
    @FXML
    private Label lblFrontDetailDuree;
    @FXML
    private VBox boxFrontDetailMapWrap;
    @FXML
    private WebView webFrontDetailMap;

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
    private Label totalEventsMetricLabel;
    @FXML
    private Label eventsWithProgramMetricLabel;
    @FXML
    private Label eventReservationsMetricLabel;
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
    private Button btnGenererImageIa;
    @FXML
    private Button btnGenererAlbumIa;
    @FXML
    private VBox boxEvAlbumIaWrap;
    @FXML
    private HBox boxEvAlbumIaThumbs;
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
    private ScrollPane spPrActivitesPreview;
    @FXML
    private TextFlow flowPrActivitesPreview;
    @FXML
    private Button btnPrModeEditionActivites;
    @FXML
    private Button btnPrGenererActivitesIa;
    @FXML
    private TextArea taPrDocuments;
    @FXML
    private TextArea taPrMateriels;

    private Programme programmeEnEdition;
    private Evenement evenementEnEdition;
    private boolean editionActivitesProgramme = false;
    private static final Pattern ACTIVITE_PREVIEW_PATTERN = Pattern.compile(
            "^\\s*[-•]?\\s*(De\\s+\\d{1,2}:\\d{2}\\s+à\\s+\\d{1,2}:\\d{2})\\s*:\\s*([^\\-\\n]+?)\\s*(?:-\\s*(.*))?$",
            Pattern.CASE_INSENSITIVE);

    /**
     * À appeler une fois après {@link javafx.fxml.FXMLLoader#load()} sur MainView.
     * Ne pas utiliser le hook {@code initialize()} : avec le même contrôleur sur plusieurs {@code fx:include},
     * JavaFX l’exécuterait trop tôt (champs d’autres pages encore null).
     */
    public void initialiserApresChargementFxml() {
        toggleBackNavEvListe.setToggleGroup(backNavGroup);
        toggleBackNavEvForm.setToggleGroup(backNavGroup);
        toggleBackNavPrListe.setToggleGroup(backNavGroup);
        toggleBackNavPrForm.setToggleGroup(backNavGroup);
        toggleBackNavStats.setToggleGroup(backNavGroup);
        if (toggleBackNavResListe != null) {
            toggleBackNavResListe.setToggleGroup(backNavGroup);
        }
        toggleFrontNavAccueil.setToggleGroup(frontNavGroup);
        toggleFrontNavEv.setToggleGroup(frontNavGroup);
        toggleFrontNavPr.setToggleGroup(frontNavGroup);
        if (toggleFrontNavFavoris != null) {
            toggleFrontNavFavoris.setToggleGroup(frontNavGroup);
        }
        if (toggleFrontNavResa != null) {
            toggleFrontNavResa.setToggleGroup(frontNavGroup);
        }

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
        if (taPrActivites != null) {
            taPrActivites.textProperty().addListener((obs, oldV, newV) -> rafraichirApercuActivitesProgramme(newV));
            rafraichirApercuActivitesProgramme(taPrActivites.getText());
        }
        appliquerModeEditionActivitesProgramme(false);

        if (toggleFrontEvListe != null && toggleFrontEvCalendrier != null) {
            toggleFrontEvListe.setToggleGroup(frontEvViewGroup);
            toggleFrontEvCalendrier.setToggleGroup(frontEvViewGroup);
            frontEvViewGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
                if (newT == null) {
                    Platform.runLater(() -> {
                        if (frontEvViewGroup.getSelectedToggle() == null) {
                            toggleFrontEvListe.setSelected(true);
                        }
                    });
                    return;
                }
                frontEvVueListe = newT == toggleFrontEvListe;
                if (boxFrontEvList != null) {
                    boxFrontEvList.setVisible(frontEvVueListe);
                    boxFrontEvList.setManaged(frontEvVueListe);
                }
                if (boxFrontEvCalendar != null) {
                    boolean cal = !frontEvVueListe;
                    boxFrontEvCalendar.setVisible(cal);
                    boxFrontEvCalendar.setManaged(cal);
                    if (cal) {
                        rafraichirCalendrierEvenementsFront();
                    }
                }
            });
        }
        if (gridFrontEvenementsCalendar != null) {
            gridFrontEvenementsCalendar.getColumnConstraints().clear();
            for (int i = 0; i < 7; i++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(100.0 / 7.0);
                cc.setHgrow(Priority.ALWAYS);
                cc.setMinWidth(72);
                gridFrontEvenementsCalendar.getColumnConstraints().add(cc);
            }
        }

        evenementsFiltresBack = new FilteredList<>(evenementsData, e -> true);
        tableEvenements.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tableEvenements.setItems(evenementsFiltresBack);
        configurerTableEvenementsBackOffice();
        reservationsFiltresBack = new FilteredList<>(reservationsData, r -> true);
        configurerTableReservationsBack();
        configurerTableFrontMesReservations();
        installerPageParticiper();
        if (tfBackResSearch != null) {
            tfBackResSearch.textProperty().addListener((o, ov, nv) -> {
                String q = nv == null ? "" : nv.trim().toLowerCase(Locale.ROOT);
                reservationsFiltresBack.setPredicate(r -> {
                    if (q.isEmpty()) {
                        return true;
                    }
                    return matcheRechercheReservation(r, q);
                });
                mettreAJourCompteurReservationsBack();
            });
        }
        mettreAJourCompteurReservationsBack();
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

        appliquerMode(ouvrirFrontOfficeParDefaut());

        backNavGroup.selectedToggleProperty().addListener((obs, o, n) -> syncPageBackOffice(n));
        frontNavGroup.selectedToggleProperty().addListener((obs, o, n) -> syncPageFrontOffice(n));

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

        if (tableStatsParType != null) {
            tableStatsParType.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
            colStatsType.setCellValueFactory(new PropertyValueFactory<>("libelleType"));
            colStatsNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
            if (colStatsPourcent != null) {
                colStatsPourcent.setCellValueFactory(new PropertyValueFactory<>("pourcentage"));
            }
        }

        onRafraichirEvenements();
        onRafraichirProgrammes();

        syncPageBackOffice(backNavGroup.getSelectedToggle());
        syncPageFrontOffice(frontNavGroup.getSelectedToggle());

        Platform.runLater(this::installerRaccourcisScene);
        majBadgeFavoris();
    }

    private void appliquerMode(boolean front) {
        paneFrontOffice.setVisible(front);
        paneFrontOffice.setManaged(front);
        paneBackOffice.setVisible(!front);
        paneBackOffice.setManaged(!front);
        if (front) {
            if (toggleFrontNavAccueil != null) {
                toggleFrontNavAccueil.setSelected(true);
            }
            rafraichirCartesEvenementsFront();
            rafraichirCartesProgrammesFront();
            majBadgeFavoris();
        } else {
            if (toggleBackNavEvListe != null) {
                toggleBackNavEvListe.setSelected(true);
            }
        }
    }

    public void openStudentMode() {
        appliquerMode(true);
    }

    public void openAdminMode() {
        appliquerMode(false);
    }

    public void showBackEventList() {
        selectBackOfficeToggle(toggleBackNavEvListe);
    }

    public void showBackEventForm() {
        selectBackOfficeToggle(toggleBackNavEvForm);
        onNouveauEvenement();
    }

    public void showBackProgramList() {
        selectBackOfficeToggle(toggleBackNavPrListe);
    }

    public void showBackProgramForm() {
        selectBackOfficeToggle(toggleBackNavPrForm);
        onNouveauProgramme();
    }

    public void showBackStats() {
        selectBackOfficeToggle(toggleBackNavStats);
    }

    public void showBackReservations() {
        selectBackOfficeToggle(toggleBackNavResListe);
    }

    private void selectBackOfficeToggle(ToggleButton toggle) {
        openAdminMode();
        if (toggle != null) {
            toggle.setSelected(true);
            syncPageBackOffice(toggle);
        }
    }

    public void selectChatMode() {
        openAdminMode();
    }

    private boolean ouvrirFrontOfficeParDefaut() {
        var u = SessionManager.getCurrentUser();
        if (u == null || u.getRoles() == null || u.getRoles().isEmpty()) {
            return true;
        }
        return !u.getRoles().contains(Role.ROLE_ADMIN);
    }

    private void montrerSeulementPageBack(int index) {
        List<Node> pages = List.of(
                paneBackPageEvListe,
                paneBackPageEvForm,
                paneBackPageEvDetail,
                paneBackPagePrListe,
                paneBackPagePrForm,
                paneBackPageStats,
                paneBackPageResListe);
        for (int i = 0; i < pages.size(); i++) {
            Node n = pages.get(i);
            if (n == null) {
                continue;
            }
            boolean show = (i == index);
            n.setVisible(show);
            n.setManaged(show);
        }
        if (index == PG_BACK_STATS) {
            rafraichirPageStatistiques();
        }
        if (index == PG_BACK_RES_LISTE) {
            onRafraichirReservationsBack();
        }
    }

    private void syncPageBackOffice(Toggle t) {
        if (paneBackPageEvListe == null) {
            return;
        }
        if (t == null) {
            return;
        }
        if (t == toggleBackNavEvListe) {
            montrerSeulementPageBack(PG_BACK_EV_LISTE);
        } else if (t == toggleBackNavEvForm) {
            montrerSeulementPageBack(PG_BACK_EV_FORM);
        } else if (t == toggleBackNavPrListe) {
            montrerSeulementPageBack(PG_BACK_PR_LISTE);
        } else if (t == toggleBackNavPrForm) {
            montrerSeulementPageBack(PG_BACK_PR_FORM);
        } else if (t == toggleBackNavStats) {
            montrerSeulementPageBack(PG_BACK_STATS);
        } else if (toggleBackNavResListe != null && t == toggleBackNavResListe) {
            montrerSeulementPageBack(PG_BACK_RES_LISTE);
        }
    }

    private void syncPageFrontOffice(Toggle t) {
        if (paneFrontPageAccueil == null || t == null) {
            return;
        }
        if (paneFrontPageParticiper != null) {
            paneFrontPageParticiper.setVisible(false);
            paneFrontPageParticiper.setManaged(false);
        }
        boolean acc = t == toggleFrontNavAccueil;
        boolean ev = t == toggleFrontNavEv;
        boolean pr = t == toggleFrontNavPr;
        boolean fav = toggleFrontNavFavoris != null && t == toggleFrontNavFavoris;
        boolean resa = toggleFrontNavResa != null && t == toggleFrontNavResa;
        paneFrontPageAccueil.setVisible(acc);
        paneFrontPageAccueil.setManaged(acc);
        paneFrontPageEv.setVisible(ev);
        paneFrontPageEv.setManaged(ev);
        paneFrontPagePr.setVisible(pr);
        paneFrontPagePr.setManaged(pr);
        if (paneFrontPageFavoris != null) {
            paneFrontPageFavoris.setVisible(fav);
            paneFrontPageFavoris.setManaged(fav);
        }
        if (paneFrontPageResa != null) {
            paneFrontPageResa.setVisible(resa);
            paneFrontPageResa.setManaged(resa);
        }
        if (ev) {
            onFrontRetourListeEvenements();
            rafraichirCartesEvenementsFront();
        }
        if (pr) {
            rafraichirCartesProgrammesFront();
        }
        if (fav) {
            rafraichirFavorisFront();
        }
        if (resa) {
            rafraichirMesReservationsFront();
        }
        if (acc) {
            majInfosAccueil();
        }
        majBadgeFavoris();
    }

    private void rafraichirPageStatistiques() {
        if (lblStatsTotalEvenements == null || tableStatsParType == null) {
            return;
        }
        int nEv = evenementsData.size();
        int nPr = programmesData.size();
        long avecPr = evenementsData.stream().filter(e -> e.getProgramme() != null).count();
        lblStatsTotalEvenements.setText(String.valueOf(nEv));
        lblStatsTotalProgrammes.setText(String.valueOf(nPr));
        lblStatsEvenementsAvecProgramme.setText(String.valueOf(avecPr));
        long sansPr = nEv - avecPr;
        if (lblStatsSansProgramme != null) {
            lblStatsSansProgramme.setText(String.valueOf(sansPr));
        }
        if (lblStatsCouvertureProgrammes != null) {
            if (nEv > 0) {
                lblStatsCouvertureProgrammes.setText(String.format(Locale.FRENCH, "%.1f %%", 100.0 * avecPr / nEv));
            } else {
                lblStatsCouvertureProgrammes.setText("—");
            }
        }

        int[] counts = new int[TypeEvenement.values().length];
        int sansType = 0;
        for (Evenement e : evenementsData) {
            TypeEvenement te = TypeEvenement.fromCode(e.getTypeEvenement());
            if (te != null) {
                counts[te.ordinal()]++;
            } else {
                sansType++;
            }
        }
        ObservableList<LigneStatsParType> lignes = FXCollections.observableArrayList();
        for (TypeEvenement te : TypeEvenement.values()) {
            int c = counts[te.ordinal()];
            lignes.add(new LigneStatsParType(te.getLibelle(), c, nEv));
        }
        if (sansType > 0) {
            lignes.add(new LigneStatsParType("Non renseigné / autre", sansType, nEv));
        }
        lignes.sort(Comparator.comparing(LigneStatsParType::getLibelleType, String.CASE_INSENSITIVE_ORDER));
        tableStatsParType.setItems(lignes);

        EvenementService.StatsInteractions st = evenementService.getStatsInteractions();
        if (lblStatsTotalLikes != null) {
            lblStatsTotalLikes.setText(String.valueOf(st.totalLikes()));
        }
        if (lblStatsTotalDislikes != null) {
            lblStatsTotalDislikes.setText(String.valueOf(st.totalDislikes()));
        }
        if (lblStatsTotalFavorites != null) {
            lblStatsTotalFavorites.setText(String.valueOf(st.totalFavorites()));
        }
        if (boxStatsCharts != null) {
            boxStatsCharts.getChildren().clear();
            boxStatsCharts.getChildren().add(creerBarChartTop("Top 10 — likes", evenementService.topParLikes(10), StatChartMode.LIKES));
            boxStatsCharts.getChildren().add(creerBarChartTop("Top 10 — dislikes", evenementService.topParDislikes(10), StatChartMode.DISLIKES));
            boxStatsCharts.getChildren().add(creerBarChartTop("Top 10 — favoris", evenementService.topParFavoris(10), StatChartMode.FAVORITES));
        }
    }

    private enum StatChartMode {
        LIKES, DISLIKES, FAVORITES
    }

    private VBox creerBarChartTop(String titre, List<Evenement> evenements, StatChartMode mode) {
        Label lt = new Label(titre);
        lt.getStyleClass().add("form-section-title");
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setLegendVisible(false);
        chart.setPrefHeight(260);
        chart.setMaxWidth(Double.MAX_VALUE);
        chart.getStyleClass().add("chart-stats-dark");
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (Evenement ev : evenements) {
            String cat = tronquer(nvl(ev.getTitre()), 22);
            int v = switch (mode) {
                case LIKES -> ev.getLikesCount();
                case DISLIKES -> ev.getDislikesCount();
                case FAVORITES -> ev.getFavoritesCount();
            };
            series.getData().add(new XYChart.Data<>(cat, v));
        }
        chart.getData().add(series);
        VBox box = new VBox(8, lt, chart);
        return box;
    }

    private void majInfosAccueil() {
        if (lblAccueilNbEvenements == null) {
            return;
        }
        int nEv = evenementsData.size();
        int nPr = programmesData.size();
        long avecPr = evenementsData.stream().filter(e -> e.getProgramme() != null).count();
        lblAccueilNbEvenements.setText(String.valueOf(nEv));
        lblAccueilNbProgrammes.setText(String.valueOf(nPr));
        if (lblAccueilNbAvecProgramme != null) {
            lblAccueilNbAvecProgramme.setText(String.valueOf(avecPr));
        }
    }

    @FXML
    private void onAccueilVersEvenements() {
        if (toggleFrontNavEv != null) {
            toggleFrontNavEv.setSelected(true);
        }
    }

    @FXML
    private void onAccueilVersProgrammes() {
        if (toggleFrontNavPr != null) {
            toggleFrontNavPr.setSelected(true);
        }
    }

    @FXML
    private void onDeconnexion() {
        Node n = paneFrontOffice != null ? paneFrontOffice : paneBackOffice;
        Window w = n != null && n.getScene() != null ? n.getScene().getWindow() : null;
        if (w instanceof Stage st) {
            try {
                MainFX.getInstance().showLoginView();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private void majBadgeFavoris() {
        if (lblFrontNavFavorisBadge == null) {
            return;
        }
        var u = SessionManager.getCurrentUser();
        if (u == null) {
            lblFrontNavFavorisBadge.setText("");
            lblFrontNavFavorisBadge.setVisible(false);
            lblFrontNavFavorisBadge.setManaged(false);
            return;
        }
        long n = interactionService.compterFavorisUtilisateur(u.getId());
        if (n > 0) {
            lblFrontNavFavorisBadge.setText(String.valueOf(n));
            lblFrontNavFavorisBadge.setVisible(true);
            lblFrontNavFavorisBadge.setManaged(true);
        } else {
            lblFrontNavFavorisBadge.setText("");
            lblFrontNavFavorisBadge.setVisible(false);
            lblFrontNavFavorisBadge.setManaged(false);
        }
    }

    private void rafraichirFavorisFront() {
        if (flowFrontFavorisCards == null) {
            return;
        }
        flowFrontFavorisCards.getChildren().clear();
        var u = SessionManager.getCurrentUser();
        if (u == null) {
            if (lblFrontFavorisVide != null) {
                lblFrontFavorisVide.setText("Connectez-vous pour voir vos événements favoris.");
                lblFrontFavorisVide.setVisible(true);
                lblFrontFavorisVide.setManaged(true);
            }
            return;
        }
        List<Evenement> favs = evenementService.listerEvenementsFavorisPourUtilisateur(u.getId(), 200);
        if (lblFrontFavorisVide != null) {
            boolean empty = favs.isEmpty();
            lblFrontFavorisVide.setText("Aucun favori pour le moment. Utilisez le bouton cœur sur les cartes « Événements ».");
            lblFrontFavorisVide.setVisible(empty);
            lblFrontFavorisVide.setManaged(empty);
        }
        for (Evenement ev : favs) {
            flowFrontFavorisCards.getChildren().add(creerCarteEvenement(ev));
        }
    }

    private boolean estUtilisateurAdmin() {
        var u = SessionManager.getCurrentUser();
        return u != null && u.getRoles() != null && u.getRoles().contains(Role.ROLE_ADMIN);
    }

    private void toggleInteractionCarte(Integer evenementId, String type) {
        var u = SessionManager.getCurrentUser();
        if (u == null || evenementId == null) {
            return;
        }
        try {
            if (UserEvenementInteraction.TYPE_LIKE.equals(type)) {
                interactionService.toggleLike(u.getId(), evenementId);
            } else if (UserEvenementInteraction.TYPE_DISLIKE.equals(type)) {
                interactionService.toggleDislike(u.getId(), evenementId);
            } else if (UserEvenementInteraction.TYPE_FAVORITE.equals(type)) {
                interactionService.toggleFavorite(u.getId(), evenementId);
            }
            onRafraichirEvenements();
            if (boxFrontEvDetail != null && boxFrontEvDetail.isVisible() && evenementId.equals(frontDetailEvenementId)) {
                Evenement fresh = evenementService.trouverParId(evenementId);
                if (fresh != null) {
                    ouvrirDetailEvenementFront(fresh);
                }
            }
            if (toggleFrontNavFavoris != null && toggleFrontNavFavoris.isSelected()) {
                rafraichirFavorisFront();
            }
            if (toggleFrontNavResa != null && toggleFrontNavResa.isSelected()) {
                rafraichirMesReservationsFront();
            }
            majBadgeFavoris();
        } catch (Exception ex) {
            LOG.warn("Interaction événement", ex);
            erreur(ex.getMessage() != null ? ex.getMessage() : "Erreur lors de l'enregistrement de la réaction.");
        }
    }

    private HBox creerBarreReactionsEvenement(Evenement e) {
        HBox row = new HBox(8);
        row.getStyleClass().add("event-card-reactions");
        row.setAlignment(Pos.CENTER_LEFT);
        if (e.getId() == null) {
            return row;
        }
        var u = SessionManager.getCurrentUser();
        InteractionService.EtatInteractions et;
        if (u != null) {
            et = interactionService.getEtat(u.getId(), e.getId());
        } else {
            et = new InteractionService.EtatInteractions(false, false, false,
                    e.getLikesCount(), e.getDislikesCount(), e.getFavoritesCount());
        }

        Button btnLike = new Button("👍 " + et.likes());
        btnLike.getStyleClass().addAll("event-react-btn", "event-react-like");
        if (et.liked()) {
            btnLike.getStyleClass().add("event-react-like-active");
        }
        btnLike.setDisable(u == null);
        btnLike.setOnAction(ev -> toggleInteractionCarte(e.getId(), UserEvenementInteraction.TYPE_LIKE));

        Button btnDis = new Button("👎 " + et.dislikes());
        btnDis.getStyleClass().addAll("event-react-btn", "event-react-dislike");
        if (et.disliked()) {
            btnDis.getStyleClass().add("event-react-dislike-active");
        }
        btnDis.setDisable(u == null);
        btnDis.setOnAction(ev -> toggleInteractionCarte(e.getId(), UserEvenementInteraction.TYPE_DISLIKE));

        String favLabel = et.favorited() ? "♥ " + et.favorites() : "♡ " + et.favorites();
        Button btnFav = new Button(favLabel);
        btnFav.getStyleClass().addAll("event-react-btn", "event-react-fav");
        if (et.favorited()) {
            btnFav.getStyleClass().add("event-react-fav-active");
        }
        btnFav.setDisable(u == null);
        btnFav.setOnAction(ev -> toggleInteractionCarte(e.getId(), UserEvenementInteraction.TYPE_FAVORITE));

        if (u == null) {
            Label hint = new Label("(connectez-vous)");
            hint.getStyleClass().add("event-react-hint");
            row.getChildren().addAll(btnLike, btnDis, btnFav, hint);
        } else {
            row.getChildren().addAll(btnLike, btnDis, btnFav);
        }
        return row;
    }

    private void installerPageParticiper() {
        if (spParticiperAdultes == null || spParticiperEnfants == null) {
            return;
        }
        spParticiperAdultes.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, 1));
        spParticiperEnfants.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 50, 0));
        spParticiperAdultes.setEditable(true);
        spParticiperEnfants.setEditable(true);
        spParticiperAdultes.valueProperty().addListener((o, a, b) -> majTotalParticiper());
        spParticiperEnfants.valueProperty().addListener((o, a, b) -> majTotalParticiper());
    }

    private void majTotalParticiper() {
        if (lblParticiperTotal == null || spParticiperAdultes == null || spParticiperEnfants == null) {
            return;
        }
        lblParticiperTotal.setText("Total de places : " + (spParticiperAdultes.getValue() + spParticiperEnfants.getValue()));
    }

    private void mettreAJourFilArianeParticiper() {
        if (lblParticiperBreadcrumb == null) {
            return;
        }
        Toggle t = lastFrontNavBeforeParticiper;
        if (t == toggleFrontNavFavoris) {
            lblParticiperBreadcrumb.setText("Mes favoris • Réservation");
        } else if (toggleFrontNavResa != null && t == toggleFrontNavResa) {
            lblParticiperBreadcrumb.setText("Mes réservations • Réservation");
        } else if (t == toggleFrontNavPr) {
            lblParticiperBreadcrumb.setText("Programmes • Réservation");
        } else if (t == toggleFrontNavAccueil) {
            lblParticiperBreadcrumb.setText("Accueil • Réservation");
        } else {
            lblParticiperBreadcrumb.setText("Événements • Réservation");
        }
    }

    private void remplirPageParticiper(Evenement e) {
        if (lblParticiperEvTitre == null || e == null || e.getId() == null) {
            return;
        }
        Evenement src = evenementService.trouverParId(e.getId());
        if (src == null) {
            return;
        }
        var u = SessionManager.getCurrentUser();
        lblParticiperEvTitre.setText(nvl(src.getTitre()));
        String dateStr = src.getDateEvenement() != null ? src.getDateEvenement().format(DATE_FR) : "—";
        String hStr = (src.getHeureDebut() != null && src.getHeureFin() != null)
                ? TIME_FMT.format(src.getHeureDebut()) + " – " + TIME_FMT.format(src.getHeureFin())
                : "—";
        lblParticiperEvMeta.setText("📅  " + dateStr + "     🕐  " + hStr);

        String lieu = src.getLocalisation();
        if (lieu != null && !lieu.isBlank()) {
            lblParticiperEvLieu.setText("📍  " + lieu.trim());
            lblParticiperEvLieu.setVisible(true);
            lblParticiperEvLieu.setManaged(true);
        } else {
            lblParticiperEvLieu.setVisible(false);
            lblParticiperEvLieu.setManaged(false);
        }

        int reserves = evenementService.sommePlacesReserveesPourEvenement(src.getId());
        Integer cap = src.getNbPlacesDisponibles();
        if (cap != null) {
            int rest = Math.max(0, cap - reserves);
            lblParticiperEvPlaces.setText("👥  Places disponibles : " + rest + " / " + cap);
            lblParticiperEvPlaces.setVisible(true);
            lblParticiperEvPlaces.setManaged(true);
        } else {
            lblParticiperEvPlaces.setVisible(false);
            lblParticiperEvPlaces.setManaged(false);
        }

        if (u != null) {
            tfParticiperNom.setText(nvl(u.getLastName()));
            tfParticiperPrenom.setText(nvl(u.getFirstName()));
            tfParticiperEmail.setText(nvl(u.getEmail()));
        }
        tfParticiperTel.clear();
        if (spParticiperAdultes.getValueFactory() != null) {
            spParticiperAdultes.getValueFactory().setValue(1);
        }
        if (spParticiperEnfants.getValueFactory() != null) {
            spParticiperEnfants.getValueFactory().setValue(0);
        }
        majTotalParticiper();
        mettreAJourFilArianeParticiper();
    }

    private void afficherSeulementPageParticiper() {
        paneFrontPageAccueil.setVisible(false);
        paneFrontPageAccueil.setManaged(false);
        paneFrontPageEv.setVisible(false);
        paneFrontPageEv.setManaged(false);
        paneFrontPagePr.setVisible(false);
        paneFrontPagePr.setManaged(false);
        if (paneFrontPageFavoris != null) {
            paneFrontPageFavoris.setVisible(false);
            paneFrontPageFavoris.setManaged(false);
        }
        if (paneFrontPageResa != null) {
            paneFrontPageResa.setVisible(false);
            paneFrontPageResa.setManaged(false);
        }
        paneFrontPageParticiper.setVisible(true);
        paneFrontPageParticiper.setManaged(true);
    }

    private void ouvrirPageParticiper(Evenement e) {
        var u = SessionManager.getCurrentUser();
        if (u == null) {
            erreur("Connectez-vous pour réserver une place.");
            return;
        }
        if (e.getId() == null) {
            return;
        }
        int reserves = evenementService.sommePlacesReserveesPourEvenement(e.getId());
        Integer cap = e.getNbPlacesDisponibles();
        if (cap != null && cap - reserves <= 0) {
            erreur("Il n'y a plus de places disponibles pour cet événement.");
            return;
        }
        lastFrontNavBeforeParticiper = frontNavGroup.getSelectedToggle();
        partenaireEvenementId = e.getId();
        remplirPageParticiper(e);
        afficherSeulementPageParticiper();
    }

    private void fermerPageParticiperEtRestaurerNavigation() {
        if (paneFrontPageParticiper != null) {
            paneFrontPageParticiper.setVisible(false);
            paneFrontPageParticiper.setManaged(false);
        }
        Toggle t = lastFrontNavBeforeParticiper != null ? lastFrontNavBeforeParticiper : toggleFrontNavEv;
        if (t != null) {
            t.setSelected(true);
        }
        syncPageFrontOffice(frontNavGroup.getSelectedToggle());
    }

    @FXML
    private void onFrontParticiperRetour() {
        fermerPageParticiperEtRestaurerNavigation();
    }

    @FXML
    private void onFrontParticiperConfirmer() {
        var u = SessionManager.getCurrentUser();
        if (u == null || partenaireEvenementId == null) {
            return;
        }
        try {
            reservationService.creerReservation(
                    u.getId(),
                    partenaireEvenementId,
                    tfParticiperNom.getText(),
                    tfParticiperPrenom.getText(),
                    tfParticiperEmail.getText(),
                    tfParticiperTel.getText(),
                    spParticiperAdultes.getValue(),
                    spParticiperEnfants.getValue());
            onRafraichirEvenements();
            if (toggleFrontNavResa != null && toggleFrontNavResa.isSelected()) {
                rafraichirMesReservationsFront();
            }
            info("Votre réservation a été enregistrée.");
            fermerPageParticiperEtRestaurerNavigation();
        } catch (IllegalArgumentException ex) {
            erreur(ex.getMessage());
        } catch (Exception ex) {
            LOG.error("Réservation", ex);
            erreur("Erreur : " + (ex.getMessage() != null ? ex.getMessage() : "inconnue"));
        }
    }

    @FXML
    private void onRafraichirReservationsBack() {
        if (tableReservations == null) {
            return;
        }
        reservationsData.setAll(reservationService.listerToutes());
        mettreAJourMetriquesEvenementsBack();
        mettreAJourCompteurReservationsBack();
    }

    private void mettreAJourCompteurReservationsBack() {
        if (lblBackResResultCount == null || reservationsFiltresBack == null) {
            return;
        }
        lblBackResResultCount.setText(reservationsFiltresBack.size() + " résultat(s)");
        majEtatVideBackReservations();
    }

    @FXML
    private void onBackResResetFiltre() {
        if (tfBackResSearch != null) {
            tfBackResSearch.clear();
        }
        if (reservationsFiltresBack != null) {
            reservationsFiltresBack.setPredicate(r -> true);
        }
        mettreAJourCompteurReservationsBack();
    }

    private static boolean matcheRechercheReservation(Reservation r, String q) {
        Evenement ev = r.getEvenement();
        String titre = ev != null ? ev.getTitre() : "";
        var usr = r.getUtilisateur();
        String uem = usr != null ? usr.getEmail() : "";
        return contient(titre, q)
                || contient(uem, q)
                || contient(r.getNom(), q)
                || contient(r.getPrenom(), q)
                || contient(r.getEmail(), q)
                || contient(r.getTelephone(), q);
    }

    private void majEtatVideBackReservations() {
        if (lblEmptyBackReservations == null || reservationsFiltresBack == null) {
            return;
        }
        boolean vide = reservationsFiltresBack.isEmpty();
        lblEmptyBackReservations.setManaged(vide);
        lblEmptyBackReservations.setVisible(vide);
        if (vide) {
            lblEmptyBackReservations.setText(reservationsData.isEmpty()
                    ? "Aucune réservation en base."
                    : "Aucune réservation ne correspond au filtre actuel. Modifiez la recherche ou cliquez sur « Réinitialiser ».");
        }
    }

    private void rafraichirMesReservationsFront() {
        if (tableFrontMesReservations == null) {
            return;
        }
        var u = SessionManager.getCurrentUser();
        if (u == null) {
            reservationsFrontData.clear();
            if (lblFrontResaVide != null) {
                lblFrontResaVide.setText("Connectez-vous pour voir vos réservations.");
                lblFrontResaVide.setVisible(true);
                lblFrontResaVide.setManaged(true);
            }
            return;
        }
        reservationsFrontData.setAll(reservationService.listerPourUtilisateur(u.getId()));
        boolean empty = reservationsFrontData.isEmpty();
        if (lblFrontResaVide != null) {
            lblFrontResaVide.setText("Vous n'avez encore aucune réservation. Participez depuis « Événements ».");
            lblFrontResaVide.setVisible(empty);
            lblFrontResaVide.setManaged(empty);
        }
    }

    private void configurerTableReservationsBack() {
        if (tableReservations == null || colResId == null) {
            return;
        }
        tableReservations.setItems(reservationsFiltresBack);
        tableReservations.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colResId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colResEvenement.setCellValueFactory(cd -> {
            Evenement ev = cd.getValue().getEvenement();
            return new SimpleStringProperty(ev != null ? nvl(ev.getTitre()) : "—");
        });
        colResUserEmail.setCellValueFactory(cd -> {
            var usr = cd.getValue().getUtilisateur();
            return new SimpleStringProperty(usr != null ? nvl(usr.getEmail()) : "—");
        });
        colResNom.setCellValueFactory(cd -> new SimpleStringProperty(
                nvl(cd.getValue().getPrenom()) + " " + nvl(cd.getValue().getNom())));
        colResEmail.setCellValueFactory(cd -> new SimpleStringProperty(nvl(cd.getValue().getEmail())));
        colResTelephone.setCellValueFactory(cd -> {
            String t = cd.getValue().getTelephone();
            return new SimpleStringProperty(t == null || t.isBlank() ? "—" : t.trim());
        });
        colResPlaces.setCellValueFactory(cd -> {
            Reservation x = cd.getValue();
            return new SimpleStringProperty((x.getNbAdultes() + x.getNbEnfants()) + " (A" + x.getNbAdultes() + " + E" + x.getNbEnfants() + ")");
        });
        colResDate.setCellValueFactory(cd -> {
            var dt = cd.getValue().getDateReservation();
            return new SimpleStringProperty(dt != null ? dt.format(RESA_DATETIME_FR) : "—");
        });
        colResActions.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
        colResActions.setCellFactory(c -> new TableCell<Reservation, Reservation>() {
            @Override
            protected void updateItem(Reservation res, boolean empty) {
                super.updateItem(res, empty);
                if (empty || res == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Button pdf = new Button("PDF");
                pdf.getStyleClass().add("back-table-action-link");
                pdf.setOnAction(ae -> exporterPassPdfReservation(res));
                if (!estUtilisateurAdmin()) {
                    setGraphic(pdf);
                    setText(null);
                    return;
                }
                Button del = new Button("Supprimer");
                del.getStyleClass().addAll("back-table-action-link", "back-table-action-del");
                del.setOnAction(ae -> supprimerReservationBack(res));
                HBox row = new HBox(8, pdf, del);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
    }

    private void supprimerReservationBack(Reservation res) {
        if (res == null || res.getId() == null) {
            return;
        }
        if (!estUtilisateurAdmin()) {
            erreur("Seuls les administrateurs peuvent supprimer une réservation.");
            return;
        }
        if (!confirmer("Supprimer cette réservation ?")) {
            return;
        }
        try {
            reservationService.supprimer(res.getId());
            onRafraichirReservationsBack();
            onRafraichirEvenements();
            info("Réservation supprimée.");
        } catch (Exception ex) {
            LOG.error("Suppression réservation", ex);
            erreur("Erreur : " + (ex.getMessage() != null ? ex.getMessage() : "inconnue"));
        }
    }

    private void configurerTableFrontMesReservations() {
        if (tableFrontMesReservations == null || colFrontResEv == null) {
            return;
        }
        tableFrontMesReservations.setItems(reservationsFrontData);
        tableFrontMesReservations.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colFrontResEv.setCellValueFactory(cd -> {
            Evenement ev = cd.getValue().getEvenement();
            return new SimpleStringProperty(ev != null ? nvl(ev.getTitre()) : "—");
        });
        colFrontResPlaces.setCellValueFactory(cd -> {
            Reservation x = cd.getValue();
            return new SimpleStringProperty((x.getNbAdultes() + x.getNbEnfants()) + " (adultes " + x.getNbAdultes() + ", enfants " + x.getNbEnfants() + ")");
        });
        colFrontResDate.setCellValueFactory(cd -> {
            var dt = cd.getValue().getDateReservation();
            return new SimpleStringProperty(dt != null ? dt.format(RESA_DATETIME_FR) : "—");
        });
        if (colFrontResPass != null) {
            colFrontResPass.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(cd.getValue()));
            colFrontResPass.setCellFactory(c -> new TableCell<Reservation, Reservation>() {
                @Override
                protected void updateItem(Reservation res, boolean empty) {
                    super.updateItem(res, empty);
                    if (empty || res == null) {
                        setGraphic(null);
                        setText(null);
                        return;
                    }
                    Button pdf = new Button("PDF / QR");
                    pdf.getStyleClass().add("back-table-action-link");
                    pdf.setOnAction(ae -> exporterPassPdfReservation(res));
                    setGraphic(pdf);
                    setText(null);
                }
            });
        }
    }

    private void exporterPassPdfReservation(Reservation row) {
        if (row == null || row.getId() == null) {
            return;
        }
        Reservation r;
        try {
            r = reservationService.trouverParId(row.getId());
        } catch (Exception ex) {
            LOG.error("Chargement réservation pour PDF", ex);
            erreur("Impossible de charger la réservation.");
            return;
        }
        if (r == null) {
            erreur("Réservation introuvable.");
            return;
        }
        Window w = null;
        if (tableFrontMesReservations != null && tableFrontMesReservations.getScene() != null) {
            w = tableFrontMesReservations.getScene().getWindow();
        }
        if (w == null && tableReservations != null && tableReservations.getScene() != null) {
            w = tableReservations.getScene().getWindow();
        }
        if (w == null) {
            erreur("Fenêtre indisponible pour l’enregistrement du fichier.");
            return;
        }
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le pass (PDF)");
        fc.setInitialFileName(suggestPassPdfFileName(r));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        File f = fc.showSaveDialog(w);
        if (f == null) {
            return;
        }
        Path path = f.toPath();
        String fileName = path.getFileName().toString();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            Path parent = path.getParent();
            path = parent != null ? parent.resolve(fileName + ".pdf") : Path.of(fileName + ".pdf");
        }
        try {
            reservationPassPdfService.exportPdf(r, path);
            info("Pass PDF enregistré : " + path.getFileName());
        } catch (Exception ex) {
            LOG.error("Export pass PDF", ex);
            erreur("Impossible de créer le PDF : " + (ex.getMessage() != null ? ex.getMessage() : "erreur inconnue"));
        }
    }

    private static String suggestPassPdfFileName(Reservation r) {
        String titre = "";
        if (r.getEvenement() != null && r.getEvenement().getTitre() != null) {
            titre = r.getEvenement().getTitre().replaceAll("[^a-zA-Z0-9_-]+", "_");
        }
        if (titre.length() > 25) {
            titre = titre.substring(0, 25);
        }
        if (titre.isBlank()) {
            titre = "evenement";
        }
        return "Pass-" + titre + "-" + r.getId() + ".pdf";
    }

    private void actualiserBlocInteractionsEtPlaces(Evenement e) {
        if (e == null || e.getId() == null) {
            return;
        }
        int reserves = evenementService.sommePlacesReserveesPourEvenement(e.getId());
        if (lblFrontDetailPlacesRestantes != null) {
            if (e.getNbPlacesDisponibles() == null) {
                lblFrontDetailPlacesRestantes.setText("Places : sans limite fixée — réservations enregistrées : " + reserves + ".");
            } else {
                int rest = Math.max(0, e.getNbPlacesDisponibles() - reserves);
                lblFrontDetailPlacesRestantes.setText("Capacité : " + e.getNbPlacesDisponibles()
                        + " — réservées : " + reserves + " — restantes : " + rest + ".");
            }
        }
        if (lblFrontDetailDuree != null) {
            java.time.Duration d = e.getDureeEvenement();
            long minutes = d.toMinutes();
            lblFrontDetailDuree.setText("Durée (début → fin) : " + minutes + " minute(s).");
        }
        if (boxFrontDetailMapWrap != null && webFrontDetailMap != null) {
            String lieu = e.getLocalisation();
            if (lieu == null || lieu.isBlank()) {
                boxFrontDetailMapWrap.setVisible(false);
                boxFrontDetailMapWrap.setManaged(false);
                webFrontDetailMap.getEngine().load("about:blank");
            } else {
                boxFrontDetailMapWrap.setVisible(true);
                boxFrontDetailMapWrap.setManaged(true);
                chargerCarteEmbedGoogleMaps(lieu.trim());
            }
        }
    }

    /**
     * Carte centrée sur le lieu, comme le site Symfony (iframe {@code output=embed}), sans la page Maps « recherche ».
     */
    private void chargerCarteEmbedGoogleMaps(String lieu) {
        if (webFrontDetailMap == null || lieu == null || lieu.isBlank()) {
            return;
        }
        String q = URLEncoder.encode(lieu, StandardCharsets.UTF_8);
        String src = "https://www.google.com/maps?q=" + q + "&output=embed&z=15";
        String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/>"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"/>"
                + "<style>html,body{margin:0;padding:0;height:100%;width:100%;overflow:hidden;background:#1a1a1a}"
                + "iframe{border:0;width:100%;height:100%;display:block}</style>"
                + "</head><body>"
                + "<iframe title=\"Carte\" width=\"100%\" height=\"100%\" "
                + "src=\"" + src + "\" "
                + "allowfullscreen=\"true\" "
                + "referrerpolicy=\"no-referrer-when-downgrade\"></iframe>"
                + "</body></html>";
        webFrontDetailMap.getEngine().loadContent(html);
    }

    private void allerVueListeEvenements() {
        if (toggleBackNavEvListe != null) {
            toggleBackNavEvListe.setSelected(true);
        }
    }

    private void allerVueListeProgrammes() {
        if (toggleBackNavPrListe != null) {
            toggleBackNavPrListe.setSelected(true);
        }
    }

    private void mettreAJourCompteurEvenementsBack() {
        if (lblBackEvResultCount == null) {
            return;
        }
        int n = evenementsFiltresBack != null ? evenementsFiltresBack.size() : evenementsData.size();
        lblBackEvResultCount.setText("Showing " + n + " of " + evenementsData.size() + " entries");
        mettreAJourMetriquesEvenementsBack();
        majEtatVideBackEvenements();
    }

    private void mettreAJourMetriquesEvenementsBack() {
        if (totalEventsMetricLabel != null) {
            totalEventsMetricLabel.setText(String.valueOf(evenementsData.size()));
        }
        if (eventsWithProgramMetricLabel != null) {
            long withProgram = evenementsData.stream().filter(e -> e.getProgramme() != null).count();
            eventsWithProgramMetricLabel.setText(String.valueOf(withProgram));
        }
        if (eventReservationsMetricLabel != null) {
            eventReservationsMetricLabel.setText(String.valueOf(reservationsData.size()));
        }
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
                    ? "No events in the database. Use Create an Event to add one."
                    : "No event matches the current filter. Change the search or click Reset.");
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
                if (toggleBackNavStats != null && toggleBackNavStats.isSelected()) {
                    rafraichirPageStatistiques();
                }
                e.consume();
                return;
            }
            if (e.getCode() == KeyCode.ESCAPE) {
                if (boxFrontEvDetail != null && boxFrontEvDetail.isVisible()) {
                    onFrontRetourListeEvenements();
                    e.consume();
                    return;
                }
                if (paneBackPageEvDetail != null && paneBackPageEvDetail.isVisible()) {
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
        backNavGroup.selectToggle(null);
        montrerSeulementPageBack(PG_BACK_EV_DETAIL);
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
        if (toggleBackNavEvForm != null) {
            toggleBackNavEvForm.setSelected(true);
        }
        onNouveauEvenement();
    }

    @FXML
    private void onBackEvModifierVersFormulaire() {
        if (tableEvenements.getSelectionModel().getSelectedItem() == null) {
            erreur("Sélectionnez un événement dans la liste pour le modifier.");
            return;
        }
        if (toggleBackNavEvForm != null) {
            toggleBackNavEvForm.setSelected(true);
        }
    }

    @FXML
    private void onBackEvRetourListe() {
        allerVueListeEvenements();
    }

    @FXML
    private void onBackPrNouveauVersFormulaire() {
        if (toggleBackNavPrForm != null) {
            toggleBackNavPrForm.setSelected(true);
        }
        onNouveauProgramme();
    }

    @FXML
    private void onBackPrModifierVersFormulaire() {
        if (tableProgrammes.getSelectionModel().getSelectedItem() == null) {
            erreur("Sélectionnez un programme dans la liste pour le modifier.");
            return;
        }
        if (toggleBackNavPrForm != null) {
            toggleBackNavPrForm.setSelected(true);
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

    /**
     * Construit une URL d’image (modèle de diffusion) à partir du titre / description, puis l’affiche en aperçu.
     */
    @FXML
    private void onGenererImageEvenementIa() {
        if (tfEvTitre == null || taEvDescription == null || tfEvImage == null) {
            return;
        }
        String titre = tfEvTitre.getText();
        String desc = taEvDescription.getText();
        if (titre != null) {
            titre = titre.trim();
        } else {
            titre = "";
        }
        if (desc != null) {
            desc = desc.trim();
        } else {
            desc = "";
        }
        if (titre.isEmpty() && desc.isEmpty()) {
            erreur("Saisissez au moins un titre ou une description pour générer l’image.");
            return;
        }
        if (btnGenererImageIa != null) {
            btnGenererImageIa.setDisable(true);
        }
        final String ft = titre;
        final String fd = desc;
        CompletableFuture.supplyAsync(() -> evenementImageAiService.buildImageUrlFromEvenementText(ft, fd))
                .thenAccept(url -> Platform.runLater(() -> {
                    if (apercuImageDebounce != null) {
                        apercuImageDebounce.stop();
                    }
                    tfEvImage.setText(url);
                    actualiserApercuImageBackOffice();
                    if (btnGenererImageIa != null) {
                        btnGenererImageIa.setDisable(false);
                    }
                }))
                .exceptionally(ex -> {
                    Throwable c = ex;
                    if (c.getCause() != null) {
                        c = c.getCause();
                    }
                    String msg = c.getMessage() != null ? c.getMessage() : "Génération impossible.";
                    LOG.warn("Génération image IA événement: {}", msg);
                    Platform.runLater(() -> {
                        erreur(msg);
                        if (btnGenererImageIa != null) {
                            btnGenererImageIa.setDisable(false);
                        }
                    });
                    return null;
                });
    }

    /** Génère 4 images IA ; l’utilisateur en choisit une → remplit uniquement le champ image (pas d’entité supplémentaire). */
    @FXML
    private void onGenererAlbumEvenementIa() {
        if (tfEvTitre == null || taEvDescription == null || tfEvImage == null) {
            return;
        }
        String titre = tfEvTitre.getText();
        String desc = taEvDescription.getText();
        if (titre != null) {
            titre = titre.trim();
        } else {
            titre = "";
        }
        if (desc != null) {
            desc = desc.trim();
        } else {
            desc = "";
        }
        if (titre.isEmpty() && desc.isEmpty()) {
            erreur("Saisissez au moins un titre ou une description pour générer l’album.");
            return;
        }
        final int nb = 4;
        final String ft = titre;
        final String fd = desc;
        if (btnGenererAlbumIa != null) {
            btnGenererAlbumIa.setDisable(true);
        }
        if (btnGenererImageIa != null) {
            btnGenererImageIa.setDisable(true);
        }
        CompletableFuture.supplyAsync(() -> evenementImageAiService.buildAlbumImageUrls(ft, fd, nb))
                .thenAccept(urls -> Platform.runLater(() -> {
                    afficherAlbumIaChoix(urls);
                    if (btnGenererAlbumIa != null) {
                        btnGenererAlbumIa.setDisable(false);
                    }
                    if (btnGenererImageIa != null) {
                        btnGenererImageIa.setDisable(false);
                    }
                }))
                .exceptionally(ex -> {
                    Throwable c = ex;
                    if (c.getCause() != null) {
                        c = c.getCause();
                    }
                    String msg = c.getMessage() != null ? c.getMessage() : "Génération album impossible.";
                    LOG.warn("Génération album IA: {}", msg);
                    Platform.runLater(() -> {
                        erreur(msg);
                        if (btnGenererAlbumIa != null) {
                            btnGenererAlbumIa.setDisable(false);
                        }
                        if (btnGenererImageIa != null) {
                            btnGenererImageIa.setDisable(false);
                        }
                    });
                    return null;
                });
    }

    private void effacerAlbumIaPropositions() {
        if (boxEvAlbumIaThumbs != null) {
            boxEvAlbumIaThumbs.getChildren().clear();
        }
        if (boxEvAlbumIaWrap != null) {
            boxEvAlbumIaWrap.setVisible(false);
            boxEvAlbumIaWrap.setManaged(false);
        }
    }

    private void afficherAlbumIaChoix(List<String> urls) {
        if (boxEvAlbumIaThumbs == null || boxEvAlbumIaWrap == null) {
            return;
        }
        boxEvAlbumIaThumbs.getChildren().clear();
        List<String> urlList = new ArrayList<>();
        List<ImageView> ivList = new ArrayList<>();
        for (String url : urls) {
            if (url == null || url.isBlank()) {
                continue;
            }
            StackPane wrap = new StackPane();
            wrap.getStyleClass().add("album-ia-thumb");
            wrap.setCursor(Cursor.HAND);
            wrap.setPrefSize(148, 108);
            Label lblWait = new Label("…");
            lblWait.getStyleClass().add("album-ia-thumb-wait");
            ImageView iv = new ImageView();
            iv.setFitWidth(132);
            iv.setFitHeight(92);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);
            wrap.getChildren().addAll(lblWait, iv);
            StackPane.setAlignment(lblWait, Pos.CENTER);
            StackPane.setAlignment(iv, Pos.CENTER);
            final String u = url.trim();
            urlList.add(u);
            ivList.add(iv);
            wrap.setOnMouseClicked(ev -> {
                if (boxEvAlbumIaThumbs != null) {
                    for (Node n : boxEvAlbumIaThumbs.getChildren()) {
                        n.getStyleClass().remove("album-ia-thumb-selected");
                    }
                }
                wrap.getStyleClass().add("album-ia-thumb-selected");
                if (apercuImageDebounce != null) {
                    apercuImageDebounce.stop();
                }
                tfEvImage.setText(u);
                actualiserApercuImageBackOffice();
            });
            boxEvAlbumIaThumbs.getChildren().add(wrap);
        }
        boolean show = !boxEvAlbumIaThumbs.getChildren().isEmpty();
        boxEvAlbumIaWrap.setVisible(show);
        boxEvAlbumIaWrap.setManaged(show);
        if (!show) {
            return;
        }
        // Téléchargements séquentiels + longue pause : Pollinations rate-limite fortement les requêtes rapprochées.
        CompletableFuture.runAsync(() -> {
            for (int i = 0; i < urlList.size(); i++) {
                if (i > 0) {
                    try {
                        Thread.sleep(3200);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                String u = urlList.get(i);
                byte[] bytes = telechargerImageHttpBytes(u);
                final int idx = i;
                final byte[] fb = bytes;
                Platform.runLater(() -> {
                    ImageView iv = ivList.get(idx);
                    StackPane parent = (StackPane) iv.getParent();
                    Label waitLbl = null;
                    if (parent != null) {
                        for (Node n : parent.getChildren()) {
                            if (n instanceof Label lbl && lbl.getStyleClass().contains("album-ia-thumb-wait")) {
                                waitLbl = lbl;
                                break;
                            }
                        }
                    }
                    if (fb != null && appliquerBytesAImageView(iv, fb, 264, 184)) {
                        if (parent != null && waitLbl != null) {
                            parent.getChildren().remove(waitLbl);
                        }
                    } else if (waitLbl != null) {
                        waitLbl.setText("×");
                        waitLbl.setTooltip(new Tooltip("Échec du chargement"));
                    }
                });
            }
        });
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
        if (frontEvVueListe) {
            boxFrontEvList.setVisible(true);
            boxFrontEvList.setManaged(true);
            if (boxFrontEvCalendar != null) {
                boxFrontEvCalendar.setVisible(false);
                boxFrontEvCalendar.setManaged(false);
            }
        } else {
            boxFrontEvList.setVisible(false);
            boxFrontEvList.setManaged(false);
            if (boxFrontEvCalendar != null) {
                boxFrontEvCalendar.setVisible(true);
                boxFrontEvCalendar.setManaged(true);
                rafraichirCalendrierEvenementsFront();
            }
        }
    }

    @FXML
    private void onFrontCalPrevMonth() {
        frontEvenementsCalMonth = frontEvenementsCalMonth.minusMonths(1);
        rafraichirCalendrierEvenementsFront();
    }

    @FXML
    private void onFrontCalNextMonth() {
        frontEvenementsCalMonth = frontEvenementsCalMonth.plusMonths(1);
        rafraichirCalendrierEvenementsFront();
    }

    private List<Evenement> listEvenementsFrontFiltresTri() {
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
        return base;
    }

    private void rafraichirCartesEvenementsFront() {
        flowEvenementsCards.getChildren().clear();
        List<Evenement> base = listEvenementsFrontFiltresTri();
        for (Evenement e : base) {
            flowEvenementsCards.getChildren().add(creerCarteEvenement(e));
        }
        lblFrontEvenementsCount.setText(base.size() + " événement(s)");
        majEtatVideFrontEvenements();
        if (toggleFrontEvCalendrier != null && toggleFrontEvCalendrier.isSelected()) {
            rafraichirCalendrierEvenementsFront();
        }
    }

    private static String capitalizeMonthTitle(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static Map<LocalDate, List<Evenement>> grouperEvenementsParDate(List<Evenement> list) {
        Map<LocalDate, List<Evenement>> m = new HashMap<>();
        for (Evenement ev : list) {
            LocalDate d = ev.getDateEvenement();
            if (d != null) {
                m.computeIfAbsent(d, k -> new ArrayList<>()).add(ev);
            }
        }
        for (List<Evenement> dayList : m.values()) {
            dayList.sort(Comparator.comparing(Evenement::getHeureDebut, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Evenement::getTitre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        }
        return m;
    }

    private void rafraichirCalendrierEvenementsFront() {
        if (gridFrontEvenementsCalendar == null || lblFrontCalMonthTitle == null) {
            return;
        }
        gridFrontEvenementsCalendar.getChildren().clear();
        List<Evenement> filtered = listEvenementsFrontFiltresTri();
        lblFrontCalMonthTitle.setText(capitalizeMonthTitle(frontEvenementsCalMonth.format(MONTH_TITLE_FR)));

        if (filtered.isEmpty()) {
            Label empty = new Label(evenementsData.isEmpty()
                    ? "Aucun événement pour le moment."
                    : "Aucun événement ne correspond à votre recherche ou au tri actuel.");
            empty.getStyleClass().add("front-cal-empty-msg");
            empty.setWrapText(true);
            empty.setMaxWidth(Double.MAX_VALUE);
            gridFrontEvenementsCalendar.add(empty, 0, 0);
            GridPane.setColumnSpan(empty, 7);
            return;
        }

        Map<LocalDate, List<Evenement>> byDay = grouperEvenementsParDate(filtered);
        LocalDate first = frontEvenementsCalMonth.atDay(1);
        int colStart = first.getDayOfWeek().getValue() == 7 ? 6 : first.getDayOfWeek().getValue() - 1;
        LocalDate gridStart = first.minusDays(colStart);

        for (int c = 0; c < 7; c++) {
            Label h = new Label(CAL_WEEK_HEADERS[c]);
            h.getStyleClass().add("front-cal-weekday");
            h.setMaxWidth(Double.MAX_VALUE);
            gridFrontEvenementsCalendar.add(h, c, 0);
        }

        for (int i = 0; i < 42; i++) {
            LocalDate d = gridStart.plusDays(i);
            VBox cell = creerCelluleCalendrierJour(d, frontEvenementsCalMonth, byDay.getOrDefault(d, List.of()));
            gridFrontEvenementsCalendar.add(cell, i % 7, 1 + i / 7);
        }
    }

    private VBox creerCelluleCalendrierJour(LocalDate d, YearMonth ym, List<Evenement> jour) {
        VBox cell = new VBox(4);
        cell.getStyleClass().add("front-cal-cell");
        boolean autreMois = !ym.equals(YearMonth.from(d));
        if (autreMois) {
            cell.getStyleClass().add("front-cal-cell-other");
        }
        if (d.equals(LocalDate.now())) {
            cell.getStyleClass().add("front-cal-cell-today");
        }

        Label num = new Label(String.valueOf(d.getDayOfMonth()));
        num.getStyleClass().add("front-cal-day-num");
        if (autreMois) {
            num.getStyleClass().add("front-cal-day-num-sub");
        }

        VBox links = new VBox(2);
        int max = 3;
        for (int j = 0; j < Math.min(max, jour.size()); j++) {
            Evenement ev = jour.get(j);
            Label link = new Label(tronquer(nvl(ev.getTitre()), 32));
            link.getStyleClass().add("front-cal-ev-link");
            link.setWrapText(true);
            link.setOnMouseClicked(e -> ouvrirDetailEvenementFront(ev));
            links.getChildren().add(link);
        }
        if (jour.size() > max) {
            Label more = new Label("+" + (jour.size() - max) + " autre(s)");
            more.getStyleClass().add("front-cal-ev-more");
            links.getChildren().add(more);
        }
        cell.getChildren().addAll(num, links);
        return cell;
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

        HBox reactions = creerBarreReactionsEvenement(e);

        HBox actions = new HBox(10);
        actions.getStyleClass().add("event-card-actions");
        Button btnParticiper = new Button("Participer");
        btnParticiper.getStyleClass().addAll("btn-participer");
        btnParticiper.setOnAction(ev -> ouvrirPageParticiper(e));
        Button btnDetails = new Button("Détails");
        btnDetails.getStyleClass().addAll("btn-details");
        btnDetails.setOnAction(ev -> ouvrirDetailEvenementFront(e));
        actions.getChildren().addAll(btnParticiper, btnDetails);

        body.getChildren().addAll(meta, titre, desc, badge, reactions, actions);
        card.getChildren().addAll(imgWrap, body);
        return card;
    }

    private byte[] telechargerImageHttpBytes(String url) {
        final int maxAttempts = 6;
        String urlHead = url.length() > 96 ? url.substring(0, 96) + "…" : url;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (attempt > 1) {
                    long backoffMs = Math.min(10_000L, 1400L * (attempt - 1));
                    Thread.sleep(backoffMs);
                }
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder(URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(180))
                        .header("User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
                                        + "Chrome/120.0.0.0 Safari/537.36")
                        .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                        .header("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8")
                        .header("Referer", "https://pollinations.ai/");
                HttpRequest req = reqBuilder.GET().build();
                HttpResponse<byte[]> resp = HTTP_CLIENT_IMAGE.send(req, HttpResponse.BodyHandlers.ofByteArray());
                int code = resp.statusCode();
                if (code == 429 || code == 503 || code == 502 || code == 504) {
                    LOG.info("Image HTTP {} — nouvelle tentative {}/{} ({})", code, attempt, maxAttempts, urlHead);
                    continue;
                }
                if (code < 200 || code >= 300) {
                    LOG.warn("Image HTTP status {} pour {}", code, urlHead);
                    return null;
                }
                byte[] body = resp.body();
                if (body == null || body.length < 256) {
                    LOG.warn("Corps image absent ou trop petit ({} o), tentative {}/{} — {}",
                            body == null ? -1 : body.length, attempt, maxAttempts, urlHead);
                    continue;
                }
                return body;
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception ex) {
                LOG.warn("Téléchargement image tentative {}/{}: {} — {}", attempt, maxAttempts, ex.getMessage(), urlHead);
                if (attempt == maxAttempts) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Décode les octets en {@link Image} (flux complet, sans dimensions forcées : meilleure compatibilité PNG/JPEG lourds).
     *
     * @return {@code true} si l’image a été appliquée sans erreur JavaFX
     */
    private static boolean appliquerBytesAImageView(ImageView iv, byte[] bytes, double decodeW, double decodeH) {
        if (bytes == null || bytes.length < 64) {
            return false;
        }
        try {
            Image img = new Image(new ByteArrayInputStream(bytes));
            if (img.isError()) {
                return false;
            }
            iv.setImage(img);
            return true;
        } catch (Exception ex) {
            LOG.warn("Décodage image: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Télécharge une image HTTP(S) hors thread UI puis affiche (évite échecs silencieux avec Pollinations / User-Agent JavaFX).
     */
    private void chargerImageHttpUrlAvecClient(ImageView iv, String url, double decodeW, double decodeH) {
        iv.setImage(null);
        CompletableFuture.supplyAsync(() -> telechargerImageHttpBytes(url))
                .thenAccept(bytes -> Platform.runLater(() -> {
                    if (!appliquerBytesAImageView(iv, bytes, decodeW, decodeH)) {
                        LOG.warn("Impossible d’afficher l’image après téléchargement");
                    }
                }));
    }

    /**
     * Affiche une image dans un {@link ImageView} : fichiers locaux (chemin ou {@code file:}),
     * ou URL http(s). Les fichiers sont lus en synchrone (fiabilité sous Windows) ;
     * les URL sont chargées en arrière-plan via {@link #chargerImageHttpUrlAvecClient}.
     */
    private void chargerImageEvenement(ImageView iv, String raw, double decodeW, double decodeH) {
        iv.setImage(null);
        if (raw == null || raw.isBlank()) {
            return;
        }
        String s = raw.trim();
        try {
            if (s.startsWith("http://") || s.startsWith("https://")) {
                chargerImageHttpUrlAvecClient(iv, s, decodeW, decodeH);
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
        frontDetailEvenementId = e.getId();
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

        actualiserBlocInteractionsEtPlaces(e);

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
        if (boxFrontEvCalendar != null) {
            boxFrontEvCalendar.setVisible(false);
            boxFrontEvCalendar.setManaged(false);
        }
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
            boolean nouveau = e.getId() == null;
            evenementService.enregistrer(e);
            if (nouveau) {
                Integer excludeUserId = null;
                if (SessionManager.getCurrentUser() != null) {
                    excludeUserId = SessionManager.getCurrentUser().getId();
                }
                final Evenement evenementPourNotification = e;
                final Integer excl = excludeUserId;
                CompletableFuture.runAsync(() -> {
                    try {
                        EvenementNotificationMailService.MailReport rep =
                                evenementNotificationMailService.notifyNewEvent(evenementPourNotification, excl);
                        if (rep.sent() > 0 || rep.failed() > 0) {
                            LOG.info("E-mails nouvel événement : {} envoyé(s), {} échec(s), {} destinataire(s).",
                                    rep.sent(), rep.failed(), rep.totalDestinataires());
                        }
                    } catch (Exception ex) {
                        LOG.warn("Notification e-mail nouvel événement : {}", ex.getMessage());
                    }
                });
            }
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
        if (toggleBackNavStats != null && toggleBackNavStats.isSelected()) {
            rafraichirPageStatistiques();
        }
        if (toggleFrontNavAccueil != null && toggleFrontNavAccueil.isSelected()) {
            majInfosAccueil();
        }
        majBadgeFavoris();
        if (toggleFrontNavFavoris != null && toggleFrontNavFavoris.isSelected()) {
            rafraichirFavorisFront();
        }
        if (toggleFrontNavResa != null && toggleFrontNavResa.isSelected()) {
            rafraichirMesReservationsFront();
        }
        if (toggleBackNavResListe != null && toggleBackNavResListe.isSelected()) {
            onRafraichirReservationsBack();
        }
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
        if (toggleBackNavStats != null && toggleBackNavStats.isSelected()) {
            rafraichirPageStatistiques();
        }
        if (toggleFrontNavAccueil != null && toggleFrontNavAccueil.isSelected()) {
            majInfosAccueil();
        }
    }

    @FXML
    private void onGenererActivitesProgrammeIa() {
        if (cbPrEvenement == null || taPrActivites == null) {
            return;
        }
        Evenement ev = cbPrEvenement.getSelectionModel().getSelectedItem();
        if (ev == null) {
            erreur("Choisissez d'abord un événement.");
            return;
        }
        if (ev.getHeureDebut() == null || ev.getHeureFin() == null || !ev.getHeureFin().isAfter(ev.getHeureDebut())) {
            erreur("L'événement sélectionné doit avoir des horaires valides (début < fin).");
            return;
        }

        LocalTime pauseDebut = null;
        LocalTime pauseFin = null;
        if (spPrPauseDebutHeure != null && spPrPauseDebutMinute != null && spPrPauseFinHeure != null && spPrPauseFinMinute != null) {
            pauseDebut = LocalTime.of(spPrPauseDebutHeure.getValue(), spPrPauseDebutMinute.getValue());
            pauseFin = LocalTime.of(spPrPauseFinHeure.getValue(), spPrPauseFinMinute.getValue());
            if (!pauseFin.isAfter(pauseDebut)) {
                erreur("La fin de pause doit être après le début pour générer les activités.");
                return;
            }
        }

        if (btnPrGenererActivitesIa != null) {
            btnPrGenererActivitesIa.setDisable(true);
        }

        final LocalTime fPauseDebut = pauseDebut;
        final LocalTime fPauseFin = pauseFin;
        CompletableFuture.supplyAsync(() -> programmeActivitesAiService.genererActivites(
                        nvl(ev.getTitre()),
                        nvl(ev.getDescription()),
                        ev.getHeureDebut(),
                        ev.getHeureFin(),
                        fPauseDebut,
                        fPauseFin))
                .thenAccept(text -> Platform.runLater(() -> {
                    taPrActivites.setText(text);
                    rafraichirApercuActivitesProgramme(text);
                    if (btnPrGenererActivitesIa != null) {
                        btnPrGenererActivitesIa.setDisable(false);
                    }
                    info("Activités générées.");
                }))
                .exceptionally(ex -> {
                    Throwable c = ex.getCause() != null ? ex.getCause() : ex;
                    Platform.runLater(() -> {
                        erreur(c.getMessage() != null ? c.getMessage() : "Génération des activités impossible.");
                        if (btnPrGenererActivitesIa != null) {
                            btnPrGenererActivitesIa.setDisable(false);
                        }
                    });
                    return null;
                });
    }

    @FXML
    private void onBasculerEditionActivitesProgramme() {
        appliquerModeEditionActivitesProgramme(!editionActivitesProgramme);
    }

    private void appliquerModeEditionActivitesProgramme(boolean edition) {
        editionActivitesProgramme = edition;
        if (taPrActivites != null) {
            taPrActivites.setVisible(edition);
            taPrActivites.setManaged(edition);
        }
        if (spPrActivitesPreview != null) {
            spPrActivitesPreview.setVisible(!edition);
            spPrActivitesPreview.setManaged(!edition);
        }
        if (btnPrModeEditionActivites != null) {
            btnPrModeEditionActivites.setText(edition ? "Aperçu" : "Modifier");
        }
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
        effacerAlbumIaPropositions();
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
        effacerAlbumIaPropositions();
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
        rafraichirApercuActivitesProgramme(taPrActivites.getText());
        appliquerModeEditionActivitesProgramme(false);
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
        rafraichirApercuActivitesProgramme("");
        appliquerModeEditionActivitesProgramme(false);
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

    private void rafraichirApercuActivitesProgramme(String raw) {
        if (flowPrActivitesPreview == null) {
            return;
        }
        flowPrActivitesPreview.getChildren().clear();
        String txt = raw == null ? "" : raw.trim();
        if (txt.isEmpty()) {
            Text hint = new Text("Aperçu coloré des activités IA.");
            hint.setFill(Color.web("#94a3b8"));
            hint.setFont(Font.font("System", 12));
            flowPrActivitesPreview.getChildren().add(hint);
            return;
        }

        String[] lines = txt.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isBlank()) {
                continue;
            }
            Matcher m = ACTIVITE_PREVIEW_PATTERN.matcher(line);
            if (m.matches()) {
                String time = m.group(1);
                String titre = m.group(2);
                String detail = m.group(3);

                Text tTime = new Text("• " + time + " : ");
                tTime.setFill(Color.web("#38bdf8"));
                tTime.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13));
                flowPrActivitesPreview.getChildren().add(tTime);

                Text tTitre = new Text(titre.trim());
                tTitre.setFill(Color.web("#fbbf24"));
                tTitre.setFont(Font.font("System", FontWeight.BOLD, 13));
                flowPrActivitesPreview.getChildren().add(tTitre);

                if (detail != null && !detail.isBlank()) {
                    Text tDetail = new Text(" — " + detail.trim());
                    tDetail.setFill(Color.web("#cbd5e1"));
                    tDetail.setFont(Font.font("System", 12.5));
                    flowPrActivitesPreview.getChildren().add(tDetail);
                }
            } else {
                Text tRaw = new Text(line.trim());
                tRaw.setFill(Color.web("#e2e8f0"));
                tRaw.setFont(Font.font("System", 12.5));
                flowPrActivitesPreview.getChildren().add(tRaw);
            }
            if (i < lines.length - 1) {
                flowPrActivitesPreview.getChildren().add(new Text("\n"));
            }
        }
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

    /** Ligne du tableau des statistiques par type (JavaBean pour {@link PropertyValueFactory}). */
    public static class LigneStatsParType {
        private final String libelleType;
        private final int nombre;
        private final String pourcentage;

        public LigneStatsParType(String libelleType, int nombre, int totalEvenements) {
            this.libelleType = libelleType;
            this.nombre = nombre;
            if (totalEvenements > 0) {
                this.pourcentage = String.format(Locale.FRENCH, "%.1f %%", 100.0 * nombre / totalEvenements);
            } else {
                this.pourcentage = "—";
            }
        }

        public String getLibelleType() {
            return libelleType;
        }

        public int getNombre() {
            return nombre;
        }

        public String getPourcentage() {
            return pourcentage;
        }
    }
}
