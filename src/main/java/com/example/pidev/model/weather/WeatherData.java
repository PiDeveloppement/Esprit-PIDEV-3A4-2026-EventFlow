package com.example.pidev.model.weather;

public class WeatherData {
    private double temperature;
    private String description;
    private int rainChance;
    private double windSpeed;
    private String weatherCode;
    private double humidity;
    private boolean isAvailable;
    private String errorMessage;

    public WeatherData() {
        this.isAvailable = false;
    }

    public WeatherData(double temperature, String description, int rainChance,
                       double windSpeed, String weatherCode, double humidity) {
        this.temperature = temperature;
        this.description = description;
        this.rainChance = rainChance;
        this.windSpeed = windSpeed;
        this.weatherCode = weatherCode;
        this.humidity = humidity;
        this.isAvailable = true;
    }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getRainChance() { return rainChance; }
    public void setRainChance(int rainChance) { this.rainChance = rainChance; }
    public double getWindSpeed() { return windSpeed; }
    public void setWindSpeed(double windSpeed) { this.windSpeed = windSpeed; }
    public String getWeatherCode() { return weatherCode; }
    public void setWeatherCode(String weatherCode) { this.weatherCode = weatherCode; }
    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; this.isAvailable = false; }

    public String getWeatherEmoji() {
        switch (weatherCode) {
            case "0": return "☀️";
            case "1": return "🌤️";
            case "2": return "⛅";
            case "3": return "☁️";
            case "45":
            case "48": return "🌫️";
            case "51":
            case "53":
            case "55": return "🌦️";
            case "61":
            case "63":
            case "65": return "🌧️";
            case "71":
            case "73":
            case "75": return "❄️";
            case "80":
            case "81":
            case "82": return "⛈️";
            case "95":
            case "96":
            case "99": return "⛈️";
            default: return "🌡️";
        }
    }
}
