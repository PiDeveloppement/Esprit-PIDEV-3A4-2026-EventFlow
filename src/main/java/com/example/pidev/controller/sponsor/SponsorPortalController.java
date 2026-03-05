package com.example.pidev.controller.sponsor;

import com.example.pidev.HelloApplication;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.event.EventCategory;
import com.example.pidev.model.sponsor.Sponsor;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.service.chart.QuickChartService;
import com.example.pidev.service.event.EventCategoryService;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.excel.ExcelExportService;
import com.example.pidev.service.pdf.LocalSponsorPdfService;
import com.example.pidev.service.sponsor.SponsorMatchingService;
import com.example.pidev.service.sponsor.SponsorService;
import com.example.pidev.utils.UserSession;
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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.awt.Desktop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class SponsorPortalController implements Initializable {

    @FXML private BorderPane rootPane;
    @FXML private Label todayLabel;
    @FXML private ComboBox<String> emailAccount;
    @FXML private Label mySponsorsLabel;
    @FXML private Label myContributionLabel;
    @FXML private Label myEventsLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> companyFilter;
    @FXML private ComboBox<String> eventFilter;
    @FXML private Button addSponsorBtn;
    @FXML private Button exportExcelBtn;
    @FXML private Button exportCsvBtn;
    @FXML private Button historyBtn;
    @FXML private Button homeBtn;
    @FXML private Label statusLabel;
    @FXML private TilePane cardsPane;

    @FXML private FlowPane recommendedEventsGrid;
    @FXML private VBox noRecommendedMessage;
    @FXML private FlowPane allEventsGrid;
    @FXML private VBox noEventsMessage;

    @FXML private Label mySponsorsSectionLabel;
    @FXML private Label myContributionSectionLabel;
    @FXML private Label myEventsSectionLabel;

    private final SponsorService sponsorService = new SponsorService();
    private final LocalSponsorPdfService pdfService = new LocalSponsorPdfService();
    private final SponsorMatchingService matchingService = new SponsorMatchingService();
    private final EventService eventService = new EventService();
    private final EventCategoryService eventCategoryService = new EventCategoryService();

    private final ObservableList<Sponsor> sponsorBaseList = FXCollections.observableArrayList();
    private FilteredList<Sponsor> sponsorFiltered;

    private List<Event> allEvents = new ArrayList<>();
    private List<Event> recommendedEvents = new ArrayList<>();
    private final Map<Integer, String> categoryNames = new HashMap<>();
    private final Map<Integer, String> categoryColors = new HashMap<>();

    private String currentEmail;
    private Node mainContent;

    private static final String CARD_FXML = "/com/example/pidev/fxml/Sponsor/sponsor-card.fxml";
    private static final String FORM_FXML = "/com/example/pidev/fxml/Sponsor/sponsor-form.fxml";
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
        if (rootPane != null) mainContent = rootPane.getCenter();

        if (todayLabel != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
            todayLabel.setText(LocalDate.now().format(fmt));
        }

        sponsorFiltered = new FilteredList<>(sponsorBaseList, s -> true);
        sponsorFiltered.addListener((ListChangeListener<Sponsor>) c -> renderSponsorCards());

        if (searchField != null) searchField.textProperty().addListener((obs, o, n) -> applySponsorFilters());
        if (companyFilter != null) companyFilter.valueProperty().addListener((obs, o, n) -> applySponsorFilters());
        if (eventFilter != null) eventFilter.valueProperty().addListener((obs, o, n) -> applySponsorFilters());

        if (addSponsorBtn != null) addSponsorBtn.setOnAction(e -> onAdd());
        if (exportExcelBtn != null) exportExcelBtn.setOnAction(e -> handleExportExcel());
        if (exportCsvBtn != null) exportCsvBtn.setOnAction(e -> handleExportCsv());
        if (historyBtn != null) historyBtn.setOnAction(e -> openHistoryPage());
        if (homeBtn != null) {
            homeBtn.setVisible(false);
            homeBtn.setManaged(false);
            homeBtn.setOnAction(e -> {
                if (rootPane != null && mainContent != null && rootPane.getCenter() != mainContent) {
                    restoreMainContent();
                    return;
                }
                HelloApplication.loadPublicEventsPage();
            });
        }

        if (cardsPane != null) {
            cardsPane.setPadding(new Insets(8));
            cardsPane.setHgap(14);
            cardsPane.setVgap(14);
            cardsPane.setPrefTileWidth(440);
            cardsPane.setTileAlignment(Pos.TOP_LEFT);
        }

        if (emailAccount != null) {
            emailAccount.setVisible(false);
            emailAccount.setManaged(false);
        }

        String sessionEmail = UserSession.getInstance().getEmail();
        if (sessionEmail != null && !sessionEmail.isBlank()) {
            setInitialEmail(sessionEmail);
        } else {
            currentEmail = null;
            setPortalEnabled(false);
            sponsorBaseList.clear();
            loadPublicViewWithoutSession();
        }

        if (currentEmail != null && !currentEmail.isBlank()) {
            setPortalEnabled(true);
            reloadMine();
        }
    }

    private void setPortalEnabled(boolean enabled) {
        if (addSponsorBtn != null) addSponsorBtn.setDisable(!enabled);
        if (exportExcelBtn != null) exportExcelBtn.setDisable(!enabled);
        if (exportCsvBtn != null) exportCsvBtn.setDisable(!enabled);
        if (historyBtn != null) historyBtn.setDisable(!enabled);
        if (searchField != null) searchField.setDisable(!enabled);
        if (companyFilter != null) companyFilter.setDisable(!enabled);
        if (eventFilter != null) eventFilter.setDisable(!enabled);
    }

    private void reloadMine() {
        try {
            loadCategoryMetadata();
            sponsorBaseList.setAll(sponsorService.getSponsorsByContactEmail(currentEmail));
            allEvents = eventService.getAllEvents();
            recommendedEvents = loadRecommendedEventsInternal();

            updateMyKpis();
            refreshSponsorFilterCombos();
            applySponsorFilters();
            renderAllEvents();
            renderRecommendedCards();
        } catch (Exception e) {
            showError("DB", e.getMessage());
        }
    }

    private void loadPublicViewWithoutSession() {
        try {
            loadCategoryMetadata();
            allEvents = eventService.getAllEvents();
            recommendedEvents = new ArrayList<>();

            if (myContributionLabel != null) myContributionLabel.setText("0,00 DT");
            if (myEventsLabel != null) myEventsLabel.setText("0");
            if (mySponsorsLabel != null) mySponsorsLabel.setText("0");
            if (statusLabel != null) statusLabel.setText("Connectez-vous pour sponsoriser");

            renderAllEvents();
            renderRecommendedCards();
        } catch (Exception e) {
            showError("DB", e.getMessage());
        }
    }

    private void loadCategoryMetadata() {
        categoryNames.clear();
        categoryColors.clear();
        for (EventCategory category : eventCategoryService.getAllCategories()) {
            categoryNames.put(category.getId(), safe(category.getName()));
            String color = safe(category.getColor());
            categoryColors.put(category.getId(), color.isBlank() ? "#0D47A1" : color);
        }
    }

    private List<Event> loadRecommendedEventsInternal() {
        List<Event> events = new ArrayList<>();
        try {
            String industry = "";
            UserModel sessionUser = UserSession.getInstance().getCurrentUser();

            // Priorite: biographie du profil sponsor = secteur.
            if (sessionUser != null) {
                industry = safe(sessionUser.getBio());
            }

            // Fallback sur industry du dernier sponsor enregistre.
            if ((industry == null || industry.isBlank()) && currentEmail != null && !currentEmail.isBlank()) {
                List<Sponsor> sponsors = sponsorService.getSponsorsByContactEmail(currentEmail);
                industry = sponsors.stream()
                        .map(Sponsor::getIndustry)
                        .filter(value -> value != null && !value.isBlank())
                        .findFirst()
                        .orElse("");
            }

            if (industry != null && !industry.isBlank()) {
                events = matchingService.findRelevantEvents(industry);
            }

            // Si aucun match IA, proposer quand meme des evenements (evite une page vide).
            if (events == null || events.isEmpty()) {
                events = pickDefaultRecommendedEvents();
            }
        } catch (Exception ignored) {
            events = pickDefaultRecommendedEvents();
        }
        return events;
    }

    private List<Event> pickDefaultRecommendedEvents() {
        List<Event> source = (allEvents != null && !allEvents.isEmpty()) ? allEvents : eventService.getAllEvents();
        List<Event> defaults = new ArrayList<>(source);
        defaults.sort((a, b) -> {
            LocalDateTime da = a == null ? null : a.getStartDate();
            LocalDateTime db = b == null ? null : b.getStartDate();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return da.compareTo(db);
        });
        if (defaults.size() > 6) {
            return new ArrayList<>(defaults.subList(0, 6));
        }
        return defaults;
    }

    private void updateMyKpis() {
        try {
            if (mySponsorsLabel != null) {
                mySponsorsLabel.setText(String.valueOf(sponsorService.getMySponsorsCountDemo(currentEmail)));
            }
            if (myContributionLabel != null) {
                myContributionLabel.setText(String.format("%,.2f DT", sponsorService.getMyTotalContributionDemo(currentEmail)));
            }
            if (myEventsLabel != null) {
                myEventsLabel.setText(String.valueOf(sponsorService.getMySponsoredEventsCountDemo(currentEmail)));
            }
        } catch (Exception e) {
            showError("KPI", e.getMessage());
        }
    }

    private void refreshSponsorFilterCombos() {
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
                    } catch (Exception ignored) {
                    }
                }
                eventFilter.getItems().setAll(titles);
                eventFilter.setValue(null);
            }
        } catch (Exception e) {
            showError("Filtres", e.getMessage());
        }
    }

    private void applySponsorFilters() {
        if (sponsorFiltered == null) return;

        String q = searchField == null ? "" : safe(searchField.getText()).toLowerCase().trim();
        String comp = companyFilter == null ? null : companyFilter.getValue();
        String eventTitle = eventFilter == null ? null : eventFilter.getValue();

        Integer eventId = null;
        if (eventTitle != null && !eventTitle.isBlank()) {
            try {
                eventId = sponsorService.getEventIdByTitle(eventTitle);
            } catch (Exception ignored) {
            }
        }
        Integer finalEventId = eventId;

        sponsorFiltered.setPredicate(s -> {
            boolean okQ = q.isEmpty()
                    || String.valueOf(s.getId()).contains(q)
                    || safe(s.getCompany_name()).toLowerCase().contains(q)
                    || safe(s.getContact_email()).toLowerCase().contains(q);
            boolean okComp = comp == null || safe(s.getCompany_name()).equalsIgnoreCase(comp);
            boolean okEv = finalEventId == null || s.getEvent_id() == finalEventId;
            return okQ && okComp && okEv;
        });

        if (statusLabel != null) statusLabel.setText(sponsorFiltered.size() + " sponsor(s)");
        renderSponsorCards();
    }

    private void renderSponsorCards() {
        if (cardsPane == null || sponsorFiltered == null) return;
        cardsPane.getChildren().clear();
        for (Sponsor sponsor : sponsorFiltered) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(CARD_FXML));
                Parent root = loader.load();
                if (root instanceof Region region) {
                    region.setPrefWidth(440);
                    region.setMaxWidth(Double.MAX_VALUE);
                }
                SponsorCardController card = loader.getController();
                card.setData(
                        sponsor,
                        () -> openDetailsAsPage(sponsor),
                        () -> onGeneratePdfFromDetails(sponsor),
                        () -> onEdit(sponsor),
                        () -> onDelete(sponsor)
                );
                cardsPane.getChildren().add(root);
            } catch (Exception ignored) {
            }
        }
    }

    private void renderAllEvents() {
        renderEventCards(allEventsGrid, allEvents);
        if (noEventsMessage != null) {
            boolean empty = allEvents == null || allEvents.isEmpty();
            noEventsMessage.setVisible(empty);
            noEventsMessage.setManaged(empty);
        }
    }

    private void renderRecommendedCards() {
        renderEventCards(recommendedEventsGrid, recommendedEvents);
        if (noRecommendedMessage != null) {
            boolean empty = recommendedEvents == null || recommendedEvents.isEmpty();
            noRecommendedMessage.setVisible(empty);
            noRecommendedMessage.setManaged(empty);
        }
    }

    private void renderEventCards(FlowPane grid, List<Event> events) {
        if (grid == null) return;
        Platform.runLater(() -> {
            grid.setAlignment(Pos.TOP_LEFT);
            grid.getChildren().clear();
            for (Event event : events) {
                grid.getChildren().add(createEventCard(event));
            }
        });
    }

    private VBox createEventCard(Event event) {
        String color = categoryColors.getOrDefault(event.getCategoryId(), "#0D47A1");
        String categoryName = categoryNames.getOrDefault(event.getCategoryId(), "Categorie");

        VBox card = new VBox(12);
        card.getStyleClass().add("sponsor-event-card");
        card.setPrefWidth(338);
        card.setMinWidth(338);
        card.setMaxWidth(338);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_RIGHT);
        header.setPadding(new Insets(12, 14, 0, 14));

        Label badge = new Label(categoryName);
        badge.getStyleClass().add("sponsor-event-category-chip");
        badge.setStyle("-fx-text-fill: " + color + "; -fx-border-color: " + color + ";");
        header.getChildren().add(badge);

        VBox content = new VBox(10);
        content.setPadding(new Insets(0, 14, 14, 14));

        Label title = new Label(safe(event.getTitle()));
        title.setWrapText(true);
        title.getStyleClass().add("sponsor-event-title");

        Label date = new Label("Date: " + formatDate(event.getStartDate()));
        date.getStyleClass().add("sponsor-event-meta");

        Label location = new Label("Lieu: " + safe(event.getLocation()));
        location.getStyleClass().add("sponsor-event-meta");

        Button sponsorBtn = new Button("Sponsoriser");
        sponsorBtn.getStyleClass().add("sponsor-event-btn");
        sponsorBtn.setOnAction(e -> handleSponsorEvent(event));

        content.getChildren().addAll(title, date, location, sponsorBtn);
        card.getChildren().addAll(header, content);
        return card;
    }

    private void handleExportExcel() {
        try {
            List<Sponsor> toExport = new ArrayList<>(sponsorFiltered);
            if (toExport.isEmpty()) {
                showError("Export", "Aucun sponsor a exporter.");
                return;
            }
            Map<String, Double> contributions = sponsorService.getMyContributionsByCompany(currentEmail);
            JsonObject chartConfig = QuickChartService.createPieChart(
                    "Mes contributions",
                    contributions.keySet().toArray(new String[0]),
                    contributions.values().stream().mapToDouble(Double::doubleValue).toArray()
            );
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Enregistrer le fichier Excel");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers Excel", "*.xlsx"));
            chooser.setInitialFileName("mes_sponsors_export.xlsx");
            File file = chooser.showSaveDialog(exportExcelBtn != null ? exportExcelBtn.getScene().getWindow() : null);
            if (file != null) {
                ExcelExportService.exportSponsors(toExport, chartConfig, file.getAbsolutePath());
                showInfo("Export reussi", "Le fichier Excel a ete genere avec succes.");
            }
        } catch (Exception e) {
            showError("Export", "Erreur: " + e.getMessage());
        }
    }

    private void handleExportCsv() {
        try {
            List<Sponsor> toExport = new ArrayList<>(sponsorFiltered);
            if (toExport.isEmpty()) {
                showError("Export CSV", "Aucun sponsor a exporter.");
                return;
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Enregistrer le fichier CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv"));
            chooser.setInitialFileName("mes_sponsors_export.csv");
            File file = chooser.showSaveDialog(exportCsvBtn != null ? exportCsvBtn.getScene().getWindow() : null);
            if (file == null) {
                return;
            }

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                writer.write("id;event_id;entreprise;email;contribution_tnd;secteur;telephone;tax_id");
                writer.newLine();
                for (Sponsor sponsor : toExport) {
                    writer.write(String.format("%d;%d;%s;%s;%.2f;%s;%s;%s",
                            sponsor.getId(),
                            sponsor.getEvent_id(),
                            csv(safe(sponsor.getCompany_name())),
                            csv(safe(sponsor.getContact_email())),
                            sponsor.getContribution_name(),
                            csv(safe(sponsor.getIndustry())),
                            csv(safe(sponsor.getPhone())),
                            csv(safe(sponsor.getTax_id()))
                    ));
                    writer.newLine();
                }
            }

            showInfo("Export CSV", "Le fichier CSV a ete genere avec succes.");
        } catch (Exception e) {
            showError("Export CSV", "Erreur: " + e.getMessage());
        }
    }

    private void onAdd() {
        if (currentEmail == null || currentEmail.isBlank()) {
            showError("Acces", "Session sponsor invalide. Veuillez vous reconnecter.");
            return;
        }
        openFormAsPage(null, null);
    }

    private void onEdit(Sponsor existing) {
        openFormAsPage(existing, null);
    }

    private void onDelete(Sponsor sponsor) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText("Supprimer sponsor");
        confirm.setContentText("Supprimer: " + sponsor.getCompany_name() + " ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    sponsorService.deleteSponsor(sponsor.getId());
                    reloadMine();
                } catch (Exception e) {
                    showError("Suppression", e.getMessage());
                }
            }
        });
    }

    private void onGeneratePdfFromDetails(Sponsor sponsor) {
        try {
            String eventTitle = sponsorService.getEventTitleById(sponsor.getEvent_id());
            File pdf = pdfService.generateSponsorContractPdf(sponsor, eventTitle);
            if (!Desktop.isDesktopSupported()) {
                showError("PDF", "Desktop non supporte.");
                return;
            }
            Desktop.getDesktop().open(pdf);
        } catch (Exception e) {
            showError("PDF", e.getMessage());
        }
    }

    private void handleSponsorEvent(Event event) {
        if (currentEmail == null || currentEmail.isBlank()) {
            showError("Acces", "Session sponsor invalide. Veuillez vous reconnecter.");
            return;
        }
        openFormAsPage(null, event);
    }

    private void openFormAsPage(Sponsor existing, Event eventToSelect) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FORM_FXML));
            Parent root = loader.load();
            SponsorFormController ctrl = loader.getController();
            ctrl.setFixedEmail(currentEmail);
            if (existing == null) ctrl.setModeAdd();
            else ctrl.setModeEdit(existing);
            if (eventToSelect != null) ctrl.preSelectEvent(eventToSelect);

            Runnable back = () -> {
                restoreMainContent();
                reloadMine();
            };
            ctrl.setOnSaved(saved -> {
                restoreMainContent();
                reloadMine();
                openDetailsAsPage(saved);
            });
            ctrl.setOnFormDone(back);

            showInlinePage("Formulaire sponsor", root, back);
        } catch (Exception e) {
            showError("UI", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    private void openDetailsAsPage(Sponsor sponsor) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(DETAILS_FXML));
            Parent root = loader.load();
            SponsorDetailsController ctrl = loader.getController();
            ctrl.setSponsor(sponsor);
            ctrl.setOnBack(() -> {
                restoreMainContent();
                reloadMine();
            });

            showInlinePage("Details sponsor", root, () -> {
                restoreMainContent();
                reloadMine();
            });
        } catch (Exception e) {
            showError("UI", "Impossible d'ouvrir les details: " + e.getMessage());
        }
    }

    private void openHistoryPage() {
        if (sponsorBaseList.isEmpty()) {
            showInfo("Historique", "Aucune sponsorship pour le moment.");
            return;
        }
        try {
            VBox container = new VBox(12);
            container.setPadding(new Insets(12));
            for (Sponsor sponsor : new ArrayList<>(sponsorBaseList)) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(CARD_FXML));
                Parent root = loader.load();
                SponsorCardController card = loader.getController();
                card.setData(
                        sponsor,
                        () -> openDetailsAsPage(sponsor),
                        () -> onGeneratePdfFromDetails(sponsor),
                        () -> onEdit(sponsor),
                        () -> onDeleteFromHistory(sponsor, root, container)
                );
                container.getChildren().add(root);
            }
            ScrollPane scroll = new ScrollPane(container);
            scroll.setFitToWidth(true);
            showInlinePage("Historique des evenements sponsorises", scroll, this::restoreMainContent);
        } catch (Exception e) {
            showError("Historique", e.getMessage());
        }
    }

    private void onDeleteFromHistory(Sponsor sponsor, Parent cardRoot, VBox container) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression");
        confirm.setHeaderText("Supprimer sponsor");
        confirm.setContentText("Supprimer: " + sponsor.getCompany_name() + " ?");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    boolean deleted = sponsorService.deleteSponsor(sponsor.getId());
                    if (!deleted) {
                        showError("Suppression", "Suppression impossible.");
                        return;
                    }

                    sponsorBaseList.removeIf(s -> s.getId() == sponsor.getId());
                    if (container != null && cardRoot != null) {
                        container.getChildren().remove(cardRoot);
                    }
                    reloadMine();

                    if (container == null || container.getChildren().isEmpty()) {
                        showInfo("Historique", "Aucune sponsorship pour le moment.");
                        restoreMainContent();
                    }
                } catch (Exception e) {
                    showError("Suppression", e.getMessage());
                }
            }
        });
    }

    private boolean showInlinePage(String title, Parent content, Runnable onBack) {
        if (rootPane == null) return false;
        if (mainContent == null) mainContent = rootPane.getCenter();

        Button backBtn = new Button("< Retour");
        backBtn.setStyle("-fx-background-color: transparent; -fx-border-color: #cbd5e1; -fx-border-radius: 8;" +
                "-fx-background-radius: 8; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-padding: 8 14;");
        backBtn.setOnAction(e -> {
            restoreMainContent();
            if (onBack != null) onBack.run();
        });

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #0f172a;");

        HBox header = new HBox(10, backBtn, titleLbl);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(12, 16, 12, 16));
        header.setStyle("-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

        ScrollPane body = new ScrollPane(content);
        body.setFitToWidth(true);
        body.setFitToHeight(true);
        body.setStyle("-fx-background-color: #f1f5f9;");
        VBox.setVgrow(body, Priority.ALWAYS);

        VBox page = new VBox(header, body);
        rootPane.setCenter(page);
        return true;
    }

    private void restoreMainContent() {
        if (rootPane != null && mainContent != null) {
            rootPane.setCenter(mainContent);
            if (mainContent instanceof ScrollPane scrollPane) {
                scrollPane.setVvalue(0.0);
            }
        }
    }

    private String formatDate(LocalDateTime dt) {
        if (dt == null) return "-";
        return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String csv(String value) {
        if (value == null) return "";
        return value.replace(";", ",").replace("\r", " ").replace("\n", " ");
    }

    private void showError(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
