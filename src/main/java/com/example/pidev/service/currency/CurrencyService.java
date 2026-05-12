package com.example.pidev.service.currency;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CurrencyService {

    private static final String API_KEY = "2a91c695078b4c027b91e11d";
    private static final String API_URL = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/%s";

    public static double convert(double amount, String from, String to) {
        String url = String.format(API_URL, from);

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("Erreur API ExchangeRate. Code: " + response.statusCode());
                System.err.println("Réponse: " + response.body());
                return -1.0;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String result = json.get("result").getAsString();

            if ("success".equals(result)) {
                JsonObject rates = json.getAsJsonObject("conversion_rates");
                double rate = rates.get(to).getAsDouble();
                return amount * rate;
            } else {
                System.err.println("Erreur dans la réponse: " + json.get("error-type").getAsString());
                return -1.0;
            }

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return -1.0;
        }
    }
}