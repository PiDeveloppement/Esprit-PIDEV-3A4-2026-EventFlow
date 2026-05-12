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

import java.io.File;
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

    private void setupFilters() {
        dateFilter.getItems().addAll(
                "Toutes les dates",
                "Aujourd'hui",
                "Cette semaine",
                "Ce mois-ci",
                "À venir"
        );
        dateFilter.setValue("Toutes les dates");

        priceFilter.getItems().addAll(
                "Tous les prix",
                "Gratuit",
                "Payant"
        );
        priceFilter.setValue("Tous les prix");

        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
        categoryFilter.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        dateFilter.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        priceFilter.valueProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    private void loadEvents() {
        try {
            // Charger TOUS les événements depuis la base
            allEvents = eventService.getAllEvents();

            // Filtrer pour exclure les événements TERMINÉS (endDate < maintenant)
            LocalDateTime now = LocalDateTime.now();
            allEvents = allEvents.stream()
                    .filter(event -> event.getEndDate() == null || event.getEndDate().isAfter(now))
                    .collect(Collectors.toList());

            allCategories = categoryService.getAllCategories();
            categoryFilter.getItems().clear();
            categoryFilter.getItems().add("Toutes les catégories");
            for (EventCategory cat : allCategories) {
                categoryFilter.getItems().add(cat.getName());
            }
            categoryFilter.setValue("Toutes les catégories");

            filteredEvents = allEvents;
            displayEvents(filteredEvents);

            System.out.println("✅ " + allEvents.size() + " événements chargés (événements terminés exclus)");

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement événements: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les événements", Alert.AlertType.ERROR);
        }
    }

    private void applyFilters() {
        if (allEvents == null || allEvents.isEmpty()) return;

        String searchText = searchField.getText().toLowerCase().trim();
        String category = categoryFilter.getValue();
        String date = dateFilter.getValue();
        String price = priceFilter.getValue();

        LocalDateTime now = LocalDateTime.now();

        filteredEvents = allEvents.stream()
                .filter(event -> {
                    boolean matchSearch = searchText.isEmpty() ||
                            event.getTitle().toLowerCase().contains(searchText) ||
                            (event.getDescription() != null && event.getDescription().toLowerCase().contains(searchText)) ||
                            (event.getLocation() != null && event.getLocation().toLowerCase().contains(searchText));

                    boolean matchCategory = category == null || "Toutes les catégories".equals(category);
                    if (!matchCategory) {
                        String eventCategoryName = getCategoryName(event.getCategoryId());
                        matchCategory = eventCategoryName.equals(category);
                    }

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

                    boolean matchPrice = true;
                    if (price != null) {
                        switch (price) {
                            case "Gratuit": matchPrice = event.isFree(); break;
                            case "Payant": matchPrice = !event.isFree(); break;
                        }
                    }

                    return matchSearch && matchCategory && matchDate && matchPrice;
                })
                .collect(Collectors.toList());

        displayEvents(filteredEvents);
    }

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

    private VBox createEventCard(Event event) {
        EventCategory category = getCategoryById(event.getCategoryId());
        String categoryColor = (category != null && category.getColor() != null)
                ? category.getColor() : "#6A1B9A";
        String categoryEmoji = (category != null && category.getIcon() != null)
                ? category.getIcon() : "📌";
        String categoryName = (category != null) ? category.getName() : "Autre";

        // ==================== CARD ====================
        VBox card = new VBox(0);
        card.setPrefWidth(360);
        card.setMaxWidth(360);
        // Pas de hauteur fixe : la card s'adapte au contenu

        card.setStyle("-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 0; " +
                "-fx-border-color: " + categoryColor + "; -fx-border-width: 4 0 0 0; " +
                "-fx-border-radius: 16; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);");

        String hoverColor = categoryColor.replace("#", "");
        long colorLong = Long.parseLong(hoverColor, 16);
        double r = ((colorLong >> 16) & 0xFF) / 255.0;
        double g = ((colorLong >> 8) & 0xFF) / 255.0;
        double b = (colorLong & 0xFF) / 255.0;

        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 0; " +
                        "-fx-border-color: " + categoryColor + "; -fx-border-width: 4 0 0 0; " +
                        "-fx-border-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(" +
                        (int)(r*255) + "," + (int)(g*255) + "," + (int)(b*255) + ",0.3), 15, 0, 0, 4); " +
                        "-fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-background-radius: 16; -fx-padding: 0; " +
                        "-fx-border-color: " + categoryColor + "; -fx-border-width: 4 0 0 0; " +
                        "-fx-border-radius: 16; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 2);"
        ));

        // ==================== IMAGE CONTAINER ====================
        StackPane imageContainer = new StackPane();
        imageContainer.setPrefSize(360, 450);
        imageContainer.setMinSize(360, 450);
        // Pas de maxHeight fixe : l'image remplit l'espace
        VBox.setVgrow(imageContainer, Priority.ALWAYS);
        imageContainer.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 16 16 0 0;");

        String posterPath = event.getImageUrl();
        System.out.println("🖼️ DEBUG - Tentative chargement image pour: " + event.getTitle());
        System.out.println("   posterPath = " + posterPath);

        if (posterPath != null && !posterPath.trim().isEmpty()) {
            try {
                Image image = null;

                // Méthode 1 : Via getResource
                try {
                    String resourcePath = posterPath.startsWith("/") ? posterPath : "/" + posterPath;
                    var resourceUrl = getClass().getResource(resourcePath);
                    if (resourceUrl != null) {
                        System.out.println("   ✅ Méthode 1 (getResource) - URL trouvée: " + resourceUrl);
                        image = new Image(resourceUrl.toExternalForm(), true);
                    } else {
                        System.out.println("   ❌ Méthode 1 (getResource) - Resource introuvable");
                    }
                } catch (Exception e1) {
                    System.out.println("   ❌ Méthode 1 échouée: " + e1.getMessage());
                }

                // Méthode 2 : Via File
                if (image == null) {
                    try {
                        File posterFile = new File("src/main/resources" + posterPath);
                        System.out.println("   Test Méthode 2 (File) - Chemin: " + posterFile.getAbsolutePath());
                        if (posterFile.exists()) {
                            image = new Image(posterFile.toURI().toString(), true);
                            System.out.println("   ✅ Méthode 2 (File) - Image chargée");
                        }
                    } catch (Exception e2) {
                        System.out.println("   ❌ Méthode 2 échouée: " + e2.getMessage());
                    }
                }

                // Méthode 3 : Chemin direct file:
                if (image == null) {
                    try {
                        String directPath = "file:src/main/resources" + posterPath;
                        image = new Image(directPath, true);
                        if (!image.isError()) {
                            System.out.println("   ✅ Méthode 3 (direct) - Image chargée");
                        }
                    } catch (Exception e3) {
                        System.out.println("   ❌ Méthode 3 échouée: " + e3.getMessage());
                    }
                }

                if (image != null && !image.isError()) {
                    System.out.println("   🎉 Image chargée avec succès!");
                    if (image.getProgress() < 1.0) {
                        Image finalImage = image;
                        image.progressProperty().addListener((obs, oldVal, newVal) -> {
                            if (newVal.doubleValue() >= 1.0) {
                                applyImageCover(imageContainer, finalImage);
                            }
                        });
                    } else {
                        applyImageCover(imageContainer, image);
                    }
                } else {
                    System.out.println("   ❌ Aucune méthode n'a réussi à charger l'image");
                }
            } catch (Exception ex) {
                System.err.println("❌ Erreur générale chargement image: " + ex.getMessage());
            }
        } else {
            System.out.println("   ℹ️ Pas d'image (posterPath vide ou null)");
        }

        // Badge catégorie
        Label categoryBadge = new Label(categoryEmoji + " " + categoryName);
        categoryBadge.setStyle("-fx-background-color: " + categoryColor + "; -fx-text-fill: white; " +
                "-fx-padding: 8 15; -fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: bold;");
        StackPane.setAlignment(categoryBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(categoryBadge, new Insets(15, 15, 0, 0));
        imageContainer.getChildren().add(categoryBadge);

        // ==================== CONTENU FIXE EN BAS ====================
        VBox content = new VBox(8);
        content.setPadding(new Insets(16));
        // Pas de VgGrow ici : le contenu reste compact en bas

        Label titleLabel = new Label(event.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0A1929; -fx-wrap-text: true;");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(328);
        titleLabel.setMaxHeight(45);

        HBox dateBox = new HBox(6);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Label dateIcon = new Label("📅");
        dateIcon.setStyle("-fx-font-size: 14px;");
        Label dateLabel = new Label(event.getFormattedStartDate());
        dateLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        dateBox.getChildren().addAll(dateIcon, dateLabel);

        HBox locationBox = new HBox(6);
        locationBox.setAlignment(Pos.CENTER_LEFT);
        Label locationIcon = new Label("📍");
        locationIcon.setStyle("-fx-font-size: 14px;");
        Label locationLabel = new Label(event.getLocation() != null ?
                (event.getLocation().length() > 35 ? event.getLocation().substring(0, 32) + "..." : event.getLocation())
                : "Lieu non spécifié");
        locationLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13px;");
        locationBox.getChildren().addAll(locationIcon, locationLabel);

        HBox priceBox = new HBox(6);
        priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceIcon = new Label("💰");
        priceIcon.setStyle("-fx-font-size: 14px;");
        Label priceLabel = new Label(event.getPriceDisplay());
        priceLabel.setStyle("-fx-text-fill: " + (event.isFree() ? "#10b981" : categoryColor) + "; " +
                "-fx-font-size: 15px; -fx-font-weight: bold;");
        priceBox.getChildren().addAll(priceIcon, priceLabel);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);
        buttonsBox.setPadding(new Insets(10, 0, 0, 0));

        Button viewDetailsBtn = new Button("Voir détails");
        viewDetailsBtn.setPrefWidth(155);
        viewDetailsBtn.setPrefHeight(36);
        viewDetailsBtn.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #0D47A1; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        viewDetailsBtn.setOnAction(e -> handleViewDetails(event));

        Button participateBtn = new Button("Participer");
        participateBtn.setPrefWidth(155);
        participateBtn.setPrefHeight(36);
        participateBtn.setStyle("-fx-background-color: #0D47A1; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        participateBtn.setOnAction(e -> handleParticipate(event));

        buttonsBox.getChildren().addAll(viewDetailsBtn, participateBtn);

        content.getChildren().addAll(titleLabel, dateBox, locationBox, priceBox, buttonsBox);
        card.getChildren().addAll(imageContainer, content);

        return card;
    }

    private void applyImageCover(StackPane container, Image image) {
        double targetWidth = 360.0;
        double targetHeight = 450.0;

        double imageWidth = image.getWidth();
        double imageHeight = image.getHeight();

        if (imageWidth == 0 || imageHeight == 0) return;

        double scaleX = targetWidth / imageWidth;
        double scaleY = targetHeight / imageHeight;
        double scale = Math.max(scaleX, scaleY);

        double finalWidth = imageWidth * scale;
        double finalHeight = imageHeight * scale;

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setFitWidth(finalWidth);
        imageView.setFitHeight(finalHeight);
        imageView.setCache(true);
        imageView.setCacheHint(javafx.scene.CacheHint.SPEED);

        StackPane.setAlignment(imageView, Pos.CENTER);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(targetWidth, targetHeight);
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        container.setClip(clip);

        container.getChildren().add(0, imageView);

        System.out.println("   ✅ Image appliquée avec effet cover");
    }

    private String getCategoryName(int categoryId) {
        if (allCategories != null) {
            for (EventCategory cat : allCategories) {
                if (cat.getId() == categoryId) return cat.getName();
            }
        }
        return "Autre";
    }

    private EventCategory getCategoryById(int categoryId) {
        if (allCategories != null) {
            for (EventCategory cat : allCategories) {
                if (cat.getId() == categoryId) return cat;
            }
        }
        return null;
    }

    @FXML
    private void handleViewDetails(Event event) {
        System.out.println("👁️ Voir détails de: " + event.getTitle());
        HelloApplication.loadEventDetailsPage(event);
    }

    @FXML
    private void handleParticipate(Event event) {
        System.out.println("🎫 Participer à: " + event.getTitle());

        UserSession session = UserSession.getInstance();
        if (session.getCurrentUser() == null) {
            session.setPendingEventId(event.getId());

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
                    session.clearPendingEventId();
                }
            });
            return;
        }

        createTicketForEvent(event.getId(), event.getTitle());
    }

    private void createTicketForEvent(int eventId, String eventTitle) {
        try {
            com.example.pidev.service.event.EventTicketService ticketService =
                    new com.example.pidev.service.event.EventTicketService();

            int userId = UserSession.getInstance().getCurrentUser().getId_User();

            com.example.pidev.model.event.EventTicket ticket = ticketService.createTicket(eventId, userId);

            if (ticket != null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("✅ Participation confirmée");
                alert.setHeaderText("Vous participez à l'événement !");
                alert.setContentText(
                        "Événement : " + eventTitle + "\n\n" +
                                "Votre ticket : " + ticket.getTicketCode() + "\n\n" +
                                "Un email de confirmation vous sera envoyé.\n" +
                                "Conservez votre code de ticket pour accéder à l'événement."
                );
                alert.getDialogPane().setStyle("-fx-background-color: white; -fx-font-size: 14px;");
                alert.showAndWait();
                System.out.println("✅ Ticket créé avec succès: " + ticket.getTicketCode());
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setHeaderText("Impossible de créer le ticket");
                alert.setContentText("Une erreur est survenue. Veuillez réessayer plus tard.");
                alert.showAndWait();
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la participation: " + e.getMessage());
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Erreur");
            alert.setHeaderText("Une erreur est survenue");
            alert.setContentText("Impossible de traiter votre demande. Veuillez réessayer.");
            alert.showAndWait();
        }
    }

    @FXML private void handleSearch() { applyFilters(); }

    @FXML
    private void handleResetFilters() {
        searchField.clear();
        categoryFilter.setValue("Toutes les catégories");
        dateFilter.setValue("Toutes les dates");
        priceFilter.setValue("Tous les prix");
        applyFilters();
    }

    @FXML private void handleGoToHome() { HelloApplication.loadLandingPage(); }
    @FXML private void handleGoToContact() { HelloApplication.loadLandingPage(); }
    @FXML private void handleGoToFeatures() { HelloApplication.loadLandingPage(); }
    @FXML private void handleGoToFeedback() { HelloApplication.loadLandingPage(); }
    @FXML private void handleLogin() { HelloApplication.loadLoginPage(); }
    @FXML private void handleSignup() { HelloApplication.loadSignupPage(); }

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

    private String shiftColorBrightness(String hexColor, int amount) {
        try {
            hexColor = hexColor.replace("#", "");
            long num = Long.parseLong(hexColor, 16);
            int r = (int) ((num >> 16) & 0xFF);
            int g = (int) ((num >> 8) & 0xFF);
            int b = (int) (num & 0xFF);
            r = Math.min(255, Math.max(0, r + amount));
            g = Math.min(255, Math.max(0, g + amount));
            b = Math.min(255, Math.max(0, b + amount));
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return hexColor;
        }
    }
}