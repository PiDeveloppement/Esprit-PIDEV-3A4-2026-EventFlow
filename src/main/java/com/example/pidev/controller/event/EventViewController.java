package com.example.pidev.controller.event;

import com.example.pidev.MainController;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.event.EventCategory;
import com.example.pidev.service.event.EventCategoryService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controller pour la consultation détaillée d'un événement
 * @author Ons Abdesslem
 */
public class EventViewController {

    // ==================== FXML ELEMENTS ====================

    @FXML private Label titleHeaderLabel;
    @FXML private Label statusBadge;
    @FXML private Label categoryBadge;

    @FXML private StackPane posterContainer;
    @FXML private ImageView posterImageView;

    @FXML private Label startDateLabel;
    @FXML private Label endDateLabel;
    @FXML private Label locationLabel;
    @FXML private Label capacityLabel;
    @FXML private Label categoryLabel;
    @FXML private Label priceLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label createdByLabel;
    @FXML private Label updatedByLabel;

    @FXML private Button backBtn;

    // ==================== SERVICES ====================

    private MainController helloController;
    private EventCategoryService categoryService;
    private Event currentEvent;

    // ==================== INITIALIZATION ====================

    @FXML
    public void initialize() {
        System.out.println("✅ EventViewController initialisé");
        categoryService = new EventCategoryService();
    }

    public void setMainController(MainController helloController) {
        this.helloController = helloController;
    }

    public void setEvent(Event event) {
        this.currentEvent = event;

        if (event == null) {
            System.err.println("❌ Événement null passé à EventViewController");
            return;
        }

        if (titleHeaderLabel == null) {
            javafx.application.Platform.runLater(() -> setEvent(event));
            return;
        }

        displayEvent(event);
    }

    private String getCategoryName(int categoryId) {
        try {
            EventCategory cat = categoryService.getCategoryById(categoryId);
            return cat != null ? cat.getName() : "Catégorie " + categoryId;
        } catch (Exception e) {
            return "Catégorie " + categoryId;
        }
    }

    private void displayEvent(Event event) {
        System.out.println("📋 Affichage événement: " + event.getTitle());

        // Titre
        titleHeaderLabel.setText(event.getTitle());

        // Statut
        LocalDateTime now = LocalDateTime.now();
        String status;
        String statusStyle;

        if (event.getEndDate() != null && event.getEndDate().isBefore(now)) {
            status = "Terminé";
            statusStyle = "-fx-background-color: #e5e7eb; -fx-text-fill: #4b5563;";
        } else if (event.getStartDate() != null && event.getStartDate().isAfter(now)) {
            status = "À venir";
            statusStyle = "-fx-background-color: #d1fae5; -fx-text-fill: #065f46;";
        } else {
            status = "En cours";
            statusStyle = "-fx-background-color: #fef3c7; -fx-text-fill: #92400e;";
        }

        statusBadge.setText(status);
        statusBadge.setStyle(statusStyle + " -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 13px; -fx-font-weight: 600;");

        // Badge catégorie
        String catName = getCategoryName(event.getCategoryId());
        categoryBadge.setText(catName);
        categoryBadge.setStyle("-fx-background-color: #e0f2fe; -fx-text-fill: #0369a1; " +
                "-fx-padding: 4 12; -fx-background-radius: 20; -fx-font-size: 13px; -fx-font-weight: 600;");

        // Affiche
        if (event.getImageUrl() != null && !event.getImageUrl().trim().isEmpty()) {
            loadPosterImage(event.getImageUrl());
        }

        // Dates
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        startDateLabel.setText(event.getStartDate() != null
                ? event.getStartDate().format(dateTimeFormatter) : "Non définie");
        endDateLabel.setText(event.getEndDate() != null
                ? event.getEndDate().format(dateTimeFormatter) : "Non définie");

        // Lieu et capacité
        locationLabel.setText(event.getLocation() != null ? event.getLocation() : "Non spécifié");
        capacityLabel.setText(event.getCapacity() + " places");

        // Catégorie et prix
        categoryLabel.setText(catName);
        priceLabel.setText(event.getPriceDisplay());
        if (event.isFree()) {
            priceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #10b981;");
        } else {
            priceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 700; -fx-text-fill: #0d47a1;");
        }

        // Description
        if (event.getDescription() != null && !event.getDescription().isEmpty()) {
            descriptionLabel.setText(event.getDescription());
        } else {
            descriptionLabel.setText("Aucune description fournie.");
            descriptionLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
        }

        // Métadonnées
        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        createdByLabel.setText(event.getCreatedAt() != null
                ? "Créé le: " + event.getCreatedAt().format(fullFormatter) : "Créé le: inconnue");
        updatedByLabel.setText(event.getUpdatedAt() != null
                ? "Modifié le: " + event.getUpdatedAt().format(fullFormatter) : "Modifié le: jamais");

        System.out.println("✅ Événement affiché: " + event.getTitle());
    }

    private void loadPosterImage(String posterPath) {
        System.out.println("🖼️ Chargement affiche: " + posterPath);

        try {
            Image image = null;

            // Méthode 1 : Via getResource (classpath)
            try {
                String resourcePath = posterPath.startsWith("/") ? posterPath : "/" + posterPath;
                var resourceUrl = getClass().getResource(resourcePath);
                if (resourceUrl != null) {
                    image = new Image(resourceUrl.toExternalForm(), true);
                    System.out.println("   ✅ Via getResource");
                }
            } catch (Exception e1) {
                System.out.println("   ❌ getResource: " + e1.getMessage());
            }

            // Méthode 2 : Via File (dev local)
            if (image == null) {
                try {
                    File posterFile = new File("src/main/resources" + posterPath);
                    if (posterFile.exists()) {
                        image = new Image(posterFile.toURI().toString(), true);
                        System.out.println("   ✅ Via File");
                    }
                } catch (Exception e2) {
                    System.out.println("   ❌ File: " + e2.getMessage());
                }
            }

            if (image != null && !image.isError()) {
                posterImageView.setImage(image);
                posterImageView.setPreserveRatio(true);
                posterImageView.setSmooth(true);
                StackPane.setAlignment(posterImageView, Pos.CENTER);
                System.out.println("   ✅ Affiche affichée");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement affiche: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        if (helloController != null) {
            helloController.showEventsList();
        }
    }
}
