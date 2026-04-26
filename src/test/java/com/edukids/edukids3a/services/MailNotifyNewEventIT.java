package com.edukids.edukids3a.services;

import com.edukids.edukids3a.models.Evenement;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Envoie de vrais e-mails comme après création d’un événement (sans passer par l’UI).
 * <p>
 * <strong>À n’exécuter que localement :</strong> notifie tous les utilisateurs actifs ayant un e-mail
 * (comme {@code notifyNewEvent} sans exclusion du créateur).
 * <pre>mvn test -Dtest=MailNotifyNewEventIT "-Dedukids.mail.e2e=true"</pre>
 * (Sous PowerShell, entourer {@code -Dedukids...} de guillemets.)
 */
@EnabledIfSystemProperty(named = "edukids.mail.e2e", matches = "true")
class MailNotifyNewEventIT {

    @Test
    void notifyNewEvent_likeAfterCreatingEvent() {
        EvenementNotificationMailService svc = new EvenementNotificationMailService();
        String probe = svc.probeSmtpConnection();
        Assumptions.assumeFalse(
                "MAILER_DSN absent".equals(probe),
                "MAILER_DSN absent — configurez .env.local"
        );
        Assumptions.assumeTrue(
                probe == null,
                "SMTP invalide ou connexion refusée : " + probe
        );

        Evenement e = new Evenement();
        e.setTitre("Test E2E notification (" + System.currentTimeMillis() + ")");
        e.setDescription("Message généré par MailNotifyNewEventIT — vous pouvez supprimer cet événement s’il a été créé en base.");
        e.setDateEvenement(LocalDate.now().plusDays(7));
        e.setHeureDebut(LocalTime.of(10, 0));
        e.setHeureFin(LocalTime.of(12, 0));
        e.setLocalisation("Salle test");
        e.setTypeEvenement("ATELIER");

        EvenementNotificationMailService.MailReport rep = svc.notifyNewEvent(e, null);

        assertTrue(rep.totalDestinataires() > 0,
                "Aucun utilisateur actif avec e-mail en base — impossible de vérifier la réception.");

        assertTrue(rep.sent() > 0 || rep.failed() > 0,
                "Aucune tentative d’envoi (sent=0, failed=0). Souvent : tous les e-mails des utilisateurs "
                        + "sont identiques à l’adresse d’expédition (MAIL_FROM / compte SMTP), elles sont ignorées.");

        assertTrue(rep.sent() > 0,
                "Échec pour tous les destinataires : " + rep.failed() + " échec(s) sur " + rep.totalDestinataires()
                        + ". Voir les logs WARN pour le détail SMTP.");
    }
}
