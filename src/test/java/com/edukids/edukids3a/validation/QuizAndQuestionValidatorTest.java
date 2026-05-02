package com.edukids.edukids3a.validation;

import com.edukids.edukids3a.model.Question;
import com.edukids.edukids3a.model.Quiz;
import com.edukids.edukids3a.model.Reponse;
import com.edukids.edukids3a.model.TypeQuestion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuizAndQuestionValidatorTest {

    private final QuizValidator quizValidator = new QuizValidator();
    private final QuestionValidator questionValidator = new QuestionValidator();

    @Test
    void quizValidator_acceptsValidQuiz() {
        assertDoesNotThrow(() -> quizValidator.validate(validQuiz()));
    }

    @Test
    void quizValidator_rejectsScoreOutsideRange() {
        Quiz quiz = new Quiz("Maths faciles", "Description complete", null, "Facile", "8-10 ans", 0, 20, 101, "Actif");

        ValidationException exception = assertThrows(ValidationException.class, () -> quizValidator.validate(quiz));

        assertEquals("Le score minimum doit etre entre 0 et 100.", exception.getMessage());
    }

    @Test
    void quizModel_keepsQuestionCountNonNegative() {
        Quiz quiz = validQuiz();

        quiz.setNombreQuestions(-10);
        quiz.decrementNombreQuestions();

        assertEquals(0, quiz.getNombreQuestions());
    }

    @Test
    void questionValidator_acceptsSingleChoiceWithOneCorrectAnswer() {
        Question question = new Question(validQuiz(), "Quelle reponse est correcte ?", TypeQuestion.QCU, 2, List.of(
                new Reponse("Bonne", true),
                new Reponse("Mauvaise", false)
        ));

        assertDoesNotThrow(() -> questionValidator.validate(question));
    }

    @Test
    void questionValidator_rejectsSingleChoiceWithMultipleCorrectAnswers() {
        Question question = new Question(validQuiz(), "Quelle reponse est correcte ?", TypeQuestion.QCU, 2, List.of(
                new Reponse("Bonne 1", true),
                new Reponse("Bonne 2", true)
        ));

        ValidationException exception = assertThrows(ValidationException.class, () -> questionValidator.validate(question));

        assertEquals("Cette question doit avoir une seule bonne reponse.", exception.getMessage());
    }

    @Test
    void questionValidator_acceptsMatchingQuestionWithTwoPairs() {
        Question question = new Question(validQuiz(), "Relie chaque mot a sa traduction", TypeQuestion.RELIER_FLECHE, 3, List.of(
                new Reponse("Chat|||Cat", true),
                new Reponse("Chien|||Dog", true)
        ));

        assertDoesNotThrow(() -> questionValidator.validate(question));
    }

    @Test
    void questionModel_formatsAnswerSummary() {
        Question question = new Question(validQuiz(), "Relie chaque mot a sa traduction", TypeQuestion.RELIER_FLECHE, 3, List.of(
                new Reponse("Chat|||Cat", true),
                new Reponse("Chien|||Dog", true)
        ));

        assertEquals("Chat -> Cat | Chien -> Dog", question.getResumeReponses());
    }

    private static Quiz validQuiz() {
        return new Quiz("Maths faciles", "Description complete", null, "Facile", "8-10 ans", 2, 20, 60, "Actif");
    }
}
