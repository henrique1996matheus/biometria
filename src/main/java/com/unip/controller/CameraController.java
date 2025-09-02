package com.unip.controller;

import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_GRAYSCALE;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

// Importações do OpenCV via Bytedeco
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_videoio;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_face.FaceRecognizer;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;

public class CameraController {

    @FXML
    private RadioButton radioButtonCamera;

    @FXML
    private RadioButton radioButtonMarcarRostos;

    @FXML
    private Button buttonReconhecerRosto;

    @FXML
    private ImageView imageViewCamera;

    private volatile boolean cameraAtiva = false;
    private volatile boolean marcarRostos = false;
    private Thread threadCamera;
    private VideoCapture videoCapture;

    private static final String TEXTO_LIGAR_CAMERA = "Ligar Câmera";
    private static final String TEXTO_DESLIGAR_CAMERA = "Desligar Câmera";

    // Objeto do reconhecedor de rosto LBPH
    private FaceRecognizer faceRecognizer;

    public void finalizarTudo() {
        cameraAtiva = false;

        if (threadCamera != null) {
            threadCamera.interrupt();
        }

        if (videoCapture != null && videoCapture.isOpened()) {
            videoCapture.release();
        }

        if (faceRecognizer != null) {
            faceRecognizer.close();
        }
    }

    @FXML
    private void initialize() {
        // Inicializa o reconhecedor de rosto LBPH
        faceRecognizer = LBPHFaceRecognizer.create();

        radioButtonCamera.setSelected(cameraAtiva);
        radioButtonCamera.setText(TEXTO_LIGAR_CAMERA);

        radioButtonCamera.setOnAction(event -> {
            if (cameraAtiva) {
                desligarCamera();
            } else {
                ligarCamera();
            }
        });

        radioButtonMarcarRostos.setVisible(cameraAtiva);

        radioButtonMarcarRostos.setOnAction(event -> {
            marcarRostos = !marcarRostos;
        });

        buttonReconhecerRosto.setOnAction(event -> {
            reconhecerRosto();
        });
    }

    private void desligarCamera() {
        radioButtonCamera.setText(TEXTO_LIGAR_CAMERA);

        cameraAtiva = false;
        radioButtonMarcarRostos.setVisible(cameraAtiva);

        tentaDesligarCamera();
    }

    private void tentaDesligarCamera() {
        if (threadCamera != null) {
            threadCamera.interrupt();
        }
    }

