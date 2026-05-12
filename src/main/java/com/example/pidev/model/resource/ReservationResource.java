package com.example.pidev.model.resource;
import java.time.LocalDateTime;

public class ReservationResource {
    private int id;
    private String resourceType;
    private Integer salleId, equipementId;
    private LocalDateTime startTimedate, endTime;
    private int quantity;
    private String resourceName; // Pour l'affichage
    private String imagePath;    // Pour l'affichage
    private int userId;

    // Constructeurs, Getters et Setters...
    public ReservationResource() {}
    public ReservationResource(int id, String type, Integer sId, Integer eId, LocalDateTime start, LocalDateTime end, int qty) {
        this.id = id; this.resourceType = type; this.salleId = sId; this.equipementId = eId;
        this.startTimedate = start; this.endTime = end; this.quantity = qty;
    }
    // Ajoutez les getters/setters pour resourceName et imagePath
    public String getResourceName() { return resourceName; }
    public void setResourceName(String resourceName) { this.resourceName = resourceName; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public int getId() { return id; }
    public String getResourceType() { return resourceType; }
    public Integer getSalleId() { return salleId; }
    public Integer getEquipementId() { return equipementId; }
    public LocalDateTime getStartTimedate() { return startTimedate; }
    public LocalDateTime getEndTime() { return endTime; }
    public int getQuantity() { return quantity; }
    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}