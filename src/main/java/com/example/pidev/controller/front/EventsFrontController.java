package com.example.pidev.controller.front;

import com.example.pidev.HelloApplication;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.event.EventCategory;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.event.EventCategoryService;
import com.example.pidev.utils.UserSession;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la page publique des événements (front office)
 * @author Ons Abdesslem
 */
public class EventsFrontController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> dateFilter;
    @FXML private ComboBox<String> priceFilter;
    @FXML private FlowPane eventsGrid;
    @FXML private Label resultCountLabel;
    @FXML private VBox noResultsMessage;

    private EventService eventService;
    private EventCategoryService categoryService;
    private List<Event> allEvents;
    private List<Event> filteredEvents;
    private List<EventCategory> allCategories;

    @FXML
    public void initialize() {
        System.out.println("✅ EventsFrontController initialisé");

        eventService = new EventService();
        categoryService = new EventCategoryService();

        setupFilters();
        loadEvents();
    }

    /**
     * Configure les filtres
     */
    private void setupFilters() {
        // Filtre par date
        dateFilter.getItems().addAll(
                "Toutes les dates",
                "Aujourd'hui",
                "Cette semaine",
                "Ce mois-ci",
                "À venir"
        );
        dateFilter.setValue("Toutes les dates");

        // Filtre par prix
        priceFilter.getItems().addAll(
                "Tous les prix",
                "Gratuit",
                "Payant"
        );
        priceFilter.setValue("Tous les prix");

        // Listeners pour filtrage automatique
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        dateFilter.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        priceFilter.valueProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    /**
     * Charge tous les événements depuis la base de données
     */
    private void loadEvents() {
        try {
            // Charger tous les événements (sans filtre de statut)
            allEvents = eventService.getAllEvents();

            // Charger les catégories pour le filtre
            allCategories = categoryService.getAllCategories();
            categoryFilter.getItems().clear();
            categoryFilter.getItems().add("Toutes les catégories");
            for (EventCategory cat : allCategories) {
                categoryFilter.getItems().add(cat.getName());
            }
            categoryFilter.setValue("Toutes les catégories");

            // Afficher tous les événements
            filteredEvents = allEvents;
            displayEvents(filteredEvents);

            System.out.println("✅ " + allEvents.size() + " événements chargés");

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement événements: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les événements", Alert.AlertType.ERROR);
        }
    }

    /**
     * Applique les filtres de recherche
     */
    private void applyFilters() {
        if (allEvents == null || allEvents.isEmpty()) {
            return;
        }

        String searchText = searchField.getText().toLowerCase().trim();
        String category = categoryFilter.getValue();
        String date = dateFilter.getValue();
        String price = priceFilter.getValue();

        LocalDateTime now = LocalDateTime.now();

        filteredEvents = allEvents.stream()
                .filter(event -> {
                    // Filtre recherche
                    boolean matchSearch = searchText.isEmpty() ||
                            event.getTitle().toLowerCase().contains(searchText) ||
                            (event.getDescription() != null && event.getDescription().toLowerCase().contains(searchText)) ||
                            (event.getLocation() != null && event.getLocation().toLowerCase().contains(searchText));

                    // Filtre catégorie
                    boolean matchCategory = category == null || "Toutes les catégories".equals(category);
                    if (!matchCategory) {
                        String eventCategoryName = getCategoryName(event.getCategoryId());
                        matchCategory = eventCategoryName.equals(category);
                    }

                    // Filtre date
                    boolean matchDate = true;
                    if (date != null && event.getStartDate() != null) {
                        switch (date) {
                            case "Aujourd'hui":
                                matchDate = event.getStartDate().toLocalDate().equals(now.toLocalDate());
                                break;
                            case "Cette semaine":
                                matchDate = event.getStartDate().isAfter(now) &&
                                        event.getStartDate().isBefore(now.plusWeeks(1));
                                break;
                            case "Ce mois-ci":
                                matchDate = event.getStartDate().getMonth().equals(now.getMonth()) &&
                                        event.getStartDate().getYear() == now.getYear();
                                break;
                            case "À venir":
                                matchDate = event.getStartDate().isAfter(now);
                                break;
                        }
                    }

                    // Filtre prix
                    boolean matchPrice = true;
                    if (price != null) {
                        switch (price) {
                            case "Gratuit":
                                matchPrice = event.isFree();
                                break;
                            case "Payant":
                                matchPrice = !event.isFree();
                                break;
                        }
                    }

                    return matchSearch && matchCategory && matchDate && matchPrice;
                })
                .collect(Collectors.toList());

        displayEvents(filteredEvents);
    }

    /**
     * Affiche les événements dans la grille
     */
    private void displayEvents(List<Event> events) {
        eventsGrid.getChildren().clear();

        if (events == null || events.isEmpty()) {
            noResultsMessage.setVisible(true);
            noResultsMessage.setManaged(true);
            resultCountLabel.setText("0 événement(s) trouvé(s)");
            return;
        }

        noResultsMessage.setVisible(false);
        noResultsMessage.setManaged(false);
        resultCountLabel.setText(events.size() + " événement(s) trouvé(s)");

        for (Event event : events) {
            VBox card = createEventCard(event);
            eventsGrid.getChildren().add(card);
        }
    }

    /**
     * Crée une carte pour un événement
     */
    private VBox createEventCard(Event event) {
        VBox card = new VBox(15);
        card.setPrefWidth(360);
        card.setMaxWidth(360);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 0; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");

        // Effet hover
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 0; " +
                        "-fx-effect: dropshadow(gaussian, rgba(13,71,161,0.2), 15, 0, 0, 4); -fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 0; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);"
        ));

        // Image
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(200);
        imageContainer.setStyle("-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%); " +
                "-fx-background-radius: 16 16 0 0;");

        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            try {
                ImageView imageView = new ImageView(new Image(event.getImageUrl()));
                imageView.setFitWidth(360);
                imageView.setFitHeight(200);
                imageView.setPreserveRatio(false);
                imageContainer.getChildren().add(imageView);
            } catch (Exception e) {
                Label placeholder = new Label("📅");
                placeholder.setStyle("-fx-font-size: 60px;");
                imageContainer.getChildren().add(placeholder);
            }
        } else {
            Label placeholder = new Label("📅");
            placeholder.setStyle("-fx-font-size: 60px;");
            imageContainer.getChildren().add(placeholder);
        }

        // Badge catégorie (en haut à droite de l'image)
        String categoryName = getCategoryName(event.getCategoryId());
        Label categoryBadge = new Label(categoryName);
        categoryBadge.setStyle("-fx-background-color: rgba(13,71,161,0.9); -fx-text-fill: white; " +
                "-fx-padding: 6 15; -fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: bold;");
        StackPane.setAlignment(categoryBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(categoryBadge, new Insets(15, 15, 0, 0));
        imageContainer.getChildren().add(categoryBadge);

        // Contenu
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        // Titre
        Label titleLabel = new Label(event.getTitle());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0A1929; -fx-wrap-text: true;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(320);

        // Date
        HBox dateBox = new HBox(8);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Label dateIcon = new Label("📅");
        dateIcon.setStyle("-fx-font-size: 16px;");
        Label dateLabel = new Label(event.getFormattedStartDate());
        dateLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        dateBox.getChildren().addAll(dateIcon, dateLabel);

        // Lieu
        HBox locationBox = new HBox(8);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        Label locationIcon = new Label("📍");
        locationIcon.setStyle("-fx-font-size: 16px;");
        Label locationLabel = new Label(event.getLocation() != null ?
                (event.getLocation().length() > 40 ? event.getLocation().substring(0, 37) + "..." : event.getLocation())
                : "Lieu non spécifié");
        locationLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        locationBox.getChildren().addAll(locationIcon, locationLabel);

        // Prix
        HBox priceBox = new HBox(8);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceIcon = new Label("💰");
        priceIcon.setStyle("-fx-font-size: 16px;");
        Label priceLabel = new Label(event.getPriceDisplay());
        priceLabel.setStyle("-fx-text-fill: " + (event.isFree() ? "#10b981" : "#0D47A1") + "; " +
                "-fx-font-size: 16px; -fx-font-weight: bold;");
        priceBox.getChildren().addAll(priceIcon, priceLabel);

        // Boutons d'action
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setPadding(new Insets(10, 0, 0, 0));

        Button viewDetailsBtn = new Button("Voir détails");
        viewDetailsBtn.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #0D47A1; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-padding: 10 20; -fx-cursor: hand;");
        viewDetailsBtn.setOnAction(e -> handleViewDetails(event));

        Button participateBtn = new Button("Participer");
        participateBtn.setStyle("-fx-background-color: #0D47A1; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-padding: 10 25; -fx-cursor: hand;");
        participateBtn.setOnAction(e -> handleParticipate(event));

        buttonsBox.getChildren().addAll(viewDetailsBtn, participateBtn);

        content.getChildren().addAll(titleLabel, dateBox, locationBox, priceBox, buttonsBox);

        card.getChildren().addAll(imageContainer, content);

        return card;
    }

    /**
     * Retourne le nom de la catégorie
     */
    private String getCategoryName(int categoryId) {
        if (allCategories != null) {
            for (EventCategory cat : allCategories) {
                if (cat.getId() == categoryId) {
                    return cat.getName();
                }
            }
        }
        return "Autre";
    }

    /**
     * Affiche les détails d'un événement
     */
    @FXML
    private void handleViewDetails(Event event) {
        System.out.println("👁️ Voir détails de: " + event.getTitle());
        HelloApplication.loadEventDetailsPage(event);
    }

    /**
     * Gère la participation à un événement
     */
    @FXML
    private void handleParticipate(Event event) {
        System.out.println("🎫 Participer à: " + event.getTitle());

        // Vérifier si l'utilisateur est connecté
        UserSession session = UserSession.getInstance();
        if (session.getCurrentUser() == null) {
            // Sauvegarder l'ID de l'événement pour y participer après connexion
            session.setPendingEventId(event.getId());

            // Rediriger vers la page de connexion
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Connexion requise");
            alert.setHeaderText("Vous devez être connecté");
            alert.setContentText("Pour participer à cet événement, veuillez vous connecter ou créer un compte.\n\n" +
                    "Après connexion, votre participation sera automatiquement enregistrée.");

            ButtonType loginBtn = new ButtonType("Se connecter");
            ButtonType signupBtn = new ButtonType("S'inscrire");
            ButtonType cancelBtn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

            alert.getButtonTypes().setAll(loginBtn, signupBtn, cancelBtn);

            alert.showAndWait().ifPresent(response -> {
                if (response == loginBtn) {
                    HelloApplication.loadLoginPage();
                } else if (response == signupBtn) {
                    HelloApplication.loadSignupPage();
                } else {
                    // Annuler : nettoyer le pendingEventId
                    session.clearPendingEventId();
                }
            });
            return;
        }

        // Utilisateur connecté : créer un ticket immédiatement
        createTicketForEvent(event.getId(), event.getTitle());
    }

    /**
     * Crée un ticket pour un événement
     */
    private void createTicketForEvent(int eventId, String eventTitle) {
        try {
            com.example.pidev.service.event.EventTicketService ticketService =
                    new com.example.pidev.service.event.EventTicketService();

            int userId = UserSession.getInstance().getCurrentUser().getId_User();

            // Créer le ticket dans la base de données
            com.example.pidev.model.event.EventTicket ticket = ticketService.createTicket(eventId, userId);

            if (ticket != null) {
                // Succès : afficher le ticket généré
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("✅ Participation confirmée");
                alert.setHeaderText("Vous participez à l'événement !");
                alert.setContentText(
                        "Événement : " + eventTitle + "\n\n" +
                                "Votre ticket : " + ticket.getTicketCode() + "\n\n" +
                                "Un email de confirmation vous sera envoyé.\n" +
                                "Conservez votre code de ticket pour accéder à l'événement."
                );

                // Style personnalisé
                alert.getDialogPane().setStyle(
                        "-fx-background-color: white; " +
                                "-fx-font-size: 14px;"
                );

                alert.showAndWait();

                System.out.println("✅ Ticket créé avec succès: " + ticket.getTicketCode());

            } else {
                // Erreur de création
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Impossible de créer le ticket");
                alert.setContentText(
                        "Une erreur est survenue lors de la création de votre ticket.\n" +
                                "Veuillez réessayer plus tard ou contacter le support."
                );
                alert.showAndWait();

                System.err.println("❌ Échec de la création du ticket");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la participation: " + e.getMessage());
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Une erreur est survenue");
            alert.setContentText("Impossible de traiter votre demande. Veuillez réessayer.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleSearch() {
        applyFilters();
    }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        categoryFilter.setValue("Toutes les catégories");
        dateFilter.setValue("Toutes les dates");
        priceFilter.setValue("Tous les prix");
        applyFilters();
    }

    @FXML
    private void handleGoToHome() {
        HelloApplication.loadLandingPage();
    }

    @FXML
    private void handleGoToContact() {
        HelloApplication.loadLandingPage();
    }

    @FXML
    private void handleLogin() {
        HelloApplication.loadLoginPage();
    }

    @FXML
    private void handleSignup() {
        HelloApplication.loadSignupPage();
    }

    @FXML
    private void handleMyTickets() {
        if (UserSession.getInstance().getCurrentUser() != null) {
            HelloApplication.loadMyTicketsPage();
        } else {
            showAlert("Accès refusé", "Vous devez être connecté pour voir vos billets", Alert.AlertType.WARNING);
            handleLogin();
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

