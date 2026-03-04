package com.example.pidev.service.questionnaire;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;

public class AIService {

    // Utilisation du nom exact attendu par l'API v1beta
    private static final String MODEL_NAME = "gemini-3.1-flash-lite-preview";

    public String appelerIA(String theme) throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("La variable d'environnement 'GEMINI_API_KEY' est manquante.");
        }

        // L'URL DOIT avoir ce format exact : /v1beta/models/{model}:generateContent
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=" + apiKey;

        String prompt = "Agis comme un expert technique. Génère une question de quiz sur le thème : " + theme + ". " +
                "Réponds UNIQUEMENT avec ce format JSON strict : " +
                "{\"question\": \"...\", \"reponse\": \"...\", \"option1\": \"...\", \"option2\": \"...\", \"option3\": \"...\", \"points\": 10}";

        JSONObject textObj = new JSONObject().put("text", prompt);
        JSONObject contentObj = new JSONObject().put("parts", new JSONArray().put(textObj));
        JSONObject root = new JSONObject().put("contents", new JSONArray().put(contentObj));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            // Affiche la réponse complète pour diagnostiquer si le modèle est bridé par région
            System.err.println("Détail de l'erreur API : " + response.body());
            throw new Exception("Erreur API (Code " + response.statusCode() + ")");
        }

        JSONObject resJson = new JSONObject(response.body());

        return resJson.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");
    }
}