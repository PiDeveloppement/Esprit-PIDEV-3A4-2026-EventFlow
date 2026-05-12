package com.example.pidev.controller.front;

import com.example.pidev.HelloApplication;
import com.example.pidev.model.event.EventTicket;
import com.example.pidev.service.event.EventTicketService;
import com.example.pidev.utils.UserSession;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Contrôleur pour afficher la liste des billets du participant
 */
public class MyTicketsListController {

    @FXML private Button backBtn;
    @FXML private Label userNameLabel;
    @FXML private Label ticketCountLabel;
    @FXML private VBox ticketsContainer;
    @FXML private VBox emptyStateBox;

    private EventTicketService ticketService;

    @FXML
    public void initialize() {
        System.out.println("✅ MyTicketsListController initialisé");
        ticketService = new EventTicketService();

        loadUserInfo();
        loadMyTickets();

        backBtn.setOnAction(e -> HelloApplication.loadPublicEventsPage());
    }

    private void loadUserInfo() {
        String userName = UserSession.getInstance().getCurrentUser().getFirst_Name() + " " +
                UserSession.getInstance().getCurrentUser().getLast_Name();
        userNameLabel.setText(userName);
    }

    private void loadMyTickets() {
        int userId = UserSession.getInstance().getCurrentUser().getId_User();
        List<EventTicket> tickets = ticketService.getTicketsByUser(userId);

        ticketsContainer.getChildren().clear();

        if (tickets.isEmpty()) {
            emptyStateBox.setVisible(true);
            emptyStateBox.setManaged(true);
            ticketCountLabel.setText("Aucun billet pour le moment");
        } else {
            emptyStateBox.setVisible(false);
            emptyStateBox.setManaged(false);
            ticketCountLabel.setText(tickets.size() + " billet" + (tickets.size() > 1 ? "s" : ""));

            for (EventTicket ticket : tickets) {
                ticketsContainer.getChildren().add(createTicketCard(ticket));
            }
        }
    }

    private HBox createTicketCard(EventTicket ticket) {
        HBox card = new HBox(25);
        card.setStyle("-fx-background-color: white; -fx-border-radius: 12; -fx-background-radius: 12; " +
                "-fx-padding: 25; -fx-border-color: #e2e8f0; -fx-border-width: 1; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 12, 0, 0, 2);");
        card.setPrefHeight(220);
        card.setStyle(card.getStyle() + " -fx-cursor: hand;");

        // Effet hover
        card.setOnMouseEntered(e -> card.setStyle(
                "-fx-background-color: white; -fx-border-radius: 12; -fx-background-radius: 12; " +
                        "-fx-padding: 25; -fx-border-color: #0D47A1; -fx-border-width: 2; " +
                        "-fx-effect: dropshadow(gaussian, rgba(13,71,161,0.15), 16, 0, 0, 4); -fx-cursor: hand;"
        ));
        card.setOnMouseExited(e -> card.setStyle(
                "-fx-background-color: white; -fx-border-radius: 12; -fx-background-radius: 12; " +
                        "-fx-padding: 25; -fx-border-color: #e2e8f0; -fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 12, 0, 0, 2); -fx-cursor: hand;"
        ));

        // QR Code Section
        VBox qrContainer = new VBox();
        qrContainer.setPrefWidth(200);
        qrContainer.setStyle("-fx-alignment: center; -fx-background-color: #f8fafc; -fx-border-radius: 10; -fx-padding: 15; -fx-border-color: #e2e8f0; -fx-border-width: 1;");

        String qrUrl = ticket.getQrCode();
        if (qrUrl != null && !qrUrl.isBlank()) {
            try {
                System.out.println("📥 Chargement QR URL: " + qrUrl);
                ImageView qrImage = new ImageView();
                qrImage.setFitWidth(170.0);
                qrImage.setFitHeight(170.0);

                Image qrImg = new Image(qrUrl, 170, 170, true, true);
                qrImage.setImage(qrImg);

                System.out.println("✅ QR code chargé");
                qrContainer.getChildren().add(qrImage);
            } catch (Exception e) {
                System.err.println("❌ Erreur chargement QR: " + e.getMessage());
                Label errorLabel = new Label("⚠️ QR non disponible");
                errorLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 12px;");
                qrContainer.getChildren().add(errorLabel);
            }
        } else {
            System.out.println("⚠️ URL QR null ou vide");
            Label noQrLabel = new Label("❌ Pas de QR");
            noQrLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-size: 14px; -fx-font-weight: bold;");
            qrContainer.getChildren().add(noQrLabel);
        }

        // Infos du billet
        VBox infoContainer = new VBox(16);
        infoContainer.setStyle("-fx-spacing: 16; -fx-padding: 5;");

        // Code ticket
        HBox codeBox = new HBox(10);
        Label codeLabel = new Label("🎫");
        codeLabel.setStyle("-fx-font-size: 14px;");
        Label codeValue = new Label(ticket.getTicketCode());
        codeValue.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0D47A1;");
        codeBox.getChildren().addAll(codeLabel, codeValue);

        // Événement
        HBox eventBox = new HBox(10);
        Label eventIcon = new Label("📅");
        eventIcon.setStyle("-fx-font-size: 14px;");
        Label eventValue = new Label("Événement #" + ticket.getEventId());
        eventValue.setStyle("-fx-font-size: 13px; -fx-text-fill: #475569;");
        eventBox.getChildren().addAll(eventIcon, eventValue);

        // Date de création
        HBox dateBox = new HBox(10);
        Label dateIcon = new Label("📆");
        dateIcon.setStyle("-fx-font-size: 14px;");
        Label dateValue = new Label(ticket.getFormattedCreatedAt());
        dateValue.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        dateBox.getChildren().addAll(dateIcon, dateValue);

        // Statut
        HBox statusBox = new HBox(10);
        Label statusIcon = new Label(ticket.isUsed() ? "✅" : "⏳");
        statusIcon.setStyle("-fx-font-size: 14px;");
        Label statusValue = new Label(ticket.isUsed() ? "Utilisé" : "Valide");
        statusValue.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; " +
                (ticket.isUsed() ? "-fx-text-fill: #7c3aed;" : "-fx-text-fill: #059669;"));
        statusBox.getChildren().addAll(statusIcon, statusValue);

