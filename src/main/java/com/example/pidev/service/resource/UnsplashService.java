package com.example.pidev.service.resource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import org.json.JSONArray;

public class UnsplashService {
    // Remplace par ta vraie clé API Unsplash
    private static final String CLIENT_ID = "dcWhGYXCIQpKznqdhSTn5ymYZi10wbm4ygCzmfUH33c";
    private static final String BASE_URL = "https://api.unsplash.com/search/photos?page=1&query=";

    public String getImageUrl(String query) {
        try {
            String urlString = BASE_URL + query.replace(" ", "%20") + "&client_id=" + CLIENT_ID;
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();

            // Extraire le lien de la première image (format "regular" ou "small")
            JSONObject jsonResponse = new JSONObject(result.toString());
            JSONArray results = jsonResponse.getJSONArray("results");

            if (results.length() > 0) {
                return results.getJSONObject(0).getJSONObject("urls").getString("regular");
            }
        } catch (Exception e) {
            System.err.println("Erreur Unsplash: " + e.getMessage());
        }
        return null;
    }
}