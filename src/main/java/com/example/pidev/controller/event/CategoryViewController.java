package com.example.pidev.controller.event;

import com.example.pidev.MainController;
import com.example.pidev.model.event.EventCategory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CategoryViewController {

    @FXML private Label iconLabel;
    @FXML private Label nameLabel;
    @FXML private Label detailNameLabel;
    @FXML private Label statusLabel;
    @FXML private Label detailIconLabel;
    @FXML private Region colorPreview;
    @FXML private Label colorHexLabel;
    @FXML private Label colorNameLabel;
    @FXML private Label createdAtLabel;
    @FXML private Label updatedAtLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label createdByLabel;
    @FXML private Label updatedByLabel;
    @FXML private Button backBtn;
    @FXML private Label dateLabel;
    @FXML private Label timeLabel;

    private MainController helloController;

    @FXML
    public void initialize() {
        System.out.println("✅ CategoryViewController initialisé");
        updateDateTime();

        Timeline clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> updateDateTime()),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    private void updateDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy", Locale.FRENCH);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

        if (dateLabel != null) {
            String dateText = now.format(dateFormatter);
            dateLabel.setText(dateText.substring(0, 1).toUpperCase() + dateText.substring(1));
        }
        if (timeLabel != null) {
            timeLabel.setText(now.format(timeFormatter));
        }
    }

    public void setMainController(MainController helloController) {
        this.helloController = helloController;
    }

    public void setCategory(EventCategory category) {
        if (category == null) {
            System.err.println("❌ setCategory: category est NULL");
            return;
        }

        // Vérifier si les labels critiques sont injectés
        if (createdAtLabel == null || updatedAtLabel == null) {
            System.out.println("⚠️ Labels pas encore injectés, retry après délai...");
            System.out.println("   - createdAtLabel: " + (createdAtLabel != null ? "OK" : "NULL"));
            System.out.println("   - updatedAtLabel: " + (updatedAtLabel != null ? "OK" : "NULL"));

            // Reessayer après 100ms
            Platform.runLater(() -> {
                System.out.println("🔄 Retry setCategory après délai");
                setCategory(category);
            });
            return;
        }

        displayCategory(category);
    }

    /**
     * Affiche les détails de la catégorie
     * Cette méthode s'exécute une fois que tous les labels sont injectés
     */
    private void displayCategory(EventCategory category) {
        System.out.println("\n📋 === AFFICHAGE CATÉGORIE: " + category.getName() + " ===");

        // Vérification des labels injectés
        System.out.println("🔍 Vérification des labels:");
        System.out.println("   - createdAtLabel: " + (createdAtLabel != null ? "OK" : "NULL"));
        System.out.println("   - updatedAtLabel: " + (updatedAtLabel != null ? "OK" : "NULL"));
        System.out.println("   - nameLabel: " + (nameLabel != null ? "OK" : "NULL"));
        System.out.println("   - iconLabel: " + (iconLabel != null ? "OK" : "NULL"));

        // En-tête
        if (iconLabel != null) {
            iconLabel.setText(category.getIcon() != null ? category.getIcon() : "📌");
            System.out.println("✅ iconLabel défini");
        }
        if (nameLabel != null) {
            nameLabel.setText(category.getName());
            System.out.println("✅ nameLabel défini");
        }

        // Détails
        if (detailNameLabel != null) {
            detailNameLabel.setText(category.getName());
            System.out.println("✅ detailNameLabel défini");
        }
        if (detailIconLabel != null) {
            detailIconLabel.setText(category.getIcon() != null ? category.getIcon() : "📌");
            System.out.println("✅ detailIconLabel défini");
        }

        // Statut
        if (statusLabel != null) {
            if (category.isActive()) {
                statusLabel.setText("Actif");
                statusLabel.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; -fx-padding: 4 12; -fx-background-radius: 20;");
            } else {
                statusLabel.setText("Inactif");
                statusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 4 12; -fx-background-radius: 20;");
            }
            System.out.println("✅ statusLabel défini: " + category.isActive());
        }

        // Couleur
        if (category.getColor() != null && !category.getColor().isEmpty() && colorPreview != null) {
            try {
                Color.web(category.getColor());
                colorPreview.setStyle("-fx-background-color: " + category.getColor() + "; -fx-min-width: 24; -fx-min-height: 24; -fx-background-radius: 6; -fx-border-color: #e2e8f0; -fx-border-width: 1;");
                if (colorHexLabel != null) colorHexLabel.setText(category.getColor().toUpperCase());
                if (colorNameLabel != null) colorNameLabel.setText("(" + getColorName(category.getColor()) + ")");
                System.out.println("✅ Couleur définie: " + category.getColor());
            } catch (Exception e) {
                if (colorHexLabel != null) colorHexLabel.setText("#??????");
                if (colorNameLabel != null) colorNameLabel.setText("(inconnue)");
                System.err.println("⚠️ Erreur couleur: " + e.getMessage());
            }
        }

        // DATES DYNAMIQUES - AVEC LOGS DÉTAILLÉS
        System.out.println("\n📅 === TRAITEMENT DES DATES ===");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH);
        DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH);

        // Vérification des dates en base
        System.out.println("📊 Données depuis la base:");
        System.out.println("   - createdAt: " + category.getCreatedAt());
        System.out.println("   - updatedAt: " + category.getUpdatedAt());

        // Traiter createdAt
        if (category.getCreatedAt() != null) {
            String createdText = category.getCreatedAt().format(dateFormatter);
            System.out.println("📝 Formatage createdAt: " + createdText);

            if (createdAtLabel != null) {
                createdAtLabel.setText(createdText);
                System.out.println("✅ createdAtLabel.setText() exécuté: '" + createdAtLabel.getText() + "'");
            } else {
                System.err.println("❌ createdAtLabel est NULL!");
            }

            if (createdByLabel != null) {
                createdByLabel.setText("Créé le: " + category.getCreatedAt().format(fullFormatter));
                System.out.println("✅ createdByLabel défini");
            }
        } else {
            System.out.println("⚠️ createdAt est NULL en base de données");
            if (createdAtLabel != null) {
                createdAtLabel.setText("Date inconnue");
                System.out.println("✅ createdAtLabel défini avec 'Date inconnue'");
            }
            if (createdByLabel != null) {
                createdByLabel.setText("Créé le: inconnue");
            }
        }

        // Traiter updatedAt
        if (category.getUpdatedAt() != null) {
            String updatedText = category.getUpdatedAt().format(dateFormatter);
            System.out.println("📝 Formatage updatedAt: " + updatedText);

            if (updatedAtLabel != null) {
                updatedAtLabel.setText(updatedText);
                System.out.println("✅ updatedAtLabel.setText() exécuté: '" + updatedAtLabel.getText() + "'");
            } else {
                System.err.println("❌ updatedAtLabel est NULL!");
            }

            if (updatedByLabel != null) {
                updatedByLabel.setText("Modifié le: " + category.getUpdatedAt().format(fullFormatter));
                System.out.println("✅ updatedByLabel défini");
            }
        } else {
            System.out.println("⚠️ updatedAt est NULL en base de données");
            if (updatedAtLabel != null) {
                updatedAtLabel.setText("Jamais modifiée");
                System.out.println("✅ updatedAtLabel défini avec 'Jamais modifiée'");
            }
            if (updatedByLabel != null) {
                updatedByLabel.setText("Modifié le: jamais");
            }
        }

        // Description
        if (descriptionLabel != null) {
            descriptionLabel.setText(category.getDescription() != null ? category.getDescription() : "Aucune description");
            System.out.println("✅ descriptionLabel défini");
        }

        System.out.println("✅ === FIN AFFICHAGE CATÉGORIE ===\n");
    }

    private String getColorName(String hex) {
        switch (hex.toUpperCase()) {
            case "#FF9800": return "Orange";
            case "#4CAF50": return "Vert";
            case "#2196F3":
            case "#1976D2": return "Bleu";
            case "#9C27B0": return "Violet";
            case "#F44336": return "Rouge";
            case "#E91E63": return "Rose";
            default: return "personnalisée";
        }
    }

    @FXML
    private void handleBack() {
        if (helloController != null) helloController.showCategories();
    }
}