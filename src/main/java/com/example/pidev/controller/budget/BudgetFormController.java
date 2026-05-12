package com.example.pidev.controller.budget;

import com.example.pidev.model.budget.Budget;
import com.example.pidev.service.budget.BudgetService;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.function.Consumer;

public class BudgetFormController {

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> eventComboBox;
    @FXML private TextField initialField;
    @FXML private TextField revenueField;
    @FXML private Label errorLabel;

    private Budget editing;
    private Budget result;
    private Runnable onFormDone;
    private Consumer<Budget> onSaved;

    private final BudgetService budgetService = new BudgetService();

    public void setOnFormDone(Runnable callback) { this.onFormDone = callback; }
    public void setOnSaved(Consumer<Budget> callback) { this.onSaved = callback; }
    public Budget getResult() { return result; }

    @FXML
    private void initialize() {
        loadEventTitles();
    }

    private void loadEventTitles() {
        try {
            ObservableList<String> titles = budgetService.getAllEventTitles();
            eventComboBox.setItems(titles);
        } catch (Exception e) {
            error("Erreur chargement événements: " + e.getMessage());
        }
    }

    public void setModeAdd() {
        titleLabel.setText("➕ Nouveau Budget");
        editing = null;
        result = null;
        clearErrors();
        eventComboBox.setValue(null);
        initialField.clear();
        revenueField.clear();
    }

    public void setModeEdit(Budget b) {
        titleLabel.setText("✏ Modifier Budget");
        editing = b;
        result = null;
        clearErrors();

        try {
            String eventTitle = budgetService.getEventTitleById(b.getEvent_id());
            eventComboBox.setValue(eventTitle);
        } catch (Exception ignored) {}

        initialField.setText(String.valueOf(b.getInitial_budget()));
        revenueField.setText(String.valueOf(b.getTotal_revenue()));
    }

    @FXML
    private void onSave() {
        clearErrors();

        String selectedEvent = eventComboBox.getValue();
        String initialText = initialField.getText() == null ? "" : initialField.getText().trim();
        String revenueText = revenueField.getText() == null ? "" : revenueField.getText().trim();

        if (selectedEvent == null || selectedEvent.isEmpty()) {
            error("Veuillez sélectionner un événement.");
            return;
        }

        if (initialText.isEmpty()) {
            error("Le budget initial est obligatoire.");
            return;
        }

        double initial;
        try {
            initial = Double.parseDouble(initialText);
            if (initial <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            error("Budget initial invalide (doit être > 0).");
            return;
        }

        double revenue = 0;
        if (!revenueText.isEmpty()) {
            try {
                revenue = Double.parseDouble(revenueText);
                if (revenue < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                error("Revenus invalides.");
                return;
            }
        }

        int eventId;
        try {
            eventId = budgetService.getEventIdByTitle(selectedEvent);
        } catch (SQLException e) {
            error("Événement invalide: " + e.getMessage());
            return;
        }

        Budget budget;
        if (editing == null) {
            budget = new Budget(eventId, initial, revenue);
        } else {
            budget = editing;
            budget.setEvent_id(eventId);
            budget.setInitial_budget(initial);
            budget.setTotal_revenue(revenue);
        }

        try {
            if (editing == null) budgetService.addBudget(budget);
            else budgetService.updateBudget(budget);

            // ✅ PAS besoin de getId() ni getBudgets()
            // On renvoie simplement l'objet budget mis à jour
            result = budget;

            if (onSaved != null) onSaved.accept(result);
            if (onFormDone != null) onFormDone.run();
            closeWindow();

        } catch (Exception e) {
            error("Erreur lors de l'enregistrement : " + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        if (onFormDone != null) onFormDone.run();
        closeWindow();
    }

    private void closeWindow() {
        try {
            Stage stage = (Stage) eventComboBox.getScene().getWindow();
            stage.close();
        } catch (Exception ignored) {}
    }

    private void error(String msg) {
        errorLabel.setText("❌ " + msg);
    }

    private void clearErrors() {
        errorLabel.setText("");
    }
}