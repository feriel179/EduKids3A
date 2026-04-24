package com.edukids.edukids3a.util;

import com.edukids.edukids3a.models.Evenement;

import java.time.LocalDate;

public final class EvenementValidator {

    private EvenementValidator() {
    }

    public static void valider(Evenement e) {
        if (e.getTitre() == null || e.getTitre().length() < 3) {
            throw new ValidationException("Le titre doit contenir au moins 3 caractères.");
        }
        if (e.getTitre().length() > 255) {
            throw new ValidationException("Le titre ne peut pas dépasser 255 caractères.");
        }
        if (e.getDescription() == null || e.getDescription().length() < 10) {
            throw new ValidationException("La description doit contenir au moins 10 caractères.");
        }
        if (e.getDateEvenement() == null) {
            throw new ValidationException("La date de l'événement est obligatoire.");
        }
        if (e.getDateEvenement().isBefore(LocalDate.now())) {
            throw new ValidationException("La date de l'événement doit être aujourd'hui ou dans le futur.");
        }
        if (e.getHeureDebut() == null || e.getHeureFin() == null) {
            throw new ValidationException("Les heures de début et de fin sont obligatoires.");
        }
        if (!e.getHeureFin().isAfter(e.getHeureDebut())) {
            throw new ValidationException("L'heure de fin doit être après l'heure de début.");
        }
        if (e.getNbPlacesDisponibles() != null && e.getNbPlacesDisponibles() < 0) {
            throw new ValidationException("Le nombre de places doit être positif ou nul.");
        }
    }
}
