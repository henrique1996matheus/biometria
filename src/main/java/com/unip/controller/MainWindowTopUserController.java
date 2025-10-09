package com.unip.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;

public class MainWindowHighLevelUserController implements Initializable{
    

     @FXML
    private Button add_user_btn;

    @FXML
    private VBox add_users;

    @FXML
    private Button properties_btn;

    @FXML
    private VBox properties_infos;

    @FXML
    private TableView<?> properties_table;

    @FXML
    private StackPane stc_pane_pages;

    @FXML
    private TableColumn<?, ?> tb_col_address;

    @FXML
    private TableColumn<?, ?> tb_col_email;

    @FXML
    private TableColumn<?, ?> tb_col_level_access;

    @FXML
    private TableColumn<?, ?> tb_col_username;

    @FXML
    private TableColumn<?, ?> tbl_col_address;

    @FXML
    private TableColumn<?, ?> tbl_col_fisc_date;

    @FXML
    private TableColumn<?, ?> tbl_col_owner;

    @FXML
    private TableView<?> users_table;

    @FXML
    private Button users_btn;

    @FXML
    private VBox users_info;

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
    }

    @FXML
    void open_users_pane(MouseEvent event) {
        add_users.setVisible(false);
        properties_infos.setVisible(false);
        users_info.setVisible(true);
    }

    private final UsuarioService usuarioService = new UsuarioService();
    private final PropriedadeRuralService propertyService = new PropriedadeRuralService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        
        add_users.setVisible(false);
        properties_infos.setVisible(true);
        users_info.setVisible(false);

        tbl_col_address.setCellValueFactory(new PropertyValueFactory<>("address"));
        tbl_col_owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
        tbl_col_fisc_date.setCellValueFactory(new PropertyValueFactory<>("date"));
        
        tb_col_username.setCellValueFactory(new PropertyValueFactory<>("username"));
        tb_col_email.setCellValueFactory(new PropertyValueFactory<>("email"));
        tb_col_level_access.setCellValueFactory(new PropertyValueFactory<>("role"));

        refreshTables();
    }

    private void refreshPropertiesTable() {
        List<PropriedadeRural> properties = propriedadeService.listarTodasPropriedades();
        properties_table.setItems(FXCollections.observableArrayList(properties));
    }

    private void refreshUsersTable() {
        List<Usuario> users = usuarioService.listarTodosUsuarios();
        users_table.setItems(FXCollections.observableArrayList(users));
    }

    private void refreshTables() {
        refreshUsersTable();
        refreshPropertiesTable();
    }
}
