package com.edukids.edukids3a.persistence;

import com.edukids.edukids3a.model.Quiz;
import com.edukids.edukids3a.model.QuizResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class QuizResultRepository {

    public QuizResultRepository() {
        DatabaseManager.initialize();
    }

    public void save(Quiz quiz, int finalScore, int earnedPoints, int totalPoints, String completedAt) {
        String sql = """
                INSERT INTO quiz_result (quiz_id, final_score, earned_points, total_points, completed_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quiz.getId());
            statement.setInt(2, finalScore);
            statement.setInt(3, earnedPoints);
            statement.setInt(4, totalPoints);
            statement.setString(5, completedAt);
            statement.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Impossible d'enregistrer le resultat du quiz.", e);
        }
    }

    public List<QuizResult> findByQuiz(Quiz quiz) {
        List<QuizResult> results = new ArrayList<>();
        String sql = """
                SELECT qr.id, qr.quiz_id, q.titre, qr.final_score, qr.earned_points, qr.total_points, qr.completed_at
                FROM quiz_result qr
                JOIN quiz q ON q.id = qr.quiz_id
                WHERE qr.quiz_id = ?
                ORDER BY qr.completed_at DESC, qr.id DESC
                """;
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quiz.getId());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    results.add(new QuizResult(
                            resultSet.getInt("id"),
                            resultSet.getInt("quiz_id"),
                            resultSet.getString("titre"),
                            resultSet.getInt("final_score"),
                            resultSet.getInt("earned_points"),
                            resultSet.getInt("total_points"),
                            resultSet.getString("completed_at")
                    ));
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Impossible de charger l'historique des resultats.", e);
        }
        return results;
    }
}
