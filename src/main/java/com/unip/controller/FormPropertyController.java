package com.unip.controller;

import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

public class FormPropertyController implements Initializable{
    
    @FXML
    private Button btn_cancel;

    @FXML
    private Button btn_save;

    @FXML
    private TextField txtfield_email;

    @FXML
    private TextField txtfield_level_access;

    @FXML
    private TextField txtfield_username;

    @FXML
    void cancel_edition(MouseEvent event) {

    }

    @FXML
    void save_new_access_level(MouseEvent event) {

    }

     @Override
    public void initialize(URL url, ResourceBundle rb) {
    }
}
