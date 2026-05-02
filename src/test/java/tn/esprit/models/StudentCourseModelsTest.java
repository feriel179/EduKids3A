package tn.esprit.models;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentCourseModelsTest {

    @Test
    void course_normalizesStatusCountsAndProgress() {
        Course course = new Course(1, "Java", "Bases Java", 7, "Programmation", null, 2, 1,
                "published", -3, -20);

        course.setCompletedLessonCount(5);
        course.setLessonCount(3);
        course.setProgressPercent(140);

        assertTrue(course.isPublished());
        assertEquals("Published", course.getStatusLabel());
        assertEquals(0, course.getTotalDurationMinutes());
        assertEquals(100, course.getProgressPercent());
        assertEquals(0, course.getRemainingLessonCount());
        assertEquals("#ef4444", course.getLevelColor());
    }

    @Test
    void course_formatsDurations() {
        assertEquals("0 min", Course.formatDuration(-10));
        assertEquals("45 min", Course.formatDuration(45));
        assertEquals("2 h", Course.formatDuration(120));
        assertEquals("2 h 15 min", Course.formatDuration(135));
    }

    @Test
    void lesson_resolvesMediaSummariesAndStatus() {
        Course course = new Course(1, "Java", "Bases Java", 1, "Programmation", null, 0, 0);
        Lesson lesson = new Lesson(2, course, 1, "Intro", "youtube",
                "support.pdf", "video.mp4", "https://youtu.be/abc", null, "published", 75);

        assertEquals("https://youtu.be/abc", lesson.getActiveUrl());
        assertEquals("PDF + VIDEO + YOUTUBE", lesson.getDisplayMediaType());
        assertEquals("[MULTI]", lesson.getMediaIcon());
        assertEquals("PDF: support.pdf | VIDEO: video.mp4 | YOUTUBE: https://youtu.be/abc", lesson.getUrlSummary());
        assertTrue(lesson.isPublished());
        assertEquals("1 h 15 min", lesson.getDurationLabel());
    }

    @Test
    void student_normalizesProfileAndEnrollmentList() {
        Student student = new Student(1, "Ada", "ada@example.com", -4, " ");
        Course course = new Course(2, "Math", "Description", 2, "Math", null, 0, 0);

        student.replaceEnrolledCourses(List.of(course));
        student.setAge(12);
        student.setPreferredCategory(" Science ");

        assertEquals(12, student.getAge());
        assertEquals("Ages 11-13", student.getAgeGroupLabel());
        assertEquals("12 years old", student.getAgeLabel());
        assertEquals("Science", student.getPreferredCategory());
        assertEquals(List.of(course), student.getEnrolledCourses());
    }

    @Test
    void progressSummary_clampsValuesAndFormatsLabels() {
        CourseProgressSummary summary = new CourseProgressSummary(3, -1, 5, 120, 90, 180,
                List.of("L1", "L2", "L3", "L4"));

        assertEquals(0, summary.getCompletedLessons());
        assertEquals(100, summary.getProgressPercent());
        assertEquals(5, summary.getRemainingLessons());
        assertEquals(1.0, summary.getProgressFraction());
        assertEquals("0/5 lessons done", summary.getCompletedLessonsLabel());
        assertEquals("5 lessons left", summary.getRemainingLessonsLabel());
        assertEquals("1 h 30 min completed", summary.getCompletedTimeLabel());
        assertEquals("L1, L2, L3...", summary.getRemainingTitlesPreview());
    }

    @Test
    void lessonExercise_trimsFieldsAndResolvesPrompt() {
        LessonExercise exercise = new LessonExercise(1, null, "Draw", " Default prompt ", 0,
                " ", " Answer ", " drawing.png ", LocalDateTime.now());

        assertEquals(1, exercise.getDisplayOrder());
        assertEquals("Default prompt", exercise.getResolvedPrompt());
        assertTrue(exercise.hasAnswer());
        assertTrue(exercise.hasDrawing());

        exercise.setAnswer(" ");
        exercise.setDrawingPath(null);
        exercise.setCustomPrompt(" Custom prompt ");

        assertEquals("Custom prompt", exercise.getResolvedPrompt());
        assertFalse(exercise.hasAnswer());
        assertFalse(exercise.hasDrawing());
    }
}
