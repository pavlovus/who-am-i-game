package com.whoami.client;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class MainClient extends Application {

    private static Scene scene;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Set AtlantaFX PrimerDark base theme
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // Load Login Screen
        URL fxmlLocation = getClass().getResource("/fxml/LoginScreen.fxml");
        if (fxmlLocation == null) {
            throw new IllegalStateException("Cannot find /fxml/LoginScreen.fxml. Make sure it exists in src/main/resources/fxml/");
        }

        FXMLLoader loader = new FXMLLoader(fxmlLocation);
        Parent root = loader.load();

        scene = new Scene(root, 900, 600);
        
        // Load custom Midnight Green CSS
        URL cssLocation = getClass().getResource("/css/midnight-green.css");
        if (cssLocation != null) {
            scene.getStylesheets().add(cssLocation.toExternalForm());
        } else {
            System.err.println("Warning: Custom CSS not found at /css/midnight-green.css");
        }

        primaryStage.setTitle("Хто я? - Онлайн Гра");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(500);
        primaryStage.show();
    }

    public static Scene getScene() {
        return scene;
    }

    public static void setRoot(String fxml) throws IOException {
        URL fxmlLocation = MainClient.class.getResource("/fxml/" + fxml + ".fxml");
        if (fxmlLocation == null) {
            throw new IllegalStateException("Cannot find /fxml/" + fxml + ".fxml");
        }
        Parent root = FXMLLoader.load(fxmlLocation);
        scene.setRoot(root);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