    private void ligarCamera() {
        radioButtonCamera.setText(TEXTO_DESLIGAR_CAMERA);

        cameraAtiva = true;
        radioButtonMarcarRostos.setVisible(cameraAtiva);

        tentaDesligarCamera();
        threadCamera = new Thread(() -> {
            videoCapture = new VideoCapture(0, opencv_videoio.CAP_DSHOW);
            if (!videoCapture.isOpened()) {
                System.err.println("Não foi possível abrir a câmera!");
                return;
            }

            try {
                Mat frame = new Mat();

                while (cameraAtiva && videoCapture.read(frame)) {
                    detectarRostos(frame);

                    opencv_imgproc.cvtColor(frame, frame, opencv_imgproc.COLOR_BGR2RGB);
                    Image image = matToWritableImage(frame);
                    Platform.runLater(() -> imageViewCamera.setImage(image));

                    try {
                        Thread.sleep(33);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            } finally {
                if (videoCapture != null && videoCapture.isOpened()) {
                    videoCapture.release();
                }
            }
        });

        threadCamera.setDaemon(true);
        threadCamera.start();
    }

    private void detectarRostos(Mat frame) {
        // Carrega o classificador Haar Cascade para detecção de rostos
        CascadeClassifier faceDetector;
        try {
            faceDetector = loadCascade("/haarcascade_frontalface_default.xml");
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Mat frameCinza = new Mat();
        opencv_imgproc.cvtColor(frame, frameCinza, opencv_imgproc.COLOR_BGR2GRAY);

        RectVector rectVectorFaces = new RectVector();
        faceDetector.detectMultiScale(frameCinza, rectVectorFaces);

        // Desenha retângulo verde em volta do rosto detectado
        if (marcarRostos) {
            for (int i = 0; i < rectVectorFaces.size(); i++) {
                Rect face = rectVectorFaces.get(i);

                opencv_imgproc.rectangle(frame, face,
                        new org.bytedeco.opencv.opencv_core.Scalar(0, 255, 0, 0));
            }
        }
    }

    private CascadeClassifier loadCascade(String resourcePath) throws Exception {
        InputStream is = getClass().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new RuntimeException("Cascade não encontrado: " + resourcePath);
        }

        // Copia recurso para arquivo temporário para o OpenCV poder ler
        File tempFile = File.createTempFile("haarcascade", ".xml");
        tempFile.deleteOnExit();
        Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        CascadeClassifier classifier = new CascadeClassifier(tempFile.getAbsolutePath());
        if (classifier.empty()) {
            throw new RuntimeException("Falha ao carregar CascadeClassifier");
        }

        return classifier;
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

    private static final String TREINO_DIR = "./treino";

    private void criarDiretorioTreino() {
        File dir = new File(TREINO_DIR);
        if (!dir.exists())
            dir.mkdirs();
    }

    @FXML
    private void reconhecerRosto() {
        if (!cameraAtiva) {
            return;
        }

        criarDiretorioTreino();

        // Captura o frame atual
        Mat frame = new Mat();
        if (videoCapture != null && videoCapture.isOpened() && videoCapture.read(frame)) {
            Mat frameCinza = new Mat();
            opencv_imgproc.cvtColor(frame, frameCinza, opencv_imgproc.COLOR_BGR2GRAY);

            RectVector faces = new RectVector();
            try {
                CascadeClassifier faceDetector = loadCascade("/haarcascade_frontalface_default.xml");
                faceDetector.detectMultiScale(frameCinza, faces);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            for (int i = 0; i < faces.size(); i++) {
                Rect faceRect = faces.get(i);
                Mat faceMat = new Mat(frameCinza, faceRect);

                int userId = obterIdUsuario(faceMat);
                int nextPhoto = contarFotosUsuario(userId) + 1;

                String filename = TREINO_DIR + File.separator + userId + "_" + nextPhoto + ".png";
                org.bytedeco.opencv.global.opencv_imgcodecs.imwrite(filename, faceMat);

                System.out.println("Rosto salvo: " + filename);
            }
        }
    }

    // Retorna próximo ID de usuário. Se já existir, retorna mesmo ID.
    private int obterIdUsuario(Mat face) {
        File dir = new File(TREINO_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".png") || name.endsWith(".jpg"));
        if (files == null || files.length == 0)
            return 1;

        // Aqui você poderia adicionar reconhecimento real para comparar face
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

        if (images.isEmpty()) {
            return 1;
        }

        MatVector matVector = new MatVector(images.size());
        Mat labelsMat = new Mat(labels.size(), 1, org.bytedeco.opencv.global.opencv_core.CV_32SC1);

        for (int i = 0; i < images.size(); i++) {
            matVector.put(i, images.get(i));
            labelsMat.ptr(i).putInt(labels.get(i));
        }

        faceRecognizer.train(matVector, labelsMat); // treina recognizer

        int[] label = new int[1];
        double[] confidence = new double[1];

        faceRecognizer.predict(face, label, confidence);
        int maxId = 0;

        if (confidence[0] < 50) {
            exibirMensagem("Reconhecido usuário: " + label[0]);
            return label[0];
        } else {
            if (files != null) {
                for (File f : files) {
                    String name = f.getName().split("_")[0];
                    maxId = Math.max(maxId, Integer.parseInt(name));
                }
            }

            exibirMensagem("Não reconhecido novo usuário: " + (maxId + 1));
            return maxId + 1;
        }
    }

    private void exibirMensagem(String mensagem) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Reconhecimento do Rosto");
            alert.setHeaderText(null);
            alert.setContentText(mensagem);
            alert.show();
        });
    }

    // Conta quantas fotos já existem de um usuário
    private int contarFotosUsuario(int userId) {
        File dir = new File(TREINO_DIR);
        File[] files = dir.listFiles((d, name) -> name.startsWith(userId + "_"));
        return files != null ? files.length : 0;
    }

}
