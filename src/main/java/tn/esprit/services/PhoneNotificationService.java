package tn.esprit.services;

import tn.esprit.models.Course;
import tn.esprit.util.AppSettings;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PhoneNotificationService {
    private static final String TWILIO_BASE_URL = "https://api.twilio.com/2010-04-01";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public NotificationResult sendCoursePublishedNotification(Course course) {
        if (!isEnabled()) {
            return NotificationResult.skipped("");
        }

        String validationError = validateConfiguration();
        if (validationError != null) {
            return NotificationResult.failed("Phone notification not sent: " + validationError);
        }

        String accountSid = readSetting("TWILIO_ACCOUNT_SID");
        String username = readSetting("TWILIO_API_KEY");
        String password = readSetting("TWILIO_API_SECRET");

        if (username.isBlank() || password.isBlank()) {
            username = accountSid;
            password = readSetting("TWILIO_AUTH_TOKEN");
        }

        List<String> recipients = readRecipients();
        int sentCount = 0;
        for (String recipient : recipients) {
            Map<String, String> formData = new LinkedHashMap<>();
            formData.put("From", readSetting("TWILIO_FROM_PHONE", "TWILIO_FROM_NUMBER"));
            formData.put("To", recipient);
            formData.put("Body", buildMessageBody(course));

            HttpRequest request = HttpRequest.newBuilder(URI.create(TWILIO_BASE_URL + "/Accounts/" + accountSid + "/Messages.json"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", basicAuthHeader(username, password))
                    .POST(HttpRequest.BodyPublishers.ofString(toFormBody(formData), StandardCharsets.UTF_8))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    return NotificationResult.failed("Phone notification not sent: Twilio returned HTTP " + response.statusCode() + ".");
                }
                sentCount++;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return NotificationResult.failed("Phone notification interrupted.");
            } catch (IOException exception) {
                return NotificationResult.failed("Phone notification not sent: unable to reach Twilio.");
            }
        }

        String successMessage = sentCount == 1
                ? "Phone notification sent to " + maskPhone(recipients.getFirst()) + "."
                : "Phone notification sent to " + sentCount + " recipients.";
        return NotificationResult.sent(successMessage);
    }

    public String describeStatus() {
        if (!isEnabled()) {
            return "SMS alerts are disabled.";
        }

        String validationError = validateConfiguration();
        if (validationError != null) {
            return "SMS alerts enabled, but setup is incomplete.";
        }

        List<String> recipients = readRecipients();
        if (recipients.size() == 1) {
        return "SMS alerts ready for " + maskPhone(recipients.getFirst()) + " when a course is published.";
    }
        return "SMS alerts ready for " + recipients.size() + " recipients when a course is published.";
    }

    public boolean isEnabled() {
        return Boolean.parseBoolean(AppSettings.get("COURSE_SMS_ENABLED", "false"));
    }

    private String validateConfiguration() {
        String accountSid = readSetting("TWILIO_ACCOUNT_SID");
        String authToken = readSetting("TWILIO_AUTH_TOKEN");
        String apiKey = readSetting("TWILIO_API_KEY");
        String apiSecret = readSetting("TWILIO_API_SECRET");
        String fromPhone = readSetting("TWILIO_FROM_PHONE", "TWILIO_FROM_NUMBER");
        List<String> recipients = readRecipients();

        if (accountSid.isBlank()) {
            return "missing Twilio Account SID.";
        }
        if ((apiKey.isBlank() || apiSecret.isBlank()) && authToken.isBlank()) {
            return "missing Twilio authentication credentials.";
        }
        if (fromPhone.isBlank()) {
            return "missing Twilio sender phone number.";
        }
        if (recipients.isEmpty()) {
            return "missing destination phone number.";
        }
        return null;
    }

    private String buildMessageBody(Course course) {
        return "EduKids: course published - "
                + safeText(course.getTitle(), "Untitled course")
                + " | " + safeText(course.getSubject(), "General")
                + " | " + course.getLevelText()
                + ".";
    }

    private String toFormBody(Map<String, String> values) {
        StringBuilder body = new StringBuilder();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (body.length() > 0) {
                body.append('&');
            }
            body.append(urlEncode(entry.getKey()))
                    .append('=')
                    .append(urlEncode(entry.getValue()));
        }
        return body.toString();
    }

    private String basicAuthHeader(String username, String password) {
        String raw = username + ":" + password;
        String encoded = java.util.Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String maskPhone(String phoneNumber) {
        String value = safeText(phoneNumber, "");
        if (value.length() <= 4) {
            return value;
        }
        return "*".repeat(Math.max(0, value.length() - 4)) + value.substring(value.length() - 4);
    }

    private String readSetting(String... keys) {
        for (String key : keys) {
            String value = AppSettings.get(key, "").trim();
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private List<String> readRecipients() {
        String rawRecipients = readSetting("STUDENT_SMS_RECIPIENTS", "TWILIO_TO_PHONE");
        List<String> recipients = new ArrayList<>();
        if (rawRecipients.isBlank()) {
            return recipients;
        }

        for (String token : rawRecipients.split("[,;]")) {
            String value = token == null ? "" : token.trim();
            if (!value.isBlank()) {
                recipients.add(value);
            }
        }
        return recipients;
    }

    public record NotificationResult(boolean attempted, boolean success, String message) {
        public static NotificationResult sent(String message) {
            return new NotificationResult(true, true, message);
        }

        public static NotificationResult failed(String message) {
            return new NotificationResult(true, false, message);
        }

        public static NotificationResult skipped(String message) {
            return new NotificationResult(false, false, message);
        }
    }
}
