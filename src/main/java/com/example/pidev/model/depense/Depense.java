package com.example.pidev.model.depense;

import java.time.LocalDate;

public class Depense {
    private int id;
    private int budget_id;
    private String description;
    private double amount;          // montant en TND
    private String category;
    private LocalDate expense_date;
    private boolean anomaly;
    private String originalCurrency; // devise originale (TND par défaut)
    private Double originalAmount;   // montant dans la devise originale

    public Depense() {}

    public Depense(int id, int budget_id, String description, double amount, String category, LocalDate expense_date) {
        this.id = id;
        this.budget_id = budget_id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.expense_date = expense_date;
        this.anomaly = false;
        this.originalCurrency = "TND";
        this.originalAmount = amount;
    }

    // Getters et setters existants...
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getBudget_id() { return budget_id; }
    public void setBudget_id(int budget_id) { this.budget_id = budget_id; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public LocalDate getExpense_date() { return expense_date; }
    public void setExpense_date(LocalDate expense_date) { this.expense_date = expense_date; }
    public boolean isAnomaly() { return anomaly; }
    public void setAnomaly(boolean anomaly) { this.anomaly = anomaly; }

    // Nouveaux getters/setters
    public String getOriginalCurrency() { return originalCurrency; }
    public void setOriginalCurrency(String originalCurrency) { this.originalCurrency = originalCurrency; }
    public Double getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(Double originalAmount) { this.originalAmount = originalAmount; }
}