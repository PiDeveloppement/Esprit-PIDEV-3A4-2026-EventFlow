package com.example.pidev.controller.budget;

import com.example.pidev.model.budget.Budget;
import com.example.pidev.service.budget.BudgetService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class BudgetDetailsController {

    @FXML private Label idLabel;
    @FXML private Label eventIdLabel;
    @FXML private Label statusLabel;
    @FXML private Label initialLabel;
    @FXML private Label expensesLabel;
    @FXML private Label revenueLabel;
    @FXML private Label rentLabel;

    private final BudgetService budgetService = new BudgetService();
    private Runnable onCloseAction;

    public void setOnCloseAction(Runnable onCloseAction) {
        this.onCloseAction = onCloseAction;
    }

    public void setBudget(Budget b) {
        if (b == null) return;

        if (idLabel != null) idLabel.setText(String.valueOf(b.getId()));

        if (eventIdLabel != null) {
            try {
                String eventTitle = budgetService.getEventTitleById(b.getEvent_id());
                eventIdLabel.setText("Événement: " + eventTitle);
            } catch (Exception e) {
                eventIdLabel.setText("Événement: (ID: " + b.getEvent_id() + ")");
            }
        }

        if (initialLabel != null) initialLabel.setText(String.format("%,.2f DT", b.getInitial_budget()));
        if (expensesLabel != null) expensesLabel.setText(String.format("%,.2f DT", b.getTotal_expenses()));
        if (revenueLabel != null) revenueLabel.setText(String.format("%,.2f DT", b.getTotal_revenue()));

        double rent = b.getRentabilite();

        if (rentLabel != null) {
            rentLabel.setText(String.format("%,.2f DT", rent));
            rentLabel.setStyle("-fx-font-weight: 900; -fx-text-fill: " + (rent >= 0 ? "#059669" : "#dc2626") + ";");
        }

        if (statusLabel != null) {
            if (rent >= 0) {
                statusLabel.setText("✓ Rentable");
                statusLabel.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 12;");
            } else {
                statusLabel.setText("⚠ Déficit");
                statusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 8 12;");
            }
        }
    }

    @FXML
    private void onClose() {
        if (onCloseAction != null) {
            onCloseAction.run();
            return;
        }
        try {
            if (eventIdLabel != null && eventIdLabel.getScene() != null) {
                Stage st = (Stage) eventIdLabel.getScene().getWindow();
                if (st != null) st.close();
            }
        } catch (Exception ignored) {}
    }
}