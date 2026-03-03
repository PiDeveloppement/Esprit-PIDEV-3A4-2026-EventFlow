package com.example.pidev.controller.budget;

import com.example.pidev.MainController;
import com.example.pidev.model.budget.Budget;
import com.example.pidev.service.analytics.RoiCalculator;
import com.example.pidev.service.budget.BudgetService;
import com.example.pidev.service.chart.QuickChartService;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
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

    // Les StackPanes sont déclarés dans le FXML (sans ImageView dedans)
    // On crée les ImageViews programmatiquement pour un contrôle total
    @FXML private StackPane budgetChartHolder;
    @FXML private StackPane roiChartHolder;

    // ImageViews créées par le controller
    private final ImageView budgetComparisonImage = new ImageView();
    private final ImageView roiTrendImage         = new ImageView();

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

        if (healthFilter != null) healthFilter.valueProperty().addListener((o, a, n) -> applyPredicate());
        if (eventFilter  != null) eventFilter .valueProperty().addListener((o, a, n) -> applyPredicate());
        if (statusFilter != null) statusFilter.valueProperty().addListener((o, a, n) -> applyPredicate());
        if (addBtn       != null) addBtn.setOnAction(e -> onAdd());

        if (cardsPane != null) {
            cardsPane.setPadding(new Insets(8));
            cardsPane.setHgap(14);
            cardsPane.setVgap(14);
        }

        // Injecter les ImageViews dans les StackPanes et les contraindre
        setupImageView(budgetChartHolder, budgetComparisonImage);
        setupImageView(roiChartHolder,    roiTrendImage);

        setupFiltersSafe();
        loadDataSafe();
        applyPredicate();

        // Attendre que le layout JavaFX soit rendu pour avoir les vraies dimensions
        Platform.runLater(() -> {
            updateBudgetComparisonChart();
            updateRoiTrendChart();

            // Recharger si l'utilisateur redimensionne la fenêtre
            if (budgetChartHolder != null) {
                budgetChartHolder.widthProperty().addListener((obs, oldVal, newVal) -> {
                    if (Math.abs(newVal.doubleValue() - oldVal.doubleValue()) > 30) {
                        updateBudgetComparisonChart();
                        updateRoiTrendChart();
                    }
                });
            }
        });
    }

    /**
     * Configure une ImageView et l'insère dans son StackPane conteneur.
     * L'ImageView est liée en taille au StackPane → elle ne peut pas déborder.
     */
    private void setupImageView(StackPane holder, ImageView imageView) {
        if (holder == null) return;

        imageView.setPreserveRatio(false);
        imageView.setSmooth(true);
        imageView.setCache(true);

        // ✅ Lier la taille AU StackPane : l'image occupera exactement le holder
        imageView.fitWidthProperty() .bind(holder.widthProperty());
        imageView.fitHeightProperty().bind(holder.heightProperty());

        // ✅ Clip pour couper net si jamais l'image dépasse (sécurité supplémentaire)
        Rectangle clip = new Rectangle();
        clip.widthProperty() .bind(holder.widthProperty());
        clip.heightProperty().bind(holder.heightProperty());
        holder.setClip(clip);

        holder.getChildren().add(imageView);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Graphiques — taille = taille réelle du holder au moment du rendu
    // ─────────────────────────────────────────────────────────────────────────

    private void updateBudgetComparisonChart() {
        if (budgetChartHolder == null) return;
        try {
            List<Budget> top5 = budgetService.getTop5Budgets();
            if (top5.isEmpty()) return;

            int n = top5.size();
            String[] labels   = new String[n];
            double[] initial  = new double[n];
            double[] expenses = new double[n];
            double[] revenue  = new double[n];

            for (int i = 0; i < n; i++) {
                Budget b    = top5.get(i);
                labels[i]   = compactEventLabel(budgetService.getEventTitleById(b.getEvent_id()));
                initial[i]  = b.getInitial_budget();
                expenses[i] = b.getTotal_expenses();
                revenue[i]  = b.getTotal_revenue();
            }

            String[]   seriesNames = {"Budget initial", "Dépenses", "Revenus"};
            double[][] seriesData  = {initial, expenses, revenue};

            // Dimensions = taille réelle du StackPane (connue après le layout)
            int w = getHolderWidth(budgetChartHolder);
            int h = getHolderHeight(budgetChartHolder);

            JsonObject config = QuickChartService.createMultiBarChart(
                    "Comparaison des 5 derniers budgets", labels, seriesNames, seriesData);
            String url = QuickChartService.getChartUrl(config, w, h);

            loadImageAsync(url, budgetComparisonImage);

        } catch (Exception e) {
            showError("Chart", "Erreur graphique : " + e.getMessage());
        }
    }

    private void updateRoiTrendChart() {
        if (roiChartHolder == null) return;
        try {
            List<Budget> all = new ArrayList<>(baseList);
            if (all.size() < 2) return;

            List<RoiCalculator.Pair<LocalDate, Double>> dataPoints = new ArrayList<>();
            LocalDate base = LocalDate.now().minusMonths(all.size());
            for (int i = 0; i < all.size(); i++)
                dataPoints.add(new RoiCalculator.Pair<>(base.plusMonths(i), all.get(i).getRentabilite()));
            dataPoints.sort(Comparator.comparing(RoiCalculator.Pair::getFirst));

            RoiCalculator.LinearRegressionResult model = RoiCalculator.linearRegression(dataPoints);
            LocalDate minDate = dataPoints.stream().map(RoiCalculator.Pair::getFirst).min(LocalDate::compareTo).orElse(LocalDate.now());

            List<String> histL = new ArrayList<>(); List<Double> histV = new ArrayList<>();
            for (RoiCalculator.Pair<LocalDate, Double> p : dataPoints) { histL.add(compactMonthYear(p.getFirst())); histV.add(p.getSecond()); }

            List<String> projL = new ArrayList<>(); List<Double> projV = new ArrayList<>();
            LocalDate last = dataPoints.get(dataPoints.size() - 1).getFirst();
            for (int i = 1; i <= 3; i++) {
                LocalDate fd = last.plusMonths(i);
                projL.add(compactMonthYear(fd));
                projV.add(model.predict(fd.toEpochDay() - minDate.toEpochDay()));
            }

            List<String> allLabels = new ArrayList<>(histL); allLabels.addAll(projL);
            String[]   sNames = {"ROI historique", "Projection"};
            double[][] sData  = new double[2][allLabels.size()];
            for (int i = 0; i < histL.size(); i++) sData[0][i] = histV.get(i);
            for (int i = 0; i < projL.size(); i++) sData[1][histL.size() + i] = projV.get(i);

            int w = getHolderWidth(roiChartHolder);
            int h = getHolderHeight(roiChartHolder);

            JsonObject config = QuickChartService.createLineChart(
                    "Tendance du ROI avec projection sur 3 mois",
                    allLabels.toArray(new String[0]), sNames, sData);
            String url = QuickChartService.getChartUrl(config, w, h);

            loadImageAsync(url, roiTrendImage);

        } catch (Exception e) {
            showError("ROI Trend", "Erreur tendance : " + e.getMessage());
        }
    }

    /** Retourne la largeur réelle du holder, avec fallback à 500 */
    private int getHolderWidth(StackPane holder) {
        double w = holder.getWidth();
        return (int)(w > 50 ? w : 500);
    }

    /** Retourne la hauteur réelle du holder, avec fallback à 340 */
    private int getHolderHeight(StackPane holder) {
        double h = holder.getHeight();
        return (int)(h > 50 ? h : 340);
    }

    /** Charge l'image en thread background, l'affecte sur le thread JavaFX */
    private void loadImageAsync(String url, ImageView target) {
        new Thread(() -> {
            try {
                Image img = new Image(url, false);
                Platform.runLater(() -> target.setImage(img));
            } catch (Exception e) {
                System.err.println("Erreur chargement image chart : " + e.getMessage());
            }
        }, "chart-loader").start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filtres / données
    // ─────────────────────────────────────────────────────────────────────────

    private void setupFiltersSafe() {
        if (healthFilter != null) { healthFilter.getItems().setAll("Tous","🟢 Excellent","🔵 Bon","🟡 Fragile","🔴 Critique"); healthFilter.setValue("Tous"); }
        if (statusFilter != null) { statusFilter.getItems().setAll("Tous","OK","Déficit"); statusFilter.setValue("Tous"); }
        if (eventFilter  != null) { try { eventFilter.getItems().setAll(budgetService.getAllEventTitles()); } catch (Exception e) { eventFilter.getItems().clear(); } eventFilter.setValue(null); }
    }

    private void loadDataSafe() {
        try {
            baseList.setAll(budgetService.getAllBudgets());
            updateKpis();
            if (statusLabel != null) statusLabel.setText("📊 " + baseList.size() + " budget(s) • Maintenant");
            renderCards();
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("❌ " + e.getMessage());
        }
    }

    private void updateKpis() {
        if (kpiCountLabel   != null) kpiCountLabel  .setText(String.valueOf(budgetService.countBudgets()));
        if (kpiInitialLabel != null) kpiInitialLabel.setText(String.format("%,.2f DT", budgetService.sumInitial()));
        if (kpiRentLabel    != null) kpiRentLabel   .setText(String.format("%,.2f DT", budgetService.globalRentability()));
        if (kpiDeficitLabel != null) kpiDeficitLabel.setText(String.valueOf(budgetService.countDeficitBudgets()));
    }

    private void applyPredicate() {
        if (filtered == null) return;
        String h  = (healthFilter == null || healthFilter.getValue() == null) ? "Tous" : healthFilter.getValue();
        String et = (eventFilter  == null) ? null : eventFilter.getValue();
        String st = (statusFilter == null || statusFilter.getValue() == null) ? "Tous" : statusFilter.getValue();

        Integer evId = null;
        if (et != null && !et.isEmpty()) { try { evId = budgetService.getEventIdByTitle(et); } catch (Exception ignored) {} }
        Integer finalEvId = evId;

        filtered.setPredicate(b -> {
            boolean okH = matchesHealth(b, h);
            boolean okE = (finalEvId == null) || b.getEvent_id() == finalEvId;
            boolean okS = "OK".equalsIgnoreCase(st) ? b.getRentabilite() >= 0
                    : "Déficit".equalsIgnoreCase(st) ? b.getRentabilite() < 0 : true;
            return okH && okE && okS;
        });

        if (statusLabel != null) statusLabel.setText("📊 " + filtered.size() + " budget(s) filtrés");
        renderCards();
    }

    private String getFinancialHealth(Budget b) {
        if (b == null) return "Tous";
        double i = b.getInitial_budget(), r = b.getRentabilite();
        if (r >= i * 0.5)  return "🟢 Excellent";
        if (r >= 0)         return "🔵 Bon";
        if (r >= -i * 0.2) return "🟡 Fragile";
        return "🔴 Critique";
    }

    private boolean matchesHealth(Budget b, String sel) {
        return "Tous".equalsIgnoreCase(sel) || getFinancialHealth(b).contains(sel);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cartes / navigation
    // ─────────────────────────────────────────────────────────────────────────

    private void renderCards() {
        if (cardsPane == null || filtered == null) return;
        cardsPane.getChildren().clear();
        for (Budget b : filtered) {
            try {
                FXMLLoader loader   = new FXMLLoader(getClass().getResource(CARD_FXML));
                Parent     cardRoot = loader.load();
                if (cardRoot instanceof Region r) { r.setPrefWidth(440); r.setMaxWidth(Double.MAX_VALUE); }
                BudgetCardController cell = loader.getController();
                cell.setData(b, () -> openDetailsAsPage(b), () -> onEdit(b), () -> onDeleteNoIdText(b));
                cardsPane.getChildren().add(cardRoot);
            } catch (Exception e) { showError("UI", "Erreur card: " + e.getMessage()); }
        }
    }

    private void onAdd() { openFormAsPage(null); }
    private void onEdit(Budget b) { if (b != null) openFormAsPage(b); }

    private void onDeleteNoIdText(Budget b) {
        String title; try { title = budgetService.getEventTitleById(b.getEvent_id()); } catch (Exception e) { title = "cet événement"; }
        Alert c = new Alert(Alert.AlertType.CONFIRMATION);
        c.setTitle("Suppression"); c.setHeaderText("Supprimer budget"); c.setContentText("Supprimer le budget de : " + title + " ?");
        c.showAndWait().ifPresent(btn -> { if (btn == ButtonType.OK) { try { budgetService.deleteBudget(b.getId()); setupFiltersSafe(); loadDataSafe(); applyPredicate(); } catch (Exception ex) { showError("Erreur", ex.getMessage()); } } });
    }

    private void openFormAsPage(Budget existing) {
        try {
            MainController.getInstance().loadIntoCenter(FORM_FXML, (BudgetFormController ctrl) -> {
                if (existing == null) ctrl.setModeAdd(); else ctrl.setModeEdit(existing);
                ctrl.setOnFormDone(() -> { Budget saved = ctrl.getResult(); setupFiltersSafe(); loadDataSafe(); applyPredicate(); if (saved != null) openDetailsAsPage(saved); else MainController.getInstance().onBudget(); });
            });
        } catch (Exception e) { showError("UI", "Impossible d'ouvrir le formulaire: " + e.getMessage()); }
    }

    private void openDetailsAsPage(Budget b) {
        try {
            MainController.getInstance().loadIntoCenter(DETAILS_FXML, (BudgetDetailsController ctrl) -> { ctrl.setBudget(b); ctrl.setOnCloseAction(() -> MainController.getInstance().onBudget()); });
        } catch (Exception e) { showError("Détails", "Impossible d'ouvrir les détails: " + e.getMessage()); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String compactEventLabel(String text) {
        if (text == null || text.isBlank()) return "";
        String c = text.trim();
        if (c.length() <= 8) return c;
        String[] w = c.split("\\s+");
        if (w.length >= 2) return (w[0].length() > 4 ? w[0].substring(0,4) : w[0]) + "." + (w[1].length() > 3 ? w[1].substring(0,3) : w[1]);
        return c.substring(0,7) + "…";
    }

    private String compactMonthYear(LocalDate d) {
        return d == null ? "" : String.format("%02d/%02d", d.getMonthValue(), d.getYear() % 100);
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}