package com.example.pidev.controller.user;

import com.example.pidev.service.user.PasswordResetService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class ResetPasswordController {

    @FXML private TextField tokenField;
    @FXML private Button validateTokenBtn;
    @FXML private VBox passwordFormBox;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button resetPasswordBtn;
    @FXML private Button backToLoginBtn;
    @FXML private Label statusLabel;

    private PasswordResetService resetService;
    private String currentToken; // Stocker le token temporairement

    @FXML
    public void initialize() {
        resetService = new PasswordResetService();
        passwordFormBox.setVisible(false);
        passwordFormBox.setManaged(false);

        // ✅ S'assurer que le champ token est VIDE au démarrage
        tokenField.clear();

        System.out.println("✅ ResetPasswordController initialisé - champ token vide");
    }

    /**
     * Cette méthode n'est plus utilisée pour pré-remplir
     * On la garde juste pour stocker le token si nécessaire
     */
    public void setToken(String token) {
        // ✅ NE PLUS pré-remplir le champ
        this.currentToken = token;
        System.out.println("🔑 Token reçu (non affiché): " + token);

        // Optionnel: Afficher un message pour guider l'utilisateur
        statusLabel.setText("📱 Collez le token reçu par WhatsApp dans le champ ci-dessus");
        statusLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
    }

    @FXML
    private void handleValidateToken() {
        String token = tokenField.getText().trim();

        if (token.isEmpty()) {
            showStatus("❌ Veuillez saisir le token reçu par WhatsApp", "red");
            return;
        }

        System.out.println("🔍 Validation du token saisi par l'utilisateur: " + token);

        // Valider le token
        boolean isValid = resetService.validateToken(token);

        if (isValid) {
            showStatus("✅ Token valide ! Saisissez votre nouveau mot de passe", "green");
            tokenField.setDisable(true);
            validateTokenBtn.setDisable(true);
            passwordFormBox.setVisible(true);
            passwordFormBox.setManaged(true);
            currentToken = token; // Stocker le token validé
        } else {
            showStatus("❌ Token invalide ou expiré. Demandez un nouveau lien.", "red");
        }
    }

    @FXML
    private void handleResetPassword() {
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation
        if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showStatus("❌ Veuillez remplir tous les champs", "red");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showStatus("❌ Les mots de passe ne correspondent pas", "red");
            return;
        }

        if (newPassword.length() < 6) {
            showStatus("❌ Le mot de passe doit contenir au moins 6 caractères", "red");
            return;
        }

        // ✅ Le mot de passe sera hashé dans le service, pas ici
        boolean success = resetService.resetPassword(newPassword);

        if (success) {
            showStatus("✅ Mot de passe réinitialisé avec succès !", "green");

            // Redirection vers login après 2 secondes
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    javafx.application.Platform.runLater(this::handleBackToLogin);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showStatus("❌ Erreur lors de la réinitialisation", "red");
        }
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/com/example/pidev/fxml/auth/login.fxml")
            );
            Stage stage = (Stage) backToLoginBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showStatus("❌ Erreur de navigation", "red");
        }
    }

    private void showStatus(String message, String color) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
    }
}