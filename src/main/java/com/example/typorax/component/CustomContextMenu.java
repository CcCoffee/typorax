package com.example.typorax.component;

import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import com.example.typorax.util.AIRewriteUtil;
import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import javafx.stage.Popup;
import javafx.stage.Window;
import org.fxmisc.flowless.VirtualizedScrollPane;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;

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

        aiRewriteItem = new MenuItem("AI修正");
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
                statusBar.showMessage("AI修正失败: " + e.getMessage());
                editArea.setDisable(false);
            });
            return null;
        });
    }

    private void showDiffWithButtons(TextArea editArea, String originalText, String rewrittenText, int start, int end) {
        CodeArea codeArea = new CodeArea();
        codeArea.getStyleClass().add("code-area");
        codeArea.replaceText(rewrittenText);
        StyleSpans<Collection<String>> highlighting = computeHighlighting(originalText, rewrittenText);
        codeArea.setStyleSpans(0, highlighting);
        
        codeArea.setWrapText(true);

        VirtualizedScrollPane<CodeArea> scrollPane = new VirtualizedScrollPane<>(codeArea);

        Button applyButton = new Button("应用");
        Button undoButton = new Button("撤销");
        undoButton.getStyleClass().add("cancel");

        HBox buttonBox = new HBox(10, applyButton, undoButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        Label headerLabel = new Label("AI 修正结果");
        headerLabel.getStyleClass().add("header-label");
        
        HBox header = new HBox(headerLabel);
        header.getStyleClass().add("diff-header");
        header.setAlignment(Pos.CENTER_LEFT);
        
        VBox content = new VBox(scrollPane);
        content.getStyleClass().add("diff-content");
        
        HBox footer = new HBox(buttonBox);
        footer.getStyleClass().add("diff-footer");
        
        VBox container = new VBox(header, content, footer);
        container.getStyleClass().add("diff-container");

        VBox.setVgrow(content, Priority.ALWAYS);

        Popup popup = new Popup();
        popup.getContent().add(container);
        popup.setAutoHide(true);

        String cssPath = getClass().getResource("/context-menu.css").toExternalForm();
        container.getStylesheets().add(cssPath);

        // 设置拖动功能
        final Delta dragDelta = new Delta();
        header.setOnMousePressed(mouseEvent -> {
            dragDelta.x = popup.getX() - mouseEvent.getScreenX();
            dragDelta.y = popup.getY() - mouseEvent.getScreenY();
        });
        header.setOnMouseDragged(mouseEvent -> {
            popup.setX(mouseEvent.getScreenX() + dragDelta.x);
            popup.setY(mouseEvent.getScreenY() + dragDelta.y);
        });

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

        // 绑定codeArea的宽度到scrollPane的宽度
        codeArea.prefWidthProperty().bind(scrollPane.widthProperty());

        // 显示 Popup
        Window window = editArea.getScene().getWindow();
        popup.show(window,
                window.getX() + (window.getWidth() - container.getPrefWidth()) / 2,
                window.getY() + (window.getHeight() - container.getPrefHeight()) / 2);
    }

    private StyleSpans<Collection<String>> computeHighlighting(String originalText, String rewrittenText) {
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        
        int originalIndex = 0;
        int rewrittenIndex = 0;
        
        while (rewrittenIndex < rewrittenText.length()) {
            if (originalIndex < originalText.length() && 
                rewrittenText.charAt(rewrittenIndex) == originalText.charAt(originalIndex)) {
                // 字符相同，不需要高亮
                spansBuilder.add(Collections.emptyList(), 1);
                rewrittenIndex++;
                originalIndex++;
            } else {
                // 字符不同，需要高亮
                spansBuilder.add(Collections.singleton("changed"), 1);
                rewrittenIndex++;
                if (originalIndex < originalText.length()) {
                    originalIndex++;
                }
            }
        }
        
        return spansBuilder.create();
    }

    // 用于跟踪拖动的辅助类
    private static class Delta {
        double x, y;
    }
}