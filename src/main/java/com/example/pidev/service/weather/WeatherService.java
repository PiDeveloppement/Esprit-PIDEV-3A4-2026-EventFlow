package com.example.pidev.service.weather;

import com.example.pidev.model.event.Event;
import com.example.pidev.model.weather.WeatherData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Service pour recuperer les donnees meteo via Open-Meteo.
 */
public class WeatherService {

    private static final String GEOCODING_API = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_API = "https://api.open-meteo.com/v1/forecast";
    private static final String ARCHIVE_API = "https://archive-api.open-meteo.com/v1/archive";

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private static final Map<String, String> LOCATION_ALIASES = Map.ofEntries(
            Map.entry("isamm", "Manouba"),
            Map.entry("isbst", "Manouba"),
            Map.entry("fshs manouba", "Manouba"),
            Map.entry("iscae manouba", "Manouba"),
            Map.entry("ensi", "Ariana"),
            Map.entry("supcom", "Ariana"),
            Map.entry("isi", "Ariana"),
            Map.entry("esprit", "Ariana"),
            Map.entry("fst tunis", "Tunis"),
            Map.entry("fseg tunis", "Tunis"),
            Map.entry("enit", "Tunis"),
            Map.entry("essec", "Tunis"),
            Map.entry("isg tunis", "Tunis"),
            Map.entry("fss sfax", "Sfax"),
            Map.entry("enis sfax", "Sfax"),
            Map.entry("isims", "Sfax"),
            Map.entry("fsm sousse", "Sousse"),
            Map.entry("isitcom", "Sousse"),
            Map.entry("fm monastir", "Monastir"),
            Map.entry("issat nabeul", "Nabeul")
    );

    /**
     * Recupere la meteo pour un evenement.
     */
    public WeatherData getWeatherForEvent(Event event) {
        if (event == null || event.getLocation() == null || event.getStartDate() == null) {
            return unavailable("Evenement ou lieu non specifie");
        }

        try {
            Coordinates coords = getCoordinates(event.getLocation());
            if (coords == null) {
                return unavailable("Lieu '" + event.getLocation() + "' non trouve");
            }

            WeatherData weather = getWeatherForecast(
                    coords.getLatitude(),
                    coords.getLongitude(),
                    event.getStartDate().toLocalDate()
            );

            String description = weather.getDescription() == null ? "indisponible" : weather.getDescription();
            System.out.println("✅ Meteo chargee pour " + event.getLocation() + ": " + description);
            return weather;
        } catch (Exception e) {
            System.err.println("❌ Erreur meteo: " + e.getMessage());
            return unavailable("Erreur lors du chargement de la meteo");
        }
    }

    /**
     * Convertit un nom de lieu en coordonnees GPS.
     */
    public Coordinates getCoordinates(String cityName) {
        List<String> candidates = buildLocationCandidates(cityName);
        for (String candidate : candidates) {
            Coordinates coords = geocodeOne(candidate);
            if (coords != null) {
                return coords;
            }
        }
        return null;
    }

    private Coordinates geocodeOne(String cityName) {
        try {
            String encodedCity = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
            String url = GEOCODING_API + "?name=" + encodedCity + "&count=1&language=fr&format=json";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray results = root.has("results") && root.get("results").isJsonArray()
                    ? root.getAsJsonArray("results") : null;

            if (results == null || results.isEmpty()) {
                return null;
            }

            JsonObject first = results.get(0).getAsJsonObject();
            double lat = readDouble(first, "latitude", Double.NaN);
            double lon = readDouble(first, "longitude", Double.NaN);

            if (Double.isNaN(lat) || Double.isNaN(lon)) {
                return null;
            }

            System.out.println("✅ Coordonnees trouvees pour " + cityName + ": " + lat + ", " + lon);
            return new Coordinates(lat, lon);
        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Erreur geocoding: " + e.getMessage());
            return null;
        }
    }

    /**
     * Recupere la meteo pour une date et des coordonnees GPS.
     */
    public WeatherData getWeatherForecast(double latitude, double longitude, LocalDate date) {
        try {
            boolean isHistorical = date.isBefore(LocalDate.now());
            String url = buildWeatherUrl(latitude, longitude, date, isHistorical);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(20))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return unavailable("API meteo indisponible (HTTP " + response.statusCode() + ")");
            }

