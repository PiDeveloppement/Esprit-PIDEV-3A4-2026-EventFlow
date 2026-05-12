package com.example.pidev.controller.sponsor;

import com.example.pidev.MainController;
import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.service.sponsor.SponsorService;
import com.example.pidev.service.pdf.LocalSponsorPdfService;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.ResourceBundle;

public class SponsorPortalController implements Initializable {

    @FXML private Label todayLabel;
    @FXML private ComboBox<String> emailAccount;
    @FXML private Label mySponsorsLabel;
    @FXML private Label myContributionLabel;
    @FXML private Label myEventsLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> companyFilter;
    @FXML private ComboBox<String> eventFilter;
    @FXML private Button addSponsorBtn;
    @FXML private Label statusLabel;
    @FXML private TilePane cardsPane;
    @FXML private BarChart<String, Number> myContributionsChart;
    @FXML private CategoryAxis xAxis;
    @FXML private NumberAxis yAxis;

    private final SponsorService sponsorService = new SponsorService();
    private final LocalSponsorPdfService pdfService = new LocalSponsorPdfService();

    private final ObservableList<Sponsor> baseList = FXCollections.observableArrayList();
    private FilteredList<Sponsor> filtered;

    private String currentEmail;

    private static final String CARD_FXML    = "/com/example/pidev/fxml/Sponsor/sponsor-card.fxml";
    private static final String FORM_FXML    = "/com/example/pidev/fxml/Sponsor/sponsor-form.fxml";
    private static final String DETAILS_FXML = "/com/example/pidev/fxml/Sponsor/sponsor-detail.fxml";

    public void setInitialEmail(String email) {
        if (email == null || email.isBlank()) return;
        currentEmail = email;
        if (emailAccount != null) emailAccount.setValue(email);
        setPortalEnabled(true);
        reloadMine();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (todayLabel != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
            todayLabel.setText(LocalDate.now().format(fmt));
        }

        filtered = new FilteredList<>(baseList, s -> true);
        filtered.addListener((ListChangeListener<Sponsor>) c -> renderCards());

        if (searchField != null)  searchField.textProperty().addListener((obs,o,n)-> applyPredicate());
        if (companyFilter != null) companyFilter.valueProperty().addListener((obs, o, n) -> applyPredicate());
        if (eventFilter != null)   eventFilter.valueProperty().addListener((obs, o, n) -> applyPredicate());
        if (addSponsorBtn != null) addSponsorBtn.setOnAction(e -> onAdd());

        if (cardsPane != null) {
            cardsPane.setPadding(new Insets(8));
            cardsPane.setHgap(14);
            cardsPane.setVgap(14);
            cardsPane.setPrefTileWidth(440);
            cardsPane.setTileAlignment(Pos.TOP_LEFT);
        }

        // Charger les emails disponibles depuis la table sponsor
        try {
            if (emailAccount != null) {
                emailAccount.getItems().setAll(sponsorService.getDemoEmailsFromSponsor());
                emailAccount.valueProperty().addListener((obs, oldV, email) -> onAccountSelected(email));
            }
        } catch (Exception e) {
            showError("DB", "Impossible de charger emails (demo): " + e.getMessage());
        }

        setPortalEnabled(false);
        clearPortal();
        renderCards();

        String remembered = MainController.getInstance().getLastSponsorPortalEmail();
        if (remembered != null && !remembered.isBlank() && emailAccount != null) {
            emailAccount.setValue(remembered);
            onAccountSelected(remembered);
        }
    }

    private void onAccountSelected(String email) {
        currentEmail = email;
        MainController.getInstance().setLastSponsorPortalEmail(currentEmail);
        if (currentEmail == null || currentEmail.isBlank()) {
            setPortalEnabled(false);
            clearPortal();
            return;
        }
        setPortalEnabled(true);
        reloadMine();
    }

