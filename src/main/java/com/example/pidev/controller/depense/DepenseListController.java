package com.example.pidev.controller.depense;

import com.example.pidev.MainController;
import com.example.pidev.model.depense.Depense;
import com.example.pidev.service.depense.DepenseService;
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

import java.net.URL;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.ResourceBundle;

public class DepenseListController implements Initializable {

    // Identifiants mis à jour pour correspondre au FXML moderne
    @FXML private Label totalDepensesLabel;
    @FXML private Label depensesCountLabel;
    @FXML private Label monthDepensesLabel; // Nouveau Label dans le FXML
    @FXML private Label topCategoryLabel;   // Remplace categoriesLabel

    @FXML private TextField searchField;    // Ajouté pour la recherche
    @FXML private ComboBox<String> categoryFilter; // Remplace filtreCategorie
    @FXML private ComboBox<String> budgetFilter;   // Remplace filtrePeriode (S'aligne au FXML)
    @FXML private Button addDepenseBtn;            // Match FXML

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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filtered = new FilteredList<>(baseList, d -> true);
        filtered.addListener((ListChangeListener<Depense>) c -> renderCards());

        // Listeners mis à jour avec les nouveaux noms de ComboBox
        if (categoryFilter != null) categoryFilter.valueProperty().addListener((obs, o, n) -> applyPredicate());
        if (budgetFilter != null) budgetFilter.valueProperty().addListener((obs, o, n) -> applyPredicate());
        if (searchField != null) searchField.textProperty().addListener((obs, o, n) -> applyPredicate());

        if (addDepenseBtn != null) addDepenseBtn.setOnAction(e -> onAdd());

        if (cardsPane != null) {
            cardsPane.setPadding(new Insets(8));
            cardsPane.setHgap(15);
            cardsPane.setVgap(15);
        }

