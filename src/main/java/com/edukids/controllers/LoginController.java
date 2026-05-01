package com.edukids.controllers;

import com.edukids.entities.User;
import com.edukids.enums.Role;
import com.edukids.services.UserService;
import com.edukids.utils.Navigator;
import com.edukids.utils.SessionManager;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import tn.esprit.MainFX;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
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
    @FXML private Label captchaQuestionLabel;
    @FXML private TextField captchaAnswerField;
    @FXML private Button googleSignInButton;

    private final UserService userService = new UserService();
    private final Random random = new Random();
    private int captchaAnswer;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        errorLabel.setVisible(false);
        generateCaptcha();
        configureGoogleSignIn();
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields.");
            return;
        }

        if (!isCaptchaValid()) {
            return;
        }

        // Check if user exists first to give proper error
        try {
            User found = userService.getByEmail(email);
            if (found != null && !found.isActive()) {
                showError("Your account has been banned. Please contact administrator for support.");
                return;
            }

            User user = userService.authenticate(email, password);
            if (user == null) {
                showError("Invalid email or password.");
                generateCaptcha();
                return;
            }

            redirectUser(user);
        } catch (RuntimeException exception) {
            showError("Login failed: " + exception.getMessage());
        }
    }

    @FXML
    public void generateCaptcha() {
        int first = random.nextInt(10) + 1;
        int second = random.nextInt(10) + 1;
        captchaAnswer = first + second;

        if (captchaQuestionLabel != null) {
            captchaQuestionLabel.setText(first + " + " + second + " = ?");
        }
        if (captchaAnswerField != null) {
            captchaAnswerField.clear();
        }
    }

    @FXML
    private void handleGoogleSignIn() {
        if (!isGoogleSignInConfigured()) {
            showError("Google sign-in is not configured yet.");
            return;
        }

        new Thread(() -> {
            try {
                Credential credential = performGoogleAuthorization();
                GoogleUserInfo googleUser = getGoogleUserInfo(credential);
                User user = userService.getByEmail(googleUser.email);

                if (user == null) {
                    user = new User();
                    user.setEmail(googleUser.email);
                    user.setFirstName(googleUser.givenName == null ? "" : googleUser.givenName);
                    user.setLastName(googleUser.familyName == null ? "" : googleUser.familyName);
                    user.setPassword(UUID.randomUUID().toString());
                    user.setRoles(List.of(Role.ROLE_ELEVE));
                    user.setVerified(true);
                    user.setActive(true);
                    userService.addOAuthUser(user);
                } else if (!user.isActive()) {
                    Platform.runLater(() -> showError("Your account has been banned. Please contact administrator for support."));
                    return;
                }

                User finalUser = user;
                Platform.runLater(() -> redirectUser(finalUser));
            } catch (Exception exception) {
                Platform.runLater(() -> showError("Google sign-in failed: " + exception.getMessage()));
            }
        }, "google-sign-in").start();
    }

    private boolean isCaptchaValid() {
        if (captchaAnswerField == null) {
            return true;
        }

        String answer = captchaAnswerField.getText().trim();
        if (answer.isEmpty()) {
            showError("Please answer the CAPTCHA calculation.");
            return false;
        }

        try {
            if (Integer.parseInt(answer) != captchaAnswer) {
                showError("Incorrect CAPTCHA answer. Please try again.");
                generateCaptcha();
                return false;
            }
            return true;
        } catch (NumberFormatException exception) {
            showError("The CAPTCHA answer must be a number.");
            generateCaptcha();
            return false;
        }
    }

    private Credential performGoogleAuthorization() throws Exception {
        InputStream clientSecret = LoginController.class.getResourceAsStream("/client_secret.json");
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

    private void configureGoogleSignIn() {
        if (googleSignInButton == null || isGoogleSignInConfigured()) {
            return;
        }

        googleSignInButton.setDisable(true);
        googleSignInButton.setVisible(false);
        googleSignInButton.setManaged(false);
    }

    private boolean isGoogleSignInConfigured() {
        return LoginController.class.getResource("/client_secret.json") != null;
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
        GoogleUserInfo userInfo = new GoogleUserInfo();
        userInfo.email = json.has("email") ? json.get("email").getAsString() : null;
        userInfo.givenName = json.has("given_name") ? json.get("given_name").getAsString() : "";
        userInfo.familyName = json.has("family_name") ? json.get("family_name").getAsString() : "";

        if (userInfo.email == null || userInfo.email.isBlank()) {
            throw new RuntimeException("Google account did not provide an email address.");
        }

        return userInfo;
    }

    private void redirectUser(User user) {
        SessionManager.setCurrentUser(user);

        try {
            if (user.getPrimaryRole() == Role.ROLE_ADMIN) {
                MainFX.getInstance().showUserDashboard();
            } else if (user.getPrimaryRole() == Role.ROLE_ELEVE) {
                MainFX.getInstance().showStudentShellForUser(user);
            } else {
                MainFX.getInstance().showParentHome();
            }
        } catch (Exception exception) {
            showError("Unable to open the requested screen.");
            exception.printStackTrace();
        }
    }

    private static class GoogleUserInfo {
        private String email;
        private String givenName;
        private String familyName;
    }

    @FXML
    private void handleGoToRegister() {
        Navigator.navigateTo("register.fxml", Navigator.getStageFromNode(rootPane));
    }

    @FXML
    private void handleForgotPassword() {
        Navigator.navigateTo("forgot-password.fxml", Navigator.getStageFromNode(rootPane));
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}
