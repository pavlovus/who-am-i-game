package com.whoami.client.ui.controllers;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.application.Platform;

import java.nio.charset.StandardCharsets;

import com.whoami.client.MainClient;
import com.whoami.client.network.PacketListener;
import com.whoami.client.network.ServerConnection;
import com.whoami.client.state.ClientContext;
import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;
import com.whoami.protocol.validation.CredentialsValidator;
import com.whoami.protocol.validation.ValidationResult;

public class LoginController implements PacketListener {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        startGlowAnimation(loginButton);
        
        ServerConnection connection = ClientContext.getInstance().getServerConnection();
        if (connection != null) {
            connection.setPacketListener(this);
        }
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        ValidationResult validation = CredentialsValidator.validateCredentials(username, password);
        if (!validation.isValid()) {
            showError(validation.getMessage());
            shakeNode(usernameField);
            shakeNode(passwordField);
            return;
        }

        // Send LOGIN request
        String payloadString = "LOGIN:" + username + ":" + password;
        byte[] payload = payloadString.getBytes(StandardCharsets.UTF_8);
        Packet request = new Packet(Packet.MAGIC_BYTE, 0, PacketType.AUTH_REQUEST.getId(), payload.length, (short)0, payload);

        ClientContext.getInstance().getServerConnection().sendPacket(request);
        System.out.println("Sent LOGIN request for user: " + username);
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        ValidationResult validation = CredentialsValidator.validateCredentials(username, password);
        if (!validation.isValid()) {
            showError(validation.getMessage());
            shakeNode(usernameField);
            shakeNode(passwordField);
            return;
        }

        // Send REGISTER request
        String payloadString = "REGISTER:" + username + ":" + password;
        byte[] payload = payloadString.getBytes(StandardCharsets.UTF_8);
        Packet request = new Packet(Packet.MAGIC_BYTE, 0, PacketType.AUTH_REQUEST.getId(), payload.length, (short)0, payload);

        ClientContext.getInstance().getServerConnection().sendPacket(request);
        System.out.println("Sent REGISTER request for user: " + username);
    }

    @Override
    public void onPacketReceived(Packet packet) {
        if (packet.getPacketType() == PacketType.AUTH_RESPONSE.getId()) {
            String payloadStr = new String(packet.getPayload(), StandardCharsets.UTF_8);
            String[] parts = payloadStr.split(":", 2); // Split into SUCCESS/ERROR and message/token
            
            Platform.runLater(() -> {
                if (parts[0].equals("SUCCESS")) {
                    ClientContext.getInstance().setJwtToken(parts[1]);
                    ClientContext.getInstance().setUsername(usernameField.getText());
                    System.out.println("Authentication successful! Token saved.");
                    try {
                        MainClient.setRoot("MainMenuScreen");
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Error loading Main Menu");
                    }
                } else {
                    errorLabel.setTextFill(Color.web("#f85149")); // Red color
                    showError("Auth Error: " + (parts.length > 1 ? parts[1] : "Unknown error"));
                    shakeNode(usernameField);
                    shakeNode(passwordField);
                }
            });
        }
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            errorLabel.setTextFill(Color.web("#f85149"));
            showError("Disconnected from server.");
        });
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setManaged(true);
        errorLabel.setVisible(true);

        FadeTransition ft = new FadeTransition(Duration.millis(150), errorLabel);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    private void shakeNode(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0f);
        tt.setByX(10f);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.playFromStart();
    }

    private void startGlowAnimation(Button button) {
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#00d46a55"));
        glow.setRadius(5);
        button.setEffect(glow);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 5)),
                new KeyFrame(Duration.millis(750), new KeyValue(glow.radiusProperty(), 20)),
                new KeyFrame(Duration.millis(1500), new KeyValue(glow.radiusProperty(), 5))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
}
