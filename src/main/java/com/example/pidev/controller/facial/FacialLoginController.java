package com.example.pidev.controller.facial;

import com.example.pidev.HelloApplication;
import com.example.pidev.model.user.UserModel;
import com.example.pidev.service.facial.CameraService;
import com.example.pidev.service.facial.FacialRecognitionService;
import com.example.pidev.service.user.UserService;
import com.example.pidev.utils.UserSession;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// ✅ Nouveaux imports JavaCV (Bytedeco)
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class FacialLoginController implements Initializable {

    @FXML private ImageView cameraView;
    @FXML private Button startCameraBtn;
    @FXML private Button loginBtn;
    @FXML private Button backToLoginBtn;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private VBox resultBox;
    @FXML private Label userNameLabel;
    @FXML private Label confidenceLabel;

    private CameraService cameraService;
    private FacialRecognitionService facialService;
    private UserService userService;
    private AnimationTimer timer;
    private boolean isCameraRunning = false;
    private UserModel detectedUser = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialisation des services adaptés à JavaCV
        cameraService = new CameraService();
        facialService = new FacialRecognitionService();
        userService = new UserService();

        loginBtn.setDisable(true);
        resultBox.setVisible(false);
        progressIndicator.setVisible(false);

        System.out.println("✅ FacialLoginController initialisé avec JavaCV (Bytedeco)");
    }

    @FXML
    private void handleStartCamera() {
        try {
            boolean started = cameraService.startCamera();
            if (started) {
                isCameraRunning = true;
                startCameraBtn.setDisable(true);
                startCameraBtn.setText("📷 Caméra active");

                statusLabel.setText("🔍 Recherche de visage...");
                statusLabel.setStyle("-fx-text-fill: #2196F3;");

                startFaceDetectionLoop();
            } else {
                statusLabel.setText("❌ Erreur démarrage caméra");
                statusLabel.setStyle("-fx-text-fill: red;");
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startFaceDetectionLoop() {
        timer = new AnimationTimer() {
            private long lastDetection = 0;

            @Override
            public void handle(long now) {
                if (!isCameraRunning) return;

                // 1. Affichage du flux (Conversion ultra-rapide JavaCV -> JavaFX)
                javafx.scene.image.Image frameImage = cameraService.captureFrame();
                if (frameImage != null) {
                    cameraView.setImage(frameImage);
                }

                // 2. Détection toutes les 500ms pour ne pas saturer le CPU
                if (now - lastDetection > 500_000_000) {
                    lastDetection = now;
                    runDetectionTask();
                }
            }
        };
        timer.start();
    }

    private void runDetectionTask() {
        // Utilisation d'un thread séparé pour l'analyse d'image
        new Thread(() -> {
            try {
                Mat frame = cameraService.captureRawFrame();

                if (frame != null && !frame.empty()) {
                    // Utilisation de la classe Rect de JavaCV
                    Rect face = facialService.detectFace(frame);

                    if (face != null) {
                        Mat processedFace = facialService.preprocessFace(frame, face);

                        if (processedFace != null) {
                            int userId = facialService.recognizeFace(processedFace);

                            if (userId > 0) {
                                UserModel user = userService.getUserById(userId);
                                if (user != null) {
                                    Platform.runLater(() -> updateUIWithDetectedUser(user));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Erreur tâche de détection: " + e.getMessage());
            }
        }).start();
    }

    private void updateUIWithDetectedUser(UserModel user) {
        detectedUser = user;
        loginBtn.setDisable(false);
        statusLabel.setText("✅ Visage reconnu !");
        statusLabel.setStyle("-fx-text-fill: #4CAF50;");

        resultBox.setVisible(true);
        userNameLabel.setText(user.getFirst_Name() + " " + user.getLast_Name());
        confidenceLabel.setText("Confiance élevée");
    }

    @FXML
    private void handleLogin() {
        if (detectedUser == null) return;

        progressIndicator.setVisible(true);
        statusLabel.setText("🔐 Connexion en cours...");

        new Thread(() -> {
            try {
                // Simulation d'authentification sécurisée
                Thread.sleep(800);

                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    UserSession.getInstance().setCurrentUser(detectedUser);
                    stopCamera();

                    try {
                        HelloApplication.loadDashboard();
                    } catch (Exception e) {
                        statusLabel.setText("❌ Erreur redirection");
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    @FXML
    private void handleBackToLogin() {
        stopCamera();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/com/example/pidev/fxml/auth/login.fxml"));
            Stage stage = (Stage) backToLoginBtn.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopCamera() {
        if (timer != null) timer.stop();
        if (cameraService != null) cameraService.stopCamera();
        isCameraRunning = false;
    }
    @FXML
    private void handleCapture() {
        // Dans ton architecture actuelle, la capture est automatique via le timer
        // Mais si tu veux forcer une détection immédiate au clic :
        statusLabel.setText("📸 Analyse en cours...");
        runDetectionTask();
    }

    @FXML
    private void handleCancel() {
        // On réutilise ta méthode existante pour arrêter proprement
        stopCamera();

        // Retour à la page précédente (Login classique)
        handleBackToLogin();
    }
}