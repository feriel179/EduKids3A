package com.ecom.service;

import com.ecom.model.Commande;
import com.ecom.model.CommandeStatut;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MailService {

    private static final String MAIL_CONFIG_PATH = "/config/mail.properties";
    private static final String DEFAULT_NOTIFICATION_EMAIL = "marwenhamrouni52@gmail.com";
    private final Properties properties;

    public MailService() {
        this.properties = loadProperties();
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(resolveProperty("mail.enabled", "MAIL_ENABLED", "false"));
    }

    public void sendCommandeStatusEmail(Commande commande, CommandeStatut newStatus) throws MessagingException {
        if (!isEnabled()) {
            throw new MessagingException("L'envoi d'e-mail est desactive. Activez mail.enabled=true ou la variable MAIL_ENABLED=true.");
        }

        validateConfiguration();

        String from = resolveRequiredProperty("mail.from", "MAIL_FROM");
        String recipient = getNotificationRecipient();

        if (recipient.isBlank()) {
            throw new MessagingException("Aucun destinataire n'est configure pour la notification.");
        }

        Session session = createSession();
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject("Mise a jour de commande #" + commande.getId());
        message.setText(buildMessageBody(commande, newStatus));
        try {
            Transport.send(message);
        } catch (MessagingException exception) {
            throw new MessagingException("Echec d'envoi SMTP : " + getDetailedErrorMessage(exception), exception);
        }
    }

    public String getNotificationRecipient() throws MessagingException {
        String recipient = resolveProperty("mail.notification.to", "MAIL_NOTIFICATION_TO", DEFAULT_NOTIFICATION_EMAIL);
        if (recipient == null || recipient.isBlank()) {
            throw new MessagingException("Aucun destinataire n'est configure pour la notification.");
        }
        return recipient.trim();
    }

    private void validateConfiguration() throws MessagingException {
        resolveRequiredProperty("mail.smtp.host", "MAIL_SMTP_HOST");
        resolveRequiredProperty("mail.smtp.port", "MAIL_SMTP_PORT");
        resolveRequiredProperty("mail.username", "MAIL_USERNAME");
        resolveRequiredProperty("mail.password", "MAIL_APP_PASSWORD");
        resolveRequiredProperty("mail.from", "MAIL_FROM");
        getNotificationRecipient();
    }

    private Session createSession() throws MessagingException {
        Properties smtpProperties = new Properties();
        smtpProperties.put("mail.smtp.auth", resolveProperty("mail.smtp.auth", "MAIL_SMTP_AUTH", "true"));
        smtpProperties.put("mail.smtp.starttls.enable", resolveProperty("mail.smtp.starttls.enable", "MAIL_SMTP_STARTTLS", "true"));
        smtpProperties.put("mail.smtp.host", resolveRequiredProperty("mail.smtp.host", "MAIL_SMTP_HOST"));
        smtpProperties.put("mail.smtp.port", resolveRequiredProperty("mail.smtp.port", "MAIL_SMTP_PORT"));
        smtpProperties.put("mail.smtp.connectiontimeout", "10000");
        smtpProperties.put("mail.smtp.timeout", "10000");
        smtpProperties.put("mail.smtp.writetimeout", "10000");

        final String username = resolveRequiredProperty("mail.username", "MAIL_USERNAME");
        final String password = resolveRequiredProperty("mail.password", "MAIL_APP_PASSWORD");

        return Session.getInstance(smtpProperties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private String resolveRequiredProperty(String propertyName, String envName) throws MessagingException {
        String value = resolveProperty(propertyName, envName, null);
        if (value == null || value.isBlank()) {
            throw new MessagingException("Configuration mail manquante : " + propertyName + " (ou variable " + envName + ").");
        }
        if (value.startsWith("your-") || value.contains("your-app-password") || value.contains("CHANGE_ME")) {
            throw new MessagingException("Configuration mail invalide pour " + propertyName + ". Remplacez la valeur par une vraie configuration.");
        }
        return value;
    }

    private String resolveProperty(String propertyName, String envName, String defaultValue) {
        String envValue = System.getenv(envName);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = properties.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        return defaultValue;
    }

    private Properties loadProperties() {
        Properties loadedProperties = new Properties();

        try (InputStream inputStream = MailService.class.getResourceAsStream(MAIL_CONFIG_PATH)) {
            if (inputStream == null) {
                throw new IllegalStateException("Fichier " + MAIL_CONFIG_PATH + " introuvable.");
            }
            loadedProperties.load(inputStream);
            return loadedProperties;
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de charger la configuration mail.", exception);
        }
    }

    private String buildMessageBody(Commande commande, CommandeStatut newStatus) {
        return """
                Bonjour,

                La commande a ete mise a jour.

                Details :
                - ID commande : %d
                - Produit : %s
                - Client : %s
                - Quantite : %d
                - Total : %.2f DT
                - Nouveau statut : %s

                Commentaire client :
                %s

                Message genere automatiquement par l'application Java e-commerce.
                """.formatted(
                commande.getId(),
                commande.getProduit().getNom(),
                commande.getClientName(),
                commande.getQuantite(),
                commande.getMontantTotal(),
                newStatus.getLabel(),
                commande.getCommentaire() == null || commande.getCommentaire().isBlank() ? "Aucun commentaire" : commande.getCommentaire()
        );
    }

    private String getDetailedErrorMessage(MessagingException exception) {
        List<String> messages = new ArrayList<>();
        MessagingException current = exception;

        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                messages.add(current.getMessage());
            }
            Exception next = current.getNextException();
            if (next instanceof MessagingException nextMessagingException) {
                current = nextMessagingException;
            } else {
                if (next != null && next.getMessage() != null && !next.getMessage().isBlank()) {
                    messages.add(next.getMessage());
                }
                break;
            }
        }

        return messages.isEmpty() ? "cause non detaillee" : String.join(" | ", messages);
    }
}
