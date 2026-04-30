package tn.esprit.services;

import tn.esprit.models.CourseProgressSummary;
import tn.esprit.models.Course;
import tn.esprit.models.Lesson;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StudentService {
    private static Student currentStudent;
    private final Connection cnx = MyConnection.getInstance().getCnx();

    public static void clearCurrentStudent() {
        currentStudent = null;
    }

    public Student loginOrCreateStudent(String identifier) {
        return loginOrCreateStudent(identifier, null, null);
    }

    public Student loginOrCreateStudent(String identifier, Integer age, String preferredCategory) {
        String normalized = identifier == null ? "" : identifier.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Please enter a student name or email.");
        }

        validateStudentAge(age);
        String normalizedCategory = normalizePreferredCategory(preferredCategory);

        Student student = normalized.contains("@")
                ? findStudentByEmail(normalized)
                : findStudentByName(normalized);

        if (student == null) {
            student = normalized.contains("@")
                    ? createStudentFromEmail(normalized, age, normalizedCategory)
                    : createStudentFromName(normalized, age, normalizedCategory);
        } else if (age != null || preferredCategory != null) {
            updateStudentProfile(student, age, normalizedCategory);
        }

        student.replaceEnrolledCourses(loadEnrolledCourses(student.getId()));
        currentStudent = student;
        return currentStudent;
    }

    public Student getCurrentStudent() {
        if (currentStudent != null) {
            refreshCurrentStudentCourses();
        }
        return currentStudent;
    }

    public Student updateCurrentStudentProfile(Integer age, String preferredCategory) {
        if (currentStudent == null) {
            throw new IllegalStateException("No student is currently connected.");
        }

        validateStudentAge(age);
        String normalizedCategory = normalizePreferredCategory(preferredCategory);
        updateStudentProfile(currentStudent, age, normalizedCategory);
        refreshCurrentStudentCourses();
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
            refreshCurrentStudentCourses();
            return true;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'inscrire l'etudiant au cours EduKids.", exception);
        }
    }

    public CourseProgressSummary getCourseProgress(Course course) {
        if (currentStudent == null || course == null) {
            return new CourseProgressSummary(course == null ? 0 : course.getId(), 0, 0, 0, 0, 0, List.of());
        }

        String sql = """
                SELECT c.id AS course_id,
                       COUNT(l.id) AS total_lessons,
                       COALESCE(SUM(CASE WHEN lp.id IS NOT NULL THEN 1 ELSE 0 END), 0) AS completed_lessons,
                       COALESCE(SUM(l.duration_minutes), 0) AS total_minutes,
                       COALESCE(SUM(CASE WHEN lp.id IS NOT NULL THEN l.duration_minutes ELSE 0 END), 0) AS completed_minutes
                FROM cours c
                LEFT JOIN lecon l ON l.cours_id = c.id AND l.status = 'PUBLISHED'
                LEFT JOIN user_lecon_progress lp ON lp.lesson_id = l.id AND lp.user_id = ?
                WHERE c.id = ?
                GROUP BY c.id
                """;

        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, currentStudent.getId());
            preparedStatement.setLong(2, course.getId());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (!resultSet.next()) {
                    return new CourseProgressSummary(course.getId(), 0, 0, 0, 0, 0, List.of());
                }

                int totalLessons = resultSet.getInt("total_lessons");
                int completedLessons = resultSet.getInt("completed_lessons");
                int totalMinutes = resultSet.getInt("total_minutes");
                int completedMinutes = resultSet.getInt("completed_minutes");
                int progressPercent = totalLessons == 0 ? 0 : (int) Math.round((completedLessons * 100.0) / totalLessons);

                return new CourseProgressSummary(
                        course.getId(),
                        completedLessons,
                        totalLessons,
                        progressPercent,
                        completedMinutes,
                        totalMinutes,
                        loadRemainingLessonTitles(currentStudent.getId(), course.getId())
                );
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de charger la progression du cours.", exception);
        }
    }

    public Set<Long> getCompletedLessonIds(Course course) {
        if (currentStudent == null || course == null) {
            return Set.of();
        }

        String sql = """
                SELECT lp.lesson_id
                FROM user_lecon_progress lp
                INNER JOIN lecon l ON l.id = lp.lesson_id
                WHERE lp.user_id = ? AND l.cours_id = ? AND l.status = 'PUBLISHED'
                """;

        Set<Long> lessonIds = new LinkedHashSet<>();
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, currentStudent.getId());
            preparedStatement.setLong(2, course.getId());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    lessonIds.add(resultSet.getLong("lesson_id"));
                }
            }
            return lessonIds;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de charger les lecons terminees.", exception);
        }
    }

    public CourseProgressSummary updateLessonCompletion(Course course, Lesson lesson, boolean completed) {
        if (currentStudent == null || course == null || lesson == null) {
            return new CourseProgressSummary(course == null ? 0 : course.getId(), 0, 0, 0, 0, 0, List.of());
        }

        try {
            cnx.setAutoCommit(false);
            if (completed) {
                try (PreparedStatement preparedStatement = cnx.prepareStatement("""
                        INSERT INTO user_lecon_progress (user_id, lesson_id)
                        VALUES (?, ?)
                        ON DUPLICATE KEY UPDATE completed_at = CURRENT_TIMESTAMP
                        """)) {
                    preparedStatement.setLong(1, currentStudent.getId());
                    preparedStatement.setLong(2, lesson.getId());
                    preparedStatement.executeUpdate();
                }
            } else {
                try (PreparedStatement preparedStatement = cnx.prepareStatement("DELETE FROM user_lecon_progress WHERE user_id = ? AND lesson_id = ?")) {
                    preparedStatement.setLong(1, currentStudent.getId());
                    preparedStatement.setLong(2, lesson.getId());
                    preparedStatement.executeUpdate();
                }
            }

            refreshProgressRow(currentStudent.getId(), course.getId());
            cnx.commit();
            refreshCurrentStudentCourses();
            return getCourseProgress(course);
        } catch (SQLException exception) {
            rollbackQuietly();
            throw new IllegalStateException("Impossible de mettre a jour la progression de la lecon.", exception);
        } finally {
            resetAutoCommit();
        }
    }

    public void refreshProgressForCourse(long courseId) {
        String sql = "SELECT user_id FROM user_cours_progress WHERE cours_id = ?";
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, courseId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    refreshProgressRow(resultSet.getLong("user_id"), courseId);
                }
            }
            if (currentStudent != null) {
                refreshCurrentStudentCourses();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de recalculer la progression du cours.", exception);
        }
    }

    public ObservableList<Student> getAllStudents() {
        ObservableList<Student> students = FXCollections.observableArrayList();
        String sql = """
                SELECT id, email, first_name, last_name, age, preferred_subject
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
                SELECT id, email, first_name, last_name, age, preferred_subject
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
                SELECT id, email, first_name, last_name, age, preferred_subject
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

    private Student createStudentFromName(String name, Integer age, String preferredCategory) {
        String[] parts = name.trim().split("\\s+", 2);
        String firstName = capitalize(parts[0]);
        String lastName = parts.length > 1 ? capitalize(parts[1]) : "Student";
        String email = buildUniqueEmail(firstName + "." + lastName);
        return createStudent(email, firstName, lastName, age, preferredCategory);
    }

    private Student createStudentFromEmail(String email, Integer age, String preferredCategory) {
        String localPart = email.substring(0, email.indexOf('@'));
        String[] parts = localPart.replace('.', ' ').replace('_', ' ').trim().split("\\s+", 2);
        String firstName = parts.length > 0 && !parts[0].isBlank() ? capitalize(parts[0]) : "Edukids";
        String lastName = parts.length > 1 && !parts[1].isBlank() ? capitalize(parts[1]) : "Student";
        return createStudent(email.trim().toLowerCase(Locale.ROOT), firstName, lastName, age, preferredCategory);
    }

    private Student createStudent(String email, String firstName, String lastName, Integer age, String preferredCategory) {
        String sql = """
                INSERT INTO user (email, roles, password, first_name, last_name, age, preferred_subject, is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preparedStatement.setString(1, email);
            preparedStatement.setString(2, "[\"ROLE_ELEVE\"]");
            preparedStatement.setString(3, "desktop-generated");
            preparedStatement.setString(4, firstName);
            preparedStatement.setString(5, lastName);
            if (age == null) {
                preparedStatement.setNull(6, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(6, age);
            }
            preparedStatement.setString(7, preferredCategory);
            preparedStatement.setBoolean(8, true);
            preparedStatement.executeUpdate();

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                long generatedId = generatedKeys.next() ? generatedKeys.getLong(1) : 0;
                return new Student(generatedId, buildDisplayName(firstName, lastName, email), email, safeAge(age), preferredCategory);
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
                       c.status, c.lesson_count, c.total_duration_minutes, progress.progress,
                       COALESCE(completion.completed_lessons, 0) AS completed_lessons
                FROM user_cours_progress progress
                INNER JOIN cours c ON c.id = progress.cours_id
                LEFT JOIN (
                    SELECT lp.user_id, l.cours_id, COUNT(*) AS completed_lessons
                    FROM user_lecon_progress lp
                    INNER JOIN lecon l ON l.id = lp.lesson_id AND l.status = 'PUBLISHED'
                    GROUP BY lp.user_id, l.cours_id
                ) completion ON completion.user_id = progress.user_id AND completion.cours_id = progress.cours_id
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
                email,
                resultSet.getInt("age"),
                normalizePreferredCategory(resultSet.getString("preferred_subject"))
        );
    }

    private Course mapCourse(ResultSet resultSet) throws SQLException {
        Course course = new Course(
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
        setCourseProgress(course, resultSet.getInt("completed_lessons"), resultSet.getInt("progress"));
        return course;
    }

    private void refreshCurrentStudentCourses() {
        if (currentStudent != null) {
            currentStudent.replaceEnrolledCourses(loadEnrolledCourses(currentStudent.getId()));
        }
    }

    private List<String> loadRemainingLessonTitles(long userId, long courseId) throws SQLException {
        List<String> titles = new ArrayList<>();
        String sql = """
                SELECT l.titre
                FROM lecon l
                LEFT JOIN user_lecon_progress lp ON lp.lesson_id = l.id AND lp.user_id = ?
                WHERE l.cours_id = ? AND l.status = 'PUBLISHED' AND lp.id IS NULL
                ORDER BY l.ordre ASC, l.id ASC
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, userId);
            preparedStatement.setLong(2, courseId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    titles.add(resultSet.getString("titre"));
                }
            }
        }
        return titles;
    }

    private void refreshProgressRow(long userId, long courseId) throws SQLException {
        String sql = """
                UPDATE user_cours_progress progress
                SET progress = (
                    SELECT CASE
                        WHEN COUNT(l.id) = 0 THEN 0
                        ELSE ROUND(COALESCE(SUM(CASE WHEN lp.id IS NOT NULL THEN 1 ELSE 0 END), 0) * 100.0 / COUNT(l.id))
                    END
                    FROM lecon l
                    LEFT JOIN user_lecon_progress lp ON lp.lesson_id = l.id AND lp.user_id = progress.user_id
                    WHERE l.cours_id = progress.cours_id AND l.status = 'PUBLISHED'
                )
                WHERE progress.user_id = ? AND progress.cours_id = ?
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, userId);
            preparedStatement.setLong(2, courseId);
            preparedStatement.executeUpdate();
        }
    }

    private void setCourseProgress(Course course, int completedLessons, int progressPercent) {
        course.setCompletedLessonCount(completedLessons);
        course.setProgressPercent(progressPercent);
    }

    private void updateStudentProfile(Student student, Integer age, String preferredCategory) {
        if (student == null) {
            return;
        }

        Integer targetAge = age == null || age <= 0 ? student.getAge() : age;
        String targetCategory = preferredCategory == null || preferredCategory.isBlank()
                ? normalizePreferredCategory(student.getPreferredCategory())
                : preferredCategory;

        String sql = """
                UPDATE user
                SET age = ?, preferred_subject = ?
                WHERE id = ?
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            if (targetAge == null || targetAge <= 0) {
                preparedStatement.setNull(1, java.sql.Types.INTEGER);
            } else {
                preparedStatement.setInt(1, targetAge);
            }
            preparedStatement.setString(2, targetCategory);
            preparedStatement.setLong(3, student.getId());
            preparedStatement.executeUpdate();
            student.setAge(safeAge(targetAge));
            student.setPreferredCategory(targetCategory);
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de mettre a jour le profil de l'etudiant.", exception);
        }
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

    private void validateStudentAge(Integer age) {
        if (age != null && (age < 8 || age > 18)) {
            throw new IllegalArgumentException("EduKids personalization is available for students aged 8 to 18.");
        }
    }

    private int safeAge(Integer age) {
        return age == null ? 0 : Math.max(0, age);
    }

    private String normalizePreferredCategory(String preferredCategory) {
        return preferredCategory == null || preferredCategory.isBlank() ? "General" : preferredCategory.trim();
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
