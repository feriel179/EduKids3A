package tn.esprit.models;

public class Course {
    private final long id;
    private String title;
    private String description;
    private int level;
    private String subject;
    private String image;
    private int likes;
    private int dislikes;
    private String status;
    private int lessonCount;
    private int totalDurationMinutes;
    private int completedLessonCount;
    private int progressPercent;

    public Course(long id, String title, String description, int level, String subject, String image, int likes, int dislikes) {
        this(id, title, description, level, subject, image, likes, dislikes, "DRAFT", 0, 0);
    }

    public Course(long id, String title, String description, int level, String subject, String image, int likes, int dislikes,
                  String status, int lessonCount, int totalDurationMinutes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.level = level;
        this.subject = subject;
        this.image = image;
        this.likes = likes;
        this.dislikes = dislikes;
        this.status = normalizeStatus(status);
        this.lessonCount = Math.max(0, lessonCount);
        this.totalDurationMinutes = Math.max(0, totalDurationMinutes);
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public int getDislikes() {
        return dislikes;
    }

    public void setDislikes(int dislikes) {
        this.dislikes = dislikes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = normalizeStatus(status);
    }

    public int getLessonCount() {
        return lessonCount;
    }

    public void setLessonCount(int lessonCount) {
        this.lessonCount = Math.max(0, lessonCount);
    }

    public int getTotalDurationMinutes() {
        return totalDurationMinutes;
    }

    public void setTotalDurationMinutes(int totalDurationMinutes) {
        this.totalDurationMinutes = Math.max(0, totalDurationMinutes);
    }

    public String getLevelText() {
        return level > 0 ? "Niveau " + level : "Niveau inconnu";
    }

    public String getLevelColor() {
        if (level <= 3) {
            return "#22c55e";
        }
        if (level <= 6) {
            return "#f59e0b";
        }
        return "#ef4444";
    }

    public boolean isPublished() {
        return "PUBLISHED".equals(status);
    }

    public boolean isArchived() {
        return "ARCHIVED".equals(status);
    }

    public String getStatusLabel() {
        return switch (status) {
            case "PUBLISHED" -> "Published";
            case "ARCHIVED" -> "Archived";
            default -> "Draft";
        };
    }

    public String getTotalDurationLabel() {
        return formatDuration(totalDurationMinutes);
    }

    public int getCompletedLessonCount() {
        return completedLessonCount;
    }

    public void setCompletedLessonCount(int completedLessonCount) {
        this.completedLessonCount = Math.max(0, completedLessonCount);
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = Math.max(0, Math.min(100, progressPercent));
    }

    public int getRemainingLessonCount() {
        return Math.max(0, lessonCount - completedLessonCount);
    }

    public static String formatDuration(int totalMinutes) {
        int safeMinutes = Math.max(0, totalMinutes);
        if (safeMinutes < 60) {
            return safeMinutes + " min";
        }

        int hours = safeMinutes / 60;
        int minutes = safeMinutes % 60;
        if (minutes == 0) {
            return hours + " h";
        }
        return hours + " h " + minutes + " min";
    }

    private String normalizeStatus(String value) {
        if (value == null) {
            return "DRAFT";
        }

        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "PUBLISHED", "ARCHIVED" -> normalized;
            default -> "DRAFT";
        };
    }

    @Override
    public String toString() {
        return title + " (" + subject + ")";
    }
}
