package com.unip.demo;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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

public class CameraAppAutenticando extends Application {

    private VideoCapture camera;
    private boolean stopCamera = false;
    private FaceRecognizer faceRecognizer;
    private AtomicInteger saveCounter = new AtomicInteger(1);

    private final String TREINO_DIR = "resources/treino"; // pasta de treino

    @Override
    public void start(Stage primaryStage) {
        ImageView imageView = new ImageView();
        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root, 640, 480);

        primaryStage.setTitle("Autenticação Facial com Salvar Rostos");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Cria pasta de treino caso não exista
        File treinoFolder = new File(TREINO_DIR);
        if (!treinoFolder.exists()) treinoFolder.mkdirs();

        // Inicializa recognizer
        faceRecognizer = LBPHFaceRecognizer.create();

        // Treina recognizer se houver imagens
        try {
            trainRecognizer();
        } catch (Exception e) {
            System.err.println("Nenhuma imagem de treino encontrada. O recognizer será treinado depois.");
        }

        startCamera(imageView);

        primaryStage.setOnCloseRequest(event -> {
            stopCamera = true;
            if (camera != null && camera.isOpened()) camera.release();
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
        File treinoDir = new File(TREINO_DIR);
        File[] files = treinoDir.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
        if (files == null || files.length == 0) return;

        List<Mat> images = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();

        for (File f : files) {
            Mat img = opencv_imgcodecs.imread(f.getAbsolutePath(), IMREAD_GRAYSCALE);
            if (img.empty()) continue;
            images.add(img);

            String name = f.getName().split("_")[0].replaceAll("[^0-9]", "");
            labels.add(Integer.parseInt(name));
        }

        if (images.isEmpty()) return;

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

                        // Salva o rosto detectado na pasta de treino
                        Mat faceMat = new Mat(gray, face);
                        String filename = TREINO_DIR + File.separator + "1_" + saveCounter.getAndIncrement() + ".png";
                        opencv_imgcodecs.imwrite(filename, faceMat);

                        // Treina recognizer novamente se houver novas imagens
                        if (saveCounter.get() % 3 == 0) { // treina a cada 3 rostos salvos
                            try { trainRecognizer(); } catch (Exception ignored) {}
                        }

                        // Só prediz se o recognizer foi treinado
                        if (faceRecognizer != null && !faceRecognizer.empty()) {
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

                try { Thread.sleep(30); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
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
