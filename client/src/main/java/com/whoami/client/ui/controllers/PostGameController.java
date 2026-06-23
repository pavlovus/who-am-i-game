package com.whoami.client.ui.controllers;

import com.whoami.client.MainClient;
import com.whoami.client.network.PacketListener;
import com.whoami.client.state.ClientContext;
import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.nio.charset.StandardCharsets;

public class PostGameController implements PacketListener {

    @FXML private Label winnerLabel;
    @FXML private Label characterLabel;
    @FXML private Label questionsLabel;
    @FXML private Label statusLabel;
    @FXML private Button rematchBtn;

    @FXML
    public void initialize() {
        ClientContext ctx = ClientContext.getInstance();
        ctx.getServerConnection().setPacketListener(this);
        
        winnerLabel.setText(ctx.getWinnerRole());
        if ("Riddler".equalsIgnoreCase(ctx.getWinnerRole())) {
            winnerLabel.setStyle("-fx-text-fill: #f85149;"); // red if riddler won (guesser failed)
        } else {
            winnerLabel.setStyle("-fx-text-fill: #00d46a;"); // green if guesser won
        }
        
        characterLabel.setText(ctx.getGameCharacterName());
        questionsLabel.setText("Questions used: " + ctx.getQuestionsAsked());
    }

    @FXML
    public void handleRematch() {
        rematchBtn.setDisable(true);
        rematchBtn.setText("Waiting...");
        
        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.REMATCH.getId(), 0, (short)0, new byte[0]);
        ClientContext.getInstance().getServerConnection().sendPacket(packet);
        showStatus("Rematch requested...", false);
    }

    @FXML
    public void handleLeave() {
        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.ROOM_LEAVE.getId(), 0, (short)0, new byte[0]);
        ClientContext.getInstance().getServerConnection().sendPacket(packet);
        
        try {
            MainClient.setRoot("MainMenuScreen");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPacketReceived(Packet packet) {
        Platform.runLater(() -> {
            if (packet.getPacketType() == PacketType.GAME_OVER.getId()) {
                String payload = new String(packet.getPayload(), StandardCharsets.UTF_8);
                if (payload.equals("OPPONENT_LEFT")) {
                    showStatus("Opponent has left the room.", true);
                    rematchBtn.setDisable(true);
                }
            } else if (packet.getPacketType() == PacketType.REMATCH.getId()) {
                String payload = new String(packet.getPayload(), StandardCharsets.UTF_8);
                if (payload.equals("RESTARTED")) {
                    // Game is restarting, but wait for GAME_START
                    showStatus("Rematch accepted! Starting...", false);
                } else if (payload.startsWith("ERROR")) {
                    showStatus(payload, true);
                    rematchBtn.setDisable(false);
                    rematchBtn.setText("Request Rematch");
                }
            } else if (packet.getPacketType() == PacketType.GAME_START.getId()) {
                // Same logic as MainMenuController
                String payload = new String(packet.getPayload(), StandardCharsets.UTF_8);
                String[] parts = payload.split(":");
                if (parts.length == 3 && parts[0].equals("ROLE")) {
                    String role = parts[1];
                    String encryptedChar = parts[2];
                    ClientContext.getInstance().setRole(role);
                    ClientContext.getInstance().setEncryptedCharacterName(encryptedChar);

                    try {
                        MainClient.setRoot("GameScreen");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            showStatus("Disconnected from server", true);
            rematchBtn.setDisable(true);
        });
    }
    
    private void showStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle(isError ? "-fx-text-fill: #f85149;" : "-fx-text-fill: #8b949e;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
