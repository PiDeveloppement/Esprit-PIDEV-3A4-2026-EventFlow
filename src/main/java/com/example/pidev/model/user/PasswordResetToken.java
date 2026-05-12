package com.example.pidev.model.user;

import java.time.LocalDateTime;
import java.util.UUID;

public class PasswordResetToken {
    private int id;
    private int userId;        // Référence à l'ID de UserModel
    private String token;
    private LocalDateTime expiryDate;
    private boolean used;

    public PasswordResetToken(int userId) {
        this.userId = userId;
        this.token = UUID.randomUUID().toString();
        this.expiryDate = LocalDateTime.now().plusHours(1);
        this.used = false;
    }

    // Getters et setters...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public boolean isValid() {
        return !used && expiryDate.isAfter(LocalDateTime.now());
    }
    public String generateResetLink() {
        return "http://localhost:8080/reset-password?token=" + this.token;
    }
}
