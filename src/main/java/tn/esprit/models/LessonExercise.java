package tn.esprit.models;

import java.time.LocalDateTime;

public class LessonExercise {
    private final long id;
    private final Lesson lesson;
    private final String title;
    private final String defaultPrompt;
    private final int displayOrder;
    private String customPrompt;
    private String answer;
    private String drawingPath;
    private LocalDateTime updatedAt;

    public LessonExercise(long id, Lesson lesson, String title, String defaultPrompt, int displayOrder,
                          String customPrompt, String answer, String drawingPath, LocalDateTime updatedAt) {
        this.id = id;
        this.lesson = lesson;
        this.title = title;
        this.defaultPrompt = defaultPrompt == null ? "" : defaultPrompt.trim();
        this.displayOrder = Math.max(1, displayOrder);
        this.customPrompt = customPrompt == null ? "" : customPrompt.trim();
        this.answer = answer == null ? "" : answer.trim();
        this.drawingPath = drawingPath == null ? "" : drawingPath.trim();
        this.updatedAt = updatedAt;
    }

    public long getId() {
        return id;
    }

    public Lesson getLesson() {
        return lesson;
    }

    public String getTitle() {
        return title;
    }

    public String getDefaultPrompt() {
        return defaultPrompt;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public String getCustomPrompt() {
        return customPrompt;
    }

    public void setCustomPrompt(String customPrompt) {
        this.customPrompt = customPrompt == null ? "" : customPrompt.trim();
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer == null ? "" : answer.trim();
    }

    public String getDrawingPath() {
        return drawingPath;
    }

    public void setDrawingPath(String drawingPath) {
        this.drawingPath = drawingPath == null ? "" : drawingPath.trim();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getResolvedPrompt() {
        return customPrompt == null || customPrompt.isBlank() ? defaultPrompt : customPrompt;
    }

    public boolean hasAnswer() {
        return answer != null && !answer.isBlank();
    }

    public boolean hasDrawing() {
        return drawingPath != null && !drawingPath.isBlank();
    }
}
