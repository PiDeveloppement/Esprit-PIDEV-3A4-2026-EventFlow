package com.example.pidev.controller.questionnaire; // Changez 'feedback' par 'questionnaire'
import com.example.pidev.model.questionnaire.Feedback;
import com.example.pidev.service.questionnaire.FeedbackService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class FeedbackViewController {
    @FXML private VBox feedbackListContainer;
    @FXML private Label lblMoyenne, lblTotalRatings;
    private final FeedbackService fs = new FeedbackService();

    @FXML
    public void initialize() {
        try {
            // Stats
            Map<String, Object> stats = fs.getStatistiques();
            lblMoyenne.setText(String.format("%.1f", (double)stats.get("moyenne")));
            lblTotalRatings.setText(stats.get("total") + " Ratings");

            // Liste
            List<Feedback> feedbacks = fs.getListeFeedbacks();
            for (Feedback fb : feedbacks) {
                feedbackListContainer.getChildren().add(creerCarte(fb));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private VBox creerCarte(Feedback fb) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 15;");

        Label user = new Label("User #" + fb.getIdUser());
        user.setStyle("-fx-font-weight: bold;");

        HBox stars = new HBox();
        for(int i=0; i<5; i++) stars.getChildren().add(new Label(i < fb.getEtoiles() ? "★" : "☆"));

        Label comment = new Label(fb.getComments());
        comment.setWrapText(true);

        card.getChildren().addAll(user, stars, comment);
        return card;
    }
    // Appelée pour chaque feedback récupéré depuis la base de données
    private void ajouterCarteFeedback(String nomUtilisateur, String commentaire, int etoiles) {
        VBox carte = new VBox(10);
        carte.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 2);");

        Label lblNom = new Label(nomUtilisateur);
        lblNom.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label lblCommentaire = new Label(commentaire);
        lblCommentaire.setWrapText(true);
        lblCommentaire.setStyle("-fx-text-fill: #546e7a;");

        Label lblEtoiles = new Label("★".repeat(etoiles) + "☆".repeat(5 - etoiles));
        lblEtoiles.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 14px;");

        carte.getChildren().addAll(lblNom, lblEtoiles, lblCommentaire);
        feedbackListContainer.getChildren().add(carte);
    }
}