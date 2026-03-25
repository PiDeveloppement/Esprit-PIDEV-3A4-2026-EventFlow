package com.example.pidev.controller.auth;

import com.example.pidev.HelloApplication;
import com.example.pidev.service.questionnaire.FeedbackService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.concurrent.Worker;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class LandingPageController implements Initializable {

    @FXML private ScrollPane mainScrollPane;
    @FXML private VBox homeSection;
    @FXML private VBox featuresSection;
    @FXML private VBox contactSection;

    private Scene currentScene;
    private final FeedbackService feedbackService = new FeedbackService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("✅ LandingPageController initialisé");
    }

    // ==================== NAVIGATION ====================

    @FXML
    private void handleLogin() {
        System.out.println("📂 Redirection vers la page de connexion");
        HelloApplication.loadLoginPage();
    }

    @FXML
    private void handleSignup() {
        System.out.println("📂 Redirection vers la page d'inscription");
        HelloApplication.loadSignupPage();
    }

    @FXML
    private void handleGoToEvents() {
        System.out.println("📂 Redirection vers la page des événements");
        HelloApplication.loadPublicEventsPage();
    }

    // ==================== SCROLL ====================

    @FXML
    private void scrollToTop() {
        if (currentScene == null) {
            if (mainScrollPane != null) mainScrollPane.setVvalue(0);
        } else {
            if (mainScrollPane.getScene() != null) {
                mainScrollPane.setVvalue(0);
                currentScene = null;
            } else {
                retourLandingPage();
            }
        }
    }

    @FXML
    private void scrollToFeatures() {
        if (mainScrollPane != null) mainScrollPane.setVvalue(0.35);
    }

    @FXML
    private void scrollToContact() {
        if (mainScrollPane != null) mainScrollPane.setVvalue(0.85);
    }

    // ==================== ANIMATION CARTES ====================

    @FXML
    private void animateCard(javafx.scene.input.MouseEvent event) {
        StackPane card = (StackPane) event.getSource();
        card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 20; -fx-padding: 30; " +
                "-fx-effect: dropshadow(gaussian, rgba(13,71,161,0.3), 20, 0, 0, 10); " +
                "-fx-cursor: hand; -fx-scale-x: 1.02; -fx-scale-y: 1.02; " +
                "-fx-transition: all 0.3s ease-in-out;");
    }

    @FXML
    private void resetCard(javafx.scene.input.MouseEvent event) {
        StackPane card = (StackPane) event.getSource();
        card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 20; -fx-padding: 30; " +
                "-fx-cursor: hand; -fx-effect: null; -fx-scale-x: 1; -fx-scale-y: 1;");
    }

    // ==================== DEMO VIDEO ====================

    @FXML
    private void handleDemo() {
        System.out.println("▶️ Ouverture de la vidéo de démonstration...");
        playVideo("/com/example/pidev/videos/Média1.mp4");
    }

    private void playVideo(String videoPath) {
        try {
            URL videoUrl = getClass().getResource(videoPath);
            if (videoUrl == null) {
                showAlert("Erreur", "Vidéo non trouvée: " + videoPath);
                return;
            }
            Stage videoStage = new Stage();
            videoStage.setTitle("EventFlow - Vidéo de démonstration");
            videoStage.initModality(Modality.APPLICATION_MODAL);
            videoStage.setWidth(1000);
            videoStage.setHeight(650);
            videoStage.setResizable(false);
            VBox mainContainer = new VBox(20);
            mainContainer.setStyle("-fx-background-color: #0A1929; -fx-padding: 30;");
            mainContainer.setAlignment(Pos.TOP_CENTER);
            mainContainer.getChildren().addAll(
                    createVideoHeader(videoStage),
                    createVideoPlayer(videoUrl.toExternalForm()),
                    createVideoInfoBox(),
                    createVideoActionBox(videoStage, videoPath)
            );
            videoStage.setScene(new Scene(mainContainer));
            videoStage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de lire la vidéo: " + e.getMessage());
        }
    }

    private VBox createVideoPlayer(String videoUrl) {
        VBox videoContainer = new VBox();
        videoContainer.setStyle("-fx-background-color: #000000; -fx-background-radius: 12; " +
                "-fx-border-color: #3b82f6; -fx-border-width: 2; -fx-border-radius: 12;");
        videoContainer.setPrefHeight(400);
        videoContainer.setAlignment(Pos.CENTER);
        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setStyle("-fx-progress-color: #3b82f6;");
        loadingIndicator.setVisible(true);
        loadingIndicator.setMaxSize(60, 60);
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webView.setPrefHeight(400);
        webView.setPrefWidth(900);
        webView.setVisible(false);
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.RUNNING) {
                loadingIndicator.setVisible(true);
                webView.setVisible(false);
            } else if (newState == Worker.State.SUCCEEDED) {
                loadingIndicator.setVisible(false);
                webView.setVisible(true);
                System.out.println("✅ Vidéo chargée avec succès");
            } else if (newState == Worker.State.FAILED) {
                loadingIndicator.setVisible(false);
                webView.setVisible(true);
            }
        });
        webEngine.loadContent(createVideoHTML(videoUrl));
        videoContainer.getChildren().addAll(loadingIndicator, webView);
        return videoContainer;
    }

    private HBox createVideoHeader(Stage videoStage) {
        HBox headerBox = new HBox(15);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        headerBox.setPadding(new Insets(0, 0, 20, 0));
        Label titleLabel = new Label("🎬 Démonstration EventFlow");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 24px; " +
                "-fx-cursor: hand; -fx-padding: 5 10; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> videoStage.close());
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle(
                "-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-size: 24px; " +
                        "-fx-cursor: hand; -fx-padding: 5 10; -fx-font-weight: bold; -fx-background-radius: 5;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size: 24px; " +
                        "-fx-cursor: hand; -fx-padding: 5 10; -fx-font-weight: bold;"));
        headerBox.getChildren().addAll(titleLabel, spacer, closeBtn);
        return headerBox;
    }

    private String createVideoHTML(String videoUrl) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body, html {
                        margin: 0; padding: 0;
                        width: 100%%; height: 100%%;
                        overflow: hidden; background: #000000;
                    }
                    video {
                        width: 100%%; height: 100%%;
                        object-fit: contain; background: #000000;
                    }
                </style>
            </head>
            <body>
                <video controls autoplay>
                    <source src="%s" type="video/mp4">
                    Votre navigateur ne supporte pas la lecture de vidéos.
                </video>
            </body>
            </html>
            """, videoUrl);
    }

    private VBox createVideoInfoBox() {
        VBox infoBox = new VBox(10);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setPadding(new Insets(20, 0, 0, 0));
        Label infoTitle = new Label("✨ Découvrez EventFlow en action");
        infoTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label infoText = new Label(
                "Cette démonstration vous montre comment :\n" +
                        "✓ Créer et gérer vos événements en quelques clics\n" +
                        "✓ Ajouter des participants et suivre leurs inscriptions\n" +
                        "✓ Gérer vos sponsors et leurs contrats\n" +
                        "✓ Visualiser les statistiques en temps réel\n" +
                        "✓ Générer des rapports détaillés"
        );
        infoText.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 14px; -fx-line-spacing: 5;");
        infoText.setWrapText(true);
        infoBox.getChildren().addAll(infoTitle, infoText);
        return infoBox;
    }

    private HBox createVideoActionBox(Stage videoStage, String videoPath) {
        HBox actionBox = new HBox(15);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        actionBox.setPadding(new Insets(20, 0, 0, 0));
        Button replayBtn = new Button("🔄 Revoir la démo");
        replayBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand; " +
                "-fx-border-color: #3b82f6; -fx-border-width: 1.5; -fx-font-size: 14px;");
        replayBtn.setOnMouseEntered(e -> replayBtn.setStyle(
                "-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand; " +
                        "-fx-border-color: #2563eb; -fx-border-width: 1.5; -fx-font-size: 14px; " +
                        "-fx-effect: dropshadow(gaussian, #3b82f6, 10, 0, 0, 0);"));
        replayBtn.setOnMouseExited(e -> replayBtn.setStyle(
                "-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-padding: 12 25; -fx-background-radius: 8; -fx-cursor: hand; " +
                        "-fx-border-color: #3b82f6; -fx-border-width: 1.5; -fx-font-size: 14px;"));
        replayBtn.setOnAction(e -> { videoStage.close(); playVideo(videoPath); });
        actionBox.getChildren().add(replayBtn);
        return actionBox;
    }

    // ==================== FEEDBACK ====================

    @FXML
    private void handleFeedback() {
        try {
            System.out.println("📂 Chargement du NOUVEAU FeedbackView.fxml");

            // Charger le nouveau FXML depuis le dossier questionnaire
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/questionnaire/FeedbackView.fxml")
            );

            VBox feedbackContenu = loader.load(); // C'est déjà un VBox

            // Récupérer la scène actuelle
            Scene scene = homeSection.getScene();
            if (scene == null) {
                System.err.println("❌ Scène non disponible");
                showAlert("Erreur", "Impossible d'afficher les feedbacks");
                return;
            }

            VBox root = (VBox) scene.getRoot();

            // Remplacer le contenu (généralement l'index 1 est le contenu principal)
            if (root.getChildren().size() > 1) {
                root.getChildren().set(1, feedbackContenu);
            } else {
                root.getChildren().add(feedbackContenu);
            }

            System.out.println("✅ Nouveau FeedbackView chargé avec succès");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement du FeedbackView:");
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la page des feedbacks: " + e.getMessage());
        }
    }

    private VBox construireFeedbackContenu() throws SQLException {
        VBox page = new VBox(0);
        page.setStyle("-fx-background-color: #f8fafc;");
        VBox.setVgrow(page, Priority.ALWAYS);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox inner = new VBox(30);
        inner.setStyle("-fx-padding: 40; -fx-background-color: #f8fafc;");

        Label titrePage = new Label("⭐  Feedbacks des participants");
        titrePage.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #0A1929;");

        Map<String, Object> stats = feedbackService.getStatistiquesDetaillees();
        double moyenne = (double) stats.get("moyenne");
        int total = (int) stats.get("total");
        @SuppressWarnings("unchecked")
        Map<Integer, Integer> repartition = (Map<Integer, Integer>) stats.get("repartition");

        HBox blocStats = new HBox(40);
        blocStats.setStyle("-fx-background-color: white; -fx-padding: 30; -fx-background-radius: 15; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 10, 0, 0, 4);");
        blocStats.setAlignment(Pos.CENTER_LEFT);

        VBox barresBox = new VBox(12);
        HBox.setHgrow(barresBox, Priority.ALWAYS);
        String[] labels = {"FIVE", "FOUR", "THREE", "TWO", "ONE"};
        int maxVal = total > 0 ? total : 1;
        for (int i = 5; i >= 1; i--) {
            int nb = repartition.getOrDefault(i, 0);
            HBox ligne = new HBox(10);
            ligne.setAlignment(Pos.CENTER_LEFT);
            Label lblNom = new Label(labels[5 - i]);
            lblNom.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-pref-width: 55;");
            Label lblStar = new Label("★");
            lblStar.setStyle("-fx-text-fill: #f59e0b;");
            ProgressBar bar = new ProgressBar((double) nb / maxVal);
            bar.setPrefWidth(260); bar.setPrefHeight(12);
            bar.setStyle("-fx-accent: #f59e0b;");
            Label lblNb = new Label(String.valueOf(nb));
            lblNb.setStyle("-fx-text-fill: #64748b; -fx-pref-width: 45;");
            ligne.getChildren().addAll(lblNom, lblStar, bar, lblNb);
            barresBox.getChildren().add(ligne);
        }

        VBox noteGlobale = new VBox(8);
        noteGlobale.setAlignment(Pos.CENTER);
        noteGlobale.setStyle("-fx-background-color: #fffbeb; -fx-padding: 25; " +
                "-fx-background-radius: 12; -fx-pref-width: 180; -fx-min-width: 180;");
        Label lblMoy = new Label(String.format("%.1f", moyenne));
        lblMoy.setStyle("-fx-font-size: 52px; -fx-font-weight: bold; -fx-text-fill: #f59e0b;");
        HBox etoilesGlobales = new HBox(3);
        etoilesGlobales.setAlignment(Pos.CENTER);
        for (int i = 0; i < 5; i++) {
            Label s = new Label(i < (int) Math.round(moyenne) ? "★" : "☆");
            s.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 20px;");
            etoilesGlobales.getChildren().add(s);
        }
        Label lblTotal = new Label(total + " Ratings");
        lblTotal.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        noteGlobale.getChildren().addAll(lblMoy, etoilesGlobales, lblTotal);
        blocStats.getChildren().addAll(barresBox, noteGlobale);

        Label titreListe = new Label("Recent Feedbacks");
        titreListe.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        VBox listeFeedbacks = new VBox(12);
        List<Map<String, Object>> feedbacks = feedbackService.getFeedbacksAvecDetails();
        String eventActuel = "";
        for (Map<String, Object> fb : feedbacks) {
            String nomEvent = (String) fb.get("nomEvent");
            if (nomEvent == null) nomEvent = "Événement inconnu";
            if (!nomEvent.equals(eventActuel)) {
                eventActuel = nomEvent;
                listeFeedbacks.getChildren().add(creerTitreEvent(nomEvent));
            }
            listeFeedbacks.getChildren().add(creerCarteFeedback(fb));
        }

        inner.getChildren().addAll(titrePage, blocStats, titreListe, listeFeedbacks);
        scroll.setContent(inner);
        page.getChildren().add(scroll);
        return page;
    }

    private HBox creerTitreEvent(String nomEvent) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setStyle("-fx-padding: 15 0 8 0;");
        Label icon = new Label("📅");
        icon.setStyle("-fx-font-size: 18px;");
        Label titre = new Label(nomEvent);
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Separator sep = new Separator();
        HBox.setHgrow(sep, Priority.ALWAYS);
        box.getChildren().addAll(icon, titre, sep);
        return box;
    }

    private HBox creerCarteFeedback(Map<String, Object> fb) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 12; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 3);");
        String firstName = fb.get("firstName") != null ? (String) fb.get("firstName") : "";
        String lastName  = fb.get("lastName")  != null ? (String) fb.get("lastName")  : "";
        String initiale  = !firstName.isEmpty() ? String.valueOf(firstName.charAt(0)).toUpperCase() : "U";
        StackPane avatar = new StackPane();
        avatar.setPrefSize(55, 55); avatar.setMinSize(55, 55);
        avatar.setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 50;");
        Label lblInit = new Label(initiale);
        lblInit.setStyle("-fx-font-weight: bold; -fx-font-size: 22px; -fx-text-fill: #0D47A1;");
        avatar.getChildren().add(lblInit);
        VBox content = new VBox(6);
        HBox.setHgrow(content, Priority.ALWAYS);
        HBox nameAndStars = new HBox(15);
        nameAndStars.setAlignment(Pos.CENTER_LEFT);
        String nomAffiche = (firstName + " " + lastName).trim();
        if (nomAffiche.isEmpty()) nomAffiche = "Utilisateur";
        Label lblNom = new Label(nomAffiche);
        lblNom.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #1e293b;");
        int etoiles = (int) fb.get("etoiles");
        HBox starsBox = new HBox(2);
        for (int i = 0; i < 5; i++) {
            Label star = new Label(i < etoiles ? "★" : "☆");
            star.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 15px;");
            starsBox.getChildren().add(star);
        }
        nameAndStars.getChildren().addAll(lblNom, starsBox);
        String comments = (String) fb.get("comments");
        Label lblComment = new Label(comments != null ? comments : "Aucun commentaire.");
        lblComment.setWrapText(true);
        lblComment.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        content.getChildren().addAll(nameAndStars, lblComment);
        card.getChildren().addAll(avatar, content);
        return card;
    }

    // ==================== CERTIFICAT ====================

    @FXML
    private void handleGetCertificate() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/questionnaire/Participant.fxml")
            );
            Parent root = loader.load();
            currentScene = homeSection.getScene();
            VBox rootVBox = (VBox) currentScene.getRoot();
            if (rootVBox.getChildren().size() > 1) {
                rootVBox.getChildren().set(1, root);
            } else {
                rootVBox.getChildren().add(root);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le questionnaire : " + e.getMessage());
        }
    }

    // ==================== RETOUR LANDING PAGE ====================

    private void retourLandingPage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/auth/LandingPage.fxml")
            );
            VBox newRoot = loader.load();
            VBox currentRoot = (VBox) currentScene.getRoot();
            if (newRoot.getChildren().size() > 1) {
                currentRoot.getChildren().set(1, newRoot.getChildren().get(1));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ==================== UTILITAIRES ====================

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}