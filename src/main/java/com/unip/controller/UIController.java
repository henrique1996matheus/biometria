package com.unip.controller;

import com.unip.model.Role;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.scene.image.ImageView;

import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.unip.service.CameraService;
import com.unip.service.FaceService;

import java.util.Optional;

import com.unip.service.RuralPropertyService;

@Component
public class UIController {

    @Autowired
    private RuralPropertyService propertyService; // Já deve estar injetado aqui

    @FXML
    private RadioButton cameraToggle;

    @FXML
    private RadioButton markFacesToggle;

    @FXML
    private Button registerFaceButton;

    @FXML
    private Button authFaceButton;

    @FXML
    private ImageView cameraView;

    private volatile boolean markFaces = false;

    @Autowired
    private CameraService cameraService;

    @Autowired
    private FaceService faceService;

    private static final String CAMERA_ON_TEXT = "Turn On Camera";
    private static final String CAMERA_OFF_TEXT = "Turn Off Camera";

    // Classe interna para armazenar nome, email e role
    private static class UserData {
        private final String name;
        private final String email;
        private final Role role;

        public UserData(String name, String email, Role role) {
            this.name = name;
            this.email = email;
            this.role = role;
        }

        public String getName() {
            return name;
        }

        public String getEmail() {
            return email;
        }

        public Role getRole() {
            return role;
        }
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
                    faceService.authenticate(frame, (message,role) ->{
                        if (role!= null){
                            openRoleWindow(role);
                        }else{
                            showMessage(message);
                        }
                    });
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
                return new UserData(nameField.getText(), emailField.getText(),Role.LEVEL_1);
            }
            return null;
        });

        Optional<UserData> result = dialog.showAndWait();

        result.ifPresent(userData -> {
            String name = userData.getName();
            String email = userData.getEmail();
            Role role = userData.getRole();

            if (name == null || name.trim().isEmpty()) {
                showMessage("Erro: Nome é obrigatório!");
                return;
            }

            if (email == null || email.trim().isEmpty() || !email.contains("@")) {
                showMessage("Erro: Email válido é obrigatório!");
                return;
            }

            faceService.register(frame, name, email, role, (message, registeredRole) -> {
            showMessage(message);
            if(registeredRole != null){
                openRoleWindow(registeredRole);
            }
            });
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
        pw.setPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getByteRgbInstance(), buffer, 0,
                width * channels);
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

    private void showMessage(String message, Role role) {
        showMessage(message);
    }

    public void shutdown() {
        cameraService.stopCamera();
    }

    private void openRoleWindow(Role role) {
    Platform.runLater(() -> {
        try {
            String fxmlFile = "";
            String title = "";
            
            switch (role) {
                case LEVEL_1:
                    fxmlFile = "/view/MainWindowBaseUser.fxml";
                    title = "Sistema - Nível 1";
                    break;
                case LEVEL_2:
                    fxmlFile = "/view/MainWindowIntermediaryUser.fxml";
                    title = "Sistema - Nível 2";
                    break;
                case LEVEL_3:
                    fxmlFile = "/view/MainWindowTopUser.fxml";
                    title = "Sistema - Nível 3";
                    break;
                default:
                    fxmlFile = "/view/MainWindowBaseUser.fxml";
                    title = "Sistema - Nível 1";
            }
            
            Stage currentStage = (Stage) cameraView.getScene().getWindow();
            currentStage.close();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            
            Object controller = loader.getController();
            if (controller instanceof MainWindowBaseUserController) {
                ((MainWindowBaseUserController) controller).setPropertyService(propertyService);
            } else if (controller instanceof MainWindowIntermediaryUserController) {
                ((MainWindowIntermediaryUserController) controller).setPropertyService(propertyService);
            } else if (controller instanceof MainWindowTopUserController) {
                ((MainWindowTopUserController) controller).setPropertyService(propertyService);
            }
            
            Stage newStage = new Stage();
            newStage.setTitle(title);
            newStage.setScene(new Scene(root));
            newStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showMessage("Erro ao abrir a janela: " + e.getMessage());
        }
    });
}

}