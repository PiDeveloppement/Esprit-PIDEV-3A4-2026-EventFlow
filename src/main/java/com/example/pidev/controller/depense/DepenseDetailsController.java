package com.example.pidev.controller.depense;

import com.example.pidev.model.depense.Depense;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;

public class DepenseDetailsController {

    @FXML private Label budgetIdLabel;
    @FXML private Label descLabel;
    @FXML private Label categoryLabel;
    @FXML private Label amountLabel;
    @FXML private Label dateLabel;
    @FXML private Label statusLabel;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private Runnable onCloseAction;

    public void setOnCloseAction(Runnable r) {
        this.onCloseAction = r;
    }

    public void setDepense(Depense d) {
        if (d == null) return;

        if (budgetIdLabel != null) budgetIdLabel.setText("Budget N°" + d.getBudget_id());
        if (descLabel != null) descLabel.setText(nullSafe(d.getDescription()));
        if (categoryLabel != null) categoryLabel.setText(nullSafe(d.getCategory()));

        if (amountLabel != null) amountLabel.setText(String.format("%,.2f DT", d.getAmount()));
        if (dateLabel != null) dateLabel.setText(d.getExpense_date() == null ? "—" : FMT.format(d.getExpense_date()));
        if (statusLabel != null) statusLabel.setText("Enregistrée");
    }

    @FXML
    private void onClose() {
        if (onCloseAction != null) onCloseAction.run();
    }

    private String nullSafe(String s) {
        return (s == null || s.isBlank()) ? "—" : s;
    }
}
