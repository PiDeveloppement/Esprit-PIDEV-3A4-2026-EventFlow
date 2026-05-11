package com.example.pidev.controller.budget;

import com.example.pidev.model.budget.Budget;
import com.example.pidev.model.depense.Depense;
import com.example.pidev.service.budget.BudgetService;
import com.example.pidev.service.depense.DepenseService;
import com.example.pidev.service.forecast.EconomicForecastService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

public class BudgetCardController {

    @FXML private AnchorPane rootPane;
    @FXML private Label titleLabel;
    @FXML private Label eventLabelPrefix;
    @FXML private Label eventLabel;
    @FXML private Label initialLabel;
    @FXML private Label expensesLabel;
    @FXML private Label revenueLabel;
    @FXML private Label rentLabel;
    @FXML private Label rent2Label;
    @FXML private Label statusLabel;
    @FXML private Button detailsBtn;
    @FXML private Button editBtn;
    @FXML private Button deleteBtn;
    @FXML private Label alertLabel;
    @FXML private Label forecastLabel;
    @FXML private Label adjustedForecastLabel;

    private final BudgetService budgetService = new BudgetService();
    private final DepenseService depenseService = new DepenseService();
    private final EconomicForecastService forecastService = new EconomicForecastService();

    public void setData(Budget b, Runnable onDetails, Runnable onEdit, Runnable onDelete) {
        if (b == null) return;

        // Informations générales
        if (titleLabel != null) titleLabel.setText("Budget");
        if (eventLabel != null) {
            try {
                String eventTitle = budgetService.getEventTitleById(b.getEvent_id());
                eventLabel.setText(eventTitle);
            } catch (Exception e) {
                eventLabel.setText("—");
            }
        }
        if (initialLabel != null) initialLabel.setText(String.format("%,.2f DT", b.getInitial_budget()));
        if (expensesLabel != null) expensesLabel.setText(String.format("%,.2f DT", b.getTotal_expenses()));
        if (revenueLabel != null) revenueLabel.setText(String.format("%,.2f DT", b.getTotal_revenue()));

        double rent = b.getRentabilite();
        if (rentLabel != null) {
            rentLabel.setText(String.format("%,.2f DT", rent));
            rentLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill:" + (rent >= 0 ? "#059669" : "#dc2626") + ";");
        }
        if (rent2Label != null) {
            rent2Label.setText(String.format("%,.2f DT", rent));
            rent2Label.setStyle("-fx-font-weight: 900; -fx-text-fill:" + (rent >= 0 ? "#059669" : "#dc2626") + ";");
        }
        if (statusLabel != null) {
            if (rent >= 0) {
                statusLabel.setText("OK");
                statusLabel.setStyle("-fx-background-color:#ecfdf5; -fx-text-fill:#047857; -fx-font-weight:900;"
                        + "-fx-padding:4 10; -fx-background-radius:999;");
            } else {
                statusLabel.setText("Déficit");
                statusLabel.setStyle("-fx-background-color:#fee2e2; -fx-text-fill:#b91c1c; -fx-font-weight:900;"
                        + "-fx-padding:4 10; -fx-background-radius:999;");
            }
        }

        // Alerte de dépassement
        double initial = b.getInitial_budget();
        double totalExpenses = b.getTotal_expenses();
        boolean overBudget = false;

        if (alertLabel != null) {
            if (initial > 0) {
                double usagePercent = (totalExpenses / initial) * 100;
                String usageText = String.format("%.1f%% utilisé", usagePercent);

                if (usagePercent >= 100) {
                    alertLabel.setText("🚨 Budget DÉPASSÉ !");
                    alertLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold; -fx-font-size: 14px;");
                    overBudget = true;
                } else if (usagePercent >= 80) {
                    alertLabel.setText("⚠️ " + usageText);
                    alertLabel.setStyle("-fx-text-fill: #f97316; -fx-font-weight: bold;");
                } else {
                    alertLabel.setText(usageText);
                    alertLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                }
            } else {
                alertLabel.setText("Budget initial nul");
                alertLabel.setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
            }
        }

        // Montant restant
        double remaining = initial - totalExpenses;
        String remainingText = String.format("%,.2f DT restants", remaining);
        if (forecastLabel != null) {
            forecastLabel.setText(remainingText);
            if (remaining < 0) {
                forecastLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-weight: bold;");
            } else {
                forecastLabel.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
            }
        }

        // Style de la carte
        if (rootPane != null) {
            rootPane.getStyleClass().removeAll("budget-card-normal", "budget-card-alert");
            if (overBudget) {
                rootPane.getStyleClass().add("budget-card-alert");
            } else {
                rootPane.getStyleClass().add("budget-card-normal");
            }
        }

        // Prévision des jours restants et ajustement économique
        List<Depense> depenses = depenseService.getDepensesByBudgetId(b.getId());
        long daysLeft = -1;
        if (!depenses.isEmpty()) {
            LocalDate earliest = depenses.stream()
                    .map(Depense::getExpense_date)
                    .filter(Objects::nonNull)
                    .min(LocalDate::compareTo)
                    .orElse(null);
            LocalDate latest = depenses.stream()
                    .map(Depense::getExpense_date)
                    .filter(Objects::nonNull)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            if (earliest != null && latest != null && !earliest.equals(latest) && remaining > 0) {
                long daysSpan = ChronoUnit.DAYS.between(earliest, latest);
                if (daysSpan > 0) {
                    double avgDaily = totalExpenses / daysSpan;
                    if (avgDaily > 0) {
                        daysLeft = (long) (remaining / avgDaily);
                        if (forecastLabel != null) {
                            forecastLabel.setText(remainingText + "  (~" + daysLeft + " jours)");
                        }
                    }
                }
            }
        }

        // Prévision ajustée par l'économie (via ExchangeRate-API)
        if (adjustedForecastLabel != null) {
            if (daysLeft > 0 && remaining > 0) {
                // Utiliser TND comme devise par défaut (vous pouvez la rendre dynamique)
                double adjustedRemaining = forecastService.adjustForInflation(remaining, (int) daysLeft, "TND");

                // Taux de change USD/TND pour info (optionnel)
                double usdToTnd = forecastService.getExchangeRate("USD", "TND");

                String adjustedText;
                if (adjustedRemaining > 0) {
                    adjustedText = String.format("Ajusté (inflation estimée) : ~%,.2f DT", adjustedRemaining);
                    if (usdToTnd > 0) {
                        adjustedText += String.format("  (1 USD = %.4f TND)", usdToTnd);
                    }
                } else {
                    adjustedText = "";
                }
                adjustedForecastLabel.setText(adjustedText);
                adjustedForecastLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 11px;");
            } else {
                adjustedForecastLabel.setText("");
            }
        }

        // Boutons
        if (detailsBtn != null) detailsBtn.setOnAction(e -> { if (onDetails != null) onDetails.run(); });
        if (editBtn != null) editBtn.setOnAction(e -> { if (onEdit != null) onEdit.run(); });
        if (deleteBtn != null) deleteBtn.setOnAction(e -> { if (onDelete != null) onDelete.run(); });
    }
}