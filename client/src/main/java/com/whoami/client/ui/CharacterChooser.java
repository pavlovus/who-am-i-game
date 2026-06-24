package com.whoami.client.ui;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;

import java.util.List;
import java.util.function.Consumer;

/**
 * Modal dialog the riddler uses to choose the hidden character. The editable
 * combo box gives both options in a single control: pick one of the suggestions
 * coming from the character bank, or type a completely custom name.
 *
 * <p>Must be called on the JavaFX application thread.</p>
 */
public final class CharacterChooser {

    private CharacterChooser() {
    }

    public static void prompt(List<String> suggestions, Consumer<String> onChosen) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Choose a character");
        dialog.setHeaderText("Pick one from the bank or type your own");

        ButtonType confirm = new ButtonType("Start game", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(confirm);

        ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPromptText("Type a character name...");
        if (suggestions != null) {
            for (String s : suggestions) {
                if (s != null && !s.isBlank()) {
                    combo.getItems().add(s.trim());
                }
            }
        }
        if (!combo.getItems().isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }
        combo.setMaxWidth(Double.MAX_VALUE);
        dialog.getDialogPane().setContent(combo);

        // Keep the dialog open until a non-empty name is provided.
        Node confirmButton = dialog.getDialogPane().lookupButton(confirm);
        confirmButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (resolve(combo).isEmpty()) {
                event.consume();
            }
        });

        dialog.setResultConverter(button -> button == confirm ? resolve(combo) : null);

        dialog.showAndWait().ifPresent(name -> {
            if (!name.isEmpty()) {
                onChosen.accept(name);
            }
        });
    }

    private static String resolve(ComboBox<String> combo) {
        String value = combo.getEditor().getText();
        if (value == null || value.isBlank()) {
            value = combo.getValue();
        }
        return value == null ? "" : value.trim();
    }
}
