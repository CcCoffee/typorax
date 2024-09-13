package com.example.typorax.component;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class StatusBar extends HBox {
    private final Label statusBarLineCol = new Label("行: 1 列: 1");
    private final Label statusBarCharCount = new Label("0 个字符");
    private final Label statusBarEOL = new Label("Windows (CRLF)");
    private final Label statusBarEncoding = new Label("UTF-8");
    private final Label messageLabel = new Label();

    private final StringProperty lineColProperty = new SimpleStringProperty("行: 1 列: 1");
    private final StringProperty charCountProperty = new SimpleStringProperty("0 个字符");
    private final StringProperty eolProperty = new SimpleStringProperty("Windows (CRLF)");
    private final StringProperty encodingProperty = new SimpleStringProperty("UTF-8");

    public StatusBar() {
        // 绑定属性到标签
        statusBarLineCol.textProperty().bind(lineColProperty);
        statusBarCharCount.textProperty().bind(charCountProperty);
        statusBarEOL.textProperty().bind(eolProperty);
        statusBarEncoding.textProperty().bind(encodingProperty);

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

        int rowNum = (text.isEmpty()) ? 1 : text.substring(0, Math.min(text.length(), caretPosition)).split("\n", -1).length;
        int lastNewlineIndex = text.lastIndexOf('\n', caretPosition - 1);
        int colNum = (lastNewlineIndex == -1) ? caretPosition + 1 : caretPosition - lastNewlineIndex;

        int charCount = text.length();
        String eol = text.contains("\r\n") ? "Windows (CRLF)" : "Unix (LF)";
        String encoding = "UTF-8"; // 假设文件编码为UTF-8

        lineColProperty.set("行: " + rowNum + " 列: " + colNum);
        charCountProperty.set(charCount + " 个字符");
        eolProperty.set(eol);
        encodingProperty.set(encoding);
    }

    private void resetStatusBar() {
        lineColProperty.set("行: 1 列: 1");
        charCountProperty.set("0 个字符");
        eolProperty.set("Windows (CRLF)");
        encodingProperty.set("UTF-8");
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
        }
    }

    public void showAIRewriteStatus(boolean isRewriting) {
        if (isRewriting) {
            showMessage("正在进行AI修正，请稍候...", 0);
        } else {
            showMessage("AI修正完成", 3000);
        }
    }
}