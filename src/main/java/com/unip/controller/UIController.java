package com.unip.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;
import javafx.scene.image.ImageView;

import org.bytedeco.opencv.opencv_core.Mat;
import com.unip.service.CameraService;
import com.unip.service.FaceService;

import java.util.Optional;

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

    // Classe interna para armazenar nome e email
    private static class UserData {
        private final String name;
        private final String email;

        public UserData(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() { return name; }
        public String getEmail() { return email; }
    }

    @FXML
    private void initialize() {
        cameraToggle.setText(CAMERA_ON_TEXT);
        cameraToggle.setOnAction(e -> toggleCamera());
        markFacesToggle.setOnAction(e -> markFaces = !markFaces);

        registerFaceButton.setOnAction(e -> {
            try {
                Mat frame = cameraService.captureFrame();
                if (frame != null) {
                    showRegistrationDialog(frame);
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

    private void showRegistrationDialog(Mat frame) {
        Dialog<UserData> dialog = new Dialog<>();
        dialog.setTitle("Registrar Novo Usuário");
        dialog.setHeaderText("Digite os dados do usuário:");

        ButtonType registerButtonType = new ButtonType("Registrar");
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField nameField = new TextField();
        nameField.setPromptText("Nome completo");
        TextField emailField = new TextField();
        emailField.setPromptText("email@exemplo.com");

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(() -> nameField.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButtonType) {
                return new UserData(nameField.getText(), emailField.getText());
            }
            return null;
        });

        Optional<UserData> result = dialog.showAndWait();

        result.ifPresent(userData -> {
            String name = userData.getName();
            String email = userData.getEmail();

            if (name == null || name.trim().isEmpty()) {
                showMessage("Erro: Nome é obrigatório!");
                return;
            }

            if (email == null || email.trim().isEmpty() || !email.contains("@")) {
                showMessage("Erro: Email válido é obrigatório!");
                return;
            }

            faceService.register(frame, name, email, this::showMessage);
        });
    }

    private void toggleCamera() {
        if (cameraService.isCameraActive()) {
            cameraService.stopCamera();
            cameraToggle.setText(CAMERA_ON_TEXT);
        } else {
            cameraService.startCamera(frame -> {
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