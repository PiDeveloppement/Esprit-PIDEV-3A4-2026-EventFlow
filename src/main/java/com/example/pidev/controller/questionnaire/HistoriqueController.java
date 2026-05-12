package com.example.pidev.controller.questionnaire;

import com.example.pidev.model.event.Event;
import com.example.pidev.model.questionnaire.FeedbackStats;
import com.example.pidev.service.questionnaire.FeedbackService;
import com.example.pidev.utils.DBConnection;
import com.example.pidev.model.questionnaire.Question;
import com.example.pidev.MainController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import java.io.IOException;
import java.util.List;

public class HistoriqueController {
    @FXML private FlowPane cardsContainer;
    private final FeedbackService fs = new FeedbackService();

    @FXML
    public void initialize() {
        refreshView();
    }

    public void refreshView() {
        try {
            cardsContainer.getChildren().clear();
            List<FeedbackStats> data = fs.recupererHistorique();

            if (data.isEmpty()) {
                cardsContainer.getChildren().add(new Label("Aucun historique disponible."));
            } else {
                for (FeedbackStats stats : data) {
                    cardsContainer.getChildren().add(createTestimonialCard(stats));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VBox createTestimonialCard(FeedbackStats stats) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setPrefWidth(300);
        // Design type "Pinterest/Testimonial"
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 5); -fx-cursor: hand;");

        // Nom de l'utilisateur
        Label lblUser = new Label(stats.getUsername().toUpperCase());
        lblUser.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #2c3e50;");

        // Affichage des étoiles ★★★☆☆
        HBox stars = new HBox(2);
        for (int i = 0; i < 5; i++) {
            Label star = new Label(i < stats.getEtoiles() ? "★" : "☆");
            star.setStyle("-fx-text-fill: #f1c40f; -fx-font-size: 20;");
            stars.getChildren().add(star);
        }

        // Commentaire
        Label lblComment = new Label(stats.getCommentaire());
        lblComment.setWrapText(true);
        lblComment.setStyle("-fx-text-fill: #7f8c8d; -fx-font-style: italic;");
        lblComment.setMinHeight(40);

        card.getChildren().addAll(lblUser, stars, lblComment);

        // CLIC : Redirection vers Resultat.fxml en utilisant le main_layout (pour garder le menu)
        card.setOnMouseClicked(event -> {
            navigateToResultats();
        });

        return card;
    }

    private void navigateToResultats() {
        try {
            // Correction du chemin du package (points remplacés par des slashs)
            String fxmlPath = "/com/melocode/pigestion/fxml/Resultat.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            // On utilise la méthode setContent de votre layout principal pour une navigation fluide
            if (MainController.getInstance() != null) {
                MainController.getInstance().setContent(root, "📑 RÉSULTATS DÉTAILLÉS");
            }
        } catch (IOException e) {
            System.err.println("Erreur de chargement du FXML Resultat: " + e.getMessage());
            e.printStackTrace();
        }
    }
}