package com.unip.service;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelFormat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class FaceService {

    private static final String TREINO_DIR = "./treino";
    private FaceRecognizer faceRecognizer;
    private final RecognitionService recognitionService = new RecognitionService();

    public FaceService() {
        faceRecognizer = LBPHFaceRecognizer.create();
    }

    public void detectFaces(Mat frame, boolean drawRects) {
        try {
            CascadeClassifier classifier = loadCascade("/haarcascade_frontalface_default.xml");
            Mat gray = new Mat();
            opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);

            RectVector faces = new RectVector();
            classifier.detectMultiScale(gray, faces);

            if (drawRects) {
                for (int i = 0; i < faces.size(); i++) {
                    Rect r = faces.get(i);
                    opencv_imgproc.rectangle(frame, r, new org.bytedeco.opencv.opencv_core.Scalar(0,255,0,0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void register(Mat frame, Consumer<String> messageCallback) throws IOException, Exception {
        createTrainDir();
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);

        int userId = getNextUserId(gray, messageCallback);
        int nextPhoto = countUserPhotos(userId) + 1;
        String filename = TREINO_DIR + File.separator + userId + "_" + nextPhoto + ".png";
        opencv_imgcodecs.imwrite(filename, gray);
        messageCallback.accept("Rosto salvo: " + filename);

        Path path = Path.of(filename);
        byte[] imageBytes = Files.readAllBytes(path);
        recognitionService.registerFace(imageBytes, "user_" + userId);
    }

    private CascadeClassifier loadCascade(String resourcePath) throws Exception {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) throw new RuntimeException("Cascade não encontrado: " + resourcePath);

        File tempFile = File.createTempFile("haarcascade", ".xml");
        tempFile.deleteOnExit();
        Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        CascadeClassifier classifier = new CascadeClassifier(tempFile.getAbsolutePath());
        if (classifier.empty()) throw new RuntimeException("Falha ao carregar CascadeClassifier");
        return classifier;
    }

    private void createTrainDir() {
        File dir = new File(TREINO_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    private int countUserPhotos(int userId) {
        File dir = new File(TREINO_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith(userId + "_"));
        return files != null ? files.length : 0;
    }

    private int getNextUserId(Mat face, Consumer<String> messageCallback) {
        File dir = new File(TREINO_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
        if (files == null || files.length == 0) return 1;

        List<Integer> labels = new ArrayList<>();
        for (File f : files) {
            String name = f.getName().split("_")[0].replaceAll("[^0-9]", "");
            labels.add(Integer.parseInt(name));
        }
        int maxId = labels.stream().mapToInt(i -> i).max().orElse(0);
        messageCallback.accept("Próximo usuário: " + (maxId + 1));
        return maxId + 1;
    }

    public Image matToImage(Mat frame) {
        int width = frame.cols();
        int height = frame.rows();
        int channels = frame.channels();
        WritableImage img = new WritableImage(width, height);
        byte[] pixels = new byte[width * height * channels];
        frame.data().get(pixels);
        PixelWriter pw = img.getPixelWriter();
        pw.setPixels(0, 0, width, height, PixelFormat.getByteRgbInstance(), pixels, 0, width * channels);
        return img;
    }

    public void closeRecognizer() {
        if (faceRecognizer != null) faceRecognizer.close();
    }
}