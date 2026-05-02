package com.edukids.edukids3a.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventDomainModelsTest {

    @Test
    void reservationComputesTotalPlacesAndDefaultsDate() {
        Reservation reservation = new Reservation();
        reservation.setNbAdultes(2);
        reservation.setNbEnfants(3);

        assertEquals(5, reservation.getNbPlacesTotal());
        assertNotNull(reservation.getDateReservation());
    }

    @Test
    void typeEvenementResolvesCodesAndKeepsDisplayOrder() {
        Map<String, String> labels = TypeEvenement.libellesParCode();

        assertEquals(TypeEvenement.SPORT, TypeEvenement.fromCode(" sport "));
        assertNull(TypeEvenement.fromCode("inconnu"));
        assertTrue(labels.containsKey("sport"));
        assertEquals(TypeEvenement.values().length, labels.size());
    }

    @Test
    void userEventInteractionDefaultsCreatedAtAndConstants() {
        UserEvenementInteraction interaction = new UserEvenementInteraction();
        interaction.setTypeInteraction(UserEvenementInteraction.TYPE_FAVORITE);
        LocalDateTime now = LocalDateTime.now();
        interaction.setCreatedAt(now);

        assertEquals("favorite", interaction.getTypeInteraction());
        assertEquals(now, interaction.getCreatedAt());
    }

    @Test
    void utilisateurDefaultsActiveAndStoresReservations() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail("ada@example.com");
        utilisateur.setRoles("[\"ROLE_ELEVE\"]");
        utilisateur.getReservations().add(new Reservation());

        assertTrue(utilisateur.isActive());
        assertFalse(utilisateur.isVerified());
        assertEquals("ada@example.com", utilisateur.getEmail());
        assertEquals(1, utilisateur.getReservations().size());
    }

    @Test
    void frontUserResolvesRolesAndNames() {
        User user = new User(1, "ada@example.com", "[\"ROLE_ADMIN\"]", "pwd", "Ada", "Lovelace", true);

        assertEquals("admin", user.getRole());
        assertEquals("Ada Lovelace", user.getNom());

        user.setNom("Grace Hopper");

        assertEquals("Grace", user.getFirstName());
        assertEquals("Hopper", user.getLastName());
    }

    @Test
    void quizResultExposesConstructorValues() {
        QuizResult result = new QuizResult(1, 2, "Math", 80, 8, 10, "2026-05-02");

        assertEquals(1, result.getId());
        assertEquals(2, result.getQuizId());
        assertEquals("Math", result.getQuizTitre());
        assertEquals(80, result.getFinalScore());
        assertEquals(8, result.getEarnedPoints());
        assertEquals(10, result.getTotalPoints());
        assertEquals("2026-05-02", result.getCompletedAt());
    }
}
