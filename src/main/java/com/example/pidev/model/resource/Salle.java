package com.example.pidev.model.resource;

public class Salle {
    private int id;
    private String name;
    private int capacity;
    private String building;
    private int floor;
    private String status;
    private String imagePath;
    private double latitude;
    private double longitude;

    public Salle(int id, String name, int capacity, String building, int floor, String status, String imagePath, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.building = building;
        this.floor = floor;
        this.status = status;
        this.imagePath = imagePath;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters (Indispensables pour que le tableau affiche les données)
    public int getId() { return id; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public String getBuilding() { return building; }
    public int getFloor() { return floor; }
    public String getStatus() { return status; }
    public String getImagePath() { return imagePath; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}