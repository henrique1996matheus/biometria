package com.unip.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.image.ImageView;

import java.io.IOException;

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
                registerFace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (Exception e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        });
    }

    private void toggleCamera() {
        if (cameraService.isCameraActive()) {
            cameraService.stopCamera();
            cameraToggle.setText(CAMERA_ON_TEXT);
        } else {
            cameraService.startCamera(frame -> {
                if (markFaces) {
                    faceService.detectFaces(frame, true);
                }
                Platform.runLater(() -> cameraView.setImage(faceService.matToImage(frame)));
            });
            cameraToggle.setText(CAMERA_OFF_TEXT);
        }
        markFacesToggle.setVisible(cameraService.isCameraActive());
    }

    private void registerFace() throws IOException, Exception {
        if (!cameraService.isCameraActive()) return;

        Mat frame = cameraService.captureFrame();
        if (frame != null) {
            faceService.register(frame, this::showMessage);
        }
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
        faceService.closeRecognizer();
    }
}