            return parseWeather(response.body(), isHistorical);
        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Erreur API meteo: " + e.getMessage());
            return unavailable("Erreur reseau meteo");
        }
    }

    private String buildWeatherUrl(double latitude, double longitude, LocalDate date, boolean historical) {
        String dateStr = date.format(DateTimeFormatter.ISO_DATE);
        String base = historical ? ARCHIVE_API : FORECAST_API;
        String dailyParams = historical
                ? "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max"
                : "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,relative_humidity_2m_mean,wind_speed_10m_max";

        return base
                + "?latitude=" + latitude
                + "&longitude=" + longitude
                + "&start_date=" + dateStr
                + "&end_date=" + dateStr
                + "&daily=" + dailyParams
                + "&timezone=auto";
    }

    private WeatherData parseWeather(String body, boolean historical) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();
            if (!root.has("daily") || !root.get("daily").isJsonObject()) {
                return unavailable("Aucune donnee meteo retournee");
            }

            JsonObject daily = root.getAsJsonObject("daily");

            double tempMax = readFirstArrayDouble(daily, "temperature_2m_max", Double.NaN);
            double tempMin = readFirstArrayDouble(daily, "temperature_2m_min", Double.NaN);
            if (Double.isNaN(tempMax) && Double.isNaN(tempMin)) {
                return unavailable("Temperature indisponible");
            }

            double temperature = Double.isNaN(tempMax) ? tempMin
                    : (Double.isNaN(tempMin) ? tempMax : (tempMax + tempMin) / 2.0);

            int weatherCode = (int) Math.round(readFirstArrayDouble(daily, "weather_code", 0.0));
            double windSpeed = readFirstArrayDouble(daily, "wind_speed_10m_max", 0.0);

            double humidity = readFirstArrayDouble(daily, "relative_humidity_2m_mean", Double.NaN);
            if (Double.isNaN(humidity)) {
                humidity = 50.0;
            }

            double rainProb = readFirstArrayDouble(daily, "precipitation_probability_max", Double.NaN);
            int rainChance;
            if (!Double.isNaN(rainProb)) {
                rainChance = clampPercent((int) Math.round(rainProb));
            } else {
                double rainMm = readFirstArrayDouble(daily, "precipitation_sum", 0.0);
                rainChance = historical ? calculateRainChanceFromMm(rainMm) : 0;
            }

            WeatherData weather = new WeatherData(
                    temperature,
                    weatherDescription(weatherCode),
                    rainChance,
                    windSpeed,
                    String.valueOf(weatherCode),
                    humidity
            );

            System.out.println("✅ Meteo: " + weather.getWeatherEmoji() + " " + temperature + "C, Pluie: " + rainChance + "%");
            return weather;
        } catch (Exception ex) {
            return unavailable("Reponse meteo invalide");
        }
    }

    private List<String> buildLocationCandidates(String location) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        if (location == null) {
            return new ArrayList<>(candidates);
        }

        String trimmed = location.trim();
        if (trimmed.isEmpty()) {
            return new ArrayList<>(candidates);
        }

        candidates.add(trimmed);

        String alias = LOCATION_ALIASES.get(normalizeKey(trimmed));
        if (alias != null) {
            candidates.add(alias);
            candidates.add(alias + ", Tunisia");
        }

        candidates.add(normalizeLocation(trimmed));

        if (trimmed.contains("-")) {
            String firstPart = trimmed.substring(0, trimmed.indexOf('-')).trim();
            if (!firstPart.isEmpty()) {
                candidates.add(firstPart);
                candidates.add(firstPart + ", Tunisia");
            }
        }

        return new ArrayList<>(candidates);
    }

    private String normalizeLocation(String value) {
        String v = value.trim();
        String lower = v.toLowerCase(Locale.ROOT);
        if (lower.contains("tunisia") || lower.contains("tunisie")) {
            return v;
        }
        return v + ", Tunisia";
    }

    private String normalizeKey(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace('’', '\'')
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s']", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private double readDouble(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        try {
            return object.get(key).getAsDouble();
        } catch (Exception ex) {
            return fallback;
        }
    }

    private double readFirstArrayDouble(JsonObject object, String key, double fallback) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return fallback;
        }
        JsonArray arr = object.getAsJsonArray(key);
        if (arr.isEmpty()) {
            return fallback;
        }
        try {
            return arr.get(0).getAsDouble();
        } catch (Exception ex) {
            return fallback;
        }
    }

    private int calculateRainChanceFromMm(double rainMm) {
        if (rainMm >= 10) return 90;
        if (rainMm >= 5) return 75;
        if (rainMm >= 2) return 55;
        if (rainMm >= 0.5) return 35;
        if (rainMm > 0) return 20;
        return 0;
    }

    private int clampPercent(int value) {
        if (value < 0) return 0;
        if (value > 100) return 100;
        return value;
    }

    private String weatherDescription(int code) {
        return switch (code) {
            case 0 -> "Ciel degage";
            case 1 -> "Principalement degage";
            case 2 -> "Partiellement nuageux";
            case 3 -> "Couvert";
            case 45, 48 -> "Brouillard";
            case 51, 53, 55 -> "Bruine";
            case 61, 63, 65 -> "Pluie";
            case 71, 73, 75 -> "Neige";
            case 80, 81, 82 -> "Averses";
            case 95, 96, 99 -> "Orage";
            default -> "Conditions variables";
        };
    }

    private WeatherData unavailable(String message) {
        WeatherData weatherData = new WeatherData();
        weatherData.setDescription("Indisponible");
        weatherData.setErrorMessage(message);
        return weatherData;
    }

    /**
     * Classe interne pour stocker les coordonnees GPS.
     */
    public static class Coordinates {
        private final double latitude;
        private final double longitude;

        public Coordinates(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public double getLongitude() {
            return longitude;
        }
    }
}
