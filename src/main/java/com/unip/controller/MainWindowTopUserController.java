package com.unip.controller;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Optional;

import org.bytedeco.opencv.opencv_core.Mat;
import org.springframework.beans.factory.annotation.Autowired;

import com.unip.model.Role;
import com.unip.model.RuralProperty;
import com.unip.model.User;
import com.unip.service.UserService;

import com.unip.service.CameraService;
import com.unip.service.FaceService;
import com.unip.service.RuralPropertyService;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.control.TableCell;

public class MainWindowTopUserController implements Initializable{

    @Autowired
    private CameraService cameraService;

    @Autowired
    private FaceService faceService;
    
    @Autowired
    private UserService userService;

    private ObservableList<User> usersList;

    private volatile boolean markFaces = false;
    private volatile boolean cameraActive = false;
    
    @Autowired
    private RuralPropertyService propertyService;

    private ObservableList<RuralProperty> propertiesList;

    @FXML
    private Button add_user_btn;

    @FXML
    private VBox add_users;

    @FXML
    private Button btn_register_user;

    @FXML
    private ImageView camera_view;

    @FXML
    private Button properties_btn;

    @FXML
    private VBox properties_infos;

    @FXML
    private TableView<RuralProperty> properties_table;

    @FXML
    private RadioButton radio_camera;

    @FXML
    private RadioButton radio_mark_face;

    @FXML
    private StackPane stc_pane_pages;

    @FXML
    private TableColumn<User, Void> tb_col_acoes;

    @FXML
    private TableColumn<RuralProperty, Void> tb_col_acoes_properties;

    @FXML
    private TableColumn<User, String> tb_col_email;

    @FXML
    private TableColumn<User, String> tb_col_level_access;

    @FXML
    private TableColumn<User, String> tb_col_username;

    @FXML
    private TableColumn<RuralProperty, LocalDate> tbl_col_fisc_date;

    @FXML
    private TableColumn<RuralProperty, String> tbl_col_owner;

    @FXML
    private Button users_btn;

    @FXML
    private VBox users_info;

    @FXML
    private TableView<User> users_table;

    @FXML
    void open_add_users_pane(MouseEvent event) {
        add_users.setVisible(true);
        properties_infos.setVisible(false);
        users_info.setVisible(false);
    }

    @FXML
    void open_properties_pane(MouseEvent event) {
        add_users.setVisible(false);
        properties_infos.setVisible(true);
        users_info.setVisible(false);

        stopCamera();

        refreshPropertiesTables();
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
    void open_users_pane(MouseEvent event) {
        add_users.setVisible(false);
        properties_infos.setVisible(false);
        users_info.setVisible(true);

        stopCamera();

        refreshUsersTables();
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
        
        add_users.setVisible(false);
        properties_infos.setVisible(true);
        users_info.setVisible(false);

        setupUsersTable();
        setupPropertiesTable();
        loadUsersData();
        loadPropertiesData();

        radio_camera.setText("Ligar Câmera");
        radio_mark_face.setVisible(false);
    }

    
    private void setupUsersTable() {

        tb_col_username.setCellValueFactory(new PropertyValueFactory<>("name"));
        tb_col_email.setCellValueFactory(new PropertyValueFactory<>("email"));
        tb_col_level_access.setCellValueFactory(new PropertyValueFactory<>("role"));

        setupActionsColumn();
        
    }
    
    private void setupPropertiesTable() {
        
        tbl_col_owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        tbl_col_fisc_date.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        setupPropertiesActionsColumn();
    }

    private void setupPropertiesActionsColumn() {
        tb_col_acoes_properties.setCellFactory(param -> new TableCell<RuralProperty, Void>() {
            private final Button btnEditar = new Button("✏️");
            private final Button btnExcluir = new Button("❌");
            private final HBox botoes = new HBox(btnEditar, btnExcluir);

            {
                // Configuração dos botões
                btnEditar.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand;");
                btnExcluir.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand;");
                
                // Tooltips
                btnEditar.setTooltip(new Tooltip("Editar propriedade"));
                btnExcluir.setTooltip(new Tooltip("Excluir propriedade"));
                
                botoes.setSpacing(8);
                botoes.setAlignment(Pos.CENTER);

                // Ação do botão editar
                btnEditar.setOnAction(event -> {
                    RuralProperty property = getTableView().getItems().get(getIndex());
                    editarProperty(property);
                });

                // Ação do botão excluir
                btnExcluir.setOnAction(event -> {
                    RuralProperty property = getTableView().getItems().get(getIndex());
                    excluirProperty(property);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(botoes);
                }
            }
        });
    }

    private void editarProperty(RuralProperty property) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/FormProperty.fxml")); 
            
            Parent root = loader.load();

            FormPropertyController formController = loader.getController();
            formController.setPropertyToEdit(property);
            formController.setMainController(this);

            Stage stage = new Stage();
            stage.setTitle("Editar Propriedade - " + property.getOwner());
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showMessage("Erro ao abrir formulário de edição: " + e.getMessage());
        }
    }


