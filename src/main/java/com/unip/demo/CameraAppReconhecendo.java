package com.unip.demo;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_videoio;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;

public class CameraAppReconhecendo extends Application {

    private VideoCapture camera;
    private boolean stopCamera = false;
    private FaceRecognizer faceRecognizer;

    @Override
    public void start(Stage primaryStage) {
        ImageView imageView = new ImageView();
        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root, 640, 480);

        primaryStage.setTitle("Autenticação Facial");
        primaryStage.setScene(scene);
        primaryStage.show();

        try {
            trainRecognizer();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erro ao treinar recognizer. Verifique as imagens de treino.");
            return;
        }

        startCamera(imageView);

        primaryStage.setOnCloseRequest(event -> {
            stopCamera = true;
            if (camera != null && camera.isOpened()) {
                camera.release();
            }
        });
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

    private void trainRecognizer() throws Exception {
        faceRecognizer = LBPHFaceRecognizer.create();

        File treinoDir = new File(getClass().getResource("/treino").toURI());
        File[] files = treinoDir.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".jpg"));

        if (files == null || files.length == 0)
            throw new RuntimeException("Nenhuma imagem de treino encontrada em resources/treino");

        List<Mat> images = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();

        for (File f : files) {
            Mat img = opencv_imgcodecs.imread(f.getAbsolutePath(), IMREAD_GRAYSCALE); // ✅ Corrigido para grayscale
            if (img.empty()) continue;
            images.add(img);

            String name = f.getName().split("_")[0].replaceAll("[^0-9]", "");
            labels.add(Integer.parseInt(name));
        }

        if (images.isEmpty()) throw new RuntimeException("Nenhuma imagem válida para treino.");

        MatVector matVector = new MatVector(images.size());
        Mat labelsMat = new Mat(labels.size(), 1, org.bytedeco.opencv.global.opencv_core.CV_32SC1);
        for (int i = 0; i < images.size(); i++) {
            matVector.put(i, images.get(i));
            labelsMat.ptr(i).putInt(labels.get(i));
        }

        faceRecognizer.train(matVector, labelsMat);
        System.out.println("Recognizer treinado com sucesso!");
    }

    private void startCamera(ImageView imageView) {
        new Thread(() -> {
            camera = new VideoCapture(0, opencv_videoio.CAP_DSHOW);
            if (!camera.isOpened()) {
                System.err.println("Não foi possível abrir a câmera!");
                return;
            }

            CascadeClassifier faceDetector;
            try {
                faceDetector = loadCascade("/haarcascade_frontalface_default.xml");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            Mat frame = new Mat();
            Mat gray = new Mat();

            while (!stopCamera) {
                if (camera.read(frame)) {
                    opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);

                    RectVector faces = new RectVector();
                    faceDetector.detectMultiScale(gray, faces);

                    for (int i = 0; i < faces.size(); i++) {
                        var face = faces.get(i);
                        opencv_imgproc.rectangle(frame, face,
                                new org.bytedeco.opencv.opencv_core.Scalar(0, 255, 0, 0));

                        if (faceRecognizer != null) {
                            Mat faceMat = new Mat(gray, face);
                            int[] label = new int[1];
                            double[] confidence = new double[1];
                            faceRecognizer.predict(faceMat, label, confidence);

                            String text = "ID: " + label[0] + " Conf: " + String.format("%.2f", confidence[0]);
                            opencv_imgproc.putText(frame, text,
                                    new org.bytedeco.opencv.opencv_core.Point(face.x(), face.y() - 10),
                                    opencv_imgproc.FONT_HERSHEY_PLAIN, 1.0,
                                    new org.bytedeco.opencv.opencv_core.Scalar(255, 0, 0, 0));
                        }
                    }

                    opencv_imgproc.cvtColor(frame, frame, opencv_imgproc.COLOR_BGR2RGB);
                    WritableImage image = matToWritableImage(frame);
                    Platform.runLater(() -> imageView.setImage(image));
                }

                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();
    }

    private WritableImage matToWritableImage(Mat frame) {
        int width = frame.cols();
        int height = frame.rows();
        int channels = frame.channels();

        WritableImage writableImage = new WritableImage(width, height);
        byte[] pixels = new byte[width * height * channels];
        frame.data().get(pixels);

        javafx.scene.image.PixelWriter pw = writableImage.getPixelWriter();
        pw.setPixels(0, 0, width, height,
                javafx.scene.image.PixelFormat.getByteRgbInstance(),
                pixels, 0, width * channels);

        return writableImage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
