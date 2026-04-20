package tn.esprit.services;

import tn.esprit.models.Course;
import tn.esprit.models.Lesson;
import tn.esprit.models.LessonExercise;
import tn.esprit.models.Student;
import tn.esprit.util.MyConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExerciseService {
    private final Connection cnx = MyConnection.getInstance().getCnx();
    private final StudentService studentService = new StudentService();
    private final LocalAiContentService localAiContentService = new LocalAiContentService();

    public List<LessonExercise> getExercisesByLesson(Lesson lesson) {
        if (lesson == null) {
            return List.of();
        }

        ensureExercisesForLesson(lesson);
        Student student = studentService.getCurrentStudent();
        long userId = student == null ? 0 : student.getId();

        String sql = """
                SELECT e.id,
                       e.title,
                       e.prompt,
                       e.display_order,
                       w.custom_prompt,
                       w.answer_text,
                       w.drawing_path,
                       w.updated_at
                FROM lesson_exercise e
                LEFT JOIN student_exercise_work w ON w.exercise_id = e.id AND w.user_id = ?
                WHERE e.lesson_id = ?
                ORDER BY e.display_order ASC, e.id ASC
                """;

        List<LessonExercise> exercises = new ArrayList<>();
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, userId);
            preparedStatement.setLong(2, lesson.getId());
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    Timestamp updatedAt = resultSet.getTimestamp("updated_at");
                    exercises.add(new LessonExercise(
                            resultSet.getLong("id"),
                            lesson,
                            resultSet.getString("title"),
                            resultSet.getString("prompt"),
                            resultSet.getInt("display_order"),
                            resultSet.getString("custom_prompt"),
                            resultSet.getString("answer_text"),
                            resultSet.getString("drawing_path"),
                            updatedAt == null ? null : updatedAt.toLocalDateTime()
                    ));
                }
            }
            return exercises;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de charger les exercices de la lecon.", exception);
        }
    }

    public void saveStudentExerciseWork(LessonExercise exercise, String customPrompt, String answerText) {
        saveStudentExerciseWork(exercise, customPrompt, answerText, exercise == null ? "" : exercise.getDrawingPath());
    }

    public void saveStudentExerciseWork(LessonExercise exercise, String customPrompt, String answerText, String drawingPath) {
        Student student = studentService.getCurrentStudent();
        if (student == null) {
            throw new IllegalStateException("A student must be connected to work on exercises.");
        }
        if (exercise == null) {
            throw new IllegalArgumentException("The selected exercise is missing.");
        }

        String resolvedPrompt = clean(customPrompt);
        if (resolvedPrompt.isBlank()) {
            resolvedPrompt = exercise.getDefaultPrompt();
        }

        String sql = """
                INSERT INTO student_exercise_work (user_id, exercise_id, custom_prompt, answer_text, drawing_path)
                VALUES (?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    custom_prompt = VALUES(custom_prompt),
                    answer_text = VALUES(answer_text),
                    drawing_path = VALUES(drawing_path),
                    updated_at = CURRENT_TIMESTAMP
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, student.getId());
            preparedStatement.setLong(2, exercise.getId());
            preparedStatement.setString(3, resolvedPrompt);
            preparedStatement.setString(4, clean(answerText));
            preparedStatement.setString(5, clean(drawingPath));
            preparedStatement.executeUpdate();

            exercise.setCustomPrompt(resolvedPrompt);
            exercise.setAnswer(answerText);
            exercise.setDrawingPath(drawingPath);
            exercise.setUpdatedAt(LocalDateTime.now());
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'enregistrer le travail de l'exercice.", exception);
        }
    }

    private void ensureExercisesForLesson(Lesson lesson) {
        List<ExerciseTemplate> templates = buildTemplates(lesson);
        List<ExistingExerciseRow> existingRows = loadExistingExerciseRows(lesson.getId());
        if (existingRows.isEmpty()) {
            insertGeneratedExercises(lesson, templates);
            return;
        }

        if (shouldRefreshGeneratedExercises(existingRows, isDrawingEnabledForCurrentStudent())) {
            refreshGeneratedExercises(existingRows, templates);
        }
    }

    private void insertGeneratedExercises(Lesson lesson, List<ExerciseTemplate> templates) {
        String sql = """
                INSERT INTO lesson_exercise (lesson_id, title, prompt, display_order)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (ExerciseTemplate template : templates) {
                preparedStatement.setLong(1, lesson.getId());
                preparedStatement.setString(2, template.title());
                preparedStatement.setString(3, template.prompt());
                preparedStatement.setInt(4, template.displayOrder());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'initialiser les exercices de la lecon.", exception);
        }
    }

    private List<ExistingExerciseRow> loadExistingExerciseRows(long lessonId) {
        String sql = """
                SELECT id, title, prompt, display_order
                FROM lesson_exercise
                WHERE lesson_id = ?
                ORDER BY display_order ASC, id ASC
                """;
        List<ExistingExerciseRow> rows = new ArrayList<>();
        try (PreparedStatement preparedStatement = cnx.prepareStatement(sql)) {
            preparedStatement.setLong(1, lessonId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new ExistingExerciseRow(
                            resultSet.getLong("id"),
                            resultSet.getString("title"),
                            resultSet.getString("prompt"),
                            resultSet.getInt("display_order")
                    ));
                }
            }
            return rows;
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible de charger les exercices existants de la lecon.", exception);
        }
    }

    private List<ExerciseTemplate> buildTemplates(Lesson lesson) {
        Course course = lesson.getCourse();
        Student student = studentService.getCurrentStudent();
        int age = student == null ? 10 : Math.max(8, student.getAge());
        String lessonTitle = safeText(lesson.getTitle(), "this lesson");
        String courseTitle = course == null ? "this course" : safeText(course.getTitle(), "this course");
        String courseDescription = course == null ? "" : safeText(course.getDescription(), "");
        String subjectKey = normalizeKey(course == null ? "" : course.getSubject());
        boolean drawingEnabled = age >= 8 && age <= 10;

        try {
            LocalAiContentService.GeneratedExerciseQuestions generatedQuestions = localAiContentService.generateExerciseQuestions(
                    courseTitle,
                    course == null ? "General education" : safeText(course.getSubject(), "General education"),
                    course == null ? 1 : course.getLevel(),
                    courseDescription,
                    lessonTitle,
                    age,
                    drawingEnabled
            );

            return List.of(
                    new ExerciseTemplate(generatedQuestions.questionOneTitle(), generatedQuestions.questionOnePrompt(), 1),
                    new ExerciseTemplate(generatedQuestions.questionTwoTitle(), generatedQuestions.questionTwoPrompt(), 2)
            );
        } catch (RuntimeException ignored) {
            return buildFallbackTemplates(courseTitle, courseDescription, lessonTitle, subjectKey, age, drawingEnabled);
        }
    }

    private List<ExerciseTemplate> buildFallbackTemplates(String courseTitle, String courseDescription, String lessonTitle,
                                                          String subjectKey, int age, boolean drawingEnabled) {
        String topic = extractPrimaryTopic(courseTitle, courseDescription, lessonTitle);

        String questionOne = switch (subjectKey) {
            case "mathematique", "math", "maths", "mathematiques" ->
                    age <= 10
                            ? "Quelle est l'idee principale de la lecon \"" + lessonTitle + "\" ? Donne une petite reponse avec un exemple sur " + topic + "."
                            : "Explique l'idee principale de la lecon \"" + lessonTitle + "\" et montre comment elle s'applique a " + topic + ".";
            case "anglais", "english" ->
                    age <= 10
                            ? "Quels mots ou expressions importants as-tu appris dans \"" + lessonTitle + "\" ? Ecris-en au moins trois."
                            : "Resume en anglais ce que tu as retenu de \"" + lessonTitle + "\" et reutilise le vocabulaire de " + topic + ".";
            case "francais", "fr", "lecture" ->
                    age <= 10
                            ? "Que raconte ou explique la lecon \"" + lessonTitle + "\" ? Ecris deux ou trois phrases simples."
                            : "Explique avec tes propres mots ce que la lecon \"" + lessonTitle + "\" t'apprend sur " + topic + ".";
            case "sciences", "science" ->
                    age <= 10
                            ? "Quelle observation importante peux-tu faire apres la lecon \"" + lessonTitle + "\" ?"
                            : "Explique le phenomene ou l'idee scientifique presente dans \"" + lessonTitle + "\" a propos de " + topic + ".";
            default ->
                    age <= 10
                            ? "Qu'as-tu compris dans la lecon \"" + lessonTitle + "\" a propos de " + topic + " ?"
                            : "Explique ce que tu as appris dans \"" + lessonTitle + "\" et relie-le au cours \"" + courseTitle + "\".";
        };

        String questionTwo = drawingEnabled
                ? "Fais un petit dessin ou schema sur " + topic + ", puis explique avec une courte phrase ce que ton dessin montre."
                : switch (subjectKey) {
                    case "mathematique", "math", "maths", "mathematiques" ->
                            "Ecris une reponse complete a une question d'application sur " + topic + " et explique chaque etape.";
                    case "anglais", "english" ->
                            "Ecris un petit texte en anglais sur " + topic + " en utilisant ce que tu as appris dans la lecon.";
                    case "francais", "fr", "lecture" ->
                            "Redige un paragraphe court pour montrer comment la lecon \"" + lessonTitle + "\" t'aide a mieux comprendre " + topic + ".";
                    case "sciences", "science" ->
                            "Ecris une explication claire ou une petite conclusion scientifique sur " + topic + " a partir de la lecon.";
                    default ->
                            "Donne un exemple concret lie a " + topic + " et explique pourquoi il correspond a la lecon \"" + lessonTitle + "\".";
                };

        return List.of(
                new ExerciseTemplate("Question 1", questionOne, 1),
                new ExerciseTemplate(drawingEnabled ? "Drawing Question" : "Question 2", questionTwo, 2)
        );
    }

    private void refreshGeneratedExercises(List<ExistingExerciseRow> existingRows, List<ExerciseTemplate> templates) {
        String updateExerciseSql = """
                UPDATE lesson_exercise
                SET title = ?, prompt = ?, display_order = ?
                WHERE id = ?
                """;
        String updateStudentWorkSql = """
                UPDATE student_exercise_work
                SET custom_prompt = ?
                WHERE exercise_id = ? AND custom_prompt = ?
                """;

        try (PreparedStatement exerciseStatement = cnx.prepareStatement(updateExerciseSql);
             PreparedStatement studentWorkStatement = cnx.prepareStatement(updateStudentWorkSql)) {
            int max = Math.min(existingRows.size(), templates.size());
            for (int index = 0; index < max; index++) {
                ExistingExerciseRow existing = existingRows.get(index);
                ExerciseTemplate template = templates.get(index);

                exerciseStatement.setString(1, template.title());
                exerciseStatement.setString(2, template.prompt());
                exerciseStatement.setInt(3, template.displayOrder());
                exerciseStatement.setLong(4, existing.id());
                exerciseStatement.addBatch();

                studentWorkStatement.setString(1, template.prompt());
                studentWorkStatement.setLong(2, existing.id());
                studentWorkStatement.setString(3, safeText(existing.prompt(), ""));
                studentWorkStatement.addBatch();
            }

            exerciseStatement.executeBatch();
            studentWorkStatement.executeBatch();
        } catch (SQLException exception) {
            throw new IllegalStateException("Impossible d'actualiser les questions de l'exercice.", exception);
        }
    }

    private boolean shouldRefreshGeneratedExercises(List<ExistingExerciseRow> existingRows, boolean drawingEnabled) {
        if (existingRows.isEmpty()) {
            return false;
        }

        for (ExistingExerciseRow row : existingRows) {
            String title = safeText(row.title(), "").toLowerCase(Locale.ROOT);
            String prompt = safeText(row.prompt(), "").toLowerCase(Locale.ROOT);
            boolean legacyTitle = title.equals("exercise 1") || title.equals("exercise studio");
            boolean legacyPrompt = prompt.contains("this lesson")
                    || prompt.contains("drawing area")
                    || prompt.contains("draw something linked")
                    || prompt.contains("write what you understood")
                    || prompt.contains("solve a small math activity");
            if (legacyTitle || legacyPrompt) {
                return true;
            }
        }

        if (existingRows.size() >= 2) {
            ExistingExerciseRow secondRow = existingRows.get(1);
            boolean secondHasDrawingCue = hasDrawingCue(secondRow.title()) || hasDrawingCue(secondRow.prompt());
            if (drawingEnabled && !secondHasDrawingCue) {
                return true;
            }
            if (!drawingEnabled && secondHasDrawingCue) {
                return true;
            }
        }
        return false;
    }

    private String extractPrimaryTopic(String courseTitle, String courseDescription, String lessonTitle) {
        String lessonTopic = extractLongestMeaningfulWord(lessonTitle);
        if (!lessonTopic.isBlank()) {
            return lessonTopic;
        }

        String descriptionTopic = extractLongestMeaningfulWord(courseDescription);
        if (!descriptionTopic.isBlank()) {
            return descriptionTopic;
        }

        String courseTopic = extractLongestMeaningfulWord(courseTitle);
        return courseTopic.isBlank() ? "ce sujet" : courseTopic;
    }

    private String extractLongestMeaningfulWord(String value) {
        String[] words = normalizeKey(value).split(" ");
        String best = "";
        for (String word : words) {
            if (word.length() <= 3 || isCommonWord(word)) {
                continue;
            }
            if (word.length() > best.length()) {
                best = word;
            }
        }
        return best;
    }

    private boolean isCommonWord(String value) {
        return List.of("cours", "lecon", "niveau", "avec", "pour", "dans", "this", "that", "from", "your", "english")
                .contains(value);
    }

    private boolean hasDrawingCue(String value) {
        String normalized = normalizeKey(value);
        return normalized.contains("draw")
                || normalized.contains("drawing")
                || normalized.contains("dessin")
                || normalized.contains("schema")
                || normalized.contains("sketch");
    }

    private boolean isDrawingEnabledForCurrentStudent() {
        Student student = studentService.getCurrentStudent();
        if (student == null) {
            return false;
        }
        int age = student.getAge();
        return age >= 8 && age <= 10;
    }

    private String normalizeKey(String value) {
        String normalized = Normalizer.normalize(safeText(value, ""), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
        return normalized.replaceAll("[^a-z0-9 ]", "").replaceAll("\\s+", " ").trim();
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private record ExerciseTemplate(String title, String prompt, int displayOrder) {
    }

    private record ExistingExerciseRow(long id, String title, String prompt, int displayOrder) {
    }
}
