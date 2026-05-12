package com.example.pidev;

import atlantafx.base.theme.PrimerLight;
import com.example.pidev.controller.front.EventDetailController;
import com.example.pidev.controller.user.ResetPasswordController;
import com.example.pidev.model.event.Event;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class HelloApplication extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;

        // Appliquer AtlantaFX
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        try {
            // Charger le FXML de landing page
            String fxmlPath = "/com/example/pidev/fxml/auth/landingPage.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));

            if (loader.getLocation() == null) {
                throw new IllegalArgumentException("Fichier FXML non trouvé: " + fxmlPath);
            }

            Parent root = loader.load();
            stage.initStyle(StageStyle.DECORATED);

            Scene scene = new Scene(root, 1400, 900);

            // CSS personnalisé
            String cssPath = "/com/example/pidev/css/atlantafx-custom.css";
            if (getClass().getResource(cssPath) != null) {
                scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            } else {
                System.err.println("⚠️ Fichier CSS non trouvé: " + cssPath);
            }

            stage.setTitle("EventFlow - Plateforme de gestion d'événements");
            stage.setScene(scene);
            stage.setMinWidth(1200);
            stage.setMinHeight(800);
            stage.show();

            System.out.println("✅ Application démarrée avec succès");

        } catch (Exception e) {
            System.err.println("❌ Erreur au démarrage de l'application: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * Charge le layout principal avec le dashboard
     */
    public static void loadMainLayout() {
        try {
            System.out.println("📂 Chargement du layout principal...");

            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/com/example/pidev/fxml/main_layout.fxml")
            );
            Parent root = loader.load();

            MainController mainController = loader.getController();
            mainController.loadPage("/com/example/pidev/fxml/dashboard/dashboard.fxml");

            Scene scene = new Scene(root, 1400, 900);
            addStylesheet(scene);

            primaryStage.setTitle("EventFlow - Dashboard");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();

            System.out.println("✅ Layout principal chargé avec succès");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement du layout principal");
            e.printStackTrace();
        }
    }

    /**
     * Charge le dashboard dans une nouvelle fenêtre (pour la redirection après connexion)
     */
    public static void loadDashboard() {
        try {
            System.out.println("📂 Chargement du dashboard...");

            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

            Stage newStage = new Stage();
            newStage.initStyle(StageStyle.DECORATED);

            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/com/example/pidev/fxml/main_layout.fxml")
            );
            Parent root = loader.load();

            Scene scene = new Scene(root, 1400, 900);
            addStylesheet(scene);

            newStage.setScene(scene);
            newStage.setTitle("EventFlow - Dashboard");
            newStage.setMinWidth(1200);
            newStage.setMinHeight(800);

            primaryStage.close();
            primaryStage = newStage;
            primaryStage.show();
            primaryStage.centerOnScreen();

            System.out.println("✅ Dashboard chargé avec succès");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement du dashboard");
            e.printStackTrace();
        }
    }

    /**
     * Charge la landing page
     */
    public static void loadLandingPage() {
        try {
            System.out.println("📂 Chargement de la landing page");

            Parent root = FXMLLoader.load(
                    HelloApplication.class.getResource("/com/example/pidev/fxml/auth/landingPage.fxml")
            );

            Scene scene = new Scene(root, 1400, 900);
            addStylesheet(scene);

            primaryStage.setTitle("EventFlow - Plateforme de gestion d'événements");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            primaryStage.centerOnScreen();
            primaryStage.show();

            System.out.println("✅ Landing page chargée avec succès");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de la landing page");
            e.printStackTrace();
        }
    }

    /**
     * Charge la page de connexion
     */
    public static void loadLoginPage() {
        try {
            System.out.println("📂 Chargement de la page de connexion");

            Parent root = FXMLLoader.load(
                    HelloApplication.class.getResource("/com/example/pidev/fxml/auth/login.fxml")
            );

            Scene scene = new Scene(root, 1200, 800);
            addStylesheet(scene);

            primaryStage.setTitle("EventFlow - Connexion");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.centerOnScreen();
            primaryStage.show();

            System.out.println("✅ Page de connexion chargée avec succès");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de la page de connexion");
            e.printStackTrace();
        }
    }

    /**
     * Charge la page d'inscription
     */
    public static void loadSignupPage() {
        try {
            System.out.println("📂 Chargement de la page d'inscription");

            Parent root = FXMLLoader.load(
                    HelloApplication.class.getResource("/com/example/pidev/fxml/auth/signup.fxml")
            );

            Scene scene = new Scene(root, 1200, 800);
            addStylesheet(scene);

            primaryStage.setTitle("EventFlow - Inscription");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.centerOnScreen();
            primaryStage.show();

            System.out.println("✅ Page d'inscription chargée avec succès");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de la page d'inscription");
            e.printStackTrace();
        }
    }

    /**
     * Charge la page publique des événements (depuis event)
     */
    public static void loadPublicEventsPage() {
        try {
            System.out.println("📂 Chargement de la page publique des événements");

            Parent root = FXMLLoader.load(
                    HelloApplication.class.getResource("/com/example/pidev/fxml/front/events.fxml")
            );

            Scene scene = new Scene(root, 1400, 900);
            addStylesheet(scene);

            primaryStage.setTitle("EventFlow - Événements");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            primaryStage.centerOnScreen();
            primaryStage.show();

            System.out.println("✅ Page des événements chargée avec succès");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de la page des événements");
            e.printStackTrace();
        }
    }

    /**
     * Charge la page de détail d'un événement (depuis event)
     */
    public static void loadEventDetailsPage(Event event) {
        try {
            System.out.println("📂 Chargement de la page de détail de l'événement: " + event.getTitle());

            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/com/example/pidev/fxml/front/event-detail.fxml")
            );
            Parent root = loader.load();

            EventDetailController controller = loader.getController();
            controller.setEvent(event);

            Scene scene = new Scene(root, 1400, 900);
            addStylesheet(scene);

            primaryStage.setTitle("EventFlow - " + event.getTitle());
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            primaryStage.centerOnScreen();
            primaryStage.show();

            System.out.println("✅ Page de détail chargée avec succès");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de la page de détail");
            e.printStackTrace();
        }
    }

    /**
     * Charge la page "Mes billets" (depuis event)
     */
    public static void loadMyTicketsPage() {
        try {
            System.out.println("📂 Chargement de la page 'Mes billets'");

            Parent root = FXMLLoader.load(
                    HelloApplication.class.getResource("/com/example/pidev/fxml/front/my-tickets-list.fxml")
            );

            Scene scene = new Scene(root, 1400, 900);
            addStylesheet(scene);

            primaryStage.setTitle("EventFlow - Mes billets");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            primaryStage.centerOnScreen();
            primaryStage.show();

            System.out.println("✅ Page 'Mes billets' chargée avec succès");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de la page 'Mes billets'");
            e.printStackTrace();
        }
    }

    /**
     * Ouvre directement la fenêtre de réinitialisation (depuis user)
     * Appelée depuis ForgotPasswordController après envoi WhatsApp
     */
    public static void openResetPasswordWindow(String token) {
        Platform.runLater(() -> {
            try {
                System.out.println("🖥️ Ouverture de la fenêtre de réinitialisation");

                FXMLLoader loader = new FXMLLoader(
                        HelloApplication.class.getResource("/com/example/pidev/fxml/user/reset_password.fxml")
                );
                Parent root = loader.load();

                // Récupérer le contrôleur
                ResetPasswordController controller = loader.getController();

                // Si le contrôleur a une méthode setToken, l'appeler
                // controller.setToken(token); // Décommentez si la méthode existe

                Stage resetStage = new Stage();
                resetStage.setTitle("Réinitialisation du mot de passe");
                Scene scene = new Scene(root);
                addStylesheet(scene);
                resetStage.setScene(scene);
                resetStage.setMinWidth(600);
                resetStage.setMinHeight(400);
                resetStage.show();

                System.out.println("✅ Fenêtre de réinitialisation ouverte avec succès");

            } catch (Exception e) {
                System.err.println("❌ Erreur ouverture fenêtre: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * Méthode utilitaire pour ajouter la feuille de style CSS
     */
    private static void addStylesheet(Scene scene) {
        String cssPath = "/com/example/pidev/css/atlantafx-custom.css";
        if (HelloApplication.class.getResource(cssPath) != null) {
            scene.getStylesheets().add(HelloApplication.class.getResource(cssPath).toExternalForm());
        } else {
            System.err.println("⚠️ Fichier CSS non trouvé: " + cssPath);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}