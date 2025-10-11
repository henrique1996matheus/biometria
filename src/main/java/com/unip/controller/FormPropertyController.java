package com.unip.controller;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

import org.springframework.beans.factory.annotation.Autowired;

import com.unip.model.RuralProperty;
import com.unip.service.RuralPropertyService;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class FormPropertyController implements Initializable{
    
    @Autowired
    private RuralPropertyService propertyService;
    
    private RuralProperty propertyToEdit;
    private MainWindowTopUserController mainController;
    
    @FXML
    private Button btn_cancel;

    @FXML
    private Button btn_save;

    @FXML
    private DatePicker dtpicker_fiscalization;

    @FXML
    private TextField txtfield_address;

    @FXML
    private TextField txtfield_owner;

    @FXML
    void cancel_edition() {
        closeWindow();
    }

    @FXML
    void save_new_date() {
        saveProperty();
    }
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configureFields();
    }

    private void configureFields() {
        txtfield_address.setEditable(false);
        txtfield_owner.setEditable(false);
        
        txtfield_address.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #666666;");
        txtfield_owner.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #666666;");
        
        dtpicker_fiscalization.setStyle("-fx-background-color: white;");
    }

    public void setPropertyToEdit(RuralProperty property) {
        this.propertyToEdit = property;
        loadPropertyData();
    }

    public void setMainController(MainWindowTopUserController mainController) {
        this.mainController = mainController;
    }

    private void loadPropertyData() {
        if (propertyToEdit != null) {

            txtfield_owner.setText(propertyToEdit.getOwner());
            
            if (propertyToEdit.getInspectionDate() != null) {
                dtpicker_fiscalization.setValue(propertyToEdit.getInspectionDate());
            }
        }
    }

    private void saveProperty() {
        if (propertyToEdit == null) {
            showAlert("Erro", "Nenhuma propriedade selecionada para edição.");
            return;
        }

        try {
            LocalDate newDate = dtpicker_fiscalization.getValue();
            if (newDate == null) {
                showAlert("Erro", "Selecione uma data de fiscalização válida.");
                return;
            }

            // Atualiza a data diretamente no objeto
            propertyToEdit.setInspectionDate(newDate);
            
            // Salva no banco de dados
            propertyService.atualizarPropriedade(propertyToEdit);

            if (mainController != null) {
                mainController.refreshPropertiesTables();
            }

            showAlert("Sucesso", "Data de fiscalização atualizada com sucesso!");
            closeWindow();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erro", "Erro ao atualizar propriedade: " + e.getMessage());
        }
    }

    private void closeWindow() {
        Stage stage = (Stage) btn_cancel.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}