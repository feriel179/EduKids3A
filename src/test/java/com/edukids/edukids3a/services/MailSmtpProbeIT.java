package com.edukids.edukids3a.services;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test d’intégration optionnel : lit {@code MAILER_DSN} depuis {@code .env.local} / {@code .env}
 * à la racine du projet (répertoire de travail Maven = base du module).
 * <p>
 * Exécution : {@code mvn test -Dtest=MailSmtpProbeIT}
 */
class MailSmtpProbeIT {

    @Test
    void probeSmtp_succeedsWhenMailerDsnConfigured() {
        EvenementNotificationMailService svc = new EvenementNotificationMailService();
        String err = svc.probeSmtpConnection();
        Assumptions.assumeFalse(
                "MAILER_DSN absent".equals(err),
                "MAILER_DSN absent — test ignoré (ajoutez .env.local ou .env à la racine du projet)"
        );
        assertNull(err, "Connexion SMTP refusée ou DSN invalide : " + err);
    }
}
