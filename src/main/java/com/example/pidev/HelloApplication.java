package com.example.pidev;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
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

        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        Parent root = FXMLLoader.load(getClass().getResource("/com/example/pidev/fxml/auth/landingPage.fxml"));
        stage.initStyle(StageStyle.DECORATED);
        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/com/example/pidev/css/atlantafx-custom.css").toExternalForm());
        stage.setTitle("EventFlow - Plateforme de gestion d'evenements");
        stage.setScene(scene);
        stage.setMinWidth(1200);
        stage.setMinHeight(800);
        stage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void loadMainLayout() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("/com/example/pidev/fxml/main_layout.fxml"));
            Parent root = loader.load();

            MainController mainController = loader.getController();
            mainController.loadPage("/com/example/pidev/fxml/dashboard/dashboard.fxml");
            Scene scene = new Scene(root, 1400, 900);
            scene.getStylesheets().add(HelloApplication.class.getResource("/com/example/pidev/css/atlantafx-custom.css").toExternalForm());
            primaryStage.setTitle("EventFlow - Dashboard");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadDashboard() {
        try {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            Stage newStage = new Stage();
            newStage.initStyle(StageStyle.DECORATED);
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("/com/example/pidev/fxml/main_layout.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 1400, 900);
            scene.getStylesheets().add(HelloApplication.class.getResource("/com/example/pidev/css/atlantafx-custom.css").toExternalForm());
            newStage.setScene(scene);
            newStage.setTitle("EventFlow - Dashboard");
            newStage.setMinWidth(1200);
            newStage.setMinHeight(800);
            primaryStage.close();
            primaryStage = newStage;
            primaryStage.show();
            primaryStage.centerOnScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadLandingPage() {
        try {
            Parent root = FXMLLoader.load(HelloApplication.class.getResource("/com/example/pidev/fxml/auth/landingPage.fxml"));
            Scene scene = new Scene(root, 1400, 900);
            scene.getStylesheets().add(HelloApplication.class.getResource("/com/example/pidev/css/atlantafx-custom.css").toExternalForm());
            primaryStage.setTitle("EventFlow - Plateforme de gestion d'evenements");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadLoginPage() {
        try {
            Parent root = FXMLLoader.load(HelloApplication.class.getResource("/com/example/pidev/fxml/auth/login.fxml"));
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(HelloApplication.class.getResource("/com/example/pidev/css/atlantafx-custom.css").toExternalForm());
            primaryStage.setTitle("EventFlow - Connexion");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadSignupPage() {
        try {
            Parent root = FXMLLoader.load(HelloApplication.class.getResource("/com/example/pidev/fxml/auth/signup.fxml"));
            Scene scene = new Scene(root, 1200, 800);
            scene.getStylesheets().add(HelloApplication.class.getResource("/com/example/pidev/css/atlantafx-custom.css").toExternalForm());
            primaryStage.setTitle("EventFlow - Inscription");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadPublicEventsPage() {
        try {
            Parent root = FXMLLoader.load(HelloApplication.class.getResource("/com/example/pidev/fxml/front/events.fxml"));
            Scene scene = new Scene(root, 1400, 900);
            scene.getStylesheets().add(HelloApplication.class.getResource("/com/example/pidev/css/atlantafx-custom.css").toExternalForm());
            primaryStage.setTitle("EventFlow - Evenements");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadEventDetailsPage(com.example.pidev.model.event.Event event) {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("/com/example/pidev/fxml/front/event-detail.fxml"));
            Parent root = loader.load();
            com.example.pidev.controller.front.EventDetailController controller = loader.getController();
            controller.setEvent(event);
            Scene scene = new Scene(root, 1400, 900);
            scene.getStylesheets().add(HelloApplication.class.getResource("/com/example/pidev/css/atlantafx-custom.css").toExternalForm());
            primaryStage.setTitle("EventFlow - " + event.getTitle());
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadMyTicketsPage() {
        try {
            Parent root = FXMLLoader.load(HelloApplication.class.getResource("/com/example/pidev/fxml/front/my-tickets-list.fxml"));
            Scene scene = new Scene(root, 1400, 900);
            scene.getStylesheets().add(HelloApplication.class.getResource("/com/example/pidev/css/atlantafx-custom.css").toExternalForm());
            primaryStage.setTitle("EventFlow - Mes billets");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(1200);
            primaryStage.setMinHeight(800);
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
