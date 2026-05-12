package com.example.pidev.controller.depense;

import com.example.pidev.model.depense.Depense;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;

public class DepenseCardController {

    @FXML private Label descLabel;
    @FXML private Label amountLabel;

    @FXML private Label categoryLabel;
    @FXML private Label dateLabel;

    @FXML private Button detailsBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void setData(Depense d, Runnable onDetails, Runnable onEdit, Runnable onDelete) {
        if (d == null) return;

        if (descLabel != null) descLabel.setText(nvl(d.getDescription(), "—"));
        if (amountLabel != null) amountLabel.setText(String.format("%,.2f DT", d.getAmount()));

        if (categoryLabel != null) categoryLabel.setText(nvl(d.getCategory(), "—"));
        if (dateLabel != null) {
            dateLabel.setText(d.getExpense_date() == null ? "—" : FMT.format(d.getExpense_date()));
        }

        if (detailsBtn != null) detailsBtn.setOnAction(e -> { if (onDetails != null) onDetails.run(); });
        if (editBtn != null) editBtn.setOnAction(e -> { if (onEdit != null) onEdit.run(); });
        if (deleteBtn != null) deleteBtn.setOnAction(e -> { if (onDelete != null) onDelete.run(); });
    }

    private String nvl(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
