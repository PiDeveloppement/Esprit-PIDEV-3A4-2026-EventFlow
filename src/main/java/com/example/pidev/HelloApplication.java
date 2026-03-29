package com.example.pidev;

import atlantafx.base.theme.PrimerLight;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.utils.UserSession;
import javafx.application.Platform;
import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

public class HelloApplication extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/auth/landingPage.fxml")
            );
            if (loader.getLocation() == null) {
                throw new IllegalArgumentException("landingPage.fxml non trouvÃ©");
            }

            Parent root = loader.load();
            stage.initStyle(StageStyle.DECORATED);

            // Taille initiale adaptÃ©e Ã  l'Ã©cran
            Rectangle2D screen = Screen.getPrimary().getVisualBounds();
            double w = Math.min(1400, screen.getWidth()  * 0.92);
            double h = Math.min(900,  screen.getHeight() * 0.92);

            Scene scene = new Scene(root, w, h);
            applyCSS(scene);

            stage.setTitle("EventFlow - Plateforme de gestion d evenements");
            stage.setScene(scene);
            stage.setMinWidth(960);
            stage.setMinHeight(680);
            centerStage(w, h);
            stage.show();

        } catch (Exception e) {
            System.err.println("âŒ Erreur dÃ©marrage : " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  HELPERS INTERNES
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Applique le CSS sur une scÃ¨ne */
    private static void applyCSS(Scene scene) {
        var cssUrl = HelloApplication.class
                .getResource("/com/example/pidev/css/atlantafx-custom.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
    }

    /**
     * RÃ©utilise la scÃ¨ne existante en changeant juste la racine (root).
     * â†’ Les dimensions de la fenÃªtre ne changent JAMAIS.
     * â†’ Pas de scintillement, pas de redimensionnement.
     */
    private static void navigateTo(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource(fxmlPath)
            );
            Parent root = loader.load();
            postConfigureView(root, fxmlPath);

            // âœ… ON RÃ‰UTILISE LA SCÃˆNE EXISTANTE â€” pas de new Scene()
            Scene currentScene = primaryStage.getScene();
            if (currentScene != null) {
                // S'assurer que le CSS est bien appliquÃ©
                if (!currentScene.getStylesheets().stream().anyMatch(s -> s.contains("atlantafx-custom"))) {
                    applyCSS(currentScene);
                }
                currentScene.setRoot(root);
            } else {
                // Cas rare : pas de scÃ¨ne existante
                Rectangle2D screen = Screen.getPrimary().getVisualBounds();
                double w = Math.min(1400, screen.getWidth()  * 0.92);
                double h = Math.min(900,  screen.getHeight() * 0.92);
                Scene scene = new Scene(root, w, h);
                applyCSS(scene);
                primaryStage.setScene(scene);
            }

            primaryStage.setTitle(title);
            primaryStage.show();

        } catch (Exception e) {
            System.err.println("âŒ Erreur navigation vers " + fxmlPath + " : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Comme navigateTo() mais avec un contrÃ´leur Ã  initialiser aprÃ¨s chargement.
     * UtilisÃ© pour loadEventDetailsPage() qui doit injecter un objet dans le contrÃ´leur.
     */
    private static <T> T navigateToWithController(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource(fxmlPath)
            );
            Parent root = loader.load();
            postConfigureView(root, fxmlPath);
            T controller = loader.getController();

            Scene currentScene = primaryStage.getScene();
            if (currentScene != null) {
                if (!currentScene.getStylesheets().stream().anyMatch(s -> s.contains("atlantafx-custom"))) {
                    applyCSS(currentScene);
                }
                currentScene.setRoot(root);
            } else {
                Rectangle2D screen = Screen.getPrimary().getVisualBounds();
                double w = Math.min(1400, screen.getWidth()  * 0.92);
                double h = Math.min(900,  screen.getHeight() * 0.92);
                Scene scene = new Scene(root, w, h);
                applyCSS(scene);
                primaryStage.setScene(scene);
            }

            primaryStage.setTitle(title);
            primaryStage.show();
            return controller;

        } catch (Exception e) {
            System.err.println("âŒ Erreur navigation avec contrÃ´leur : " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /** Centre la fenÃªtre Ã  l'Ã©cran selon w/h donnÃ©s */
    private static void centerStage(double w, double h) {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        primaryStage.setX(screen.getMinX() + (screen.getWidth()  - w) / 2);
        primaryStage.setY(screen.getMinY() + (screen.getHeight() - h) / 2);
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //  NAVIGATION PUBLIQUE â€” appelÃ©e depuis les contrÃ´leurs
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /** Dashboard principal (aprÃ¨s login) */
    public static void loadMainLayout() {
        if (!canAccessDashboard()) {
            loadLandingPage();
            return;
        }
        navigateTo(
                "/com/example/pidev/fxml/main_layout.fxml",
                "EventFlow - Dashboard"
        );
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(680);
    }

    /** Alias pour compatibilitÃ© */
    public static void loadDashboard() {
        loadMainLayout();
    }

    /** Landing page publique */
    public static void loadLandingPage() {
        navigateTo(
                "/com/example/pidev/fxml/auth/landingPage.fxml",
                "EventFlow - Plateforme de gestion d evenements"
        );
    }

    /** Page de connexion */
    public static void loadLoginPage() {
        navigateTo(
                "/com/example/pidev/fxml/auth/login.fxml",
                "EventFlow - Connexion"
        );
    }

    /** Page d'inscription */
    public static void loadSignupPage() {
        navigateTo(
                "/com/example/pidev/fxml/auth/signup.fxml",
                "EventFlow - Inscription"
        );
    }

    /** Portail sponsor */
    public static void loadSponsorPortal() {
        navigateTo(
                "/com/example/pidev/fxml/Sponsor/sponsor_portal.fxml",
                "EventFlow - Sponsors"
        );
    }

    /** Page profil utilisateur */
    public static void loadProfilePage() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/com/example/pidev/fxml/user/profil.fxml")
            );
            Parent profileRoot = loader.load();

            Button backBtn = new Button("Back");
            backBtn.setStyle("-fx-background-color: #ffffff; -fx-text-fill: #1e293b; -fx-font-weight: bold; " +
                    "-fx-border-color: #cbd5e1; -fx-border-width: 1; -fx-border-radius: 8; " +
                    "-fx-background-radius: 8; -fx-padding: 8 16; -fx-cursor: hand;");
            backBtn.setOnAction(e -> navigateBackFromProfile());

            javafx.scene.control.Label title = new javafx.scene.control.Label("Mon profil");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #334155;");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            HBox header = new HBox(12, backBtn, title, spacer);
            header.setStyle("-fx-background-color: white; -fx-padding: 12 28; -fx-border-color: #e2e8f0; -fx-border-width: 0 0 1 0;");
            header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            VBox wrapped = new VBox(header, profileRoot);
            VBox.setVgrow(profileRoot, Priority.ALWAYS);

            Scene currentScene = primaryStage.getScene();
            if (currentScene != null) {
                if (!currentScene.getStylesheets().stream().anyMatch(s -> s.contains("atlantafx-custom"))) {
                    applyCSS(currentScene);
                }
                currentScene.setRoot(wrapped);
            } else {
                Rectangle2D screen = Screen.getPrimary().getVisualBounds();
                double w = Math.min(1400, screen.getWidth() * 0.92);
                double h = Math.min(900, screen.getHeight() * 0.92);
                Scene scene = new Scene(wrapped, w, h);
                applyCSS(scene);
                primaryStage.setScene(scene);
            }

            primaryStage.setTitle("EventFlow - Profil");
            primaryStage.show();
        } catch (Exception e) {
            System.err.println("Erreur navigation profil: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void navigateBackFromProfile() {
        UserSession session = UserSession.getInstance();
        String role = session.getRole() == null ? "" : session.getRole().trim().toLowerCase();
        if (role.contains("sponsor")) {
            loadLandingPage();
            return;
        }
        if (canAccessDashboard()) {
            loadMainLayout();
            return;
        }
        loadPublicEventsPage();
    }

    /** Page publique des Ã©vÃ©nements */
    public static void loadPublicEventsPage() {
        UserSession session = UserSession.getInstance();
        UserModel currentUser = session.getCurrentUser();
        boolean fromLoginPage = primaryStage != null
                && primaryStage.getTitle() != null
                && primaryStage.getTitle().toLowerCase().contains("connexion");
        if (fromLoginPage && currentUser != null && !session.hasPendingEvent() && canAccessDashboard()) {
            loadMainLayout();
            return;
        }

        loadLandingContentFromFrontPage(
                "/com/example/pidev/fxml/front/events.fxml",
                "EventFlow - Evenements",
                "evenements"
        );
    }
    private static boolean canAccessDashboard() {
        UserSession session = UserSession.getInstance();
        String role = session.getRole();
        if (role == null) {
            return false;
        }
        String lowerRole = role.trim().toLowerCase();
        return lowerRole.contains("organisateur") || lowerRole.contains("admin");
    }

    /** Page de dÃ©tail d'un Ã©vÃ©nement */
    public static void loadEventDetailsPage(com.example.pidev.model.event.Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/com/example/pidev/fxml/front/event-detail.fxml")
            );
            Parent loadedRoot = loader.load();
            com.example.pidev.controller.front.EventDetailController ctrl = loader.getController();
            if (ctrl != null) {
                ctrl.setEvent(event);
            }

            Parent content = extractFrontContent(loadedRoot);
            if (!setLandingMainContent(content, "EventFlow - " + event.getTitle(), "evenements")) {
                com.example.pidev.controller.front.EventDetailController fallbackCtrl =
                        navigateToWithController(
                                "/com/example/pidev/fxml/front/event-detail.fxml",
                                "EventFlow - " + event.getTitle()
                        );
                if (fallbackCtrl != null) {
                    fallbackCtrl.setEvent(event);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement detail evenement dans le layout landing: " + e.getMessage());
            e.printStackTrace();
            com.example.pidev.controller.front.EventDetailController fallbackCtrl =
                    navigateToWithController(
                            "/com/example/pidev/fxml/front/event-detail.fxml",
                            "EventFlow - " + event.getTitle()
                    );
            if (fallbackCtrl != null) {
                fallbackCtrl.setEvent(event);
            }
        }
    }

    /** Page mes billets */
    public static void loadMyTicketsPage() {
        navigateTo(
                "/com/example/pidev/fxml/front/my-tickets-list.fxml",
                "EventFlow - Mes billets"
        );
    }

    public static void showSponsorPortalInLanding() {
        loadLandingAndShowSponsorPortal();
    }

    private static void postConfigureView(Parent root, String fxmlPath) {
        if (root == null || fxmlPath == null) {
            return;
        }
        if (fxmlPath.endsWith("/auth/landingPage.fxml")) {
            configureLandingTopNav(root);
        } else if (fxmlPath.endsWith("/front/events.fxml") || fxmlPath.endsWith("/front/event-detail.fxml")) {
            configureFrontTopNav(root);
        }
    }

    private static void configureFrontTopNav(Parent root) {
        if (!(root instanceof VBox pageRoot) || pageRoot.getChildren().isEmpty()) {
            return;
        }
        if (!(pageRoot.getChildren().get(0) instanceof HBox navBar)) {
            return;
        }

        List<Button> navButtons = collectButtons(navBar).stream()
                .filter(btn -> {
                    String text = normalize(btn.getText());
                    return "accueil".equals(text) || "evenements".equals(text) || "fonctionnalites".equals(text)
                            || "feedback".equals(text) || "contact".equals(text);
                })
                .toList();
        if (navButtons.isEmpty()) {
            return;
        }

        Button accueilBtn = findNavButton(navButtons, "accueil");
        Button eventsBtn = findNavButton(navButtons, "evenements");
        Button featuresBtn = findNavButton(navButtons, "fonctionnalites");
        Button feedbackBtn = findNavButton(navButtons, "feedback");
        Button contactBtn = findNavButton(navButtons, "contact");

        if (eventsBtn != null) {
            eventsBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #1e293b; -fx-font-size: 16px; -fx-font-weight: 500; -fx-cursor: hand;");
            eventsBtn.setOnAction(e -> loadPublicEventsPage());
        }
        if (accueilBtn != null) {
            accueilBtn.setOnAction(e -> loadLandingPage());
        }
        if (featuresBtn != null) {
            featuresBtn.setOnAction(e -> loadLandingPageAndScrollTo("fonctionnalites"));
        }
        if (feedbackBtn != null) {
            feedbackBtn.setOnAction(e -> loadLandingPageAndScrollTo("feedback"));
        }
        if (contactBtn != null) {
            contactBtn.setOnAction(e -> loadLandingPageAndScrollTo("contact"));
        }

        ensureSponsorRecommendationInFrontNav(navBar);

        List<Button> authButtons = collectButtons(navBar).stream()
                .filter(btn -> {
                    String text = normalize(btn.getText());
                    return "connexion".equals(text) || "inscription".equals(text);
                })
                .toList();
        if (authButtons.size() == 2) {
            Button loginBtn = authButtons.stream()
                    .filter(btn -> "connexion".equals(normalize(btn.getText())))
                    .findFirst()
                    .orElse(authButtons.get(0));
            Button signupBtn = authButtons.stream()
                    .filter(btn -> "inscription".equals(normalize(btn.getText())))
                    .findFirst()
                    .orElse(authButtons.get(1));

            if (UserSession.getInstance().isLoggedIn()) {
                String fullName = UserSession.getInstance().getFullName();
                loginBtn.setText((fullName == null || fullName.isBlank()) ? "Profil" : fullName);
                loginBtn.setOnAction(e -> loadProfilePage());

                signupBtn.setText("Deconnexion");
                signupBtn.setOnAction(e -> {
                    UserSession.getInstance().clearSession();
                    loadLandingPage();
                });
            } else {
                loginBtn.setText("Connexion");
                loginBtn.setOnAction(e -> loadLoginPage());
                signupBtn.setText("Inscription");
                signupBtn.setOnAction(e -> loadSignupPage());
            }
        }
    }

    private static void ensureSponsorRecommendationInFrontNav(HBox navBar) {
        if (navBar == null) {
            return;
        }

        HBox linksBox = findFrontLinksBox(navBar);
        if (linksBox == null) {
            return;
        }

        Button recoBtn = linksBox.getChildren().stream()
                .filter(node -> node instanceof Button)
                .map(node -> (Button) node)
                .filter(btn -> normalize(btn.getText()).contains("recommand"))
                .findFirst()
                .orElse(null);

        if (!isCurrentUserSponsor()) {
            if (recoBtn != null) {
                linksBox.getChildren().remove(recoBtn);
            }
            return;
        }

        if (recoBtn == null) {
            recoBtn = new Button("Recommandations sponsor");
            recoBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #0D47A1; -fx-font-size: 16px; -fx-font-weight: 700; -fx-cursor: hand;");
            int index = findInsertionIndexAfterEvents(linksBox);
            linksBox.getChildren().add(index, recoBtn);
        } else {
            recoBtn.setVisible(true);
            recoBtn.setManaged(true);
            recoBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #0D47A1; -fx-font-size: 16px; -fx-font-weight: 700; -fx-cursor: hand;");
        }

        Button finalRecoBtn = recoBtn;
        finalRecoBtn.setOnAction(e -> loadLandingAndShowSponsorPortal());
    }

    private static HBox findFrontLinksBox(HBox navBar) {
        for (Node node : navBar.getChildren()) {
            if (node instanceof HBox candidate) {
                boolean hasAccueil = candidate.getChildren().stream()
                        .anyMatch(n -> n instanceof Button b && "accueil".equals(normalize(b.getText())));
                boolean hasContact = candidate.getChildren().stream()
                        .anyMatch(n -> n instanceof Button b && "contact".equals(normalize(b.getText())));
                if (hasAccueil && hasContact) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static int findInsertionIndexAfterEvents(HBox linksBox) {
        for (int i = 0; i < linksBox.getChildren().size(); i++) {
            Node child = linksBox.getChildren().get(i);
            if (child instanceof Button b && "evenements".equals(normalize(b.getText()))) {
                return i + 1;
            }
        }
        return Math.min(2, linksBox.getChildren().size());
    }

    private static void loadLandingAndShowSponsorPortal() {
        loadLandingContentFromFrontPage(
                "/com/example/pidev/fxml/Sponsor/sponsor_portal.fxml",
                "EventFlow - Plateforme de gestion d evenements",
                "recommandations sponsor"
        );
    }

    private static boolean isCurrentUserSponsor() {
        UserSession session = UserSession.getInstance();
        String role = session.getRole();
        if ((role == null || role.isBlank()) && session.getCurrentUser() != null
                && session.getCurrentUser().getRole() != null) {
            role = session.getCurrentUser().getRole().getRoleName();
        }
        return role != null && role.trim().toLowerCase().contains("sponsor");
    }

    private static void configureLandingTopNav(Parent root) {
        if (!(root instanceof VBox landingRoot) || landingRoot.getChildren().size() < 2) {
            return;
        }
        if (!(landingRoot.getChildren().get(0) instanceof HBox navBar)) {
            return;
        }
        if (!(landingRoot.getChildren().get(1) instanceof ScrollPane mainScrollPane)) {
            return;
        }
        if (!(mainScrollPane.getContent() instanceof VBox contentRoot) || contentRoot.getChildren().size() < 4) {
            return;
        }

        Node featuresSection = contentRoot.getChildren().get(1);
        Node feedbackSection = contentRoot.getChildren().get(2);
        Node contactSection = contentRoot.getChildren().get(3);

        List<Button> allNavButtons = getLandingNavButtons(navBar);

        if (allNavButtons.isEmpty()) {
            return;
        }

        allNavButtons.forEach(btn -> {
            if (!btn.getStyleClass().contains("landing-nav-link")) {
                btn.getStyleClass().add("landing-nav-link");
            }
        });

        Button accueilBtn = findNavButton(allNavButtons, "accueil");
        Button eventsBtn = findNavButton(allNavButtons, "evenements");
        Button featuresBtn = findNavButton(allNavButtons, "fonctionnalites");
        Button feedbackBtn = findNavButton(allNavButtons, "feedback");
        Button contactBtn = findNavButton(allNavButtons, "contact");
        Button recoBtn = findRecommendationButton(allNavButtons);
        javafx.event.EventHandler<javafx.event.ActionEvent> accueilDefaultHandler =
                accueilBtn != null ? accueilBtn.getOnAction() : null;
        javafx.event.EventHandler<javafx.event.ActionEvent> featuresDefaultHandler =
                featuresBtn != null ? featuresBtn.getOnAction() : null;
        javafx.event.EventHandler<javafx.event.ActionEvent> feedbackDefaultHandler =
                feedbackBtn != null ? feedbackBtn.getOnAction() : null;
        javafx.event.EventHandler<javafx.event.ActionEvent> contactDefaultHandler =
                contactBtn != null ? contactBtn.getOnAction() : null;

        if (eventsBtn != null && !eventsBtn.getStyleClass().contains("landing-nav-link")) {
            eventsBtn.getStyleClass().add("landing-nav-link");
        }

        if (accueilBtn != null) {
            accueilBtn.setOnAction(e -> {
                setLandingNavActive(allNavButtons, accueilBtn);
                if (mainScrollPane == null || mainScrollPane.getScene() == null) {
                    loadLandingPage();
                    return;
                }
                if (accueilDefaultHandler != null) {
                    accueilDefaultHandler.handle(e);
                } else {
                    mainScrollPane.setVvalue(0.0);
                }
            });
            setLandingNavActive(allNavButtons, accueilBtn);
        }
        if (featuresBtn != null) {
            featuresBtn.setOnAction(e -> {
                setLandingNavActive(allNavButtons, featuresBtn);
                if (mainScrollPane == null || mainScrollPane.getScene() == null) {
                    loadLandingPageAndScrollTo("fonctionnalites");
                    return;
                }
                if (featuresDefaultHandler != null) {
                    featuresDefaultHandler.handle(e);
                } else {
                    scrollToNodeDeferred(mainScrollPane, contentRoot, featuresSection);
                }
            });
        }
        if (feedbackBtn != null) {
            feedbackBtn.setOnAction(e -> {
                setLandingNavActive(allNavButtons, feedbackBtn);
                if (mainScrollPane == null || mainScrollPane.getScene() == null) {
                    loadLandingPageAndScrollTo("feedback");
                    return;
                }
                if (feedbackDefaultHandler != null) {
                    feedbackDefaultHandler.handle(e);
                } else {
                    scrollToNodeDeferred(mainScrollPane, contentRoot, feedbackSection);
                }
            });
        }
        if (contactBtn != null) {
            contactBtn.setOnAction(e -> {
                setLandingNavActive(allNavButtons, contactBtn);
                if (mainScrollPane == null || mainScrollPane.getScene() == null) {
                    loadLandingPageAndScrollTo("contact");
                    return;
                }
                if (contactDefaultHandler != null) {
                    contactDefaultHandler.handle(e);
                } else {
                    scrollToNodeDeferred(mainScrollPane, contentRoot, contactSection);
                }
            });
        }
        if (eventsBtn != null) {
            eventsBtn.setOnAction(e -> {
                setLandingNavActive(allNavButtons, eventsBtn);
                loadPublicEventsPage();
            });
        }
        if (recoBtn != null) {
            recoBtn.setOnAction(e -> {
                setLandingNavActive(allNavButtons, recoBtn);
                loadLandingAndShowSponsorPortal();
            });
        }
    }

    private static void scrollToNodeDeferred(ScrollPane scrollPane, VBox contentRoot, Node target) {
        Platform.runLater(() -> Platform.runLater(() -> scrollToNode(scrollPane, contentRoot, target)));
    }

    private static void loadLandingPageAndScrollTo(String section) {
        loadLandingPage();
        Platform.runLater(() -> {
            if (primaryStage == null || primaryStage.getScene() == null) {
                return;
            }
            Parent root = primaryStage.getScene().getRoot();
            if (!(root instanceof VBox landingRoot) || landingRoot.getChildren().size() < 2) {
                return;
            }
            if (!(landingRoot.getChildren().get(0) instanceof HBox navBar)) {
                return;
            }
            List<Button> navButtons = getLandingNavButtons(navBar);
            Button targetBtn = findNavButton(navButtons, section);
            if (targetBtn != null) {
                targetBtn.fire();
                return;
            }

            Button activeBtn = findNavButton(navButtons, "accueil");
            setLandingNavActive(navButtons, activeBtn);
        });
    }

    private static void loadLandingContentFromFrontPage(String fxmlPath, String title, String activeNav) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource(fxmlPath));
            Parent loadedRoot = loader.load();
            Parent content = extractFrontContent(loadedRoot);

            if (!setLandingMainContent(content, title, activeNav)) {
                navigateTo(fxmlPath, title);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement contenu front dans landing: " + e.getMessage());
            e.printStackTrace();
            navigateTo(fxmlPath, title);
        }
    }

    private static Parent extractFrontContent(Parent loadedRoot) {
        if (loadedRoot instanceof VBox container && container.getChildren().size() > 1) {
            Node contentNode = container.getChildren().remove(1);
            if (contentNode instanceof Parent contentParent) {
                return contentParent;
            }
        }
        return loadedRoot;
    }

    private static boolean setLandingMainContent(Parent content, String title, String activeNav) {
        if (primaryStage == null) {
            return false;
        }

        Scene scene = primaryStage.getScene();
        VBox currentRoot = null;
        if (scene != null && scene.getRoot() instanceof VBox root && isLandingRoot(root)) {
            currentRoot = root;
        }

        if (currentRoot == null) {
            loadLandingPage();
            scene = primaryStage.getScene();
            if (scene == null || !(scene.getRoot() instanceof VBox loadedRoot) || !isLandingRoot(loadedRoot)) {
                return false;
            }
            currentRoot = loadedRoot;
        }

        if (currentRoot.getChildren().size() > 1) {
            currentRoot.getChildren().set(1, content);
        } else {
            currentRoot.getChildren().add(content);
        }

        VBox.setVgrow(content, Priority.ALWAYS);
        if (primaryStage.getTitle() == null || !primaryStage.getTitle().equals(title)) {
            primaryStage.setTitle(title);
        }

        if (currentRoot.getChildren().get(0) instanceof HBox navBar) {
            List<Button> navButtons = getLandingNavButtons(navBar);
            Button activeButton = "recommandations sponsor".equals(activeNav)
                    ? findRecommendationButton(navButtons)
                    : findNavButton(navButtons, activeNav);
            setLandingNavActive(navButtons, activeButton);
        }
        return true;
    }

    private static boolean isLandingRoot(VBox root) {
        return root != null
                && root.getChildren().size() >= 2
                && root.getChildren().get(0) instanceof HBox;
    }

    private static List<Button> getLandingNavButtons(HBox navBar) {
        return collectButtons(navBar).stream()
                .filter(btn -> {
                    String text = normalize(btn.getText());
                    return "accueil".equals(text) || "evenements".equals(text) || "fonctionnalites".equals(text)
                            || "feedback".equals(text) || "contact".equals(text) || text.contains("recommand");
                })
                .toList();
    }

    private static Button findRecommendationButton(List<Button> buttons) {
        for (Button btn : buttons) {
            if (normalize(btn.getText()).contains("recommand")) {
                return btn;
            }
        }
        return null;
    }

    private static void scrollToNode(ScrollPane scrollPane, VBox contentRoot, Node target) {
        if (scrollPane == null || contentRoot == null || target == null) {
            return;
        }

        Bounds viewport = scrollPane.getViewportBounds();
        Bounds contentBounds = contentRoot.getLayoutBounds();
        Bounds targetBounds = target.getBoundsInParent();
        double maxScrollable = Math.max(1, contentBounds.getHeight() - viewport.getHeight());
        double targetY = Math.max(0, Math.min(targetBounds.getMinY(), maxScrollable));
        scrollPane.setVvalue(targetY / maxScrollable);
    }

    private static void setLandingNavActive(List<Button> navButtons, Button activeBtn) {
        for (Button btn : navButtons) {
            btn.getStyleClass().remove("landing-nav-link-active");
        }
        if (activeBtn != null && !activeBtn.getStyleClass().contains("landing-nav-link-active")) {
            activeBtn.getStyleClass().add("landing-nav-link-active");
        }
    }

    private static Button findNavButton(List<Button> buttons, String normalizedText) {
        for (Button btn : buttons) {
            if (normalizedText.equals(normalize(btn.getText()))) {
                return btn;
            }
        }
        return null;
    }

    private static List<Button> collectButtons(Node node) {
        List<Button> out = new ArrayList<>();
        if (node instanceof Button button) {
            out.add(button);
        } else if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                out.addAll(collectButtons(child));
            }
        }
        return out;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase();
        normalized = normalized.replace("Ã©", "e").replace("Ã¨", "e").replace("Ãª", "e");
        return normalized;
    }

    public static void main(String[] args) {
        ConsoleLogSanitizer.install();
        launch(args);
    }

    private static final class ConsoleLogSanitizer {
        private static final String[][] TOKENS = new String[][]{
                {"✅", "[OK] "},
                {"✔", "[OK] "},
                {"❌", "[ERROR] "},
                {"⚠", "[WARN] "},
                {"🔄", "[REFRESH] "},
                {"➕", "[ADD] "},
                {"📊", "[REPORT] "},
                {"📋", "[LIST] "},
                {"👤", "[USER] "},
                {"🚪", "[LOGOUT] "},
                {"🔧", "[CONFIG] "},
                {"🎫", "[TICKET] "},
                {"📄", "[PDF] "},
                {"👁", "[VIEW] "},
                {"🤖", "[BOT] "}
        };

        private ConsoleLogSanitizer() {
        }

        static void install() {
            System.setOut(new SanitizingPrintStream(System.out));
            System.setErr(new SanitizingPrintStream(System.err));
        }

        private static String sanitize(String value) {
            if (value == null || value.isEmpty()) {
                return value;
            }

            String text = value;
            for (String[] token : TOKENS) {
                text = text.replace(token[0], token[1]);
            }

            text = repairMojibake(text);
            text = text.replace('\u00A0', ' ');
            text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
            text = text.replaceAll("[^\\x09\\x0A\\x0D\\x20-\\x7E]", "");

            return text;
        }

        private static String repairMojibake(String input) {
            String current = input;
            for (int i = 0; i < 2; i++) {
                if (!looksBroken(current)) {
                    break;
                }
                String repaired = new String(current.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                if (repaired.equals(current)) {
                    break;
                }
                current = repaired;
            }
            return current;
        }

        private static boolean looksBroken(String value) {
            return value.contains("Ã") || value.contains("â") || value.contains("Å") || value.contains("Â");
        }
    }

    private static final class SanitizingPrintStream extends PrintStream {
        private final PrintStream delegate;

        private SanitizingPrintStream(PrintStream delegate) {
            super(delegate);
            this.delegate = delegate;
        }

        @Override
        public void println(String x) {
            delegate.println(ConsoleLogSanitizer.sanitize(x));
        }

        @Override
        public void print(String s) {
            delegate.print(ConsoleLogSanitizer.sanitize(s));
        }

        @Override
        public void println(Object x) {
            delegate.println(ConsoleLogSanitizer.sanitize(String.valueOf(x)));
        }

        @Override
        public void print(Object obj) {
            delegate.print(ConsoleLogSanitizer.sanitize(String.valueOf(obj)));
        }
    }
}
