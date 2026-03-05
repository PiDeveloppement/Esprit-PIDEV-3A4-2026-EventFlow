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
                "/com/example/pidev/fxml/MainLayout.fxml",
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

        navigateTo(
                "/com/example/pidev/fxml/front/events.fxml",
                "EventFlow - Evenements"
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
        com.example.pidev.controller.front.EventDetailController ctrl =
                navigateToWithController(
                        "/com/example/pidev/fxml/front/event-detail.fxml",
                        "EventFlow - " + event.getTitle()
                );
        if (ctrl != null) {
            ctrl.setEvent(event);
        }
    }

    /** Page mes billets */
    public static void loadMyTicketsPage() {
        navigateTo(
                "/com/example/pidev/fxml/front/my-tickets-list.fxml",
                "EventFlow - Mes billets"
        );
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

        List<Button> allNavButtons = collectButtons(navBar).stream()
                .filter(btn -> {
                    String text = normalize(btn.getText());
                    return "accueil".equals(text) || "evenements".equals(text) || "fonctionnalites".equals(text)
                            || "feedback".equals(text) || "contact".equals(text);
                })
                .toList();

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

        if (eventsBtn != null && !eventsBtn.getStyleClass().contains("landing-nav-link")) {
            eventsBtn.getStyleClass().add("landing-nav-link");
        }

        if (accueilBtn != null) {
            accueilBtn.setOnAction(e -> {
                setLandingNavActive(allNavButtons, accueilBtn);
                mainScrollPane.setVvalue(0.0);
            });
            setLandingNavActive(allNavButtons, accueilBtn);
        }
        if (featuresBtn != null) {
            featuresBtn.setOnAction(e -> {
                setLandingNavActive(allNavButtons, featuresBtn);
                scrollToNodeDeferred(mainScrollPane, contentRoot, featuresSection);
            });
        }
        if (feedbackBtn != null) {
            feedbackBtn.setOnAction(e -> {
                setLandingNavActive(allNavButtons, feedbackBtn);
                scrollToNodeDeferred(mainScrollPane, contentRoot, feedbackSection);
            });
        }
        if (contactBtn != null) {
            contactBtn.setOnAction(e -> {
                setLandingNavActive(allNavButtons, contactBtn);
                scrollToNodeDeferred(mainScrollPane, contentRoot, contactSection);
            });
        }
        if (eventsBtn != null) {
            eventsBtn.setOnAction(e -> {
                setLandingNavActive(allNavButtons, eventsBtn);
                loadPublicEventsPage();
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
            if (!(landingRoot.getChildren().get(1) instanceof ScrollPane mainScrollPane)) {
                return;
            }
            if (!(mainScrollPane.getContent() instanceof VBox contentRoot) || contentRoot.getChildren().size() < 4) {
                return;
            }

            Node target = switch (section) {
                case "fonctionnalites" -> contentRoot.getChildren().get(1);
                case "feedback" -> contentRoot.getChildren().get(2);
                case "contact" -> contentRoot.getChildren().get(3);
                default -> contentRoot.getChildren().get(0);
            };
            scrollToNodeDeferred(mainScrollPane, contentRoot, target);

            List<Button> navButtons = collectButtons(navBar).stream()
                    .filter(btn -> {
                        String text = normalize(btn.getText());
                        return "accueil".equals(text) || "fonctionnalites".equals(text)
                                || "feedback".equals(text) || "contact".equals(text);
                    })
                    .toList();
            Button activeBtn = findNavButton(navButtons, section);
            setLandingNavActive(navButtons, activeBtn);
        });
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
        launch(args);
    }
}
