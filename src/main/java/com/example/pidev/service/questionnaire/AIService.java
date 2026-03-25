package com.example.pidev.service.questionnaire;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class AIService {

    private static final String MODEL_NAME = "gemini-1.5-flash";

    public String appelerIA(String theme) {
        String safeTheme = (theme == null || theme.isBlank()) ? "culture generale" : theme.trim();
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            return buildFallbackJson(safeTheme);
        }

        try {
            String prompt = "Genere une question QCM sur le theme: " + safeTheme + ". "
                    + "Retourne UNIQUEMENT un JSON valide avec les cles: "
                    + "question, reponse, option1, option2, option3, points.";

            JSONObject body = new JSONObject()
                    .put("contents", new JSONArray().put(
                            new JSONObject().put("parts", new JSONArray().put(
                                    new JSONObject().put("text", prompt)
                            ))
                    ));

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + MODEL_NAME + ":generateContent?key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Erreur API IA (" + response.statusCode() + "): " + response.body());
                return buildFallbackJson(safeTheme);
            }

            JSONObject resJson = new JSONObject(response.body());
            String text = resJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

            if (text == null || text.isBlank()) {
                return buildFallbackJson(safeTheme);
            }
            return text;
        } catch (Exception e) {
            System.err.println("IA indisponible, fallback local: " + e.getMessage());
            return buildFallbackJson(safeTheme);
        }
    }

    private String buildFallbackJson(String theme) {
        JSONObject fallback = new JSONObject();
        fallback.put("question", "Quel est le meilleur objectif d'un projet lie a " + theme + " ?");
        fallback.put("reponse", "Repondre a un besoin clair avec une solution mesurable.");
        fallback.put("option1", "Copier un ancien projet sans adaptation.");
        fallback.put("option2", "Ignorer les contraintes de temps et de budget.");
        fallback.put("option3", "Lancer le projet sans definir les utilisateurs cibles.");
        fallback.put("points", 10);
        return fallback.toString();
    }
}
