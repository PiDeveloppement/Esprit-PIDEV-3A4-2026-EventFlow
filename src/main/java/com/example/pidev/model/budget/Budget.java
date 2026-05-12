package com.example.pidev.model.budget;

public class Budget {
    private int id;
    private int event_id;
    private double initial_budget;
    private double total_expenses;
    private double total_revenue;
    private double rentabilite;

    public Budget() {}

    public Budget(int id, int event_id, double initial_budget, double total_expenses, double total_revenue, double rentabilite) {
        this.id = id;
        this.event_id = event_id;
        this.initial_budget = initial_budget;
        this.total_expenses = total_expenses;
        this.total_revenue = total_revenue;
        this.rentabilite = rentabilite;
    }

    public Budget(int event_id, double initial_budget, double total_revenue) {
        this.event_id = event_id;
        this.initial_budget = initial_budget;
        this.total_revenue = total_revenue;
        this.total_expenses = 0;
        this.rentabilite = total_revenue;
    }

    public int getId() { return id; }
    public int getEvent_id() { return event_id; }
    public double getInitial_budget() { return initial_budget; }
    public double getTotal_expenses() { return total_expenses; }
    public double getTotal_revenue() { return total_revenue; }
    public double getRentabilite() { return rentabilite; }

    public void setId(int id) { this.id = id; }
    public void setEvent_id(int event_id) { this.event_id = event_id; }
    public void setInitial_budget(double initial_budget) { this.initial_budget = initial_budget; }
    public void setTotal_expenses(double total_expenses) { this.total_expenses = total_expenses; }
    public void setTotal_revenue(double total_revenue) { this.total_revenue = total_revenue; }
    public void setRentabilite(double rentabilite) { this.rentabilite = rentabilite; }
}
