package com.example.typorax.manager;

import com.example.typorax.component.CustomTabPane;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class QueryAndReplaceManager {

    private static Stage searchReplaceStage;
    private static CustomTabPane tabPane;

    public static void createSearchReplaceDialog(Stage parentStage, CustomTabPane tabPane) {
        QueryAndReplaceManager.tabPane = tabPane;
        searchReplaceStage = new Stage();
        searchReplaceStage.initModality(Modality.NONE);
        searchReplaceStage.initOwner(parentStage);
        searchReplaceStage.setTitle("查找和替换");

        // 设置对话框的最大化和可调整大小属性
        searchReplaceStage.setResizable(false); // 禁止调整大小
        searchReplaceStage.setWidth(300); // 设置宽度为600px

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        TextField searchField = new TextField();
        searchField.setPromptText("查找");
        TextField replaceField = new TextField();
        replaceField.setPromptText("替换为");

        Button findButton = new Button("查找下一个");
        Button replaceButton = new Button("替换");
        Button replaceAllButton = new Button("全部替换");

        findButton.setOnAction(e -> findNext(searchField.getText()));
        replaceButton.setOnAction(e -> replace(searchField.getText(), replaceField.getText()));
        replaceAllButton.setOnAction(e -> replaceAll(searchField.getText(), replaceField.getText()));

        HBox buttonBox = new HBox(10, findButton, replaceButton, replaceAllButton);
        layout.getChildren().addAll(searchField, replaceField, buttonBox);

        Scene scene = new Scene(layout);
        scene.getStylesheets().add(QueryAndReplaceManager.class.getResource("/application.css").toExternalForm());
        searchReplaceStage.setScene(scene);
    }

    public static void showSearchReplaceDialog() {
        if (!searchReplaceStage.isShowing()) {
            searchReplaceStage.show();
        } else {
            searchReplaceStage.requestFocus();
        }
    }

    private static void findNext(String searchText) {
        TextArea currentTextArea = getCurrentTextArea();
        if (currentTextArea != null) {
            int caretPosition = currentTextArea.getCaretPosition();
            String text = currentTextArea.getText();
            int foundIndex = text.indexOf(searchText, caretPosition);
            if (foundIndex != -1) {
                currentTextArea.selectRange(foundIndex, foundIndex + searchText.length());
            } else {
                // 如果没有找到，从头开始搜索
                foundIndex = text.indexOf(searchText);
                if (foundIndex != -1) {
                    currentTextArea.selectRange(foundIndex, foundIndex + searchText.length());
                } else {
                    showAlert("未找到", "找不到 \"" + searchText + "\"");
                }
            }
        }
    }

    private static void replace(String searchText, String replaceText) {
        TextArea currentTextArea = getCurrentTextArea();
        if (currentTextArea != null) {
            if (currentTextArea.getSelectedText().equals(searchText)) {
                currentTextArea.replaceSelection(replaceText);
                findNext(searchText);
            } else {
                findNext(searchText);
            }
        }
    }

    private static void replaceAll(String searchText, String replaceText) {
        TextArea currentTextArea = getCurrentTextArea();
        if (currentTextArea != null) {
            String text = currentTextArea.getText();
            String newText = text.replace(searchText, replaceText);
            currentTextArea.setText(newText);
        }
    }

    private static void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private static TextArea getCurrentTextArea() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            if (currentTab.getContent() instanceof BorderPane) {
                BorderPane tabContent = (BorderPane) currentTab.getContent();
                if (tabContent.getCenter() instanceof SplitPane) {
                    SplitPane splitPane = (SplitPane) tabContent.getCenter();
                    return (TextArea) splitPane.getItems().get(0);
                } else if (tabContent.getCenter() instanceof TextArea) {
                    return (TextArea) tabContent.getCenter();
                }
            }
        }
        return null;
    }
}
