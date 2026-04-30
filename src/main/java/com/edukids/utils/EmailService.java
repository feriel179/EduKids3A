package com.edukids.utils;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;

import java.io.IOException;

public class EmailService {

    private static final String API_KEY = System.getenv("SENDGRID_API_KEY");
    private static final String FROM_EMAIL = getEnvOrDefault("SENDGRID_FROM_EMAIL", "noreply@edukids.com");
    private static final String FROM_NAME = getEnvOrDefault("SENDGRID_FROM_NAME", "EduKids Platform");

    public static boolean sendOTP(String toEmail, String otp) {
        if (API_KEY == null || API_KEY.isBlank()) {
            System.err.println("SendGrid API key is not configured. Set SENDGRID_API_KEY.");
            return false;
        }

        if (toEmail == null || toEmail.isBlank()) {
            System.err.println("Email address cannot be empty.");
            return false;
        }

        try {
            Email from = new Email(FROM_EMAIL, FROM_NAME);
            Email to = new Email(toEmail);
            Content content = new Content("text/html", buildOtpHtml(otp));
            Mail mail = new Mail(from, "EduKids - Verification Code", to, content);

            SendGrid sendGrid = new SendGrid(API_KEY);
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            return response.getStatusCode() == 202;
        } catch (IOException exception) {
            System.err.println("Email sending failed: " + exception.getMessage());
            return false;
        }
    }

    private static String buildOtpHtml(String otp) {
        return "<div style='font-family:Arial,sans-serif;max-width:420px;margin:auto'>"
                + "<h2 style='color:#2196F3'>EduKids</h2>"
                + "<p>Your verification code is:</p>"
                + "<h1 style='letter-spacing:8px;color:#333'>" + otp + "</h1>"
                + "<p style='color:#888;font-size:12px'>This code expires in 10 minutes.</p>"
                + "</div>";
    }

    private static String getEnvOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