    private void excluirProperty(RuralProperty property) {
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmação de Exclusão");
        confirmacao.setHeaderText("Excluir Propriedade");
        confirmacao.setContentText("Tem certeza que deseja excluir a propriedade " + property.getOwner() + "?");

        Optional<ButtonType> resultado = confirmacao.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                propertyService.deletarPropriedade(property.getId());
                
                propertiesList.remove(property);
                
                showMessage("Propriedade excluída com sucesso!");
                
            } catch (Exception e) {
                e.printStackTrace();
                showMessage("Erro ao excluir propriedade: " + e.getMessage());
            }
        }
    }

    private void setupActionsColumn() {
        tb_col_acoes.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button btnEditar = new Button("✏️");
            private final Button btnExcluir = new Button("❌");
            private final HBox botoes = new HBox(btnEditar, btnExcluir);

            {
                btnEditar.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand;");
                btnExcluir.setStyle("-fx-background-color: transparent; -fx-font-size: 14px; -fx-cursor: hand;");
                
                btnEditar.setTooltip(new Tooltip("Editar usuário"));
                btnExcluir.setTooltip(new Tooltip("Excluir usuário"));
                
                botoes.setSpacing(8);
                botoes.setAlignment(Pos.CENTER);

                btnEditar.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    editarUsuario(user);
                });

                btnExcluir.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    excluirUsuario(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(botoes);
                }
            }
        });
    }

    private void excluirUsuario(User user) {
        Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacao.setTitle("Confirmação de Exclusão");
        confirmacao.setHeaderText("Excluir Usuário");
        confirmacao.setContentText("Tem certeza que deseja excluir o usuário " + user.getName() + "?");

        Optional<ButtonType> resultado = confirmacao.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                // Exclui o usuário do banco de dados
                userService.delete(user.getId());
                
                // Remove da lista local
                usersList.remove(user);
                
                showMessage("Usuário excluído com sucesso!");
                
            } catch (Exception e) {
                e.printStackTrace();
                showMessage("Erro ao excluir usuário: " + e.getMessage());
            }
        }
    }

    private void editarUsuario(User user) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/FormUser.fxml"));
        
        Parent root = loader.load();

        FormUserController formController = loader.getController();
        formController.setUserToEdit(user);
        formController.setMainController(this);

        Stage stage = new Stage();
        stage.setTitle("Editar Usuário - " + user.getName());
        stage.setScene(new Scene(root));
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.showAndWait();

    } catch (IOException e) {
        e.printStackTrace();
        showMessage("Erro ao abrir formulário de edição: " + e.getMessage());
    }
}


    private void loadUsersData() {
        
        List<User> users = userService.findAll();
        
        
        usersList = FXCollections.observableArrayList(users);
        
        
        users_table.setItems(usersList);
    }
    
    private void loadPropertiesData() {
        
        List<RuralProperty> properties = propertyService.listarTodasPropriedades();
        
    
        propertiesList = FXCollections.observableArrayList(properties);
    
    
        properties_table.setItems(propertiesList);
    }
    
    public void refreshUsersTables() {
        loadUsersData();
        
    }
    
    public void refreshPropertiesTables() {
            loadPropertiesData();
        
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

        // ComboBox para selecionar o nível de acesso
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

            // Registrar o usuário
            faceService.register(frame, name, email, role, (message, registeredRole) -> {
                showMessage(message);
                // Atualiza a tabela de usuários após o registro
                if (registeredRole != null) {
                    refreshUsersTables();
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
}
