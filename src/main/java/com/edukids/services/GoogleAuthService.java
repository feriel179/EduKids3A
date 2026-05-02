package com.edukids.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

public class GoogleAuthService {

    public boolean isConfigured() {
        return GoogleAuthService.class.getResource("/client_secret.json") != null;
    }

    public GoogleUserInfo authorizeAndFetchUser() throws Exception {
        Credential credential = performAuthorization();
        return getGoogleUserInfo(credential);
    }

    private Credential performAuthorization() throws Exception {
        InputStream clientSecret = GoogleAuthService.class.getResourceAsStream("/client_secret.json");
        if (clientSecret == null) {
            throw new RuntimeException("client_secret.json not found in src/main/resources.");
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                new InputStreamReader(clientSecret));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientSecrets,
                Arrays.asList(
                        "https://www.googleapis.com/auth/userinfo.profile",
                        "https://www.googleapis.com/auth/userinfo.email"))
                .setAccessType("offline")
                .build();

        int[] ports = {8889, 8890, 8891, 8892, 8893};
        Exception lastException = null;
        for (int port : ports) {
            try {
                LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(port).build();
                return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
            } catch (java.net.BindException exception) {
                lastException = exception;
            }
        }

        throw new RuntimeException("No available OAuth callback port.", lastException);
    }

    private GoogleUserInfo getGoogleUserInfo(Credential credential) throws Exception {
        URL url = new URL("https://www.googleapis.com/oauth2/v2/userinfo");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + credential.getAccessToken());

        if (connection.getResponseCode() != 200) {
            throw new RuntimeException("Google user info request failed with status " + connection.getResponseCode());
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
        GoogleUserInfo userInfo = new GoogleUserInfo(
                json.has("email") ? json.get("email").getAsString() : null,
                json.has("given_name") ? json.get("given_name").getAsString() : "",
                json.has("family_name") ? json.get("family_name").getAsString() : "");

        if (userInfo.email() == null || userInfo.email().isBlank()) {
            throw new RuntimeException("Google account did not provide an email address.");
        }

        return userInfo;
    }

    public record GoogleUserInfo(String email, String givenName, String familyName) {
    }
}
