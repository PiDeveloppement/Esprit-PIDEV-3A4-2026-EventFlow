package com.example.pidev.service.questionnaire;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;

public class AIService {
    // 1. Utilisez la clé que vous avez copiée depuis votre capture d'écran
    private static final String API_KEY = "AIzaSyCojFSco3MNpm19ef7wYgCKxSXhw3V-YyE";

    // 2. URL Corrigée : v1beta + gemini-1.5-flash (sans -latest)
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key=" + API_KEY;
    public String appelerIA(String theme) throws Exception {
        String prompt = "Génère une question de quiz technique sur le thème : " + theme +
                ". Réponds UNIQUEMENT avec ce format JSON : {\"question\": \"...\", \"reponse\": \"...\", \"points\": 10}";

        JSONObject textObj = new JSONObject().put("text", prompt);
        JSONObject contentObj = new JSONObject().put("parts", new JSONArray().put(textObj));
        JSONObject root = new JSONObject().put("contents", new JSONArray().put(contentObj));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();

        // LOG pour vérifier la réponse exacte de Google en cas d'échec
        System.out.println("DEBUG GOOGLE : " + body);

        JSONObject resJson = new JSONObject(body);
        if (resJson.has("error")) {
            throw new Exception("Erreur Google : " + resJson.getJSONObject("error").getString("message"));
        }

        String aiText = resJson.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        // Nettoyage pour isoler le JSON
        int start = aiText.indexOf("{");
        int end = aiText.lastIndexOf("}");
        return (start != -1 && end != -1) ? aiText.substring(start, end + 1) : aiText;
    }
}