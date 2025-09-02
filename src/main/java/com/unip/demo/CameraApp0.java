package com.unip.demo;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// Importações do OpenCV via Bytedeco
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

// Importações do JavaFX
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;

public class CameraApp0 extends Application {

    // Limite de fotos por pessoa para não salvar infinitamente
    private static final int MAX_FOTOS = 5; 

    // Objeto do reconhecedor de rosto LBPH
    private FaceRecognizer faceRecognizer;
    private final String TREINO_DIR = "./treino"; // pasta onde serão salvas as imagens
    private AtomicInteger saveCounter = new AtomicInteger(1); // contador de imagens salvas
    private volatile boolean stopCamera = false; // flag para parar a câmera
    private volatile boolean alreadyShown = false; // evita abrir múltiplos popups

    public static void main(String[] args) {
        launch(args); // inicia a aplicação JavaFX
    }

    @Override
    public void start(Stage primaryStage) {
        // Cria a pasta de treino se não existir
        File folder = new File(TREINO_DIR);
        if (!folder.exists())
            folder.mkdirs();

        // Inicializa o reconhecedor de rosto LBPH
        faceRecognizer = LBPHFaceRecognizer.create();

        // Criação dos botões e visualização da câmera
        Button saveFaceBtn = new Button("Salvar rosto");
        Button recognizeFaceBtn = new Button("Reconhecer rosto");
        ImageView cameraView = new ImageView(); // onde a câmera será exibida

        // Ação ao clicar em "Salvar rosto"
        saveFaceBtn.setOnAction(e -> startCamera(cameraView, true));

        // Ação ao clicar em "Reconhecer rosto"
        recognizeFaceBtn.setOnAction(e -> {
            try {
                trainRecognizer(); // treina o recognizer antes de reconhecer
            } catch (Exception ex) {
                System.err.println("Erro ao treinar recognizer: " + ex.getMessage());
            }
            startCamera(cameraView, false);
        });

        // Layout vertical com espaçamento entre componentes
        VBox root = new VBox(10, saveFaceBtn, recognizeFaceBtn, cameraView);
        root.setPadding(new Insets(20));
        Scene scene = new Scene(root, 700, 600);

        primaryStage.setTitle("Autenticação Facial");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Para a câmera quando a janela principal for fechada
        primaryStage.setOnCloseRequest(event -> stopCamera = true);
    }

