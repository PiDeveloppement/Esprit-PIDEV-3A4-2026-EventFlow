package com.example.pidev.controller.auth;

import com.example.pidev.HelloApplication;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.service.user.UserService;
import com.example.pidev.utils.UserSession;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class LoginController implements Initializable {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField visiblePasswordField;

    @FXML
    private CheckBox rememberCheckbox;

    @FXML
    private Label loginMessageLabel;

    @FXML
    private Button submitButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Hyperlink forgotPasswordLink;

    @FXML
    private Button facialLoginButton;

    @FXML
    private Label eyeIcon;

    @FXML
    private Label lastLoginInfoLabel; // NOUVEAU: Label pour afficher la dernière connexion

    private boolean passwordVisible = false;
    private UserService userService;
    private Preferences preferences;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            userService = new UserService();
            preferences = Preferences.userNodeForPackage(LoginController.class);
            System.out.println("✅ LoginController initialisé");

            // Charger les identifiants sauvegardés
            loadSavedCredentials();

            // Afficher les informations de dernière connexion
            displayLastLoginInfo();

            // Configurer le bouton facial
            setupFacialLoginButton();

            // Configurer le bouton œil
            setupPasswordToggle();

            // Ajouter un listener sur la checkbox
            setupRememberCheckbox();

        } catch (Exception e) {
            showAlert("Erreur", "Erreur de connexion à la base de données");
            e.printStackTrace();
        }
    }

    /**
     * Configure la checkbox "Se souvenir de moi"
     */
    private void setupRememberCheckbox() {
        rememberCheckbox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                // Si coché, on sauvegarde immédiatement
                String email = emailField.getText().trim();
                String password = passwordField.getText().trim();

                if (!email.isEmpty() && !password.isEmpty()) {
                    saveCredentials(email, password);

                    // Sauvegarder aussi la date de connexion
                    String lastLogin = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                    preferences.put("last_login", lastLogin);
                    preferences.put("last_email", email);

                    showMessage("✓ Identifiants sauvegardés", "success");

                    // Afficher les infos de dernière connexion
                    displayLastLoginInfo();
                }
            } else {
                // Si décoché, on efface
                clearSavedCredentials();
                showMessage("Identifiants effacés", "info");

                // Effacer aussi les infos de dernière connexion
                preferences.remove("last_login");
                preferences.remove("last_email");
                if (lastLoginInfoLabel != null) {
                    lastLoginInfoLabel.setText("");
                }
            }
        });
    }

    /**
     * Affiche les informations de la dernière connexion
     */
    private void displayLastLoginInfo() {
        if (lastLoginInfoLabel != null) {
            String lastLogin = preferences.get("last_login", null);
            String lastEmail = preferences.get("last_email", null);

            if (lastLogin != null && lastEmail != null) {
                lastLoginInfoLabel.setText("Dernière connexion: " + lastEmail + " le " + lastLogin);
                lastLoginInfoLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11px; -fx-font-weight: bold;");
            } else {
                lastLoginInfoLabel.setText("");
            }
        }
    }

    /**
     * Configure le bouton œil
     */
    private void setupPasswordToggle() {
        // Lier les deux champs de mot de passe
        visiblePasswordField.setManaged(false);
        visiblePasswordField.setVisible(false);
        visiblePasswordField.textProperty().bindBidirectional(passwordField.textProperty());

        // Mettre à jour l'icône initiale
        updateEyeIcon();
    }

    /**
     * Alterne la visibilité du mot de passe
     */
    @FXML
    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;

        if (passwordVisible) {
            // Montrer le mot de passe en clair
            passwordField.setVisible(false);
            passwordField.setManaged(false);
            visiblePasswordField.setVisible(true);
            visiblePasswordField.setManaged(true);
            visiblePasswordField.requestFocus();
        } else {
            // Cacher le mot de passe
            passwordField.setVisible(true);
            passwordField.setManaged(true);
            visiblePasswordField.setVisible(false);
            visiblePasswordField.setManaged(false);
            passwordField.requestFocus();
        }

        updateEyeIcon();
    }

    /**
     * Met à jour l'icône de l'œil
     */
    private void updateEyeIcon() {
        if (eyeIcon != null) {
            if (passwordVisible) {
                eyeIcon.setText("👁️‍🗨️"); // Œil barré
            } else {
                eyeIcon.setText("👁️"); // Œil normal
            }
        }
    }

    /**
     * Configure le bouton de connexion faciale
     */
    private void setupFacialLoginButton() {
        if (facialLoginButton != null) {
            boolean isOpenCvAvailable = checkOpenCvAvailability();

            if (isOpenCvAvailable) {
                facialLoginButton.setDisable(false);
                facialLoginButton.setTooltip(new Tooltip("Se connecter avec reconnaissance faciale"));
                System.out.println("✅ Connexion faciale disponible");
            } else {
                facialLoginButton.setDisable(true);
                facialLoginButton.setTooltip(new Tooltip("Reconnaissance faciale non disponible (OpenCV manquant)"));
                System.out.println("⚠️ Connexion faciale non disponible");
            }
        }
    }

    /**
     * Vérifie si OpenCV est disponible
     */
    private boolean checkOpenCvAvailability() {
        try {
            Class.forName("org.bytedeco.javacpp.Loader");
            Class.forName("org.bytedeco.opencv.opencv_java");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @FXML
    private void handleFacialLogin(ActionEvent event) {
        try {
            System.out.println("👤 Tentative de connexion faciale");
            URL fxmlLocation = getClass().getResource("/com/example/pidev/fxml/facial/facial_login.fxml");

            if (fxmlLocation == null) {
                showFacialLoginNotImplemented(event);
                return;
            }

            Parent root = FXMLLoader.load(fxmlLocation);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Connexion Faciale - EventFlow");
            stage.show();

        } catch (IOException e) {
            System.err.println("❌ Erreur lors de l'ouverture de la connexion faciale: " + e.getMessage());
            showFacialLoginNotImplemented(event);
        }
    }

    private void showFacialLoginNotImplemented(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Connexion Faciale");
        alert.setHeaderText("Fonctionnalité en cours de développement");
        alert.setContentText("La connexion par reconnaissance faciale sera bientôt disponible !");

        ButtonType tryPasswordBtn = new ButtonType("Utiliser mot de passe", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelBtn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(tryPasswordBtn, cancelBtn);

        alert.showAndWait().ifPresent(response -> {
            if (response == tryPasswordBtn) {
                System.out.println("↩️ Retour au login classique");
            }
        });
    }

    @FXML
    private void loginButtonOnAction(ActionEvent event) {
        String email = emailField.getText().trim();
        String password = passwordField.getText().trim();

        if (email.isEmpty()) {
            showMessage("Veuillez saisir votre email", "error");
            emailField.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            showMessage("Veuillez saisir votre mot de passe", "error");
            passwordField.requestFocus();
            return;
        }

        try {
            System.out.println("🔐 Tentative de connexion pour: " + email);
            UserModel user = userService.authenticate(email, password);

            if (user != null) {
                System.out.println("✅ Utilisateur authentifié: " + user.getFirst_Name() + " " + user.getLast_Name());

                // Si "Se souvenir de moi" est coché, sauvegarder les identifiants
                if (rememberCheckbox.isSelected()) {
                    saveCredentials(email, password);

                    // Sauvegarder la date de dernière connexion
                    String lastLogin = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                    preferences.put("last_login", lastLogin);
                    preferences.put("last_email", email);
                } else {
                    clearSavedCredentials();
                }

                // Stocker l'utilisateur dans la session
                UserSession.getInstance().setCurrentUser(user);
                UserSession.getInstance().printSessionInfo();

                // Vérifier s'il y a un événement en attente de participation
                handlePendingEventParticipation();

                // Redirection intelligente : pendingEvent -> vitrine, sinon selon rôle
                if (UserSession.getInstance().hasPendingEvent()) {
                    HelloApplication.loadPublicEventsPage();
                } else {
                    String roleName = user.getRoleName() != null ? user.getRoleName().trim().toLowerCase() : "";
                    boolean isAdminOrOrganizer = user.getRole_Id() == 2
                            || roleName.contains("organisateur")
                            || roleName.contains("admin");
                    boolean isPureSponsor = roleName.contains("sponsor") && !roleName.contains("admin");

                    if (isAdminOrOrganizer) {
                        HelloApplication.loadDashboard();
                    } else if (isPureSponsor) {
                        HelloApplication.loadLandingPage();
                    } else {
                        HelloApplication.loadPublicEventsPage();
                    }
                }

            } else {
                System.out.println("❌ Échec de connexion pour: " + email);
                showMessage("Email ou mot de passe incorrect", "error");
                passwordField.clear();
                visiblePasswordField.clear();
                passwordField.requestFocus();
            }

        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Erreur de connexion: " + e.getMessage(), "error");
        }
    }

    @FXML
    private void handleForgotPassword(ActionEvent event) {
        try {
            System.out.println("🔑 Redirection vers la page mot de passe oublié");
            URL fxmlLocation = getClass().getResource("/com/example/pidev/fxml/user/forgot_password.fxml");

            if (fxmlLocation == null) {
                System.err.println("❌ FXML forgot_password.fxml introuvable!");
                showAlert("Erreur", "Fichier de récupération de mot de passe introuvable");
                return;
            }

            Parent root = FXMLLoader.load(fxmlLocation);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Mot de passe oublié - EventFlow");
            stage.show();

        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la redirection: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page de récupération de mot de passe");
        }
    }

    /**
     * Charge les identifiants sauvegardés
     */
    private void loadSavedCredentials() {
        String savedEmail = preferences.get("saved_email", "");
        String savedPassword = preferences.get("saved_password", "");

        if (!savedEmail.isEmpty() && !savedPassword.isEmpty()) {
            emailField.setText(savedEmail);
            passwordField.setText(savedPassword);
            visiblePasswordField.setText(savedPassword);
            rememberCheckbox.setSelected(true);
            System.out.println("📧 Identifiants chargés pour: " + savedEmail);
        }
    }

    /**
     * Sauvegarde les identifiants
     */
    private void saveCredentials(String email, String password) {
        preferences.put("saved_email", email);
        preferences.put("saved_password", password);
        System.out.println("💾 Identifiants sauvegardés");
    }

    /**
     * Efface les identifiants sauvegardés
     */
    private void clearSavedCredentials() {
        preferences.remove("saved_email");
        preferences.remove("saved_password");
        System.out.println("🗑️ Identifiants effacés");
    }

    @FXML
    private void cancelButtonOnAction(ActionEvent event) {
        clearFields();
        showMessage("Champs effacés", "info");
    }

    @FXML
    private void goToSignup(ActionEvent event) {
        try {
            System.out.println("📝 Redirection vers la page d'inscription");
            URL fxmlLocation = getClass().getResource("/com/example/pidev/fxml/auth/signup.fxml");

            if (fxmlLocation == null) {
                System.err.println("❌ FXML signup.fxml introuvable!");
                showAlert("Erreur", "Fichier d'inscription introuvable");
                return;
            }

            Parent root = FXMLLoader.load(fxmlLocation);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la redirection vers l'inscription");
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page d'inscription");
        }
    }

    /**
     * Affiche un message dans le label prévu
     */
    private void showMessage(String message, String type) {
        if (loginMessageLabel != null) {
            loginMessageLabel.setText(message);
            switch (type) {
                case "error":
                    loginMessageLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 12px;");
                    break;
                case "success":
                    loginMessageLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 12px;");
                    break;
                default:
                    loginMessageLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-size: 12px;");
            }
        }
    }

    /**
     * Efface les champs du formulaire
     */
    private void clearFields() {
        emailField.clear();
        passwordField.clear();
        visiblePasswordField.clear();
        if (loginMessageLabel != null) {
            loginMessageLabel.setText("");
        }
    }

    /**
     * Gère la participation différée à un événement après connexion
     */
    private void handlePendingEventParticipation() {
        UserSession session = UserSession.getInstance();

        // Vérifier s'il y a un événement en attente
        if (!session.hasPendingEvent()) {
            return;
        }

        Integer eventId = session.getPendingEventId();
        System.out.println("🎫 Traitement de la participation différée pour l'événement: " + eventId);

        try {
            // Créer le ticket service
            com.example.pidev.service.event.EventTicketService ticketService =
                new com.example.pidev.service.event.EventTicketService();

            // Récupérer l'event service pour obtenir le nom de l'événement
            com.example.pidev.service.event.EventService eventService =
                new com.example.pidev.service.event.EventService();

            com.example.pidev.model.event.Event event = eventService.getEventById(eventId);

            if (event == null) {
                System.err.println("❌ Événement non trouvé: " + eventId);
                session.clearPendingEventId();
                return;
            }

            int userId = session.getCurrentUser().getId_User();

            // Créer le ticket dans la base de données
            com.example.pidev.model.event.EventTicket ticket = ticketService.createTicket(eventId, userId);

            if (ticket != null) {
                // Succès : afficher le ticket généré
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION
                );
                alert.setTitle("✅ Participation confirmée");
                alert.setHeaderText("Bienvenue ! Votre participation est enregistrée");
                alert.setContentText(
                    "Vous participez à l'événement :\n" +
                    event.getTitle() + "\n\n" +
                    "Votre ticket : " + ticket.getTicketCode() + "\n\n" +
                    "Un email de confirmation vous sera envoyé.\n" +
                    "Conservez votre code de ticket pour accéder à l'événement."
                );

                // Style personnalisé
                alert.getDialogPane().setStyle(
                    "-fx-background-color: white; " +
                    "-fx-font-size: 14px;"
                );

                alert.showAndWait();

                System.out.println("✅ Ticket créé avec succès après connexion: " + ticket.getTicketCode());

            } else {
                // Erreur de création
                System.err.println("❌ Échec de la création du ticket différé");

                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.WARNING
                );
                alert.setTitle("Participation incomplète");
                alert.setHeaderText("Impossible de créer votre ticket");
                alert.setContentText(
                    "Votre connexion a réussi, mais nous n'avons pas pu créer votre ticket.\n\n" +
                    "Veuillez réessayer de participer à l'événement depuis la page des événements."
                );
                alert.showAndWait();
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la participation différée: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Toujours nettoyer le pendingEventId
            session.clearPendingEventId();
        }
    }

    /**
     * Affiche une boîte de dialogue d'alerte
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void goToLanding(ActionEvent event) {
        HelloApplication.loadLandingPage();
    }
}
