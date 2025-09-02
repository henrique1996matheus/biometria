package com.unip.demo;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;

import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_videoio;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class CameraApp1 extends Application {

    private VideoCapture camera;
    private boolean stopCamera = false;

    @Override
    public void start(Stage primaryStage) {
        ImageView imageView = new ImageView();
        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root, 640, 480);

        primaryStage.setTitle("Camera OpenCV - Detecção de Rosto");
        primaryStage.setScene(scene);
        primaryStage.show();

        startCamera(imageView);

        primaryStage.setOnCloseRequest(event -> {
            stopCamera = true;
            if (camera != null && camera.isOpened()) {
                camera.release();
            }
        });
    }

    public CascadeClassifier loadClassifier(String resourcePath) throws Exception {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new RuntimeException("Arquivo de cascade não encontrado: " + resourcePath);
        }

        // Cria arquivo temporário
        File tempFile = File.createTempFile("haarcascade", ".xml");
        tempFile.deleteOnExit();
        Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Carrega no OpenCV
        CascadeClassifier classifier = new CascadeClassifier(tempFile.getAbsolutePath());
        if (classifier.empty()) {
            throw new RuntimeException("Falha ao carregar CascadeClassifier");
        }

        return classifier;
    }

    private void startCamera(ImageView imageView) {
        new Thread(() -> {
            camera = new VideoCapture(0, opencv_videoio.CAP_DSHOW);
            if (!camera.isOpened()) {
                System.err.println("Não foi possível abrir a câmera!");
                return;
            }

            // Carregar classificador Haar Cascade do resources
            CascadeClassifier faceDetector;
            try {
                faceDetector = loadClassifier("/haarcascade_frontalface_default.xml");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            Mat frame = new Mat();
            Mat gray = new Mat();

            while (!stopCamera) {
                if (camera.read(frame)) {
                    // Converte para cinza
                    opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);
                    RectVector faces = new RectVector();
                    faceDetector.detectMultiScale(gray, faces);

                    // Desenha retângulos ao redor dos rostos
                    for (int i = 0; i < faces.size(); i++) {
                        var face = faces.get(i);
                        opencv_imgproc.rectangle(frame, face, new org.bytedeco.opencv.opencv_core.Scalar(0, 255, 0, 0));
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
        ByteBuffer buffer = frame.createBuffer();
        byte[] pixels = new byte[width * height * channels];
        buffer.get(pixels);

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
