package com.example.pidev.controller.auth;

import com.example.pidev.HelloApplication;
import com.example.pidev.service.questionnaire.FeedbackService;
import com.example.pidev.utils.UserSession;
import javafx.application.Platform;
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
    @FXML private Button eventsBtn;
    @FXML private Button sponsorRecoBtn;
    @FXML private HBox authButtonsBox;
    @FXML private MenuButton profileMenuButton;
    @FXML private MenuItem profileMenuItem;
    @FXML private MenuItem logoutMenuItem;

    private Scene currentScene;
    private final FeedbackService feedbackService = new FeedbackService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        updateTopRightActionsBySession();
        updateSponsorAccessVisibility();
        Platform.runLater(() -> {
            updateTopRightActionsBySession();
            updateSponsorAccessVisibility();
            applyLegacyNavbarFallback();
        });
        System.out.println("LandingPageController initialise");
    }

    // ==================== NAVIGATION ====================

    @FXML
    private void handleLogin() {
        HelloApplication.loadLoginPage();
    }

    @FXML
    private void handleSignup() {
        HelloApplication.loadSignupPage();
    }

    @FXML
    private void handleGoToEvents() {
        HelloApplication.loadPublicEventsPage();
    }

    @FXML
    private void handleSponsorPortal() {
        if (!isCurrentUserSponsor()) {
            showAlert("Acces refuse", "Cette section est reservee aux sponsors.");
            return;
        }
        HelloApplication.showSponsorPortalInLanding();
    }

    @FXML
    private void handleProfile() {
        HelloApplication.loadProfilePage();
    }

    @FXML
    private void handleLogout() {
        UserSession.getInstance().clearSession();
        HelloApplication.loadLandingPage();
    }

    private void updateSponsorAccessVisibility() {
        boolean sponsor = isCurrentUserSponsor();
        if (sponsorRecoBtn != null) {
            sponsorRecoBtn.setVisible(sponsor);
            sponsorRecoBtn.setManaged(sponsor);
            sponsorRecoBtn.setDisable(!sponsor);
        }

        // Fallback si le FXML integre n'a pas le bouton sponsorRecoBtn.
        if (sponsorRecoBtn == null && eventsBtn != null) {
            if (sponsor) {
                eventsBtn.setText("Recommandations sponsor");
                eventsBtn.setOnAction(e -> handleSponsorPortal());
            } else {
                eventsBtn.setText("Evenements");
                eventsBtn.setOnAction(e -> handleGoToEvents());
            }
        }
    }

    private void updateTopRightActionsBySession() {
        boolean loggedIn = UserSession.getInstance().isLoggedIn();
        if (authButtonsBox != null) {
            authButtonsBox.setVisible(!loggedIn);
            authButtonsBox.setManaged(!loggedIn);
        }
        if (profileMenuButton != null) {
            profileMenuButton.setVisible(loggedIn);
            profileMenuButton.setManaged(loggedIn);
            if (loggedIn) {
                String fullName = UserSession.getInstance().getFullName();
                profileMenuButton.setText((fullName == null || fullName.isBlank()) ? "Profil" : fullName);
            }
        }

        // Fallback pour FXML legacy sans fx:id authButtonsBox/profileMenuButton.
        if (authButtonsBox == null || profileMenuButton == null) {
            updateLegacyAuthButtons(loggedIn);
        }
    }

    private boolean isCurrentUserSponsor() {
        String role = UserSession.getInstance().getRole();
        if ((role == null || role.isBlank()) && UserSession.getInstance().getCurrentUser() != null
                && UserSession.getInstance().getCurrentUser().getRole() != null) {
            role = UserSession.getInstance().getCurrentUser().getRole().getRoleName();
        }
        return role != null && role.trim().toLowerCase().contains("sponsor");
    }

    private void applyLegacyNavbarFallback() {
        if (mainScrollPane == null) {
            return;
        }
        Parent root = mainScrollPane.getParent();
        if (!(root instanceof VBox pageRoot) || pageRoot.getChildren().isEmpty()) {
            return;
        }
        if (!(pageRoot.getChildren().get(0) instanceof HBox navBar)) {
            return;
        }

        HBox linksHBox = null;
        for (javafx.scene.Node node : navBar.getChildren()) {
            if (node instanceof HBox candidate) {
                boolean hasEvents = candidate.getChildren().stream()
                        .anyMatch(n -> n instanceof Button b && normalizeButtonText(b.getText()).contains("evenement"));
                boolean hasContact = candidate.getChildren().stream()
                        .anyMatch(n -> n instanceof Button b && normalizeButtonText(b.getText()).contains("contact"));
                if (hasEvents && hasContact) {
                    linksHBox = candidate;
                    break;
                }
            }
        }
        if (linksHBox == null) {
            return;
        }

        Button recoButton = null;
        for (javafx.scene.Node n : linksHBox.getChildren()) {
            if (n instanceof Button b && normalizeButtonText(b.getText()).contains("recommand")) {
                recoButton = b;
                break;
            }
        }

        if (isCurrentUserSponsor()) {
            if (recoButton == null) {
                Button btn = new Button("Recommandations sponsor");
                btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #0D47A1; -fx-font-size: 16px; -fx-font-weight: 700; -fx-cursor: hand;");
                btn.setOnAction(e -> handleSponsorPortal());
                linksHBox.getChildren().add(2, btn);
            } else {
                recoButton.setVisible(true);
                recoButton.setManaged(true);
                recoButton.setOnAction(e -> handleSponsorPortal());
            }
        } else if (recoButton != null) {
            linksHBox.getChildren().remove(recoButton);
        }
    }

    private void updateLegacyAuthButtons(boolean loggedIn) {
        if (mainScrollPane == null) {
            return;
        }
        Parent root = mainScrollPane.getParent();
        if (!(root instanceof VBox pageRoot) || pageRoot.getChildren().isEmpty()) {
            return;
        }
        if (!(pageRoot.getChildren().get(0) instanceof HBox navBar)) {
            return;
        }

        HBox authBox = null;
        for (javafx.scene.Node node : navBar.getChildren()) {
            if (node instanceof HBox candidate) {
                long countAuth = candidate.getChildren().stream()
                        .filter(n -> n instanceof Button b &&
                                (normalizeButtonText(b.getText()).contains("connexion")
                                        || normalizeButtonText(b.getText()).contains("inscription")
                                        || normalizeButtonText(b.getText()).contains("profil")
                                        || normalizeButtonText(b.getText()).contains("deconnexion")))
                        .count();
                if (countAuth >= 2) {
                    authBox = candidate;
                    break;
                }
            }
        }

        if (authBox == null) {
            return;
        }

        List<Button> buttons = authBox.getChildren().stream()
                .filter(n -> n instanceof Button)
                .map(n -> (Button) n)
                .toList();
        if (buttons.size() < 2) {
            return;
        }

        Button first = buttons.get(0);
        Button second = buttons.get(1);
        if (loggedIn) {
            String fullName = UserSession.getInstance().getFullName();
            first.setText((fullName == null || fullName.isBlank()) ? "Profil" : fullName);
            first.setOnAction(e -> handleProfile());
            second.setText("Deconnexion");
            second.setOnAction(e -> handleLogout());
        } else {
            first.setText("Connexion");
            first.setOnAction(e -> handleLogin());
            second.setText("Inscription");
            second.setOnAction(e -> handleSignup());
        }
    }

    private String normalizeButtonText(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase()
                .replace("ÃƒÆ’Ã‚Â©", "e")
                .replace("ÃƒÆ’Ã‚Â¨", "e")
                .replace("ÃƒÆ’Ã‚Âª", "e")
                .replace("ÃƒÆ’Ã‚Â ", "a");
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
        System.out.println("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¶ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¯ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â Ouverture de la vidÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©o de dÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©monstration...");
        playVideo("/com/example/pidev/videos/MÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©dia1.mp4");
    }

    private void playVideo(String videoPath) {
        try {
            URL videoUrl = getClass().getResource(videoPath);
            if (videoUrl == null) {
                showAlert("Erreur", "VidÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©o non trouvÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©e: " + videoPath);
                return;
            }
            Stage videoStage = new Stage();
            videoStage.setTitle("EventFlow - VidÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©o de dÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©monstration");
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
            showAlert("Erreur", "Impossible de lire la vidÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©o: " + e.getMessage());
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
                System.out.println("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¦ VidÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©o chargÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©e avec succÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¨s");
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
        Label titleLabel = new Label("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â½ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¬ DÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©monstration EventFlow");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚Â¢");
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
                    Votre navigateur ne supporte pas la lecture de vidÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©os.
                </video>
            </body>
            </html>
            """, videoUrl);
    }

    private VBox createVideoInfoBox() {
        VBox infoBox = new VBox(10);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setPadding(new Insets(20, 0, 0, 0));
        Label infoTitle = new Label("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â¨ DÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©couvrez EventFlow en action");
        infoTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label infoText = new Label(
                "Cette dÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©monstration vous montre comment :\n" +
                        "ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã¢â‚¬Å“ CrÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©er et gÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©rer vos ÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©vÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©nements en quelques clics\n" +
                        "ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã¢â‚¬Å“ Ajouter des participants et suivre leurs inscriptions\n" +
                        "ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã¢â‚¬Å“ GÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©rer vos sponsors et leurs contrats\n" +
                        "ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã¢â‚¬Å“ Visualiser les statistiques en temps rÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©el\n" +
                        "ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â¢ÃƒÆ’Ã¢â‚¬Â¦ÃƒÂ¢Ã¢â€šÂ¬Ã…â€œÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã¢â‚¬Å“ GÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©nÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©rer des rapports dÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©taillÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©s"
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
        Button replayBtn = new Button("ÃƒÆ’Ã†â€™Ãƒâ€šÃ‚Â°ÃƒÆ’Ã¢â‚¬Â¦Ãƒâ€šÃ‚Â¸ÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€šÃ‚ÂÃƒÆ’Ã‚Â¢ÃƒÂ¢Ã¢â‚¬Å¡Ã‚Â¬Ãƒâ€¦Ã‚Â¾ Revoir la dÃƒÆ’Ã†â€™Ãƒâ€ Ã¢â‚¬â„¢ÃƒÆ’Ã¢â‚¬Å¡Ãƒâ€šÃ‚Â©mo");
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

    // ==================== FEEDBACK (Ghofrane) ====================

    @FXML
    private void handleFeedback() {
        try {
            VBox root = resolveRootContainer();
            if (root == null) {
                showAlert("Erreur", "Impossible d'afficher la section feedback.");
                return;
            }
            currentScene = root.getScene();
            VBox feedbackContenu = construireFeedbackContenu();
            if (root.getChildren().size() > 1) {
                root.getChildren().set(1, feedbackContenu);
            } else {
                root.getChildren().add(feedbackContenu);
            }
        } catch (Exception e) {
            e.printStackTrace();
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

        Label titrePage = new Label("Avis des participants");
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
        String[] labels = {"5 etoiles", "4 etoiles", "3 etoiles", "2 etoiles", "1 etoile"};
        int maxVal = total > 0 ? total : 1;
        for (int i = 5; i >= 1; i--) {
            int nb = repartition.getOrDefault(i, 0);
            HBox ligne = new HBox(10);
            ligne.setAlignment(Pos.CENTER_LEFT);
            Label lblNom = new Label(labels[5 - i]);
            lblNom.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-pref-width: 55;");
            Label lblStar = new Label("\u2605");
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
            Label s = new Label(i < (int) Math.round(moyenne) ? "\u2605" : "\u2606");
            s.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 20px;");
            etoilesGlobales.getChildren().add(s);
        }
        Label lblTotal = new Label(total + " notes");
        lblTotal.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14px;");
        noteGlobale.getChildren().addAll(lblMoy, etoilesGlobales, lblTotal);
        blocStats.getChildren().addAll(barresBox, noteGlobale);

        Label titreListe = new Label("Avis recents");
        titreListe.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

        VBox listeFeedbacks = new VBox(12);
        List<Map<String, Object>> feedbacks = feedbackService.getFeedbacksAvecDetails();
        String eventActuel = "";
        for (Map<String, Object> fb : feedbacks) {
            String nomEvent = (String) fb.get("nomEvent");
            if (nomEvent == null) nomEvent = "Evenement inconnu";
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
        Label icon = new Label("Evenement");
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
            Label star = new Label(i < etoiles ? "\u2605" : "\u2606");
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

    // ==================== CERTIFICAT (Ghofrane) ====================

    @FXML
    private void handleGetCertificate() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/questionnaire/Participant.fxml")
            );
            Parent root = loader.load();
            VBox rootVBox = resolveRootContainer();
            if (rootVBox == null) {
                showAlert("Erreur", "Impossible d'afficher le questionnaire.");
                return;
            }
            currentScene = rootVBox.getScene();
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

    private VBox resolveRootContainer() {
        Scene scene = null;
        if (mainScrollPane != null) {
            scene = mainScrollPane.getScene();
        }
        if (scene == null && homeSection != null) {
            scene = homeSection.getScene();
        }
        if (scene == null && HelloApplication.getPrimaryStage() != null) {
            scene = HelloApplication.getPrimaryStage().getScene();
        }
        if (scene != null && scene.getRoot() instanceof VBox root) {
            return root;
        }
        return null;
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

