package com.example.pidev.model.event;

import java.time.LocalDateTime;

/**
 * Entité EventCategory - Représente une catégorie d'événement
 * Correspond à la table 'event_category' en base de données
 *
 * @author Ons Abdesslem
 * @version 1.0
 */
public class EventCategory {

    // ==================== ATTRIBUTS ====================

    private int id;
    private String name;
    private String description;
    private String icon;
    private String color;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Attribut calculé (pas en base)
    private int eventCount; // Nombre d'événements dans cette catégorie


    // ==================== CONSTRUCTEURS ====================

    /**
     * Constructeur par défaut
     */
    public EventCategory() {
        this.isActive = true; // Par défaut active
    }

    /**
     * Constructeur pour création (sans ID)
     */
    public EventCategory(String name, String description, String icon, String color) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.color = color;
        this.isActive = true;
    }

    /**
     * Constructeur pour création avec statut
     */
    public EventCategory(String name, String description, String icon, String color, boolean isActive) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.color = color;
        this.isActive = isActive;
    }

    /**
     * Constructeur complet (pour lecture depuis BDD)
     */
    public EventCategory(int id, String name, String description, String icon,
                         String color, boolean isActive, LocalDateTime createdAt,
                         LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.color = color;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }


    // ==================== GETTERS & SETTERS ====================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
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

    public int getEventCount() {
        return eventCount;
    }

    public void setEventCount(int eventCount) {
        this.eventCount = eventCount;
    }


    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Retourne le statut sous forme de badge texte
     */
    public String getStatusBadge() {
        return isActive ? "✅ Active" : "❌ Inactive";
    }

    /**
     * Retourne l'icône avec le nom (pour affichage)
     */
    public String getDisplayName() {
        return (icon != null ? icon + " " : "") + name;
    }

    /**
     * Vérifie si la catégorie est valide pour insertion/update
     */
    public boolean isValid() {
        return name != null && !name.trim().isEmpty() && name.length() <= 100;
    }


    // ==================== toString ====================

    @Override
    public String toString() {
        return "EventCategory{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", icon='" + icon + '\'' +
                ", color='" + color + '\'' +
                ", isActive=" + isActive +
                ", eventCount=" + eventCount +
                '}';
    }


    // ==================== equals & hashCode ====================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EventCategory that = (EventCategory) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}