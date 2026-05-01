package com.edukids.edukids3a.validation;

import com.edukids.edukids3a.model.Programme;

public final class ProgrammeValidator {

    private ProgrammeValidator() {
    }

    public static void valider(Programme p) {
        if (p.getEvenement() == null) {
            throw new ValidationException("Choisissez un événement.");
        }
        if (p.getPauseDebut() == null || p.getPauseFin() == null) {
            throw new ValidationException("Les heures de pause sont obligatoires.");
        }
        if (!p.getPauseFin().isAfter(p.getPauseDebut())) {
            throw new ValidationException("La fin de pause doit être après le début.");
        }
        if (p.getActivites() == null || p.getActivites().length() < 10) {
            throw new ValidationException("Les activités doivent contenir au moins 10 caractères.");
        }
        if (p.getDocumentsRequis() == null || p.getDocumentsRequis().length() < 10) {
            throw new ValidationException("Les documents requis doivent contenir au moins 10 caractères.");
        }
        if (p.getMaterielsRequis() == null || p.getMaterielsRequis().length() < 10) {
            throw new ValidationException("Les matériels requis doivent contenir au moins 10 caractères.");
        }
    }
}
