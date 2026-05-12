package com.example.pidev.service.facial;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.JavaFXFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.OpenCVFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import javafx.scene.image.Image;

public class CameraService {

    private OpenCVFrameGrabber grabber;
    private JavaFXFrameConverter fxConverter;
    private OpenCVFrameConverter.ToMat matConverter;
    private boolean isRunning = false;
    private static final int CAMERA_ID = 0;

    public CameraService() {
        // JavaCV utilise un Grabber au lieu de VideoCapture pour plus de flexibilité
        grabber = new OpenCVFrameGrabber(CAMERA_ID);
        fxConverter = new JavaFXFrameConverter();
        matConverter = new OpenCVFrameConverter.ToMat();
    }

    /**
     * Démarre la capture vidéo
     */
    public boolean startCamera() {
        try {
            if (!isRunning) {
                grabber.start();
                isRunning = true;
                System.out.println("📷 Caméra JavaCV démarrée");
            }
            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur démarrage caméra: " + e.getMessage());
            return false;
        }
    }

    /**
     * Arrête la caméra proprement
     */
    public void stopCamera() {
        try {
            if (grabber != null) {
                grabber.stop();
                grabber.release();
                isRunning = false;
                System.out.println("⏹️ Caméra arrêtée");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur arrêt caméra: " + e.getMessage());
        }
    }

    /**
     * Capture une frame et la convertit directement en Image JavaFX
     * Très rapide avec JavaCV
     */
    public Image captureFrame() {
        if (!isRunning) return null;

        try {
            Frame frame = grabber.grab();
            if (frame != null) {
                // Conversion directe native : Frame -> WritableImage (JavaFX)
                return fxConverter.convert(frame);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur capture frame: " + e.getMessage());
        }
        return null;
    }

    /**
     * Capture une frame au format Mat (pour la reconnaissance faciale)
     */
    public Mat captureRawFrame() {
        if (!isRunning) return null;

        try {
            Frame frame = grabber.grab();
            if (frame != null) {
                // Conversion Frame -> Mat
                return matConverter.convert(frame);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void close() {
        stopCamera();
    }
}