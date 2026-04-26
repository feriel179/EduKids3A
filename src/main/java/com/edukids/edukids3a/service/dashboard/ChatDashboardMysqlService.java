package com.edukids.edukids3a.service.dashboard;

import com.edukids.edukids3a.model.ChatChartPoint;
import com.edukids.edukids3a.model.ChatDashboardCharts;
import com.edukids.edukids3a.model.ChatDashboardPeriod;
import com.edukids.edukids3a.model.ChatDashboardSnapshot;
import com.edukids.edukids3a.model.ChatRecentActivity;
import com.edukids.edukids3a.model.ChatTopUser;
import com.edukids.edukids3a.utils.Myconnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatDashboardMysqlService {

    private static final DateTimeFormatter GENERATED_AT_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withLocale(Locale.FRENCH);

    public ChatDashboardSnapshot fetchDashboard(ChatDashboardPeriod period) {
        return fetchDashboard(period, null);
    }

    public ChatDashboardSnapshot fetchDashboard(ChatDashboardPeriod period, Long focusUserId) {
        ChatDashboardPeriod effectivePeriod = period == null ? ChatDashboardPeriod.TODAY : period;
        try (Connection connection = Myconnection.getInstance().getCnx()) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime periodStart = getPeriodStart(effectivePeriod, now);
            LocalDateTime weekStart = now.minusDays(6).with(LocalTime.MIN);
            LocalDateTime monthStart = now.minusDays(29).with(LocalTime.MIN);

            ChatDashboardSnapshot snapshot = new ChatDashboardSnapshot();
            snapshot.setTimezone(ZoneId.systemDefault().toString());
            snapshot.setGeneratedAt(GENERATED_AT_FMT.format(now));

            snapshot.setTotalMessages(countLong(connection, buildTotalMessagesSql(focusUserId), focusUserId));
            snapshot.setTotalUsers(countLong(connection, "SELECT COUNT(*) FROM `user` WHERE is_active = 1"));
            snapshot.setActiveUsers7(countDistinctUsers(connection, weekStart, now, focusUserId));
            snapshot.setActiveUsers30(countDistinctUsers(connection, monthStart, now, focusUserId));
            snapshot.setMessagesToday(countMessages(connection, now.toLocalDate().atStartOfDay(), now, focusUserId));
            snapshot.setMessagesWeek(countMessages(connection, weekStart, now, focusUserId));

            TopUserResult topUser = findTopUser(connection, weekStart, now, focusUserId);
            snapshot.setTopUserName(topUser.name());
            snapshot.setTopUserMessages(topUser.messages());

            ChatDashboardCharts charts = new ChatDashboardCharts();
            charts.setHourly(loadHourlySeries(connection, effectivePeriod, now, focusUserId));
            charts.setDaily(loadDailySeries(connection, 7, now, focusUserId));
            charts.setActivitySplit(buildActivitySplit(connection, periodStart, now, snapshot, focusUserId));
            snapshot.setCharts(charts);

            List<ChatTopUser> topUsers = loadTopUsers(connection, effectivePeriod, now);
            snapshot.setTopUsers(topUsers);
            snapshot.setRecentActivities(buildRecentActivities(connection, effectivePeriod, now, snapshot, focusUserId));
            return snapshot;
        } catch (SQLException ex) {
            return buildFallbackSnapshot(effectivePeriod, focusUserId);
        }
    }

    private ChatDashboardSnapshot buildFallbackSnapshot(ChatDashboardPeriod period, Long focusUserId) {
        ChatDashboardSnapshot snapshot = new ChatDashboardSnapshot();
        LocalDateTime now = LocalDateTime.now();
        snapshot.setTimezone(ZoneId.systemDefault().toString());
        snapshot.setGeneratedAt(GENERATED_AT_FMT.format(now));
        snapshot.setTotalMessages(0);
        snapshot.setTotalUsers(0);
        snapshot.setActiveUsers7(0);
        snapshot.setActiveUsers30(0);
        snapshot.setMessagesToday(0);
        snapshot.setMessagesWeek(0);
        snapshot.setTopUserName("-");
        snapshot.setTopUserMessages(0);

        ChatDashboardCharts charts = new ChatDashboardCharts();
        charts.setHourly(buildEmptyHourly());
        charts.setDaily(buildEmptyDaily(7));
        if (focusUserId == null) {
            charts.setActivitySplit(List.of(
                    new ChatChartPoint("Actifs", 0),
                    new ChatChartPoint("Inactifs", 0)
            ));
        } else {
            charts.setActivitySplit(List.of(
                    new ChatChartPoint("Messages utilisateur", 0),
                    new ChatChartPoint("Autres messages", 0)
            ));
        }
        snapshot.setCharts(charts);
        snapshot.setTopUsers(List.of());
        snapshot.setRecentActivities(List.of());
        return snapshot;
    }

    private String buildTotalMessagesSql(Long focusUserId) {
        if (focusUserId == null) {
            return "SELECT COUNT(*) FROM message WHERE deleted_at IS NULL";
        }
        return """
                SELECT COUNT(*)
                FROM message
                WHERE deleted_at IS NULL
                  AND sender_id = ?
                """;
    }

    private long countLong(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private long countLong(Connection connection, String sql, Long focusUserId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (focusUserId != null) {
                statement.setLong(1, focusUserId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private long countMessages(Connection connection, LocalDateTime from, LocalDateTime to, Long focusUserId) throws SQLException {
        String sql = """
                SELECT COUNT(*)
                FROM message
                WHERE deleted_at IS NULL
                  AND created_at >= ?
                  AND created_at < ?
                """;
        if (focusUserId != null) {
            sql += "  AND sender_id = ?\n";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(to));
            if (focusUserId != null) {
                statement.setLong(3, focusUserId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private long countDistinctUsers(Connection connection, LocalDateTime from, LocalDateTime to, Long focusUserId) throws SQLException {
        String sql = """
                SELECT COUNT(DISTINCT sender_id)
                FROM message
                WHERE deleted_at IS NULL
                  AND created_at >= ?
                  AND created_at < ?
                """;
        if (focusUserId != null) {
            sql += "  AND sender_id = ?\n";
        }
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(to));
            if (focusUserId != null) {
                statement.setLong(3, focusUserId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private List<ChatChartPoint> loadHourlySeries(Connection connection, ChatDashboardPeriod period, LocalDateTime now, Long focusUserId) throws SQLException {
        LocalDateTime from = getPeriodStart(period, now);
        StringBuilder sql = new StringBuilder("""
                SELECT HOUR(created_at) AS bucket, COUNT(*) AS total
                FROM message
                WHERE deleted_at IS NULL
                  AND created_at >= ?
                  AND created_at < ?
                """);
        if (focusUserId != null) {
            sql.append("  AND sender_id = ?\n");
        }
        sql.append("GROUP BY HOUR(created_at)");

        Map<Integer, Long> values = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(now));
            if (focusUserId != null) {
                statement.setLong(3, focusUserId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    values.put(rs.getInt("bucket"), rs.getLong("total"));
                }
            }
        }

        List<ChatChartPoint> hourly = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            hourly.add(new ChatChartPoint(String.format(Locale.ROOT, "%02d h", hour), values.getOrDefault(hour, 0L)));
        }
        return hourly;
    }

    private List<ChatChartPoint> loadDailySeries(Connection connection, int days, LocalDateTime now, Long focusUserId) throws SQLException {
        LocalDateTime from = now.minusDays(days - 1).with(LocalTime.MIN);
        StringBuilder sql = new StringBuilder("""
                SELECT DATE(created_at) AS day_bucket, COUNT(*) AS total
                FROM message
                WHERE deleted_at IS NULL
                  AND created_at >= ?
                  AND created_at < ?
                """);
        if (focusUserId != null) {
            sql.append("  AND sender_id = ?\n");
        }
        sql.append("GROUP BY DATE(created_at)");

        Map<LocalDate, Long> values = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(now));
            if (focusUserId != null) {
                statement.setLong(3, focusUserId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    values.put(rs.getDate("day_bucket").toLocalDate(), rs.getLong("total"));
                }
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        List<ChatChartPoint> daily = new ArrayList<>();
        for (int i = days - 1; i >= 0; i--) {
            LocalDate day = now.toLocalDate().minusDays(i);
            daily.add(new ChatChartPoint(day.format(fmt), values.getOrDefault(day, 0L)));
        }
        return daily;
    }

    private List<ChatChartPoint> buildActivitySplit(Connection connection,
                                                    LocalDateTime periodStart,
                                                    LocalDateTime now,
                                                    ChatDashboardSnapshot snapshot,
                                                    Long focusUserId) throws SQLException {
        if (focusUserId == null) {
            return List.of(
                    new ChatChartPoint("Actifs", snapshot.getActiveUsers7()),
                    new ChatChartPoint("Inactifs", Math.max(snapshot.getTotalUsers() - snapshot.getActiveUsers7(), 0))
            );
        }

        long userMessages = countMessages(connection, periodStart, now, focusUserId);
        long totalMessages = countMessages(connection, periodStart, now, null);
        return List.of(
                new ChatChartPoint("Messages utilisateur", userMessages),
                new ChatChartPoint("Autres messages", Math.max(totalMessages - userMessages, 0))
        );
    }

    private TopUserResult findTopUser(Connection connection, LocalDateTime from, LocalDateTime to, Long focusUserId) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(NULLIF(TRIM(CONCAT_WS(' ', u.first_name, u.last_name)), ''), u.email) AS display_name,
                       COUNT(*) AS total_messages
                FROM message m
                JOIN `user` u ON u.id = m.sender_id
                WHERE m.deleted_at IS NULL
                  AND m.created_at >= ?
                  AND m.created_at < ?
                """);
        if (focusUserId != null) {
            sql.append("  AND m.sender_id = ?\n");
        }
        sql.append("""
                GROUP BY u.id, u.first_name, u.last_name, u.email
                ORDER BY total_messages DESC, display_name ASC
                LIMIT 1
                """);

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(to));
            if (focusUserId != null) {
                statement.setLong(3, focusUserId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return new TopUserResult(rs.getString("display_name"), rs.getLong("total_messages"));
                }
            }
        }
        return new TopUserResult("-", 0);
    }

    private List<ChatTopUser> loadTopUsers(Connection connection, ChatDashboardPeriod period, LocalDateTime now) throws SQLException {
        LocalDateTime from = getPeriodStart(period, now);
        String sql = """
                SELECT u.id,
                       COALESCE(NULLIF(TRIM(CONCAT_WS(' ', u.first_name, u.last_name)), ''), u.email) AS display_name,
                       u.email,
                       COUNT(*) AS total_messages,
                       MAX(m.created_at) AS last_activity
                FROM message m
                JOIN `user` u ON u.id = m.sender_id
                WHERE m.deleted_at IS NULL
                  AND m.created_at >= ?
                  AND m.created_at < ?
                GROUP BY u.id, u.first_name, u.last_name, u.email
                ORDER BY total_messages DESC, last_activity DESC, display_name ASC
                LIMIT 10
                """;

        List<Row> rows = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(now));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    rows.add(new Row(
                            rs.getLong("id"),
                            rs.getString("display_name"),
                            rs.getString("email"),
                            rs.getLong("total_messages"),
                            rs.getTimestamp("last_activity") == null ? null : rs.getTimestamp("last_activity").toLocalDateTime()
                    ));
                }
            }
        }

        long max = rows.stream().mapToLong(Row::messages).max().orElse(1L);
        List<ChatTopUser> result = new ArrayList<>();
        int rank = 1;
        for (Row row : rows) {
            double progress = max <= 0 ? 0 : (row.messages() * 100.0 / max);
            result.add(new ChatTopUser(
                    row.id(),
                    rank++,
                    row.name(),
                    row.email(),
                    (int) row.messages(),
                    progress,
                    row.lastActivity() == null ? "-" : GENERATED_AT_FMT.format(row.lastActivity())
            ));
        }
        return result;
    }

    private List<ChatRecentActivity> buildRecentActivities(Connection connection,
                                                           ChatDashboardPeriod period,
                                                           LocalDateTime now,
                                                           ChatDashboardSnapshot snapshot,
                                                           Long focusUserId) throws SQLException {
        List<ChatRecentActivity> activities = new ArrayList<>();
        LocalDateTime last30Minutes = now.minusMinutes(30);
        long recentMessages = countMessages(connection, last30Minutes, now, focusUserId);

        if (focusUserId == null) {
            activities.add(new ChatRecentActivity(
                    "Nouveaux messages detectes",
                    recentMessages + " message(s) sur les 30 dernieres minutes",
                    "A l'instant",
                    "messages"
            ));

            PeakResult peak = findPeakHour(connection, period, now, null);
            activities.add(new ChatRecentActivity(
                    "Pic d'activite",
                    peak.label() + " avec " + peak.count() + " message(s)",
                    "Periode " + period.getLabel(),
                    "peak"
            ));

            activities.add(new ChatRecentActivity(
                    "Activite des utilisateurs",
                    snapshot.getActiveUsers7() + " utilisateur(s) actifs sur 7 jours",
                    "Vue globale",
                    "users"
            ));

            activities.add(new ChatRecentActivity(
                    "Derniers utilisateurs actifs",
                    "Mise a jour recemment",
                    snapshot.getTopUserName(),
                    "recent"
            ));
            return activities;
        }

        activities.add(new ChatRecentActivity(
                "Messages de l'utilisateur",
                recentMessages + " message(s) sur les 30 dernieres minutes",
                "A l'instant",
                "messages"
        ));

        PeakResult peak = findPeakHour(connection, period, now, focusUserId);
        activities.add(new ChatRecentActivity(
                "Pic d'activite",
                peak.label() + " avec " + peak.count() + " message(s)",
                "Periode " + period.getLabel(),
                "peak"
        ));

        long todayMessages = countMessages(connection, now.toLocalDate().atStartOfDay(), now, focusUserId);
        long weekMessages = countMessages(connection, now.minusDays(6).with(LocalTime.MIN), now, focusUserId);
        activities.add(new ChatRecentActivity(
                "Activite utilisateur",
                todayMessages + " message(s) aujourd'hui, " + weekMessages + " sur 7 jours",
                "Vue utilisateur",
                "users"
        ));

        activities.add(new ChatRecentActivity(
                "Utilisateur selectionne",
                snapshot.getTopUserName(),
                findLastActivityForUser(connection, focusUserId),
                "recent"
        ));

        return activities;
    }

    private String findLastActivityForUser(Connection connection, Long focusUserId) throws SQLException {
        String sql = """
                SELECT MAX(created_at)
                FROM message
                WHERE deleted_at IS NULL
                  AND sender_id = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, focusUserId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next() && rs.getTimestamp(1) != null) {
                    return GENERATED_AT_FMT.format(rs.getTimestamp(1).toLocalDateTime());
                }
            }
        }
        return "Aucune activite";
    }

    private PeakResult findPeakHour(Connection connection, ChatDashboardPeriod period, LocalDateTime now, Long focusUserId) throws SQLException {
        LocalDateTime from = getPeriodStart(period, now);
        StringBuilder sql = new StringBuilder("""
                SELECT HOUR(created_at) AS bucket, COUNT(*) AS total
                FROM message
                WHERE deleted_at IS NULL
                  AND created_at >= ?
                  AND created_at < ?
                """);
        if (focusUserId != null) {
            sql.append("  AND sender_id = ?\n");
        }
        sql.append("""
                GROUP BY HOUR(created_at)
                ORDER BY total DESC, bucket ASC
                LIMIT 1
                """);

        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(now));
            if (focusUserId != null) {
                statement.setLong(3, focusUserId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    int hour = rs.getInt("bucket");
                    long count = rs.getLong("total");
                    return new PeakResult(String.format(Locale.ROOT, "%02d h", hour), count);
                }
            }
        }
        return new PeakResult("00 h", 0);
    }

    private List<ChatChartPoint> buildEmptyHourly() {
        List<ChatChartPoint> points = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            points.add(new ChatChartPoint(String.format(Locale.ROOT, "%02d h", hour), 0));
        }
        return points;
    }

    private List<ChatChartPoint> buildEmptyDaily(int days) {
        List<ChatChartPoint> points = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        for (int i = days - 1; i >= 0; i--) {
            points.add(new ChatChartPoint(LocalDate.now().minusDays(i).format(fmt), 0));
        }
        return points;
    }

    private LocalDateTime getPeriodStart(ChatDashboardPeriod period, LocalDateTime now) {
        return switch (period) {
            case TODAY -> now.toLocalDate().atStartOfDay();
            case DAYS_7 -> now.minusDays(6).with(LocalTime.MIN);
            case DAYS_30 -> now.minusDays(29).with(LocalTime.MIN);
        };
    }

    private record Row(long id, String name, String email, long messages, LocalDateTime lastActivity) {
    }

    private record TopUserResult(String name, long messages) {
    }

    private record PeakResult(String label, long count) {
    }
}
