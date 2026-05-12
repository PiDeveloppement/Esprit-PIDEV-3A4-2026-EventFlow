package com.example.pidev.controller.user;

import com.example.pidev.MainController;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.service.role.RoleService;
import com.example.pidev.service.user.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;

public class EditUserController {
    @FXML private Label userInfoLabel;
    @FXML private TextField firstnameField;
    @FXML private TextField lastnameField;
    @FXML private TextField emailField;
    @FXML private TextField faculteField;
    @FXML private ComboBox<String> roleComboBox;

    private UserModel user;
    private UserService userService;
    private RoleService roleService;
    private MainController mainController;
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        try {
            userService = new UserService();
            roleService = new RoleService();
            roleComboBox.setItems(FXCollections.observableArrayList(roleService.getAllRoleNames()));
            System.out.println("Rôles chargés: " + roleComboBox.getItems().size());
        } catch (SQLException e) {
            showAlert("Erreur", "Impossible de charger les rôles : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setOnSaveCallback(Runnable callback) {
        this.onSaveCallback = callback;
    }

    public void setUser(UserModel user) {
        this.user = user;
        if (user != null) {
            firstnameField.setText(user.getFirst_Name());
            lastnameField.setText(user.getLast_Name());
            emailField.setText(user.getEmail());
            faculteField.setText(user.getFaculte());
            if (user.getRole() != null) {
                roleComboBox.setValue(user.getRole().getRoleName());
            }
            userInfoLabel.setText("Modification de: " + user.getFirst_Name() + " " + user.getLast_Name());
        }
    }

    @FXML
    private void handleSave() {
        if (!validateFields()) return;

        user.setFirst_Name(firstnameField.getText());
        user.setLast_Name(lastnameField.getText());
        user.setEmail(emailField.getText());
        user.setFaculte(faculteField.getText());

        String roleName = roleComboBox.getValue();
        if (roleName == null || roleName.isEmpty()) {
            showAlert("Erreur", "Veuillez sélectionner un rôle");
            return;
        }

        try {
            int roleId = roleService.getRoleIdByName(roleName);
            if (roleId <= 0) {
                showAlert("Erreur", "Rôle invalide");
                return;
            }
            user.setRole_Id(roleId);

            if (userService.updateUser(user)) {
                showAlert("Succès", "Utilisateur mis à jour avec succès");
                if (onSaveCallback != null) {
                    onSaveCallback.run();
                }
                handleBack();
            } else {
                showAlert("Erreur", "Échec de la mise à jour");
            }
        } catch (SQLException e) {
            showAlert("Erreur", e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancel() {
        handleBack();
    }

    @FXML
    private void handleBack() {
        if (mainController != null) {
            mainController.loadUserView();
        }
    }

    private boolean validateFields() {
        if (firstnameField.getText().trim().isEmpty()) {
            showAlert("Champ manquant", "Le prénom est obligatoire.");
            return false;
        }
        if (lastnameField.getText().trim().isEmpty()) {
            showAlert("Champ manquant", "Le nom est obligatoire.");
            return false;
        }
        if (emailField.getText().trim().isEmpty()) {
            showAlert("Champ manquant", "L'email est obligatoire.");
            return false;
        }
        return true;
    }

    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}