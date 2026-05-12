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
import java.util.Collections;
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
    @FXML private VBox vboxOptions;
    @FXML private ProgressBar pgBar;
    @FXML private Label lblScoreSide, lblScoreDetail;

    private List<Question> listeQuestions = new ArrayList<>();
    private final List<String> reponsesUtilisateur = new ArrayList<>();
    private int indexActuel = 0;
    private int etoilesSelectionnees = 0;
    private String reponseSelectionnee = "";
    // Simulation de l'utilisateur connecté (A remplacer par votre session utilisateur)
    private final int idParticipantConnecte = 1;
    private final int idEventActuel = 1;

    private final FeedbackService fs = new FeedbackService();
    private final CertificateService certificateService = new CertificateService();
    private final com.example.pidev.service.questionnaire.BadWordService badWordService = new com.example.pidev.service.questionnaire.BadWordService();
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

    @FXML private VBox optionsContainer; // Liez ceci au FXML
    private ToggleGroup groupOptions;
    @FXML
    private void afficherQuestion() {
        Question q = listeQuestions.get(indexActuel);

        // 1. Calcul et mise à jour de la barre de progression (Header)
        double progression = (double) indexActuel / listeQuestions.size();
        if (pgBar != null) pgBar.setProgress(progression);
        lblProgression.setText((int)(progression * 100) + "%");

        lblQuestion.setText(q.getTexte());

        // 2. Mise à jour Sidebar & Score Card
        rafraichirSidebar();

        // 3. Options stylisées (Cartes)
        vboxOptions.getChildren().clear();
        reponseSelectionnee = "";

        List<String> choix = new ArrayList<>(q.getOptions());
        Collections.shuffle(choix);

        // ... (reste du code)
        for (String option : choix) {
            Button btnOpt = new Button(option);
            btnOpt.setMaxWidth(Double.MAX_VALUE);

            String styleNormal = "-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-padding: 15; -fx-alignment: CENTER_LEFT; -fx-cursor: hand; -fx-font-size: 14px;";
            String styleSelected = "-fx-background-color: #eff6ff; -fx-border-color: #2563eb; -fx-border-width: 2; -fx-border-radius: 8; -fx-padding: 15; -fx-alignment: CENTER_LEFT; -fx-font-size: 14px; -fx-font-weight: bold;";

            btnOpt.setStyle(styleNormal);

            btnOpt.setOnAction(e -> {
                // 1. On réinitialise le style de TOUTES les cartes d'options
                vboxOptions.getChildren().forEach(n -> n.setStyle(styleNormal));

                // 2. On applique le style bleu à la carte cliquée
                btnOpt.setStyle(styleSelected);

                // 3. LA CORRECTION : On enregistre le texte de l'option cliquée
                // Sans cette ligne, reponseSelectionnee reste vide et déclenche l'alerte !
                this.reponseSelectionnee = option;
            });

            vboxOptions.getChildren().add(btnOpt);
        }
// ...

        boolean estDerniere = (indexActuel == listeQuestions.size() - 1);
        vboxEvaluation.setVisible(estDerniere);
        vboxEvaluation.setManaged(estDerniere);
        btnSuivant.setText(estDerniere ? "TERMINER LE QUIZ" : "Suivant →");
    }
    @FXML
    private void handleSuivant() {
        // 1. On vérifie si une option est sélectionnée
        if (reponseSelectionnee == null || reponseSelectionnee.isEmpty()) {
            afficherAlerte("Sélection requise", "Veuillez choisir une réponse.");
            return;
        }

        // 2. On ajoute TOUJOURS la réponse sélectionnée à la liste
        reponsesUtilisateur.add(reponseSelectionnee);

        if (indexActuel < listeQuestions.size() - 1) {
            // Cas normal : on passe à la question suivante
            indexActuel++;
            afficherQuestion();
        } else {
            // Cas de la DERNIÈRE question (Bouton Terminer)
            if (etoilesSelectionnees == 0) {
                // Si l'utilisateur a oublié les étoiles, on retire la réponse
                // pour ne pas l'ajouter en double au prochain essai
                reponsesUtilisateur.remove(reponsesUtilisateur.size() - 1);
                afficherAlerte("Note requise", "Merci de donner une note (étoiles).");
                return;
            }
            // OK : on sauvegarde tout
            sauvegarderEtChangerPage();
        }
    }

    private void sauvegarderEtChangerPage() {
        try {
            int dernierIdFeedback = 0;
            int nombreBonnesReponses = 0;

            // --- 1. FILTRAGE DES BAD WORDS (Appel API PurgoMalum) ---
            // On récupère le texte, on le filtre, et on utilise cette version partout
            String commentaireBrut = txtCommentaire.getText();
            String commentaireFiltre = badWordService.filtrerTexte(commentaireBrut);

            // --- 2. SAUVEGARDE EN BASE DE DONNÉES ET CALCUL DU SCORE ---
            for (int i = 0; i < listeQuestions.size(); i++) {
                Question q = listeQuestions.get(i);
                String repDonnee = reponsesUtilisateur.get(i);

                if (repDonnee.equalsIgnoreCase(q.getReponse())) {
                    nombreBonnesReponses++;
                }

                // Utilisation du commentaireFiltre pour la base de données
                dernierIdFeedback = fs.enregistrerFeedbackComplet(
                        idParticipantConnecte,
                        idEventActuel,
                        q.getIdQuestion(),
                        repDonnee,
                        commentaireFiltre,
                        etoilesSelectionnees
                );
            }

            // --- 3. GÉNÉRATION DU CERTIFICAT SI ADMIS ---
            if (nombreBonnesReponses >= (listeQuestions.size() / 2.0)) {
                com.example.pidev.service.questionnaire.CertificateService certService = new com.example.pidev.service.questionnaire.CertificateService();
                String nomGagnant = fs.getNomUserComplet(idParticipantConnecte);
                String scoreFinal = nombreBonnesReponses + " / " + listeQuestions.size();
                String cheminLogo = "C:\\Users\\USER\\Desktop\\pigestion\\src\\main\\java\\com\\example\\pidev\\images\\logo.png";

                certService.genererCertificat(nomGagnant, scoreFinal, cheminLogo);
            }

            // --- 4. ENVOI DE L'EMAIL AUTOMATIQUE (Version Filtrée) ---
            String emailDestinataire = "jridighofrane48@gmail.com";
            String sujet = "Merci pour votre avis ! - EventFlow";
            String contenu = "Bonjour,\n\n" +
                    "Nous avons bien reçu votre avis concernant l'événement.\n" +
                    "Note : " + etoilesSelectionnees + " / 5\n" +
                    "Commentaire : " + (commentaireFiltre.isEmpty() ? "Aucun" : commentaireFiltre) + "\n\n" +
                    "Merci de votre participation !";

            new Thread(() -> {
                com.example.pidev.utils.MailUtils.envoyerMailConfirmation(emailDestinataire, sujet, contenu);
            }).start();

            // --- 5. CHARGEMENT DE LA PAGE RÉSULTAT ---
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/questionnaire/Resultat.fxml"));
            Parent root = loader.load();

            // Injection des données filtrées dans le contrôleur de résultat
            ResultatController resCtrl = loader.getController();
            resCtrl.initData(
                    dernierIdFeedback,
                    listeQuestions,
                    reponsesUtilisateur,
                    commentaireFiltre, // On affiche la version censurée
                    etoilesSelectionnees
            );

            // --- 6. NAVIGATION ---
            javafx.scene.Scene scene = btnSuivant.getScene();
            if (com.example.pidev.MainController.getInstance() != null) {
                com.example.pidev.MainController.getInstance().getPageContentContainer().getChildren().clear();
                com.example.pidev.MainController.getInstance().getPageContentContainer().getChildren().add(root);
            } else {
                // Fallback si le MainController n'est pas dispo
                if (scene.getRoot() instanceof VBox rootVBox) {
                    if (rootVBox.getChildren().size() > 1) {
                        rootVBox.getChildren().set(1, root);
                    } else {
                        rootVBox.getChildren().add(root);
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            afficherAlerte("Erreur", "Problème lors du traitement : " + e.getMessage());
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
    // Dans ParticipantController.java

    @FXML
    private VBox sidebarQuestionsVBox; // Assure-toi d'ajouter fx:id="sidebarQuestionsVBox" dans ton FXML sur la VBox du ScrollPane de droite


    @FXML
    private void rafraichirSidebar() {
        if (sidebarQuestionsVBox == null) return;
        sidebarQuestionsVBox.getChildren().clear();

        int bonnesReponsesTemp = 0;

        for (int i = 0; i < listeQuestions.size(); i++) {
            HBox item = new HBox(10);
            item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            item.setPadding(new javafx.geometry.Insets(10, 14, 10, 14));
            item.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 1;");

            Label icon = new Label();
            Label text = new Label("Question " + (i + 1));

            if (i < indexActuel) {
                // Comparaison pour l'icône de la sidebar
                boolean correct = reponsesUtilisateur.get(i).equalsIgnoreCase(listeQuestions.get(i).getReponse());
                if(correct) bonnesReponsesTemp++;

                icon.setText(correct ? "✅" : "❌");
                item.setStyle(item.getStyle() + (correct ? "-fx-background-color: #f0fdf4; -fx-border-color: #bbf7d0;" : "-fx-background-color: #fff1f2; -fx-border-color: #fecdd3;"));
            } else if (i == indexActuel) {
                icon.setText("⬜");
                item.setStyle(item.getStyle() + "-fx-background-color: white; -fx-border-color: #2563eb; -fx-border-width: 2;");
            } else {
                icon.setText("⬜");
                icon.setOpacity(0.4);
                item.setStyle(item.getStyle() + "-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0;");
            }

            item.getChildren().addAll(icon, text);
            sidebarQuestionsVBox.getChildren().add(item);
        }

        // Mise à jour de la Score Card (Panneau latéral droit)
        if (lblScoreSide != null) {
            double pourcentage = (indexActuel == 0) ? 0 : ((double) bonnesReponsesTemp / indexActuel) * 100;
            lblScoreSide.setText(String.format("%.1f%%", pourcentage));
            lblScoreDetail.setText(bonnesReponsesTemp + " réponse(s) correcte(s)");
        }
    }


}