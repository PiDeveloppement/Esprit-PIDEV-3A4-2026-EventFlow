package com.example.pidev.controller.resource;

import com.example.pidev.MainController;
import com.example.pidev.model.resource.Equipement;
import com.example.pidev.service.resource.EquipementService;
import com.example.pidev.service.resource.VoiceRecognitionService; // Import du service
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.example.pidev.service.resource.UnsplashService;

public class EquipementFormController implements Initializable {
    @FXML private TextField nameField, typeField, quantityField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label titleLabel;
    @FXML private ImageView previewImage;

    // --- ÉLÉMENTS POUR LE VOCAL ---
    @FXML private Button voiceBtn;
    @FXML private Label voiceStatusLabel;
    private VoiceRecognitionService voiceService;
    private boolean isListening = false;

    private final EquipementService service = new EquipementService();
    private String currentImagePath = "";
    private int selectedId = -1;

    private final UnsplashService unsplashService = new UnsplashService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusCombo.setItems(FXCollections.observableArrayList("DISPONIBLE", "EN_PANNE"));
    }

    // --- LOGIQUE VOCALE ---
    @FXML
    private void toggleVoiceControl() {
        if (!isListening) {
            isListening = true;
            if (voiceBtn != null) voiceBtn.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 50;");

            voiceService = new VoiceRecognitionService(json -> {
                JSONObject obj = new JSONObject(json);
                String text = obj.optString("text", "").toLowerCase();
                if (!text.isEmpty()) {
                    Platform.runLater(() -> handleVoiceCommand(text));
                }
            });
            voiceService.start();
        } else {
            stopVoiceControl();
        }
    }

    private void stopVoiceControl() {
        isListening = false;
        if (voiceService != null) voiceService.stopListening();
        if (voiceBtn != null) voiceBtn.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 50;");
    }

    private void handleVoiceCommand(String cmd) {
        if (voiceStatusLabel != null) voiceStatusLabel.setText("🎙️ : " + cmd);

        // 1. NOM (ex: "nom micro")
        if (cmd.contains("nom")) {
            nameField.setText(cmd.replace("nom", "").trim());
        }

        // 2. TYPE (ex: "type électronique")
        if (cmd.contains("type")) {
            typeField.setText(cmd.replace("type", "").trim());
        }

        // 3. STATUT (ex: "statut disponible" ou "en panne")
        if (cmd.contains("disponible")) statusCombo.setValue("DISPONIBLE");
        if (cmd.contains("panne")) statusCombo.setValue("EN_PANNE");

        // 4. QUANTITÉ (ex: "quantité vingt")
        if (cmd.contains("quantité") || cmd.contains("nombre")) {
            String num = cmd.replaceAll("[^0-9]", "");
            if (!num.isEmpty()) {
                quantityField.setText(num);
            } else {
                // Conversion texte vers chiffre
                if (cmd.contains("dix")) quantityField.setText("10");
                if (cmd.contains("vingt")) quantityField.setText("20");
                if (cmd.contains("cinquante")) quantityField.setText("50");
            }
        }

        // 5. ACTIONS
        if (cmd.contains("enregistrer") || cmd.contains("valider") || cmd.contains("sauvegarder")) {
            enregistrer();
            stopVoiceControl();
        }
        if (cmd.contains("annuler") || cmd.contains("quitter")) {
            annuler();
        }
        if (cmd.contains("image") || cmd.contains("photo")) {
            rechercherImageAutomatique();
        }
    }

    // --- LE RESTE DE TON CODE RESTE IDENTIQUE ---

    public void setEquipementData(Equipement e) {
        selectedId = e.getId();
        nameField.setText(e.getName());
        typeField.setText(e.getType());
        quantityField.setText(String.valueOf(e.getQuantity()));
        statusCombo.setValue(e.getStatus());
        currentImagePath = e.getImagePath();
        if (currentImagePath != null && !currentImagePath.isEmpty()) {
            previewImage.setImage(new Image(new File(currentImagePath).toURI().toString()));
        }
        titleLabel.setText("Modifier l'Équipement");
    }

    private boolean estValide() {
        StringBuilder erreurs = new StringBuilder();
        if (nameField.getText().trim().isEmpty()) erreurs.append("- Le nom est obligatoire.\n");
        if (typeField.getText().trim().isEmpty()) erreurs.append("- Le type est obligatoire.\n");
        if (statusCombo.getValue() == null) erreurs.append("- Veuillez sélectionner un statut.\n");
        try {
            int qte = Integer.parseInt(quantityField.getText());
            if (qte < 0) erreurs.append("- La quantité ne peut pas être négative.\n");
        } catch (NumberFormatException e) {
            erreurs.append("- La quantité doit être un nombre valide.\n");
        }
        if (erreurs.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING, erreurs.toString());
            alert.showAndWait();
            return false;
        }
        return true;
    }

    @FXML
    void choisirImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File selectedFile = fc.showOpenDialog(null);
        if (selectedFile != null) {
            try {
                File uploadDir = new File("uploads");
                if (!uploadDir.exists()) uploadDir.mkdir();
                File destFile = new File(uploadDir.getPath() + "/" + selectedFile.getName());
                java.nio.file.Files.copy(selectedFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                currentImagePath = destFile.getAbsolutePath();
                previewImage.setImage(new Image(destFile.toURI().toString()));
            } catch (IOException e) { e.printStackTrace(); }
        }
    }

    @FXML
    void enregistrer() {
        if (estValide()) {
            try {
                Equipement e = new Equipement(
                        selectedId == -1 ? 0 : selectedId,
                        nameField.getText().trim(),
                        typeField.getText().trim(),
                        statusCombo.getValue(),
                        Integer.parseInt(quantityField.getText().trim()),
                        currentImagePath
                );
                if (selectedId == -1) service.ajouter(e);
                else service.modifier(e);
                annuler();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    @FXML
    void annuler() {
        stopVoiceControl();
        MainController.getInstance().showEquipements();
    }
    @FXML
    void rechercherImageAutomatique() {
        String query = nameField.getText().trim();
        if (query.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Veuillez saisir un nom d'équipement d'abord.");
            alert.show();
            return;
        }

        // Exécuter dans un thread séparé pour ne pas figer l'interface
        new Thread(() -> {
            String imageUrl = unsplashService.getImageUrl(query);

            Platform.runLater(() -> {
                if (imageUrl != null) {
                    currentImagePath = imageUrl; // On stocke l'URL au lieu du chemin local
                    previewImage.setImage(new Image(imageUrl));
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Aucune image trouvée pour : " + query);
                    alert.show();
                }
            });
        }).start();
    }
}