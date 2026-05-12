package com.example.pidev.controller.auth;

import com.example.pidev.HelloApplication;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.service.user.UserService;
import com.example.pidev.service.user.EmailService; // ✅ AJOUTER CET IMPORT
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class SignupController implements Initializable {

    @FXML private TextField Id_User;
    @FXML private TextField Password;
    @FXML private TextField ConfirmerPassword;
    @FXML private TextField First_Name;
    @FXML private TextField Last_Name;
    @FXML private TextField Email;
    @FXML private TextField Faculte;
    @FXML private Label ConfirmerPasswordLabel;
    @FXML private Label RegistrationMessageLabel;
    @FXML private Label emailLabel;
    @FXML private Button closeButton;

    // ⚠️ IMPORTANT: Vous avez oublié ce label dans votre FXML !
    @FXML private Label passwordLabel;  // À AJOUTER dans votre signup.fxml

    private UserService userService;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            userService = new UserService();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // === VALIDATION EMAIL ===
        Email.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validateEmail();
            }
        });

        // === VALIDATION MOT DE PASSE ===
        Password.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePassword();
            if (!ConfirmerPassword.getText().isEmpty()) {
                validatePasswordMatch();
            }
        });

        Password.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validatePassword();
            }
        });

        // === VALIDATION CONFIRMATION ===
        ConfirmerPassword.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePasswordMatch();
        });

        ConfirmerPassword.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                validatePasswordMatch();
            }
        });
    }

    // ================ VALIDATION EMAIL ================
    private boolean validateEmail() {
        String email = Email.getText();
        if (email == null || email.trim().isEmpty()) {
            emailLabel.setText("L'email est requis");
            emailLabel.setStyle("-fx-text-fill: red;");
            Email.setStyle("-fx-border-color: red;");
            return false;
        } else if (!isValidEmail(email)) {
            emailLabel.setText("Email invalide (ex: test@gmail.com)");
            emailLabel.setStyle("-fx-text-fill: red;");
            Email.setStyle("-fx-border-color: red;");
            return false;
        } else {
            emailLabel.setText("✓ Email valide");
            emailLabel.setStyle("-fx-text-fill: green;");
            Email.setStyle("-fx-border-color: green;");
            return true;
        }
    }

    // Validation email utilitaire
    private boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    // ================ VALIDATION MOT DE PASSE ================
    private boolean validatePassword() {
        String password = Password.getText();

        // Règles: min 6 car, 1 lettre, 1 chiffre, 1 symbole
        String regex = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?])[A-Za-z\\d!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]{6,}$";

        if (password == null || password.isEmpty()) {
            if (passwordLabel != null) {
                passwordLabel.setText("Le mot de passe est requis");
                passwordLabel.setStyle("-fx-text-fill: red;");
            }
            Password.setStyle("-fx-border-color: red;");
            return false;
        }

        if (!password.matches(regex)) {
            if (passwordLabel != null) {
                passwordLabel.setText("6+ caractères avec lettre, chiffre ET symbole");
                passwordLabel.setStyle("-fx-text-fill: red;");
            }
            Password.setStyle("-fx-border-color: red;");
            return false;
        }

        if (passwordLabel != null) {
            passwordLabel.setText("✓ Mot de passe fort");
            passwordLabel.setStyle("-fx-text-fill: green;");
        }
        Password.setStyle("-fx-border-color: green;");
        return true;
    }

    // ================ VALIDATION CONFIRMATION ================
    private boolean validatePasswordMatch() {
        String password = Password.getText();
        String confirmPassword = ConfirmerPassword.getText();

        if (confirmPassword.isEmpty()) {
            ConfirmerPasswordLabel.setText("");
            ConfirmerPassword.setStyle("");
            return false;
        }

        if (password.equals(confirmPassword)) {
            ConfirmerPasswordLabel.setText("✓ Mots de passe identiques");
            ConfirmerPasswordLabel.setStyle("-fx-text-fill: green;");
            ConfirmerPassword.setStyle("-fx-border-color: green;");
            return true;
        } else {
            ConfirmerPasswordLabel.setText("✗ Les mots de passe ne correspondent pas");
            ConfirmerPasswordLabel.setStyle("-fx-text-fill: red;");
            ConfirmerPassword.setStyle("-fx-border-color: red;");
            return false;
        }
    }

    // ================ VALIDATION COMPLÈTE ================
    private boolean validateAll() {
        boolean isValid = true;
        isValid &= validateEmail();
        isValid &= validatePassword();
        isValid &= validatePasswordMatch();
        // Ajoutez d'autres validations ici (prénom, nom, faculté)
        return isValid;
    }

    // ================ BOUTON D'INSCRIPTION ================
    @FXML
    public void registerButtonOnAction(ActionEvent event) throws IOException {

        // ✅ Validation complète avant soumission
        if (!validateAll()) {
            RegistrationMessageLabel.setText("Veuillez saisir un mot de passe avec : min 6 car, 1 lettre, 1 chiffre, 1 symbole");
            RegistrationMessageLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        // 1️⃣ Vérifier si l'email existe déjà
        if (userService.isEmailExists(Email.getText())) {
            emailLabel.setText("Cet email est déjà utilisé");
            emailLabel.setStyle("-fx-text-fill: red;");
            Email.setStyle("-fx-border-color: red;");
            RegistrationMessageLabel.setText("Email déjà existant");
            return;
        }

        // 2️⃣ Création de l'utilisateur
        UserModel user = new UserModel(
                First_Name.getText(),
                Last_Name.getText(),
                Email.getText(),
                Faculte.getText(),
                Password.getText(),
                1
        );

        // 3️⃣ Enregistrement via UserService
        boolean success = userService.registerUser(user);

        if (success) {
            RegistrationMessageLabel.setText("✓ Inscription réussie !");
            RegistrationMessageLabel.setStyle("-fx-text-fill: green;");

            // ✅ 4️⃣ ENVOYER LA NOTIFICATION À L'ADMIN (APPEL AJOUTÉ)
            new Thread(() -> {
                try {
                    System.out.println("📧 Envoi notification admin pour: " + user.getEmail());
                    EmailService.sendNewUserNotificationToAdmin(user);
                } catch (Exception e) {
                    System.err.println("⚠️ Erreur envoi notification admin: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();

            // Redirection vers Login
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pidev/fxml/auth/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } else {
            RegistrationMessageLabel.setText("✗ Échec de l'inscription");
            RegistrationMessageLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // Bouton fermeture
    @FXML
    public void closeButtonOnAction(ActionEvent event){
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
        Platform.exit();
    }

    // Redirection vers login
    @FXML
    private void goToLogin(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/pidev/fxml/auth/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToLanding(ActionEvent event) {
        HelloApplication.loadLandingPage();
    }
}