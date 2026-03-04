package com.example.pidev.service.questionnaire;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.json.JSONObject;
import org.json.JSONArray;

public class AIService {

    private static final String MODEL = "gemini-3.1-flash-lite-preview";

    public String appelerIA(String theme) throws Exception {
        // 1. Récupération de la clé depuis la variable d'environnement définie dans IntelliJ
        String apiKey = System.getenv("GEMINI_API_KEY");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new Exception("La variable d'environnement 'GEMINI_API_KEY' n'est pas définie. " +
                    "Configurez-la dans 'Run > Edit Configurations > Environment variables'.");
        }

        // 2. Construction de l'URL avec v1beta
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent?key=" + apiKey;

        // 3. Préparation du prompt
        String prompt = "Génère une question de quiz technique sur le thème : " + theme +
                ". Réponds UNIQUEMENT avec ce format JSON strict : {\"question\": \"...\", \"reponse\": \"...\", \"points\": 10}";

        JSONObject textObj = new JSONObject().put("text", prompt);
        JSONObject contentObj = new JSONObject().put("parts", new JSONArray().put(textObj));
        JSONObject root = new JSONObject().put("contents", new JSONArray().put(contentObj));

        // 4. Appel HTTP
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(root.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // 5. Gestion des erreurs HTTP
        if (response.statusCode() != 200) {
            throw new Exception("Erreur API Google (Code " + response.statusCode() + ") : " + response.body());
        }

        // 6. Extraction du texte
        JSONObject resJson = new JSONObject(response.body());
        String aiText = resJson.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text");

        // 7. Nettoyage pour isoler le JSON
        int start = aiText.indexOf("{");
        int end = aiText.lastIndexOf("}");

        if (start == -1 || end == -1) {
            throw new Exception("L'IA n'a pas retourné de JSON valide : " + aiText);
        }

        return aiText.substring(start, end + 1);
    }
}