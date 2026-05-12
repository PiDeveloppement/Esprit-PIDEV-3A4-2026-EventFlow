package com.example.pidev.controller.event;

import com.example.pidev.MainController;
import com.example.pidev.model.event.Event;
import com.example.pidev.model.event.EventTicket;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.service.event.EventService;
import com.example.pidev.service.event.EventTicketService;
import com.example.pidev.service.user.UserService;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventTicketViewController {

    // ============ FXML FIELDS ============
    @FXML private StackPane qrCodeContainer;
    @FXML private ImageView qrCodeImage;
    @FXML private Label ticketCodeLabel;
    @FXML private Label statusBadge;
    @FXML private Label eventNameLabel;
    @FXML private Label eventDateLabel;
    @FXML private Label eventLocationLabel;
    @FXML private Label participantNameLabel;
    @FXML private Label participantEmailLabel;
    @FXML private Label createdAtLabel;
    @FXML private Label updatedAtLabel;
    @FXML private Button backBtn;
    @FXML private Button downloadBtn;

    // ============ SERVICES ============
    private EventTicketService ticketService;
    private EventService eventService;
    private UserService userService;
    private MainController mainController;
    private EventTicket currentTicket;

    // ============ INITIALIZE ============
    @FXML
    public void initialize() {
        try {
            ticketService = new EventTicketService();
            eventService = new EventService();
            userService = new UserService();
            System.out.println("✅ EventTicketViewController initialisé");
        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============ PUBLIC METHODS ============
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setTicket(EventTicket ticket) {
        this.currentTicket = ticket;
        displayTicket(ticket);
    }

    // ============ DISPLAY TICKET ============
    private void displayTicket(EventTicket ticket) {
        try {
            // ===== VÉRIFICATION DES INJECTIONS FXML =====
            System.out.println("🔍 [DEBUG] Affichage du ticket: " + ticket.getTicketCode());
            System.out.println("   - ticketCodeLabel: " + (ticketCodeLabel != null ? "✅ OK" : "❌ NULL"));
            System.out.println("   - statusBadge: " + (statusBadge != null ? "✅ OK" : "❌ NULL"));
            System.out.println("   - eventNameLabel: " + (eventNameLabel != null ? "✅ OK" : "❌ NULL"));
            System.out.println("   - participantNameLabel: " + (participantNameLabel != null ? "✅ OK" : "❌ NULL"));

            // Ticket Code
            if (ticketCodeLabel != null) {
                ticketCodeLabel.setText(ticket.getTicketCode());
                System.out.println("   ✅ Ticket Code: " + ticket.getTicketCode());
            }

            // QR Code Image
            String qrUrl = ticket.getQrCode();
            if (qrCodeImage != null && qrUrl != null && !qrUrl.isEmpty()) {
                try {
                    Image qrImage = new Image(qrUrl, 280, 280, true, true);
                    qrCodeImage.setImage(qrImage);
                    System.out.println("   ✅ QR Code chargé: " + qrUrl);
                } catch (Exception e) {
                    System.err.println("❌ Erreur chargement QR code: " + e.getMessage());
                }
            }

            // Status Badge
            if (statusBadge != null) {
                if (ticket.isUsed()) {
                    statusBadge.setText("❌ Utilisé");
                    statusBadge.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; " +
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 16; " +
                            "-fx-background-radius: 20; -fx-alignment: CENTER_LEFT;");
                    System.out.println("   ✅ Statut: Utilisé");
                } else {
                    statusBadge.setText("✅ Ticket Valide");
                    statusBadge.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; " +
                            "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 10 16; " +
                            "-fx-background-radius: 20; -fx-alignment: CENTER_LEFT;");
                    System.out.println("   ✅ Statut: Valide");
                }
            }

            // Event Info
            Event event = eventService.getEventById(ticket.getEventId());
            if (event != null) {
                if (eventNameLabel != null) {
                    eventNameLabel.setText(event.getTitle());
                    System.out.println("   ✅ Événement: " + event.getTitle());
                }

                if (eventDateLabel != null && event.getStartDate() != null) {
                    eventDateLabel.setText(formatDate(event.getStartDate()));
                    System.out.println("   ✅ Date: " + formatDate(event.getStartDate()));
                }

                if (eventLocationLabel != null) {
                    String location = event.getLocation() != null ? event.getLocation() : "Non spécifié";
                    eventLocationLabel.setText(location);
                    System.out.println("   ✅ Lieu: " + location);
                }
            } else {
                if (eventNameLabel != null) eventNameLabel.setText("Événement supprimé");
                if (eventDateLabel != null) eventDateLabel.setText("-");
                if (eventLocationLabel != null) eventLocationLabel.setText("-");
                System.out.println("   ⚠️ Événement non trouvé");
            }

            // Participant Info
            UserModel user = userService.getUserById(ticket.getUserId());
            if (user != null) {
                if (participantNameLabel != null) {
                    String fullName = user.getFirst_Name() + " " + user.getLast_Name();
                    participantNameLabel.setText(fullName);
                    System.out.println("   ✅ Participant: " + fullName);
                }
                if (participantEmailLabel != null) {
                    participantEmailLabel.setText(user.getEmail());
                    System.out.println("   ✅ Email: " + user.getEmail());
                }
            } else {
                if (participantNameLabel != null) participantNameLabel.setText("Utilisateur #" + ticket.getUserId());
                if (participantEmailLabel != null) participantEmailLabel.setText("email@example.com");
                System.out.println("   ⚠️ Participant non trouvé");
            }

            // Dates
            if (createdAtLabel != null) {
                createdAtLabel.setText(formatDateTime(ticket.getCreatedAt()));
                System.out.println("   ✅ Créé le: " + formatDateTime(ticket.getCreatedAt()));
            }

            if (updatedAtLabel != null) {
                if (ticket.isUsed() && ticket.getUsedAt() != null) {
                    updatedAtLabel.setText(formatDateTime(ticket.getUsedAt()));
                    updatedAtLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #1e293b; -fx-font-weight: 600;");
                    System.out.println("   ✅ Utilisé le: " + formatDateTime(ticket.getUsedAt()));
                } else {
                    updatedAtLabel.setText("Non utilisé");
                    updatedAtLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8; -fx-font-style: italic;");
                    System.out.println("   ✅ Non utilisé");
                }
            }

            System.out.println("✅ Affichage du ticket complété avec succès");

        } catch (Exception e) {
            System.err.println("❌ Erreur affichage ticket: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ============ EVENT HANDLERS ============
    @FXML
    private void handleBack() {
        if (mainController != null) {
            mainController.showTickets();
        }
    }

    @FXML
    private void handleDownloadPDF() {
        if (currentTicket != null) {
            try {
                showSuccess("Succès", "PDF en cours de téléchargement...");
            } catch (Exception e) {
                showError("Erreur", "Impossible de télécharger le PDF: " + e.getMessage());
            }
        }
    }


    // ============ HELPER METHODS ============
    private String formatDate(LocalDateTime date) {
        if (date == null) return "-";
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String formatDateTime(LocalDateTime date) {
        if (date == null) return "-";
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}




