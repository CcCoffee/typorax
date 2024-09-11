package com.example.typorax.component;

import javafx.scene.control.*;
import javafx.scene.input.Clipboard;

public class CustomContextMenu extends ContextMenu {

    private final MenuItem copyItem;
    private final MenuItem pasteItem;
    private final MenuItem selectAllItem;
    private final MenuItem aiRewriteItem;

    public CustomContextMenu(TextArea editArea) {
        copyItem = new MenuItem("复制");
        copyItem.setOnAction(event -> editArea.copy());

        pasteItem = new MenuItem("粘贴");
        pasteItem.setOnAction(event -> editArea.paste());
        pasteItem.setDisable(true);

        selectAllItem = new MenuItem("全选");
        selectAllItem.setOnAction(event -> editArea.selectAll());

        aiRewriteItem = new MenuItem("AI重写");
        aiRewriteItem.setOnAction(event -> handleAIRewrite(editArea));

        getItems().addAll(copyItem, pasteItem, selectAllItem, new SeparatorMenuItem(), aiRewriteItem);

        editArea.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                pasteItem.setDisable(!Clipboard.getSystemClipboard().hasString());
            }
        });
    }

    private void handleAIRewrite(TextArea editArea) {
        String selectedText = editArea.getSelectedText();
        if (selectedText.isEmpty()) {
            selectedText = editArea.getText();
        }

        String rewrittenText = "AI重写后的内容: " + selectedText;

        if (editArea.getSelectedText().isEmpty()) {
            editArea.setText(rewrittenText);
        } else {
            editArea.replaceSelection(rewrittenText);
        }
    }
}