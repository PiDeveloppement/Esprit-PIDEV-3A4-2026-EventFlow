package com.example.pidev.controller.front;

import com.example.pidev.HelloApplication;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.event.EventTicket;
import com.example.pidev.service.event.EventService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Contrôleur pour la page "Mon Billet" (front office)
 * @author Ons Abdesslem
 */
public class MyTicketController {

    @FXML private Label eventTitleLabel;
    @FXML private Label ticketCodeLabel;
    @FXML private Label eventDateLabel;
    @FXML private Label eventLocationLabel;
    @FXML private Label ticketStatusLabel;
    @FXML private Label eventPriceLabel;
    @FXML private ImageView qrCodeImageView;

    private EventTicket currentTicket;
    private Event currentEvent;
    private EventService eventService;

    @FXML
    public void initialize() {
        System.out.println("✅ MyTicketController initialisé");
        eventService = new EventService();
    }

    /**
     * Définir le ticket à afficher
     */
    public void setTicket(EventTicket ticket) {
        this.currentTicket = ticket;

        if (ticket == null) {
            System.err.println("❌ Ticket null dans MyTicketController");
            return;
        }

        // Charger l'événement associé
        try {
            currentEvent = eventService.getEventById(ticket.getEventId());
            displayTicket();
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement événement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Afficher les informations du billet
     */
    private void displayTicket() {
        if (currentTicket == null || currentEvent == null) return;

        // Titre de l'événement
        eventTitleLabel.setText(currentEvent.getTitle());

        // Code du ticket
        ticketCodeLabel.setText(currentTicket.getTicketCode());

        // Date de l'événement
        eventDateLabel.setText(currentEvent.getFormattedStartDate());

        // Lieu
        eventLocationLabel.setText(currentEvent.getLocation() != null ? currentEvent.getLocation() : "Non spécifié");

        // Statut du ticket
        if (currentTicket.isUsed()) {
            ticketStatusLabel.setText("✓ Utilisé");
            ticketStatusLabel.setStyle("-fx-padding: 5 15; -fx-background-radius: 20; -fx-font-size: 13px; " +
                    "-fx-font-weight: 600; -fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
        } else {
            ticketStatusLabel.setText("✓ Valide");
            ticketStatusLabel.setStyle("-fx-padding: 5 15; -fx-background-radius: 20; -fx-font-size: 13px; " +
                    "-fx-font-weight: 600; -fx-background-color: #d1fae5; -fx-text-fill: #065f46;");
        }

        // Prix
        eventPriceLabel.setText(currentEvent.getPriceDisplay());
        if (currentEvent.isFree()) {
            eventPriceLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #10b981;");
        } else {
            eventPriceLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #0D47A1;");
        }

        // QR Code
        loadQRCode();
    }

    /**
     * Charger le QR code
     */
    private void loadQRCode() {
        String qrUrl = normalizeQrUrl(currentTicket.getQrCode(), currentTicket.getTicketCode());

        if (qrUrl != null && !qrUrl.trim().isEmpty()) {
            try {
                Image qrImage = new Image(qrUrl, 250, 250, true, true);
                if (qrImage.isError()) {
                    System.err.println("❌ QR code non charge: " + qrImage.getException());
                    qrCodeImageView.setImage(createPlaceholderImage());
                } else {
                    qrCodeImageView.setImage(qrImage);
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur chargement QR: " + e.getMessage());
                qrCodeImageView.setImage(createPlaceholderImage());
            }
        } else {
            qrCodeImageView.setImage(createPlaceholderImage());
        }
    }

    /**
     * Normaliser l'URL du QR code
     */
    private String normalizeQrUrl(String rawQr, String ticketCode) {
        if (rawQr != null && !rawQr.isBlank()) {
            String trimmed = rawQr.trim();
            if (trimmed.contains("quickchart.io/qr-code-api")) {
                return trimmed.replace("/qr-code-api/", "/qr");
            }
            if (trimmed.startsWith("http")) {
                return trimmed;
            }
        }
        if (ticketCode == null || ticketCode.isBlank()) {
            return null;
        }
        String encoded = URLEncoder.encode(ticketCode, StandardCharsets.UTF_8);
        return "https://quickchart.io/qr?text=" + encoded + "&size=250";
    }

    /**
     * Créer une image placeholder pour le QR
     */
    private Image createPlaceholderImage() {
        int size = 250;
        WritableImage placeholder = new WritableImage(size, size);
        PixelWriter writer = placeholder.getPixelWriter();

        // Fond gris clair
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                writer.setColor(x, y, Color.web("#e9ecef"));
            }
        }

        return placeholder;
    }

    /**
     * Retour aux événements
     */
    @FXML
    private void handleBackToEvents() {
        System.out.println("🔙 Retour aux événements");
        HelloApplication.loadPublicEventsPage();
    }

    /**
     * Envoyer le billet par email
     */
    @FXML
    private void handleSendEmail() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fonctionnalité à venir");
        alert.setHeaderText("Envoi par email");
        alert.setContentText("Cette fonctionnalité sera bientôt disponible !");
        alert.showAndWait();
    }

    /**
     * Télécharger le billet en PDF
     */
    @FXML
    private void handleDownloadPDF() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Fonctionnalité à venir");
        alert.setHeaderText("Téléchargement PDF");
        alert.setContentText("Cette fonctionnalité sera bientôt disponible !");
        alert.showAndWait();
    }
}
