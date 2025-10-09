package com.unip.service;

import com.unip.model.Role;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.FloatPointer;

import java.io.*;
import java.util.*;
import java.util.function.Consumer;

import static org.bytedeco.opencv.global.opencv_core.CV_32FC1;
import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class FaceService {
    private final FaceRecognizer faceRecognizer;
    private final Map<Integer, String> idToNameMap = new HashMap<>();
    private final Map<Integer, String> idToEmailMap = new HashMap<>();
    private final Map<Integer, Role> idToRoleMap = new HashMap<>();
    private int nextId = 0;

    private final String FACES_DIR = "faces";
    private final String LABELS_FILE = FACES_DIR + "/labels.txt";

    private static final double LIMIAR_DUPLICATA = 45.0;
    private static final double LIMIAR_RECONHECIMENTO = 55.0;
    private static final int MIN_IMAGES_PER_USER = 3;

    private Map<Integer, Integer> recognitionAttempts = new HashMap<>();
    private Map<Integer, Integer> successfulRecognitions = new HashMap<>();

    public FaceService() {
        faceRecognizer = LBPHFaceRecognizer.create();
        loadLabels();
        retrainModel();
    }

    private Mat preprocessFace(Mat face) {
        Mat processed = new Mat();
        Mat grayFace = new Mat();

        cvtColor(face, grayFace, COLOR_BGR2GRAY);
        equalizeHist(grayFace, processed);
        resize(processed, processed, new Size(200, 200));
        GaussianBlur(processed, processed, new Size(3, 3), 0);

        return processed;
    }

    private void generateAugmentedImages(Mat original, File outputDir) {
        try {
            // brilho aumentado
            Mat brighter = adjustBrightness(original, 1.3);
            opencv_imgcodecs.imwrite(
                new File(outputDir, "bright_" + System.currentTimeMillis() + ".png").getAbsolutePath(),
                brighter
            );

            // brilho reduzido
            Mat darker = adjustBrightness(original, 0.7);
            opencv_imgcodecs.imwrite(
                new File(outputDir, "dark_" + System.currentTimeMillis() + ".png").getAbsolutePath(),
                darker
            );

            // espelho
            Mat flipped = new Mat();
            org.bytedeco.opencv.global.opencv_core.flip(original, flipped, 1);
            opencv_imgcodecs.imwrite(
                new File(outputDir, "flipped_" + System.currentTimeMillis() + ".png").getAbsolutePath(),
                flipped
            );

            // rotação
            Mat rotated2 = rotateImageSimple(original, 2);
            if (!rotated2.empty()) {
                opencv_imgcodecs.imwrite(
                    new File(outputDir, "rotated2_" + System.currentTimeMillis() + ".png").getAbsolutePath(),
                    rotated2
                );
            }

            // rotação -2 graus
            Mat rotatedMinus2 = rotateImageSimple(original, -2);
            if (!rotatedMinus2.empty()) {
                opencv_imgcodecs.imwrite(
                    new File(outputDir, "rotatedMinus2_" + System.currentTimeMillis() + ".png").getAbsolutePath(),
                    rotatedMinus2
                );
            }

        } catch (Exception e) {
            System.out.println("Erro ao gerar imagens aumentadas: " + e.getMessage());
        }
    }

    private Mat adjustBrightness(Mat image, double factor) {
        Mat result = new Mat();
        image.convertTo(result, -1, factor, 0);
        return result;
    }

    private Mat rotateImageSimple(Mat src, double angle) {
        try {
            Mat dst = new Mat();

            if (angle == 0) {
                return src.clone();
            }

            double radians = Math.toRadians(angle);
            double shear = Math.tan(radians);

            Mat warpMat = new Mat(2, 3, CV_32FC1);

            float[] data = {
                1.0f, (float) shear, (float) (-shear * src.rows() / 2),
                (float) -shear, 1.0f, (float) (shear * src.cols() / 2)
            };

            warpMat.data().put(new FloatPointer(data));

            warpAffine(src, dst, warpMat, src.size());

            return dst;

        } catch (Exception e) {
            System.out.println("Erro na rotação simples: " + e.getMessage());
            return src.clone();
        }
    }

    private boolean isConfidenceReliable(double confidence, int label) {
        // Verificar se a confiança é consistentemente boa para este usuário
        File userDir = new File(FACES_DIR, String.valueOf(label));
        if (!userDir.exists()) return false;

        File[] imageFiles = userDir.listFiles();
        int imageCount = imageFiles != null ? imageFiles.length : 0;

        // Requer confiança melhor para usuários com poucas imagens
        if (imageCount < MIN_IMAGES_PER_USER) {
            return confidence < LIMIAR_RECONHECIMENTO * 0.7;
        }

        return true;
    }

    public void register(Mat face, String personName, String email, Role role, Consumer<String> callback) {
        Mat processedFace = preprocessFace(face);

        if (!idToNameMap.isEmpty()) {
            try {
                IntPointer label = new IntPointer(1);
                DoublePointer confidence = new DoublePointer(1);
                faceRecognizer.predict(processedFace, label, confidence);

                double conf = confidence.get(0);
                System.out.println("DEBUG REGISTER - Confiança: " + conf + ", Limiar: " + LIMIAR_DUPLICATA);

                if (conf < LIMIAR_DUPLICATA) {
                    String existingName = idToNameMap.get(label.get(0));
                    callback.accept(" Erro: Rosto já registrado como '" + existingName + "'");
                    return;
                }
            } catch (Exception e) {
                System.out.println("Modelo não treinado, primeiro registro...");
            }
        }

        if (idToEmailMap.containsValue(email)) {
            callback.accept("Erro: Email já registrado");
            return;
        }

        int personId = nextId++;
        idToNameMap.put(personId, personName);
        idToEmailMap.put(personId, email);
        idToRoleMap.put(personId, role);

        File personDir = new File(FACES_DIR, String.valueOf(personId));
        personDir.mkdirs();

        File[] existingImages = personDir.listFiles();
        int currentImageCount = existingImages != null ? existingImages.length : 0;

        String filename = new File(personDir, "original_" + System.currentTimeMillis() + ".png").getAbsolutePath();
        opencv_imgcodecs.imwrite(filename, processedFace);

        if (currentImageCount < 5) {
            generateAugmentedImages(processedFace, personDir);
        }

        saveLabels();
        retrainModel();

        recognitionAttempts.put(personId, 0);
        successfulRecognitions.put(personId, 0);

        callback.accept(" Sucesso: " + personName + " registrado com email " + email + ", nível " + role);
    }

    public void authenticate(Mat face, Consumer<String> callback) {
        if (idToNameMap.isEmpty()) {
            callback.accept(" Erro: Nenhum rosto registrado no sistema!");
            return;
        }

        Mat processedFace = preprocessFace(face);
        IntPointer label = new IntPointer(1);
        DoublePointer confidence = new DoublePointer(1);

        faceRecognizer.predict(processedFace, label, confidence);

        int predictedLabel = label.get(0);
        double conf = confidence.get(0);

        if (predictedLabel == -1 || conf > LIMIAR_RECONHECIMENTO || !isConfidenceReliable(conf, predictedLabel)) {
            callback.accept("Rosto não reconhecido (confiança: " + String.format("%.2f", conf) + ")");
        } else {
            recognitionAttempts.put(predictedLabel,
                recognitionAttempts.getOrDefault(predictedLabel, 0) + 1);
            successfulRecognitions.put(predictedLabel,
                successfulRecognitions.getOrDefault(predictedLabel, 0) + 1);

            String personName = idToNameMap.get(predictedLabel);
            String email = idToEmailMap.get(predictedLabel);
            Role role = idToRoleMap.get(predictedLabel);

            System.out.printf("RECONHECIMENTO - User: %s, Confiança: %.2f",personName, conf);

            callback.accept("Autenticado como: " + personName + " (" + email + ") - Nível: " + role + " - Confiança: " + String.format("%.2f", conf));
        }
    }

    private void retrainModel() {
        try {
            List<Mat> images = new ArrayList<>();
            List<Integer> labels = new ArrayList<>();

            for (Map.Entry<Integer, String> entry : idToNameMap.entrySet()) {
                int id = entry.getKey();
                File personDir = new File(FACES_DIR, String.valueOf(id));
                if (!personDir.exists()) continue;

                File[] imageFiles = personDir.listFiles();
                if (imageFiles == null || imageFiles.length < MIN_IMAGES_PER_USER) {
                    System.out.println("Usuário " + entry.getValue() + " ignorado no treinamento - poucas imagens: " +
                        (imageFiles != null ? imageFiles.length : 0));
                    continue;
                }

                for (File imgFile : imageFiles) {
                    if (imageFiles.length >= 8 &&
                        (imgFile.getName().contains("bright_") ||
                         imgFile.getName().contains("dark_") ||
                         imgFile.getName().contains("flipped_") ||
                         imgFile.getName().contains("rotated"))) {
                        continue;
                    }

                    Mat img = opencv_imgcodecs.imread(imgFile.getAbsolutePath(), opencv_imgcodecs.IMREAD_GRAYSCALE);
                    if (!img.empty()) {
                        images.add(img);
                        labels.add(id);
                    }
                }
            }

            if (images.size() >= MIN_IMAGES_PER_USER) {
                MatVector matImages = new MatVector(images.size());
                Mat labelsMat = new Mat(labels.size(), 1, CV_32SC1);

                for (int i = 0; i < images.size(); i++) {
                    matImages.put(i, images.get(i));
                    labelsMat.ptr(i).putInt(labels.get(i));
                }

                faceRecognizer.train(matImages, labelsMat);
                System.out.println("Modelo treinado com " + images.size() + " imagens de " +
                    new HashSet<>(labels).size() + " usuários");
            } else {
                System.out.println("Dados insuficientes para treinamento: " + images.size() + " imagens");
            }
        } catch (Exception e) {
            System.err.println(" Erro no treinamento: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadLabels() {
        File file = new File(LABELS_FILE);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 3) {
                    int id = Integer.parseInt(parts[0]);
                    String name = parts[1];
                    String email = parts[2];
                    idToNameMap.put(id, name);
                    idToEmailMap.put(id, email);

                    if (id >= nextId) nextId = id + 1;

                    recognitionAttempts.put(id, 0);
                    successfulRecognitions.put(id, 0);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveLabels() {
        try {
            new File(FACES_DIR).mkdirs(); // Garantir que o diretório existe
            try (PrintWriter pw = new PrintWriter(new FileWriter(LABELS_FILE))) {
                for (Map.Entry<Integer, String> entry : idToNameMap.entrySet()) {
                    int id = entry.getKey();
                    String name = entry.getValue();
                    String email = idToEmailMap.get(id);
                    Role role = idToRoleMap.get(id);
                    pw.println(id + ";" + name + ";" + email + ";" + role);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}