package tn.esprit.services;

import tn.esprit.models.Course;
import tn.esprit.models.Lesson;
import tn.esprit.util.MyConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LessonService {
    private final Connection cnx = MyConnection.getInstance().getCnx();

    public ObservableList<Lesson> getLessonsByCourse(Course course) {
        ObservableList<Lesson> lessons = FXCollections.observableArrayList();
        String sql = """
                SELECT id, titre, ordre, media_type, media_url, video_url, youtube_url, image
                FROM lecon
                WHERE cours_id = ?
                ORDER BY ordre ASC, id ASC
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, course.getId());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    lessons.add(mapLesson(resultSet, course));
                }
            }
            return lessons;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de charger les lecons EduKids.", exception);
        }
    }

    public ObservableList<Lesson> getAllLessons() {
        ObservableList<Lesson> lessons = FXCollections.observableArrayList();
        String sql = """
                SELECT l.id,
                       l.titre,
                       l.ordre,
                       l.media_type,
                       l.media_url,
                       l.video_url,
                       l.youtube_url,
                       l.image,
                       c.id AS cours_id,
                       c.titre AS cours_titre,
                       c.description AS cours_description,
                       c.niveau AS cours_niveau,
                       c.matiere AS cours_matiere,
                       c.image AS cours_image,
                       c.likes AS cours_likes,
                       c.dislikes AS cours_dislikes
                FROM lecon l
                JOIN cours c ON c.id = l.cours_id
                ORDER BY l.ordre ASC, l.id ASC
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                lessons.add(mapLesson(resultSet, mapJoinedCourse(resultSet)));
            }
            return lessons;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de charger toutes les lecons EduKids.", exception);
        }
    }

    public Lesson addLesson(Course course, int order, String title,
                            String pdfUrl, String videoUrl, String youtubeUrl) {
        Lesson lesson = new Lesson(
                0,
                course,
                order,
                title,
                deriveMediaType(pdfUrl, videoUrl, youtubeUrl),
                clean(pdfUrl),
                clean(videoUrl),
                clean(youtubeUrl),
                null
        );
        return add(lesson);
    }

    public void updateLesson(Lesson lesson, int order, String title,
                             String pdfUrl, String videoUrl, String youtubeUrl) {
        lesson.setOrder(order);
        lesson.setTitle(title);
        lesson.setMediaType(deriveMediaType(pdfUrl, videoUrl, youtubeUrl));
        lesson.setMediaUrl(clean(pdfUrl));
        lesson.setVideoUrl(clean(videoUrl));
        lesson.setYoutubeUrl(clean(youtubeUrl));
        update(lesson);
    }

    public void deleteLesson(Lesson lesson) {
        delete(lesson);
    }

    public int countLessons() {
        try (Statement statement = cnx.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM lecon")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de lire les lecons EduKids.", exception);
        }
    }

    public Lesson add(Lesson lesson) {
        String sql = """
                INSERT INTO lecon (titre, ordre, media_type, media_url, cours_id, video_url, youtube_url, image)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, lesson.getTitle());
            preparedStatement.setInt(2, lesson.getOrder());
            preparedStatement.setString(3, lesson.getMediaType());
            preparedStatement.setString(4, lesson.getMediaUrl());
            preparedStatement.setLong(5, lesson.getCourse().getId());
            preparedStatement.setString(6, lesson.getVideoUrl());
            preparedStatement.setString(7, lesson.getYoutubeUrl());
            preparedStatement.setString(8, lesson.getImage());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                long generatedId = generatedKeys.next() ? generatedKeys.getLong(1) : 0;
                return new Lesson(
                        generatedId,
                        lesson.getCourse(),
                        lesson.getOrder(),
                        lesson.getTitle(),
                        lesson.getMediaType(),
                        lesson.getMediaUrl(),
                        lesson.getVideoUrl(),
                        lesson.getYoutubeUrl(),
                        lesson.getImage()
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'ajouter la lecon dans EduKids.", exception);
        }
    }

    public void update(Lesson lesson) {
        String sql = """
                UPDATE lecon
                SET titre = ?, ordre = ?, media_type = ?, media_url = ?, cours_id = ?, video_url = ?, youtube_url = ?, image = ?
                WHERE id = ?
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setString(1, lesson.getTitle());
            preparedStatement.setInt(2, lesson.getOrder());
            preparedStatement.setString(3, lesson.getMediaType());
            preparedStatement.setString(4, lesson.getMediaUrl());
            preparedStatement.setLong(5, lesson.getCourse().getId());
            preparedStatement.setString(6, lesson.getVideoUrl());
            preparedStatement.setString(7, lesson.getYoutubeUrl());
            preparedStatement.setString(8, lesson.getImage());
            preparedStatement.setLong(9, lesson.getId());
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de mettre a jour la lecon EduKids.", exception);
        }
    }

    public void delete(Lesson lesson) {
        try (PreparedStatement preparedStatement = cnx.prepareStatement("DELETE FROM lecon WHERE id = ?")) {
            preparedStatement.setLong(1, lesson.getId());
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de supprimer la lecon EduKids.", exception);
        }
    }

    private Lesson mapLesson(ResultSet resultSet, Course course) throws SQLException {
        String rawMediaType = normalizeMediaType(resultSet.getString("media_type"));
        String rawMediaUrl = clean(resultSet.getString("media_url"));
        String rawVideoUrl = clean(resultSet.getString("video_url"));
        String rawYoutubeUrl = clean(resultSet.getString("youtube_url"));

        return new Lesson(
                resultSet.getLong("id"),
                course,
                resultSet.getInt("ordre"),
                resultSet.getString("titre"),
                rawMediaType,
                extractPdfUrl(rawMediaType, rawMediaUrl, rawVideoUrl, rawYoutubeUrl),
                rawVideoUrl,
                rawYoutubeUrl,
                resultSet.getString("image")
        );
    }

    private Course mapJoinedCourse(ResultSet resultSet) throws SQLException {
        return new Course(
                resultSet.getLong("cours_id"),
                resultSet.getString("cours_titre"),
                resultSet.getString("cours_description"),
                resultSet.getInt("cours_niveau"),
                resultSet.getString("cours_matiere"),
                resultSet.getString("cours_image"),
                resultSet.getInt("cours_likes"),
                resultSet.getInt("cours_dislikes")
        );
    }

    private String extractPdfUrl(String mediaType, String mediaUrl, String videoUrl, String youtubeUrl) {
        if (mediaUrl.isBlank()) {
            return "";
        }

        String normalized = normalizeMediaType(mediaType);
        if (normalized.contains("PDF")) {
            return mediaUrl;
        }

        if (!mediaUrl.equals(videoUrl) && !mediaUrl.equals(youtubeUrl)) {
            return mediaUrl;
        }
        return "";
    }

    private String deriveMediaType(String pdfUrl, String videoUrl, String youtubeUrl) {
        List<String> mediaTypes = new ArrayList<>();
        if (!clean(pdfUrl).isBlank()) {
            mediaTypes.add("PDF");
        }
        if (!clean(videoUrl).isBlank()) {
            mediaTypes.add("VIDEO");
        }
        if (!clean(youtubeUrl).isBlank()) {
            mediaTypes.add("YOUTUBE");
        }
        return mediaTypes.isEmpty() ? "MEDIA" : String.join("_", mediaTypes);
    }

    private String normalizeMediaType(String mediaType) {
        if (mediaType == null || mediaType.isBlank()) {
            return "PDF";
        }
        return mediaType.trim().toUpperCase(Locale.ROOT);
    }

    private String nonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
