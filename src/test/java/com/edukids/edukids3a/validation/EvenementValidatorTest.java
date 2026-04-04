package com.edukids.edukids3a.validation;

import com.edukids.edukids3a.model.Evenement;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EvenementValidatorTest {

    @Test
    void titreTropCourt_rejete() {
        Evenement e = evenementValide();
        e.setTitre("ab");
        assertThrows(ValidationException.class, () -> EvenementValidator.valider(e));
    }

    @Test
    void evenementValide_accepte() {
        assertDoesNotThrow(() -> EvenementValidator.valider(evenementValide()));
    }

    @Test
    void finAvantDebut_rejete() {
        Evenement e = evenementValide();
        e.setHeureDebut(LocalTime.of(14, 0));
        e.setHeureFin(LocalTime.of(9, 0));
        assertThrows(ValidationException.class, () -> EvenementValidator.valider(e));
    }

    private static Evenement evenementValide() {
        Evenement e = new Evenement();
        e.setTitre("Titre valide");
        e.setDescription("Description assez longue pour passer.");
        e.setDateEvenement(LocalDate.now().plusDays(1));
        e.setHeureDebut(LocalTime.of(9, 0));
        e.setHeureFin(LocalTime.of(12, 0));
        return e;
    }
}
