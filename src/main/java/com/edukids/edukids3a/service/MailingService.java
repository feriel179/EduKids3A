package com.edukids.edukids3a.service;

import jakarta.activation.DataHandler;
import jakarta.activation.FileDataSource;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class MailingService {
    private static final Path LOCAL_CONFIG_PATH = Path.of("data", "mail.properties");

    public boolean hasConfiguration() {
        try {
            resolveConfiguration();
            return true;
        } catch (IllegalStateException ex) {
            return false;
        }
    }

    public void sendEmailWithAttachment(String to, String subject, String body, Path attachmentPath) throws MessagingException {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("L'adresse email du destinataire est requise.");
        }
        if (attachmentPath == null || !Files.exists(attachmentPath)) {
            throw new IllegalArgumentException("Le fichier a envoyer est introuvable.");
        }

        MailConfiguration configuration = resolveConfiguration();
        MessagingException lastError = null;

        for (MailConfiguration candidate : buildCandidates(configuration)) {
            try {
                sendWithConfiguration(candidate, to.trim(), subject, body, attachmentPath);
                return;
            } catch (MessagingException ex) {
                lastError = ex;
            }
        }

        if (lastError != null) {
            throw lastError;
        }
    }

    private void sendWithConfiguration(
            MailConfiguration configuration,
            String to,
            String subject,
            String body,
            Path attachmentPath
    ) throws MessagingException {
        Session session = createSession(configuration);
        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(configuration.from()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject == null || subject.isBlank() ? "Rapport EduKids" : subject.trim(), "UTF-8");

        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(body == null || body.isBlank() ? "Bonjour,\n\nVeuillez trouver ci-joint le rapport EduKids.\n" : body, "UTF-8");

        MimeBodyPart attachmentPart = new MimeBodyPart();
        FileDataSource dataSource = new FileDataSource(attachmentPath.toFile());
        attachmentPart.setDataHandler(new DataHandler(dataSource));
        attachmentPart.setFileName(attachmentPath.getFileName().toString());

        MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(textPart);
        multipart.addBodyPart(attachmentPart);
        message.setContent(multipart);

        Transport.send(message);
    }

    private Session createSession(MailConfiguration configuration) {
        Properties sessionProperties = new Properties();
        sessionProperties.put("mail.smtp.host", configuration.host());
        sessionProperties.put("mail.smtp.port", String.valueOf(configuration.port()));
        sessionProperties.put("mail.smtp.auth", String.valueOf(configuration.auth()));
        sessionProperties.put("mail.smtp.starttls.enable", String.valueOf(configuration.starttls()));
        sessionProperties.put("mail.smtp.ssl.enable", String.valueOf(configuration.ssl()));
        sessionProperties.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        sessionProperties.put("mail.smtp.ssl.trust", configuration.host());
        sessionProperties.put("mail.smtp.connectiontimeout", "20000");
        sessionProperties.put("mail.smtp.timeout", "20000");
        sessionProperties.put("mail.smtp.writetimeout", "20000");

        if (configuration.ssl()) {
            sessionProperties.put("mail.smtp.socketFactory.port", String.valueOf(configuration.port()));
            sessionProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            sessionProperties.put("mail.smtp.socketFactory.fallback", "false");
        }

        if (configuration.auth()) {
            return Session.getInstance(sessionProperties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(configuration.username(), configuration.password());
                }
            });
        }
        return Session.getInstance(sessionProperties);
    }

    private List<MailConfiguration> buildCandidates(MailConfiguration baseConfiguration) {
        List<MailConfiguration> candidates = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        addCandidate(candidates, seen, baseConfiguration);

        if ("smtp.gmail.com".equalsIgnoreCase(baseConfiguration.host())) {
            addCandidate(candidates, seen, baseConfiguration.withTransportProfile(587, true, false));
            addCandidate(candidates, seen, baseConfiguration.withTransportProfile(465, false, true));
        }

        return candidates;
    }

    private void addCandidate(List<MailConfiguration> candidates, Set<String> seen, MailConfiguration candidate) {
        String key = candidate.host() + "|" + candidate.port() + "|" + candidate.starttls() + "|" + candidate.ssl();
        if (seen.add(key)) {
            candidates.add(candidate);
        }
    }

    private MailConfiguration resolveConfiguration() {
        String host = readConfigValue("smtp.host", "EDUKIDS_SMTP_HOST");
        String username = readConfigValue("smtp.username", "EDUKIDS_SMTP_USERNAME");
        String password = readConfigValue("smtp.password", "EDUKIDS_SMTP_PASSWORD");
        String from = readConfigValue("smtp.from", "EDUKIDS_SMTP_FROM");
        int port = parsePort(readConfigValue("smtp.port", "EDUKIDS_SMTP_PORT"));
        boolean auth = parseBoolean(readConfigValue("smtp.auth", "EDUKIDS_SMTP_AUTH"), true);
        boolean starttls = parseBoolean(readConfigValue("smtp.starttls", "EDUKIDS_SMTP_STARTTLS"), true);
        boolean ssl = parseBoolean(readConfigValue("smtp.ssl", "EDUKIDS_SMTP_SSL"), false);

        if (host.isBlank()) {
            throw new IllegalStateException("Le serveur SMTP n'est pas configure.");
        }
        if (from.isBlank()) {
            from = username;
        }
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("L'adresse email expediteur n'est pas configuree.");
        }
        if (auth && (username == null || username.isBlank() || password == null || password.isBlank())) {
            throw new IllegalStateException("Le compte SMTP est incomplet.");
        }

        return new MailConfiguration(host.trim(), port, safe(username), safe(password), from.trim(), auth, starttls, ssl);
    }

    private String readConfigValue(String propertyKey, String envKey) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        Properties properties = loadProperties();
        return properties.getProperty(propertyKey, "").trim();
    }

    private Properties loadProperties() {
        Properties properties = new Properties();
        if (!Files.exists(LOCAL_CONFIG_PATH)) {
            return properties;
        }
        try (Reader reader = Files.newBufferedReader(LOCAL_CONFIG_PATH)) {
            properties.load(reader);
        } catch (IOException ignored) {
        }
        return properties;
    }

    private int parsePort(String portValue) {
        if (portValue == null || portValue.isBlank()) {
            return 587;
        }
        try {
            return Integer.parseInt(portValue.trim());
        } catch (NumberFormatException ex) {
            return 587;
        }
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record MailConfiguration(
            String host,
            int port,
            String username,
            String password,
            String from,
            boolean auth,
            boolean starttls,
            boolean ssl
    ) {
        private MailConfiguration withTransportProfile(int updatedPort, boolean updatedStarttls, boolean updatedSsl) {
            return new MailConfiguration(host, updatedPort, username, password, from, auth, updatedStarttls, updatedSsl);
        }
    }
}
