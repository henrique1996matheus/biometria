package com.unip.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.unip.config.SpringContext;
import com.unip.model.Role;
import com.unip.model.RuralProperty;
import com.unip.service.CameraService;
import com.unip.service.FaceService;
import com.unip.service.RuralPropertyService;
import com.unip.service.UserService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

@Component
public class MainWindowIntermediaryUserController implements Initializable{
    
    private RuralPropertyService propertyService; 
    private ObservableList<RuralProperty> propertiesList;

    @Autowired
    private CameraService cameraService;

    @Autowired
    private FaceService faceService;

    private volatile boolean markFaces = false;
    private volatile boolean cameraActive = false;

    @FXML
    private Button add_user_btn;

    @FXML
    private Button btn_register_user;

    @FXML
    private ImageView camera_view;

    @FXML
    private VBox medium_acess_add_users;

    @FXML
    private VBox medium_acess_user_prop;

    @FXML
    private Button properties_btn;

    @FXML
    private TableView<RuralProperty> properties_table;

    @FXML
    private RadioButton radio_camera;

    @FXML
    private RadioButton radio_mark_face;

    @FXML
    private StackPane stc_pane_pages;

    @FXML
    private TableColumn<RuralProperty, LocalDate> tbl_col_fisc_date;

    @FXML
    private TableColumn<RuralProperty, String> tbl_col_owner;

    @FXML
    void open_add_user_pane(MouseEvent event) {
        medium_acess_user_prop.setVisible(false);
        medium_acess_add_users.setVisible(true);
    }

    @FXML
    void open_properties_pane(MouseEvent event) {
        medium_acess_add_users.setVisible(false);
        medium_acess_user_prop.setVisible(true);

        loadPropertiesData();
        stopCamera();
    }
    
    @FXML
    void toggle_camera(MouseEvent event) {
        if (cameraActive) {
            stopCamera();
            radio_camera.setText("Ligar Câmera");
        } else {
            startCamera();
            radio_camera.setText("Desligar Câmera");
        }
        radio_mark_face.setVisible(cameraActive);
    }

    @FXML
    void toggle_mark_faces(MouseEvent event) {
        markFaces = !markFaces;
    }

    @FXML
    void register_user(MouseEvent event) {
        try {
            Mat frame = cameraService.captureFrame();
            if (frame != null) {
                showRegistrationDialog(frame);
            } else {
                showMessage("Erro: Não foi possível capturar a imagem da câmera!");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            showMessage("Erro ao capturar imagem: " + ex.getMessage());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        medium_acess_add_users.setVisible(false);
        medium_acess_user_prop.setVisible(true);

        this.propertyService = SpringContext.getBean(RuralPropertyService.class);
        
        setupPropertiesTable(); 
        loadPropertiesData();
    }

    public void refreshPropertiesTables() {
        loadPropertiesData();
    }

    private void loadPropertiesData() {
        if (propertyService != null) {
            List<RuralProperty> properties = propertyService.listarTodasPropriedades();
            propertiesList = FXCollections.observableArrayList(properties);
            properties_table.setItems(propertiesList);
        } else {
            System.err.println("ERRO: propertyService é null!");
        }
    }

    private void setupPropertiesTable() {
        tbl_col_owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        tbl_col_fisc_date.setCellValueFactory(new PropertyValueFactory<>("inspectionDate")); 
    }

    private void startCamera() {
        cameraService.startCamera(frame -> {
            Platform.runLater(() -> {
                if (camera_view != null) {
                    camera_view.setImage(matToImage(frame));
                }
            });
        });
        cameraActive = true;
    }

    private void stopCamera() {
        if (cameraActive) {
            cameraService.stopCamera();
            cameraActive = false;
        }
    }

    private static class UserData {
        private final String name;
        private final String email;
        private final Role role;

        public UserData(String name, String email, Role role) {
            this.name = name;
            this.email = email;
            this.role = role;
        }

        public String getName() { return name; }
        public String getEmail() { return email; }
        public Role getRole() { return role; }
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

        ComboBox<Role> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll(Role.LEVEL_1, Role.LEVEL_2);
        roleComboBox.setValue(Role.LEVEL_1);

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Nível:"), 0, 2);
        grid.add(roleComboBox, 1, 2);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(() -> nameField.requestFocus());

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registerButtonType) {
                return new UserData(nameField.getText(), emailField.getText(), roleComboBox.getValue());
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
                if (registeredRole != null) {
                }
            });
        });
    }

    private javafx.scene.image.Image matToImage(Mat frame) {
        try {
            int width = frame.cols();
            int height = frame.rows();
            int channels = frame.channels();
            byte[] buffer = new byte[width * height * channels];
            frame.data().get(buffer);

            javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(width, height);
            javafx.scene.image.PixelWriter pw = image.getPixelWriter();
            pw.setPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getByteRgbInstance(), buffer, 0, width * channels);
            return image;
        } catch (Exception e) {
            return null;
        }
    }

    private void showMessage(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Sistema");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

    public void shutdown() {
        stopCamera();
    }

    public void setPropertyService(RuralPropertyService propertyService) {
        this.propertyService = propertyService;
        loadPropertiesData(); 
    }
}