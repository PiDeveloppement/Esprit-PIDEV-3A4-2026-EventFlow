package com.example.pidev.model.facial;

import java.time.LocalDateTime;

public class FaceEncoding {
    private int id;
    private int userId;
    private String encodingPath;
    private LocalDateTime createdAt;
    private double confidence;

    public FaceEncoding() {}

    public FaceEncoding(int userId, String encodingPath) {
        this.userId = userId;
        this.encodingPath = encodingPath;
        this.createdAt = LocalDateTime.now();
        this.confidence = 0.0;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getEncodingPath() { return encodingPath; }
    public void setEncodingPath(String encodingPath) { this.encodingPath = encodingPath; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}