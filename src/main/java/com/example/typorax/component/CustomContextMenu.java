package com.example.typorax.component;

import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import com.example.typorax.util.AIRewriteUtil;
import javafx.application.Platform;

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

        MenuItem undoItem = new MenuItem("撤销");
        undoItem.setOnAction(event -> editArea.undo());

        MenuItem redoItem = new MenuItem("重做");
        redoItem.setOnAction(event -> editArea.redo());

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

        // 显示加载提示
        editArea.setDisable(true);
        String originalText = selectedText;
        editArea.setText("正在进行AI重写，请稍候...");

        AIRewriteUtil.rewriteText(selectedText).thenAccept(rewrittenText -> {
            Platform.runLater(() -> {
                if (editArea.getSelectedText().isEmpty()) {
                    editArea.setText(rewrittenText);
                } else {
                    editArea.replaceSelection(rewrittenText);
                }
                editArea.setDisable(false);
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                editArea.setText("AI重写失败: " + e.getMessage() + "\n原文本：\n" + originalText);
                editArea.setDisable(false);
            });
            return null;
        });
    }
}