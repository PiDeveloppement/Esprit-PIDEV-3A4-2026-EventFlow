package com.example.pidev.controller.user;

import com.example.pidev.model.user.PasswordResetToken;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.service.user.WhatsAppService;  // Changé de SmsService à WhatsAppService
import com.example.pidev.service.user.PasswordResetService;
import com.example.pidev.service.user.UserService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class ForgotPasswordController {

    @FXML private TextField emailField;  // Gardé pour compatibilité
    @FXML private TextField phoneField;  // Champ pour numéro de téléphone
    @FXML private Button sendResetLinkBtn;
    @FXML private Button backToLoginBtn;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button testLinkButton;

    private PasswordResetService resetService;
    private UserService userService;
    private String lastToken;

    @FXML
    public void initialize() {
        resetService = new PasswordResetService();
        userService = new UserService();

        if (loadingIndicator != null) {
            loadingIndicator.setVisible(false);
        }

        if (testLinkButton != null) {
            testLinkButton.setVisible(false);
        }

        // Message d'information pour WhatsApp
        System.out.println("📱 WhatsApp Service prêt - Utilisation du Sandbox Twilio");
    }

    @FXML
    private void handleSendResetLink() {
        String phoneNumber = phoneField.getText().trim();

        if (phoneNumber.isEmpty()) {
            showStatus("❌ Veuillez saisir votre numéro de téléphone", "red");
            return;
        }

        if (!phoneNumber.matches("^\\+?[0-9]{8,15}$")) {
            showStatus("❌ Format de numéro invalide (ex: +21692500441)", "red");
            return;
        }

        sendResetLinkBtn.setDisable(true);
        loadingIndicator.setVisible(true);
        showStatus("Préparation du message WhatsApp...", "blue");

        new Thread(() -> {
            try {
                UserModel user = userService.getUserByPhone(phoneNumber);

                Platform.runLater(() -> {
                    if (user == null) {
                        showStatus("❌ Aucun compte avec ce numéro", "red");
                        sendResetLinkBtn.setDisable(false);
                        loadingIndicator.setVisible(false);
                        return;
                    }

                    try {
                        PasswordResetToken token = new PasswordResetToken(user.getId_User());
                        resetService.createToken(token);

                        if (token != null) {
                            lastToken = token.getToken();

                            // ✅ 1. Envoyer le message WhatsApp
                            String userPhone = user.getPhone();
                            WhatsAppService.sendResetPasswordWhatsApp(userPhone, token.getToken());

                            // ✅ 2. OUVRIR AUTOMATIQUEMENT LA FENÊTRE DE RÉINITIALISATION
                            openResetPasswordWindow(token.getToken());

                            // ✅ 3. Mettre à jour le message de statut
                            showStatus("✅ Message envoyé sur WhatsApp! La fenêtre de réinitialisation est ouverte", "green");

                            // Afficher les instructions
                            showWhatsAppInstructions();

                            System.out.println("🔑 Token: " + token.getToken());

                            // Optionnel: Afficher le bouton de test
                            if (testLinkButton != null) {
                                testLinkButton.setVisible(true);
                                testLinkButton.setText("Tester le lien (mode développement)");
                            }

                        } else {
                            showStatus("❌ Erreur création token", "red");
                        }

                    } catch (Exception e) {
                        showStatus("❌ Erreur: " + e.getMessage(), "red");
                        e.printStackTrace();
                        showTestModeOption();
                    }

                    sendResetLinkBtn.setDisable(false);
                    loadingIndicator.setVisible(false);
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showStatus("❌ Erreur: " + e.getMessage(), "red");
                    sendResetLinkBtn.setDisable(false);
                    loadingIndicator.setVisible(false);
                });
            }
        }).start();
    }
    /**
     * Affiche les instructions pour le sandbox WhatsApp
     */
    private void showWhatsAppInstructions() {
        String instructions =
                "📱 *Instructions WhatsApp Sandbox*\n\n" +
                        "1. Ouvrez WhatsApp sur votre téléphone\n" +
                        "2. Envoyez 'join orange-popsicle' au +14155238886\n" +
                        "3. Attendez la confirmation\n" +
                        "4. Vous recevrez alors le message de réinitialisation\n\n" +
                        "⚠️ Le sandbox expire après 3 jours, renouvelez si nécessaire";

        System.out.println(instructions);

        // Optionnel: Afficher une alerte informative
        if (statusLabel != null) {
            statusLabel.setText("📱 N'oubliez pas de rejoindre le sandbox WhatsApp!");
        }
    }

    /**
     * Propose le mode test en cas d'échec
     */
    private void showTestModeOption() {
        // Vous pouvez ajouter un bouton ou une option pour le mode test
        System.out.println("💡 Mode test: Utilisez le bouton 'Tester le lien' pour continuer");
    }

    @FXML
    private void handleTestLink() {
        if (lastToken != null && !lastToken.isEmpty()) {
            openResetPasswordWindow(lastToken);
        } else {
            showStatus("❌ Aucun lien récent à tester", "red");
        }
    }

    private void openResetPasswordWindow(String token) {
        try {
            System.out.println("🖥️ Tentative d'ouverture de la fenêtre de réinitialisation...");
            System.out.println("🔑 Avec token: " + token);

            // Charger le FXML
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/pidev/fxml/user/reset_password.fxml")
            );

            Parent root = loader.load();

            // Récupérer le contrôleur et passer le token
            ResetPasswordController controller = loader.getController();
            controller.setToken(token);  // 👈 IMPORTANT: Cette méthode doit exister

            // Créer et afficher la nouvelle fenêtre
            Stage stage = new Stage();
            stage.setTitle("Réinitialisation du mot de passe");
            stage.setScene(new Scene(root));
            stage.show();

            System.out.println("✅ Fenêtre de réinitialisation ouverte avec succès!");

        } catch (Exception e) {
            System.err.println("❌ Erreur détaillée d'ouverture:");
            e.printStackTrace();
            showStatus("❌ Erreur d'ouverture: " + e.getMessage(), "red");
        }
    }
    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pidev/fxml/auth/login.fxml"));
            Stage stage = (Stage) backToLoginBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showStatus(String message, String color) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
        }
    }
}