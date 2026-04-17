package tn.esprit.services;

import tn.esprit.models.Course;
import tn.esprit.models.Student;
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

public class StudentService {
    private static Student currentStudent;
    private final Connection cnx = MyConnection.getInstance().getCnx();

    public Student loginOrCreateStudent(String identifier) {
        String normalized = identifier == null ? "" : identifier.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Please enter a student name or email.");
        }

        Student student = normalized.contains("@")
                ? findStudentByEmail(normalized)
                : findStudentByName(normalized);

        if (student == null) {
            student = normalized.contains("@")
                    ? createStudentFromEmail(normalized)
                    : createStudentFromName(normalized);
        }

        student.replaceEnrolledCourses(loadEnrolledCourses(student.getId()));
        currentStudent = student;
        return currentStudent;
    }

    public Student getCurrentStudent() {
        if (currentStudent != null) {
            currentStudent.replaceEnrolledCourses(loadEnrolledCourses(currentStudent.getId()));
        }
        return currentStudent;
    }

    public boolean enrollInCourse(Course course) {
        if (currentStudent == null || course == null) {
            return false;
        }

        if (isAlreadyEnrolled(currentStudent.getId(), course.getId())) {
            return false;
        }

        String sql = "INSERT INTO user_cours_progress (user_id, cours_id, progress) VALUES (?, ?, ?)";
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, currentStudent.getId());
            preparedStatement.setLong(2, course.getId());
            preparedStatement.setInt(3, 0);
            preparedStatement.executeUpdate();
            currentStudent.replaceEnrolledCourses(loadEnrolledCourses(currentStudent.getId()));
            return true;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'inscrire l'etudiant au cours EduKids.", exception);
        }
    }

    public ObservableList<Student> getAllStudents() {
        ObservableList<Student> students = FXCollections.observableArrayList();
        String sql = """
                SELECT id, email, first_name, last_name
                FROM user
                WHERE is_active = 1 AND roles LIKE '%ROLE_ELEVE%'
                ORDER BY first_name, last_name, email
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql);
             ResultSet resultSet = preparedStatement.executeQuery()) {
            while (resultSet.next()) {
                students.add(mapStudent(resultSet));
            }
            return students;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de charger les etudiants EduKids.", exception);
        }
    }

    public int countStudents() {
        try (Statement statement = cnx.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM user WHERE is_active = 1 AND roles LIKE '%ROLE_ELEVE%'")) {
            return resultSet.next() ? resultSet.getInt(1) : 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de lire les etudiants EduKids.", exception);
        }
    }

    private Student findStudentByEmail(String email) {
        String sql = """
                SELECT id, email, first_name, last_name
                FROM user
                WHERE is_active = 1 AND roles LIKE '%ROLE_ELEVE%' AND LOWER(email) = LOWER(?)
                LIMIT 1
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setString(1, email);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() ? mapStudent(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de rechercher l'etudiant par email.", exception);
        }
    }

    private Student findStudentByName(String name) {
        String sql = """
                SELECT id, email, first_name, last_name
                FROM user
                WHERE is_active = 1 AND roles LIKE '%ROLE_ELEVE%'
                  AND (
                    LOWER(TRIM(CONCAT(COALESCE(first_name, ''), ' ', COALESCE(last_name, '')))) = LOWER(?)
                    OR LOWER(COALESCE(first_name, '')) = LOWER(?)
                    OR LOWER(COALESCE(last_name, '')) = LOWER(?)
                  )
                LIMIT 1
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, name);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() ? mapStudent(resultSet) : null;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de rechercher l'etudiant par nom.", exception);
        }
    }

    private Student createStudentFromName(String name) {
        String[] parts = name.trim().split("\\s+", 2);
        String firstName = capitalize(parts[0]);
        String lastName = parts.length > 1 ? capitalize(parts[1]) : "Student";
        String email = buildUniqueEmail(firstName + "." + lastName);
        return createStudent(email, firstName, lastName);
    }

    private Student createStudentFromEmail(String email) {
        String localPart = email.substring(0, email.indexOf('@'));
        String[] parts = localPart.replace('.', ' ').replace('_', ' ').trim().split("\\s+", 2);
        String firstName = parts.length > 0 && !parts[0].isBlank() ? capitalize(parts[0]) : "Edukids";
        String lastName = parts.length > 1 && !parts[1].isBlank() ? capitalize(parts[1]) : "Student";
        return createStudent(email.trim().toLowerCase(Locale.ROOT), firstName, lastName);
    }

    private Student createStudent(String email, String firstName, String lastName) {
        String sql = """
                INSERT INTO user (email, roles, password, first_name, last_name, is_active)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, "[\"ROLE_ELEVE\"]");
            preparedStatement.setString(3, "desktop-generated");
            preparedStatement.setString(4, firstName);
            preparedStatement.setString(5, lastName);
            preparedStatement.setBoolean(6, true);
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                long generatedId = generatedKeys.next() ? generatedKeys.getLong(1) : 0;
                return new Student(generatedId, buildDisplayName(firstName, lastName, email), email);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de creer l'etudiant dans EduKids.", exception);
        }
    }

    private boolean isAlreadyEnrolled(long userId, long courseId) {
        String sql = "SELECT COUNT(*) FROM user_cours_progress WHERE user_id = ? AND cours_id = ?";
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, userId);
            preparedStatement.setLong(2, courseId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de verifier l'inscription au cours.", exception);
        }
    }

    private List<Course> loadEnrolledCourses(long userId) {
        List<Course> courses = new ArrayList<>();
        String sql = """
                SELECT c.id, c.titre, c.description, c.niveau, c.matiere, c.image, c.likes, c.dislikes,
                       c.status, c.lesson_count, c.total_duration_minutes
                FROM user_cours_progress progress
                INNER JOIN cours c ON c.id = progress.cours_id
                WHERE progress.user_id = ?
                ORDER BY c.titre
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(mapCourse(resultSet));
                }
            }
            return courses;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de charger les cours de l'etudiant.", exception);
        }
    }

    private Student mapStudent(ResultSet resultSet) throws SQLException {
        String email = resultSet.getString("email");
        String firstName = resultSet.getString("first_name");
        String lastName = resultSet.getString("last_name");
        return new Student(
                resultSet.getLong("id"),
                buildDisplayName(firstName, lastName, email),
                email
        );
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

    private String buildDisplayName(String firstName, String lastName, String email) {
        String fullName = ((firstName == null ? "" : firstName.trim()) + " " + (lastName == null ? "" : lastName.trim())).trim();
        return fullName.isBlank() ? email : fullName;
    }

    private String buildUniqueEmail(String baseValue) {
        String localPart = slugify(baseValue);
        if (localPart.isBlank()) {
            localPart = "student";
        }

        String candidate = localPart + "@edukids.local";
        int suffix = 1;
        while (emailExists(candidate)) {
            candidate = localPart + suffix + "@edukids.local";
            suffix++;
        }
        return candidate;
    }

    private boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM user WHERE LOWER(email) = LOWER(?)";
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setString(1, email);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de verifier l'email EduKids.", exception);
        }
    }

    private String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", ".")
                .replaceAll("(^\\.|\\.$)", "");
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
