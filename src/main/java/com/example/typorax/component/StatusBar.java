package com.example.typorax.component;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class StatusBar extends HBox {
    private final Label statusBarLineCol;
    private final Label statusBarCharCount;
    private final Label statusBarEOL;
    private final Label statusBarEncoding;
    private final Label messageLabel;

    public StatusBar() {
        statusBarLineCol = new Label("行: 1 列: 1");
        statusBarCharCount = new Label("0 个字符");
        statusBarEOL = new Label("Windows (CRLF)");
        statusBarEncoding = new Label("UTF-8");
        messageLabel = new Label();

        // 创建一个空的Region来占据中间的空间
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        this.getChildren().addAll(
            statusBarLineCol,
            new Separator(),
            statusBarCharCount,
            new Separator(),
            statusBarEOL,
            new Separator(),
            statusBarEncoding,
            spacer,
            messageLabel
        );

        this.setAlignment(Pos.CENTER_LEFT);
        this.setStyle("-fx-padding: 5; -fx-background-color: #e0e0e0; -fx-border-color: #cccccc; -fx-border-width: 1px 0 0 0;");
    }

    public void updateStatusBar(TextArea textArea) {
        if (textArea == null || textArea.getText() == null) {
            resetStatusBar();
            return;
        }

        String text = textArea.getText();
        int caretPosition = textArea.getCaretPosition();
        
        // 计算行号和列号
        int rowNum = (text.isEmpty()) ? 1 : text.substring(0, Math.min(text.length(), caretPosition)).split("\n", -1).length;
        int lastNewlineIndex = text.lastIndexOf('\n', caretPosition - 1);
        int colNum = (lastNewlineIndex == -1) ? caretPosition + 1 : caretPosition - lastNewlineIndex;

        int charCount = text.length();
        String eol = text.contains("\r\n") ? "Windows (CRLF)" : "Unix (LF)";
        String encoding = "UTF-8"; // 假设文件编码为UTF-8

        statusBarLineCol.setText("行: " + rowNum + " 列: " + colNum);
        statusBarCharCount.setText(charCount + " 个字符");
        statusBarEOL.setText(eol);
        statusBarEncoding.setText(encoding);
    }

    private void resetStatusBar() {
        statusBarLineCol.setText("行: 1 列: 1");
        statusBarCharCount.setText("0 个字符");
        statusBarEOL.setText("Windows (CRLF)");
        statusBarEncoding.setText("UTF-8");
    }

    public void showMessage(String message) {
        showMessage(message, 3000);
    }

    public void showMessage(String message, long timeout) {
        messageLabel.setText(message);
        if (timeout > 0) {
            new Thread(() -> {
            try {
                Thread.sleep(timeout);
            } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                javafx.application.Platform.runLater(() -> messageLabel.setText(""));
            }).start();
        } else {
            messageLabel.setText(message);
        }
    }

    public void showAIRewriteStatus(boolean isRewriting) {
        if (isRewriting) {
            showMessage("正在进行AI重写，请稍候...", 0);
        } else {
            showMessage("AI重写完成", 3000);
        }
    }
}