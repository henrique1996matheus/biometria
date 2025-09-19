package com.unip.application;

import com.unip.controller.UIController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args); // inicia a aplicação JavaFX
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/view/CameraView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 600);
        stage.setTitle("Biometria - Câmera");
        stage.setScene(scene);

        stage.setOnCloseRequest(event -> {
            UIController controller = fxmlLoader.getController();
            if (controller != null) {
                controller.shutdown();
            }
        });

        stage.show();
    }
}