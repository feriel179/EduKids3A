package com.edukids.utils;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.*;
import java.io.IOException;

public class EmailService {

    // TODO: Replace with your actual SendGrid API key from https://app.sendgrid.com/settings/api_keys
    private static final String API_KEY = System.getenv("SENDGRID_API_KEY") != null 
            ? System.getenv("SENDGRID_API_KEY") 
            : "SG.YOUR_API_KEY_HERE";
    private static final String FROM_EMAIL = "noreply@edukids.com";
    private static final String FROM_NAME = "EduKids Platform";

    public static boolean sendOTP(String toEmail, String otp) {
        // Validate API key
        if (API_KEY == null || API_KEY.equals("SG.YOUR_API_KEY_HERE") || API_KEY.isEmpty()) {
            System.err.println("❌ SendGrid API key not configured. Set SENDGRID_API_KEY environment variable.");
            return false;
        }

        if (toEmail == null || toEmail.isEmpty()) {
            System.err.println("❌ Email address cannot be null or empty.");
            return false;
        }

        try {
            Email from = new Email(FROM_EMAIL, FROM_NAME);
            Email to = new Email(toEmail);
            String subject = "EduKids - Verification Code";

            Content content = new Content("text/html",
                    "<div style='font-family:Arial,sans-serif;max-width:400px;margin:auto'>" +
                            "<h2 style='color:#2196F3'>EduKids</h2>" +
                            "<p>Your verification code is:</p>" +
                            "<h1 style='letter-spacing:8px;color:#333'>" + otp + "</h1>" +
                            "<p style='color:#888;font-size:12px'>This code expires in 10 minutes.</p>" +
                            "</div>"
            );

            Mail mail = new Mail(from, subject, to, content);
            SendGrid sg = new SendGrid(API_KEY);
            Request request = new Request();

            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            
            Response response = sg.api(request);
            System.out.println("📧 Email sent. Status code: " + response.getStatusCode());
            
            return response.getStatusCode() == 202;
        } catch (IOException e) {
            System.err.println("❌ Email sending failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

