package com.example.pidev.controller.budget;

import com.example.pidev.MainController;
import com.example.pidev.model.budget.Budget;
import com.example.pidev.service.budget.BudgetService;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class BudgetListController implements Initializable {

    @FXML private ScrollPane pageScroll;
    @FXML private Label kpiCountLabel;
    @FXML private Label kpiInitialLabel;
    @FXML private Label kpiRentLabel;
    @FXML private Label kpiDeficitLabel;
    @FXML private ComboBox<String> healthFilter;
    @FXML private ComboBox<String> eventFilter;
    @FXML private ComboBox<String> statusFilter;
    @FXML private Button addBtn;
    @FXML private Label statusLabel;
    @FXML private TilePane cardsPane;
    @FXML private BarChart<String, Number> budgetComparisonChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private final BudgetService budgetService = new BudgetService();
    private final ObservableList<Budget> baseList = FXCollections.observableArrayList();
    private FilteredList<Budget> filtered;

    private static final String CARD_FXML    = "/com/example/pidev/fxml/Budget/budget-card.fxml";
    private static final String FORM_FXML    = "/com/example/pidev/fxml/Budget/budget-form.fxml";
    private static final String DETAILS_FXML = "/com/example/pidev/fxml/Budget/budget-detail.fxml";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filtered = new FilteredList<>(baseList, b -> true);
        filtered.addListener((ListChangeListener<Budget>) c -> renderCards());

        if (healthFilter != null) healthFilter.valueProperty().addListener((obs, o, n) -> applyPredicate());
        if (eventFilter  != null) eventFilter.valueProperty().addListener((obs, o, n) -> applyPredicate());
        if (statusFilter != null) statusFilter.valueProperty().addListener((obs, o, n) -> applyPredicate());
        if (addBtn != null) addBtn.setOnAction(e -> onAdd());

        if (cardsPane != null) {
            cardsPane.setPadding(new Insets(8));
            cardsPane.setHgap(14);
            cardsPane.setVgap(14);
        }

        if (pageScroll != null && cardsPane != null) {
            pageScroll.viewportBoundsProperty().addListener((obs, oldB, b) ->
                    cardsPane.setPrefColumns(computeCols(b.getWidth()))
            );
        }

        setupFiltersSafe();
        loadDataSafe();
        applyPredicate();
    }

    private int computeCols(double width) {
        if (width < 520) return 1;
        if (width < 980) return 2;
        return 3;
    }

    private void setupFiltersSafe() {
        if (healthFilter != null) {
            healthFilter.getItems().setAll("Tous", "🟢 Excellent", "🔵 Bon", "🟡 Fragile", "🔴 Critique");
            healthFilter.setValue("Tous");
        }

        if (statusFilter != null) {
            statusFilter.getItems().setAll("Tous", "OK", "Déficit");
            statusFilter.setValue("Tous");
        }

        if (eventFilter != null) {
            try {
                eventFilter.getItems().setAll(budgetService.getAllEventTitles());
            } catch (Exception e) {
                eventFilter.getItems().clear();
            }
            eventFilter.setValue(null);
        }
    }

    private void loadDataSafe() {
        try {
            baseList.setAll(budgetService.getAllBudgets());
            updateKpis();
            initBudgetChart();

            if (statusLabel != null) {
                statusLabel.setText("📊 " + baseList.size() + " budget(s) • Mise à jour: Maintenant");
            }

            renderCards();
        } catch (Exception e) {
            if (statusLabel != null) {
                statusLabel.setText("❌ Erreur chargement budgets: " + e.getMessage());
            }
        }
    }

    private void updateKpis() {
        if (kpiCountLabel   != null) kpiCountLabel.setText(String.valueOf(budgetService.countBudgets()));
        if (kpiInitialLabel != null) kpiInitialLabel.setText(String.format("%,.2f DT", budgetService.sumInitial()));
        if (kpiRentLabel    != null) kpiRentLabel.setText(String.format("%,.2f DT", budgetService.globalRentability()));
        if (kpiDeficitLabel != null) kpiDeficitLabel.setText(String.valueOf(budgetService.countDeficitBudgets()));
    }

    private void initBudgetChart() {
        try {
            List<Budget> top5 = budgetService.getTop5Budgets();
            XYChart.Series<String, Number> seriesInitial = new XYChart.Series<>();
            XYChart.Series<String, Number> seriesExpenses = new XYChart.Series<>();
            XYChart.Series<String, Number> seriesRevenue = new XYChart.Series<>();

            seriesInitial.setName("Budget initial");
            seriesExpenses.setName("Dépenses");
            seriesRevenue.setName("Revenus");

            for (Budget b : top5) {
                String eventName = budgetService.getEventTitleById(b.getEvent_id());
                seriesInitial.getData().add(new XYChart.Data<>(eventName, b.getInitial_budget()));
                seriesExpenses.getData().add(new XYChart.Data<>(eventName, b.getTotal_expenses()));
                seriesRevenue.getData().add(new XYChart.Data<>(eventName, b.getTotal_revenue()));
            }

            budgetComparisonChart.setData(FXCollections.observableArrayList(seriesInitial, seriesExpenses, seriesRevenue));
            budgetComparisonChart.setTitle("Comparaison des 5 derniers budgets");
        } catch (Exception e) {
            showError("Chart", "Erreur chargement graphique : " + e.getMessage());
        }
    }

    private void applyPredicate() {
        if (filtered == null) return;

        String selectedHealth = (healthFilter == null || healthFilter.getValue() == null) ? "Tous" : healthFilter.getValue();
        String selectedEventTitle = (eventFilter == null) ? null : eventFilter.getValue();
        Integer eventId = null;
        if (selectedEventTitle != null && !selectedEventTitle.isEmpty()) {
            try {
                eventId = budgetService.getEventIdByTitle(selectedEventTitle);
            } catch (Exception ignored) {}
        }
        String st = (statusFilter == null || statusFilter.getValue() == null) ? "Tous" : statusFilter.getValue();
        Integer finalEventId = eventId;

        filtered.setPredicate(b -> {
            boolean okHealth = matchesHealth(b, selectedHealth);
            boolean okEv = (finalEventId == null) || b.getEvent_id() == finalEventId;
            boolean okStatus;
            if ("OK".equalsIgnoreCase(st)) okStatus = b.getRentabilite() >= 0;
            else if ("Déficit".equalsIgnoreCase(st)) okStatus = b.getRentabilite() < 0;
            else okStatus = true;
            return okHealth && okEv && okStatus;
        });

        if (statusLabel != null) {
            statusLabel.setText("📊 " + filtered.size() + " budget(s) filtrés • Mise à jour: Maintenant");
        }
        renderCards();
    }

    private String getFinancialHealth(Budget b) {
        if (b == null) return "Tous";
        double initial = b.getInitial_budget();
        double rent = b.getRentabilite();
        if (rent >= initial * 0.5) return "🟢 Excellent";
        if (rent >= 0) return "🔵 Bon";
        if (rent >= -initial * 0.2) return "🟡 Fragile";
        return "🔴 Critique";
    }

    private boolean matchesHealth(Budget b, String selectedHealth) {
        if ("Tous".equalsIgnoreCase(selectedHealth)) return true;
        return getFinancialHealth(b).contains(selectedHealth);
    }

    private void renderCards() {
        if (cardsPane == null || filtered == null) return;
        cardsPane.getChildren().clear();

        for (Budget b : filtered) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(CARD_FXML));
                Parent cardRoot = loader.load();
                if (cardRoot instanceof Region r) {
                    r.setPrefWidth(440);
                    r.setMaxWidth(Double.MAX_VALUE);
                }

                BudgetCardController cell = loader.getController();
                cell.setData(
                        b,
                        () -> openDetailsAsPage(b),
                        () -> onEdit(b),
                        () -> onDeleteNoIdText(b)
                );

                cardsPane.getChildren().add(cardRoot);
            } catch (Exception e) {
                showError("UI", "Erreur card budget: " + e.getMessage());
            }
        }
    }

    private void onAdd() {
        openFormAsPage(null);
    }

    private void onEdit(Budget existing) {
        if (existing == null) return;
        openFormAsPage(existing);
    }

    private void onDeleteNoIdText(Budget b) {
        String eventTitle;
        try {
            eventTitle = budgetService.getEventTitleById(b.getEvent_id());
        } catch (Exception e) {
            eventTitle = "cet événement";
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText("Supprimer budget");
        confirm.setContentText("Voulez-vous supprimer le budget de : " + eventTitle + " ?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    budgetService.deleteBudget(b.getId());
                    setupFiltersSafe();
                    loadDataSafe();
                    applyPredicate();
                } catch (Exception ex) {
                    showError("Erreur suppression", ex.getMessage());
                }
            }
        });
    }

    private void openFormAsPage(Budget existing) {
        try {
            MainController.getInstance().loadIntoCenter(
                    FORM_FXML,
                    (BudgetFormController ctrl) -> {
                        if (existing == null) ctrl.setModeAdd();
                        else ctrl.setModeEdit(existing);
                        ctrl.setOnFormDone(() -> {
                            Budget saved = ctrl.getResult();
                            setupFiltersSafe();
                            loadDataSafe();
                            applyPredicate();
                            if (saved != null) {
                                openDetailsAsPage(saved);
                            } else {
                                MainController.getInstance().onBudget();
                            }
                        });
                    }
            );
        } catch (Exception e) {
            showError("UI", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    private void openDetailsAsPage(Budget b) {
        try {
            MainController.getInstance().loadIntoCenter(
                    DETAILS_FXML,
                    (BudgetDetailsController ctrl) -> {
                        ctrl.setBudget(b);
                        ctrl.setOnCloseAction(() -> MainController.getInstance().onBudget());
                    }
            );
        } catch (Exception e) {
            showError("Détails", "Impossible d'ouvrir les détails: " + e.getMessage());
        }
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}