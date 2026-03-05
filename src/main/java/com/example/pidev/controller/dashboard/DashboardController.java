package com.example.pidev.controller.dashboard;

import com.example.pidev.MainController;
import com.example.pidev.model.event.Event;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.user.UserService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
// Remplacer les imports génériques par des imports spécifiques
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class DashboardController implements Initializable {

    // Stats cards
    @FXML
    private Label totalEventsLabel;
    @FXML
    private Label eventsEvolutionLabel;
    @FXML
    private Label totalParticipantsLabel;
    @FXML
    private Label participantsEvolutionLabel;
    @FXML
    private Label participationRateLabel;
    @FXML
    private Label participationEvolutionLabel;

    // Progress bars and labels
    @FXML
    private ProgressBar planifieProgress;
    @FXML
    private Label planifiePercentLabel;
    @FXML
    private Label planifieCountLabel;
    @FXML
    private ProgressBar enCoursProgress;
    @FXML
    private Label enCoursPercentLabel;
    @FXML
    private Label enCoursCountLabel;
    @FXML
    private ProgressBar termineProgress;
    @FXML
    private Label terminePercentLabel;
    @FXML
    private Label termineCountLabel;

    // Upcoming events
    @FXML
    private VBox upcomingEventsContainer;
    @FXML
    private Button viewAllEventsBtn;

    // Action buttons
    @FXML
    private Button refreshBtn;
    @FXML
    private Button createEventBtn;
    @FXML
    private Button exportBtn;
    @FXML
    private Button reportBtn;

    private EventService eventService;
    private UserService userService;
    private MainController mainController;

    private ScheduledExecutorService scheduler;
    private LocalDateTime lastUpdate = LocalDateTime.now();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("Initialisation DashboardController...");

        eventService = new EventService();
        try {
            userService = new UserService();
        } catch (Exception e) {
            System.err.println("❌ Erreur de connexion au service utilisateur: " + e.getMessage());
            e.printStackTrace();
        }

        debugElements();
        loadDashboardData();
        setupButtons();

        startAutoRefresh();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    private void startAutoRefresh() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (shouldRefresh()) {
                Platform.runLater(() -> {
                    System.out.println("🔄 Rafraîchissement automatique du dashboard...");
                    loadDashboardData();
                });
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    private boolean shouldRefresh() {
        try {
            int currentEventCount = eventService.countEvents();
            int displayedEventCount = Integer.parseInt(totalEventsLabel.getText());

            int currentParticipantCount = userService.getTotalParticipantsCount();
            int displayedParticipantCount = parseNumber(totalParticipantsLabel.getText());

            return currentEventCount != displayedEventCount ||
                    currentParticipantCount != displayedParticipantCount;
        } catch (Exception e) {
            return true;
        }
    }

    private int parseNumber(String text) {
        try {
            if (text.endsWith("k")) {
                return (int) (Double.parseDouble(text.replace("k", "")) * 1000);
            }
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void refreshData() {
        Platform.runLater(() -> {
            System.out.println("🔄 Rafraîchissement manuel du dashboard...");
            loadDashboardData();
        });
    }

    private void debugElements() {
        System.out.println("=== Vérification des éléments FXML ===");
        System.out.println("totalEventsLabel: " + (totalEventsLabel != null ? "OK" : "NULL"));
        System.out.println("totalParticipantsLabel: " + (totalParticipantsLabel != null ? "OK" : "NULL"));
        System.out.println("participationRateLabel: " + (participationRateLabel != null ? "OK" : "NULL"));
        System.out.println("planifieProgress: " + (planifieProgress != null ? "OK" : "NULL"));
        System.out.println("enCoursProgress: " + (enCoursProgress != null ? "OK" : "NULL"));
        System.out.println("termineProgress: " + (termineProgress != null ? "OK" : "NULL"));
        System.out.println("upcomingEventsContainer: " + (upcomingEventsContainer != null ? "OK" : "NULL"));
        System.out.println("createEventBtn: " + (createEventBtn != null ? "OK" : "NULL"));
        System.out.println("exportBtn: " + (exportBtn != null ? "OK" : "NULL"));
        System.out.println("reportBtn: " + (reportBtn != null ? "OK" : "NULL"));
        System.out.println("=====================================");
    }

    private void loadDashboardData() {
        try {
            List<Event> allEvents = eventService.getAllEvents();
            List<Event> upcomingEvents = eventService.getUpcomingEvents();

            List<Event> limitedUpcomingEvents = upcomingEvents.stream()
                    .limit(5)
                    .collect(Collectors.toList());

            List<Event> draftEvents = eventService.getEventsByStatus("DRAFT");
            List<Event> publishedEvents = eventService.getEventsByStatus("PUBLISHED");

            int totalParticipants = 0;
            if (userService != null) {
                totalParticipants = userService.getTotalParticipantsCount();
            }

            EventStats stats = calculateEventStats(allEvents);
            double participationRate = calculateAverageParticipationRate(allEvents, totalParticipants);

            updateStatsCards(allEvents.size(), totalParticipants, participationRate);
            updateStatusBars(stats.planned, stats.ongoing, stats.completed, allEvents.size());
            updateUpcomingEvents(limitedUpcomingEvents);

            lastUpdate = LocalDateTime.now();

            System.out.println("✅ Dashboard mis à jour à " + lastUpdate.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            System.out.println("   Total événements: " + allEvents.size());
            System.out.println("   Total participants: " + totalParticipants);
            System.out.println("   Brouillons: " + draftEvents.size());
            System.out.println("   Publiés: " + publishedEvents.size());

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement des données: " + e.getMessage());
            e.printStackTrace();
            showErrorPlaceholders();
        }
    }

    private EventStats calculateEventStats(List<Event> events) {
        int planned = 0;
        int ongoing = 0;
        int completed = 0;

        LocalDateTime now = LocalDateTime.now();

        for (Event event : events) {
            String status = event.getStatus() != null ? event.getStatus().name() : "DRAFT";

            if ("DRAFT".equals(status)) {
                planned++;
            } else if ("PUBLISHED".equals(status)) {
                if (event.getStartDate() != null && event.getEndDate() != null) {
                    if (now.isAfter(event.getEndDate())) {
                        completed++;
                    } else if (now.isAfter(event.getStartDate()) && now.isBefore(event.getEndDate())) {
                        ongoing++;
                    } else {
                        planned++;
                    }
                } else {
                    planned++;
                }
            }
        }

        return new EventStats(planned, ongoing, completed);
    }

    private double calculateAverageParticipationRate(List<Event> events, int totalParticipants) {
        if (events.isEmpty()) return 0;

        int totalCapacity = events.stream()
                .mapToInt(Event::getCapacity)
                .sum();

        return totalCapacity > 0 ? (double) totalParticipants / totalCapacity * 100 : 0;
    }

    private int getWeeklyEvents() {
        try {
            List<Event> upcomingEvents = eventService.getUpcomingEvents();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextWeek = now.plusWeeks(1);

            return (int) upcomingEvents.stream()
                    .filter(e -> e.getStartDate() != null &&
                            e.getStartDate().isAfter(now) &&
                            e.getStartDate().isBefore(nextWeek))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    private int getWeeklyParticipants() {
        try {
            if (userService == null) return 0;

            var allUsers = userService.getAllUsers();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime weekAgo = now.minusWeeks(1);

            return (int) allUsers.stream()
                    .filter(u -> u.getRegistrationDate() != null &&
                            u.getRegistrationDate().isAfter(weekAgo))
                    .count();
        } catch (Exception e) {
            System.err.println("❌ Erreur calcul participants semaine: " + e.getMessage());
            return 0;
        }
    }

    private void updateStatsCards(int totalEvents, int totalParticipants, double participationRate) {
        if (totalEventsLabel != null) {
            totalEventsLabel.setText(String.valueOf(totalEvents));
        }
        if (totalParticipantsLabel != null) {
            totalParticipantsLabel.setText(formatNumber(totalParticipants));
        }
        if (participationRateLabel != null) {
            participationRateLabel.setText(String.format("%.1f%%", participationRate));
        }

        if (eventsEvolutionLabel != null) {
            int weeklyEvents = getWeeklyEvents();
            eventsEvolutionLabel.setText((weeklyEvents > 0 ? "+" : "") + weeklyEvents + " cette semaine");
        }
        if (participantsEvolutionLabel != null) {
            int weeklyParticipants = getWeeklyParticipants();
            participantsEvolutionLabel.setText((weeklyParticipants > 0 ? "+" : "") + weeklyParticipants + " cette semaine");
        }
        if (participationEvolutionLabel != null) {
            String evolution = participationRate > 75 ? "+5%" : "-2%";
            participationEvolutionLabel.setText(evolution + " vs dernier mois");
        }
    }

    private void updateStatusBars(int planned, int ongoing, int completed, int total) {
        if (total == 0) {
            if (planifieProgress != null) planifieProgress.setProgress(0);
            if (enCoursProgress != null) enCoursProgress.setProgress(0);
            if (termineProgress != null) termineProgress.setProgress(0);

            if (planifieCountLabel != null) planifieCountLabel.setText("0 événement");
            if (enCoursCountLabel != null) enCoursCountLabel.setText("0 événement");
            if (termineCountLabel != null) termineCountLabel.setText("0 événement");

            if (planifiePercentLabel != null) planifiePercentLabel.setText("0%");
            if (enCoursPercentLabel != null) enCoursPercentLabel.setText("0%");
            if (terminePercentLabel != null) terminePercentLabel.setText("0%");
            return;
        }

        double plannedPercent = (double) planned / total;
        double ongoingPercent = (double) ongoing / total;
        double completedPercent = (double) completed / total;

        if (planifieProgress != null) planifieProgress.setProgress(plannedPercent);
        if (enCoursProgress != null) enCoursProgress.setProgress(ongoingPercent);
        if (termineProgress != null) termineProgress.setProgress(completedPercent);

        if (planifiePercentLabel != null) {
            planifiePercentLabel.setText(String.format("%.0f%%", plannedPercent * 100));
        }
        if (enCoursPercentLabel != null) {
            enCoursPercentLabel.setText(String.format("%.0f%%", ongoingPercent * 100));
        }
        if (terminePercentLabel != null) {
            terminePercentLabel.setText(String.format("%.0f%%", completedPercent * 100));
        }

        if (planifieCountLabel != null) {
            planifieCountLabel.setText(planned + " événement" + (planned > 1 ? "s" : ""));
        }
        if (enCoursCountLabel != null) {
            enCoursCountLabel.setText(ongoing + " événement" + (ongoing > 1 ? "s" : ""));
        }
        if (termineCountLabel != null) {
            termineCountLabel.setText(completed + " événement" + (completed > 1 ? "s" : ""));
        }
    }

    private void updateUpcomingEvents(List<Event> events) {
        if (upcomingEventsContainer == null) return;

        upcomingEventsContainer.getChildren().clear();

        if (events.isEmpty()) {
            Label noEventsLabel = new Label("Aucun événement à venir");
            noEventsLabel.setStyle("-fx-text-fill: #64748b; -fx-padding: 20;");
            upcomingEventsContainer.getChildren().add(noEventsLabel);
            return;
        }

        for (Event event : events) {
            HBox eventBox = createEventCard(event);
            upcomingEventsContainer.getChildren().add(eventBox);
        }
    }

    private HBox createEventCard(Event event) {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(15, 20, 15, 20));
        box.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 1; " +
                "-fx-cursor: hand;");

        box.setOnMouseEntered(e ->
                box.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 10; " +
                        "-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-cursor: hand;"));
        box.setOnMouseExited(e ->
                box.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; " +
                        "-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-cursor: hand;"));

        StackPane statusIndicator = new StackPane();
        Circle circle = new Circle(5);

        String status = event.getStatus() != null ? event.getStatus().name() : "DRAFT";
        String displayStatus = getDisplayStatus(status);
        String statusColor = getStatusColor(status);

        circle.setFill(Color.web(statusColor));
        statusIndicator.getChildren().add(circle);

        VBox infoBox = new VBox(3);
        VBox.setVgrow(infoBox, Priority.ALWAYS);
        infoBox.setMaxWidth(Double.MAX_VALUE);

        Label titleLabel = new Label(event.getTitle());
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        String dateStr = event.getStartDate() != null ?
                event.getStartDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")) : "Date non définie";

        String location = event.getLocation() != null ? event.getLocation() : "Lieu non défini";
        int capacity = event.getCapacity();

        int participants = getEventParticipantCount(event.getId());

        Label detailsLabel = new Label(String.format("%s • %s • %d/%d participants",
                dateStr, location, participants, capacity));
        detailsLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");

        infoBox.getChildren().addAll(titleLabel, detailsLabel);

        Label statusLabel = new Label(displayStatus);
        statusLabel.setStyle(getStatusStyle(displayStatus));

        HBox.setHgrow(infoBox, Priority.ALWAYS);

        box.getChildren().addAll(statusIndicator, infoBox, statusLabel);
        box.setOnMouseClicked(e -> showEventDetails(event));

        return box;
    }

    private int getEventParticipantCount(int eventId) {
        try {
            // TODO: Implémenter avec votre InscriptionService
            // return inscriptionService.countParticipantsByEvent(eventId);
            // Pour l'instant, retourne une valeur simulée
            return (int) (Math.random() * 50) + 10;
        } catch (Exception e) {
            return 0;
        }
    }

    private String getDisplayStatus(String status) {
        switch (status) {
            case "PUBLISHED": return "Publié";
            case "DRAFT": return "Brouillon";
            default: return status;
        }
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "PUBLISHED": return "#22c55e";
            case "DRAFT": return "#f59e0b";
            default: return "#3b82f6";
        }
    }

    private String getStatusStyle(String status) {
        switch (status) {
            case "Publié":
                return "-fx-text-fill: #22c55e; -fx-font-size: 12px; -fx-font-weight: bold; " +
                        "-fx-padding: 6 15; -fx-background-color: #dcfce7; -fx-background-radius: 15;";
            case "Brouillon":
                return "-fx-text-fill: #f59e0b; -fx-font-size: 12px; -fx-font-weight: bold; " +
                        "-fx-padding: 6 15; -fx-background-color: #fef3c7; -fx-background-radius: 15;";
            default:
                return "-fx-text-fill: #3b82f6; -fx-font-size: 12px; -fx-font-weight: bold; " +
                        "-fx-padding: 6 15; -fx-background-color: #dbeafe; -fx-background-radius: 15;";
        }
    }

    private void setupButtons() {
        if (refreshBtn != null) {
            refreshBtn.setOnAction(e -> {
                System.out.println("🔄 Actualisation manuelle du dashboard...");
                loadDashboardData();
            });
        }

        if (createEventBtn != null) {
            createEventBtn.setOnAction(e -> {
                System.out.println("➕ Création d'un nouvel événement...");
                if (mainController != null) {
                    mainController.showEventForm(null);
                } else {
                    showAlert("Information", "Fonction de création d'événement");
                }
            });
        }

        if (viewAllEventsBtn != null) {
            viewAllEventsBtn.setOnAction(e -> {
                System.out.println("📋 Voir tous les événements...");
                if (mainController != null) {
                    mainController.showEventsList();
                }
            });
        }

        if (exportBtn != null) {
            exportBtn.setOnAction(e ->
                    showAlert("Exportation", "Fonction d'exportation à implémenter"));
        }

        if (reportBtn != null) {
            reportBtn.setOnAction(e -> generateReport());
        }
    }

    // ================= MÉTHODE DE GÉNÉRATION DE RAPPORT AMÉLIORÉE =================

    @FXML
    private void generateReport() {
        try {
            System.out.println("📊 Génération du rapport...");

            List<Event> allEvents = eventService.getAllEvents();
            int totalParticipants = userService.getTotalParticipantsCount();

            Stage reportStage = new Stage();
            reportStage.setTitle("EventFlow - Rapport détaillé des événements");
            reportStage.initModality(Modality.APPLICATION_MODAL);
            reportStage.setWidth(1000);
            reportStage.setHeight(800);

            // ScrollPane principal
            ScrollPane mainScrollPane = new ScrollPane();
            mainScrollPane.setFitToWidth(true);
            mainScrollPane.setStyle("-fx-background-color: #f8fafc; -fx-background: #f8fafc; -fx-border-color: transparent;");

            VBox mainContainer = new VBox(0);
            mainContainer.setStyle("-fx-background-color: #f8fafc;");
            mainContainer.setPrefWidth(950);

            // ================= EN-TÊTE AVEC LOGO =================
            VBox headerBox = new VBox(20);
            headerBox.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");

            HBox logoBox = new HBox(15);
            logoBox.setAlignment(Pos.CENTER_LEFT);

            // Logo EventFlow
            StackPane logoCircle = new StackPane();
            logoCircle.setStyle("-fx-background-color: linear-gradient(to bottom right, #3b82f6, #8b5cf6); " +
                    "-fx-background-radius: 30; -fx-min-width: 60; -fx-min-height: 60;");

            Label logoText = new Label("EF");
            logoText.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
            logoCircle.getChildren().add(logoText);

            VBox titleBox = new VBox(5);
            Label appName = new Label("EventFlow");
            appName.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            Label reportTitle = new Label("Rapport général des événements");
            reportTitle.setStyle("-fx-font-size: 16px; -fx-text-fill: #64748b;");

            titleBox.getChildren().addAll(appName, reportTitle);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label dateLabel = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            dateLabel.setStyle("-fx-background-color: #f1f5f9; -fx-padding: 8 15; -fx-background-radius: 20; " +
                    "-fx-text-fill: #475569; -fx-font-size: 14px;");

            logoBox.getChildren().addAll(logoCircle, titleBox, spacer, dateLabel);

            // Statistiques rapides
            HBox quickStats = new HBox(20);
            quickStats.setAlignment(Pos.CENTER_LEFT);
            quickStats.setPadding(new Insets(20, 0, 0, 0));

            VBox eventsQuickStat = createQuickStatBox("📅", String.valueOf(allEvents.size()), "Événements");
            VBox participantsQuickStat = createQuickStatBox("👥", formatNumber(totalParticipants), "Participants");
            VBox capacityQuickStat = createQuickStatBox("🎫", formatNumber(calculateTotalCapacity(allEvents)), "Places");

            quickStats.getChildren().addAll(eventsQuickStat, participantsQuickStat, capacityQuickStat);

            headerBox.getChildren().addAll(logoBox, quickStats);

            // ================= STATISTIQUES DÉTAILLÉES =================
            VBox statsBox = createEnhancedGlobalStatsBox(allEvents);

            // ================= LISTE DES ÉVÉNEMENTS =================
            VBox eventsBox = new VBox(20);
            eventsBox.setStyle("-fx-padding: 30; -fx-background-color: white;");

            Label eventsSectionTitle = new Label("📋 Détail des événements");
            eventsSectionTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            VBox eventsList = new VBox(15);

            if (allEvents.isEmpty()) {
                Label noEvents = new Label("Aucun événement trouvé");
                noEvents.setStyle("-fx-text-fill: #64748b; -fx-font-size: 16px; -fx-padding: 40;");
                noEvents.setAlignment(Pos.CENTER);
                eventsList.getChildren().add(noEvents);
            } else {
                for (Event event : allEvents) {
                    VBox eventCard = createEnhancedEventDetailCard(event);
                    eventsList.getChildren().add(eventCard);
                }
            }

            eventsBox.getChildren().addAll(eventsSectionTitle, eventsList);

            // ================= BOUTONS D'ACTION =================
            HBox buttonBox = new HBox(15);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);
            buttonBox.setStyle("-fx-padding: 20 30 30 30; -fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");

            Button exportPdfBtn = createActionButton("📥 Exporter PDF", "#3b82f6");
            exportPdfBtn.setOnAction(e -> exportToPDF(allEvents));

            Button printBtn = createActionButton("🖨️ Imprimer", "#10b981");
            printBtn.setOnAction(e -> printReport(mainScrollPane));

            Button closeBtn = createActionButton("Fermer", "#64748b");
            closeBtn.setOnAction(e -> reportStage.close());

            buttonBox.getChildren().addAll(exportPdfBtn, printBtn, closeBtn);

            // Assemblage final
            mainContainer.getChildren().addAll(headerBox, statsBox, eventsBox, buttonBox);
            mainScrollPane.setContent(mainContainer);

            Scene scene = new Scene(mainScrollPane);
            reportStage.setScene(scene);
            reportStage.show();

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la génération du rapport: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de générer le rapport: " + e.getMessage());
        }
    }

    private VBox createQuickStatBox(String icon, String value, String label) {
        VBox box = new VBox(5);
        box.setStyle("-fx-background-color: #f8fafc; -fx-padding: 15 25; -fx-background-radius: 12; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 1;");

        HBox iconBox = new HBox(10);
        iconBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px;");

        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        iconBox.getChildren().addAll(iconLabel, valueLabel);

        Label descLabel = new Label(label);
        descLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");

        box.getChildren().addAll(iconBox, descLabel);
        return box;
    }

    private VBox createEnhancedGlobalStatsBox(List<Event> events) {
        VBox box = new VBox(20);
        box.setStyle("-fx-padding: 30; -fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-width: 1 0;");

        Label statsTitle = new Label("📊 Statistiques globales");
        statsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        // Calcul des statistiques
        int draftCount = 0;
        int publishedCount = 0;
        int ongoingCount = 0;
        int completedCount = 0;

        LocalDateTime now = LocalDateTime.now();

        for (Event event : events) {
            String status = event.getStatus() != null ? event.getStatus().name() : "DRAFT";

            if ("DRAFT".equals(status)) {
                draftCount++;
            } else if ("PUBLISHED".equals(status)) {
                if (event.getStartDate() != null && event.getEndDate() != null) {
                    if (now.isAfter(event.getEndDate())) {
                        completedCount++;
                    } else if (now.isAfter(event.getStartDate()) && now.isBefore(event.getEndDate())) {
                        ongoingCount++;
                    } else {
                        publishedCount++;
                    }
                } else {
                    publishedCount++;
                }
            }
        }

        // Grille de statistiques
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(30);
        statsGrid.setVgap(20);
        statsGrid.setPadding(new Insets(20, 0, 20, 0));

        // Ligne 1
        statsGrid.add(createStatCard("Brouillons", String.valueOf(draftCount), "#f59e0b", "📝"), 0, 0);
        statsGrid.add(createStatCard("À venir", String.valueOf(publishedCount), "#3b82f6", "📅"), 1, 0);

        // Ligne 2
        statsGrid.add(createStatCard("En cours", String.valueOf(ongoingCount), "#10b981", "⚡"), 0, 1);
        statsGrid.add(createStatCard("Terminés", String.valueOf(completedCount), "#64748b", "✅"), 1, 1);

        box.getChildren().addAll(statsTitle, statsGrid);
        return box;
    }

    private HBox createStatCard(String label, String value, String color, String icon) {
        HBox card = new HBox(20);
        card.setStyle("-fx-background-color: #f8fafc; -fx-padding: 20; -fx-background-radius: 12; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-min-width: 250;");
        card.setAlignment(Pos.CENTER_LEFT);

        StackPane iconCircle = new StackPane();
        iconCircle.setStyle("-fx-background-color: " + color + "20; -fx-background-radius: 25; " +
                "-fx-min-width: 50; -fx-min-height: 50;");

        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px;");
        iconCircle.getChildren().add(iconLabel);

        VBox textBox = new VBox(5);
        Label valueLabel = new Label(value);
        valueLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        Label descLabel = new Label(label);
        descLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");

        textBox.getChildren().addAll(valueLabel, descLabel);

        card.getChildren().addAll(iconCircle, textBox);
        return card;
    }

    private VBox createEnhancedEventDetailCard(Event event) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: #f8fafc; -fx-padding: 20; -fx-background-radius: 12; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 1;");

        // En-tête de l'événement
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);

        String status = event.getStatus() != null ? event.getStatus().name() : "DRAFT";
        String statusColor = getStatusColor(status);
        String statusText = getDisplayStatus(status);

        Label statusBadge = new Label(statusText);
        statusBadge.setStyle("-fx-background-color: " + statusColor + "20; -fx-text-fill: " + statusColor + "; " +
                "-fx-padding: 5 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label titleLabel = new Label(event.getTitle());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label capacityLabel = new Label("🎫 " + event.getCapacity() + " places");
        capacityLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 14px;");

        header.getChildren().addAll(statusBadge, titleLabel, spacer, capacityLabel);

        // Informations détaillées
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(30);
        infoGrid.setVgap(10);
        infoGrid.setPadding(new Insets(10, 0, 10, 0));

        // Dates
        HBox datesBox = new HBox(10);
        datesBox.setAlignment(Pos.CENTER_LEFT);
        Label datesIcon = new Label("📅");
        datesIcon.setStyle("-fx-font-size: 16px;");
        Label datesText = new Label(formatEventDates(event));
        datesText.setStyle("-fx-text-fill: #334155;");
        datesBox.getChildren().addAll(datesIcon, datesText);
        infoGrid.add(datesBox, 0, 0);

        // Lieu
        HBox locationBox = new HBox(10);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        Label locationIcon = new Label("📍");
        locationIcon.setStyle("-fx-font-size: 16px;");
        Label locationText = new Label(event.getLocation() != null ? event.getLocation() : "Lieu non défini");
        locationText.setStyle("-fx-text-fill: #334155;");
        locationBox.getChildren().addAll(locationIcon, locationText);
        infoGrid.add(locationBox, 1, 0);

        // Prix
        HBox priceBox = new HBox(10);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceIcon = new Label("💰");
        priceIcon.setStyle("-fx-font-size: 16px;");
        String priceText = event.isFree() ? "Gratuit" : event.getTicketPrice() + " DT";
        Label priceLabel = new Label(priceText);
        priceLabel.setStyle("-fx-text-fill: #334155;");
        priceBox.getChildren().addAll(priceIcon, priceLabel);
        infoGrid.add(priceBox, 0, 1);

        // Catégorie
        HBox categoryBox = new HBox(10);
        categoryBox.setAlignment(Pos.CENTER_LEFT);
        Label categoryIcon = new Label("🏷️");
        categoryIcon.setStyle("-fx-font-size: 16px;");
        Label categoryText = new Label("Catégorie " + event.getCategoryId());
        categoryText.setStyle("-fx-text-fill: #334155;");
        categoryBox.getChildren().addAll(categoryIcon, categoryText);
        infoGrid.add(categoryBox, 1, 1);

        // Description si présente
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            VBox descriptionBox = new VBox(5);
            Label descTitle = new Label("Description:");
            descTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569;");

            Label descContent = new Label(event.getDescription());
            descContent.setWrapText(true);
            descContent.setStyle("-fx-text-fill: #334155; -fx-padding: 0 0 0 20;");

            descriptionBox.getChildren().addAll(descTitle, descContent);
            card.getChildren().add(descriptionBox);
        }

        // Participants
        VBox participantsBox = new VBox(10);
        participantsBox.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; " +
                "-fx-border-color: #e2e8f0; -fx-border-width: 1;");

        HBox participantsHeader = new HBox(10);
        participantsHeader.setAlignment(Pos.CENTER_LEFT);

        Label participantsIcon = new Label("👥");
        participantsIcon.setStyle("-fx-font-size: 18px;");

        int participantCount = getEventParticipantCount(event.getId());
        double fillRate = calculateEventFillRate(event);

        Label participantsTitle = new Label("Participants (" + participantCount + "/" + event.getCapacity() + ")");
        participantsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");

        Label fillRateLabel = new Label(String.format("%.1f%% de remplissage", fillRate));
        fillRateLabel.setStyle("-fx-text-fill: " + (fillRate > 80 ? "#22c55e" : (fillRate > 50 ? "#eab308" : "#ef4444")) + "; " +
                "-fx-font-size: 13px; -fx-font-weight: bold;");

        Region participantsSpacer = new Region();
        HBox.setHgrow(participantsSpacer, Priority.ALWAYS);

        participantsHeader.getChildren().addAll(participantsIcon, participantsTitle, participantsSpacer, fillRateLabel);

        // Barre de progression
        ProgressBar progressBar = new ProgressBar(fillRate / 100);
        progressBar.setPrefWidth(200);
        progressBar.setStyle("-fx-accent: " + (fillRate > 80 ? "#22c55e" : (fillRate > 50 ? "#eab308" : "#ef4444")) + ";");

        HBox progressBox = new HBox(15);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        progressBox.getChildren().addAll(progressBar, new Label(String.format("%.1f%%", fillRate)));

        participantsBox.getChildren().addAll(participantsHeader, progressBox);

        // Assemblage de la carte
        card.getChildren().add(header);
        card.getChildren().add(infoGrid);
        card.getChildren().add(participantsBox);

        return card;
    }

    private int calculateTotalCapacity(List<Event> events) {
        return events.stream().mapToInt(Event::getCapacity).sum();
    }

    private Button createActionButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;");

        button.setOnMouseEntered(e ->
                button.setStyle("-fx-background-color: derive(" + color + ", -10%); -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;"));

        button.setOnMouseExited(e ->
                button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-size: 14px;"));

        return button;
    }

    private String formatEventDates(Event event) {
        if (event.getStartDate() == null) return "Dates non définies";

        String start = event.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        if (event.getEndDate() == null) return start;

        String end = event.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        return start + " - " + end;
    }

    private String getStatusDisplay(Event event) {
        if (event.getStatus() == null) return "Non défini";

        LocalDateTime now = LocalDateTime.now();
        String status = event.getStatus().name();

        if ("DRAFT".equals(status)) {
            return "Brouillon";
        } else if ("PUBLISHED".equals(status)) {
            if (event.getStartDate() != null && event.getEndDate() != null) {
                if (now.isAfter(event.getEndDate())) {
                    return "Terminé";
                } else if (now.isAfter(event.getStartDate()) && now.isBefore(event.getEndDate())) {
                    return "En cours";
                }
            }
            return "Publié à venir";
        }
        return status;
    }

    private VBox getEventParticipantsList(int eventId) {
        VBox listBox = new VBox(5);
        listBox.setPadding(new Insets(5, 0, 5, 15));

        try {
            int participantCount = getEventParticipantCount(eventId);

            if (participantCount > 0) {
                for (int i = 1; i <= Math.min(participantCount, 5); i++) {
                    HBox participantRow = new HBox(10);
                    participantRow.setAlignment(Pos.CENTER_LEFT);

                    Label nameLabel = new Label("• Participant " + i);
                    nameLabel.setStyle("-fx-text-fill: #334155;");

                    Label emailLabel = new Label("participant" + i + "@email.com");
                    emailLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

                    participantRow.getChildren().addAll(nameLabel, emailLabel);
                    listBox.getChildren().add(participantRow);
                }

                if (participantCount > 5) {
                    Label moreLabel = new Label("... et " + (participantCount - 5) + " autres");
                    moreLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
                    listBox.getChildren().add(moreLabel);
                }
            } else {
                Label noParticipants = new Label("Aucun participant pour le moment");
                noParticipants.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
                listBox.getChildren().add(noParticipants);
            }
        } catch (Exception e) {
            Label errorLabel = new Label("Erreur chargement participants");
            errorLabel.setStyle("-fx-text-fill: #ef4444;");
            listBox.getChildren().add(errorLabel);
        }

        return listBox;
    }

    private double calculateEventFillRate(Event event) {
        if (event.getCapacity() <= 0) return 0;

        int participantCount = getEventParticipantCount(event.getId());
        return (double) participantCount / event.getCapacity() * 100;
    }

    private HBox createInfoRow(String label, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold; -fx-text-fill: #475569; -fx-min-width: 100;");

        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #334155;");
        valueNode.setWrapText(true);

        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }

    // ================= MÉTHODE D'EXPORT PDF UNIQUE =================
    private void exportToPDF(List<Event> events) {
        try {
            System.out.println("📄 Début de l'export PDF...");

            // Créer le dossier Rapports s'il n'existe pas
            String userHome = System.getProperty("user.home");
            String reportsDir = userHome + File.separator + "EventFlow_Rapports";
            File directory = new File(reportsDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Générer le nom du fichier avec timestamp
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = reportsDir + File.separator + "rapport_evenements_" + timestamp + ".pdf";

            // Créer le document PDF
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(fileName));
            document.open();

            // Police pour supporter l'UTF-8
            BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.EMBEDDED);
            Font titleFont = new Font(baseFont, 24, Font.BOLD, BaseColor.BLACK);
            Font subtitleFont = new Font(baseFont, 16, Font.BOLD, BaseColor.DARK_GRAY);
            Font normalFont = new Font(baseFont, 12, Font.NORMAL, BaseColor.BLACK);
            Font boldFont = new Font(baseFont, 12, Font.BOLD, BaseColor.BLACK);
            Font smallFont = new Font(baseFont, 10, Font.NORMAL, BaseColor.GRAY);

            // ========== EN-TÊTE ==========
            Paragraph title = new Paragraph("EventFlow - Rapport des événements", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            Paragraph datePara = new Paragraph("Généré le " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), smallFont);
            datePara.setAlignment(Element.ALIGN_RIGHT);
            datePara.setSpacingAfter(30);
            document.add(datePara);

            // ========== STATISTIQUES GLOBALES ==========
            Paragraph statsTitle = new Paragraph("📊 Statistiques globales", subtitleFont);
            statsTitle.setSpacingAfter(15);
            document.add(statsTitle);

            // Calculer les stats
            int totalEvents = events.size();
            int totalParticipants = 0;
            try {
                totalParticipants = userService.getTotalParticipantsCount();
            } catch (Exception e) {
                totalParticipants = 0;
            }

            int draftCount = 0, publishedCount = 0, ongoingCount = 0, completedCount = 0;
            LocalDateTime now = LocalDateTime.now();

            for (Event event : events) {
                String status = event.getStatus() != null ? event.getStatus().name() : "DRAFT";
                if ("DRAFT".equals(status)) {
                    draftCount++;
                } else if ("PUBLISHED".equals(status)) {
                    if (event.getStartDate() != null && event.getEndDate() != null) {
                        if (now.isAfter(event.getEndDate())) {
                            completedCount++;
                        } else if (now.isAfter(event.getStartDate()) && now.isBefore(event.getEndDate())) {
                            ongoingCount++;
                        } else {
                            publishedCount++;
                        }
                    } else {
                        publishedCount++;
                    }
                }
            }

            // Créer un tableau pour les stats
            PdfPTable statsTable = new PdfPTable(2);
            statsTable.setWidthPercentage(80);
            statsTable.setHorizontalAlignment(Element.ALIGN_CENTER);
            statsTable.setSpacingAfter(20);

            addStatCell(statsTable, "Total événements", String.valueOf(totalEvents), boldFont, normalFont);
            addStatCell(statsTable, "Total participants", String.valueOf(totalParticipants), boldFont, normalFont);
            addStatCell(statsTable, "Brouillons", String.valueOf(draftCount), boldFont, normalFont);
            addStatCell(statsTable, "À venir", String.valueOf(publishedCount), boldFont, normalFont);
            addStatCell(statsTable, "En cours", String.valueOf(ongoingCount), boldFont, normalFont);
            addStatCell(statsTable, "Terminés", String.valueOf(completedCount), boldFont, normalFont);

            document.add(statsTable);

            // ========== DÉTAIL DES ÉVÉNEMENTS ==========
            Paragraph eventsTitle = new Paragraph("📋 Détail des événements", subtitleFont);
            eventsTitle.setSpacingBefore(20);
            eventsTitle.setSpacingAfter(15);
            document.add(eventsTitle);

            for (Event event : events) {
                // Titre de l'événement
                Paragraph eventName = new Paragraph(event.getTitle(), boldFont);
                eventName.setSpacingBefore(10);
                eventName.setSpacingAfter(5);
                document.add(eventName);

                // Informations de l'événement
                PdfPTable eventTable = new PdfPTable(2);
                eventTable.setWidthPercentage(90);
                eventTable.setSpacingAfter(10);

                addInfoRow(eventTable, "Statut:", getEventStatus(event), normalFont);
                addInfoRow(eventTable, "Dates:", formatEventDates(event), normalFont);
                addInfoRow(eventTable, "Lieu:", event.getLocation() != null ? event.getLocation() : "Non défini", normalFont);
                addInfoRow(eventTable, "Capacité:", String.valueOf(event.getCapacity()) + " places", normalFont);
                addInfoRow(eventTable, "Prix:", event.isFree() ? "Gratuit" : event.getTicketPrice() + " DT", normalFont);

                document.add(eventTable);

                // Description si présente
                if (event.getDescription() != null && !event.getDescription().isEmpty()) {
                    Paragraph desc = new Paragraph("Description: " + event.getDescription(), smallFont);
                    desc.setIndentationLeft(20);
                    desc.setSpacingAfter(5);
                    document.add(desc);
                }

                // Séparateur entre événements
                document.add(new Paragraph(" "));
            }

            // ========== PIED DE PAGE ==========
            Paragraph footer = new Paragraph("EventFlow - Système de gestion événementielle\n" +
                    "© 2024 Tous droits réservés", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(30);
            document.add(footer);

            // Fermer le document
            document.close();

            System.out.println("✅ PDF généré avec succès: " + fileName);

            // Afficher une boîte de dialogue de succès avec option d'ouverture
            showPDFSuccessDialog(fileName);

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'export PDF: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'exporter le rapport PDF: " + e.getMessage());
        }
    }

    // Méthodes helper pour le PDF
    private void addStatCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private void addInfoRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, font));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, font));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    private String getEventStatus(Event event) {
        if (event.getStatus() == null) return "Non défini";

        LocalDateTime now = LocalDateTime.now();
        String status = event.getStatus().name();

        if ("DRAFT".equals(status)) {
            return "Brouillon";
        } else if ("PUBLISHED".equals(status)) {
            if (event.getStartDate() != null && event.getEndDate() != null) {
                if (now.isAfter(event.getEndDate())) {
                    return "Terminé";
                } else if (now.isAfter(event.getStartDate()) && now.isBefore(event.getEndDate())) {
                    return "En cours";
                }
            }
            return "Publié à venir";
        }
        return status;
    }

    private void showPDFSuccessDialog(String filePath) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Export PDF réussi");
        alert.setHeaderText("Le rapport a été généré avec succès !");
        alert.setContentText("Fichier: " + filePath);

        ButtonType openButton = new ButtonType("Ouvrir le dossier", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeButton = new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(openButton, closeButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == openButton) {
            try {
                Desktop.getDesktop().open(new File(filePath).getParentFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void printReport(Node node) {
        showAlert("Impression", "Fonction d'impression à implémenter");
    }

    private String formatNumber(int number) {
        if (number >= 1000) {
            return String.format("%.1fk", number / 1000.0);
        }
        return String.valueOf(number);
    }

    private void showErrorPlaceholders() {
        if (totalEventsLabel != null) totalEventsLabel.setText("0");
        if (totalParticipantsLabel != null) totalParticipantsLabel.setText("0");
        if (participationRateLabel != null) participationRateLabel.setText("0%");
        if (eventsEvolutionLabel != null) eventsEvolutionLabel.setText("Erreur");
        if (participantsEvolutionLabel != null) participantsEvolutionLabel.setText("Erreur");
    }

    private void showEventDetails(Event event) {
        System.out.println("👁️ Détails de l'événement: " + event.getTitle());
        if (mainController != null) {
            mainController.showEventView(event);
        } else {
            showAlert("Détails de l'événement",
                    "Titre: " + event.getTitle() + "\n" +
                            "Date: " + (event.getStartDate() != null ? event.getStartDate() : "Non définie") + "\n" +
                            "Lieu: " + (event.getLocation() != null ? event.getLocation() : "Non défini") + "\n" +
                            "Capacité: " + event.getCapacity() + " places");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void cleanup() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    public void goToLogin(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pidev/fxml/auth/login.fxml"));
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void goToGestionUser(ActionEvent actionEvent) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pidev/fxml/user/user.fxml"));
            Stage stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gestion des utilisateurs");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void goToProfil(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pidev/fxml/user/profil.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Profil");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void goToDashboard(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pidev/fxml/dashboard/dashboard.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Dashboard");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class EventStats {
        int planned;
        int ongoing;
        int completed;

        EventStats(int planned, int ongoing, int completed) {
            this.planned = planned;
            this.ongoing = ongoing;
            this.completed = completed;
        }
    }
}