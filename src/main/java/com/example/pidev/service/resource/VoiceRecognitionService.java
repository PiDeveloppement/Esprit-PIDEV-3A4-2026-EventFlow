package com.example.pidev.service.resource;

import org.vosk.Model;
import org.vosk.Recognizer;
import javax.sound.sampled.*;
import java.io.File;

public class VoiceRecognitionService extends Thread {

    private final VoiceCommandListener listener;
    private volatile boolean running = true;

    public interface VoiceCommandListener {
        void onCommandDetected(String jsonResult);
    }

    public VoiceRecognitionService(VoiceCommandListener listener) {
        this.listener = listener;
    }

    @Override
    public void run() {
        Model model = null;
        Recognizer recognizer = null;
        TargetDataLine line = null;

        try {
            // ON UTILISE TON CHEMIN PRÉCIS
            // User.dir récupère C:\Users\meche\Desktop\pi25-01-2026
            String userPath = System.getProperty("user.dir");
            String modelPath = userPath + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator + "com" + File.separator + "example" + File.separator + "pidev" + File.separator + "model" + File.separator + "resource" + File.separator + "fr";

            File modelFile = new File(modelPath);

            if (!modelFile.exists()) {
                System.err.println("❌ Modèle Vosk introuvable au chemin : " + modelPath);
                return;
            }

            model = new Model(modelFile.getAbsolutePath());
            recognizer = new Recognizer(model, 16000.0f);
            System.out.println("✅ Modèle Vosk chargé depuis : " + modelPath);

            // Config Audio
            AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            byte[] buffer = new byte[4096];
            while (running) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                        listener.onCommandDetected(recognizer.getResult());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur Service Vocal: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (line != null) { line.stop(); line.close(); }
            if (recognizer != null) recognizer.close();
            if (model != null) model.close();
        }
    }

    public void stopListening() {
        this.running = false;
    }
}