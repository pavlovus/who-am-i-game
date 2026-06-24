package com.whoami.client.ui;

import com.whoami.client.MainClient;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Modal dialog the riddler uses to choose the hidden character. Suggestions from
 * the character bank are shown as clickable chips; a free-text field lets the
 * riddler type a completely custom name. The dialog inherits the application's
 * Midnight Green theme so it matches the rest of the UI.
 *
 * <p>Must be called on the JavaFX application thread.</p>
 */
public final class CharacterChooser {

    private CharacterChooser() {
    }

    public static void prompt(List<String> suggestions, Consumer<String> onChosen) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Choose a character");
        dialog.initModality(Modality.APPLICATION_MODAL);

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("character-dialog");
        applyAppTheme(dialog, pane);

        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonType confirm = new ButtonType("Start game", ButtonBar.ButtonData.OK_DONE);
        pane.getButtonTypes().addAll(cancel, confirm);

        Label title = new Label("Who should they guess?");
        title.getStyleClass().add("h2");

        Label subtitle = new Label("Pick one of the suggestions below, or type your own character.");
        subtitle.getStyleClass().add("text-muted");
        subtitle.setWrapText(true);

        TextField field = new TextField();
        field.setPromptText("Type a character name...");
        field.getStyleClass().add("text-field");
        field.setMaxWidth(Double.MAX_VALUE);

        FlowPane chips = new FlowPane(10, 10);
        List<Button> chipButtons = new ArrayList<>();
        if (suggestions != null) {
            for (String suggestion : suggestions) {
                if (suggestion == null || suggestion.isBlank()) {
                    continue;
                }
                String name = suggestion.trim();
                Button chip = new Button(name);
                chip.getStyleClass().add("chip");
                chip.setFocusTraversable(false);
                chip.setOnAction(e -> {
                    field.setText(name);
                    field.positionCaret(name.length());
                    for (Button other : chipButtons) {
                        other.getStyleClass().remove("chip-selected");
                    }
                    chip.getStyleClass().add("chip-selected");
                });
                chipButtons.add(chip);
                chips.getChildren().add(chip);
            }
        }

        VBox content = new VBox(16, title, subtitle);
        if (!chips.getChildren().isEmpty()) {
            content.getChildren().add(chips);
        }
        content.getChildren().add(field);
        content.setPadding(new Insets(4, 4, 8, 4));
        content.setPrefWidth(440);
        pane.setContent(content);

        Node confirmButton = pane.lookupButton(confirm);
        confirmButton.getStyleClass().add("button-primary");
        confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (resolve(field).isEmpty()) {
                event.consume();
                field.requestFocus();
            }
        });

        Node cancelButton = pane.lookupButton(cancel);
        cancelButton.getStyleClass().add("button-ghost");

        dialog.setResultConverter(button -> button == confirm ? resolve(field) : null);

        Platform.runLater(field::requestFocus);

        dialog.showAndWait().ifPresent(name -> {
            if (!name.isEmpty()) {
                onChosen.accept(name);
            }
        });
    }

    private static void applyAppTheme(Dialog<?> dialog, DialogPane pane) {
        Scene appScene = MainClient.getScene();
        if (appScene == null) {
            return;
        }
        pane.getStylesheets().addAll(appScene.getStylesheets());
        if (appScene.getWindow() != null) {
            dialog.initOwner(appScene.getWindow());
        }
    }

    private static String resolve(TextField field) {
        String value = field.getText();
        return value == null ? "" : value.trim();
    }
}
