package com.example.pidev.service.questionnaire;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class BadWordService {
    private static final String API_URL = "https://www.purgomalum.com/service/plain?text=";

    // Dans ton BadWordService.java
    public String filtrerTexte(String texte) {
        if (texte == null || texte.isEmpty()) return texte;
        try {
            String encodedTexte = URLEncoder.encode(texte, StandardCharsets.UTF_8);

            // On ajoute des mots personnalisés à la fin de l'URL (ex: nul, mauvais, bof)
            String customWords = "&add=stupid,idiot,nul,mauvais";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + encodedTexte + customWords))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return texte;
        }
    }
}