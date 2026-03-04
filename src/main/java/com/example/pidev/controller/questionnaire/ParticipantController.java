package com.example.pidev.controller.questionnaire;

import com.example.pidev.model.questionnaire.Question;
import com.example.pidev.model.questionnaire.FeedbackStats;
import com.example.pidev.service.questionnaire.CertificateService;
import com.example.pidev.service.questionnaire.FeedbackService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.layout.FlowPane;
public class ParticipantController {
    @FXML private Label lblQuestion, lblProgression;
    @FXML private TextField txtReponseParticipant;
    @FXML private TextArea txtCommentaire;
    @FXML private HBox starContainer;
    @FXML private VBox vboxEvaluation;
    @FXML private Button btnSuivant;
    @FXML private FlowPane emojiPicker;

    private List<Question> listeQuestions = new ArrayList<>();
    private final List<String> reponsesUtilisateur = new ArrayList<>();
    private int indexActuel = 0;
    private int etoilesSelectionnees = 0;

    // Simulation de l'utilisateur connecté (A remplacer par votre session utilisateur)
    private final int idParticipantConnecte = 1;
    private final int idEventActuel = 1;

    private final FeedbackService fs = new FeedbackService();
    private final CertificateService certificateService = new CertificateService();
    @FXML
    public void initialize() {
        try {
            listeQuestions = fs.chargerQuestionsAleatoires(idEventActuel);

            if (listeQuestions.isEmpty()) {
                lblQuestion.setText("Désolé, aucune question n'est configurée pour cet événement.");
                btnSuivant.setDisable(true);
                return;
            }

            setupStars();

            // Cacher la section évaluation (étoiles + commentaire) au début
            if (vboxEvaluation != null) {
                vboxEvaluation.setVisible(false);
                vboxEvaluation.setManaged(false);
            }

            afficherQuestion();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        setupEmojiPicker();
    }

    private void setupStars() {
        if (starContainer == null) return;

        for (int i = 0; i < starContainer.getChildren().size(); i++) {
            final int val = i + 1;
            if (starContainer.getChildren().get(i) instanceof Button b) {
                b.setCursor(javafx.scene.Cursor.HAND);
                b.setOnAction(e -> {
                    etoilesSelectionnees = val;
                    actualiserEtoiles();
                });
            }
        }
    }

    private void actualiserEtoiles() {
        for (int j = 0; j < starContainer.getChildren().size(); j++) {
            if (starContainer.getChildren().get(j) instanceof Button b) {
                // Style Or pour sélectionné, Gris pour vide
                b.setStyle(j < etoilesSelectionnees
                        ? "-fx-text-fill: #f1c40f; -fx-background-color: transparent; -fx-font-size: 30; -fx-padding: 0;"
                        : "-fx-text-fill: #bdc3c7; -fx-background-color: transparent; -fx-font-size: 30; -fx-padding: 0;");
            }
        }
    }

    private void afficherQuestion() {
        Question q = listeQuestions.get(indexActuel);
        lblProgression.setText("Question " + (indexActuel + 1) + " / " + listeQuestions.size());
        lblQuestion.setText(q.getTexteQuestion());

        // Si c'est la dernière question, on affiche le formulaire d'évaluation finale
        if (indexActuel == listeQuestions.size() - 1) {
            if (vboxEvaluation != null) {
                vboxEvaluation.setVisible(true);
                vboxEvaluation.setManaged(true);
            }
            btnSuivant.setText("TERMINER ET VOIR LE RÉSULTAT");
            btnSuivant.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
        }
    }

    @FXML
    private void handleSuivant() {
        String rep = txtReponseParticipant.getText().trim();
        if (rep.isEmpty()) {
            afficherAlerte("Champ requis", "Veuillez saisir une réponse avant de continuer.");
            return;
        }

        // On enregistre la réponse localement pour l'instant
        reponsesUtilisateur.add(rep);

        if (indexActuel < listeQuestions.size() - 1) {
            indexActuel++;
            txtReponseParticipant.clear();
            afficherQuestion();
        } else {
            // Validation finale des étoiles
            if (etoilesSelectionnees == 0) {
                afficherAlerte("Note requise", "Merci de donner une note à l'événement (étoiles).");
                return;
            }
            sauvegarderEtChangerPage();
        }
    }

    private void sauvegarderEtChangerPage() {
        try {
            int dernierIdFeedback = 0;
            int nombreBonnesReponses = 0;

            // 1. Sauvegarde en base de données et calcul du score
            for (int i = 0; i < listeQuestions.size(); i++) {
                Question q = listeQuestions.get(i);
                String repDonnee = reponsesUtilisateur.get(i);

                if (repDonnee.equalsIgnoreCase(q.getBonneReponse())) {
                    nombreBonnesReponses++;
                }

                dernierIdFeedback = fs.enregistrerFeedbackComplet(
                        idParticipantConnecte,
                        idEventActuel,
                        q.getIdQuestion(),
                        repDonnee,
                        txtCommentaire.getText(),
                        etoilesSelectionnees
                );
            }

            // ================= AJOUT : GÉNÉRATION DU CERTIFICAT SI ADMIS =================
            // Seuil de réussite : au moins la moitié des réponses justes
            if (nombreBonnesReponses >= (listeQuestions.size() / 2)) {
                com.example.pidev.service.questionnaire.CertificateService certService = new com.example.pidev.service.questionnaire.CertificateService();

                // RÉCUPÉRATION DU VRAI NOM DEPUIS LA BASE DE DONNÉES
                String nomGagnant = fs.getNomUserComplet(idParticipantConnecte);

                String scoreFinal = nombreBonnesReponses + " / " + listeQuestions.size();
                String cheminLogo = "C:\\Users\\USER\\Desktop\\pigestion\\src\\main\\java\\com\\example\\pidev\\images\\logo.png";
                // On génère avec le nom récupéré (ex: "Ghofrane Jridi")
                certService.genererCertificat(nomGagnant, scoreFinal, cheminLogo);
            }
            // =============================================================================

            // ================= AJOUT : ENVOI DE L'EMAIL AUTOMATIQUE =================
            String emailDestinataire = "jridighofrane48@gmail.com";
            String sujet = "Merci pour votre avis ! - EventFlow";
            String contenu = "Bonjour,\n\n" +
                    "Nous avons bien reçu votre avis concernant l'événement.\n" +
                    "Note : " + etoilesSelectionnees + " / 5\n" +
                    "Commentaire : " + (txtCommentaire.getText().isEmpty() ? "Aucun" : txtCommentaire.getText()) + "\n\n" +
                    "Merci de votre participation !";

            new Thread(() -> {
                com.example.pidev.utils.MailUtils.envoyerMailConfirmation(emailDestinataire, sujet, contenu);
            }).start();
            // ========================================================================

            // 2. Charger le FXML Résultat
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/questionnaire/Resultat.fxml"));
            Parent root = loader.load();

            // 3. Injection des données dans le contrôleur de résultat
            ResultatController resCtrl = loader.getController();
            resCtrl.initData(
                    dernierIdFeedback,
                    listeQuestions,
                    reponsesUtilisateur,
                    txtCommentaire.getText(),
                    etoilesSelectionnees
            );

            // 4. Navigation via la scène courante
            javafx.scene.Scene scene = btnSuivant.getScene();
            if (com.example.pidev.MainController.getInstance() != null) {
                com.example.pidev.MainController.getInstance().getPageContentContainer().getChildren().clear();
                com.example.pidev.MainController.getInstance().getPageContentContainer().getChildren().add(root);
            } else {
                VBox rootVBox = (VBox) scene.getRoot();
                if (rootVBox.getChildren().size() > 1) {
                    rootVBox.getChildren().set(1, root);
                } else {
                    rootVBox.getChildren().add(root);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Problème lors du chargement des résultats : " + e.getMessage());
        }
    }

    private void afficherAlerte(String titre, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titre);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
    private void setupEmojiPicker() {
        String[] emojis = {"👍", "❤️", "😂", "😮", "😢", "😡", "🔥", "👏", "🎉", "✅"};
        emojiPicker.getChildren().clear();
        for (String e : emojis) {
            Button b = new Button(e);
            b.setStyle("-fx-background-color: transparent; -fx-font-size: 18; -fx-cursor: hand;");
            b.setOnAction(event -> txtCommentaire.appendText(e));
            emojiPicker.getChildren().add(b);
        }
    }

    @FXML
    private void toggleEmojiPicker() {
        boolean state = !emojiPicker.isVisible();
        emojiPicker.setVisible(state);
        emojiPicker.setManaged(state);
    }
}