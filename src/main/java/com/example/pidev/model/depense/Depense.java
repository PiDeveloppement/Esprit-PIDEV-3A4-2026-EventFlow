package com.example.pidev.model.depense;

import java.time.LocalDate;

public class Depense {
    private int id;
    private int budget_id;
    private String description;
    private double amount;
    private String category;
    private LocalDate expense_date;

    public Depense() {}

    public Depense(int id, int budget_id, String description, double amount, String category, LocalDate expense_date) {
        this.id = id;
        this.budget_id = budget_id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.expense_date = expense_date;
    }

    public Depense(int budget_id, String description, double amount, String category, LocalDate expense_date) {
        this.budget_id = budget_id;
        this.description = description;
        this.amount = amount;
        this.category = category;
        this.expense_date = expense_date;
    }

    public int getId() { return id; }
    public int getBudget_id() { return budget_id; }
    public String getDescription() { return description; }
    public double getAmount() { return amount; }
    public String getCategory() { return category; }
    public LocalDate getExpense_date() { return expense_date; }

    public void setId(int id) { this.id = id; }
    public void setBudget_id(int budget_id) { this.budget_id = budget_id; }
    public void setDescription(String description) { this.description = description; }
    public void setAmount(double amount) { this.amount = amount; }
    public void setCategory(String category) { this.category = category; }
    public void setExpense_date(LocalDate expense_date) { this.expense_date = expense_date; }
}
