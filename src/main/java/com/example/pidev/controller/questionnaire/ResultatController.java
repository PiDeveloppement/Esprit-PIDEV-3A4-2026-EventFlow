package com.example.pidev.controller.questionnaire;

import com.example.pidev.MainController;
import com.example.pidev.model.questionnaire.Question;
import com.example.pidev.service.questionnaire.BadWordService;
import com.example.pidev.service.questionnaire.FeedbackService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

public class ResultatController {

    @FXML private VBox vboxCorrection;
    @FXML private Label lblScore, lblStatus;
    @FXML private HBox editStarContainer;
    @FXML private TextArea txtAreaComment;
    @FXML private Button btnCertificat;

    private int feedbackId;
    private int etoilesModif = 0;
    private int participantId;

    private final FeedbackService fs = new FeedbackService();
    private final BadWordService badWordService = new BadWordService();
    @FXML
    public void initialize() {
        setupEditStars();
    }

    public void setParticipantId(int id) {
        this.participantId = id;
    }

    /**
     * Initialise les données et affiche la correction
     */
    public void initData(int fId, List<Question> questions, List<String> reponsesDonnees, String commentaire, int noteEtoiles) {
        this.feedbackId = fId;
        int score = 0;
        int total = questions.size();

        vboxCorrection.getChildren().clear();

        for (int i = 0; i < total; i++) {
            Question q = questions.get(i);
            String repUser = reponsesDonnees.get(i);
            String bonneRep = q.getReponse();

            VBox bloc = new VBox(5);
            bloc.setStyle("-fx-padding: 10; -fx-border-color: #ecf0f1; -fx-border-radius: 5; -fx-background-color: #ffffff;");

            Label qLabel = new Label((i + 1) + ". " + q.getTexte());
            qLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            Label rLabel = new Label("Votre réponse : " + repUser);

            if (repUser.equalsIgnoreCase(bonneRep)) {
                score++;
                rLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            } else {
                rLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                Label cLabel = new Label("Correct : " + bonneRep);
                cLabel.setStyle("-fx-text-fill: #2980b9; -fx-font-style: italic;");
                bloc.getChildren().add(cLabel);
            }

            bloc.getChildren().add(0, qLabel);
            bloc.getChildren().add(1, rLabel);
            vboxCorrection.getChildren().add(bloc);
        }

        lblScore.setText("Score: " + score + " / " + total);
        txtAreaComment.setText(commentaire);
        this.etoilesModif = noteEtoiles;
        actualiserEtoiles();

        if (score >= (total / 2.0)) {
            lblStatus.setText("ADMIS ✅");
            lblStatus.setStyle("-fx-text-fill: #27ae60;");
            btnCertificat.setVisible(true);
            btnCertificat.setManaged(true);
        } else {
            lblStatus.setText("ÉCHEC ❌");
            lblStatus.setStyle("-fx-text-fill: #e74c3c;");
            btnCertificat.setVisible(false);
            btnCertificat.setManaged(false);
        }
    }

    private void setupEditStars() {
        for (int i = 0; i < editStarContainer.getChildren().size(); i++) {
            final int val = i + 1;
            if (editStarContainer.getChildren().get(i) instanceof Button b) {
                b.setOnAction(e -> {
                    etoilesModif = val;
                    actualiserEtoiles();
                });
            }
        }
    }

    private void actualiserEtoiles() {
        for (int j = 0; j < editStarContainer.getChildren().size(); j++) {
            if (editStarContainer.getChildren().get(j) instanceof Button b) {
                if (j < etoilesModif) {
                    b.setStyle("-fx-text-fill: #f1c40f; -fx-background-color: transparent; -fx-font-size: 25; -fx-padding: 0;");
                } else {
                    b.setStyle("-fx-text-fill: #bdc3c7; -fx-background-color: transparent; -fx-font-size: 25; -fx-padding: 0;");
                }
            }
        }
    }

    // --- ACTIONS BOUTONS ---

    /**
     * Correction de l'erreur FXML : Ajout de la méthode de navigation
     */
    @FXML
    private void retourAccueil() {
        // Redirection vers la liste des événements ou l'accueil participant
        changeScene("/com/example/pidev/fxml/questionnaire/accueil_participant.fxml", "ACCUEIL QUIZ");
    }

    @FXML
    private void onModifier() {
        try {
            // 2. Récupérer le texte brut
            String texteBrut = txtAreaComment.getText();

            // 3. Filtrer le texte via l'API
            // Note: Cela peut prendre 1-2 secondes car c'est un appel réseau
            String texteFiltre = badWordService.filtrerTexte(texteBrut);

            // 4. Mettre à jour l'affichage pour que l'utilisateur voie les étoiles (****)
            txtAreaComment.setText(texteFiltre);

            // 5. Envoyer le texte filtré à la base de données
            fs.modifierFeedback(feedbackId, texteFiltre, etoilesModif);

            afficherAlerte("Succès", "Votre avis a été mis à jour (les mots inappropriés ont été censurés) !", Alert.AlertType.INFORMATION);
        } catch (SQLException e) {
            afficherAlerte("Erreur", "Erreur SQL : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void onSupprimer() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Supprimer définitivement cet avis ?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                try {
                    fs.supprimerFeedback(feedbackId);
                    txtAreaComment.clear();
                    etoilesModif = 0;
                    actualiserEtoiles();
                    afficherAlerte("Supprimé", "Votre avis a été effacé.", Alert.AlertType.INFORMATION);
                } catch (SQLException ex) {
                    afficherAlerte("Erreur", "Impossible de supprimer : " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void onDownloadCertificat() {
        afficherAlerte("Certificat", "Génération du PDF en cours...", Alert.AlertType.INFORMATION);
    }

    /**
     * Méthode utilitaire pour changer de scène via le MainController
     */
    private void changeScene(String fxmlPath, String title) {
        Platform.runLater(() -> {
            try {
                URL resource = getClass().getResource(fxmlPath);
                if (resource == null) {
                    afficherAlerte("Erreur", "Fichier FXML introuvable : " + fxmlPath, Alert.AlertType.ERROR);
                    return;
                }

                FXMLLoader loader = new FXMLLoader(resource);
                Parent root = loader.load();

                if (MainController.getInstance() != null) {
                    MainController.getInstance().setContent(root, title);
                }
            } catch (IOException e) {
                e.printStackTrace();
                afficherAlerte("Erreur", "Erreur de chargement de la page.", Alert.AlertType.ERROR);
            }
        });
    }

    private void afficherAlerte(String titre, String message, Alert.AlertType type) {
        Alert a = new Alert(type);
        a.setTitle(titre);
        a.setHeaderText(null);
        a.setContentText(message);
        a.show();
    }
}