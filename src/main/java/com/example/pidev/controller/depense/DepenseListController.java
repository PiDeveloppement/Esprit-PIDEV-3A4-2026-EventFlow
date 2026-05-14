package com.example.pidev.controller.depense;

import com.example.pidev.MainController;
import com.example.pidev.model.depense.Depense;
import com.example.pidev.service.depense.DepenseService;
import com.example.pidev.service.excel.ExcelExportService;
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
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DepenseListController implements Initializable {

    @FXML private Label totalDepensesLabel;
    @FXML private Label countDepensesLabel;
    @FXML private Label avgDepenseLabel;
    @FXML private Label categoriesLabel;
    @FXML private Label anomaliesLabel;
    @FXML private ComboBox<String> filtreCategorie;
    @FXML private ComboBox<String> filtrePeriode;
    @FXML private ComboBox<String> filtreEtatFinancier;
    @FXML private Button addBtn;
    @FXML private Button exportExcelBtn;
    @FXML private Label statusLabel;
    @FXML private TilePane cardsPane;
    @FXML private PieChart categoryPieChart;

    private final DepenseService depenseService = new DepenseService();
    private final ObservableList<Depense> baseList = FXCollections.observableArrayList();
    private FilteredList<Depense> filtered;

    private static final String LIST_FXML    = "/com/example/pidev/fxml/Depense/depense-modern.fxml";
    private static final String CARD_FXML    = "/com/example/pidev/fxml/Depense/depense-card.fxml";
    private static final String FORM_FXML    = "/com/example/pidev/fxml/Depense/depense-form.fxml";
    private static final String DETAILS_FXML = "/com/example/pidev/fxml/Depense/depense-detail.fxml";

    // Auto-refresh toutes les 5 secondes
    private ScheduledExecutorService scheduler;
    private static final int REFRESH_INTERVAL_SECONDS = 5;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        filtered = new FilteredList<>(baseList, d -> true);
        filtered.addListener((ListChangeListener<Depense>) c -> renderCards());

        if (filtreCategorie != null) filtreCategorie.valueProperty().addListener((obs, o, n) -> applyPredicate());
        if (filtrePeriode != null) filtrePeriode.valueProperty().addListener((obs, o, n) -> applyPredicate());
        if (filtreEtatFinancier != null) filtreEtatFinancier.valueProperty().addListener((obs, o, n) -> applyPredicate());

        if (addBtn != null) addBtn.setOnAction(e -> onAdd());
        if (exportExcelBtn != null) exportExcelBtn.setOnAction(e -> exportDepensesToExcel());

        if (cardsPane != null) {
            cardsPane.setPadding(new Insets(8));
            cardsPane.setHgap(14);
            cardsPane.setVgap(14);
        }

        setupFilters();
        loadData();
        applyPredicate();

        // Démarrer le rafraîchissement automatique
        startAutoRefresh();
    }

    /**
     * Démarre le rafraîchissement automatique toutes les 5 secondes
     */
    private void startAutoRefresh() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(
                    this::refreshDataFromDatabase,
                    REFRESH_INTERVAL_SECONDS,
                    REFRESH_INTERVAL_SECONDS,
                    TimeUnit.SECONDS
            );
            System.out.println("✅ Auto-refresh Dépenses démarré (tous les " + REFRESH_INTERVAL_SECONDS + "s)");
        }
    }

    /**
     * Arrête le rafraîchissement automatique
     */
    private void stopAutoRefresh() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("⏹️ Auto-refresh Dépenses arrêté");
        }
    }

    /**
     * Rafraîchit les données depuis la base de données
     */
    private void refreshDataFromDatabase() {
        try {
            List<Depense> newData = depenseService.getAllDepenses();
            Platform.runLater(() -> {
                baseList.setAll(newData);
                updateKpis();
                initCategoryChart();
                applyPredicate();
                if (statusLabel != null) {
                    statusLabel.setText("📊 " + baseList.size() + " dépense(s) ✅ Mis à jour");
                }
            });
        } catch (Exception e) {
            System.err.println("⚠️ Erreur auto-refresh Dépenses: " + e.getMessage());
        }
    }

    private void setupFilters() {
        if (filtrePeriode != null) {
            filtrePeriode.getItems().setAll("Toutes", "Ce mois", "Ce trimestre", "Cette année");
            filtrePeriode.setValue("Toutes");
        }

        if (filtreCategorie != null) {
            filtreCategorie.getItems().clear();
            filtreCategorie.getItems().add("Toutes");
            try {
                filtreCategorie.getItems().addAll(depenseService.getCategories());
            } catch (Exception ignored) {}
            filtreCategorie.setValue("Toutes");
        }

        if (filtreEtatFinancier != null) {
            filtreEtatFinancier.getItems().setAll(
                    "Tous",
                    "🟢 Faible",
                    "🟡 Moyen",
                    "🔴 Élevé"
            );
            filtreEtatFinancier.setValue("Tous");
        }
    }

    private void loadData() {
        try {
            baseList.setAll(depenseService.getAllDepenses());

            // Pas de détection d'anomalies
            updateKpis();
            initCategoryChart();

            if (statusLabel != null) statusLabel.setText("📊 " + baseList.size() + " dépense(s) • Mise à jour: Maintenant");
            renderCards();
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("❌ Erreur chargement dépenses: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateKpis() {
        int count = depenseService.countDepenses();
        double total = depenseService.sumDepenses();

        int cats = 0;
        try { cats = depenseService.getCategories().size(); } catch (Exception ignored) {}

        // anomalies désactivées
        long anomalies = 0;

        if (countDepensesLabel != null) countDepensesLabel.setText(String.valueOf(count));
        if (totalDepensesLabel != null) totalDepensesLabel.setText(String.format("%,.2f DT", total));
        if (avgDepenseLabel != null) {
            double avg = count == 0 ? 0 : total / count;
            avgDepenseLabel.setText(String.format("%,.2f DT", avg));
        }
        if (categoriesLabel != null) categoriesLabel.setText(String.valueOf(cats));
        if (anomaliesLabel != null) anomaliesLabel.setText(String.valueOf(anomalies));
    }

    private void initCategoryChart() {
        try {
            Map<String, Double> data = depenseService.getSumByCategory();
            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                if (entry.getValue() > 0) {
                    pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
                }
            }
            categoryPieChart.setData(pieData);
            categoryPieChart.setTitle("Répartition des dépenses par catégorie");
            categoryPieChart.setLabelsVisible(true);
            categoryPieChart.setLegendVisible(true);
        } catch (Exception e) {
            showError("Chart", "Erreur chargement graphique : " + e.getMessage());
        }
    }

    private String getEtatFinancier(Depense d) {
        if (d == null) return "Tous";
        double a = d.getAmount();
        if (a < 100) return "🟢 Faible";
        if (a <= 1000) return "🟡 Moyen";
        return "🔴 Élevé";
    }

    private void applyPredicate() {
        if (filtered == null) return;

        String cat = (filtreCategorie == null || filtreCategorie.getValue() == null) ? "Toutes" : filtreCategorie.getValue();
        String periode = (filtrePeriode == null || filtrePeriode.getValue() == null) ? "Toutes" : filtrePeriode.getValue();
        String etat = (filtreEtatFinancier == null || filtreEtatFinancier.getValue() == null) ? "Tous" : filtreEtatFinancier.getValue();

        LocalDate minDate = null;
        LocalDate maxDate = null;
        LocalDate today = LocalDate.now();

        switch (periode) {
            case "Ce mois" -> {
                YearMonth ym = YearMonth.now();
                minDate = ym.atDay(1);
                maxDate = ym.atEndOfMonth();
            }
            case "Ce trimestre" -> {
                int qtr = (today.getMonthValue() - 1) / 3;
                int startMonth = qtr * 3 + 1;
                YearMonth start = YearMonth.of(today.getYear(), startMonth);
                YearMonth end = YearMonth.of(today.getYear(), startMonth + 2);
                minDate = start.atDay(1);
                maxDate = end.atEndOfMonth();
            }
            case "Cette année" -> {
                minDate = LocalDate.of(today.getYear(), 1, 1);
                maxDate = LocalDate.of(today.getYear(), 12, 31);
            }
            default -> { /* Toutes */ }
        }

        LocalDate finalMin = minDate;
        LocalDate finalMax = maxDate;

        filtered.setPredicate(d -> {
            boolean okCat = "Toutes".equalsIgnoreCase(cat)
                    || (d.getCategory() != null && d.getCategory().equalsIgnoreCase(cat));

            boolean okPeriode = true;
            if (finalMin != null && finalMax != null) {
                if (d.getExpense_date() == null) okPeriode = false;
                else okPeriode = !d.getExpense_date().isBefore(finalMin) && !d.getExpense_date().isAfter(finalMax);
            }

            boolean okEtat = "Tous".equalsIgnoreCase(etat) || getEtatFinancier(d).equals(etat);

            return okCat && okPeriode && okEtat;
        });

        if (statusLabel != null) statusLabel.setText("📊 " + filtered.size() + " dépense(s) filtrées • Mise à jour: Maintenant");
        renderCards();
    }

    private void renderCards() {
        if (cardsPane == null || filtered == null) return;
        cardsPane.getChildren().clear();

        for (Depense d : filtered) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(CARD_FXML));
                Parent cardRoot = loader.load();

                if (cardRoot instanceof Region r) {
                    r.setPrefWidth(440);
                    r.setMaxWidth(Double.MAX_VALUE);
                }

                DepenseCardController cell = loader.getController();
                cell.setData(
                        d,
                        () -> openDetailsAsPage(d),
                        () -> onEdit(d),
                        () -> onDeleteNoIdText(d)
                );

                cardsPane.getChildren().add(cardRoot);

            } catch (Exception ex) {
                showError("UI", "Erreur card dépense: " + ex.getMessage());
            }
        }
    }

    private void onAdd() {
        openFormAsPage(null);
    }

    private void onEdit(Depense existing) {
        if (existing == null) return;
        openFormAsPage(existing);
    }

    private void onDeleteNoIdText(Depense d) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText("Supprimer dépense");
        confirm.setContentText("Voulez-vous supprimer cette dépense ?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    depenseService.deleteDepense(d.getId(), d.getBudget_id());
                    loadData();
                    setupFilters();
                } catch (Exception ex) {
                    showError("Erreur suppression", ex.getMessage());
                }
            }
        });
    }

    private void exportDepensesToExcel() {
        try {
            List<Depense> allDepenses = depenseService.getAllDepenses();
            Map<String, Double> data = depenseService.getSumByCategory();
            JsonObject chartConfig = QuickChartService.createDoughnutChart(
                    "Répartition des dépenses",
                    data.keySet().toArray(new String[0]),
                    data.values().stream().mapToDouble(Double::doubleValue).toArray()
            );

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le fichier Excel");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx"));
            File file = fileChooser.showSaveDialog(getStage());
            if (file != null) {
                ExcelExportService.exportDepenses(allDepenses, chartConfig, file.getAbsolutePath());
                showInfo("Export réussi", "Le fichier Excel a été généré avec succès.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Export", "Erreur lors de l'export : " + e.getMessage());
        }
    }

    private Stage getStage() {
        return (Stage) addBtn.getScene().getWindow();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void openFormAsPage(Depense existing) {
        try {
            MainController.getInstance().loadIntoCenter(
                    FORM_FXML,
                    (DepenseFormController ctrl) -> {
                        ctrl.setDepense(existing);
                        ctrl.setOnFormDone(() -> {
                            setupFilters();
                            loadData();
                            if (ctrl.isSaved() && ctrl.getDepense() != null) {
                                openDetailsAsPage(ctrl.getDepense());
                            } else {
                                backToList();
                            }
                        });
                    }
            );
        } catch (Exception e) {
            showError("UI", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    private void openDetailsAsPage(Depense d) {
        try {
            MainController.getInstance().loadIntoCenter(
                    DETAILS_FXML,
                    (DepenseDetailsController ctrl) -> {
                        ctrl.setDepense(d);
                        ctrl.setOnCloseAction(this::backToList);
                    }
            );
        } catch (Exception e) {
            showError("Détails", "Impossible d'ouvrir les détails: " + e.getMessage());
        }
    }

    private void backToList() {
        try {
            MainController.getInstance().loadIntoCenter(LIST_FXML, (DepenseListController ctrl) -> {});
        } catch (Exception e) {
            setupFilters();
            loadData();
        }
    }
}