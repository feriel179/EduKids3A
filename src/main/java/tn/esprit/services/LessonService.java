package tn.esprit.services;

import com.edukids.utils.MyConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tn.esprit.models.Course;
import tn.esprit.models.Lesson;

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
    private final StudentService studentService = new StudentService();

    public ObservableList<Lesson> getLessonsByCourse(Course course) {
        return loadLessonsByCourse(course, false);
    }

    public ObservableList<Lesson> getPublishedLessonsByCourse(Course course) {
        return loadLessonsByCourse(course, true);
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
                       l.status,
                       l.duration_minutes,
                       c.id AS cours_id,
                       c.titre AS cours_titre,
                       c.description AS cours_description,
                       c.niveau AS cours_niveau,
                       c.matiere AS cours_matiere,
                       c.image AS cours_image,
                       c.likes AS cours_likes,
                       c.dislikes AS cours_dislikes,
                       c.status AS cours_status,
                       c.lesson_count AS cours_lesson_count,
                       c.total_duration_minutes AS cours_total_duration_minutes
                FROM lecon l
                JOIN cours c ON c.id = l.cours_id
                ORDER BY c.id ASC, l.ordre ASC, l.id ASC
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

    public Lesson addLesson(Course course, int order, String title, String pdfUrl, String videoUrl, String youtubeUrl) {
        return addLesson(course, order, title, pdfUrl, videoUrl, youtubeUrl, "DRAFT", 10);
    }

    public Lesson addLesson(Course course, int order, String title,
                            String pdfUrl, String videoUrl, String youtubeUrl,
                            String status, int durationMinutes) {
        Lesson lesson = new Lesson(
                0,
                course,
                order,
                title,
                deriveMediaType(pdfUrl, videoUrl, youtubeUrl),
                clean(pdfUrl),
                clean(videoUrl),
                clean(youtubeUrl),
                null,
                status,
                durationMinutes
        );
        return add(lesson);
    }

    public void updateLesson(Lesson lesson, Course targetCourse, int order, String title,
                             String pdfUrl, String videoUrl, String youtubeUrl,
                             String status, int durationMinutes) {
        Lesson updatedLesson = new Lesson(
                lesson.getId(),
                targetCourse,
                order,
                title,
                deriveMediaType(pdfUrl, videoUrl, youtubeUrl),
                clean(pdfUrl),
                clean(videoUrl),
                clean(youtubeUrl),
                lesson.getImage(),
                status,
                durationMinutes
        );
        update(lesson, updatedLesson);
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

    public int countPublishedLessons() {
        try (Statement statement = cnx.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM lecon WHERE status = 'PUBLISHED'")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de lire les lecons publiees EduKids.", exception);
        }
    }

    public int getTotalLessonDurationMinutes() {
        try (Statement statement = cnx.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COALESCE(SUM(duration_minutes), 0) FROM lecon")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de lire la duree totale des lecons EduKids.", exception);
        }
    }

    public int getNextOrderForCourse(Course course) {
        if (course == null) {
            return 1;
        }
        return countLessonsInCourse(course.getId()) + 1;
    }

    public Lesson add(Lesson lesson) {
        validateLessonForSave(lesson);

        try {
            cnx.setAutoCommit(false);
            long courseId = lesson.getCourse().getId();
            assertCourseAllowsLessonChanges(courseId);

            int finalOrder = clamp(lesson.getOrder(), 1, countLessonsInCourse(courseId) + 1);
            shiftOrdersUp(courseId, finalOrder);
            lesson.setOrder(finalOrder);

            long generatedId = insertLessonRecord(lesson);
            refreshCourseStats(courseId);
            studentService.refreshProgressForCourse(courseId);
            ensurePublishedCourseHasPublishedLesson(courseId);
            cnx.commit();

            Lesson createdLesson = new Lesson(
                    generatedId,
                    lesson.getCourse(),
                    lesson.getOrder(),
                    lesson.getTitle(),
                    lesson.getMediaType(),
                    lesson.getMediaUrl(),
                    lesson.getVideoUrl(),
                    lesson.getYoutubeUrl(),
                    lesson.getImage(),
                    lesson.getStatus(),
                    lesson.getDurationMinutes()
            );
            lesson.getCourse().setLessonCount(lesson.getCourse().getLessonCount() + 1);
            return createdLesson;
        } catch (SQLException exception) {
            rollbackQuietly();
            throw new IllegalStateException("Impossible d'ajouter la lecon dans EduKids.", exception);
        } finally {
            resetAutoCommit();
        }
    }

    public void update(Lesson existingLesson, Lesson updatedLesson) {
        validateLessonForSave(updatedLesson);

        long previousCourseId = existingLesson.getCourse() == null ? 0 : existingLesson.getCourse().getId();
        long targetCourseId = updatedLesson.getCourse() == null ? 0 : updatedLesson.getCourse().getId();
        int previousOrder = existingLesson.getOrder();

        try {
            cnx.setAutoCommit(false);
            assertCourseAllowsLessonChanges(targetCourseId);

            if (previousCourseId == targetCourseId) {
                int maxOrder = Math.max(1, countLessonsInCourse(targetCourseId));
                int finalOrder = clamp(updatedLesson.getOrder(), 1, maxOrder);
                moveLessonWithinCourse(targetCourseId, existingLesson.getId(), previousOrder, finalOrder);
                updatedLesson.setOrder(finalOrder);
            } else {
                closeOrderGap(previousCourseId, previousOrder);
                int finalOrder = clamp(updatedLesson.getOrder(), 1, countLessonsInCourse(targetCourseId) + 1);
                shiftOrdersUp(targetCourseId, finalOrder);
                updatedLesson.setOrder(finalOrder);
            }

            updateLessonRecord(updatedLesson);
            refreshCourseStats(previousCourseId);
            studentService.refreshProgressForCourse(previousCourseId);
            if (targetCourseId != previousCourseId) {
                refreshCourseStats(targetCourseId);
                studentService.refreshProgressForCourse(targetCourseId);
            }
            ensurePublishedCourseHasPublishedLesson(previousCourseId);
            if (targetCourseId != previousCourseId) {
                ensurePublishedCourseHasPublishedLesson(targetCourseId);
            }
            cnx.commit();

            existingLesson.setCourse(updatedLesson.getCourse());
            existingLesson.setOrder(updatedLesson.getOrder());
            existingLesson.setTitle(updatedLesson.getTitle());
            existingLesson.setMediaType(updatedLesson.getMediaType());
            existingLesson.setMediaUrl(updatedLesson.getMediaUrl());
            existingLesson.setVideoUrl(updatedLesson.getVideoUrl());
            existingLesson.setYoutubeUrl(updatedLesson.getYoutubeUrl());
            existingLesson.setStatus(updatedLesson.getStatus());
            existingLesson.setDurationMinutes(updatedLesson.getDurationMinutes());
        } catch (SQLException exception) {
            rollbackQuietly();
            throw new IllegalStateException("Impossible de mettre a jour la lecon EduKids.", exception);
        } finally {
            resetAutoCommit();
        }
    }

    public void delete(Lesson lesson) {
        long courseId = lesson.getCourse() == null ? 0 : lesson.getCourse().getId();
        int order = lesson.getOrder();

        try {
            cnx.setAutoCommit(false);

            try (PreparedStatement deleteLessonProgress = cnx.prepareStatement("DELETE FROM user_lecon_progress WHERE lesson_id = ?");
                 PreparedStatement deleteExerciseWork = cnx.prepareStatement("""
                        DELETE sew
                        FROM student_exercise_work sew
                        INNER JOIN lesson_exercise le ON le.id = sew.exercise_id
                        WHERE le.lesson_id = ?
                        """);
                 PreparedStatement deleteExercises = cnx.prepareStatement("DELETE FROM lesson_exercise WHERE lesson_id = ?");
                 PreparedStatement preparedStatement = cnx.prepareStatement("DELETE FROM lecon WHERE id = ?")) {
                deleteLessonProgress.setLong(1, lesson.getId());
                deleteLessonProgress.executeUpdate();

                deleteExerciseWork.setLong(1, lesson.getId());
                deleteExerciseWork.executeUpdate();

                deleteExercises.setLong(1, lesson.getId());
                deleteExercises.executeUpdate();

                preparedStatement.setLong(1, lesson.getId());
                preparedStatement.executeUpdate();
            }

            closeOrderGap(courseId, order);
            refreshCourseStats(courseId);
            studentService.refreshProgressForCourse(courseId);
            ensurePublishedCourseHasPublishedLesson(courseId);
            cnx.commit();
        } catch (SQLException exception) {
            rollbackQuietly();
            throw new IllegalStateException("Impossible de supprimer la lecon EduKids.", exception);
        } finally {
            resetAutoCommit();
        }
    }

    private ObservableList<Lesson> loadLessonsByCourse(Course course, boolean publishedOnly) {
        ObservableList<Lesson> lessons = FXCollections.observableArrayList();
        String sql = """
                SELECT id, titre, ordre, media_type, media_url, video_url, youtube_url, image, status, duration_minutes
                FROM lecon
                WHERE cours_id = ?
                """ + (publishedOnly ? " AND status = 'PUBLISHED'" : "") + """
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

    private void validateLessonForSave(Lesson lesson) {
        if (lesson.getCourse() == null) {
            throw new IllegalArgumentException("A lesson must belong to a course.");
        }
        if (lesson.getDurationMinutes() <= 0) {
            throw new IllegalArgumentException("Lesson duration must be greater than 0 minute.");
        }
    }

    private void assertCourseAllowsLessonChanges(long courseId) throws SQLException {
        String sql = "SELECT status FROM cours WHERE id = ?";
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, courseId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new IllegalArgumentException("The selected course does not exist anymore.");
                }

                String status = resultSet.getString("status");
                if ("ARCHIVED".equalsIgnoreCase(status)) {
                    throw new IllegalArgumentException("Archived courses cannot receive new lesson changes.");
                }
            }
        }
    }

    private int countLessonsInCourse(long courseId) {
        String sql = "SELECT COUNT(*) FROM lecon WHERE cours_id = ?";
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, courseId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de compter les lecons du cours.", exception);
        }
    }

    private long insertLessonRecord(Lesson lesson) throws SQLException {
        String sql = """
                INSERT INTO lecon (titre, ordre, media_type, media_url, cours_id, video_url, youtube_url, image, status, duration_minutes)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            preparedStatement.setString(9, lesson.getStatus());
            preparedStatement.setInt(10, lesson.getDurationMinutes());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                return generatedKeys.next() ? generatedKeys.getLong(1) : 0;
            }
        }
    }

    private void updateLessonRecord(Lesson lesson) throws SQLException {
        String sql = """
                UPDATE lecon
                SET titre = ?, ordre = ?, media_type = ?, media_url = ?, cours_id = ?, video_url = ?, youtube_url = ?, image = ?, status = ?, duration_minutes = ?
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
            preparedStatement.setString(9, lesson.getStatus());
            preparedStatement.setInt(10, lesson.getDurationMinutes());
            preparedStatement.setLong(11, lesson.getId());
            preparedStatement.executeUpdate();
        }
    }

    private void shiftOrdersUp(long courseId, int startingOrder) throws SQLException {
        String sql = "UPDATE lecon SET ordre = ordre + 1 WHERE cours_id = ? AND ordre >= ?";
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, courseId);
            preparedStatement.setInt(2, startingOrder);
            preparedStatement.executeUpdate();
        }
    }

    private void closeOrderGap(long courseId, int removedOrder) throws SQLException {
        String sql = "UPDATE lecon SET ordre = ordre - 1 WHERE cours_id = ? AND ordre > ?";
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, courseId);
            preparedStatement.setInt(2, removedOrder);
            preparedStatement.executeUpdate();
        }
    }

    private void moveLessonWithinCourse(long courseId, long lessonId, int oldOrder, int newOrder) throws SQLException {
        if (newOrder == oldOrder) {
            return;
        }

        String sql;
        if (newOrder > oldOrder) {
            sql = """
                    UPDATE lecon
                    SET ordre = ordre - 1
                    WHERE cours_id = ? AND id <> ? AND ordre > ? AND ordre <= ?
                    """;
        } else {
            sql = """
                    UPDATE lecon
                    SET ordre = ordre + 1
                    WHERE cours_id = ? AND id <> ? AND ordre >= ? AND ordre < ?
                    """;
        }

        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, courseId);
            preparedStatement.setLong(2, lessonId);
            preparedStatement.setInt(3, Math.min(oldOrder, newOrder));
            preparedStatement.setInt(4, Math.max(oldOrder, newOrder));
            preparedStatement.executeUpdate();
        }
    }

    private void refreshCourseStats(long courseId) {
        String sql = """
                UPDATE cours c
                SET lesson_count = (
                        SELECT COUNT(*)
                        FROM lecon l
                        WHERE l.cours_id = c.id
                    ),
                    total_duration_minutes = (
                        SELECT COALESCE(SUM(l.duration_minutes), 0)
                        FROM lecon l
                        WHERE l.cours_id = c.id
                    )
                WHERE c.id = ?
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, courseId);
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            System.err.println("Impossible de recalculer les statistiques du cours " + courseId + ": " + exception.getMessage());
        }
    }

    private void ensurePublishedCourseHasPublishedLesson(long courseId) throws SQLException {
        if (courseId <= 0) {
            return;
        }

        String sql = """
                UPDATE cours
                SET status = 'DRAFT'
                WHERE id = ?
                  AND status = 'PUBLISHED'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM lecon
                      WHERE cours_id = ? AND status = 'PUBLISHED'
                  )
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, courseId);
            preparedStatement.setLong(2, courseId);
            preparedStatement.executeUpdate();
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
                resultSet.getString("image"),
                resultSet.getString("status"),
                resultSet.getInt("duration_minutes")
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
                resultSet.getInt("cours_dislikes"),
                resultSet.getString("cours_status"),
                resultSet.getInt("cours_lesson_count"),
                resultSet.getInt("cours_total_duration_minutes")
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

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private String normalizeMediaType(String mediaType) {
        if (mediaType == null || mediaType.isBlank()) {
            return "PDF";
        }
        return mediaType.trim().toUpperCase(Locale.ROOT);
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void rollbackQuietly() {
        try {
            cnx.rollback();
        } catch (SQLException ignored) {
        }
    }

    private void resetAutoCommit() {
        try {
            cnx.setAutoCommit(true);
        } catch (SQLException ignored) {
        }
    }
}
