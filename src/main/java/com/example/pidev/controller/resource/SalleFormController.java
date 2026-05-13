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

    // ===== FXML Fields =====
    @FXML private TextField nameField;
    @FXML private TextField capacityField;
    @FXML private TextField buildingField;
    @FXML private TextField floorField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label titleLabel;
    @FXML private ImageView previewImage;

    // ===== Voice =====
    @FXML private Button voiceBtn;
    @FXML private Label voiceStatusLabel;
    private VoiceRecognitionService voiceService;
    private boolean isListening = false;

    // ===== Services & State =====
    private final SalleService service       = new SalleService();
    private final UnsplashService unsplashService = new UnsplashService();
    private String currentImagePath = "";
    private int selectedId = -1;

    // ====================================================================
    //  INIT
    // ====================================================================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        statusCombo.setItems(FXCollections.observableArrayList("DISPONIBLE", "OCCUPEE"));
    }

    // ====================================================================
    //  MODE ÉDITION — appelé depuis SalleController
    // ====================================================================
    public void setSalleData(Salle s) {
        selectedId = s.getId();
        nameField.setText(s.getName());
        capacityField.setText(String.valueOf(s.getCapacity()));
        buildingField.setText(s.getBuilding());
        floorField.setText(String.valueOf(s.getFloor()));
        statusCombo.setValue(s.getStatus());
        currentImagePath = s.getImagePath() != null ? s.getImagePath() : "";

        // Titre dynamique comme la page web "Modifier la Salle"
        titleLabel.setText("Modifier la Salle");

        // Preview image
        if (!currentImagePath.isEmpty()) {
            try {
                previewImage.setImage(new Image(currentImagePath, true));
            } catch (Exception ignored) {}
        }
    }

    // ====================================================================
    //  VOICE CONTROL
    // ====================================================================
    @FXML
    private void toggleVoiceControl() {
        if (!isListening) {
            startVoiceControl();
        } else {
            stopVoiceControl();
        }
    }

    private void startVoiceControl() {
        isListening = true;
        voiceBtn.setStyle(
                "-fx-background-color: #22c55e; -fx-background-radius: 50;" +
                        "-fx-text-fill: white; -fx-font-size: 16; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(34,197,94,0.3), 5, 0, 0, 2);"
        );
        voiceStatusLabel.setText("🎙️ Écoute en cours...");

        voiceService = new VoiceRecognitionService(json -> {
            JSONObject obj = new JSONObject(json);
            String text = obj.optString("text", "").toLowerCase();
            if (!text.isEmpty()) {
                Platform.runLater(() -> handleVoiceCommand(text));
            }
        });
        voiceService.start();
    }

    private void stopVoiceControl() {
        isListening = false;
        if (voiceService != null) voiceService.stopListening();
        voiceBtn.setStyle(
                "-fx-background-color: #ef4444; -fx-background-radius: 50;" +
                        "-fx-text-fill: white; -fx-font-size: 16; -fx-cursor: hand;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);"
        );
        voiceStatusLabel.setText("Micro Off");
    }

    private void handleVoiceCommand(String cmd) {
        voiceStatusLabel.setText("🎙️ : " + cmd);

        // 1. Nom de la salle
        if (cmd.contains("nom")) {
            nameField.setText(cmd.replace("nom", "").trim());
        }

        // 2. Bâtiment / Bloc
        if (cmd.contains("bâtiment") || cmd.contains("bloc")) {
            buildingField.setText(
                    cmd.replace("bâtiment", "").replace("bloc", "").trim()
            );
        }

        // 3. Capacité
        if (cmd.contains("capacité") || cmd.contains("place")) {
            String num = cmd.replaceAll("[^0-9]", "");
            if (!num.isEmpty()) {
                capacityField.setText(num);
            } else {
                if (cmd.contains("trente"))     capacityField.setText("30");
                if (cmd.contains("cinquante"))  capacityField.setText("50");
                if (cmd.contains("cent"))       capacityField.setText("100");
            }
        }

        // 4. Étage
        if (cmd.contains("étage")) {
            String num = cmd.replaceAll("[^0-9]", "");
            if (!num.isEmpty()) {
                floorField.setText(num);
            } else {
                if (cmd.contains("premier"))         floorField.setText("1");
                if (cmd.contains("deuxième"))        floorField.setText("2");
                if (cmd.contains("troisième"))       floorField.setText("3");
                if (cmd.contains("rez-de-chaussée"))floorField.setText("0");
            }
        }

        // 5. Statut
        if (cmd.contains("disponible") || cmd.contains("libre"))
            statusCombo.setValue("DISPONIBLE");
        if (cmd.contains("occupée") || cmd.contains("prise"))
            statusCombo.setValue("OCCUPEE");

        // 6. Actions globales
        if (cmd.contains("enregistrer") || cmd.contains("valider")) enregistrer();
        if (cmd.contains("annuler")     || cmd.contains("retour"))  annuler();
        if (cmd.contains("image")       || cmd.contains("cherche")) rechercherImageAutomatique();
    }

    // ====================================================================
    //  IMAGE
    // ====================================================================
    @FXML
    private void choisirImage() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir une image");
        fc.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        File f = fc.showOpenDialog(nameField.getScene().getWindow());
        if (f != null) {
            currentImagePath = f.toURI().toString();
            try {
                previewImage.setImage(new Image(currentImagePath, true));
            } catch (Exception ignored) {}
        }
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
                    try {
                        previewImage.setImage(new Image(urlImage, true));
                    } catch (Exception ignored) {}
                } else {
                    new Alert(Alert.AlertType.INFORMATION,
                            "Aucune image trouvée pour : " + query).show();
                }
            });
        }).start();
    }

    // ====================================================================
    //  VALIDATION
    // ====================================================================
    private boolean estValide() {
        StringBuilder error = new StringBuilder();

        if (nameField.getText().trim().isEmpty())
            error.append("• Nom obligatoire\n");

        if (buildingField.getText().trim().isEmpty())
            error.append("• Bâtiment obligatoire\n");

        if (statusCombo.getValue() == null)
            error.append("• Statut obligatoire\n");

        try {
            int cap = Integer.parseInt(capacityField.getText().trim());
            if (cap <= 0 || cap > 1000)
                error.append("• Capacité doit être entre 1 et 1000\n");
        } catch (NumberFormatException e) {
            error.append("• Capacité doit être un nombre\n");
        }

        try {
            int floor = Integer.parseInt(floorField.getText().trim());
            if (floor < 0 || floor > 50)
                error.append("• Étage doit être entre 0 et 50\n");
        } catch (NumberFormatException e) {
            error.append("• Étage doit être un nombre\n");
        }

        if (error.length() > 0) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Erreurs de validation");
            alert.setHeaderText("Veuillez corriger les erreurs suivantes :");
            alert.setContentText(error.toString());
            alert.showAndWait();
            return false;
        }
        return true;
    }

    // ====================================================================
    //  ENREGISTRER / ANNULER
    // ====================================================================
    @FXML
    private void enregistrer() {
        if (!estValide()) return;

        try {
            Salle s = new Salle(
                    selectedId == -1 ? 0 : selectedId,
                    nameField.getText().trim(),
                    Integer.parseInt(capacityField.getText().trim()),
                    buildingField.getText().trim(),
                    Integer.parseInt(floorField.getText().trim()),
                    statusCombo.getValue(),
                    currentImagePath,
                    0.0, 0.0
            );

            if (selectedId == -1) service.ajouter(s);
            else                  service.modifier(s);

            annuler();

        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Erreur lors de la sauvegarde : " + e.getMessage()).show();
        }
    }

    @FXML
    private void annuler() {
        stopVoiceControl();
        MainController.getInstance().showSalles();
    }
}