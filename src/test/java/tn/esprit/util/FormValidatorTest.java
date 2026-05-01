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
}
