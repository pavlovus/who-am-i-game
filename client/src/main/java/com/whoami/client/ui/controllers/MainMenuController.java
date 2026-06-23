package com.whoami.client.ui.controllers;

import com.whoami.client.network.PacketListener;
import com.whoami.client.network.ServerConnection;
import com.whoami.client.state.ClientContext;
import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class MainMenuController implements PacketListener {

    @FXML private Label welcomeLabel;
    @FXML private Label statusLabel;
    @FXML private VBox headerBox;
    @FXML private VBox actionBox;
    @FXML private TextField roomCodeField;
    @FXML private Button createRoomBtn;
    @FXML private Button joinRoomBtn;
    
    @FXML private VBox waitingBox;
    @FXML private Canvas radarCanvas;
    @FXML private Label displayRoomCode;

    private AnimationTimer radarTimer;
    private double currentAngle = 0;
    private List<Blip> blips = new ArrayList<>();
    private Random random = new Random();

    private static class Blip {
        double x, y, alpha;
        Blip(double x, double y) {
            this.x = x;
            this.y = y;
            this.alpha = 1.0;
        }
    }

    @FXML
    public void initialize() {
        welcomeLabel.setText("Welcome, " + ClientContext.getInstance().getUsername() + "!");
        
        ServerConnection connection = ClientContext.getInstance().getServerConnection();
        if (connection != null) {
            connection.setPacketListener(this);
        }

        setupRadar();
    }

    private void setupRadar() {
        GraphicsContext gc = radarCanvas.getGraphicsContext2D();
        double width = radarCanvas.getWidth();
        double height = radarCanvas.getHeight();
        double cx = width / 2;
        double cy = height / 2;
        double radius = Math.min(cx, cy) - 10;

        radarTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // Clear canvas completely
                gc.clearRect(0, 0, width, height);

                // 1. Draw Grid
                gc.setStroke(Color.web("#1a2e1f"));
                gc.setLineWidth(1.5);
                
                // Concentric circles
                for (int i = 1; i <= 4; i++) {
                    double r = radius * (i / 4.0);
                    gc.strokeOval(cx - r, cy - r, r * 2, r * 2);
                }
                
                // Crosshairs
                gc.strokeLine(cx, cy - radius, cx, cy + radius);
                gc.strokeLine(cx - radius, cy, cx + radius, cy);
                
                // Diagonal crosshairs
                double diag = radius * Math.sin(Math.PI / 4);
                gc.strokeLine(cx - diag, cy - diag, cx + diag, cy + diag);
                gc.strokeLine(cx - diag, cy + diag, cx + diag, cy - diag);

                // 2. Draw Sweep Tail (Conical Gradient approximation)
                double sweepLength = 120; // 120 degrees tail
                for (int i = 0; i < sweepLength; i++) {
                    double alpha = 1.0 - (i / sweepLength);
                    // Fade out non-linearly for cooler effect
                    alpha = Math.pow(alpha, 2) * 0.6;
                    gc.setFill(Color.web("#00d46a").deriveColor(0, 1, 1, alpha));
                    gc.fillArc(cx - radius, cy - radius, radius * 2, radius * 2, currentAngle - i, 2, ArcType.ROUND);
                }

                // Sweep leading line
                gc.setStroke(Color.web("#00f077"));
                gc.setLineWidth(2.5);
                gc.strokeLine(cx, cy, cx + radius * Math.cos(Math.toRadians(-currentAngle)), cy + radius * Math.sin(Math.toRadians(-currentAngle)));

                // 3. Update and Draw Blips
                // Spawn new blip occasionally
                if (random.nextDouble() < 0.015) { // 1.5% chance per frame
                    double r = random.nextDouble() * radius;
                    double a = random.nextDouble() * Math.PI * 2;
                    blips.add(new Blip(cx + r * Math.cos(a), cy + r * Math.sin(a)));
                }

                Iterator<Blip> it = blips.iterator();
                while (it.hasNext()) {
                    Blip b = it.next();
                    // Decay alpha slowly
                    b.alpha -= 0.005;
                    if (b.alpha <= 0) {
                        it.remove();
                    } else {
                        // Draw blip glow
                        gc.setFill(Color.web("#00d46a").deriveColor(0, 1, 1, b.alpha * 0.5));
                        gc.fillOval(b.x - 6, b.y - 6, 12, 12);
                        // Draw blip core
                        gc.setFill(Color.web("#ffffff").deriveColor(0, 1, 1, b.alpha));
                        gc.fillOval(b.x - 2, b.y - 2, 4, 4);
                    }
                }

                // Advance angle
                currentAngle += 2.5; // Speed of rotation
                if (currentAngle >= 360) currentAngle = 0;
            }
        };
    }

    @FXML
    public void handleCreateRoom() {
        Packet request = new Packet(Packet.MAGIC_BYTE, 0, PacketType.ROOM_CREATE.getId(), 0, (short)0, new byte[0]);
        ClientContext.getInstance().getServerConnection().sendPacket(request);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    @FXML
    public void handleJoinRoom() {
        String code = roomCodeField.getText().trim().toUpperCase();
        if (code.isEmpty()) {
            showStatus("Please enter room code!", true);
            return;
        }

        // Set eagerly to prevent race condition if GAME_START arrives before ROOM_JOIN response
        ClientContext.getInstance().setCurrentRoomCode(code);

        byte[] payload = code.getBytes(StandardCharsets.UTF_8);
        Packet request = new Packet(Packet.MAGIC_BYTE, 0, PacketType.ROOM_JOIN.getId(), payload.length, (short)0, payload);
        ClientContext.getInstance().getServerConnection().sendPacket(request);
        showStatus("Connecting...", false);
    }

    @FXML
    public void handleBack() {
        Packet request = new Packet(Packet.MAGIC_BYTE, 0, PacketType.ROOM_LEAVE.getId(), 0, (short)0, new byte[0]);
        ClientContext.getInstance().getServerConnection().sendPacket(request);
        
        radarTimer.stop();
        
        waitingBox.setVisible(false);
        waitingBox.setManaged(false);
        
        headerBox.setVisible(true);
        headerBox.setManaged(true);
        
        actionBox.setVisible(true);
        actionBox.setManaged(true);
        
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }

    private void startWaitingState(String roomCode) {
        headerBox.setVisible(false);
        headerBox.setManaged(false);
        actionBox.setVisible(false);
        actionBox.setManaged(false);
        waitingBox.setVisible(true);
        waitingBox.setManaged(true);
        displayRoomCode.setText(roomCode);
        radarTimer.start();
    }

    @Override
    public void onPacketReceived(Packet packet) {
        if (packet.getPacketType() == PacketType.ROOM_CREATE.getId()) {
            String payload = new String(packet.getPayload(), StandardCharsets.UTF_8);
            String[] parts = payload.split(":");
            Platform.runLater(() -> {
                if (parts[0].equals("SUCCESS")) {
                    String roomCode = parts[1];
                    ClientContext.getInstance().setCurrentRoomCode(roomCode);
                    startWaitingState(roomCode);
                } else {
                    showStatus("Error creating room", true);
                }
            });
        } else if (packet.getPacketType() == PacketType.ROOM_JOIN.getId()) {
            String payload = new String(packet.getPayload(), StandardCharsets.UTF_8);
            String[] parts = payload.split(":");
            Platform.runLater(() -> {
                if (parts[0].equals("SUCCESS")) {
                    ClientContext.getInstance().setCurrentRoomCode(roomCodeField.getText().toUpperCase());
                    showStatus("Joined! Waiting for start...", false);
                } else {
                    showStatus(parts.length > 1 ? parts[1] : "Error joining room", true);
                }
            });
        } else if (packet.getPacketType() == PacketType.GAME_START.getId()) {
            String payload = new String(packet.getPayload(), StandardCharsets.UTF_8);
            // payload format: ROLE:Riddler:EncryptedCharacter
            String[] parts = payload.split(":");
            if (parts.length == 3 && parts[0].equals("ROLE")) {
                String role = parts[1];
                String encryptedChar = parts[2];
                ClientContext.getInstance().setRole(role);
                ClientContext.getInstance().setEncryptedCharacterName(encryptedChar);

                Platform.runLater(() -> {
                    radarTimer.stop();
                    showStatus("Game starts! Your role: " + role, false);
                    statusLabel.setStyle("-fx-text-fill: #00d46a;"); // green for success
                    
                    try {
                        com.whoami.client.MainClient.setRoot("GameScreen");
                    } catch (Exception e) {
                        e.printStackTrace();
                        showStatus("Error loading GameScreen", true);
                    }
                });
            }
        } else if (packet.getPacketType() == PacketType.ROOM_LEAVE.getId()) {
            // Already handled on the UI side during handleBack, 
            // but we could log it or show a clean status here.
            System.out.println("Left room successfully.");
        }
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            showStatus("Disconnected from server", true);
            if (radarTimer != null) radarTimer.stop();
        });
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: #f85149;" : "-fx-text-fill: #8b949e;");
        statusLabel.setManaged(true);
        statusLabel.setVisible(true);
    }
}
