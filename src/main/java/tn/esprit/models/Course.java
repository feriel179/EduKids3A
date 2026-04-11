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

    public Course(long id, String title, String description, int level, String subject, String image, int likes, int dislikes) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.level = level;
        this.subject = subject;
        this.image = image;
        this.likes = likes;
        this.dislikes = dislikes;
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

    @Override
    public String toString() {
        return title + " (" + subject + ")";
    }
}
