package com.edukids.edukids3a.model;

public class QuizResult {
    private final Integer id;
    private final Integer quizId;
    private final String quizTitre;
    private final int finalScore;
    private final int earnedPoints;
    private final int totalPoints;
    private final String completedAt;

    public QuizResult(Integer id, Integer quizId, String quizTitre, int finalScore, int earnedPoints, int totalPoints, String completedAt) {
        this.id = id;
        this.quizId = quizId;
        this.quizTitre = quizTitre;
        this.finalScore = finalScore;
        this.earnedPoints = earnedPoints;
        this.totalPoints = totalPoints;
        this.completedAt = completedAt;
    }

    public Integer getId() {
        return id;
    }

    public Integer getQuizId() {
        return quizId;
    }

    public String getQuizTitre() {
        return quizTitre;
    }

    public int getFinalScore() {
        return finalScore;
    }

    public int getEarnedPoints() {
        return earnedPoints;
    }

    public int getTotalPoints() {
        return totalPoints;
    }

    public String getCompletedAt() {
        return completedAt;
    }
}
