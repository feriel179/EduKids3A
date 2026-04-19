package tn.esprit.services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tn.esprit.interfaces.GlobalInterface;
import tn.esprit.models.Course;
import tn.esprit.util.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class CourseService implements GlobalInterface<Course> {
    private final ObservableList<Course> courses = FXCollections.observableArrayList();
    private final Connection cnx = MyConnection.getInstance().getCnx();

    @Override
    public ObservableList<Course> getAll() {
        return getAllCourses();
    }

    public ObservableList<Course> getAllCourses() {
        if (courses.isEmpty()) {
            refreshCourses();
        }
        return courses;
    }

    public ObservableList<Course> getPublishedCourses() {
        return FXCollections.observableArrayList(
                getAllCourses().stream()
                        .filter(Course::isPublished)
                        .toList()
        );
    }

    public Course addCourse(String title, String description, int level, String subject) {
        return addCourse(title, description, level, subject, "DRAFT");
    }

    public Course addCourse(String title, String description, int level, String subject, String status) {
        return addCourse(title, description, level, subject, status, "course-default.png");
    }

    public Course addCourse(String title, String description, int level, String subject, String status, String image) {
        Course course = new Course(0, title, description, level, subject, normalizeImage(image), 0, 0, status, 0, 0);
        return add(course);
    }

    public void updateCourse(Course course, String title, String description, int level, String subject) {
        updateCourse(course, title, description, level, subject, course.getStatus());
    }

    public void updateCourse(Course course, String title, String description, int level, String subject, String status) {
        updateCourse(course, title, description, level, subject, status, course.getImage());
    }

    public void updateCourse(Course course, String title, String description, int level, String subject, String status, String image) {
        course.setTitle(title);
        course.setDescription(description);
        course.setLevel(level);
        course.setSubject(subject);
        course.setStatus(status);
        course.setImage(normalizeImage(image));
        update(course);
    }

    public void deleteCourse(Course course) {
        delete(course);
    }

    public Course findById(long courseId) {
        String sql = """
                SELECT id, titre, description, niveau, matiere, image, likes, dislikes, status, lesson_count, total_duration_minutes
                FROM cours
                WHERE id = ?
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, courseId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() ? mapCourse(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de charger le cours EduKids.", exception);
        }
    }

    public void refreshCourses() {
        refreshAllCourseStats();
        courses.setAll(fetchCourses());
    }

    public void refreshCourseStats(long courseId) {
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
            refreshCoursesCacheItem(courseId);
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de recalculer les statistiques du cours.", exception);
        }
    }

    public void refreshAllCourseStats() {
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
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de recalculer les statistiques des cours.", exception);
        }
    }

    public int countCourses() {
        try (Statement statement = cnx.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM cours")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de lire les cours EduKids.", exception);
        }
    }

    public int countPublishedCourses() {
        try (Statement statement = cnx.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM cours WHERE status = 'PUBLISHED'")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de lire les cours publies EduKids.", exception);
        }
    }

    @Override
    public Course add(Course course) {
        validateCourseBeforeSave(course, true);

        String sql = """
                INSERT INTO cours (titre, description, niveau, matiere, image, likes, dislikes, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, course.getTitle());
            preparedStatement.setString(2, course.getDescription());
            preparedStatement.setInt(3, course.getLevel());
            preparedStatement.setString(4, course.getSubject());
            preparedStatement.setString(5, course.getImage());
            preparedStatement.setInt(6, course.getLikes());
            preparedStatement.setInt(7, course.getDislikes());
            preparedStatement.setString(8, course.getStatus());
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                long generatedId = generatedKeys.next() ? generatedKeys.getLong(1) : 0;
                Course createdCourse = new Course(
                        generatedId,
                        course.getTitle(),
                        course.getDescription(),
                        course.getLevel(),
                        course.getSubject(),
                        course.getImage(),
                        course.getLikes(),
                        course.getDislikes(),
                        course.getStatus(),
                        0,
                        0
                );
                refreshCourses();
                return createdCourse;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'ajouter le cours dans EduKids.", exception);
        }
    }

    @Override
    public void update(Course course) {
        validateCourseBeforeSave(course, false);

        String sql = """
                UPDATE cours
                SET titre = ?, description = ?, niveau = ?, matiere = ?, image = ?, likes = ?, dislikes = ?, status = ?
                WHERE id = ?
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setString(1, course.getTitle());
            preparedStatement.setString(2, course.getDescription());
            preparedStatement.setInt(3, course.getLevel());
            preparedStatement.setString(4, course.getSubject());
            preparedStatement.setString(5, course.getImage());
            preparedStatement.setInt(6, course.getLikes());
            preparedStatement.setInt(7, course.getDislikes());
            preparedStatement.setString(8, course.getStatus());
            preparedStatement.setLong(9, course.getId());
            preparedStatement.executeUpdate();
            refreshCourseStats(course.getId());
            refreshCourses();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de mettre a jour le cours EduKids.", exception);
        }
    }

    @Override
    public void delete(Course course) {
        try {
            cnx.setAutoCommit(false);

            try (PreparedStatement deleteLessonProgress = cnx.prepareStatement("""
                        DELETE ulp
                        FROM user_lecon_progress ulp
                        INNER JOIN lecon l ON l.id = ulp.lesson_id
                        WHERE l.cours_id = ?
                        """);
                 PreparedStatement deleteProgress = cnx.prepareStatement("DELETE FROM user_cours_progress WHERE cours_id = ?");
                 PreparedStatement deleteLessons = cnx.prepareStatement("DELETE FROM lecon WHERE cours_id = ?");
                 PreparedStatement deleteCourse = cnx.prepareStatement("DELETE FROM cours WHERE id = ?")) {
                deleteLessonProgress.setLong(1, course.getId());
                deleteLessonProgress.executeUpdate();

                deleteProgress.setLong(1, course.getId());
                deleteProgress.executeUpdate();

                deleteLessons.setLong(1, course.getId());
                deleteLessons.executeUpdate();

                deleteCourse.setLong(1, course.getId());
                deleteCourse.executeUpdate();
            }

            cnx.commit();
            refreshCourses();
        } catch (SQLException exception) {
            rollbackQuietly();
            throw new IllegalStateException("Impossible de supprimer le cours EduKids.", exception);
        } finally {
            resetAutoCommit();
        }
    }

    private void validateCourseBeforeSave(Course course, boolean creating) {
        if (course.getLikes() < 0 || course.getDislikes() < 0) {
            throw new IllegalArgumentException("Likes and dislikes cannot be negative.");
        }

        if (creating && course.isPublished()) {
            throw new IllegalArgumentException("Create the course as Draft first, then add and publish at least one lesson before publishing the course.");
        }

        if (!creating && course.isPublished() && countPublishedLessons(course.getId()) == 0) {
            throw new IllegalArgumentException("A course can be published only after at least one lesson in that course is published.");
        }
    }

    private int countPublishedLessons(long courseId) {
        String sql = "SELECT COUNT(*) FROM lecon WHERE cours_id = ? AND status = 'PUBLISHED'";
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, courseId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt(1) : 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de verifier les lecons publiees du cours.", exception);
        }
    }

    private List<Course> fetchCourses() {
        List<Course> results = new ArrayList<>();
        String sql = """
                SELECT id, titre, description, niveau, matiere, image, likes, dislikes, status, lesson_count, total_duration_minutes
                FROM cours
                ORDER BY id DESC
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                results.add(mapCourse(resultSet));
            }
            return results;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de charger les cours EduKids.", exception);
        }
    }

    private void refreshCoursesCacheItem(long courseId) {
        for (int index = 0; index < courses.size(); index++) {
            Course course = courses.get(index);
            if (course.getId() == courseId) {
                Course refreshedCourse = findById(courseId);
                if (refreshedCourse != null) {
                    courses.set(index, refreshedCourse);
                }
                return;
            }
        }
    }

    private Course mapCourse(ResultSet resultSet) throws SQLException {
        return new Course(
                resultSet.getLong("id"),
                resultSet.getString("titre"),
                resultSet.getString("description"),
                resultSet.getInt("niveau"),
                resultSet.getString("matiere"),
                resultSet.getString("image"),
                resultSet.getInt("likes"),
                resultSet.getInt("dislikes"),
                resultSet.getString("status"),
                resultSet.getInt("lesson_count"),
                resultSet.getInt("total_duration_minutes")
        );
    }

    private String normalizeImage(String image) {
        return image == null || image.isBlank() ? "course-default.png" : image.trim();
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
