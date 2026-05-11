package com.example.pidev.controller.budget;

import com.example.pidev.model.budget.Budget;
import com.example.pidev.model.depense.Depense;
import com.example.pidev.model.event.Event;
import com.example.pidev.service.budget.BudgetService;
import com.example.pidev.service.depense.DepenseService;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.event.EventTicketService;
import com.example.pidev.service.forecast.EconomicForecastService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class BudgetDetailsController {

    @FXML private Label eventTitleLabel;
    @FXML private Label initialLabel;
    @FXML private Label expensesLabel;
    @FXML private Label revenueLabel;
    @FXML private Label rentLabel;
    @FXML private Label statusLabel;
    @FXML private Label forecastLabel;
    @FXML private Label adjustedForecastLabel;
    @FXML private TextField ticketPriceField;
    @FXML private Label soldTicketsLabel;
    @FXML private Label participantsToTargetLabel;
    @FXML private Label participantsToBreakEvenLabel;
    @FXML private Label capacityAlertLabel;
    @FXML private Button closeBtn;

    private Budget currentBudget;
    private Runnable onCloseAction;
    private int currentEventCapacity;
    private int currentSoldTickets;
    private double currentTicketPrice;

    private final BudgetService budgetService = new BudgetService();
    private final DepenseService depenseService = new DepenseService();
    private final EconomicForecastService forecastService = new EconomicForecastService();
    private final EventService eventService = new EventService();
    private final EventTicketService eventTicketService = new EventTicketService();

    @FXML
    private void initialize() {
        if (ticketPriceField != null) {
            ticketPriceField.setEditable(false);
            ticketPriceField.setFocusTraversable(false);
        }
    }

    public void setBudget(Budget budget) {
        this.currentBudget = budget;
        displayBudget();
    }

    public void setOnCloseAction(Runnable action) {
        this.onCloseAction = action;
    }

    private void displayBudget() {
        if (currentBudget == null) {
            return;
        }

        String eventTitle = budgetService.getEventTitleById(currentBudget.getEvent_id());
        eventTitleLabel.setText(eventTitle);

        initialLabel.setText(String.format("%,.2f DT", currentBudget.getInitial_budget()));
        expensesLabel.setText(String.format("%,.2f DT", currentBudget.getTotal_expenses()));
        revenueLabel.setText(String.format("%,.2f DT", currentBudget.getTotal_revenue()));

        double rent = currentBudget.getRentabilite();
        rentLabel.setText(String.format("%,.2f DT", rent));
        if (rent >= 0) {
            statusLabel.setText("Rentable");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #16a34a; -fx-font-weight: bold; " +
                    "-fx-background-color: #d1fae5; -fx-background-radius: 8; -fx-padding: 8 12;");
        } else {
            statusLabel.setText("Deficit");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #b91c1c; -fx-font-weight: bold; " +
                    "-fx-background-color: #fee2e2; -fx-background-radius: 8; -fx-padding: 8 12;");
        }

        double initial = currentBudget.getInitial_budget();
        double totalExpenses = currentBudget.getTotal_expenses();
        double remaining = initial - totalExpenses;

        List<Depense> depenses = depenseService.getDepensesByBudgetId(currentBudget.getId());
        long daysLeft = -1;

        if (!depenses.isEmpty() && remaining > 0) {
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

            if (earliest != null && latest != null && !earliest.equals(latest)) {
                long daysSpan = ChronoUnit.DAYS.between(earliest, latest);
                if (daysSpan > 0) {
                    double avgDaily = totalExpenses / daysSpan;
                    if (avgDaily > 0) {
                        daysLeft = (long) (remaining / avgDaily);
                        forecastLabel.setText("Le budget couvre environ " + daysLeft + " jours au rythme actuel.");
                    } else {
                        forecastLabel.setText("Budget restant : " + String.format("%,.2f DT", remaining));
                    }
                } else {
                    forecastLabel.setText("Budget restant : " + String.format("%,.2f DT", remaining));
                }
            } else {
                forecastLabel.setText("Budget restant : " + String.format("%,.2f DT", remaining));
            }
        } else {
            if (remaining >= 0) {
                forecastLabel.setText("Budget restant : " + String.format("%,.2f DT", remaining));
            } else {
                forecastLabel.setText("Depassement budgetaire de " + String.format("%,.2f DT", -remaining));
            }
        }

        if (daysLeft > 0 && remaining > 0) {
            double adjustedRemaining = forecastService.adjustForInflation(remaining, (int) daysLeft, "TND");
            double usdToTnd = forecastService.getExchangeRate("USD", "TND");
            String adjustedText = String.format("Budget ajuste (inflation) : ~%,.2f DT", adjustedRemaining);
            if (usdToTnd > 0) {
                adjustedText += String.format(" (1 USD = %.4f TND)", usdToTnd);
            }
            adjustedForecastLabel.setText(adjustedText);
            adjustedForecastLabel.setStyle("-fx-text-fill: #2563eb; -fx-font-size: 12px;");
        } else {
            adjustedForecastLabel.setText("");
        }

        setupTicketForecast();
    }

    private void setupTicketForecast() {
        currentEventCapacity = 0;
        currentSoldTickets = 0;
        currentTicketPrice = 0;

        if (currentBudget == null) {
            return;
        }

        Event event = eventService.getEventById(currentBudget.getEvent_id());
        if (event != null) {
            currentEventCapacity = event.getCapacity();
            currentTicketPrice = event.getTicketPrice();
            if (ticketPriceField != null) {
                if (currentTicketPrice > 0) {
                    ticketPriceField.setText(String.format(Locale.FRANCE, "%.2f", currentTicketPrice));
                } else {
                    ticketPriceField.setText("0,00");
                }
            }
        }

        currentSoldTickets = eventTicketService.countTicketsByEvent(currentBudget.getEvent_id());
        if (soldTicketsLabel != null) {
            soldTicketsLabel.setText("Tickets vendus : " + currentSoldTickets);
        }

        updateTicketForecast();
    }

    private void updateTicketForecast() {
        if (currentBudget == null) {
            return;
        }

        if (currentTicketPrice <= 0) {
            if (participantsToTargetLabel != null) {
                participantsToTargetLabel.setText("Participants necessaires pour atteindre le budget initial : --");
            }
            if (participantsToBreakEvenLabel != null) {
                participantsToBreakEvenLabel.setText("Participants necessaires pour couvrir les depenses : --");
            }
            if (capacityAlertLabel != null) {
                capacityAlertLabel.setText("Prix billet non defini dans la table event.");
                capacityAlertLabel.setStyle("-fx-text-fill: #b45309; -fx-font-size: 12px; -fx-font-weight: bold;");
            }
            return;
        }

        long participantsForTarget = forecastService.calculateParticipantsToReachTarget(
                currentBudget.getInitial_budget(),
                currentBudget.getTotal_revenue(),
                currentTicketPrice
        );

        long participantsForBreakEven = forecastService.calculateParticipantsToCoverDeficit(
                currentBudget.getTotal_expenses(),
                currentBudget.getTotal_revenue(),
                currentTicketPrice
        );

        if (participantsToTargetLabel != null) {
            participantsToTargetLabel.setText("Participants necessaires pour atteindre le budget initial : " + participantsForTarget);
        }
        if (participantsToBreakEvenLabel != null) {
            participantsToBreakEvenLabel.setText("Participants necessaires pour couvrir les depenses : " + participantsForBreakEven);
        }

        if (capacityAlertLabel != null) {
            if (currentEventCapacity <= 0) {
                capacityAlertLabel.setText("Capacite de l'evenement non definie.");
                capacityAlertLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");
                return;
            }

            int seatsLeft = Math.max(0, currentEventCapacity - currentSoldTickets);
            long maxNeeded = Math.max(participantsForTarget, participantsForBreakEven);

            if (maxNeeded > seatsLeft) {
                capacityAlertLabel.setText("Alerte : il faut " + maxNeeded + " participants mais il reste seulement " + seatsLeft + " places.");
                capacityAlertLabel.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 12px; -fx-font-weight: bold;");
            } else {
                capacityAlertLabel.setText("Simulation valide : " + seatsLeft + " places restantes pour " + maxNeeded + " participants.");
                capacityAlertLabel.setStyle("-fx-text-fill: #15803d; -fx-font-size: 12px; -fx-font-weight: bold;");
            }
        }
    }

    @FXML
    private void onClose() {
        if (onCloseAction != null) {
            onCloseAction.run();
        }
    }
}