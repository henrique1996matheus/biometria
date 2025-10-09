package com.unip.controller;

import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

public class FormUserController implements Initializable{
    

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
    void cancel_edition(MouseEvent event) {

    }

    @FXML
    void save_new_date(MouseEvent event) {

    }
    
     @Override
    public void initialize(URL url, ResourceBundle rb) {
    }
}
