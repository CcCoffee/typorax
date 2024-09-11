package com.example.typorax.component;

import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import com.example.typorax.util.AIRewriteUtil;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import javafx.stage.Popup;
import javafx.geometry.Insets;
import javafx.stage.Window;

import java.util.Collection;
import java.util.Collections;

public class CustomContextMenu extends ContextMenu {

    private final MenuItem copyItem;
    private final MenuItem pasteItem;
    private final MenuItem selectAllItem;
    private final MenuItem aiRewriteItem;
    private final StatusBar statusBar;

    public CustomContextMenu(TextArea editArea, StatusBar statusBar) {
        this.statusBar = statusBar;
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
            return;
        }

        int start = editArea.getSelection().getStart();
        int end = editArea.getSelection().getEnd();

        editArea.setDisable(true);
        String originalText = selectedText;
        statusBar.showAIRewriteStatus(true);

        AIRewriteUtil.rewriteText(selectedText).thenAccept(rewrittenText -> {
            Platform.runLater(() -> {
                showDiffWithButtons(editArea, originalText, rewrittenText, start, end);
                editArea.setDisable(false);
                statusBar.showAIRewriteStatus(false);
            });
        }).exceptionally(e -> {
            Platform.runLater(() -> {
                statusBar.showMessage("AI重写失败: " + e.getMessage());
                editArea.setDisable(false);
            });
            return null;
        });
    }

    private void showDiffWithButtons(TextArea editArea, String originalText, String rewrittenText, int start, int end) {
        CodeArea codeArea = new CodeArea();
        codeArea.setStyle("-fx-font-family: monospace;");
        codeArea.replaceText(rewrittenText);
        StyleSpans<Collection<String>> highlighting = computeHighlighting(originalText, rewrittenText);
        codeArea.setStyleSpans(0, highlighting);

        Button applyButton = new Button("应用");
        Button undoButton = new Button("撤销");

        HBox buttonBox = new HBox(10, applyButton, undoButton);
        VBox container = new VBox(10, buttonBox, codeArea);
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: white; -fx-border-color: gray; -fx-border-width: 1;");

        Popup popup = new Popup();
        popup.getContent().add(container);
        popup.setAutoHide(true);

        applyButton.setOnAction(event -> {
            editArea.replaceText(start, end, rewrittenText);
            popup.hide();
        });

        undoButton.setOnAction(event -> {
            editArea.replaceText(start, end, originalText);
            popup.hide();
        });

        // 设置 Popup 的大小
        container.setPrefWidth(editArea.getWidth() * 0.8);
        container.setPrefHeight(editArea.getHeight() * 0.8);

        // 显示 Popup
        Window window = editArea.getScene().getWindow();
        popup.show(window,
                window.getX() + (window.getWidth() - container.getPrefWidth()) / 2,
                window.getY() + (window.getHeight() - container.getPrefHeight()) / 2);
    }

    private StyleSpans<Collection<String>> computeHighlighting(String originalText, String rewrittenText) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        String[] originalWords = originalText.split("\\s+");
        String[] rewrittenWords = rewrittenText.split("\\s+");

        int currentIndex = 0;
        for (int i = 0; i < rewrittenWords.length; i++) {
            String word = rewrittenWords[i];
            if (i < originalWords.length && !word.equals(originalWords[i])) {
                spansBuilder.add(Collections.singleton("changed"), word.length());
            } else if (i >= originalWords.length) {
                spansBuilder.add(Collections.singleton("added"), word.length());
            } else {
                spansBuilder.add(Collections.emptyList(), word.length());
            }
            currentIndex += word.length();
            
            // 为单词之间的空格添加样式
            if (currentIndex < rewrittenText.length()) {
                spansBuilder.add(Collections.emptyList(), 1);
                currentIndex++;
            }
        }

        // 如果还有剩余文本，将其标记为"added"
        if (currentIndex < rewrittenText.length()) {
            spansBuilder.add(Collections.singleton("added"), rewrittenText.length() - currentIndex);
        }

        return spansBuilder.create();
    }
}