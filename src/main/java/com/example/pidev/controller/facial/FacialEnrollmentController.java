package com.example.pidev.controller.facial;

import com.example.pidev.service.facial.CameraService;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.service.facial.FacialRecognitionService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.animation.AnimationTimer;

public class FacialEnrollmentController {

    @FXML private ImageView cameraView;
    @FXML private Button startCameraBtn;
    @FXML private Button captureFaceBtn;
    @FXML private Button enrollBtn;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private VBox facePreviewBox;

    private CameraService cameraService;
    private FacialRecognitionService facialService;
    private UserModel currentUser;
    private AnimationTimer timer;
    private boolean isCameraRunning = false;

    @FXML
    public void initialize() {
        cameraService = new CameraService();
        facialService = new FacialRecognitionService();

        captureFaceBtn.setDisable(true);
        enrollBtn.setDisable(true);
        progressIndicator.setVisible(false);

        System.out.println("✅ FacialEnrollmentController initialisé");
    }

    public void setUser(UserModel user) {
        this.currentUser = user;
    }

    @FXML
    private void handleStartCamera() {
        try {
            cameraService.startCamera();
            isCameraRunning = true;
            startCameraBtn.setDisable(true);
            captureFaceBtn.setDisable(false);

            statusLabel.setText("📷 Caméra active - Regardez l'écran");
            statusLabel.setStyle("-fx-text-fill: green;");

            startVideoStream();

        } catch (Exception e) {
            statusLabel.setText("❌ Erreur caméra: " + e.getMessage());
            statusLabel.setStyle("-fx-text-fill: red;");
        }
    }

    private void startVideoStream() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isCameraRunning) {
                    javafx.scene.image.Image frame = cameraService.captureFrame();
                    if (frame != null) {
                        cameraView.setImage(frame);
                    }
                }
            }
        };
        timer.start();
    }

    @FXML
    private void handleCaptureFace() {
        progressIndicator.setVisible(true);
        statusLabel.setText("🔍 Analyse du visage...");

        // Capturer le frame actuel
        javafx.scene.image.Image currentFrame = cameraView.getImage();

        // Traitement asynchrone
        new Thread(() -> {
            try {
                // Convertir l'image JavaFX en Mat OpenCV
                // (implémentation simplifiée - nécessite conversion)

                Thread.sleep(1000); // Simulation

                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    captureFaceBtn.setDisable(true);
                    enrollBtn.setDisable(false);
                    statusLabel.setText("✅ Visage capturé - Cliquez sur Enregistrer");
                    statusLabel.setStyle("-fx-text-fill: green;");

                    // Afficher un aperçu
                    facePreviewBox.setVisible(true);
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("❌ Erreur capture: " + e.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                });
            }
        }).start();
    }

    @FXML
    private void handleEnroll() {
        progressIndicator.setVisible(true);
        statusLabel.setText("💾 Enregistrement en cours...");

        new Thread(() -> {
            try {
                // Simuler l'enregistrement
                Thread.sleep(1500);

                boolean success = facialService.enrollFace(
                        currentUser.getId_User(),
                        currentUser.getFirst_Name() + " " + currentUser.getLast_Name(),
                        null // Mat image à passer
                );

                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);

                    if (success) {
                        statusLabel.setText("✅ Visage enregistré avec succès !");
                        statusLabel.setStyle("-fx-text-fill: green;");

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Succès");
                        alert.setHeaderText("Enregistrement facial réussi");
                        alert.setContentText("Vous pouvez maintenant vous connecter avec votre visage !");
                        alert.showAndWait();

                        stopCamera();
                    } else {
                        statusLabel.setText("❌ Échec de l'enregistrement");
                        statusLabel.setStyle("-fx-text-fill: red;");
                    }
                });

            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("❌ Erreur: " + e.getMessage());
                });
            }
        }).start();
    }

    private void stopCamera() {
        if (timer != null) {
            timer.stop();
        }
        if (cameraService != null) {
            cameraService.stopCamera();
        }
        isCameraRunning = false;
    }

    @FXML
    private void handleCancel() {
        stopCamera();
        // Fermer la fenêtre
        enrollBtn.getScene().getWindow().hide();
    }
}