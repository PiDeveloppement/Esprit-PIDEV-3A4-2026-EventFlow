package com.example.pidev.service.whatsapp;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;

public class WhatsAppService {

    private static final Properties LOCAL_CONFIG = loadLocalConfig();
    private static final String DEFAULT_FROM_NUMBER = "whatsapp:+14155238886";
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    // Read from env vars (or JVM properties as fallback)
    private static final String ACCOUNT_SID = readConfig("TWILIO_ACCOUNT_SID");
    private static final String AUTH_TOKEN = readConfig("TWILIO_AUTH_TOKEN");
    private static final String FROM_NUMBER_RAW = readConfig("TWILIO_WHATSAPP_FROM", DEFAULT_FROM_NUMBER);
    private static final String SANDBOX_JOIN_CODE = readConfig("TWILIO_WHATSAPP_JOIN_CODE");

    private static volatile String lastError = "";
    private static volatile String lastMessageSid = "";
    private static volatile String lastMessageStatus = "";

    static {
        if (ACCOUNT_SID.isBlank() || AUTH_TOKEN.isBlank()) {
            System.err.println("Twilio config missing: TWILIO_ACCOUNT_SID / TWILIO_AUTH_TOKEN");
        }
        String normalizedFrom = normalizeWhatsAppAddress(FROM_NUMBER_RAW);
        if (normalizedFrom.isBlank()) {
            System.err.println("Twilio config warning: TWILIO_WHATSAPP_FROM invalide, fallback sandbox utilise.");
        }
    }

    public static String getLastError() {
        return lastError;
    }

    public static String getLastMessageSid() {
        return lastMessageSid;
    }

    public static String getLastMessageStatus() {
        return lastMessageStatus;
    }

    public static boolean sendConfirmation(String phoneNumber, String company, double amount) {
        lastError = "";
        lastMessageSid = "";
        lastMessageStatus = "";

        if (ACCOUNT_SID.isBlank() || AUTH_TOKEN.isBlank()) {
            lastError = "Identifiants Twilio manquants (variables d'environnement)";
            return false;
        }

        try {
            String to = normalizeWhatsAppAddress(phoneNumber);
            if (to.isBlank()) {
                lastError = "Numero WhatsApp invalide.";
                return false;
            }

            String from = normalizeWhatsAppAddress(FROM_NUMBER_RAW);
            if (from.isBlank()) {
                from = DEFAULT_FROM_NUMBER;
            }

            String messageBody = String.format(
                    "Merci %s ! Votre contribution de %.2f TND a ete enregistree. - EventFlow",
                    company, amount
            );

            String body = "To=" + URLEncoder.encode(to, StandardCharsets.UTF_8)
                    + "&From=" + URLEncoder.encode(from, StandardCharsets.UTF_8)
                    + "&Body=" + URLEncoder.encode(messageBody, StandardCharsets.UTF_8);

            String auth = ACCOUNT_SID + ":" + AUTH_TOKEN;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json"))
                    .header("Authorization", "Basic " + encodedAuth)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Reponse Twilio : " + response.body());

            if (response.statusCode() != 201) {
                String details = extractJsonField(response.body(), "message");
                if (details.isBlank()) {
                    details = response.body();
                }
                String code = extractJsonRawField(response.body(), "code");
                String mapped = mapTwilioFailure(code, details, "http_" + response.statusCode());
                if (!mapped.isBlank()) {
                    details = mapped;
                }
                if (looksLikeSandboxIssue(details)) {
                    details = appendSandboxHint(details);
                }
                lastError = "Erreur Twilio HTTP " + response.statusCode() + " : " + details;
                return false;
            }

            lastMessageSid = extractJsonField(response.body(), "sid");
            lastMessageStatus = extractJsonField(response.body(), "status");

            if (lastMessageSid.isBlank()) {
                return true;
            }

            return waitForDeliveryResult(lastMessageSid, encodedAuth);
        } catch (Exception e) {
            lastError = "Exception reseau : " + e.getMessage();
            e.printStackTrace();
            return false;
        }
    }

    private static String normalizeWhatsAppAddress(String rawPhone) {
        String e164 = normalizeE164(rawPhone);
        if (e164.isBlank()) {
            return "";
        }
        return "whatsapp:" + e164;
    }

    private static String normalizeE164(String rawPhone) {
        if (rawPhone == null) return "";

        String value = rawPhone.trim();
        if (value.isEmpty()) return "";

        if (value.toLowerCase().startsWith("whatsapp:")) {
            value = value.substring("whatsapp:".length()).trim();
        }

        // Keep leading '+' if present, remove the rest of separators.
        if (value.startsWith("+")) {
            value = "+" + value.substring(1).replaceAll("[^0-9]", "");
        } else {
            value = value.replaceAll("[^0-9]", "");
        }

        if (value.startsWith("00")) {
            value = "+" + value.substring(2);
        }
        if (!value.startsWith("+")) {
            if (value.length() == 8) {
                value = "+216" + value;
            } else if (value.length() >= 9 && value.length() <= 15) {
                value = "+" + value;
            } else {
                return "";
            }
        }

        if (!value.matches("^\\+[1-9][0-9]{7,14}$")) {
            return "";
        }
        return value;
    }

