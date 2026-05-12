package com.example.pidev.model.event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modèle représentant un ticket d'événement
 * @author Ons Abdesslem
 */
public class EventTicket {

    // ==================== ATTRIBUTS ====================

    private int id;
    private String ticketCode;
    private int eventId;
    private int userId;
    private String qrCode;
    private boolean isUsed;
    private LocalDateTime usedAt;
    private LocalDateTime createdAt;

    // ==================== CONSTRUCTEURS ====================

    public EventTicket() {}

    public EventTicket(String ticketCode, int eventId, int userId) {
        this.ticketCode = ticketCode;
        this.eventId = eventId;
        this.userId = userId;
        this.isUsed = false;
    }

    // ==================== GETTERS & SETTERS ====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Génère un code de ticket unique
     * Format: EVT-[eventId]-[userId]-[timestamp]
     */
    public static String generateTicketCode(int eventId, int userId) {
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(7);
        return "EVT-" + eventId + "-" + userId + "-" + timestamp;
    }

    /**
     * Vérifie si le ticket est valide (non utilisé)
     */
    public boolean isValid() {
        return !isUsed;
    }

    /**
     * Marque le ticket comme utilisé
     */
    public void markAsUsed() {
        this.isUsed = true;
        this.usedAt = LocalDateTime.now();
    }

    /**
     * Retourne la date de création formatée
     */
    public String getFormattedCreatedAt() {
        if (createdAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return createdAt.format(formatter);
    }

    /**
     * Retourne la date d'utilisation formatée
     */
    public String getFormattedUsedAt() {
        if (usedAt == null) return "Non utilisé";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return usedAt.format(formatter);
    }

    @Override
    public String toString() {
        return "EventTicket{" +
                "id=" + id +
                ", ticketCode='" + ticketCode + '\'' +
                ", eventId=" + eventId +
                ", userId=" + userId +
                ", isUsed=" + isUsed +
                '}';
    }
}