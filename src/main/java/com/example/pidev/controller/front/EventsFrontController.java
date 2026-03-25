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
 * ContrÃ´leur pour la page publique des Ã©vÃ©nements (front office)
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
        System.out.println("âœ… EventsFrontController initialisÃ©");

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
                "Ã€ venir"
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
     * Charge tous les Ã©vÃ©nements depuis la base de donnÃ©es
     */
    private void loadEvents() {
        try {
            // Charger tous les Ã©vÃ©nements (sans filtre de statut)
            allEvents = eventService.getAllEvents();

            // Charger les catÃ©gories pour le filtre
            allCategories = categoryService.getAllCategories();
            categoryFilter.getItems().clear();
            categoryFilter.getItems().add("Toutes les catÃ©gories");
            for (EventCategory cat : allCategories) {
                categoryFilter.getItems().add(cat.getName());
            }
            categoryFilter.setValue("Toutes les catÃ©gories");

            // Afficher tous les Ã©vÃ©nements
            filteredEvents = allEvents;
            displayEvents(filteredEvents);

            System.out.println("âœ… " + allEvents.size() + " Ã©vÃ©nements chargÃ©s");

        } catch (Exception e) {
            System.err.println("âŒ Erreur chargement Ã©vÃ©nements: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les Ã©vÃ©nements", Alert.AlertType.ERROR);
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

                    // Filtre catÃ©gorie
                    boolean matchCategory = category == null || "Toutes les catÃ©gories".equals(category);
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
                            case "Ã€ venir":
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
     * Affiche les Ã©vÃ©nements dans la grille
     */
    private void displayEvents(List<Event> events) {
        eventsGrid.getChildren().clear();

        if (events == null || events.isEmpty()) {
            noResultsMessage.setVisible(true);
            noResultsMessage.setManaged(true);
            resultCountLabel.setText("0 Ã©vÃ©nement(s) trouvÃ©(s)");
            return;
        }

        noResultsMessage.setVisible(false);
        noResultsMessage.setManaged(false);
        resultCountLabel.setText(events.size() + " Ã©vÃ©nement(s) trouvÃ©(s)");

        for (Event event : events) {
            VBox card = createEventCard(event);
            eventsGrid.getChildren().add(card);
        }
    }

    /**
     * CrÃ©e une carte pour un Ã©vÃ©nement avec couleurs dynamiques de la BD
     */
    private VBox createEventCard(Event event) {
        // RÃ©cupÃ©rer la catÃ©gorie et ses couleurs/icÃ´nes depuis la BD
        EventCategory category = getCategoryById(event.getCategoryId());
        String categoryColor = sanitizeHexColor(category != null ? category.getColor() : null, "#6A1B9A");
        String categoryEmoji = (category != null && category.getIcon() != null)
                ? category.getIcon()
                : "ðŸ“Œ"; // Ã‰pingle par dÃ©faut
        String categoryName = (category != null) ? category.getName() : "Autre";

        VBox card = new VBox(15);
        card.setPrefWidth(360);
        card.setMaxWidth(360);

        // Style de base avec bordure top colorÃ©e (vraie couleur de la catÃ©gorie)
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 0; " +
                "-fx-border-color: " + categoryColor + "; -fx-border-width: 4 0 0 0; " +
                "-fx-border-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");

        // Effet hover avec la couleur rÃ©elle de la catÃ©gorie
        int[] rgb = toRgb(categoryColor);

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 0; " +
                        "-fx-border-color: " + categoryColor + "; -fx-border-width: 4 0 0 0; " +
                        "-fx-border-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(" +
                        rgb[0] + "," + rgb[1] + "," + rgb[2] + ",0.3), 15, 0, 0, 4); " +
                        "-fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 0; " +
                        "-fx-border-color: " + categoryColor + "; -fx-border-width: 4 0 0 0; " +
                        "-fx-border-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);"
        ));

        // Image container avec gradient basÃ© sur la vraie couleur
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(200);

        String gradientColor1 = categoryColor;
        String gradientColor2 = shiftColorBrightness(categoryColor, -30);
        imageContainer.setStyle("-fx-background-color: linear-gradient(from 0% 0% to 100% 100%, " +
                gradientColor1 + " 0%, " + gradientColor2 + " 100%); " +
                "-fx-background-radius: 16 16 0 0;");

        if (event.getImageUrl() != null && !event.getImageUrl().isEmpty()) {
            try {
                ImageView imageView = new ImageView(new Image(event.getImageUrl()));
                imageView.setFitWidth(360);
                imageView.setFitHeight(200);
                imageView.setPreserveRatio(false);
                imageContainer.getChildren().add(imageView);
            } catch (Exception e) {
                Label placeholder = new Label(categoryEmoji);
                placeholder.setStyle("-fx-font-size: 80px; -fx-text-fill: rgba(255,255,255,0.3);");
                imageContainer.getChildren().add(placeholder);
            }
        } else {
            Label placeholder = new Label(categoryEmoji);
            placeholder.setStyle("-fx-font-size: 80px; -fx-text-fill: rgba(255,255,255,0.3);");
            imageContainer.getChildren().add(placeholder);
        }

        // Badge catÃ©gorie avec la vraie couleur et l'emoji de la BD
        Label categoryBadge = new Label(categoryEmoji + " " + categoryName);
        categoryBadge.setStyle("-fx-background-color: " + categoryColor + "; -fx-text-fill: white; " +
                "-fx-padding: 8 15; -fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: bold;");
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
        Label dateIcon = new Label("ðŸ“…");
        dateIcon.setStyle("-fx-font-size: 16px;");
        Label dateLabel = new Label(event.getFormattedStartDate());
        dateLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        dateBox.getChildren().addAll(dateIcon, dateLabel);

        // Lieu
        HBox locationBox = new HBox(8);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        Label locationIcon = new Label("ðŸ“");
        locationIcon.setStyle("-fx-font-size: 16px;");
        Label locationLabel = new Label(event.getLocation() != null ?
                (event.getLocation().length() > 40 ? event.getLocation().substring(0, 37) + "..." : event.getLocation())
                : "Lieu non spÃ©cifiÃ©");
        locationLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        locationBox.getChildren().addAll(locationIcon, locationLabel);

        // Prix avec la couleur de la catÃ©gorie
        HBox priceBox = new HBox(8);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceIcon = new Label("ðŸ’°");
        priceIcon.setStyle("-fx-font-size: 16px;");
        Label priceLabel = new Label(event.getPriceDisplay());
        priceLabel.setStyle("-fx-text-fill: " + (event.isFree() ? "#10b981" : categoryColor) + "; " +
                "-fx-font-size: 16px; -fx-font-weight: bold;");
        priceBox.getChildren().addAll(priceIcon, priceLabel);

        // Boutons d'action
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setPadding(new Insets(10, 0, 0, 0));

        Button viewDetailsBtn = new Button("Voir dÃ©tails");
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
     * Retourne le nom de la catÃ©gorie
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
     * Affiche les dÃ©tails d'un Ã©vÃ©nement
     */
    @FXML
    private void handleViewDetails(Event event) {
        System.out.println("ðŸ‘ï¸ Voir dÃ©tails de: " + event.getTitle());
        HelloApplication.loadEventDetailsPage(event);
    }

    /**
     * GÃ¨re la participation Ã  un Ã©vÃ©nement
     */
    @FXML
    private void handleParticipate(Event event) {
        System.out.println("ðŸŽ« Participer Ã : " + event.getTitle());

        // VÃ©rifier si l'utilisateur est connectÃ©
        UserSession session = UserSession.getInstance();
        if (session.getCurrentUser() == null) {
            // Sauvegarder l'ID de l'Ã©vÃ©nement pour y participer aprÃ¨s connexion
            session.setPendingEventId(event.getId());

            // Rediriger vers la page de connexion
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Connexion requise");
            alert.setHeaderText("Vous devez Ãªtre connectÃ©");
            alert.setContentText("Pour participer Ã  cet Ã©vÃ©nement, veuillez vous connecter ou crÃ©er un compte.\n\n" +
                    "AprÃ¨s connexion, votre participation sera automatiquement enregistrÃ©e.");

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

        // Utilisateur connectÃ© : crÃ©er un ticket immÃ©diatement
        createTicketForEvent(event.getId(), event.getTitle());
    }

    /**
     * CrÃ©e un ticket pour un Ã©vÃ©nement
     */
    private void createTicketForEvent(int eventId, String eventTitle) {
        try {
            com.example.pidev.service.event.EventTicketService ticketService =
                    new com.example.pidev.service.event.EventTicketService();

            int userId = UserSession.getInstance().getCurrentUser().getId_User();

            // CrÃ©er le ticket dans la base de donnÃ©es
            com.example.pidev.model.event.EventTicket ticket = ticketService.createTicket(eventId, userId);

            if (ticket != null) {
                // SuccÃ¨s : afficher le ticket gÃ©nÃ©rÃ©
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("âœ… Participation confirmÃ©e");
                alert.setHeaderText("Vous participez Ã  l'Ã©vÃ©nement !");
                alert.setContentText(
                        "Ã‰vÃ©nement : " + eventTitle + "\n\n" +
                                "Votre ticket : " + ticket.getTicketCode() + "\n\n" +
                                "Un email de confirmation vous sera envoyÃ©.\n" +
                                "Conservez votre code de ticket pour accÃ©der Ã  l'Ã©vÃ©nement."
                );

                // Style personnalisÃ©
                alert.getDialogPane().setStyle(
                        "-fx-background-color: white; " +
                                "-fx-font-size: 14px;"
                );

                alert.showAndWait();

                System.out.println("âœ… Ticket crÃ©Ã© avec succÃ¨s: " + ticket.getTicketCode());

            } else {
                // Erreur de crÃ©ation
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Impossible de crÃ©er le ticket");
                alert.setContentText(
                        "Une erreur est survenue lors de la crÃ©ation de votre ticket.\n" +
                                "Veuillez rÃ©essayer plus tard ou contacter le support."
                );
                alert.showAndWait();

                System.err.println("âŒ Ã‰chec de la crÃ©ation du ticket");
            }

        } catch (Exception e) {
            System.err.println("âŒ Erreur lors de la participation: " + e.getMessage());
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Une erreur est survenue");
            alert.setContentText("Impossible de traiter votre demande. Veuillez rÃ©essayer.");
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
        categoryFilter.setValue("Toutes les catÃ©gories");
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
    private void handleGoToFeatures() {
        // Redirection vers la section fonctionnalitÃ©s (landing page)
        HelloApplication.loadLandingPage();
    }

    @FXML
    private void handleGoToFeedback() {
        // Redirection vers la section feedback (landing page)
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
            showAlert("AccÃ¨s refusÃ©", "Vous devez Ãªtre connectÃ© pour voir vos billets", Alert.AlertType.WARNING);
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

    /**
     * Classe interne pour stocker le style d'une catÃ©gorie (couleur + emoji)
     */
    private static class CategoryStyle {
        String color;
        String emoji;

        CategoryStyle(String color, String emoji) {
            this.color = color;
            this.emoji = emoji;
        }
    }

    /**
     * RÃ©cupÃ¨re la catÃ©gorie par son ID depuis le cache allCategories
     */
    private EventCategory getCategoryById(int categoryId) {
        if (allCategories != null) {
            for (EventCategory cat : allCategories) {
                if (cat.getId() == categoryId) {
                    return cat;
                }
            }
        }
        return null;
    }

    /**
     * DÃ©cale la luminositÃ© d'une couleur hexadÃ©cimale
     * @param hexColor couleur au format #RRGGBB
     * @param amount montant du dÃ©calage (-100 Ã  +100)
     */
    private String shiftColorBrightness(String hexColor, int amount) {
        try {
            // Enlever le # et convertir
            hexColor = hexColor.replace("#", "");
            long num = Long.parseLong(hexColor, 16);

            int r = (int) ((num >> 16) & 0xFF);
            int g = (int) ((num >> 8) & 0xFF);
            int b = (int) (num & 0xFF);

            // Appliquer le dÃ©calage
            r = Math.min(255, Math.max(0, r + amount));
            g = Math.min(255, Math.max(0, g + amount));
            b = Math.min(255, Math.max(0, b + amount));

            // Reconvertir en hex
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return hexColor; // Retourner la couleur originale en cas d'erreur
        }
    }
    private String sanitizeHexColor(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String trimmed = value.trim();
        if (trimmed.matches("^#([0-9a-fA-F]{6})$")) {
            return trimmed;
        }
        return fallback;
    }

    private int[] toRgb(String hexColor) {
        String normalized = sanitizeHexColor(hexColor, "#6A1B9A").replace("#", "");
        int colorInt = Integer.parseInt(normalized, 16);
        int r = (colorInt >> 16) & 0xFF;
        int g = (colorInt >> 8) & 0xFF;
        int b = colorInt & 0xFF;
        return new int[]{r, g, b};
    }
}