    /**
     * Método que inicia a câmera e processa rostos
     * 
     * @param imageView - componente JavaFX para mostrar a câmera
     * @param saveMode  - true = salvar rostos, false = reconhecer rostos
     */
    private void startCamera(ImageView imageView, boolean saveMode) {
        stopCamera = false;
        alreadyShown = false; // reset para permitir popups no modo reconhecimento

        new Thread(() -> {
            // Abre a câmera
            VideoCapture camera = new VideoCapture(0, opencv_videoio.CAP_DSHOW);
            if (!camera.isOpened()) {
                System.err.println("Não foi possível abrir a câmera!");
                return;
            }

            // Carrega o classificador Haar Cascade para detecção de rostos
            CascadeClassifier faceDetector;
            try {
                faceDetector = loadCascade("/haarcascade_frontalface_default.xml");
            } catch (Exception e) {
                e.printStackTrace();
                camera.release();
                return;
            }

            Mat frame = new Mat(); // frame colorido da câmera
            Mat gray = new Mat();  // frame em escala de cinza

            while (!stopCamera && camera.read(frame)) { // loop principal da câmera
                // Converte para escala de cinza para detecção
                opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);

                // Detecta rostos
                RectVector faces = new RectVector();
                faceDetector.detectMultiScale(gray, faces);

                for (int i = 0; i < faces.size(); i++) {
                    var face = faces.get(i);

                    // Desenha retângulo verde em volta do rosto detectado
                    opencv_imgproc.rectangle(frame, face,
                            new org.bytedeco.opencv.opencv_core.Scalar(0, 255, 0, 0));

                    Mat faceMat = new Mat(gray, face); // recorta apenas o rosto

                    if (saveMode) { // modo salvar rostos
                        if (saveCounter.get() <= MAX_FOTOS) {
                            String filename = TREINO_DIR + File.separator + "1_" + saveCounter.getAndIncrement() + ".png";
                            opencv_imgcodecs.imwrite(filename, faceMat); // salva imagem

                            // Mostra popup discreto apenas uma vez
                            Platform.runLater(() -> {
                                if (!alreadyShown) {
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle("Rosto salvo");
                                    alert.setHeaderText(null);
                                    alert.setContentText("Rosto salvo com sucesso: " + filename);
                                    alert.show();
                                    alreadyShown = true;
                                }
                            });
                        } else {
                            stopCamera = true; // fecha câmera ao atingir limite
                            break;
                        }
                    } else { // modo reconhecer rostos
                        if (!faceRecognizer.empty() && !alreadyShown) {
                            int[] label = new int[1];
                            double[] confidence = new double[1];

                            faceRecognizer.predict(faceMat, label, confidence); // prediz ID e confiança

                            // Escreve texto no frame
                            String text = "ID: " + label[0] + " Conf: " + String.format("%.2f", confidence[0]);
                            opencv_imgproc.putText(frame, text,
                                    new org.bytedeco.opencv.opencv_core.Point(face.x(), face.y() - 10),
                                    opencv_imgproc.FONT_HERSHEY_PLAIN, 1.0,
                                    new org.bytedeco.opencv.opencv_core.Scalar(255, 0, 0, 0));

                            // Mostra alerta apenas uma vez
                            Platform.runLater(() -> {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Rosto Reconhecido");
                                alert.setHeaderText(null);
                                alert.setContentText("ID: " + label[0] + "\nConfiança: " + String.format("%.2f", confidence[0]));
                                alert.show();
                                alreadyShown = true;
                            });
                        }
                    }
                }

                // Converte frame para RGB para exibir na ImageView
                opencv_imgproc.cvtColor(frame, frame, opencv_imgproc.COLOR_BGR2RGB);
                WritableImage image = matToWritableImage(frame);
                Platform.runLater(() -> imageView.setImage(image));

                try { Thread.sleep(30); } catch (InterruptedException e) { break; } // taxa de atualização
            }
            camera.release(); // libera a câmera
        }).start();
    }

    /**
     * Carrega um arquivo Haar Cascade do recurso e cria um CascadeClassifier
     */
    private CascadeClassifier loadCascade(String resourcePath) throws Exception {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null)
            throw new RuntimeException("Cascade não encontrado: " + resourcePath);

        // Copia recurso para arquivo temporário para o OpenCV poder ler
        File tempFile = File.createTempFile("haarcascade", ".xml");
        tempFile.deleteOnExit();
        Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        CascadeClassifier classifier = new CascadeClassifier(tempFile.getAbsolutePath());
        if (classifier.empty())
            throw new RuntimeException("Falha ao carregar CascadeClassifier");
        return classifier;
    }

    /**
     * Treina o reconhecedor com as imagens salvas na pasta de treino
     */
    private void trainRecognizer() throws Exception {
        File treinoDir = new File(TREINO_DIR);
        File[] files = treinoDir.listFiles((dir, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
        if (files == null || files.length == 0)
            return;

        List<Mat> images = new ArrayList<>();
        List<Integer> labels = new ArrayList<>();

        for (File f : files) {
            Mat img = opencv_imgcodecs.imread(f.getAbsolutePath(), IMREAD_GRAYSCALE);
            if (img.empty())
                continue;
            images.add(img);

            // Pega ID da imagem (antes do "_") e remove qualquer caractere não numérico
            String name = f.getName().split("_")[0].replaceAll("[^0-9]", "");
            labels.add(Integer.parseInt(name));
        }

        if (images.isEmpty())
            return;

        MatVector matVector = new MatVector(images.size());
        Mat labelsMat = new Mat(labels.size(), 1, org.bytedeco.opencv.global.opencv_core.CV_32SC1);

        for (int i = 0; i < images.size(); i++) {
            matVector.put(i, images.get(i));
            labelsMat.ptr(i).putInt(labels.get(i));
        }

        faceRecognizer.train(matVector, labelsMat); // treina recognizer
        System.out.println("Recognizer treinado com sucesso!");
    }

    /**
     * Converte um Mat do OpenCV para WritableImage do JavaFX
     */
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
}
