package com.example.pidev.controller.role;

import com.example.pidev.MainController;
import com.example.pidev.model.role.Role;
import com.example.pidev.service.role.RoleService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;

import java.util.ResourceBundle;

public class EditRoleController implements Initializable {


    @FXML private Label formTitle;
    @FXML private Label formHint;
    @FXML private TextField roleNameField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    private RoleService roleService;
    private Role currentRole;
    private MainController mainController;
    private boolean isEditMode = false; // false = ajout, true = modification

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Initialiser RoleService avec gestion d'erreur
        try {
            roleService = new RoleService();
        } catch (SQLException e) {
            showAlert("Erreur Critique", "Impossible de se connecter à la base de données: " + e.getMessage());
            e.printStackTrace();
            return; // Arrêter l'initialisation si la DB ne fonctionne pas
        }

        // Initialiser les composants UI
        try {



            // Configurer les placeholders
            roleNameField.setPromptText("Entrez le nom du rôle");

            System.out.println("✅ EditRoleController initialisé");

        } catch (Exception e) {
            showAlert("Erreur", "Erreur d'initialisation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public void setAddMode() {
        this.isEditMode = false;
        this.currentRole = null;

        // Mettre à jour les labels pour le mode ajout

        formTitle.setText("➕ Nouveau rôle");
        formHint.setText("Ajoutez un nouveau rôle");

        // Vider le champ
        roleNameField.clear();
        roleNameField.setPromptText("Entrez le nom du nouveau rôle");

        // Changer le style du bouton pour le mode ajout
        saveButton.setStyle("-fx-background-color: linear-gradient(to right, #10b981, #059669);" +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;" +
                "-fx-background-radius: 10; -fx-padding: 12 30; -fx-pref-width: 180;" +
                "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(16,185,129,0.3), 8, 0, 0, 2);");
        saveButton.setText("➕ Ajouter");
    }

    /**
     * Configure le contrôleur en mode MODIFICATION
     */
    public void setEditMode(Role role) {
        this.isEditMode = true;
        this.currentRole = role;

        // Mettre à jour les labels pour le mode modification

        formTitle.setText("✏️ Modification du rôle");
        formHint.setText("Modifiez le rôle: " + role.getRoleName());

        // Remplir le champ avec les données existantes
        roleNameField.setText(role.getRoleName());
        roleNameField.setPromptText("Entrez le nouveau nom du rôle");

        // Changer le style du bouton pour le mode modification
        saveButton.setStyle("-fx-background-color: linear-gradient(to right, #3b82f6, #1d4ed8);" +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 15px;" +
                "-fx-background-radius: 10; -fx-padding: 12 30; -fx-pref-width: 180;" +
                "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(59,130,246,0.3), 8, 0, 0, 2);");
        saveButton.setText("💾 Enregistrer");
    }

    @FXML
    private void saveRole () {
        if (!validateFields()) return;

        try {
            if (isEditMode) {
                // Mode MODIFICATION
                currentRole.setRoleName(roleNameField.getText().trim());

                if (roleService.updateRole(currentRole)) {
                    showAlert("Succès", "Rôle modifié avec succès");
                    retournerAListe();
                } else {
                    showAlert("Erreur", "Échec de la modification");
                }
            } else {
                // Mode AJOUT
                Role newRole = new Role(roleNameField.getText().trim());

                if (roleService.addRole(newRole)) {
                    showAlert("Succès", "Rôle ajouté avec succès");
                    retournerAListe();
                } else {
                    showAlert("Erreur", "Échec de l'ajout");
                }
            }
        } catch (Exception e) {
            showAlert("Erreur", e.getMessage());
        }
    }

    @FXML
    private void cancelEdit() {
        retournerAListe();
    }

    private void retournerAListe() {
        if (mainController != null) {
            mainController.loadRoleView();
        } else {
            // Fermer la fenêtre si pas de MainController
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        }
    }

    private boolean validateFields() {
        if (roleNameField.getText() == null || roleNameField.getText().trim().isEmpty()) {
            showAlert("Champ manquant", "Le nom du rôle est obligatoire");
            roleNameField.requestFocus();
            return false;
        }
        return true;
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.setTitle(title);
        alert.showAndWait();
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
}
