package tn.esprit.services;

import tn.esprit.interfaces.GlobalInterface;
import tn.esprit.models.Course;
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

    public Course addCourse(String title, String description, int level, String subject) {
        Course course = new Course(0, title, description, level, subject, "course-default.png", 0, 0);
        return add(course);
    }

    public void updateCourse(Course course, String title, String description, int level, String subject) {
        course.setTitle(title);
        course.setDescription(description);
        course.setLevel(level);
        course.setSubject(subject);
        update(course);
    }

    public void deleteCourse(Course course) {
        delete(course);
    }

    public void refreshCourses() {
        courses.setAll(fetchCourses());
    }

    public int countCourses() {
        try (Statement statement = cnx.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM cours")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de lire les cours EduKids.", exception);
        }
    }

    @Override
    public Course add(Course course) {
        String sql = """
                INSERT INTO cours (titre, description, niveau, matiere, image, likes, dislikes)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, course.getTitle());
            preparedStatement.setString(2, course.getDescription());
            preparedStatement.setInt(3, course.getLevel());
            preparedStatement.setString(4, course.getSubject());
            preparedStatement.setString(5, course.getImage());
            preparedStatement.setInt(6, course.getLikes());
            preparedStatement.setInt(7, course.getDislikes());
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
                        course.getDislikes()
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
        String sql = """
                UPDATE cours
                SET titre = ?, description = ?, niveau = ?, matiere = ?, image = ?, likes = ?, dislikes = ?
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
            preparedStatement.setLong(8, course.getId());
            preparedStatement.executeUpdate();
            refreshCourses();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de mettre a jour le cours EduKids.", exception);
        }
    }

    @Override
    public void delete(Course course) {
        try {
            cnx.setAutoCommit(false);

            try (PreparedStatement deleteProgress = cnx.prepareStatement("DELETE FROM user_cours_progress WHERE cours_id = ?");
                 PreparedStatement deleteLessons = cnx.prepareStatement("DELETE FROM lecon WHERE cours_id = ?");
                 PreparedStatement deleteCourse = cnx.prepareStatement("DELETE FROM cours WHERE id = ?")) {
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
            try {
                cnx.rollback();
            } catch (SQLException ignored) {
            }
            throw new IllegalStateException("Impossible de supprimer le cours EduKids.", exception);
        } finally {
            try {
                cnx.setAutoCommit(true);
            } catch (SQLException ignored) {
            }
        }
    }

    private List<Course> fetchCourses() {
        List<Course> results = new ArrayList<>();
        String sql = """
                SELECT id, titre, description, niveau, matiere, image, likes, dislikes
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

    private Course mapCourse(ResultSet resultSet) throws SQLException {
        return new Course(
                resultSet.getLong("id"),
                resultSet.getString("titre"),
                resultSet.getString("description"),
                resultSet.getInt("niveau"),
                resultSet.getString("matiere"),
                resultSet.getString("image"),
                resultSet.getInt("likes"),
                resultSet.getInt("dislikes")
        );
    }
}
