package com.example.pidev.controller.event;

import com.example.pidev.MainController;
import com.example.pidev.model.event.EventTicket;
import com.example.pidev.service.event.EventTicketService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controller pour la consultation détaillée d'un ticket
 * @author Ons Abdesslem
 */
public class EventTicketViewController {

    // ==================== FXML ELEMENTS ====================

    @FXML private Label ticketCodeLabel;
    @FXML private Label eventNameLabel;
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label statusLabel;
    @FXML private Label createdAtLabel;
    @FXML private Label usedAtLabel;
    @FXML private ImageView qrCodeImage;
    @FXML private Button backBtn;
    @FXML private Button scanBtn;
    @FXML private Button deleteBtn;

    // ==================== NAVBAR ELEMENTS ====================
    @FXML private Label dateLabel;
    @FXML private Label timeLabel;

    // ==================== SERVICES ====================

    private MainController helloController;
    private EventTicketService ticketService;
    private EventTicket currentTicket;

    // ==================== INITIALIZATION ====================

    @FXML
    public void initialize() {
        System.out.println("✅ EventTicketViewController initialisé");
        ticketService = new EventTicketService();

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

    public void setTicket(EventTicket ticket) {
        this.currentTicket = ticket;

        if (ticket == null) return;

        displayTicket(ticket);
    }

    private void displayTicket(EventTicket ticket) {
        ticketCodeLabel.setText(ticket.getTicketCode());

        // TODO: Récupérer les vrais noms depuis les services
        eventNameLabel.setText("Événement #" + ticket.getEventId());
        userNameLabel.setText("Utilisateur #" + ticket.getUserId());
        userEmailLabel.setText("user@email.com");

        // Statut avec badge
        if (ticket.isUsed()) {
            statusLabel.setText("Utilisé");
            statusLabel.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; " +
                    "-fx-padding: 4 12; -fx-background-radius: 20;");
            scanBtn.setDisable(true);
        } else {
            statusLabel.setText("Non utilisé");
            statusLabel.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #065f46; " +
                    "-fx-padding: 4 12; -fx-background-radius: 20;");
            scanBtn.setDisable(false);
        }

        createdAtLabel.setText(ticket.getFormattedCreatedAt());
        usedAtLabel.setText(ticket.getFormattedUsedAt());

        // Charger le QR code depuis l'URL stockée
        String qrUrl = ticket.getQrCode();
        if (qrUrl != null && !qrUrl.isBlank()) {
            qrCodeImage.setImage(new Image(qrUrl, true));
        } else {
            qrCodeImage.setImage(null);
        }
    }

    @FXML
    private void handleBack() {
        if (helloController != null) {
            helloController.showTicketsList();
        }
    }

    @FXML
    private void handleScan() {
        if (currentTicket != null && !currentTicket.isUsed()) {
            boolean success = ticketService.markTicketAsUsed(currentTicket.getId());
            if (success) {
                currentTicket.markAsUsed();
                displayTicket(currentTicket);
                showSuccess("Succès", "Ticket scanné et marqué comme utilisé");
            } else {
                showError("Erreur", "Impossible de scanner le ticket");
            }
        }
    }

    @FXML
    private void handleDelete() {
        if (currentTicket == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le ticket ?");
        confirm.setContentText("Cette action est irréversible.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (ticketService.deleteTicket(currentTicket.getId())) {
                    showSuccess("Succès", "Ticket supprimé");
                    handleBack();
                } else {
                    showError("Erreur", "Impossible de supprimer");
                }
            }
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
