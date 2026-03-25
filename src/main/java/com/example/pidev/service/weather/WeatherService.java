package com.example.pidev.service.weather;

import com.example.pidev.model.weather.WeatherData;
import com.example.pidev.model.event.Event;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Service pour récupérer les données météorologiques via l'API Open-Meteo
 * @author Ons Abdesslem
 */
public class WeatherService {

    private static final String GEOCODING_API = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String WEATHER_API = "https://archive-api.open-meteo.com/v1/archive";
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    /**
     * Récupère la météo pour un événement
     */
    public WeatherData getWeatherForEvent(Event event) {
        if (event == null || event.getLocation() == null || event.getStartDate() == null) {
            WeatherData errorData = new WeatherData();
            errorData.setErrorMessage("Événement ou lieu non spécifié");
            return errorData;
        }

        try {
            // 1. Récupérer les coordonnées GPS du lieu
            Coordinates coords = getCoordinates(event.getLocation());
            if (coords == null) {
                WeatherData errorData = new WeatherData();
                errorData.setErrorMessage("Lieu '" + event.getLocation() + "' non trouvé");
                return errorData;
            }

            // 2. Récupérer la météo pour la date de l'événement
            WeatherData weather = getWeatherForecast(coords.getLatitude(), coords.getLongitude(),
                    event.getStartDate().toLocalDate());

            System.out.println("✅ Météo chargée pour " + event.getLocation() + ": " + weather.getDescription());
            return weather;

        } catch (Exception e) {
            System.err.println("❌ Erreur météo: " + e.getMessage());
            e.printStackTrace();
            WeatherData errorData = new WeatherData();
            errorData.setErrorMessage("Erreur lors du chargement de la météo");
            return errorData;
        }
    }

    /**
     * Convertit un nom de ville en coordonnées GPS
     */
    public Coordinates getCoordinates(String cityName) {
        try {
            String encodedCity = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
            String url = GEOCODING_API + "?name=" + encodedCity + "&count=1&language=fr&format=json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();
                // Parser simple : chercher "latitude" et "longitude"
                if (body.contains("\"results\"")) {
                    // Extraire latitude
                    int latStart = body.indexOf("\"latitude\":");
                    if (latStart != -1) {
                        int latEnd = body.indexOf(",", latStart);
                        String latStr = body.substring(latStart + 11, latEnd).trim();
                        double lat = Double.parseDouble(latStr);

                        // Extraire longitude
                        int lonStart = body.indexOf("\"longitude\":", latStart);
                        int lonEnd = body.indexOf(",", lonStart);
                        String lonStr = body.substring(lonStart + 12, lonEnd).trim();
                        double lon = Double.parseDouble(lonStr);

                        System.out.println("✅ Coordonnées trouvées pour " + cityName + ": " + lat + ", " + lon);
                        return new Coordinates(lat, lon);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Erreur Geocoding: " + e.getMessage());
        }
        return null;
    }

    /**
     * Récupère la météo pour une date, localisation et coordonnées GPS
     */
    public WeatherData getWeatherForecast(double latitude, double longitude, LocalDate date) {
        try {
            String dateStr = date.format(DateTimeFormatter.ISO_DATE);
            String url = WEATHER_API +
                    "?latitude=" + latitude +
                    "&longitude=" + longitude +
                    "&start_date=" + dateStr +
                    "&end_date=" + dateStr +
                    "&hourly=temperature_2m,precipitation,weather_code,relative_humidity_2m&timezone=auto";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String body = response.body();

                // Extraire température à midi (index 12)
                double temp = extractDoubleFromArray(body, "temperature_2m", 12);
                int rainChance = calculateRainChance(extractDoubleFromArray(body, "precipitation", 12));
                int weatherCode = (int) extractDoubleFromArray(body, "weather_code", 12);
                double humidity = extractDoubleFromArray(body, "relative_humidity_2m", 12);

                WeatherData weather = new WeatherData(temp, "", rainChance, 10.0,
                        String.valueOf(weatherCode), humidity);
                System.out.println("✅ Météo: " + weather.getWeatherEmoji() + " " + temp + "°C, Pluie: " + rainChance + "%");
                return weather;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Erreur API Météo: " + e.getMessage());
        }
        return new WeatherData();
    }

    /**
     * Extrait un double d'un array JSON avec parsing simple
     */
    private double extractDoubleFromArray(String json, String arrayName, int index) {
        try {
            String search = "\"" + arrayName + "\":[";
            int start = json.indexOf(search);
            if (start == -1) return 0;

            start = json.indexOf("[", start);
            int count = 0;
            int pos = start + 1;

            while (count < index && pos < json.length()) {
                if (json.charAt(pos) == ',') count++;
                pos++;
            }

            // Chercher le nombre
            int numStart = pos;
            while (pos < json.length() && (Character.isDigit(json.charAt(pos)) || json.charAt(pos) == '.' || json.charAt(pos) == '-')) {
                pos++;
            }

            String numStr = json.substring(numStart, pos).trim();
            return Double.parseDouble(numStr);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Calcule la probabilité de pluie (pourcentage)
     */
    private int calculateRainChance(double rain) {
        if (rain > 5) return 80;
        if (rain > 2) return 60;
        if (rain > 0.5) return 40;
        if (rain > 0) return 20;
        return 0;
    }

    /**
     * Classe interne pour stocker les coordonnées GPS
     */
    public static class Coordinates {
        private double latitude;
        private double longitude;

        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
    }
}


