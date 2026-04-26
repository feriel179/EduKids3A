package com.edukids.controllers;

import com.edukids.entities.User;
import com.edukids.enums.Role;
import com.edukids.services.UserService;
import com.edukids.utils.Navigator;
import com.edukids.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.UUID;

public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Label errorLabel;
    @FXML private VBox rootPane;

    // ── CAPTCHA ─────────────────────────────────────────────────
    @FXML private Label captchaQuestionLabel;
    @FXML private TextField captchaAnswerField;

    private int captchaAnswer;
    private final Random random = new Random();
    // ────────────────────────────────────────────────────────────

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);
        generateCaptcha();
    }

    // ── CAPTCHA ──────────────────────────────────────────────────
    @FXML
    public void generateCaptcha() {
        int a = random.nextInt(10) + 1;
        int b = random.nextInt(10) + 1;
        captchaAnswer = a + b;
        captchaQuestionLabel.setText(a + " + " + b + " = ?");
        if (captchaAnswerField != null) captchaAnswerField.clear();
    }

    private boolean isCaptchaValid() {
        String input = captchaAnswerField.getText().trim();
        if (input.isEmpty()) {
            showError("Veuillez résoudre le CAPTCHA.");
            return false;
        }
        try {
            if (Integer.parseInt(input) != captchaAnswer) {
                showError("Réponse CAPTCHA incorrecte. Réessayez.");
                generateCaptcha();
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            showError("Le CAPTCHA doit être un nombre entier.");
            generateCaptcha();
            return false;
        }
    }

    // ── Login ────────────────────────────────────────────────────
    @FXML
    private void handleLogin() {
        String email    = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }
        if (!isCaptchaValid()) return;

        User found = userService.getByEmail(email);
        if (found != null && !found.isActive()) {
            showError("Votre compte est banni. Contactez l'administrateur.");
            return;
        }

        User user = userService.authenticate(email, password);
        if (user == null) {
            showError("Email ou mot de passe incorrect.");
            generateCaptcha();
            return;
        }

        redirectUser(user);
    }

    // ── Google Sign-In ───────────────────────────────────────────
    @FXML
    private void handleGoogleSignIn() {
        new Thread(() -> {
            try {
                Credential credential = performAuthorization();
                if (credential == null) {
                    System.out.println("❌ credential est null");
                    return;
                }

                UserInfoData userInfo = getUserInfo(credential);
                if (userInfo == null) {
                    System.out.println("❌ userInfo est null");
                    return;
                }

                System.out.println("✅ Email: " + userInfo.email);
                System.out.println("✅ Prénom: " + userInfo.givenName);
                System.out.println("✅ Nom: " + userInfo.familyName);

                String email = userInfo.email;
                User user = userService.getByEmail(email);

                if (user == null) {
                    System.out.println("➡️ Nouvel utilisateur, ajout en base...");
                    user = new User();
                    user.setEmail(email);
                    user.setFirstName(userInfo.givenName != null
                            ? userInfo.givenName : "");
                    user.setLastName(userInfo.familyName != null
                            ? userInfo.familyName : "");
                    user.setPassword(UUID.randomUUID().toString());
                    user.setRoles(List.of(Role.ROLE_ELEVE));
                    user.setVerified(true);
                    user.setActive(true);
                    user.setAvatar(null);

                    userService.addOAuthUser(user);
                    System.out.println("✅ Utilisateur ajouté ! ID: " + user.getId());
                } else {
                    System.out.println("✅ Utilisateur existant, ID: " + user.getId());
                }

                final User finalUser = user;
                Platform.runLater(() -> redirectUser(finalUser));

            } catch (Exception e) {
                System.out.println("❌ Erreur: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() ->
                        showError("Connexion Google échouée : " + e.getMessage())
                );
            }
        }).start();
    }

    // ── OAuth helpers ────────────────────────────────────────────
    private Credential performAuthorization() throws Exception {

        InputStream is = LoginController.class
                .getResourceAsStream("/client_secret.json");

        if (is == null) {
            throw new RuntimeException(
                    "Fichier client_secret.json introuvable dans src/main/resources/");
        }

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                new InputStreamReader(is));

        GoogleClientSecrets.Details details = clientSecrets.getInstalled();
        if (details == null) {
            throw new RuntimeException(
                    "Mauvais type OAuth ! Application type doit être 'Desktop app'");
        }

        String clientId = details.getClientId();
        if (clientId == null || clientId.isBlank() || clientId.contains("VOTRE")) {
            throw new RuntimeException(
                    "client_id invalide ! Télécharge le vrai fichier depuis Google Cloud Console.");
        }

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                clientSecrets,
                Arrays.asList(
                        "https://www.googleapis.com/auth/userinfo.profile",
                        "https://www.googleapis.com/auth/userinfo.email"
                ))
                .setAccessType("offline")
                .build();

        // Try multiple ports in case one is in use
        int[] portCandidates = {8889, 8890, 8891, 8892, 8893};
        Credential credential = null;
        
        for (int port : portCandidates) {
            try {
                LocalServerReceiver receiver = new LocalServerReceiver
                        .Builder().setPort(port).build();
                credential = new AuthorizationCodeInstalledApp(flow, receiver)
                        .authorize("user");git
                System.out.println("✅ OAuth successful on port: " + port);
                break;
            } catch (java.net.BindException e) {
                System.out.println("⚠️ Port " + port + " in use, trying next...");
                continue;
            }
        }
        
        if (credential == null) {
            throw new RuntimeException("Could not complete OAuth authorization on any available port");
        }

        return credential;
    }

    private UserInfoData getUserInfo(Credential credential) throws Exception {
        // Fetch user info from Google API endpoint manually
        String url = "https://www.googleapis.com/oauth2/v2/userinfo?access_token=" 
                + credential.getAccessToken();
        
        URL obj = new URL(url);
        com.sun.net.httpserver.HttpExchange exchange = null;
        
        // Use basic URL connection instead
        java.net.HttpURLConnection connection = (java.net.HttpURLConnection) obj.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + credential.getAccessToken());
        
        int status = connection.getResponseCode();
        if (status == 200) {
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(connection.getInputStream()));
            StringBuilder jsonResponse = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonResponse.append(line);
            }
            reader.close();
            
            // Parse JSON response
            JsonObject jsonObject = JsonParser.parseString(jsonResponse.toString()).getAsJsonObject();
            UserInfoData userInfo = new UserInfoData();
            userInfo.email = jsonObject.has("email") ? jsonObject.get("email").getAsString() : null;
            userInfo.givenName = jsonObject.has("given_name") ? jsonObject.get("given_name").getAsString() : null;
            userInfo.familyName = jsonObject.has("family_name") ? jsonObject.get("family_name").getAsString() : null;
            return userInfo;
        } else {
            throw new RuntimeException("Failed to get user info from Google. Status code: " + status);
        }
    }
    
    // Helper class to store user info
    private static class UserInfoData {
        String email;
        String givenName;
        String familyName;
    }

    // ── Navigation ───────────────────────────────────────────────
    private void redirectUser(User user) {
        SessionManager.setCurrentUser(user);
        String target = (user.getPrimaryRole() == Role.ROLE_ADMIN)
                ? "dashboard.fxml"
                : "user-home.fxml";
        Navigator.navigateTo(target, Navigator.getStageFromNode(rootPane));
    }

    @FXML private void handleGoToRegister() {
        Navigator.navigateTo("register.fxml", Navigator.getStageFromNode(rootPane));
    }

    @FXML private void handleForgotPassword() {
        Navigator.navigateTo("forgot-password.fxml", Navigator.getStageFromNode(rootPane));
    }

    // ── UI helper ────────────────────────────────────────────────
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}

