package com.edukids.edukids3a.persistence;

import com.edukids.edukids3a.model.Question;
import com.edukids.edukids3a.model.Quiz;
import com.edukids.edukids3a.model.Reponse;
import com.edukids.edukids3a.model.TypeQuestion;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class QuestionRepository {
    private final ObservableList<Question> questions = FXCollections.observableArrayList();
    private final QuizRepository quizRepository;

    public QuestionRepository(QuizRepository quizRepository) {
        this.quizRepository = quizRepository;
        DatabaseManager.initialize();
        questions.setAll(loadAll());
    }

    public ObservableList<Question> findAll() {
        return questions;
    }

    public void save(Question question) {
        String insertQuestionSql = """
                INSERT INTO question (quiz_id, intitule, type, points)
                VALUES (?, ?, ?, ?)
                """;
        String insertReponseSql = """
                INSERT INTO reponse (question_id, texte, correcte)
                VALUES (?, ?, ?)
                """;

        Connection connection = null;
        try {
            connection = DatabaseManager.getConnection();
            connection.setAutoCommit(false);
            int questionId;

            try (PreparedStatement questionStatement = connection.prepareStatement(insertQuestionSql, Statement.RETURN_GENERATED_KEYS)) {
                questionStatement.setInt(1, question.getQuiz().getId());
                questionStatement.setString(2, question.getIntitule());
                questionStatement.setString(3, question.getType().name());
                questionStatement.setInt(4, question.getPoints());
                questionStatement.executeUpdate();

                try (ResultSet generatedKeys = questionStatement.getGeneratedKeys()) {
                    generatedKeys.next();
                    questionId = generatedKeys.getInt(1);
                }
            }

            List<Reponse> savedReponses = new ArrayList<>();
            try (PreparedStatement reponseStatement = connection.prepareStatement(insertReponseSql, Statement.RETURN_GENERATED_KEYS)) {
                for (Reponse reponse : question.getReponses()) {
                    reponseStatement.setInt(1, questionId);
                    reponseStatement.setString(2, reponse.getTexte());
                    reponseStatement.setInt(3, reponse.isCorrecte() ? 1 : 0);
                    reponseStatement.executeUpdate();

                    try (ResultSet generatedKeys = reponseStatement.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            savedReponses.add(new Reponse(generatedKeys.getInt(1), reponse.getTexte(), reponse.isCorrecte()));
                        }
                    }
                }
            }

            connection.commit();
            quizRepository.synchronizeQuestionCount(question.getQuiz());
            questions.add(new Question(questionId, question.getQuiz(), question.getIntitule(), question.getType(), question.getPoints(), savedReponses));
        } catch (Exception e) {
            rollbackQuietly(connection);
            throw new IllegalStateException("Impossible d'enregistrer la question.", e);
        } finally {
            closeQuietly(connection);
        }
    }

    public void update(Question originalQuestion, Question updatedQuestion) {
        String updateQuestionSql = """
                UPDATE question
                SET quiz_id = ?, intitule = ?, type = ?, points = ?
                WHERE id = ?
                """;
        String deleteAnswersSql = "DELETE FROM reponse WHERE question_id = ?";
        String insertReponseSql = """
                INSERT INTO reponse (question_id, texte, correcte)
                VALUES (?, ?, ?)
                """;

        Connection connection = null;
        try {
            connection = DatabaseManager.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement updateQuestion = connection.prepareStatement(updateQuestionSql);
                 PreparedStatement deleteAnswers = connection.prepareStatement(deleteAnswersSql);
                 PreparedStatement insertAnswer = connection.prepareStatement(insertReponseSql, Statement.RETURN_GENERATED_KEYS)) {
                updateQuestion.setInt(1, updatedQuestion.getQuiz().getId());
                updateQuestion.setString(2, updatedQuestion.getIntitule());
                updateQuestion.setString(3, updatedQuestion.getType().name());
                updateQuestion.setInt(4, updatedQuestion.getPoints());
                updateQuestion.setInt(5, originalQuestion.getId());
                updateQuestion.executeUpdate();

                deleteAnswers.setInt(1, originalQuestion.getId());
                deleteAnswers.executeUpdate();

                List<Reponse> savedAnswers = new ArrayList<>();
                for (Reponse reponse : updatedQuestion.getReponses()) {
                    insertAnswer.setInt(1, originalQuestion.getId());
                    insertAnswer.setString(2, reponse.getTexte());
                    insertAnswer.setInt(3, reponse.isCorrecte() ? 1 : 0);
                    insertAnswer.executeUpdate();
                    try (ResultSet generatedKeys = insertAnswer.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            savedAnswers.add(new Reponse(generatedKeys.getInt(1), reponse.getTexte(), reponse.isCorrecte()));
                        }
                    }
                }

                connection.commit();
                quizRepository.synchronizeQuestionCount(originalQuestion.getQuiz());
                if (!originalQuestion.getQuiz().getId().equals(updatedQuestion.getQuiz().getId())) {
                    quizRepository.synchronizeQuestionCount(updatedQuestion.getQuiz());
                }
                replaceInMemory(new Question(
                        originalQuestion.getId(),
                        updatedQuestion.getQuiz(),
                        updatedQuestion.getIntitule(),
                        updatedQuestion.getType(),
                        updatedQuestion.getPoints(),
                        savedAnswers
                ));
            }
        } catch (Exception e) {
            rollbackQuietly(connection);
            throw new IllegalStateException("Impossible de modifier la question.", e);
        } finally {
            closeQuietly(connection);
        }
    }

    public void delete(Question question) {
        Connection connection = null;
        try {
            connection = DatabaseManager.getConnection();
            connection.setAutoCommit(false);

            try (PreparedStatement deleteAnswers = connection.prepareStatement("DELETE FROM reponse WHERE question_id = ?");
                 PreparedStatement deleteQuestion = connection.prepareStatement("DELETE FROM question WHERE id = ?")) {
                deleteAnswers.setInt(1, question.getId());
                deleteAnswers.executeUpdate();

                deleteQuestion.setInt(1, question.getId());
                deleteQuestion.executeUpdate();
            }

            connection.commit();
            quizRepository.synchronizeQuestionCount(question.getQuiz());
            questions.removeIf(existing -> existing.getId() != null && existing.getId().equals(question.getId()));
        } catch (Exception e) {
            rollbackQuietly(connection);
            throw new IllegalStateException("Impossible de supprimer la question.", e);
        } finally {
            closeQuietly(connection);
        }
    }

    private ObservableList<Question> loadAll() {
        ObservableList<Question> loadedQuestions = FXCollections.observableArrayList();
        String sql = """
                SELECT q.id AS question_id, q.intitule, q.type, q.points,
                       z.id AS quiz_id, z.titre, z.description, z.image_url, z.niveau, z.categorie_age, z.nombre_questions, z.duree_minutes, z.score_minimum, z.statut
                FROM question q
                JOIN quiz z ON z.id = q.quiz_id
                ORDER BY q.id
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Quiz quiz = findQuizById(resultSet.getInt("quiz_id"));
                if (quiz == null) {
                    quiz = new Quiz(
                            resultSet.getInt("quiz_id"),
                            resultSet.getString("titre"),
                            resultSet.getString("description"),
                            resultSet.getString("image_url"),
                            resultSet.getString("niveau"),
                            resultSet.getString("categorie_age"),
                            resultSet.getInt("nombre_questions"),
                            resultSet.getInt("duree_minutes"),
                            resultSet.getInt("score_minimum"),
                            resultSet.getString("statut")
                    );
                }

                loadedQuestions.add(new Question(
                        resultSet.getInt("question_id"),
                        quiz,
                        resultSet.getString("intitule"),
                        TypeQuestion.valueOf(resultSet.getString("type")),
                        resultSet.getInt("points"),
                        loadReponses(resultSet.getInt("question_id"))
                ));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les questions.", e);
        }
        return loadedQuestions;
    }

    private List<Reponse> loadReponses(int questionId) {
        List<Reponse> reponses = new ArrayList<>();
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM reponse WHERE question_id = ? ORDER BY id")) {
            statement.setInt(1, questionId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    reponses.add(new Reponse(
                            resultSet.getInt("id"),
                            resultSet.getString("texte"),
                            resultSet.getInt("correcte") == 1
                    ));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger les reponses.", e);
        }
        return reponses;
    }

    private Quiz findQuizById(int quizId) {
        return quizRepository.findAll().stream()
                .filter(quiz -> quiz.getId() != null && quiz.getId() == quizId)
                .findFirst()
                .orElse(null);
    }

    private void replaceInMemory(Question updatedQuestion) {
        for (int i = 0; i < questions.size(); i++) {
            Question existing = questions.get(i);
            if (existing.getId() != null && existing.getId().equals(updatedQuestion.getId())) {
                questions.set(i, updatedQuestion);
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
}
