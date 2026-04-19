package tn.esprit.util;

import javafx.scene.control.TextFormatter;

import java.util.Locale;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public final class FormValidator {
    private static final Pattern TITLE_PATTERN = Pattern.compile("^[\\p{L}\\p{N} .,'()\\-_/&+]+$");
    private static final Pattern YOUTUBE_PATTERN = Pattern.compile("^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be)/.+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern WINDOWS_PATH_PATTERN = Pattern.compile("^[A-Za-z]:\\\\.+");

    private FormValidator() {
    }

    public static TextFormatter<String> createLengthFormatter(int maxLength) {
        return new TextFormatter<>(change -> change.getControlNewText().length() <= maxLength ? change : null);
    }

    public static TextFormatter<String> createDigitsFormatter(int maxLength) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.length() > maxLength) {
                return null;
            }
            return newText.matches("\\d*") ? change : null;
        };
        return new TextFormatter<>(filter);
    }

    public static String validateCourse(String title, String subject, String description) {
        String cleanTitle = clean(title);
        String cleanSubject = clean(subject);
        String cleanDescription = clean(description);

        if (cleanTitle.isBlank()) {
            return "Course title is required.";
        }
        if (cleanTitle.length() < 3) {
            return "Course title must contain at least 3 characters.";
        }
        if (cleanTitle.length() > 120) {
            return "Course title is too long.";
        }
        if (!TITLE_PATTERN.matcher(cleanTitle).matches()) {
            return "Course title contains unsupported characters.";
        }
        if (cleanTitle.matches("\\d+")) {
            return "Course title cannot contain only numbers.";
        }

        if (cleanSubject.isBlank()) {
            return "Subject is required.";
        }
        if (cleanSubject.length() < 2) {
            return "Subject must contain at least 2 characters.";
        }
        if (cleanSubject.length() > 80) {
            return "Subject is too long.";
        }
        if (containsControlCharacters(cleanSubject)) {
            return "Subject contains unsupported control characters.";
        }
        if (cleanSubject.matches("\\d+")) {
            return "Subject cannot contain only numbers.";
        }

        if (cleanDescription.isBlank()) {
            return "Description is required.";
        }
        if (cleanDescription.length() < 10) {
            return "Description must contain at least 10 characters.";
        }
        if (cleanDescription.length() > 1000) {
            return "Description is too long.";
        }

        return null;
    }

    public static String validateLesson(String lessonTitle, String order, String duration, String pdfUrl, String videoUrl, String youtubeUrl) {
        String cleanLessonTitle = clean(lessonTitle);
        String cleanOrder = clean(order);
        String cleanDuration = clean(duration);
        String cleanPdfUrl = clean(pdfUrl);
        String cleanVideoUrl = clean(videoUrl);
        String cleanYoutubeUrl = clean(youtubeUrl);

        if (cleanLessonTitle.isBlank()) {
            return "Lesson title is required.";
        }
        if (cleanLessonTitle.length() < 3) {
            return "Lesson title must contain at least 3 characters.";
        }
        if (cleanLessonTitle.length() > 120) {
            return "Lesson title is too long.";
        }
        if (!TITLE_PATTERN.matcher(cleanLessonTitle).matches()) {
            return "Lesson title contains unsupported characters.";
        }
        if (cleanLessonTitle.matches("\\d+")) {
            return "Lesson title cannot contain only numbers.";
        }

        if (cleanOrder.isBlank()) {
            return "Order is required.";
        }
        if (!cleanOrder.matches("\\d+")) {
            return "Order must be a whole number.";
        }

        int parsedOrder = Integer.parseInt(cleanOrder);
        if (parsedOrder <= 0) {
            return "Order must be greater than 0.";
        }
        if (parsedOrder > 999) {
            return "Order is too large.";
        }

        if (cleanDuration.isBlank()) {
            return "Duration is required.";
        }
        if (!cleanDuration.matches("\\d+")) {
            return "Duration must be a whole number.";
        }

        int parsedDuration = Integer.parseInt(cleanDuration);
        if (parsedDuration <= 0) {
            return "Duration must be greater than 0.";
        }
        if (parsedDuration > 1440) {
            return "Duration is too large.";
        }

        if (cleanPdfUrl.isBlank() && cleanVideoUrl.isBlank() && cleanYoutubeUrl.isBlank()) {
            return "Please fill at least one lesson link.";
        }

        if (!cleanPdfUrl.isBlank() && !isValidPdfLink(cleanPdfUrl)) {
            return "PDF link must point to a valid PDF file.";
        }
        if (!cleanVideoUrl.isBlank() && !isValidVideoLink(cleanVideoUrl)) {
            return "Video link must be a valid video file or URL.";
        }
        if (!cleanYoutubeUrl.isBlank() && !isValidYoutubeLink(cleanYoutubeUrl)) {
            return "YouTube link must be a valid youtube.com or youtu.be URL.";
        }

        return null;
    }

    private static boolean isValidPdfLink(String value) {
        return isFileOrWebLink(value) && hasExtension(value, ".pdf");
    }

    private static boolean isValidVideoLink(String value) {
        return isFileOrWebLink(value)
                && (hasExtension(value, ".mp4", ".mkv", ".avi", ".mov", ".wmv", ".webm", ".m4v", ".mpeg")
                || value.toLowerCase(Locale.ROOT).startsWith("http://")
                || value.toLowerCase(Locale.ROOT).startsWith("https://"));
    }

    private static boolean isValidYoutubeLink(String value) {
        return YOUTUBE_PATTERN.matcher(value).matches();
    }

    private static boolean isFileOrWebLink(String value) {
        String lowerValue = value.toLowerCase(Locale.ROOT);
        return lowerValue.startsWith("http://")
                || lowerValue.startsWith("https://")
                || lowerValue.startsWith("file:/")
                || WINDOWS_PATH_PATTERN.matcher(value).matches();
    }

    private static boolean hasExtension(String value, String... extensions) {
        String normalized = stripQuery(clean(value).toLowerCase(Locale.ROOT));
        for (String extension : extensions) {
            if (normalized.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private static String stripQuery(String value) {
        int queryIndex = value.indexOf('?');
        return queryIndex >= 0 ? value.substring(0, queryIndex) : value;
    }

    private static boolean containsControlCharacters(String value) {
        return value.codePoints().anyMatch(Character::isISOControl);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