    private void setPortalEnabled(boolean enabled) {
        if (addSponsorBtn != null) addSponsorBtn.setDisable(!enabled);
        if (searchField != null)   searchField.setDisable(!enabled);
        if (companyFilter != null) companyFilter.setDisable(!enabled);
        if (eventFilter != null)   eventFilter.setDisable(!enabled);
        if (cardsPane != null)     cardsPane.setDisable(!enabled);
    }

    private void clearPortal() {
        baseList.clear();
        if (mySponsorsLabel != null) mySponsorsLabel.setText("0");
        if (myContributionLabel != null) myContributionLabel.setText("0,00 DT");
        if (myEventsLabel != null) myEventsLabel.setText("0");
        if (statusLabel != null) statusLabel.setText("Sélectionnez un compte pour afficher vos sponsors.");
        if (searchField != null) searchField.clear();
        if (companyFilter != null) companyFilter.getItems().clear();
        if (eventFilter != null) eventFilter.getItems().clear();
        myContributionsChart.setData(FXCollections.observableArrayList());
    }

    private void reloadMine() {
        try {
            baseList.setAll(sponsorService.getSponsorsByContactEmail(currentEmail));
            updateMyKpis();
            refreshFilterCombos();
            applyPredicate();
            if (statusLabel != null) statusLabel.setText("📊 " + filtered.size() + " sponsor(s)");
            initMyChart();
        } catch (Exception e) {
            showError("DB", e.getMessage());
        }
    }

    private void updateMyKpis() {
        try {
            int count = sponsorService.getMySponsorsCountDemo(currentEmail);
            double sum = sponsorService.getMyTotalContributionDemo(currentEmail);
            int evCount = sponsorService.getMySponsoredEventsCountDemo(currentEmail);

            if (mySponsorsLabel != null) mySponsorsLabel.setText(String.valueOf(count));
            if (myContributionLabel != null) myContributionLabel.setText(String.format("%,.2f DT", sum));
            if (myEventsLabel != null) myEventsLabel.setText(String.valueOf(evCount));
        } catch (Exception e) {
            showError("KPI", e.getMessage());
        }
    }

    private void refreshFilterCombos() {
        try {
            if (companyFilter != null) {
                companyFilter.getItems().setAll(sponsorService.getCompanyNamesByContactEmail(currentEmail));
                companyFilter.setValue(null);
            }
            if (eventFilter != null) {
                ObservableList<Integer> eventIds = sponsorService.getEventIdsByContactEmail(currentEmail);
                ObservableList<String> titles = FXCollections.observableArrayList();
                for (Integer id : eventIds) {
                    try {
                        String title = sponsorService.getEventTitleById(id);
                        if (title != null && !title.isBlank()) titles.add(title);
                    } catch (Exception ignored) {}
                }
                eventFilter.getItems().setAll(titles);
                eventFilter.setValue(null);
            }
        } catch (Exception e) {
            showError("Filtres", e.getMessage());
        }
    }

    private void applyPredicate() {
        if (filtered == null) return;

        String q = (searchField == null || searchField.getText() == null) ? "" : searchField.getText().trim().toLowerCase();
        String comp = (companyFilter == null) ? null : companyFilter.getValue();
        String eventTitle = (eventFilter == null) ? null : eventFilter.getValue();

        Integer eventId = null;
        if (eventTitle != null && !eventTitle.isBlank()) {
            try {
                eventId = sponsorService.getEventIdByTitle(eventTitle);
            } catch (Exception ignored) {}
        }
        Integer finalEventId = eventId;

        filtered.setPredicate(s -> {
            boolean okQ = q.isEmpty()
                    || String.valueOf(s.getId()).contains(q)
                    || (s.getCompany_name() != null && s.getCompany_name().toLowerCase().contains(q))
                    || (s.getContact_email() != null && s.getContact_email().toLowerCase().contains(q));

            boolean okComp = (comp == null) || (s.getCompany_name() != null && s.getCompany_name().equalsIgnoreCase(comp));
            boolean okEv = (finalEventId == null) || s.getEvent_id() == finalEventId;

            return okQ && okComp && okEv;
        });

        if (statusLabel != null) statusLabel.setText("📊 " + filtered.size() + " sponsor(s)");
        renderCards();
    }

