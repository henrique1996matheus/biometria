package com.unip.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class MainWindowBaseUserController implements Initializable{
    
    // @Autowired
    // private PropertyService propertyService;

    // private ObservableList<Property> propertiesList;

    @FXML
    private Button properties_btn;

    @FXML
    private TableView<?> properties_table;

    @FXML
    private VBox properties_vbox;

    @FXML
    private StackPane stc_pane_pages;

    @FXML
    private TableColumn<?, String> tbl_col_address;

    @FXML
    private TableColumn<?, String> tbl_col_fisc_date;

    @FXML
    private TableColumn<?, ?> tbl_col_owner;

    // private final PropriedadeRuralService propertyService = new PropriedadeRuralService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        
        // loadPropertiesData();
    }

    // public void refreshPropertiesTables() {
    //     loadPropertiesData();

    // }

    // private void loadPropertiesData() {

    //     List<Property> properties = propertyService.findAll();
        

    //     propertiesList = FXCollections.observableArrayList(properties);
        

    //     properties_table.setItems(usersList);
    // }

    // private void setupPropertiesTable() {
    //     tb_col_address.setCellValueFactory(new PropertyValueFactory<>("address"));
    //     tbl_col_owner.setCellValueFactory(new PropertyValueFactory<>("owner"));
    //     tbl_col_fisc_date.setCellValueFactory(new PropertyValueFactory<>("date"));

    // }
}
