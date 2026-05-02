package com.edukids.edukids3a.validation;

import com.edukids.edukids3a.model.Evenement;
import com.edukids.edukids3a.model.Programme;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EventsAndProgrammesValidatorTest {

    @Test
    void evenementValidator_acceptsValidEvent() {
        Evenement evenement = validEvent();

        assertDoesNotThrow(() -> EvenementValidator.valider(evenement));
    }

    @Test
    void evenementValidator_rejectsEndBeforeStart() {
        Evenement evenement = validEvent();
        evenement.setHeureFin(LocalTime.of(9, 0));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> EvenementValidator.valider(evenement)
        );

        assertEquals("L'heure de fin doit être après l'heure de début.", exception.getMessage());
    }

    @Test
    void evenementModel_doesNotDecrementCountersBelowZero() {
        Evenement evenement = validEvent();

        evenement.decrementLikes();
        evenement.decrementDislikes();
        evenement.decrementFavorites();

        assertEquals(0, evenement.getLikesCount());
        assertEquals(0, evenement.getDislikesCount());
        assertEquals(0, evenement.getFavoritesCount());
    }

    @Test
    void evenementModel_computesDuration() {
        Evenement evenement = validEvent();

        assertEquals(Duration.ofHours(2), evenement.getDureeEvenement());
    }

    @Test
    void programmeValidator_acceptsValidProgramme() {
        Programme programme = validProgramme();

        assertDoesNotThrow(() -> ProgrammeValidator.valider(programme));
    }

    @Test
    void programmeValidator_rejectsInvalidPauseOrder() {
        Programme programme = validProgramme();
        programme.setPauseFin(LocalTime.of(10, 15));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> ProgrammeValidator.valider(programme)
        );

        assertEquals("La fin de pause doit être après le début.", exception.getMessage());
    }

    private static Evenement validEvent() {
        Evenement evenement = new Evenement();
        evenement.setTitre("Atelier robotique");
        evenement.setDescription("Atelier pratique pour apprendre la robotique.");
        evenement.setDateEvenement(LocalDate.now().plusDays(5));
        evenement.setHeureDebut(LocalTime.of(10, 0));
        evenement.setHeureFin(LocalTime.of(12, 0));
        evenement.setNbPlacesDisponibles(20);
        return evenement;
    }

    private static Programme validProgramme() {
        Programme programme = new Programme();
        programme.setEvenement(validEvent());
        programme.setPauseDebut(LocalTime.of(10, 30));
        programme.setPauseFin(LocalTime.of(10, 45));
        programme.setActivites("Accueil, atelier, demonstration finale.");
        programme.setDocumentsRequis("Autorisation parentale et fiche enfant.");
        programme.setMaterielsRequis("Ordinateur portable et cahier de notes.");
        return programme;
    }
}
