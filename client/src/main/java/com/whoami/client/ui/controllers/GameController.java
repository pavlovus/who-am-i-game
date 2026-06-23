package com.whoami.client.ui.controllers;

import com.whoami.client.MainClient;
import com.whoami.client.network.PacketListener;
import com.whoami.client.state.ClientContext;
import com.whoami.protocol.crypto.AESDecrypter;
import com.whoami.protocol.packets.Packet;
import com.whoami.protocol.packets.PacketType;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.nio.charset.StandardCharsets;

public class GameController implements PacketListener {

    @FXML private Label roomLabel;
    @FXML private Label roleLabel;
    @FXML private Label statusLabel;
    
    @FXML private VBox transcriptBox;
    @FXML private ScrollPane transcriptScrollPane;
    
    // Guesser UI
    @FXML private VBox guesserBox;
    @FXML private Label questionsLeftLabel;
    @FXML private Label guessesLeftLabel;
    @FXML private TextField questionField;
    @FXML private TextField guessField;
    
    // Riddler UI
    @FXML private VBox riddlerBox;
    @FXML private Label characterNameLabel;
    @FXML private VBox answerBox;
    @FXML private Label pendingQuestionLabel;
    @FXML private Label riddlerWaitingLabel;

    @FXML
    public void initialize() {
        ClientContext ctx = ClientContext.getInstance();
        ctx.getServerConnection().setPacketListener(this);
        
        roomLabel.setText("Room: " + ctx.getCurrentRoomCode());
        roleLabel.setText("Role: " + ctx.getRole());
        
        if ("Riddler".equalsIgnoreCase(ctx.getRole())) {
            guesserBox.setVisible(false);
            guesserBox.setManaged(false);
            riddlerBox.setVisible(true);
            riddlerBox.setManaged(true);
            
            decryptCharacterName(ctx.getEncryptedCharacterName());
        } else {
            riddlerBox.setVisible(false);
            riddlerBox.setManaged(false);
            guesserBox.setVisible(true);
            guesserBox.setManaged(true);
        }
        
        // Auto-scroll transcript
        transcriptBox.heightProperty().addListener((obs, oldVal, newVal) -> 
            transcriptScrollPane.setVvalue((Double) newVal)
        );
    }
    
    private void decryptCharacterName(String hexEncrypted) {
        if (hexEncrypted == null || hexEncrypted.isEmpty()) return;
        try {
            AESDecrypter decrypter = new AESDecrypter();
            byte[] encBytes = hexStringToByteArray(hexEncrypted);
            byte[] decrypted = decrypter.decrypt(encBytes);
            characterNameLabel.setText(new String(decrypted, StandardCharsets.UTF_8));
        } catch (Exception e) {
            characterNameLabel.setText("ERROR DECRYPTING");
            e.printStackTrace();
        }
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    // --- Guesser Actions ---

    @FXML
    public void handleSendQuestion() {
        String text = questionField.getText().trim();
        if (text.isEmpty()) return;
        
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.QUESTION.getId(), payload.length, (short)0, payload);
        ClientContext.getInstance().getServerConnection().sendPacket(packet);
        
        questionField.clear();
        questionField.setDisable(true); // Disable until answered
    }

