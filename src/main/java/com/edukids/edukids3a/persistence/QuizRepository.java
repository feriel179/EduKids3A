package com.edukids.edukids3a.persistence;

import com.edukids.edukids3a.model.Quiz;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class QuizRepository {
    private final ObservableList<Quiz> quizzes = FXCollections.observableArrayList();

    public QuizRepository() {
        DatabaseManager.initialize();
        seedIfEmpty();
        refreshQuestionCounts();
        quizzes.setAll(loadAll());
    }

    public ObservableList<Quiz> findAll() {
        return quizzes;
    }

    public void save(Quiz quiz) {
        String sql = """
                INSERT INTO quiz (titre, description, niveau, nombre_questions, duree_minutes, score_minimum, statut)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, quiz.getTitre());
            statement.setString(2, quiz.getDescription());
            statement.setString(3, quiz.getNiveau());
            statement.setInt(4, quiz.getNombreQuestions());
            statement.setInt(5, quiz.getDureeMinutes());
            statement.setInt(6, quiz.getScoreMinimum());
            statement.setString(7, quiz.getStatut());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    quizzes.add(new Quiz(
                            generatedKeys.getInt(1),
                            quiz.getTitre(),
                            quiz.getDescription(),
                            quiz.getNiveau(),
                            quiz.getNombreQuestions(),
                            quiz.getDureeMinutes(),
                            quiz.getScoreMinimum(),
                            quiz.getStatut()
                    ));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'enregistrer le quiz.", e);
        }
    }

    public void update(Quiz quiz) {
        String sql = """
                UPDATE quiz
                SET titre = ?, description = ?, niveau = ?, nombre_questions = ?, duree_minutes = ?, score_minimum = ?, statut = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, quiz.getTitre());
            statement.setString(2, quiz.getDescription());
            statement.setString(3, quiz.getNiveau());
            statement.setInt(4, quiz.getNombreQuestions());
            statement.setInt(5, quiz.getDureeMinutes());
            statement.setInt(6, quiz.getScoreMinimum());
            statement.setString(7, quiz.getStatut());
            statement.setInt(8, quiz.getId());
            statement.executeUpdate();

            replaceInMemory(quiz);
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de modifier le quiz.", e);
        }
    }

    public void delete(Quiz quiz) {
        Connection connection = null;
        try {
            connection = DatabaseManager.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement deleteResponses = connection.prepareStatement("""
                    DELETE FROM reponse
                    WHERE question_id IN (SELECT id FROM question WHERE quiz_id = ?)
                    """);
                 PreparedStatement deleteQuestions = connection.prepareStatement("DELETE FROM question WHERE quiz_id = ?");
                 PreparedStatement deleteQuiz = connection.prepareStatement("DELETE FROM quiz WHERE id = ?")) {
                deleteResponses.setInt(1, quiz.getId());
                deleteResponses.executeUpdate();

                deleteQuestions.setInt(1, quiz.getId());
                deleteQuestions.executeUpdate();

                deleteQuiz.setInt(1, quiz.getId());
                deleteQuiz.executeUpdate();
            }

            connection.commit();
            quizzes.removeIf(existing -> existing.getId() != null && existing.getId().equals(quiz.getId()));
        } catch (Exception e) {
            rollbackQuietly(connection);
            throw new IllegalStateException("Impossible de supprimer le quiz.", e);
        } finally {
            closeQuietly(connection);
        }
    }

    public void incrementQuestionCount(Quiz quiz) {
        if (quiz.getId() == null) {
            return;
        }
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE quiz SET nombre_questions = nombre_questions + 1 WHERE id = ?")) {
            statement.setInt(1, quiz.getId());
            statement.executeUpdate();
            quiz.incrementNombreQuestions();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de mettre a jour le nombre de questions du quiz.", e);
        }
    }

    public void decrementQuestionCount(Quiz quiz) {
        if (quiz.getId() == null) {
            return;
        }
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE quiz SET nombre_questions = CASE WHEN nombre_questions > 0 THEN nombre_questions - 1 ELSE 0 END WHERE id = ?")) {
            statement.setInt(1, quiz.getId());
            statement.executeUpdate();
            quiz.decrementNombreQuestions();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de mettre a jour le nombre de questions du quiz.", e);
        }
    }

    private ObservableList<Quiz> loadAll() {
        ObservableList<Quiz> loadedQuizzes = FXCollections.observableArrayList();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM quiz ORDER BY id");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                loadedQuizzes.add(new Quiz(
                        resultSet.getInt("id"),
                        resultSet.getString("titre"),
                        resultSet.getString("description"),
                        resultSet.getString("niveau"),
                        resultSet.getInt("nombre_questions"),
                        resultSet.getInt("duree_minutes"),
                        resultSet.getInt("score_minimum"),
                        resultSet.getString("statut")
                ));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les quiz.", e);
        }
        return loadedQuizzes;
    }

    private void seedIfEmpty() {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement countStatement = connection.prepareStatement("SELECT COUNT(*) FROM quiz");
             ResultSet resultSet = countStatement.executeQuery()) {
            if (resultSet.next() && resultSet.getInt(1) > 0) {
                return;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de verifier les quiz existants.", e);
        }

        save(new Quiz("Quiz Java", "Quiz d'introduction au langage Java.", "Debutant", 0, 20, 60, "Publie"));
        save(new Quiz("Quiz SQL", "Quiz de revision sur les bases SQL.", "Intermediaire", 0, 15, 50, "Brouillon"));
        quizzes.clear();
    }

    private void replaceInMemory(Quiz updatedQuiz) {
        for (int i = 0; i < quizzes.size(); i++) {
            Quiz existing = quizzes.get(i);
            if (existing.getId() != null && existing.getId().equals(updatedQuiz.getId())) {
                quizzes.set(i, updatedQuiz);
                return;
            }
        }
    }

    private void rollbackQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (Exception ignored) {
        }
    }

    private void refreshQuestionCounts() {
        String sql = """
                UPDATE quiz
                SET nombre_questions = (
                    SELECT COUNT(*)
                    FROM question
                    WHERE question.quiz_id = quiz.id
                )
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de synchroniser le nombre de questions.", e);
        }
    }
}
