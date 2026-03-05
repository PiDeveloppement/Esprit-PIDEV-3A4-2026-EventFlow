package com.example.pidev.service.whatsapp;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class WhatsAppService {

    // Read from env vars (or JVM properties as fallback)
    private static final String ACCOUNT_SID = readConfig("TWILIO_ACCOUNT_SID");
    private static final String AUTH_TOKEN = readConfig("TWILIO_AUTH_TOKEN");
    private static final String FROM_NUMBER = readConfig("TWILIO_WHATSAPP_FROM", "whatsapp:+14155238886");

    private static volatile String lastError = "";

    static {
        if (ACCOUNT_SID.isBlank() || AUTH_TOKEN.isBlank()) {
            System.err.println("Twilio config missing: TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN");
        }
    }

    public static String getLastError() {
        return lastError;
    }

    public static boolean sendConfirmation(String phoneNumber, String company, double amount) {
        lastError = "";

        if (ACCOUNT_SID.isBlank() || AUTH_TOKEN.isBlank()) {
            lastError = "Identifiants Twilio manquants (variables d'environnement)";
            return false;
        }

        try {
            String normalizedPhone = normalizePhone(phoneNumber);
            if (normalizedPhone.isBlank()) {
                lastError = "Numero WhatsApp invalide.";
                return false;
            }

            String to = "whatsapp:" + normalizedPhone;
            String messageBody = String.format(
                    "Merci %s ! Votre contribution de %.2f TND a ete enregistree. - EventFlow",
                    company, amount
            );

            String body = "To=" + URLEncoder.encode(to, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(FROM_NUMBER, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(messageBody, StandardCharsets.UTF_8);

            String auth = ACCOUNT_SID + ":" + AUTH_TOKEN;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json"))
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Reponse Twilio : " + response.body());

            if (response.statusCode() == 201) {
                return true;
            }

            lastError = "Erreur Twilio HTTP " + response.statusCode() + " : " + response.body();
            return false;
        } catch (Exception e) {
            lastError = "Exception reseau : " + e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

    private static String normalizePhone(String phoneNumber) {
        if (phoneNumber == null) return "";
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) digits = digits.substring(2);
        if (digits.length() == 8) digits = "216" + digits;
        return digits;
    }

    private static String readConfig(String key) {
        return readConfig(key, "");
    }

    private static String readConfig(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = System.getProperty(key);
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value.trim();
    }
}
