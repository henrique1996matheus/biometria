package com.unip.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

import org.springframework.stereotype.Component;

import com.unip.config.SpringContext;
import com.unip.model.RuralProperty;
import com.unip.service.RuralPropertyService;

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

@Component
public class MainWindowBaseUserController implements Initializable {

    private RuralPropertyService propertyService;
    private ObservableList<RuralProperty> propertiesList;

    @FXML
    private Button properties_btn;

    @FXML
    private TableView<RuralProperty> properties_table;

    @FXML
    private VBox properties_vbox;

    @FXML
    private StackPane stc_pane_pages;

    @FXML
    private TableColumn<RuralProperty, LocalDate> tbl_col_fisc_date;

    @FXML
    private TableColumn<RuralProperty, String> tbl_col_owner;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
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

    public void setPropertyService(RuralPropertyService propertyService) {
        this.propertyService = propertyService;
        loadPropertiesData();
    }
}