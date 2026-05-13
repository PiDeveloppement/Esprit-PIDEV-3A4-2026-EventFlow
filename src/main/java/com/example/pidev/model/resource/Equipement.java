package com.example.pidev.model.resource;

public class Equipement {
    private int id;
    private String name;
    private String type;
    private String status;
    private int quantity;
    private int originalQuantity;
    private String imagePath;

    public Equipement(int id, String name, String type, String status, int quantity, int originalQuantity, String imagePath) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.quantity = quantity;
        this.originalQuantity = originalQuantity;
        this.imagePath = imagePath;
    }

    // Constructor for new equipment (sets original_quantity to quantity)
    public Equipement(int id, String name, String type, String status, int quantity, String imagePath) {
        this(id, name, type, status, quantity, quantity, imagePath);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public int getQuantity() { return quantity; }
    public int getOriginalQuantity() { return originalQuantity; }
    public String getImagePath() { return imagePath; }

    @Override
    public String toString() {
        return name;
    }
}