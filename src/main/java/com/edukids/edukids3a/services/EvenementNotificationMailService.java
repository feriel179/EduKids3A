package com.edukids.edukids3a.services;

import com.edukids.edukids3a.models.Evenement;
import com.edukids.edukids3a.util.DotEnvLoader;
import com.edukids.edukids3a.util.JdbcAuthDataSource;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

/**
 * Notifications par e-mail lors de la création d’un événement (aligné sur le back-office web Symfony).
 * <p>
 * Configuration : {@code MAILER_DSN} (format Symfony {@code smtp://user:pass@host:port?...}) et optionnellement
 * {@code MAIL_FROM} (adresse seule ou {@code Nom <email@domaine>}).
 */
public final class EvenementNotificationMailService {

    private static final Logger LOG = LoggerFactory.getLogger(EvenementNotificationMailService.class);
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy").withLocale(Locale.FRENCH);
    private static final DateTimeFormatter TIME_FR = DateTimeFormatter.ofPattern("HH:mm");

    public record MailReport(int sent, int failed, int totalDestinataires) {
    }

    /**
     * Ouvre une session SMTP authentifiée puis la ferme, sans envoyer de message.
     * Utile pour valider {@code MAILER_DSN} / {@code MAIL_FROM}.
     *
     * @return {@code null} si la connexion réussit ; {@code "MAILER_DSN absent"} si aucune config ;
     *         sinon un message d’erreur (parse DSN ou authentification SMTP).
     */
    public String probeSmtpConnection() {
        String dsn = DotEnvLoader.get("MAILER_DSN");
        if (dsn == null || dsn.isBlank()) {
            return "MAILER_DSN absent";
        }
        SmtpConfig cfg;
        try {
            cfg = SmtpConfig.parse(dsn, DotEnvLoader.get("MAIL_FROM"));
        } catch (Exception ex) {
            return ex.getMessage();
        }
        try {
            Session session = buildSmtpSession(cfg);
            try (Transport transport = session.getTransport("smtp")) {
                transport.connect(cfg.host(), cfg.port(), cfg.user(), cfg.password());
            }
            LOG.info("Probe SMTP OK vers {}:{}", cfg.host(), cfg.port());
            return null;
        } catch (Exception ex) {
            LOG.warn("Probe SMTP échouée : {}", ex.getMessage());
            return ex.getMessage();
        }
    }

    /**
     * Envoie un e-mail à chaque utilisateur actif (table {@code user}), en excluant éventuellement le créateur.
     */
    public MailReport notifyNewEvent(Evenement evenement, Integer excludeUserId) {
        String dsn = DotEnvLoader.get("MAILER_DSN");
        if (dsn == null || dsn.isBlank()) {
            LOG.info("MAILER_DSN absent — notification nouvel événement ignorée.");
            return new MailReport(0, 0, 0);
        }
        SmtpConfig cfg;
        try {
            cfg = SmtpConfig.parse(dsn, DotEnvLoader.get("MAIL_FROM"));
        } catch (Exception ex) {
            LOG.warn("MAILER_DSN invalide — notification ignorée : {}", ex.getMessage());
            return new MailReport(0, 0, 0);
        }

        List<Destinataire> destinataires;
        try {
            destinataires = listerDestinatairesActifs(excludeUserId);
        } catch (Exception ex) {
            LOG.error("Impossible de lire les destinataires pour la notification événement", ex);
            return new MailReport(0, 0, 0);
        }
        if (destinataires.isEmpty()) {
            LOG.info("Aucun utilisateur actif avec e-mail pour la notification.");
            return new MailReport(0, 0, 0);
        }

        Session session = buildSmtpSession(cfg);

        int sent = 0;
        int failed = 0;
        Set<String> dejaEnvoye = new HashSet<>();

        for (int i = 0; i < destinataires.size(); i++) {
            Destinataire d = destinataires.get(i);
            String addr = d.email() == null ? "" : d.email().trim().toLowerCase(Locale.ROOT);
            if (addr.isEmpty() || !addr.contains("@") || dejaEnvoye.contains(addr)) {
                continue;
            }
            if (addr.equalsIgnoreCase(cfg.fromAddress())) {
                continue;
            }
            dejaEnvoye.add(addr);
            try {
                MimeMessage msg = new MimeMessage(session);
                msg.setFrom(new InternetAddress(cfg.fromAddress(), cfg.fromPersonal(), "UTF-8"));
                msg.setRecipient(Message.RecipientType.TO, new InternetAddress(d.email(), d.prenomOuEmail(), "UTF-8"));
                msg.setSubject(sujetNotification(evenement), "UTF-8");
                msg.setContent(buildHtmlBody(evenement, d), "text/html; charset=UTF-8");

                try (Transport transport = session.getTransport("smtp")) {
                    transport.connect(cfg.host(), cfg.port(), cfg.user(), cfg.password());
                    transport.sendMessage(msg, msg.getAllRecipients());
                }
                sent++;
                if (i < destinataires.size() - 1) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception ex) {
                failed++;
                LOG.warn("Échec envoi notification à {} : {}", addr, ex.getMessage());
            }
        }
        return new MailReport(sent, failed, destinataires.size());
    }

