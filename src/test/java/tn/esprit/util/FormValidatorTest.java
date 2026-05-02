package tn.esprit.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class FormValidatorTest {

    @Test
    void validateCourse_acceptsUnicodeSubjectWithDash() {
        String validationMessage = FormValidator.validateCourse(
                "Algebre pour debutants",
                "MATH\u00C9MATIQUES \u2013 Niveau 1",
                "Une introduction claire aux nombres et aux operations."
        );

        assertNull(validationMessage);
    }

    @Test
    void validateCourse_rejectsSubjectMadeOnlyOfNumbers() {
        String validationMessage = FormValidator.validateCourse(
                "Algebre pour debutants",
                "12345",
                "Une introduction claire aux nombres et aux operations."
        );

        assertEquals("Subject cannot contain only numbers.", validationMessage);
    }

    @Test
    void validateCourse_rejectsUnsupportedTitleCharacters() {
        String validationMessage = FormValidator.validateCourse(
                "Java <script>",
                "Programmation",
                "Une introduction claire aux bases de Java."
        );

        assertEquals("Course title contains unsupported characters.", validationMessage);
    }

    @Test
    void validateLesson_acceptsPdfAndYoutubeLinks() {
        String validationMessage = FormValidator.validateLesson(
                "Introduction Java",
                "1",
                "45",
                "https://example.com/support.pdf",
                "",
                "https://www.youtube.com/watch?v=abc123"
        );

        assertNull(validationMessage);
    }

    @Test
    void validateLesson_rejectsMissingAllMediaLinks() {
        String validationMessage = FormValidator.validateLesson(
                "Introduction Java",
                "1",
                "45",
                "",
                "",
                ""
        );

        assertEquals("Please fill at least one lesson link.", validationMessage);
    }

    @Test
    void validateLesson_rejectsInvalidYoutubeLink() {
        String validationMessage = FormValidator.validateLesson(
                "Introduction Java",
                "1",
                "45",
                "",
                "",
                "https://example.com/watch?v=abc123"
        );

        assertEquals("YouTube link must be a valid youtube.com or youtu.be URL.", validationMessage);
    }
}
