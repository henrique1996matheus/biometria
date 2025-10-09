package com.unip.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainWindowIntermediaryUserController implements Initializable{
    
    @FXML
    private Button add_user_btn;

    @FXML
    private VBox medium_acess_add_users;

    @FXML
    private VBox medium_acess_user_prop;

    @FXML
    private Button properties_btn;

    @FXML
    private TableView<?> properties_table;

    @FXML
    private StackPane stc_pane_pages;

    @FXML
    private TableColumn<?, ?> tbl_col_address;

    @FXML
    private TableColumn<?, ?> tbl_col_fisc_date;

    @FXML
    private TableColumn<?, ?> tbl_col_owner;

    @FXML
    void open_add_user_pane(MouseEvent event) {
        
        medium_acess_user_prop.setVisible(false);
        medium_acess_add_users.setVisible(true);
    }

    @FXML
    void open_properties_pane(MouseEvent event) {
        medium_acess_add_users.setVisible(false);
        medium_acess_user_prop.setVisible(true);
    }
    
   private final UsuarioService usuarioService = new UsuarioService();
    private final PropriedadeRuralService propertyService = new PropriedadeRuralService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        medium_acess_add_users.setVisible(false);
        medium_acess_user_prop.setVisible(true);

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
