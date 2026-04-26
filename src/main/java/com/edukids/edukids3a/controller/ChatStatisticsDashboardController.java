package com.edukids.edukids3a.controller;

import com.edukids.edukids3a.model.ChatChartPoint;
import com.edukids.edukids3a.model.ChatDashboardCharts;
import com.edukids.edukids3a.model.ChatDashboardPeriod;
import com.edukids.edukids3a.model.ChatDashboardSnapshot;
import com.edukids.edukids3a.model.ChatRecentActivity;
import com.edukids.edukids3a.model.ChatTopUser;
import com.edukids.edukids3a.service.dashboard.ChatDashboardMysqlService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatStatisticsDashboardController {

    private static final Logger LOG = LoggerFactory.getLogger(ChatStatisticsDashboardController.class);
    private static final DecimalFormat INTEGER_FMT = new DecimalFormat("#,##0");

    private final ChatDashboardMysqlService apiService = new ChatDashboardMysqlService();
    private final ToggleGroup periodGroup = new ToggleGroup();
    private final AtomicBoolean loading = new AtomicBoolean(false);
    private final ObservableList<ChatTopUser> topUsers = FXCollections.observableArrayList();
    private final ObservableList<PieChart.Data> donutData = FXCollections.observableArrayList();
    private final ObservableList<ChatChartPoint> hourlyPoints = FXCollections.observableArrayList();
    private final ObservableList<ChatChartPoint> dailyPoints = FXCollections.observableArrayList();

    private ChatDashboardPeriod currentPeriod = ChatDashboardPeriod.TODAY;
    private Timeline autoRefresh;
    private Long selectedUserId;
    private String selectedUserName;
    private boolean suppressSelectionEvents;

    @FXML private Label lblDashboardTitle;
    @FXML private Label lblTimezone;
    @FXML private Label lblGeneratedAt;
    @FXML private Label lblLiveBadge;
    @FXML private Label lblPeriodSummary;
    @FXML private Label lblFocusedUser;

    @FXML private ToggleButton btnPeriodToday;
    @FXML private ToggleButton btnPeriod7Days;
    @FXML private ToggleButton btnPeriod30Days;
    @FXML private Button btnRefresh;

    @FXML private Label lblTotalMessages;
    @FXML private Label lblTotalUsers;
    @FXML private Label lblActiveUsers7;
    @FXML private Label lblActiveUsers30;
    @FXML private Label lblTopUserName;
    @FXML private Label lblTopUserMessages;
    @FXML private Label lblMessagesToday;
    @FXML private Label lblMessagesWeek;

    @FXML private AreaChart<String, Number> hourlyChart;
    @FXML private PieChart donutChart;
    @FXML private LineChart<String, Number> dailyChart;
    @FXML private VBox boxRecentActivity;
    @FXML private TableView<ChatTopUser> tableTopUsers;
    @FXML private TableColumn<ChatTopUser, Number> colRank;
    @FXML private TableColumn<ChatTopUser, String> colName;
    @FXML private TableColumn<ChatTopUser, String> colEmail;
    @FXML private TableColumn<ChatTopUser, Number> colMessages;
    @FXML private TableColumn<ChatTopUser, Number> colProgress;
    @FXML private TableColumn<ChatTopUser, String> colLastActivity;

    @FXML
    private void initialize() {
        btnPeriodToday.setToggleGroup(periodGroup);
        btnPeriod7Days.setToggleGroup(periodGroup);
        btnPeriod30Days.setToggleGroup(periodGroup);
        btnPeriodToday.setSelected(true);
        updateFocusLabel(null);

        configureTable();
        configureCharts();
        updatePeriodSummary(ChatDashboardPeriod.TODAY);

        periodGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                return;
            }
            if (newToggle == btnPeriodToday) {
                changePeriod(ChatDashboardPeriod.TODAY);
            } else if (newToggle == btnPeriod7Days) {
                changePeriod(ChatDashboardPeriod.DAYS_7);
            } else if (newToggle == btnPeriod30Days) {
                changePeriod(ChatDashboardPeriod.DAYS_30);
            }
        });

        tableTopUsers.getSelectionModel().selectedItemProperty().addListener((obs, oldUser, newUser) -> {
            if (suppressSelectionEvents) {
                return;
            }
            if (newUser == null) {
                selectedUserId = null;
                selectedUserName = null;
                updateFocusLabel(null);
                refreshDashboard();
                return;
            }
            selectedUserId = newUser.getUserId();
            selectedUserName = newUser.getName();
            updateFocusLabel(selectedUserName);
            refreshDashboard();
        });

        autoRefresh = new Timeline(new KeyFrame(Duration.seconds(30), e -> refreshDashboard()));
        autoRefresh.setCycleCount(Animation.INDEFINITE);

        refreshDashboard();
        autoRefresh.playFromStart();
    }

    @FXML
    private void onManualRefresh() {
        refreshDashboard();
    }

    private void changePeriod(ChatDashboardPeriod period) {
        if (period == null || period == currentPeriod) {
            refreshDashboard();
            return;
        }
        currentPeriod = period;
        updatePeriodSummary(period);
        refreshDashboard();
    }

    private void refreshDashboard() {
        if (!loading.compareAndSet(false, true)) {
            return;
        }

        setLiveBadge("Actualisation...");
        btnRefresh.setDisable(true);

        Task<DashboardLoadResult> task = new Task<>() {
            @Override
            protected DashboardLoadResult call() {
                ChatDashboardSnapshot globalSnapshot = apiService.fetchDashboard(currentPeriod);
                ChatDashboardSnapshot focusedSnapshot = selectedUserId == null ? null : apiService.fetchDashboard(currentPeriod, selectedUserId);
                return new DashboardLoadResult(globalSnapshot, focusedSnapshot);
            }
        };

        task.setOnSucceeded(event -> {
            loading.set(false);
            btnRefresh.setDisable(false);
            DashboardLoadResult result = task.getValue();
            renderSnapshot(result == null ? null : result.globalSnapshot(), result == null ? null : result.focusedSnapshot());
        });
        task.setOnFailed(event -> {
            loading.set(false);
            btnRefresh.setDisable(false);
            LOG.error("Erreur lors du chargement du dashboard chat", task.getException());
            setLiveBadge("Erreur");
        });

        Thread thread = new Thread(task, "chat-dashboard-refresh");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderSnapshot(ChatDashboardSnapshot globalSnapshot, ChatDashboardSnapshot focusedSnapshot) {
        if (globalSnapshot == null) {
            return;
        }

        lblDashboardTitle.setText("Statistiques du Chat");
        lblTimezone.setText("Fuseau horaire: " + safe(globalSnapshot.getTimezone()));
        lblGeneratedAt.setText("Derniere mise a jour: " + safe(globalSnapshot.getGeneratedAt()));
        setLiveBadge("Live");

        lblTotalMessages.setText(format(globalSnapshot.getTotalMessages()));
        lblTotalUsers.setText(format(globalSnapshot.getTotalUsers()));
        lblActiveUsers7.setText(format(globalSnapshot.getActiveUsers7()));
        lblActiveUsers30.setText(format(globalSnapshot.getActiveUsers30()));
        lblMessagesToday.setText(format(globalSnapshot.getMessagesToday()));
        lblMessagesWeek.setText(format(globalSnapshot.getMessagesWeek()));

        ChatDashboardSnapshot chartSnapshot = focusedSnapshot != null ? focusedSnapshot : globalSnapshot;
        lblTopUserName.setText(safe(chartSnapshot.getTopUserName()));
        lblTopUserMessages.setText(format(chartSnapshot.getTopUserMessages()) + " messages");
        updateFocusLabel(selectedUserName);

        renderHourlyChart(chartSnapshot.getCharts());
        renderDailyChart(chartSnapshot.getCharts());
        renderDonutChart(chartSnapshot.getCharts());
        renderRecentActivity(chartSnapshot.getRecentActivities());
        renderTopUsers(globalSnapshot.getTopUsers());
        restoreSelectedRow(globalSnapshot.getTopUsers());
    }

    private void configureTable() {
        colRank.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getRank()));
        colName.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(safe(cell.getValue().getName())));
        colEmail.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(safe(cell.getValue().getEmail())));
        colMessages.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getMessages()));
        colLastActivity.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(safe(cell.getValue().getLastActivity())));
        colProgress.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getProgress()));
        colProgress.setCellFactory(column -> new TableCell<>() {
            private final ProgressBar progressBar = new ProgressBar(0);
            private final Label label = new Label();
            private final HBox wrapper = new HBox(10, progressBar, label);

            {
                wrapper.setAlignment(Pos.CENTER_LEFT);
                progressBar.setPrefWidth(120);
                label.getStyleClass().add("chat-dashboard-table-progress-label");
            }

            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    double progress = Math.max(0, Math.min(1, item.doubleValue() / 100.0));
                    progressBar.setProgress(progress);
                    label.setText((int) Math.round(progress * 100) + "%");
                    setGraphic(wrapper);
                }
            }
        });
        tableTopUsers.setItems(topUsers);
    }

    private void restoreSelectedRow(List<ChatTopUser> users) {
        if (selectedUserId == null || users == null || users.isEmpty()) {
            return;
        }
        for (int i = 0; i < users.size(); i++) {
            ChatTopUser user = users.get(i);
            if (user != null && selectedUserId.equals(user.getUserId())) {
                suppressSelectionEvents = true;
                tableTopUsers.getSelectionModel().select(i);
                tableTopUsers.scrollTo(i);
                suppressSelectionEvents = false;
                return;
            }
        }
    }

    private void configureCharts() {
        hourlyChart.setLegendVisible(false);
        dailyChart.setLegendVisible(false);
        donutChart.setLegendVisible(true);
        donutChart.setLabelsVisible(true);
    }

    private void renderHourlyChart(ChatDashboardCharts charts) {
        hourlyChart.getData().clear();
        if (charts == null) {
            return;
        }
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Messages");
        hourlyPoints.setAll(charts.getHourly());
        for (ChatChartPoint point : hourlyPoints) {
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(point.getLabel(), point.getValue()));
        }
        hourlyChart.getData().add(series);
    }

    private void renderDailyChart(ChatDashboardCharts charts) {
        dailyChart.getData().clear();
        if (charts == null) {
            return;
        }
        javafx.scene.chart.XYChart.Series<String, Number> series = new javafx.scene.chart.XYChart.Series<>();
        series.setName("Tendance 7 jours");
        dailyPoints.setAll(charts.getDaily());
        for (ChatChartPoint point : dailyPoints) {
            series.getData().add(new javafx.scene.chart.XYChart.Data<>(point.getLabel(), point.getValue()));
        }
        dailyChart.getData().add(series);
    }

    private void renderDonutChart(ChatDashboardCharts charts) {
        donutData.clear();
        if (charts == null || charts.getActivitySplit() == null || charts.getActivitySplit().isEmpty()) {
            donutData.addAll(
                    new PieChart.Data("Actifs", 1),
                    new PieChart.Data("Inactifs", 1)
            );
        } else {
            for (ChatChartPoint point : charts.getActivitySplit()) {
                donutData.add(new PieChart.Data(point.getLabel(), point.getValue()));
            }
        }
        donutChart.setData(donutData);
    }

    private void renderRecentActivity(List<ChatRecentActivity> activities) {
        boxRecentActivity.getChildren().clear();
        List<ChatRecentActivity> safeActivities = activities == null ? List.of() : activities;
        if (safeActivities.isEmpty()) {
            Label empty = new Label("Aucune activité récente.");
            empty.getStyleClass().add("chat-dashboard-empty");
            boxRecentActivity.getChildren().add(empty);
            return;
        }

        for (ChatRecentActivity activity : safeActivities) {
            VBox card = new VBox(4);
            card.getStyleClass().add("chat-dashboard-activity-card");

            Label title = new Label(safe(activity.getTitle()));
            title.getStyleClass().add("chat-dashboard-activity-title");

            Label subtitle = new Label(safe(activity.getSubtitle()));
            subtitle.setWrapText(true);
            subtitle.getStyleClass().add("chat-dashboard-activity-subtitle");

            HBox footer = new HBox(8);
            footer.setAlignment(Pos.CENTER_LEFT);
            Label timestamp = new Label(safe(activity.getTimestamp()));
            timestamp.getStyleClass().add("chat-dashboard-activity-timestamp");
            Label badge = new Label(normalizeActivityType(activity.getType()));
            badge.getStyleClass().addAll("chat-dashboard-mini-badge", "chat-dashboard-mini-badge-" + normalizeActivityType(activity.getType()).toLowerCase(Locale.ROOT));
            footer.getChildren().addAll(timestamp, badge);

            card.getChildren().addAll(title, subtitle, footer);
            boxRecentActivity.getChildren().add(card);
        }
    }

    private void renderTopUsers(List<ChatTopUser> users) {
        topUsers.setAll(users == null ? List.of() : users);
    }

    private void updatePeriodSummary(ChatDashboardPeriod period) {
        lblPeriodSummary.setText("Periode active: " + period.getLabel());
        btnPeriodToday.getStyleClass().remove("selected");
        btnPeriod7Days.getStyleClass().remove("selected");
        btnPeriod30Days.getStyleClass().remove("selected");
        if (period == ChatDashboardPeriod.TODAY) {
            btnPeriodToday.getStyleClass().add("selected");
        } else if (period == ChatDashboardPeriod.DAYS_7) {
            btnPeriod7Days.getStyleClass().add("selected");
        } else if (period == ChatDashboardPeriod.DAYS_30) {
            btnPeriod30Days.getStyleClass().add("selected");
        }
    }

    private void setLiveBadge(String text) {
        lblLiveBadge.setText(text);
    }

    private void updateFocusLabel(String userName) {
        if (lblFocusedUser == null) {
            return;
        }
        lblFocusedUser.setText(userName == null || userName.isBlank() ? "Vue globale" : "Utilisateur: " + userName);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String format(long value) {
        return INTEGER_FMT.format(value);
    }

    private String normalizeActivityType(String type) {
        if (type == null || type.isBlank()) {
            return "Info";
        }
        return switch (type.trim().toLowerCase(Locale.ROOT)) {
            case "messages" -> "Messages";
            case "peak" -> "Pic";
            case "users" -> "Users";
            case "recent" -> "Recent";
            default -> "Info";
        };
    }

    private record DashboardLoadResult(ChatDashboardSnapshot globalSnapshot, ChatDashboardSnapshot focusedSnapshot) {
    }
}