        setupFilters();
        loadData();
        applyPredicate();
    }

    private void setupFilters() {
        if (budgetFilter != null) {
            // Ici, on garde ta logique de période dans le combo budgetFilter du FXML
            budgetFilter.getItems().setAll("Toutes", "Ce mois", "Ce trimestre", "Cette année");
            budgetFilter.setValue("Toutes");
        }

        if (categoryFilter != null) {
            categoryFilter.getItems().clear();
            categoryFilter.getItems().add("Toutes");
            try {
                categoryFilter.getItems().addAll(depenseService.getCategories());
            } catch (Exception ignored) {}
            categoryFilter.setValue("Toutes");
        }
    }

    private void loadData() {
        try {
            baseList.setAll(depenseService.getAllDepenses());
            updateKpis();
            if (categoryPieChart != null) initCategoryChart();

            if (statusLabel != null) statusLabel.setText("📊 " + baseList.size() + " dépense(s) chargée(s)");
            renderCards();
        } catch (Exception e) {
            if (statusLabel != null) statusLabel.setText("❌ Erreur: " + e.getMessage());
        }
    }

    private void updateKpis() {
        int count = depenseService.countDepenses();
        double total = depenseService.sumDepenses();

        YearMonth now = YearMonth.now();
        LocalDate from = now.atDay(1);
        LocalDate to = now.atEndOfMonth();
        double monthTotal = depenseService.sumDepensesBetween(from, to);

        if (depensesCountLabel != null) depensesCountLabel.setText(String.valueOf(count));
        if (totalDepensesLabel != null) totalDepensesLabel.setText(String.format("%,.2f DT", total));
        if (monthDepensesLabel != null) monthDepensesLabel.setText(String.format("%,.2f DT", monthTotal));

        // On peut afficher la catégorie la plus utilisée ici
        if (topCategoryLabel != null) {
            try {
                Map<String, Double> stats = depenseService.getSumByCategory();
                String top = stats.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("—");
                topCategoryLabel.setText(top);
            } catch (Exception e) { topCategoryLabel.setText("—"); }
        }
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
            categoryPieChart.setTitle("Répartition des dépenses");
        } catch (Exception e) {
            System.err.println("Erreur graphique: " + e.getMessage());
        }
    }

    private void applyPredicate() {
        if (filtered == null) return;

        String cat = (categoryFilter == null || categoryFilter.getValue() == null) ? "Toutes" : categoryFilter.getValue();
        String periode = (budgetFilter == null || budgetFilter.getValue() == null) ? "Toutes" : budgetFilter.getValue();
        String search = (searchField == null) ? "" : searchField.getText().toLowerCase().trim();

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
                minDate = LocalDate.of(today.getYear(), startMonth, 1);
                maxDate = minDate.plusMonths(3).minusDays(1);
            }
            case "Cette année" -> {
                minDate = LocalDate.of(today.getYear(), 1, 1);
                maxDate = LocalDate.of(today.getYear(), 12, 31);
            }
        }

        LocalDate finalMin = minDate;
        LocalDate finalMax = maxDate;

        filtered.setPredicate(d -> {
            boolean okCat = "Toutes".equalsIgnoreCase(cat) || (d.getCategory() != null && d.getCategory().equalsIgnoreCase(cat));

            boolean okPeriode = true;
            if (finalMin != null && finalMax != null) {
                if (d.getExpense_date() == null) okPeriode = false;
                else okPeriode = !d.getExpense_date().isBefore(finalMin) && !d.getExpense_date().isAfter(finalMax);
            }

            boolean okSearch = search.isEmpty()
                    || (d.getDescription() != null && d.getDescription().toLowerCase().contains(search))
                    || (d.getCategory() != null && d.getCategory().toLowerCase().contains(search));

            return okCat && okPeriode && okSearch;
        });

        if (statusLabel != null) statusLabel.setText("📊 " + filtered.size() + " dépense(s) trouvée(s)");
        renderCards();
    }

    private void renderCards() {
        if (cardsPane == null || filtered == null) return;
        cardsPane.getChildren().clear();

        for (Depense d : filtered) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(CARD_FXML));
                Parent cardRoot = loader.load();

                // FIX: Largeur adaptée pour tenir dans 1200px avec la sidebar
                if (cardRoot instanceof Region r) {
                    r.setPrefWidth(325);
                    r.setMaxWidth(340);
                }

                DepenseCardController cell = loader.getController();
                cell.setData(d, () -> openDetailsAsPage(d), () -> onEdit(d), () -> onDeleteNoIdText(d));

                cardsPane.getChildren().add(cardRoot);
            } catch (Exception ex) {
                System.err.println("Erreur card: " + ex.getMessage());
            }
        }
    }

    // --- Méthodes de navigation et actions (Inchangées) ---

    private void onAdd() { openFormAsPage(null); }

    private void onEdit(Depense existing) { if (existing != null) openFormAsPage(existing); }

    private void onDeleteNoIdText(Depense d) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous supprimer cette dépense ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                try {
                    depenseService.deleteDepense(d.getId(), d.getBudget_id());
                    loadData();
                } catch (Exception ex) { showError("Erreur", ex.getMessage()); }
            }
        });
    }

    private void openFormAsPage(Depense existing) {
        try {
            MainController.getInstance().loadIntoCenter(FORM_FXML, (DepenseFormController ctrl) -> {
                ctrl.setDepense(existing);
                ctrl.setOnFormDone(() -> {
                    loadData();
                    if (ctrl.isSaved() && ctrl.getDepense() != null) openDetailsAsPage(ctrl.getDepense());
                    else backToList();
                });
            });
        } catch (Exception e) { showError("UI", e.getMessage()); }
    }

    private void openDetailsAsPage(Depense d) {
        try {
            MainController.getInstance().loadIntoCenter(DETAILS_FXML, (DepenseDetailsController ctrl) -> {
                ctrl.setDepense(d);
                ctrl.setOnCloseAction(this::backToList);
            });
        } catch (Exception e) { showError("Détails", e.getMessage()); }
    }

    private void backToList() {
        try {
            MainController.getInstance().loadIntoCenter(LIST_FXML, (DepenseListController ctrl) -> {});
        } catch (Exception e) { loadData(); }
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.showAndWait();
    }
}