    private void renderCards() {
        if (cardsPane == null || filtered == null) return;
        cardsPane.getChildren().clear();

        for (Sponsor s : filtered) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(CARD_FXML));
                Parent root = loader.load();
                if (root instanceof Region r) {
                    r.setPrefWidth(440);
                    r.setMaxWidth(Double.MAX_VALUE);
                }

                SponsorCardController card = loader.getController();
                card.setData(
                        s,
                        () -> openDetailsAsPage(s),
                        () -> onGeneratePdfFromDetails(s),
                        () -> onEdit(s),
                        () -> onDelete(s)
                );

                cardsPane.getChildren().add(root);
            } catch (Exception ignored) {}
        }
    }

    private void initMyChart() {
        try {
            Map<String, Double> data = sponsorService.getMyContributionsByCompany(currentEmail);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Contributions");
            for (Map.Entry<String, Double> entry : data.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }
            myContributionsChart.setData(FXCollections.observableArrayList(series));
            myContributionsChart.setTitle("Mes contributions par sponsor");
        } catch (Exception e) {
            showError("Chart", "Erreur chargement graphique : " + e.getMessage());
        }
    }

    private void onAdd() {
        if (currentEmail == null || currentEmail.isBlank()) {
            showError("Accès", "Choisissez un email (mode DEMO).");
            return;
        }
        openFormAsPage(null, currentEmail);
    }

    private void onEdit(Sponsor existing) {
        if (currentEmail == null || currentEmail.isBlank()) {
            showError("Accès", "Choisissez un email (mode DEMO).");
            return;
        }
        openFormAsPage(existing, currentEmail);
    }

    private void onDelete(Sponsor s) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText("Supprimer sponsor");
        confirm.setContentText("Supprimer: " + s.getCompany_name() + " ?");

        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    sponsorService.deleteSponsor(s.getId());
                    reloadMine();
                } catch (Exception ex) {
                    showError("Erreur suppression", ex.getMessage());
                }
            }
        });
    }

    private void onGeneratePdfFromDetails(Sponsor s) {
        try {
            if (s == null) return;
            String eventTitle = sponsorService.getEventTitleById(s.getEvent_id());
            File pdf = pdfService.generateSponsorContractPdf(s, eventTitle);
            if (!Desktop.isDesktopSupported()) {
                showError("PDF", "Desktop non supporté sur cette machine.");
                return;
            }
            Desktop.getDesktop().open(pdf);
        } catch (Exception ex) {
            showError("PDF", ex.getMessage());
        }
    }

    private void openFormAsPage(Sponsor existing, String fixedEmail) {
        MainController.getInstance().loadIntoCenter(
                FORM_FXML,
                (SponsorFormController ctrl) -> {
                    ctrl.setFixedEmail(fixedEmail);
                    if (existing == null) ctrl.setModeAdd();
                    else ctrl.setModeEdit(existing);
                    ctrl.setOnSaved(saved -> {
                        reloadMine();
                        openDetailsAsPage(saved);
                    });
                    ctrl.setOnFormDone(() -> {
                        reloadMine();
                        MainController.getInstance().showSponsorPortal(currentEmail);
                    });
                }
        );
    }

    private void openDetailsAsPage(Sponsor sponsor) {
        try {
            MainController.getInstance().loadIntoCenter(
                    DETAILS_FXML,
                    (SponsorDetailsController ctrl) -> {
                        ctrl.setSponsor(sponsor);
                        ctrl.setOnBack(() -> MainController.getInstance().showSponsorPortal(currentEmail));
                    }
            );
        } catch (Exception e) {
            showError("UI", "Impossible d'ouvrir détails: " + e.getMessage());
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