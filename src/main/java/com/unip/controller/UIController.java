package com.unip.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.ImageView;
import java.io.IOException;
import java.util.Optional;

import org.bytedeco.opencv.opencv_core.Mat;
import com.unip.service.CameraService;
import com.unip.service.FaceService;

public class UIController {

    @FXML private RadioButton cameraToggle;
    @FXML private RadioButton markFacesToggle;
    @FXML private Button registerFaceButton;
    @FXML private Button authFaceButton;
    @FXML private ImageView cameraView;

    private volatile boolean markFaces = false;

    private final CameraService cameraService = new CameraService();
    private final FaceService faceService = new FaceService();

    private static final String CAMERA_ON_TEXT = "Turn On Camera";
    private static final String CAMERA_OFF_TEXT = "Turn Off Camera";

    @FXML
    private void initialize() {
        cameraToggle.setText(CAMERA_ON_TEXT);
        cameraToggle.setOnAction(e -> toggleCamera());
        markFacesToggle.setOnAction(e -> markFaces = !markFaces);

        registerFaceButton.setOnAction(e -> {
            try {
                Mat frame = cameraService.captureFrame();
                if (frame != null) {
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setHeaderText("Digite o nome da pessoa:");
                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(name -> faceService.register(frame, name, this::showMessage));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        authFaceButton.setOnAction(e -> {
            try {
                Mat frame = cameraService.captureFrame();
                if (frame != null) {
                    faceService.authenticate(frame, this::showMessage);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private void toggleCamera() {
        if (cameraService.isCameraActive()) {
            cameraService.stopCamera();
            cameraToggle.setText(CAMERA_ON_TEXT);
        } else {
            cameraService.startCamera(frame -> {
                // if (markFaces) {
                //     faceService.detectFaces(frame, true);
                // }
                Platform.runLater(() -> {
                    cameraView.setImage(matToImage(frame));
                });
            });
            cameraToggle.setText(CAMERA_OFF_TEXT);
        }
        markFacesToggle.setVisible(cameraService.isCameraActive());
    }

    private javafx.scene.image.Image matToImage(Mat frame) {
        int width = frame.cols();
        int height = frame.rows();
        int channels = frame.channels();
        byte[] buffer = new byte[width * height * channels];
        frame.data().get(buffer);

        javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(width, height);
        javafx.scene.image.PixelWriter pw = image.getPixelWriter();
        pw.setPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getByteRgbInstance(), buffer, 0, width * channels);
        return image;
    }

    private void showMessage(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Face Recognition");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

    public void shutdown() {
        cameraService.stopCamera();
    }
}