    @FXML
    public void handleSendGuess() {
        String text = guessField.getText().trim();
        if (text.isEmpty()) return;
        
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);
        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.GUESS.getId(), payload.length, (short)0, payload);
        ClientContext.getInstance().getServerConnection().sendPacket(packet);
        
        guessField.clear();
    }

    // --- Riddler Actions ---

    @FXML
    public void handleAnswerYes() {
        sendAnswer("YES");
    }

    @FXML
    public void handleAnswerNo() {
        sendAnswer("NO");
    }

    @FXML
    public void handleAnswerPartially() {
        sendAnswer("PARTIALLY");
    }
    
    private void sendAnswer(String answer) {
        byte[] payload = answer.getBytes(StandardCharsets.UTF_8);
        Packet packet = new Packet(Packet.MAGIC_BYTE, 0, PacketType.ANSWER.getId(), payload.length, (short)0, payload);
        ClientContext.getInstance().getServerConnection().sendPacket(packet);
        
        answerBox.setVisible(false);
        riddlerWaitingLabel.setVisible(true);
    }

    // --- Network Handlers ---

    @Override
    public void onPacketReceived(Packet packet) {
        Platform.runLater(() -> {
            if (packet.getPacketType() == PacketType.GAME_STATE.getId()) {
                handleGameState(packet);
            } else if (packet.getPacketType() == PacketType.GAME_OVER.getId()) {
                handleGameOver(packet);
            } else if (packet.getPacketType() == PacketType.ERROR.getId()) {
                String msg = new String(packet.getPayload(), StandardCharsets.UTF_8);
                showError("Game Error: " + msg);
            }
        });
    }

    private void handleGameState(Packet packet) {
        String payload = new String(packet.getPayload(), StandardCharsets.UTF_8);
        // Format: Q_LEFT=N;G_LEFT=M;PENDING=txt;LAST_Q=txt;LAST_A=txt
        int qLeft = 0;
        int gLeft = 0;
        String pending = "";
        String lastQ = "";
        String lastA = "";
        
        for (String part : payload.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length < 2) continue;
            switch(kv[0]) {
                case "Q_LEFT" -> qLeft = Integer.parseInt(kv[1]);
                case "G_LEFT" -> gLeft = Integer.parseInt(kv[1]);
                case "PENDING" -> pending = kv[1];
                case "LAST_Q" -> lastQ = kv[1];
                case "LAST_A" -> lastA = kv[1];
            }
        }
        
        // Update Guesser Counters
        questionsLeftLabel.setText("Questions left: " + qLeft);
        guessesLeftLabel.setText("Guesses left: " + gLeft);
        
        // Handle Transcript update (only add if we have a new answered question)
        if (!lastQ.isEmpty() && !lastA.isEmpty()) {
            addTranscriptEntry(lastQ, lastA);
        }
        
        // Handle Pending state
        if (!pending.isEmpty()) {
            // Riddler needs to answer
            if ("Riddler".equalsIgnoreCase(ClientContext.getInstance().getRole())) {
                pendingQuestionLabel.setText(pending);
                riddlerWaitingLabel.setVisible(false);
                answerBox.setVisible(true);
            } else {
                // Guesser waits
                questionField.setDisable(true);
                questionField.setPromptText("Waiting for answer...");
            }
        } else {
            // No pending question, guesser can ask again
            if ("Guesser".equalsIgnoreCase(ClientContext.getInstance().getRole())) {
                questionField.setDisable(false);
                questionField.setPromptText("Am I an animal?");
            }
        }
    }
    
    private void addTranscriptEntry(String q, String a) {
        // Prevent duplicate adds by checking the last entry
        int size = transcriptBox.getChildren().size();
        String entryText = q + " \u2192 " + a;
        if (size > 0) {
            HBox last = (HBox) transcriptBox.getChildren().get(size - 1);
            Label l = (Label) last.getChildren().get(0);
            if (l.getText().equals(entryText)) {
                return; // Already added
            }
        }

        Label label = new Label(entryText);
        label.setWrapText(true);
        label.setStyle("-fx-background-color: #1a2e1f; -fx-padding: 8 12; -fx-background-radius: 8;");
        
        HBox row = new HBox(label);
        row.setAlignment(Pos.CENTER_LEFT);
        
        // Color code answers
        if (a.equals("YES")) {
            label.setStyle(label.getStyle() + " -fx-border-color: #00d46a; -fx-border-radius: 8;");
        } else if (a.equals("NO")) {
            label.setStyle(label.getStyle() + " -fx-border-color: #f85149; -fx-border-radius: 8;");
        } else {
            label.setStyle(label.getStyle() + " -fx-border-color: #d29922; -fx-border-radius: 8;");
        }

        transcriptBox.getChildren().add(row);
    }

    private void handleGameOver(Packet packet) {
        String payload = new String(packet.getPayload(), StandardCharsets.UTF_8);
        if (payload.equals("OPPONENT_LEFT")) {
            showError("Opponent disconnected!");
            // We could redirect to main menu here automatically, but PostGameScreen can handle it.
        }
        
        // Format: WINNER=ROLE;CHARACTER=Name;QUESTIONS=N
        String winner = "Unknown";
        String character = "???";
        int questions = 0;
        
        for (String part : payload.split(";")) {
            String[] kv = part.split("=", 2);
            if (kv.length < 2) continue;
            switch(kv[0]) {
                case "WINNER" -> winner = kv[1];
                case "CHARACTER" -> character = kv[1];
                case "QUESTIONS" -> questions = Integer.parseInt(kv[1]);
            }
        }
        
        ClientContext ctx = ClientContext.getInstance();
        ctx.setWinnerRole(winner);
        ctx.setGameCharacterName(character);
        ctx.setQuestionsAsked(questions);
        
        try {
            MainClient.setRoot("PostGameScreen");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            showError("Connection lost! Reconnecting...");
        });
    }
    
    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
