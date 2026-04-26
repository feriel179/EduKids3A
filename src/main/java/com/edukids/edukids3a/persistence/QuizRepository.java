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
        synchronizeQuestionCounts();
        quizzes.setAll(loadAll());
    }

    public ObservableList<Quiz> findAll() {
        return quizzes;
    }

    public Quiz save(Quiz quiz) {
        String sql = """
                INSERT INTO quiz (titre, description, image_url, niveau, categorie_age, nombre_questions, duree_minutes, score_minimum, statut)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, quiz.getTitre());
            statement.setString(2, quiz.getDescription());
            statement.setString(3, quiz.getImageUrl());
            statement.setString(4, quiz.getNiveau());
            statement.setString(5, quiz.getCategorieAge());
            statement.setInt(6, quiz.getNombreQuestions());
            statement.setInt(7, quiz.getDureeMinutes());
            statement.setInt(8, quiz.getScoreMinimum());
            statement.setString(9, quiz.getStatut());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    Quiz savedQuiz = new Quiz(
                            generatedKeys.getInt(1),
                            quiz.getTitre(),
                            quiz.getDescription(),
                            quiz.getImageUrl(),
                            quiz.getNiveau(),
                            quiz.getCategorieAge(),
                            quiz.getNombreQuestions(),
                            quiz.getDureeMinutes(),
                            quiz.getScoreMinimum(),
                            quiz.getStatut()
                    );
                    quizzes.add(savedQuiz);
                    return savedQuiz;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'enregistrer le quiz.", e);
        }
        throw new IllegalStateException("Le quiz a ete insere mais aucun identifiant n'a ete retourne.");
    }

    public void update(Quiz quiz) {
        String sql = """
                UPDATE quiz
                SET titre = ?, description = ?, image_url = ?, niveau = ?, categorie_age = ?, nombre_questions = ?, duree_minutes = ?, score_minimum = ?, statut = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, quiz.getTitre());
            statement.setString(2, quiz.getDescription());
            statement.setString(3, quiz.getImageUrl());
            statement.setString(4, quiz.getNiveau());
            statement.setString(5, quiz.getCategorieAge());
            statement.setInt(6, quiz.getNombreQuestions());
            statement.setInt(7, quiz.getDureeMinutes());
            statement.setInt(8, quiz.getScoreMinimum());
            statement.setString(9, quiz.getStatut());
            statement.setInt(10, quiz.getId());
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

    public void synchronizeQuestionCount(Quiz quiz) {
        if (quiz.getId() == null) {
            return;
        }
        String sql = """
                UPDATE quiz
                SET nombre_questions = (
                    SELECT COUNT(*)
                    FROM question
                    WHERE question.quiz_id = quiz.id
                )
                WHERE id = ?
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             PreparedStatement countStatement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM question WHERE quiz_id = ?")) {
            statement.setInt(1, quiz.getId());
            statement.executeUpdate();
            countStatement.setInt(1, quiz.getId());
            try (ResultSet resultSet = countStatement.executeQuery()) {
                if (resultSet.next()) {
                    int questionCount = resultSet.getInt(1);
                    quiz.setNombreQuestions(questionCount);
                    applyQuestionCount(quiz.getId(), questionCount);
                }
            }
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
                        resultSet.getString("image_url"),
                        resultSet.getString("niveau"),
                        resultSet.getString("categorie_age"),
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

        save(new Quiz("Vrai ou Faux Nature", "Quiz facile pour identifier des affirmations simples sur la nature.", "", "Debutant", Quiz.CATEGORIE_AGE_FACILE, 0, 10, 50, "Publie"));
        save(new Quiz("Quiz Java", "Quiz d'introduction au langage Java.", "", "Debutant", Quiz.CATEGORIE_AGE_STANDARD, 0, 20, 60, "Publie"));
        save(new Quiz("Quiz SQL", "Quiz de revision sur les bases SQL.", "", "Intermediaire", Quiz.CATEGORIE_AGE_STANDARD, 0, 15, 50, "Brouillon"));
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

    private void synchronizeQuestionCounts() {
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

    private void applyQuestionCount(Integer quizId, int questionCount) {
        if (quizId == null) {
            return;
        }
        for (Quiz existing : quizzes) {
            if (quizId.equals(existing.getId())) {
                existing.setNombreQuestions(questionCount);
            }
        }
    }
}
