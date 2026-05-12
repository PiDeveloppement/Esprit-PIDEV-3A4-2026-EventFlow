package com.example.pidev.service.facial;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_face.*;
import org.bytedeco.opencv.opencv_objdetect.*;
import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_face.*;

import java.io.File;
import java.io.InputStream;
import java.nio.IntBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FacialRecognitionService {

    private CascadeClassifier faceDetector;
    private LBPHFaceRecognizer faceRecognizer;
    private Map<Integer, String> userIdToNameMap;
    private boolean isModelTrained = false;

    private static final String TRAINING_DATA_PATH = "facial_data/";
    private static final String MODEL_PATH = TRAINING_DATA_PATH + "lbph_model.yml";

    public FacialRecognitionService() {
        this.userIdToNameMap = new ConcurrentHashMap<>();
        initFaceDetector();
        initFaceRecognizer();
        createTrainingDirectory();

        // Essayer de charger un modèle existant OU d'entraîner avec les images disponibles
        if (!loadExistingModel()) {
            System.out.println("ℹ️ Aucun modèle existant trouvé. Tentative d'entraînement avec les images disponibles...");
            initialTrain();
        }
    }

    private void initFaceDetector() {
        try {
            InputStream is = getClass().getResourceAsStream("/haarcascades/haarcascade_frontalface_default.xml");

            if (is == null) {
                throw new Exception("Le fichier XML est introuvable dans src/main/resources/haarcascades/");
            }

            File tempFile = File.createTempFile("haarcascade_default", ".xml");
            tempFile.deleteOnExit();

            java.nio.file.Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            faceDetector = new CascadeClassifier(tempFile.getAbsolutePath());

            if (faceDetector.empty()) {
                System.err.println("❌ Erreur : Le classifieur est vide (problème interne OpenCV).");
            } else {
                System.out.println("✅ Détecteur de visage chargé avec succès !");
            }
        } catch (Exception e) {
            System.err.println("❌ ERREUR CRITIQUE CHARGEMENT XML : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initFaceRecognizer() {
        try {
            faceRecognizer = LBPHFaceRecognizer.create();

            // Configuration des paramètres LBPH
            faceRecognizer.setRadius(1);
            faceRecognizer.setNeighbors(8);
            faceRecognizer.setGridX(8);
            faceRecognizer.setGridY(8);
            faceRecognizer.setThreshold(100.0);

            System.out.println("✅ Reconnaisseur LBPH JavaCV initialisé");
        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation reconnaisseur: " + e.getMessage());
        }
    }

    private boolean loadExistingModel() {
        try {
            File modelFile = new File(MODEL_PATH);
            if (modelFile.exists()) {
                faceRecognizer.read(MODEL_PATH);
                isModelTrained = true;
                System.out.println("✅ Modèle LBPH chargé depuis: " + MODEL_PATH);
                loadUserNamesFromFiles();
                return true;
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement modèle: " + e.getMessage());
        }
        return false;
    }

    private void initialTrain() {
        try {
            File trainingDir = new File(TRAINING_DATA_PATH);
            File[] files = trainingDir.listFiles((dir, name) -> name.endsWith(".jpg"));

            if (files != null && files.length > 0) {
                System.out.println("📸 Entraînement initial avec " + files.length + " images...");
                retrainModel();
            } else {
                System.out.println("ℹ️ Aucune image trouvée pour l'entraînement initial. Veuillez d'abord enregistrer des visages.");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'entraînement initial: " + e.getMessage());
        }
    }

    private void loadUserNamesFromFiles() {
        try {
            File trainingDir = new File(TRAINING_DATA_PATH);
            File[] files = trainingDir.listFiles((dir, name) -> name.endsWith(".jpg"));

            if (files != null) {
                for (File file : files) {
                    String filename = file.getName();
                    try {
                        int userId = Integer.parseInt(filename.split("_")[0]);
                        if (!userIdToNameMap.containsKey(userId)) {
                            userIdToNameMap.put(userId, "User_" + userId);
                        }
                    } catch (NumberFormatException e) {
                        // Ignorer les fichiers avec nom invalide
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur chargement noms utilisateurs: " + e.getMessage());
        }
    }

    private void createTrainingDirectory() {
        File dir = new File(TRAINING_DATA_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("📁 Dossier créé: " + TRAINING_DATA_PATH);
        }
    }

    public Rect detectFace(Mat image) {
        if (faceDetector == null || image == null || image.empty()) return null;

        RectVector detectedFaces = new RectVector();
        faceDetector.detectMultiScale(image, detectedFaces);

        if (detectedFaces.size() > 0) {
            return detectedFaces.get(0);
        }
        return null;
    }

    public Mat preprocessFace(Mat image, Rect face) {
        if (image == null || face == null) return null;

        try {
            Mat faceROI = new Mat(image, face);

            Mat grayFace = new Mat();
            cvtColor(faceROI, grayFace, COLOR_BGR2GRAY);

            Mat resizedFace = new Mat();
            resize(grayFace, resizedFace, new Size(200, 200));

            Mat equalizedFace = new Mat();
            equalizeHist(resizedFace, equalizedFace);

            return equalizedFace;
        } catch (Exception e) {
            System.err.println("❌ Erreur preprocessing: " + e.getMessage());
            return null;
        }
    }

    public boolean enrollFace(int userId, String userName, Mat faceImage) {
        try {
            if (faceImage == null || faceImage.empty()) {
                System.err.println("❌ Image de visage invalide");
                return false;
            }

            // Vérifier que l'image est en niveaux de gris
            Mat finalImage = faceImage;
            if (faceImage.channels() > 1) {
                Mat grayFace = new Mat();
                cvtColor(faceImage, grayFace, COLOR_BGR2GRAY);
                finalImage = grayFace;
            }

            String filename = TRAINING_DATA_PATH + userId + "_" + System.currentTimeMillis() + ".jpg";

            boolean saved = imwrite(filename, finalImage);
            if (!saved) {
                System.err.println("❌ Échec sauvegarde image: " + filename);
                return false;
            }

            userIdToNameMap.put(userId, userName);
            System.out.println("✅ Visage enregistré pour: " + userName + " (" + filename + ")");

            // Réentraîner le modèle immédiatement
            boolean trained = retrainModel();
            if (trained) {
                System.out.println("✅ Modèle mis à jour avec le nouveau visage");
            } else {
                System.err.println("⚠️ Le visage a été sauvegardé mais le modèle n'a pas pu être entraîné");
            }

            return true;
        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde visage: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public int recognizeFace(Mat faceImage) {
        // Vérifier si le modèle est entraîné
        if (!isModelTrained) {
            System.err.println("❌ Le modèle n'est pas entraîné. Veuillez d'abord enregistrer des visages.");
            return -1;
        }

        if (faceRecognizer == null || faceImage == null || faceImage.empty()) {
            System.out.println("⚠️ Reconnaissance impossible: paramètres invalides");
            return -1;
        }

        Mat grayFace = faceImage;
        if (faceImage.channels() > 1) {
            grayFace = new Mat();
            cvtColor(faceImage, grayFace, COLOR_BGR2GRAY);
        }

        try {
            int[] label = new int[1];
            double[] confidence = new double[1];

            faceRecognizer.predict(grayFace, label, confidence);

            System.out.println("🔍 Résultat prédiction - ID: " + label[0] + " | Confiance: " + confidence[0]);

            // Seuil de confiance pour LBPH
            if (label[0] != -1 && confidence[0] < 100.0) {
                return label[0];
            } else if (label[0] != -1) {
                System.out.println("⚠️ Correspondance faible (confiance: " + confidence[0] + ")");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur prédiction: " + e.getMessage());
        }

        return -1;
    }

    public boolean retrainModel() {
        try {
            File trainingDir = new File(TRAINING_DATA_PATH);
            File[] files = trainingDir.listFiles((dir, name) -> name.endsWith(".jpg"));

            if (files == null || files.length == 0) {
                System.out.println("⚠️ Aucune image d'entraînement trouvée dans " + TRAINING_DATA_PATH);
                isModelTrained = false;
                return false;
            }

            System.out.println("📸 Début de l'entraînement avec " + files.length + " images...");

            // Utiliser MatVector pour les images
            MatVector images = new MatVector(files.length);
            Mat labels = new Mat(files.length, 1, CV_32SC1);
            IntBuffer labelsBuf = labels.createBuffer();

            int validImagesCount = 0;
            for (int i = 0; i < files.length; i++) {
                String filename = files[i].getName();
                try {
                    int userId = Integer.parseInt(filename.split("_")[0]);

                    Mat img = imread(files[i].getAbsolutePath(), IMREAD_GRAYSCALE);

                    if (!img.empty() && img.total() > 0) {
                        images.put(validImagesCount, img);
                        labelsBuf.put(validImagesCount, userId);
                        validImagesCount++;

                        if (!userIdToNameMap.containsKey(userId)) {
                            userIdToNameMap.put(userId, "User_" + userId);
                        }

                        System.out.println("  ✓ Image chargée: " + filename + " (User: " + userId + ")");
                    } else {
                        System.err.println("  ⚠️ Image vide ignorée: " + filename);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("  ⚠️ Format de nom invalide: " + filename);
                } catch (Exception e) {
                    System.err.println("  ⚠️ Erreur lecture: " + filename + " - " + e.getMessage());
                }
            }

            if (validImagesCount == 0) {
                System.out.println("⚠️ Aucune image valide pour l'entraînement");
                isModelTrained = false;
                return false;
            }

            // Créer un nouveau MatVector avec seulement les images valides
            MatVector validImages = new MatVector(validImagesCount);
            for (int i = 0; i < validImagesCount; i++) {
                validImages.put(i, images.get(i));
            }

            // Créer une sous-matrice avec seulement les labels valides
            Mat validLabels = labels.rowRange(0, validImagesCount);

            // Entraîner le modèle
            System.out.println("🔄 Entraînement du modèle avec " + validImagesCount + " images...");
            faceRecognizer.train(validImages, validLabels);

            // Sauvegarder le modèle
            faceRecognizer.save(MODEL_PATH);

            // Marquer le modèle comme entraîné
            isModelTrained = true;

            System.out.println("✅ Modèle entraîné et sauvegardé avec succès!");
            System.out.println("  - Images utilisées: " + validImagesCount + "/" + files.length);
            System.out.println("  - Utilisateurs: " + userIdToNameMap.size());

            return true;

        } catch (Exception e) {
            System.err.println("❌ Erreur entraînement: " + e.getMessage());
            e.printStackTrace();
            isModelTrained = false;
            return false;
        }
    }

    public String getUserNameFromId(int id) {
        return userIdToNameMap.getOrDefault(id, "Inconnu");
    }

    public Map<Integer, String> getUserMap() {
        return new HashMap<>(userIdToNameMap);
    }

    public int getTrainingCount() {
        File trainingDir = new File(TRAINING_DATA_PATH);
        File[] files = trainingDir.listFiles((dir, name) -> name.endsWith(".jpg"));
        return files != null ? files.length : 0;
    }

    public boolean isModelTrained() {
        return isModelTrained;
    }

    public void close() {
        try {
            if (faceDetector != null) {
                faceDetector.close();
            }
            if (faceRecognizer != null) {
                try {
                    if (isModelTrained) {
                        faceRecognizer.save(MODEL_PATH);
                        System.out.println("💾 Modèle sauvegardé avant fermeture");
                    }
                } catch (Exception e) {
                    System.err.println("⚠️ Erreur sauvegarde modèle: " + e.getMessage());
                }
                faceRecognizer.close();
            }
            System.out.println("👋 Ressources libérées");
        } catch (Exception e) {
            System.err.println("❌ Erreur fermeture ressources: " + e.getMessage());
        }
    }
}