    private static boolean waitForDeliveryResult(String sid, String encodedAuth) {
        // Attendre quelques secondes pour capturer "failed/undelivered" rapidement.
        int maxAttempts = 8;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                Thread.sleep(1500);
                String body = fetchMessageBySid(sid, encodedAuth);
                if (body == null || body.isBlank()) {
                    continue;
                }
                String status = extractJsonField(body, "status");
                if (!status.isBlank()) {
                    lastMessageStatus = status;
                }

                if ("delivered".equalsIgnoreCase(status)
                        || "sent".equalsIgnoreCase(status)
                        || "read".equalsIgnoreCase(status)) {
                    return true;
                }
                if ("failed".equalsIgnoreCase(status)
                        || "undelivered".equalsIgnoreCase(status)
                        || "canceled".equalsIgnoreCase(status)) {
                    String errMsg = extractJsonField(body, "error_message");
                    String errCode = extractJsonRawField(body, "error_code");
                    String reason = mapTwilioFailure(errCode, errMsg, status);
                    if (reason.isBlank()) {
                        reason = "Twilio status=" + status;
                    }
                    if (looksLikeSandboxIssue(reason)) {
                        reason = appendSandboxHint(reason);
                    }
                    lastError = reason;
                    return false;
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                lastError = "Verification Twilio interrompue.";
                return false;
            } catch (Exception ignored) {
                // Ignore polling errors and keep trying until timeout.
            }
        }

        // Message still pending; don't claim success when user may receive nothing.
        String pending = lastMessageStatus == null || lastMessageStatus.isBlank()
                ? "queued"
                : lastMessageStatus;
        lastError = appendSandboxHint("Message non confirme (" + pending + ").");
        return false;
    }

    private static String fetchMessageBySid(String sid, String encodedAuth) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages/" + sid + ".json"))
                .header("Authorization", "Basic " + encodedAuth)
                .GET()
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return response.body();
        }
        return "";
    }

    private static String extractJsonField(String json, String fieldName) {
        if (json == null || json.isBlank() || fieldName == null || fieldName.isBlank()) {
            return "";
        }
        String needle = "\"" + fieldName + "\"";
        int keyPos = json.indexOf(needle);
        if (keyPos < 0) return "";
        int colon = json.indexOf(':', keyPos + needle.length());
        if (colon < 0) return "";
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return "";
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) return "";
        return json.substring(firstQuote + 1, secondQuote).trim();
    }

    private static String extractJsonRawField(String json, String fieldName) {
        if (json == null || json.isBlank() || fieldName == null || fieldName.isBlank()) {
            return "";
        }
        String needle = "\"" + fieldName + "\"";
        int keyPos = json.indexOf(needle);
        if (keyPos < 0) return "";
        int colon = json.indexOf(':', keyPos + needle.length());
        if (colon < 0) return "";
        int end = json.indexOf(',', colon + 1);
        if (end < 0) {
            end = json.indexOf('}', colon + 1);
        }
        if (end < 0) return "";
        return json.substring(colon + 1, end).replace("\"", "").trim();
    }

    private static boolean looksLikeSandboxIssue(String details) {
        if (details == null) return false;
        String d = details.toLowerCase();
        return d.contains("sandbox")
                || d.contains("join")
                || d.contains("opted in")
                || d.contains("21608")
                || d.contains("63016")
                || d.contains("63015");
    }

    private static String appendSandboxHint(String details) {
        return details + " " + sandboxJoinInstruction();
    }

    private static String mapTwilioFailure(String code, String message, String status) {
        String c = code == null ? "" : code.trim();
        String m = message == null ? "" : message.trim();

        if ("63015".equals(c)) {
            return "Code 63015: le numero destinataire n'a pas rejoint le sandbox WhatsApp Twilio.";
        }
        if ("63016".equals(c)) {
            return "Code 63016: fenetre WhatsApp expiree, utilisez un template approuve.";
        }

        String base = "";
        if (!c.isBlank() && !"null".equalsIgnoreCase(c)) {
            base = "code " + c;
        }
        if (!m.isBlank() && !"null".equalsIgnoreCase(m)) {
            base = base.isBlank() ? m : (base + " - " + m);
        }
        if (!base.isBlank()) {
            return base;
        }
        if (status != null && !status.isBlank()) {
            return "Twilio status=" + status;
        }
        return "";
    }

    private static String sandboxJoinInstruction() {
        if (SANDBOX_JOIN_CODE != null && !SANDBOX_JOIN_CODE.isBlank()) {
            return "Rejoignez le sandbox Twilio WhatsApp: envoyez 'join " + SANDBOX_JOIN_CODE.trim()
                    + "' au +14155238886 depuis le numero destinataire.";
        }
        return "Rejoignez le sandbox Twilio WhatsApp: envoyez 'join <code de votre console>' au +14155238886 depuis le numero destinataire.";
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
            value = LOCAL_CONFIG.getProperty(key);
        }
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String trimmed = value.trim();
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
    }

    private static Properties loadLocalConfig() {
        Properties props = new Properties();
        Path homeConfig = Paths.get(System.getProperty("user.home"), ".eventflow", "secrets.properties");
        Path projectConfig = Paths.get(System.getProperty("user.dir"), "config", "local-secrets.properties");
        loadPropertiesIfExists(props, homeConfig);
        loadPropertiesIfExists(props, projectConfig);
        return props;
    }

    private static void loadPropertiesIfExists(Properties target, Path path) {
        if (path == null || !Files.exists(path) || !Files.isRegularFile(path)) {
            return;
        }
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            Properties tmp = new Properties();
            tmp.load(in);
            target.putAll(tmp);
        } catch (IOException ignored) {
        }
    }
}
