package com.example.pidev.controller.budget;

import com.example.pidev.model.budget.Budget;
import com.example.pidev.service.budget.BudgetService;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class BudgetFormController {

    @FXML private Label titleLabel;
    @FXML private ComboBox<String> eventComboBox;
    @FXML private TextField initialField;
    @FXML private TextField revenueField;
    @FXML private Label errorLabel;

    private Budget editing;
    private Budget result;
    private Runnable onFormDone;

    private final BudgetService budgetService = new BudgetService();

    public void setOnFormDone(Runnable callback) { this.onFormDone = callback; }
    public Budget getResult() { return result; }

    @FXML
    private void initialize() {
        loadEventTitles();
    }

    private void loadEventTitles() {
        try {
            eventComboBox.setItems(budgetService.getAllEventTitles());
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
        } catch (Exception e) {
            // ignore
        }
        initialField.setText(String.valueOf(b.getInitial_budget()));
        revenueField.setText(String.valueOf(b.getTotal_revenue()));
    }

    @FXML
    private void onSave() {
        clearErrors();

        String selectedEvent = eventComboBox.getValue();
        String initialTxt = initialField.getText().trim().replace(",", ".");
        String revenueTxt = revenueField.getText().trim().replace(",", ".");

        if (selectedEvent == null || selectedEvent.isEmpty()) {
            error("Sélectionnez un événement");
            return;
        }

        int eventId;
        try {
            eventId = budgetService.getEventIdByTitle(selectedEvent);
        } catch (Exception e) {
            error("Événement invalide");
            return;
        }

        double initial, revenue;
        try {
            initial = Double.parseDouble(initialTxt);
            if (initial < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            error("Budget initial invalide (doit être >= 0)");
            return;
        }

        try {
            revenue = Double.parseDouble(revenueTxt);
            if (revenue < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            error("Revenus invalides (doit être >= 0)");
            return;
        }

        Budget out = new Budget();
        if (editing != null) out.setId(editing.getId());
        out.setEvent_id(eventId);
        out.setInitial_budget(initial);
        out.setTotal_revenue(revenue);
        out.setTotal_expenses(editing == null ? 0 : editing.getTotal_expenses());
        out.setRentabilite(revenue - (editing == null ? 0 : editing.getTotal_expenses()));

        try {
            if (editing == null) budgetService.addBudget(out);
            else budgetService.updateBudget(out);

            result = out;
            if (onFormDone != null) onFormDone.run();

            closeWindowIfModal();

        } catch (Exception ex) {
            error("Erreur sauvegarde: " + ex.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        result = null;
        if (onFormDone != null) onFormDone.run();
        closeWindowIfModal();
    }

    private void closeWindowIfModal() {
        try {
            Stage stage = (Stage) eventComboBox.getScene().getWindow();
            if (stage != null && stage.isShowing()) stage.close();
        } catch (Exception ignored) {}
    }

    private void error(String msg) {
        if (errorLabel != null) errorLabel.setText("❌ " + msg);
    }

    private void clearErrors() {
        if (errorLabel != null) errorLabel.setText("");
    }
}