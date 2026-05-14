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
import javafx.scene.control.ProgressBar;
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
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    @FXML private Button recommendSponsorBtn;
    @FXML private Button exportExcelBtn;
    @FXML private Button exportCsvBtn;
    @FXML private Button historyBtn;
    @FXML private Button homeBtn;
    @FXML private Label statusLabel;
    @FXML private TilePane cardsPane;

    @FXML private FlowPane recommendedEventsGrid;
    @FXML private VBox noRecommendedMessage;
    @FXML private FlowPane sponsorRecommendationGrid;
    @FXML private VBox noRecommendedSponsorMessage;
    @FXML private FlowPane allEventsGrid;
    @FXML private VBox noEventsMessage;

    @FXML private Label mySponsorsSectionLabel;
    @FXML private Label myContributionSectionLabel;
    @FXML private Label myEventsSectionLabel;
    @FXML private Label avgContributionLabel;
    @FXML private Label activeSponsorsLabel;
    @FXML private Label finishedSponsorsLabel;
    @FXML private Label topCompanyLabel;
    @FXML private VBox topCompaniesBox;
    @FXML private ComboBox<String> recommendedSortFilter;
    @FXML private ComboBox<String> allEventsSortFilter;

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
    private int visibleRecommendedCount = 0;
    private int visibleCatalogCount = 0;

    private static final String CARD_FXML = "/com/example/pidev/fxml/Sponsor/sponsor-card.fxml";
    private static final String FORM_FXML = "/com/example/pidev/fxml/Sponsor/sponsor-form.fxml";
    private static final String DETAILS_FXML = "/com/example/pidev/fxml/Sponsor/sponsor-detail.fxml";

    // ==================== AUTO-REFRESH (UNIQUE) ====================
    private ScheduledExecutorService scheduler;
    private static final int REFRESH_INTERVAL_SECONDS = 5;

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

        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> {
                applySponsorFilters();
                renderRecommendedCards();
                renderAllEvents();
                refreshSearchStatus();
            });
        }
        if (companyFilter != null) companyFilter.valueProperty().addListener((obs, o, n) -> applySponsorFilters());
        if (eventFilter != null) eventFilter.valueProperty().addListener((obs, o, n) -> applySponsorFilters());
        if (recommendedSortFilter != null) recommendedSortFilter.valueProperty().addListener((obs, o, n) -> applyEventSorts());
        if (allEventsSortFilter != null) allEventsSortFilter.valueProperty().addListener((obs, o, n) -> applyEventSorts());

        if (addSponsorBtn != null) addSponsorBtn.setOnAction(e -> onAdd());
        if (recommendSponsorBtn != null) recommendSponsorBtn.setOnAction(e -> onRecommendSponsor());
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

        configureSortFilters();

        if (emailAccount != null) {
            emailAccount.setVisible(false);
            emailAccount.setManaged(false);
        }

        String sessionEmail = UserSession.getInstance().getEmail();
        if (sessionEmail != null && !sessionEmail.isBlank()) {
            setInitialEmail(sessionEmail);
            startAutoRefresh();
        } else {
            currentEmail = null;
            setPortalEnabled(false);
            sponsorBaseList.clear();
            loadPublicViewWithoutSession();
        }
    }

    private void setPortalEnabled(boolean enabled) {
        if (addSponsorBtn != null) addSponsorBtn.setDisable(!enabled);
        if (recommendSponsorBtn != null) recommendSponsorBtn.setDisable(!enabled);
        if (exportExcelBtn != null) exportExcelBtn.setDisable(!enabled);
        if (exportCsvBtn != null) exportCsvBtn.setDisable(!enabled);
        if (historyBtn != null) historyBtn.setDisable(!enabled);
        if (searchField != null) searchField.setDisable(!enabled);
        if (companyFilter != null) companyFilter.setDisable(!enabled);
        if (eventFilter != null) eventFilter.setDisable(!enabled);
        if (recommendedSortFilter != null) recommendedSortFilter.setDisable(!enabled);
        if (allEventsSortFilter != null) allEventsSortFilter.setDisable(!enabled);

        if (!enabled) {
            stopAutoRefresh();
        }
    }

    private void reloadMine() {
        try {
            loadCategoryMetadata();
            sponsorBaseList.setAll(sponsorService.getSponsorsByContactEmail(currentEmail));
            allEvents = eventService.getAllEvents();
            recommendedEvents = loadRecommendedEventsInternal();

            updateMyKpis();
            updateAdvancedStats();
            refreshSponsorFilterCombos();
            configureSortFilters();
            applyEventSorts();
            applySponsorFilters();
            renderRecommendedSponsors();
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
            if (avgContributionLabel != null) avgContributionLabel.setText("0,00 DT");
            if (activeSponsorsLabel != null) activeSponsorsLabel.setText("0");
            if (finishedSponsorsLabel != null) finishedSponsorsLabel.setText("0");
            if (topCompanyLabel != null) topCompanyLabel.setText("--");
            if (topCompaniesBox != null) {
                topCompaniesBox.getChildren().clear();
                Label empty = new Label("Aucune contribution pour le moment.");
                empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
                topCompaniesBox.getChildren().add(empty);
            }
            if (statusLabel != null) statusLabel.setText("Connectez-vous pour sponsoriser");

            applyEventSorts();
            renderAllEvents();
            renderRecommendedCards();
            refreshSearchStatus();
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

            if (sessionUser != null) {
                industry = safe(sessionUser.getBio()).trim();
            }

            if ((industry == null || industry.isBlank()) && currentEmail != null && !currentEmail.isBlank()) {
                List<Sponsor> sponsors = sponsorService.getSponsorsByContactEmail(currentEmail);
                industry = sponsors.stream()
                        .map(Sponsor::getIndustry)
                        .filter(value -> value != null && !value.isBlank())
                        .findFirst()
                        .orElse("")
                        .trim();
            }

            if (industry != null && !industry.isBlank()) {
                events = matchingService.findRelevantEvents(industry);
            }

            events = keepOnlyActiveEventsSortedByDate(events);

            if (events == null || events.isEmpty()) {
                events = pickDefaultRecommendedEvents();
            }
        } catch (Exception ignored) {
            events = pickDefaultRecommendedEvents();
        }
        return keepOnlyActiveEventsSortedByDate(events);
    }

    private List<Event> pickDefaultRecommendedEvents() {
        List<Event> source = (allEvents != null && !allEvents.isEmpty()) ? allEvents : eventService.getAllEvents();
        List<Event> defaults = keepOnlyActiveEventsSortedByDate(source);
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

    private void updateAdvancedStats() {
        try {
            double total = sponsorBaseList.stream().mapToDouble(Sponsor::getContribution_name).sum();
            int sponsorCount = sponsorBaseList.size();
            double average = sponsorCount == 0 ? 0 : total / sponsorCount;

            Map<Integer, Event> eventById = buildEventIndex();
            LocalDateTime now = LocalDateTime.now();
            long activeCount = sponsorBaseList.stream()
                    .filter(s -> {
                        Event e = eventById.get(s.getEvent_id());
                        return e == null || e.getEndDate() == null || !e.getEndDate().isBefore(now);
                    })
                    .count();
            long finishedCount = sponsorCount - activeCount;

            if (avgContributionLabel != null) {
                avgContributionLabel.setText(String.format("%,.2f DT", average));
            }
            if (activeSponsorsLabel != null) {
                activeSponsorsLabel.setText(String.valueOf(activeCount));
            }
            if (finishedSponsorsLabel != null) {
                finishedSponsorsLabel.setText(String.valueOf(finishedCount));
            }

            if (topCompaniesBox != null) {
                topCompaniesBox.getChildren().clear();
                if (sponsorBaseList.isEmpty()) {
                    Label empty = new Label("Aucune sponsorship pour le moment.");
                    empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
                    topCompaniesBox.getChildren().add(empty);
                    if (topCompanyLabel != null) {
                        topCompanyLabel.setText("--");
                    }
                } else {
                    Map<String, Double> byEvent = new HashMap<>();
                    for (Sponsor sponsor : sponsorBaseList) {
                        String eventTitle = getEventTitle(sponsor.getEvent_id(), eventById);
                        byEvent.merge(eventTitle, sponsor.getContribution_name(), Double::sum);
                    }

                    List<Map.Entry<String, Double>> sortedEvents = new ArrayList<>(byEvent.entrySet());
                    sortedEvents.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

                    if (topCompanyLabel != null) {
                        Map.Entry<String, Double> top = sortedEvents.get(0);
                        topCompanyLabel.setText(top.getKey() + " (" + String.format("%,.2f DT", top.getValue()) + ")");
                        topCompanyLabel.setWrapText(true);
                    }

                    int limit = Math.min(4, sortedEvents.size());
                    double max = sortedEvents.stream().mapToDouble(Map.Entry::getValue).max().orElse(0);

                    for (int i = 0; i < limit; i++) {
                        Map.Entry<String, Double> eventEntry = sortedEvents.get(i);
                        String eventTitle = eventEntry.getKey();
                        double eventAmount = eventEntry.getValue();

                        HBox line = new HBox();
                        line.setAlignment(Pos.CENTER_LEFT);
                        line.setSpacing(10);
                        line.getStyleClass().add("sponsor-chart-line");

                        Label rank = new Label(String.valueOf(i + 1));
                        rank.getStyleClass().add("sponsor-chart-rank");

                        Label company = new Label(eventTitle);
                        company.getStyleClass().add("sponsor-chart-label");
                        company.setWrapText(true);
                        company.setMaxWidth(Double.MAX_VALUE);
                        HBox.setHgrow(company, Priority.ALWAYS);

                        Label amount = new Label(String.format("%,.2f DT", eventAmount));
                        amount.getStyleClass().add("sponsor-chart-amount");
                        amount.setMinWidth(120);
                        amount.setAlignment(Pos.CENTER_RIGHT);

                        line.getChildren().addAll(rank, company, amount);

                        ProgressBar progressBar = new ProgressBar(max <= 0 ? 0 : eventAmount / max);
                        progressBar.getStyleClass().add("sponsor-mini-progress");
                        progressBar.setMaxWidth(Double.MAX_VALUE);
                        progressBar.prefWidthProperty().bind(topCompaniesBox.widthProperty().subtract(24));

                        VBox row = new VBox(4, line, progressBar);
                        row.getStyleClass().add("sponsor-chart-row");
                        topCompaniesBox.getChildren().add(row);
                    }
                }
            }
        } catch (Exception e) {
            if (avgContributionLabel != null) avgContributionLabel.setText("0,00 DT");
            if (activeSponsorsLabel != null) activeSponsorsLabel.setText("0");
            if (finishedSponsorsLabel != null) finishedSponsorsLabel.setText("0");
            if (topCompanyLabel != null) topCompanyLabel.setText("--");
        }
    }

    private void configureSortFilters() {
        // Tri manuel retiré (tri automatique par date)
    }

    private void applyEventSorts() {
        allEvents = keepOnlyActiveEventsSortedByDate(allEvents);
        recommendedEvents = keepOnlyActiveEventsSortedByDate(recommendedEvents);
        renderRecommendedCards();
        renderAllEvents();
        renderRecommendedSponsors();
    }

    private List<Event> keepOnlyActiveEventsSortedByDate(List<Event> source) {
        List<Event> sorted = new ArrayList<>();
        if (source == null) {
            return sorted;
        }

        Set<Integer> sponsoredEventIds = getSponsoredEventIds();
        for (Event event : source) {
            if (event != null && isEventActive(event) && !sponsoredEventIds.contains(event.getId())) {
                sorted.add(event);
            }
        }

        sorted.sort(Comparator.comparing(
                Event::getStartDate,
                Comparator.nullsLast(LocalDateTime::compareTo)
        ));
        return sorted;
    }

    private Set<Integer> getSponsoredEventIds() {
        Set<Integer> ids = new HashSet<>();
        if (sponsorBaseList == null || sponsorBaseList.isEmpty()) {
            return ids;
        }
        for (Sponsor sponsor : sponsorBaseList) {
            if (sponsor != null && sponsor.getEvent_id() > 0) {
                ids.add(sponsor.getEvent_id());
            }
        }
        return ids;
    }

    private void refreshSponsorFilterCombos() {
        // Filtres combo retirés
    }

    private void applySponsorFilters() {
        if (sponsorFiltered == null) return;

        String q = searchField == null ? "" : safe(searchField.getText()).toLowerCase().trim();
        Map<Integer, Event> eventById = buildEventIndex();

        sponsorFiltered.setPredicate(s -> {
            String eventTitle = getEventTitle(s.getEvent_id(), eventById).toLowerCase();
            boolean okQ = q.isEmpty()
                    || String.valueOf(s.getId()).contains(q)
                    || safe(s.getCompany_name()).toLowerCase().contains(q)
                    || safe(s.getContact_email()).toLowerCase().contains(q)
                    || eventTitle.contains(q);
            return okQ;
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
                        null
                );
                cardsPane.getChildren().add(root);
            } catch (Exception ignored) {
            }
        }
    }

    private void renderAllEvents() {
        List<Event> visibleEvents = filterEventsBySearch(allEvents);
        renderEventCards(allEventsGrid, visibleEvents);
        if (noEventsMessage != null) {
            boolean empty = visibleEvents == null || visibleEvents.isEmpty();
            noEventsMessage.setVisible(empty);
            noEventsMessage.setManaged(empty);
        }
        visibleCatalogCount = visibleEvents == null ? 0 : visibleEvents.size();
        refreshSearchStatus();
    }

    private void renderRecommendedCards() {
        List<Event> visibleEvents = filterEventsBySearch(recommendedEvents);
        renderEventCards(recommendedEventsGrid, visibleEvents);
        if (noRecommendedMessage != null) {
            boolean empty = visibleEvents == null || visibleEvents.isEmpty();
            noRecommendedMessage.setVisible(empty);
            noRecommendedMessage.setManaged(empty);
        }
        visibleRecommendedCount = visibleEvents == null ? 0 : visibleEvents.size();
        refreshSearchStatus();
    }

    private void renderRecommendedSponsors() {
        if (sponsorRecommendationGrid == null) return;
        try {
            Platform.runLater(() -> {
                sponsorRecommendationGrid.getChildren().clear();
                try {
                    List<Sponsor> allSponsors = sponsorService.getAllSponsors();
                    List<Sponsor> recommendedSponsors = new ArrayList<>();

                    if (allSponsors != null && !allSponsors.isEmpty()) {
                        recommendedSponsors = allSponsors.stream()
                                .limit(6)
                                .collect(java.util.stream.Collectors.toList());
                    }

                    if (recommendedSponsors.isEmpty()) {
                        if (noRecommendedSponsorMessage != null) {
                            noRecommendedSponsorMessage.setVisible(true);
                            noRecommendedSponsorMessage.setManaged(true);
                        }
                    } else {
                        if (noRecommendedSponsorMessage != null) {
                            noRecommendedSponsorMessage.setVisible(false);
                            noRecommendedSponsorMessage.setManaged(false);
                        }
                        for (Sponsor sponsor : recommendedSponsors) {
                            sponsorRecommendationGrid.getChildren().add(createSponsorCard(sponsor));
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Erreur: " + ex.getMessage());
                    if (noRecommendedSponsorMessage != null) {
                        noRecommendedSponsorMessage.setVisible(true);
                        noRecommendedSponsorMessage.setManaged(true);
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Erreur chargement sponsors recommandés: " + e.getMessage());
        }
    }

    private VBox createSponsorCard(Sponsor sponsor) {
        VBox card = new VBox(12);
        card.getStyleClass().add("sponsor-event-card");
        card.setPrefWidth(338);
        card.setMinWidth(338);
        card.setMaxWidth(338);

        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_RIGHT);
        header.setPadding(new Insets(12, 14, 0, 14));

        Label badge = new Label("💼 Sponsor");
        badge.getStyleClass().add("sponsor-event-category-chip");
        badge.setStyle("-fx-text-fill: #0D47A1; -fx-border-color: #0D47A1;");
        header.getChildren().add(badge);

        VBox content = new VBox(10);
        content.setPadding(new Insets(0, 14, 14, 14));

        Label name = new Label(safe(sponsor.getCompany_name()));
        name.setWrapText(true);
        name.getStyleClass().add("sponsor-event-title");

        Label email = new Label("Email: " + safe(sponsor.getContact_email()));
        email.getStyleClass().add("sponsor-event-meta");
        email.setWrapText(true);

        Label industry = new Label("Secteur: " + safe(sponsor.getIndustry()));
        industry.getStyleClass().add("sponsor-event-meta");

        Label contribution = new Label("Contribution: " + String.format("%,.2f DT", sponsor.getContribution_name()));
        contribution.getStyleClass().add("sponsor-event-meta");

        Button viewBtn = new Button("Voir Détails");
        viewBtn.getStyleClass().add("sponsor-event-btn");
        viewBtn.setOnAction(e -> openDetailsAsPage(sponsor));

        content.getChildren().addAll(name, email, industry, contribution, viewBtn);
        card.getChildren().addAll(header, content);
        return card;
    }

    private void renderEventCards(FlowPane grid, List<Event> events) {
        if (grid == null) return;
        Platform.runLater(() -> {
            grid.setAlignment(Pos.TOP_LEFT);
            grid.getChildren().clear();
            List<Event> sortedEvents = keepOnlyActiveEventsSortedByDate(events);
            for (Event event : sortedEvents) {
                grid.getChildren().add(createEventCard(event));
            }
        });
    }

    private List<Event> filterEventsBySearch(List<Event> source) {
        List<Event> base = keepOnlyActiveEventsSortedByDate(source);
        String q = normalizeSearch(searchField == null ? "" : safe(searchField.getText()));
        if (q.isEmpty()) {
            return base;
        }

        List<Event> exactTitleMatches = new ArrayList<>();
        for (Event event : base) {
            String title = normalizeSearch(safe(event.getTitle()));
            if (title.equals(q)) {
                exactTitleMatches.add(event);
            }
        }
        if (!exactTitleMatches.isEmpty()) {
            return exactTitleMatches;
        }

        List<Event> filtered = new ArrayList<>();
        for (Event event : base) {
            String title = normalizeSearch(safe(event.getTitle()));
            String location = normalizeSearch(safe(event.getLocation()));
            String category = normalizeSearch(safe(categoryNames.get(event.getCategoryId())));
            if (title.contains(q) || location.contains(q) || category.contains(q)) {
                filtered.add(event);
            }
        }
        return filtered;
    }

    private void refreshSearchStatus() {
        if (statusLabel == null) {
            return;
        }
        int total = visibleRecommendedCount + visibleCatalogCount;
        statusLabel.setText(total + " evenement(s) affiche(s)");
    }

    private String normalizeSearch(String value) {
        String base = safe(value).toLowerCase().trim();
        String normalized = Normalizer.normalize(base, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
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
        title.setMaxWidth(310);
        title.setMinHeight(50);
        title.getStyleClass().add("sponsor-event-title");

        Label date = new Label("📅 " + formatDate(event.getStartDate()));
        date.getStyleClass().add("sponsor-event-meta");
        date.setWrapText(false);

        Label location = new Label("📍 " + safe(event.getLocation()));
        location.getStyleClass().add("sponsor-event-meta");
        location.setWrapText(true);

        Label description = new Label(safe(event.getDescription()));
        description.getStyleClass().add("sponsor-event-meta");
        description.setWrapText(true);
        description.setMaxWidth(310);
        description.setMinHeight(40);

        Button sponsorBtn = new Button("Sponsoriser");
        sponsorBtn.getStyleClass().add("sponsor-event-btn");
        sponsorBtn.setOnAction(e -> handleSponsorEvent(event));

        content.getChildren().addAll(title, date, location, description, sponsorBtn);
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

    private void onRecommendSponsor() {
        if (currentEmail == null || currentEmail.isBlank()) {
            showError("Acces", "Session sponsor invalide. Veuillez vous reconnecter.");
            return;
        }
        showInfo("Recommander Sponsor", "Voulez-vous recommander un sponsor? Fonctionnalité en développement.");
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
            Map<Integer, Event> eventById = buildEventIndex();
            LocalDateTime now = LocalDateTime.now();

            List<Sponsor> ongoingAndNew = new ArrayList<>();
            List<Sponsor> finished = new ArrayList<>();

            for (Sponsor sponsor : new ArrayList<>(sponsorBaseList)) {
                Event event = eventById.get(sponsor.getEvent_id());
                LocalDateTime endDate = event == null ? null : event.getEndDate();
                if (endDate != null && endDate.isBefore(now)) {
                    finished.add(sponsor);
                } else {
                    ongoingAndNew.add(sponsor);
                }
            }

            Comparator<Sponsor> sponsorByEventStart = Comparator.comparing(
                    s -> getEventStartDate(s.getEvent_id(), eventById),
                    Comparator.nullsLast(LocalDateTime::compareTo)
            );
            ongoingAndNew.sort(sponsorByEventStart);
            finished.sort(sponsorByEventStart);

            VBox container = new VBox(16);
            container.setPadding(new Insets(12));

            Label intro = new Label("Historique metier: separation des sponsorships en cours et termines.");
            intro.setStyle("-fx-font-size: 13px; -fx-text-fill: #334155; -fx-font-weight: 700;");
            container.getChildren().add(intro);

            container.getChildren().add(buildHistorySection(
                    "Nouveaux evenements sponsorises / en cours",
                    ongoingAndNew,
                    "Aucun evenement sponsorise en cours.",
                    true,
                    true,
                    false
            ));

            container.getChildren().add(buildHistorySection(
                    "Evenements sponsorises deja termines",
                    finished,
                    "Aucun evenement sponsorise termine.",
                    false,
                    false,
                    false
            ));

            ScrollPane scroll = new ScrollPane(container);
            scroll.setFitToWidth(true);
            scroll.setStyle("-fx-background-color: #f1f5f9;");
            showInlinePage("Historique sponsor avance", scroll, this::restoreMainContent);
        } catch (Exception e) {
            showError("Historique", e.getMessage());
        }
    }

    private VBox buildHistorySection(String title, List<Sponsor> sponsors, String emptyMessage, boolean allowEdit, boolean allowPdf, boolean allowDelete) {
        VBox section = new VBox(10);
        section.setPadding(new Insets(14));
        section.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12;");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #0f172a; -fx-font-weight: 800;");

        double total = sponsors.stream().mapToDouble(Sponsor::getContribution_name).sum();
        Label kpiLabel = new Label(sponsors.size() + " sponsorship(s) | Contribution totale: " + String.format("%,.2f DT", total));
        kpiLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b; -fx-font-weight: 700;");

        section.getChildren().addAll(titleLabel, kpiLabel);

        if (sponsors.isEmpty()) {
            Label emptyLabel = new Label(emptyMessage);
            emptyLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
            section.getChildren().add(emptyLabel);
            return section;
        }

        VBox cardsBox = new VBox(10);
        for (Sponsor sponsor : sponsors) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(CARD_FXML));
                Parent root = loader.load();
                SponsorCardController card = loader.getController();
                card.setData(
                        sponsor,
                        () -> openDetailsAsPage(sponsor),
                        allowPdf ? () -> onGeneratePdfFromDetails(sponsor) : null,
                        allowEdit ? () -> onEdit(sponsor) : null,
                        allowDelete ? () -> onDeleteFromHistory(sponsor) : null
                );
                cardsBox.getChildren().add(root);
            } catch (Exception e) {
                Label err = new Label("Erreur affichage sponsor ID " + sponsor.getId() + ": " + e.getMessage());
                err.setStyle("-fx-text-fill: #b91c1c; -fx-font-size: 12px;");
                cardsBox.getChildren().add(err);
            }
        }
        section.getChildren().add(cardsBox);
        return section;
    }

    private Map<Integer, Event> buildEventIndex() {
        Map<Integer, Event> eventById = new HashMap<>();
        if (allEvents != null) {
            for (Event event : allEvents) {
                if (event != null) {
                    eventById.put(event.getId(), event);
                }
            }
        }
        for (Sponsor sponsor : sponsorBaseList) {
            int eventId = sponsor.getEvent_id();
            if (!eventById.containsKey(eventId)) {
                Event event = eventService.getEventById(eventId);
                if (event != null) {
                    eventById.put(event.getId(), event);
                }
            }
        }
        return eventById;
    }

    private LocalDateTime getEventStartDate(int eventId, Map<Integer, Event> eventById) {
        Event event = eventById.get(eventId);
        if (event != null && event.getStartDate() != null) {
            return event.getStartDate();
        }
        return null;
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String csv(String value) {
        if (value == null) return "";
        return value.replace(";", ",").replace("\r", " ").replace("\n", " ");
    }

    private void showInlinePage(String title, Parent content, Runnable onClose) {
        try {
            if (rootPane == null) return;
            StackPane overlay = new StackPane();
            overlay.getStyleClass().add("page-overlay");
            overlay.setPrefWidth(rootPane.getWidth());
            overlay.setPrefHeight(rootPane.getHeight());

            VBox container = new VBox(0);
            container.getStyleClass().add("page-container");
            container.setPrefWidth(800);
            container.setPrefHeight(600);
            container.setAlignment(Pos.TOP_CENTER);

            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("page-title");

            Button closeButton = new Button("Fermer");
            closeButton.getStyleClass().add("page-close-button");
            closeButton.setOnAction(e -> {
                rootPane.getChildren().remove(overlay);
                if (onClose != null) onClose.run();
            });

            container.getChildren().addAll(titleLabel, closeButton, content);
            overlay.getChildren().add(container);
            rootPane.setCenter(overlay);
        } catch (Exception e) {
            showError("UI", "Erreur affichage page: " + e.getMessage());
        }
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

    // ==================== AUTO-REFRESH MÉTHODES ====================
    private void startAutoRefresh() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(
                    this::refreshDataFromDatabase,
                    REFRESH_INTERVAL_SECONDS,
                    REFRESH_INTERVAL_SECONDS,
                    TimeUnit.SECONDS
            );
            System.out.println("✅ Auto-refresh Sponsor Portal démarré (tous les " + REFRESH_INTERVAL_SECONDS + "s)");
        }
    }

    private void stopAutoRefresh() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            System.out.println("⏹️ Auto-refresh Sponsor Portal arrêté");
        }
    }

    private void refreshDataFromDatabase() {
        try {
            List<Sponsor> newSponsors = sponsorService.getSponsorsByContactEmail(currentEmail);
            List<Event> newEvents = eventService.getAllEvents();

            Platform.runLater(() -> {
                sponsorBaseList.setAll(newSponsors);
                allEvents = newEvents;
                recommendedEvents = loadRecommendedEventsInternal();

                updateMyKpis();
                updateAdvancedStats();
                applyEventSorts();
                applySponsorFilters();
                renderRecommendedSponsors();
            });
        } catch (Exception e) {
            System.err.println("⚠️ Erreur auto-refresh Portal Sponsor: " + e.getMessage());
        }
    }

    private boolean isEventActive(Event event) {
        if (event == null) return false;
        LocalDateTime end = event.getEndDate();
        return end == null || !end.isBefore(LocalDateTime.now());
    }

    private String getEventTitle(int eventId, Map<Integer, Event> eventById) {
        Event event = eventById.get(eventId);
        if (event != null && event.getTitle() != null && !event.getTitle().isBlank()) {
            return event.getTitle();
        }
        try {
            String title = sponsorService.getEventTitleById(eventId);
            return (title == null || title.isBlank()) ? "-" : title;
        } catch (Exception ignored) {
            return "-";
        }
    }

    private void onDeleteFromHistory(Sponsor sponsor) {
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
                    reloadMine();
                    if (sponsorBaseList.isEmpty()) {
                        showInfo("Historique", "Aucune sponsorship pour le moment.");
                        restoreMainContent();
                    } else {
                        openHistoryPage();
                    }
                } catch (Exception e) {
                    showError("Suppression", e.getMessage());
                }
            }
        });
    }
}