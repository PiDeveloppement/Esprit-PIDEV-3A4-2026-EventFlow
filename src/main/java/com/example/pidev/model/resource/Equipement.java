package com.example.pidev.model.resource;

public class Equipement {
    private int id;
    private String name;
    private String type;
    private String status;
    private int quantity;
    private String imagePath;

    public Equipement(int id, String name, String type, String status, int quantity, String imagePath) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.quantity = quantity;
        this.imagePath = imagePath;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getStatus() { return status; }
    public int getQuantity() { return quantity; }
    public String getImagePath() { return imagePath; }

    @Override
    public String toString() {
        return name;
    }
}