    private static Session buildSmtpSession(SmtpConfig cfg) {
        Properties props = new Properties();
        props.put("mail.smtp.host", cfg.host());
        props.put("mail.smtp.port", String.valueOf(cfg.port()));
        props.put("mail.smtp.auth", "true");
        if (cfg.ssl()) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", cfg.host());
        } else if (cfg.startTls()) {
            props.put("mail.smtp.starttls.enable", "true");
        }
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(cfg.user(), cfg.password());
            }
        });
    }

    private static String titreSafe(Evenement e) {
        String t = e.getTitre();
        return t == null ? "Événement" : t.trim();
    }

    /** Sujet court, lisible sur mobile. */
    private static String sujetNotification(Evenement e) {
        String base = "EduKids · Nouvelle activité : " + titreSafe(e);
        if (base.length() > 180) {
            return base.substring(0, 177) + "…";
        }
        return base;
    }

    private static String mailDetailRow(String label, String valueEscaped) {
        return "<tr>"
                + "<td style=\"padding:12px 16px;background:#f1f5f9;width:118px;vertical-align:top;font-size:13px;font-weight:600;color:#475569;border-bottom:1px solid #e2e8f0;\">"
                + label
                + "</td>"
                + "<td style=\"padding:12px 16px;vertical-align:top;font-size:15px;color:#0f172a;border-bottom:1px solid #e2e8f0;\">"
                + valueEscaped
                + "</td>"
                + "</tr>";
    }

    private static String buildHtmlBody(Evenement e, Destinataire d) {
        String prenom = escapeHtml(d.prenomOuEmail());
        String titre = escapeHtml(titreSafe(e));

        String desc = e.getDescription() == null ? "" : e.getDescription().trim();
        if (desc.length() > 450) {
            desc = desc.substring(0, 447) + "…";
        }
        desc = escapeHtml(desc);

        String dateStr = e.getDateEvenement() != null ? e.getDateEvenement().format(DATE_FR) : "—";
        String horaires = (e.getHeureDebut() != null && e.getHeureFin() != null)
                ? e.getHeureDebut().format(TIME_FR) + " – " + e.getHeureFin().format(TIME_FR)
                : "—";
        String lieu = e.getLocalisation() == null || e.getLocalisation().isBlank() ? "—" : escapeHtml(e.getLocalisation().trim());

        String typeRow = "";
        String type = e.getTypeEvenement();
        if (type != null && !type.isBlank()) {
            typeRow = mailDetailRow("Type", escapeHtml(type.trim()));
        }

        String blocDescription = "";
        if (!desc.isEmpty()) {
            blocDescription = "<div style=\"margin:20px 0 0;padding:16px 18px;background:#fafafa;border-radius:10px;border-left:4px solid #6366f1;\">"
                    + "<p style=\"margin:0 0 8px;font-size:12px;font-weight:700;color:#6366f1;text-transform:uppercase;letter-spacing:0.06em;\">À propos de l’activité</p>"
                    + "<p style=\"margin:0;font-size:15px;line-height:1.55;color:#334155;\">" + desc.replace("\n", "<br/>") + "</p>"
                    + "</div>";
        }

        return "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"/>"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"/>"
                + "<title>Nouvelle activité EduKids</title></head>"
                + "<body style=\"margin:0;padding:0;background-color:#e2e8f0;\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color:#e2e8f0;\">"
                + "<tr><td align=\"center\" style=\"padding:28px 14px;\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width:560px;border-radius:16px;overflow:hidden;"
                + "box-shadow:0 10px 40px rgba(15,23,42,0.12);background-color:#ffffff;\">"
                + "<tr><td bgcolor=\"#4f46e5\" style=\"background-color:#4f46e5;background-image:linear-gradient(135deg,#4f46e5 0%,#7c3aed 55%,#a855f7 100%);"
                + "padding:32px 28px;text-align:center;\">"
                + "<div style=\"font-size:26px;font-weight:800;color:#ffffff;letter-spacing:-0.03em;line-height:1.1;\">EduKids</div>"
                + "<div style=\"margin-top:10px;font-size:15px;color:rgba(255,255,255,0.92);line-height:1.45;\">Une nouvelle activité vient d’être publiée</div>"
                + "</td></tr>"
                + "<tr><td style=\"padding:28px 28px 8px;\">"
                + "<p style=\"margin:0 0 14px;font-size:17px;line-height:1.5;color:#0f172a;font-family:Segoe UI,Roboto,Helvetica Neue,Arial,sans-serif;\">"
                + "Bonjour <strong style=\"color:#3730a3;\">" + prenom + "</strong>,</p>"
                + "<p style=\"margin:0;font-size:15px;line-height:1.6;color:#475569;font-family:Segoe UI,Roboto,Helvetica Neue,Arial,sans-serif;\">"
                + "Nous avons le plaisir de vous informer qu’une <strong>nouvelle activité</strong> est disponible sur la plateforme. "
                + "Vous pouvez la consulter ou vous inscrire depuis l’application EduKids (espace famille ou administration).</p>"
                + "</td></tr>"
                + "<tr><td style=\"padding:8px 28px 24px;\">"
                + "<p style=\"margin:0 0 12px;font-size:13px;font-weight:700;color:#64748b;text-transform:uppercase;letter-spacing:0.08em;font-family:Segoe UI,Roboto,Helvetica Neue,Arial,sans-serif;\">"
                + "Résumé de l’activité</p>"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"border-collapse:collapse;border-radius:12px;overflow:hidden;"
                + "border:1px solid #e2e8f0;font-family:Segoe UI,Roboto,Helvetica Neue,Arial,sans-serif;\">"
                + mailDetailRow("Titre", titre)
                + typeRow
                + mailDetailRow("Date", escapeHtml(dateStr))
                + mailDetailRow("Horaires", escapeHtml(horaires))
                + mailDetailRow("Lieu", lieu)
                + "</table>"
                + blocDescription
                + "</td></tr>"
                + "<tr><td style=\"padding:0 28px 28px;\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background:linear-gradient(135deg,#eef2ff,#faf5ff);border-radius:12px;\">"
                + "<tr><td style=\"padding:18px 20px;\">"
                + "<p style=\"margin:0;font-size:14px;line-height:1.55;color:#4338ca;font-family:Segoe UI,Roboto,Helvetica Neue,Arial,sans-serif;\">"
                + "<strong>Prochaine étape :</strong> ouvrez l’application EduKids pour voir tous les détails, les places disponibles et les actions possibles (inscription, favoris, etc.).</p>"
                + "</td></tr></table>"
                + "</td></tr>"
                + "<tr><td style=\"padding:0 28px 24px;border-top:1px solid #f1f5f9;\">"
                + "<p style=\"margin:20px 0 0;font-size:12px;line-height:1.5;color:#94a3b8;text-align:center;font-family:Segoe UI,Roboto,Helvetica Neue,Arial,sans-serif;\">"
                + "Ce message est envoyé automatiquement par <strong style=\"color:#64748b;\">EduKids</strong> lors de la publication d’une activité.<br/>"
                + "Merci de ne pas répondre directement à cet e-mail si la boîte n’est pas surveillée.</p>"
                + "</td></tr>"
                + "</table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private static String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private record Destinataire(String email, String prenom) {
        String prenomOuEmail() {
            if (prenom != null && !prenom.isBlank()) {
                return prenom.trim();
            }
            return email;
        }
    }

    private static List<Destinataire> listerDestinatairesActifs(Integer excludeUserId) throws Exception {
        List<Destinataire> list = new ArrayList<>();
        try (Connection c = JdbcAuthDataSource.getConnection()) {
            String sql = "SELECT email, first_name FROM `user` WHERE is_active = 1 AND email IS NOT NULL AND TRIM(email) <> ''";
            if (excludeUserId != null && excludeUserId > 0) {
                sql += " AND id <> ?";
            }
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                if (excludeUserId != null && excludeUserId > 0) {
                    ps.setInt(1, excludeUserId);
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Destinataire(rs.getString("email"), rs.getString("first_name")));
                    }
                }
            }
        }
        return list;
    }

    private record SmtpConfig(String host, int port, String user, String password, boolean ssl, boolean startTls,
                              String fromAddress, String fromPersonal) {

        static SmtpConfig parse(String dsn, String mailFromOverride) throws Exception {
            String raw = dsn.trim();
            if (!raw.startsWith("smtp://") && !raw.startsWith("smtps://")) {
                throw new IllegalArgumentException("MAILER_DSN doit commencer par smtp:// ou smtps://");
            }
            boolean smtpsScheme = raw.startsWith("smtps://");
            URI uri = new URI(raw);
            String userInfo = uri.getUserInfo();
            if (userInfo == null || !userInfo.contains(":")) {
                throw new IllegalArgumentException("MAILER_DSN : identifiants manquants (user:pass)");
            }
            int colon = userInfo.indexOf(':');
            String user = userInfo.substring(0, colon);
            String pass = userInfo.substring(colon + 1);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                throw new IllegalArgumentException("MAILER_DSN : hôte manquant");
            }
            int port = uri.getPort();
            if (port <= 0) {
                port = smtpsScheme ? 465 : 587;
            }
            String query = uri.getRawQuery();
            boolean encryptionSmtps = smtpsScheme || (query != null && query.contains("encryption=smtps"));
            boolean ssl = port == 465 || encryptionSmtps;
            boolean startTls = !ssl && (port == 587 || (query != null && query.contains("encryption=tls")));

            String fromAddr;
            String fromName = "EduKids — Événements";
            if (mailFromOverride != null && !mailFromOverride.isBlank()) {
                String mf = mailFromOverride.trim();
                int lt = mf.indexOf('<');
                int gt = mf.indexOf('>');
                if (lt > 0 && gt > lt) {
                    fromName = mf.substring(0, lt).trim();
                    fromAddr = mf.substring(lt + 1, gt).trim();
                } else {
                    fromAddr = mf;
                }
            } else {
                fromAddr = user;
            }
            return new SmtpConfig(host, port, user, pass, ssl, startTls, fromAddr, fromName);
        }
    }
}
