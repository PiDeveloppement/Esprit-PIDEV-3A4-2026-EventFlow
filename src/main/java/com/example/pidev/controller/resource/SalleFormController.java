package com.example.pidev.controller.resource;

import com.example.pidev.MainController;
import com.example.pidev.model.resource.Salle;
import com.example.pidev.service.resource.SalleService;
import com.example.pidev.service.resource.UnsplashService;
import com.example.pidev.service.resource.VoiceRecognitionService;
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
import java.net.URL;
import java.util.ResourceBundle;

public class SalleFormController implements Initializable {

    @FXML private TextField nameField, capacityField, buildingField, floorField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label titleLabel;
    @FXML private ImageView previewImage;

    // --- ÉLÉMENTS VOCAUX ---
    @FXML private Button voiceBtn;
    @FXML private Label voiceStatusLabel;
    private VoiceRecognitionService voiceService;
    private boolean isListening = false;

    private final SalleService service = new SalleService();
    private String currentImagePath = "";
    private int selectedId = -1;

    private final UnsplashService unsplashService = new UnsplashService(); // INITIALISATION

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusCombo.setItems(FXCollections.observableArrayList("DISPONIBLE", "OCCUPEE"));
    }

    // --- LOGIQUE VOCALE POUR LES SALLES ---
    @FXML
    private void toggleVoiceControl() {
        if (!isListening) {
            isListening = true;
            voiceBtn.setStyle("-fx-background-color: #22c55e; -fx-background-radius: 50;");
            voiceStatusLabel.setText("🎙️ Écoute en cours...");

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
        voiceBtn.setStyle("-fx-background-color: #ef4444; -fx-background-radius: 50;");
        voiceStatusLabel.setText("Micro désactivé");
    }

    private void handleVoiceCommand(String cmd) {
        voiceStatusLabel.setText("🎙️ : " + cmd);

        // 1. NOM DE LA SALLE (ex: "nom salle de conférence")
        if (cmd.contains("nom")) {
            nameField.setText(cmd.replace("nom", "").trim());
        }

        // 2. BÂTIMENT (ex: "bâtiment a")
        if (cmd.contains("bâtiment") || cmd.contains("bloc")) {
            buildingField.setText(cmd.replace("bâtiment", "").replace("bloc", "").trim());
        }

        // 3. CAPACITÉ (Extraction du nombre)
        if (cmd.contains("capacité") || cmd.contains("place")) {
            String num = cmd.replaceAll("[^0-9]", "");
            if (!num.isEmpty()) capacityField.setText(num);
            else {
                if (cmd.contains("trente")) capacityField.setText("30");
                if (cmd.contains("cinquante")) capacityField.setText("50");
                if (cmd.contains("cent")) capacityField.setText("100");
            }
        }

        // 4. ÉTAGE (ex: "étage deux")
        if (cmd.contains("étage")) {
            String num = cmd.replaceAll("[^0-9]", "");
            if (!num.isEmpty()) floorField.setText(num);
            else {
                if (cmd.contains("premier")) floorField.setText("1");
                if (cmd.contains("deuxième")) floorField.setText("2");
                if (cmd.contains("troisième")) floorField.setText("3");
                if (cmd.contains("rez-de-chaussée")) floorField.setText("0");
            }
        }

        // 5. STATUT
        if (cmd.contains("disponible") || cmd.contains("libre")) statusCombo.setValue("DISPONIBLE");
        if (cmd.contains("occupée") || cmd.contains("prise")) statusCombo.setValue("OCCUPEE");

        // 6. ACTIONS GLOBALES
        if (cmd.contains("enregistrer") || cmd.contains("valider")) enregistrer();
        if (cmd.contains("annuler") || cmd.contains("retour")) annuler();
        if (cmd.contains("image") || cmd.contains("cherche")) rechercherImageAutomatique();
    }

    // ================= RESTE DU CODE (SANS CHANGEMENT) =================

    public void setSalleData(Salle s) {
        selectedId = s.getId();
        nameField.setText(s.getName());
        capacityField.setText(String.valueOf(s.getCapacity()));
        buildingField.setText(s.getBuilding());
        floorField.setText(String.valueOf(s.getFloor()));
        statusCombo.setValue(s.getStatus());
        currentImagePath = s.getImagePath();
        if (currentImagePath != null && !currentImagePath.isEmpty()) {
            try { previewImage.setImage(new Image(currentImagePath)); } catch (Exception e) {}
        }
        titleLabel.setText("Modifier la Salle");
    }

    private boolean estValide() {
        StringBuilder error = new StringBuilder();
        if (nameField.getText().trim().isEmpty()) error.append("- Nom obligatoire\n");
        if (buildingField.getText().trim().isEmpty()) error.append("- Bâtiment obligatoire\n");
        if (statusCombo.getValue() == null) error.append("- Statut obligatoire\n");
        try {
            int cap = Integer.parseInt(capacityField.getText());
            if (cap <= 0) error.append("- Capacité doit être positive\n");
        } catch (NumberFormatException e) { error.append("- Capacité doit être un nombre\n"); }
        try { Integer.parseInt(floorField.getText()); } catch (NumberFormatException e) { error.append("- Étage doit être un nombre\n"); }

        if (error.length() > 0) {
            new Alert(Alert.AlertType.WARNING, error.toString()).showAndWait();
            return false;
        }
        return true;
    }

    @FXML private void choisirImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File f = fc.showOpenDialog(null);
        if (f != null) {
            currentImagePath = f.toURI().toString();
            previewImage.setImage(new Image(currentImagePath));
        }
    }

    @FXML private void enregistrer() {
        if (estValide()) {
            try {
                Salle s = new Salle(selectedId == -1 ? 0 : selectedId, nameField.getText().trim(),
                        Integer.parseInt(capacityField.getText().trim()), buildingField.getText().trim(),
                        Integer.parseInt(floorField.getText().trim()), statusCombo.getValue(),
                        currentImagePath, 0.0, 0.0);
                if (selectedId == -1) service.ajouter(s);
                else service.modifier(s);
                annuler();
            } catch (Exception e) { new Alert(Alert.AlertType.ERROR, "Erreur lors de la sauvegarde").show(); }
        }
    }

    @FXML private void annuler() {
        stopVoiceControl();
        MainController.getInstance().showSalles();
    }
    @FXML
    void rechercherImageAutomatique() {
        String query = nameField.getText().trim();
        if (query.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Veuillez entrer le nom d'une salle.").show();
            return;
        }

        new Thread(() -> {
            String urlImage = unsplashService.getImageUrl(query);
            Platform.runLater(() -> {
                if (urlImage != null) {
                    currentImagePath = urlImage;
                    previewImage.setImage(new Image(urlImage));
                } else {
                    new Alert(Alert.AlertType.INFORMATION, "Aucune image trouvée pour : " + query).show();
                }
            });
        }).start();
    }
}