package com.unip.service;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.DoublePointer;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;

import static org.bytedeco.opencv.global.opencv_core.CV_32SC1;
import static org.bytedeco.opencv.global.opencv_imgproc.cvtColor;
import static org.bytedeco.opencv.global.opencv_imgproc.COLOR_BGR2GRAY;


public class FaceService {
    private final FaceRecognizer faceRecognizer;
    private final Map<Integer, String> idToNameMap = new HashMap<>();
    private int nextId = 0;

    private final String FACES_DIR = "faces";
    private final String LABELS_FILE = FACES_DIR + "/labels.txt";

    public FaceService() {
        faceRecognizer = LBPHFaceRecognizer.create();
        loadLabels();
        retrainModel();
    }

    public void register(Mat face, String personName, Consumer<String> callback) {
        Mat grayFace = new Mat();
        cvtColor(face, grayFace, COLOR_BGR2GRAY);

        if (!idToNameMap.isEmpty()) {
            IntPointer label = new IntPointer(1);
            DoublePointer confidence = new DoublePointer(1);
            faceRecognizer.predict(grayFace, label, confidence);

            // Se confidence menor que LIMIAR_DUPLICATA, considera já registrado
            double LIMIAR_DUPLICATA = 50.0;
            if (confidence.get(0) < LIMIAR_DUPLICATA) {
                String existingName = idToNameMap.get(label.get(0));
                callback.accept("Erro: rosto já registrado como '" + existingName + "'!");
                return;
            }
        }
        if (idToNameMap.containsValue(personName)) {
            callback.accept("Erro: usuário '" + personName + "' já registrado!");
            return;
        }

        int personId = nextId++;
        idToNameMap.put(personId, personName);

        File personDir = new File(FACES_DIR, String.valueOf(personId));
        personDir.mkdirs();
        String filename = new File(personDir, System.currentTimeMillis() + ".png").getAbsolutePath();
        opencv_imgcodecs.imwrite(filename, grayFace);

        saveLabels();
        retrainModel();

        callback.accept("Sucesso: rosto registrado para '" + personName + "'");
    }

    public void authenticate(Mat face, Consumer<String> callback) {
        if (idToNameMap.isEmpty()) {
            callback.accept("Erro: nenhum rosto registrado!");
            return;
        }

        IntPointer label = new IntPointer(1);
        DoublePointer confidence = new DoublePointer(1);

        Mat grayFace = new Mat();
        cvtColor(face, grayFace, COLOR_BGR2GRAY);
        faceRecognizer.predict(grayFace, label, confidence);

        int predictedLabel = label.get(0);
        double conf = confidence.get(0);

        // quanto menor a confiança, melhor (0 = match perfeito)
        if (predictedLabel == -1 || conf > 10) {
            callback.accept("Erro: rosto não reconhecido (conf=" + conf + ")");
        } else {
            String personName = idToNameMap.get(predictedLabel);
            callback.accept("Sucesso: autenticado como " + personName + " (conf=" + conf + ")");
        }
    }

    private void retrainModel() {
        List<Mat> images = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();

        for (Map.Entry<Integer, String> entry : idToNameMap.entrySet()) {
            int id = entry.getKey();
            File personDir = new File(FACES_DIR, String.valueOf(id));
            if (!personDir.exists()) continue;

            for (File imgFile : Objects.requireNonNull(personDir.listFiles())) {
                Mat img = opencv_imgcodecs.imread(imgFile.getAbsolutePath(), opencv_imgcodecs.IMREAD_GRAYSCALE);
                if (!img.empty()) {
                    images.add(img);
                    labels.add(id);
                }
            }
        }

        if (!images.isEmpty()) {
            MatVector matImages = new MatVector(images.size());
            Mat labelsMat = new Mat(labels.size(), 1, CV_32SC1);

            for (int i = 0; i < images.size(); i++) {
                matImages.put(i, images.get(i));
                labelsMat.ptr(i).putInt(labels.get(i));
            }

            faceRecognizer.train(matImages, labelsMat);
        }
    }

    private void loadLabels() {
        File file = new File(LABELS_FILE);
        if (!file.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length == 2) {
                    int id = Integer.parseInt(parts[0]);
                    String name = parts[1];
                    idToNameMap.put(id, name);
                    if (id >= nextId) nextId = id + 1;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveLabels() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LABELS_FILE))) {
            for (Map.Entry<Integer, String> entry : idToNameMap.entrySet()) {
                pw.println(entry.getKey() + ";" + entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
