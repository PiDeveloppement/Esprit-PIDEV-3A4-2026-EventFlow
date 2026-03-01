package com.example.pidev.model.questionnaire;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;

public class AIService {
    // REMPLACE PAR TA CLÉ RÉCUPÉRÉE À L'ÉTAPE 1
    private static final String API_KEY = "TON_API_KEY_ICI";
    private static final String URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    public String appelerIA(String theme) throws Exception {
        // On demande à l'IA de répondre en JSON pur pour faciliter le parsing
        String prompt = "Génère une question technique de niveau expert sur : " + theme +
                ". Réponds uniquement sous ce format JSON strict sans texte autour : " +
                "{\"question\": \"...\", \"reponse\": \"...\", \"points\": 15}";

        // Construction du corps de la requête (Format exigé par Google Gemini)
        String jsonBody = "{ \"contents\": [{ \"parts\":[{ \"text\": \"" + prompt + "\" }] }] }";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Extraction du texte de la réponse
        JSONObject fullRes = new JSONObject(response.body());
        return fullRes.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");
    }
}