        // Buttons
        HBox buttonsBox = new HBox(12);
        buttonsBox.setPadding(new Insets(10, 0, 0, 0));

        Button viewBtn = new Button("Voir détails");
        viewBtn.setStyle("-fx-padding: 10 18; -fx-font-size: 12px; -fx-background-color: #dbeafe; " +
                "-fx-text-fill: #0D47A1; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        viewBtn.setOnAction(e -> showTicketDetail(ticket));

        Button pdfBtn = new Button("📥 Télécharger PDF");
        pdfBtn.setStyle("-fx-padding: 10 18; -fx-font-size: 12px; -fx-background-color: #10b981; " +
                "-fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        pdfBtn.setOnAction(e -> downloadTicketPDF(ticket));

        buttonsBox.getChildren().addAll(viewBtn, pdfBtn);

        infoContainer.getChildren().addAll(codeBox, eventBox, dateBox, statusBox, buttonsBox);

        card.getChildren().addAll(qrContainer, infoContainer);
        return card;
    }

    private void downloadTicketPDF(EventTicket ticket) {
        System.out.println("📥 Génération du PDF pour : " + ticket.getTicketCode());

        try {
            // Récupérer l'événement
            com.example.pidev.service.event.EventService eventService = new com.example.pidev.service.event.EventService();
            com.example.pidev.model.event.Event event = eventService.getEventById(ticket.getEventId());

            if (event == null) {
                showAlert("Erreur", "Événement introuvable", Alert.AlertType.ERROR);
                return;
            }

            // Récupérer le nom du participant
            com.example.pidev.utils.UserSession session = com.example.pidev.utils.UserSession.getInstance();
            String userName = session.getFullName();

            // Générer le PDF
            String pdfPath = com.example.pidev.utils.TicketPDFGenerator.generateTicketPDF(ticket, event, userName);

            if (pdfPath != null) {
                // Ouvrir le PDF
                com.example.pidev.utils.TicketPDFGenerator.openPDF(pdfPath);

                // Afficher un message de succès
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("PDF Généré");
                alert.setHeaderText("✅ Votre billet est prêt !");
                alert.setContentText("Le PDF a été généré et ouvert.\n\n" +
                        "Scannez le QR code du PDF avec votre téléphone pour l'ouvrir.\n" +
                        "Présentez ce PDF le jour de l'événement.");
                alert.showAndWait();
            } else {
                showAlert("Erreur", "Impossible de générer le PDF", Alert.AlertType.ERROR);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur génération PDF: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Une erreur est survenue lors de la génération du PDF", Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showTicketDetail(EventTicket ticket) {
        System.out.println("📋 Affichage du détail du billet : " + ticket.getTicketCode());
        // TODO: Ouvrir la page détail du billet en front office
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détail du billet");
        alert.setHeaderText("Code : " + ticket.getTicketCode());
        alert.setContentText("Billet créé le : " + ticket.getFormattedCreatedAt());
        alert.showAndWait();
    }
}



