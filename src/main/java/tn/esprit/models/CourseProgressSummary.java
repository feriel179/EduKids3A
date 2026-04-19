package tn.esprit.models;

import java.util.List;

public class CourseProgressSummary {
    private final long courseId;
    private final int completedLessons;
    private final int totalLessons;
    private final int progressPercent;
    private final int completedMinutes;
    private final int totalMinutes;
    private final List<String> remainingLessonTitles;

    public CourseProgressSummary(long courseId, int completedLessons, int totalLessons, int progressPercent,
                                 int completedMinutes, int totalMinutes, List<String> remainingLessonTitles) {
        this.courseId = courseId;
        this.completedLessons = Math.max(0, completedLessons);
        this.totalLessons = Math.max(0, totalLessons);
        this.progressPercent = Math.max(0, Math.min(100, progressPercent));
        this.completedMinutes = Math.max(0, completedMinutes);
        this.totalMinutes = Math.max(0, totalMinutes);
        this.remainingLessonTitles = remainingLessonTitles == null ? List.of() : List.copyOf(remainingLessonTitles);
    }

    public long getCourseId() {
        return courseId;
    }

    public int getCompletedLessons() {
        return completedLessons;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public int getCompletedMinutes() {
        return completedMinutes;
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    public List<String> getRemainingLessonTitles() {
        return remainingLessonTitles;
    }

    public int getRemainingLessons() {
        return Math.max(0, totalLessons - completedLessons);
    }

    public double getProgressFraction() {
        return progressPercent / 100.0;
    }

    public String getCompletedLessonsLabel() {
        return completedLessons + "/" + totalLessons + " lessons done";
    }

    public String getRemainingLessonsLabel() {
        int remaining = getRemainingLessons();
        return remaining + (remaining == 1 ? " lesson left" : " lessons left");
    }

    public String getCompletedTimeLabel() {
        return Course.formatDuration(completedMinutes) + " completed";
    }

    public String getRemainingTitlesPreview() {
        if (remainingLessonTitles.isEmpty()) {
            return "Nothing remains. This course is complete.";
        }
        if (remainingLessonTitles.size() <= 3) {
            return String.join(", ", remainingLessonTitles);
        }
        return String.join(", ", remainingLessonTitles.subList(0, 3)) + "...";
    }
}
