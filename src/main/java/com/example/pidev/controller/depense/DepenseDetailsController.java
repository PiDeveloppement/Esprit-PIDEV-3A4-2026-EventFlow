package com.example.pidev.controller.depense;

import com.example.pidev.model.depense.Depense;
import com.example.pidev.service.budget.BudgetService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;
import java.util.Locale;   // ✅ IMPORT MANQUANT

public class DepenseDetailsController {

    @FXML private Label budgetIdLabel;
    @FXML private Label descLabel;
    @FXML private Label categoryLabel;
    @FXML private Label amountLabel;
    @FXML private Label dateLabel;
    @FXML private Label statusLabel;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final BudgetService budgetService = new BudgetService();
    private Runnable onCloseAction;

    public void setOnCloseAction(Runnable r) {
        this.onCloseAction = r;
    }

    public void setDepense(Depense d) {
        if (d == null) return;

        // affichage du nom de l'événement sans ID
        String eventTitle = budgetService.getEventTitleByBudgetId(d.getBudget_id());
        budgetIdLabel.setText("Budget : " + eventTitle);

        descLabel.setText(nullSafe(d.getDescription()));
        categoryLabel.setText(nullSafe(d.getCategory()));

        amountLabel.setText(String.format(Locale.US, "%,.2f DT", d.getAmount()));
        dateLabel.setText(d.getExpense_date() == null ? "—" : FMT.format(d.getExpense_date()));

        statusLabel.setText("Enregistrée");
    }

    @FXML
    private void onClose() {
        if (onCloseAction != null) {
            onCloseAction.run();
        }
    }

    private String nullSafe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}