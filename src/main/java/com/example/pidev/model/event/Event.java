package com.example.pidev.model.event;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modèle représentant un événement
 * @author Ons Abdesslem
 */
public class Event {

    // ==================== CONSTANTES ====================

    public enum EventStatus {
        DRAFT("Brouillon"),
        PUBLISHED("Publié");

        private final String displayName;

        EventStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // ==================== ATTRIBUTS ====================

    private int id;
    private String title;
    private String description;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;
    private String gouvernorat;
    private String ville;
    private int capacity;
    private String imageUrl;
    private int categoryId;
    private int createdBy;
    private EventStatus status;
    private boolean isFree;
    private double ticketPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ==================== CONSTRUCTEURS ====================

    public Event() {
        this.status = EventStatus.DRAFT;
        this.isFree = true;
        this.ticketPrice = 0.0;
        this.capacity = 50;
    }

    public Event(String title, String description, LocalDateTime startDate, LocalDateTime endDate,
                 String location, int capacity, int categoryId, int createdBy) {
        this();
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.capacity = capacity;
        this.categoryId = categoryId;
        this.createdBy = createdBy;
    }

    public Event(String title, String description, LocalDateTime startDate, LocalDateTime endDate,
                 String location, int capacity, String imageUrl, int categoryId, int createdBy,
                 boolean isFree, double ticketPrice) {
        this(title, description, startDate, endDate, location, capacity, categoryId, createdBy);
        this.imageUrl = imageUrl;
        this.isFree = isFree;
        this.ticketPrice = ticketPrice;
    }

    // ==================== GETTERS & SETTERS ====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGouvernorat() {
        return gouvernorat;
    }

    public void setGouvernorat(String gouvernorat) {
        this.gouvernorat = gouvernorat;
    }

    public String getVille() {
        return ville;
    }

    public void setVille(String ville) {
        this.ville = ville;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public int getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(int createdBy) {
        this.createdBy = createdBy;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public void setStatus(String status) {
        try {
            this.status = EventStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            this.status = EventStatus.DRAFT;
        }
    }

    public boolean isFree() {
        return isFree;
    }

    public void setFree(boolean free) {
        isFree = free;
    }

    public double getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(double ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Vérifie si l'événement est valide (champs obligatoires)
     */
    public boolean isValid() {
        return title != null && !title.trim().isEmpty() &&
                startDate != null && endDate != null &&
                startDate.isBefore(endDate) &&
                categoryId > 0 && createdBy > 0;
    }
    public Event(int id, String title) {
        this.id = id;
        this.title = title;
    }

    /**
     * Vérifie si l'événement est complet
     */
    public boolean isFull() {
        return capacity <= 0; // À améliorer avec le nombre d'inscriptions
    }

    /**
     * Retourne la durée de l'événement en heures
     */
    public double getDurationInHours() {
        if (startDate == null || endDate == null) return 0;
        return java.time.Duration.between(startDate, endDate).toHours();
    }

    /**
     * Retourne le statut affichable
     */
    public String getStatusDisplay() {
        return status != null ? status.getDisplayName() : "Inconnu";
    }

    /**
     * Retourne le type de prix
     */
    public String getPriceDisplay() {
        if (isFree) {
            return "Gratuit";
        } else {
            return String.format("%.2f DT", ticketPrice);
        }
    }

    /**
     * Retourne la date de début formatée
     */
    public String getFormattedStartDate() {
        if (startDate == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return startDate.format(formatter);
    }

    /**
     * Retourne la date de fin formatée
     */
    public String getFormattedEndDate() {
        if (endDate == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return endDate.format(formatter);
    }

    /**
     * Retourne la date de création formatée
     */
    public String getFormattedCreatedAt() {
        if (createdAt == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        return createdAt.format(formatter);
    }

    @Override
    public String toString() {
        return "Event{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", startDate=" + startDate +
                ", status=" + status +
                '}';
    }

    /**
     * Retourne le titre pour affichage
     */
    public String getDisplayName() {
        return title + (status != null ? " (" + status.getDisplayName() + ")" : "");
    }
}