package com.example.typorax.component;

import com.example.typorax.model.TabInfo;
import com.example.typorax.util.ConfigLoader;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

public class CustomTabPane extends TabPane {

    private final StatusBar statusBar;

    public CustomTabPane(StatusBar statusBar) {
        this.statusBar = statusBar;
        // 添加双击事件监听器
        this.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() == 2 && isDoubleClickOnEmptyTabHeader(event)) {
                int newTabIndex = getTabs().size() + 1;
                createNewTab("新文件 " + newTabIndex, "", "", true);
            }
        });
    }

    public void createNewTab(String title, String content, String filePath) {
        createNewTab(title, content, filePath, false);
    }

    public void createNewTab(String title, String content, String filePath, boolean isTemp) {
        Tab tab = new Tab(title);
        BorderPane tabContent = new BorderPane();

        TextArea editArea = new TextArea(content);
        WebView preview = new WebView();

        boolean isMarkdown = filePath.toLowerCase().endsWith(".md");

        if (isMarkdown) {
            editArea.textProperty().addListener((observable, oldValue, newValue) -> {
                updatePreview(newValue, preview);
                TabInfo tabInfo = (TabInfo) tab.getUserData();
                tabInfo.setModified(!newValue.equals(tabInfo.getContent()));
                updateTabTitle(tab);
                statusBar.updateStatusBar(editArea); // 更新状态栏
            });

            editArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
                statusBar.updateStatusBar(editArea); // 更新状态栏
            });

            SplitPane splitPane = new SplitPane();
            splitPane.getItems().addAll(editArea, preview);
            splitPane.setDividerPositions(0.6); // 设置初始分割比例为60% 编辑区，40% 预览区

            // 设置预览区的最小宽度
            preview.setMinWidth(100);

            // 添加监听器来限制分隔符的移动
            splitPane.getDividers().get(0).positionProperty().addListener((obs, oldPos, newPos) -> {
                double totalWidth = splitPane.getWidth();
                double previewWidth = totalWidth * (1 - newPos.doubleValue());
                if (previewWidth < 100) {
                    splitPane.setDividerPosition(0, 1 - 100 / totalWidth);
                }
            });

            tabContent.setCenter(splitPane);

            updatePreview(content, preview);
        } else {
            editArea.textProperty().addListener((observable, oldValue, newValue) -> {
                TabInfo tabInfo = (TabInfo) tab.getUserData();
                tabInfo.setModified(!newValue.equals(tabInfo.getContent()));
                updateTabTitle(tab);
                statusBar.updateStatusBar(editArea); // 更新状态栏
            });

            editArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
                statusBar.updateStatusBar(editArea); // 更新状态栏
            });

            tabContent.setCenter(editArea);
        }

        TabInfo tabInfo = new TabInfo(title, content, filePath);
        tabInfo.setTemp(isTemp);
        tab.setContent(tabContent);
        tab.setUserData(tabInfo);
        updateTabTitle(tab);
        getTabs().add(tab);
        getSelectionModel().select(tab);

        statusBar.updateStatusBar(editArea); // 初始化状态栏

        tab.setOnCloseRequest(event -> {
            if (tabInfo.isModified()) {
                if (tabInfo.isTemp()) {
                    if (!showSaveAsConfirmation(tab)) {
                        event.consume();
                    }
                } else {
                    if (!showSaveConfirmation(tab)) {
                        event.consume();
                    }
                }
            }
        });
    }

    public void saveCurrentTab() {
        Tab currentTab = getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            TabInfo tabInfo = (TabInfo) currentTab.getUserData();
            if (tabInfo.isModified()) {
                String content = getTabContent(currentTab);
                if (content != null) {
                    try {
                        Files.write(Paths.get(tabInfo.getFilePath()), content.getBytes());
                        tabInfo.setModified(false);
                        updateTabTitle(currentTab);
                        // 可以在这里添加一个保存成功的提示，比如更新状态栏
                        statusBar.showMessage("文件已保存");
                    } catch (IOException e) {
                        e.printStackTrace();
                        // 显示错误消息
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("保存错误");
                        errorAlert.setHeaderText(null);
                        errorAlert.setContentText("保存文件时发生错误: " + e.getMessage());
                        errorAlert.showAndWait();
                    }
                }
            }
        }
    }

    private void updatePreview(String markdown, WebView preview) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        String html = renderer.render(parser.parse(markdown));
        preview.getEngine().loadContent(html);
    }

    private boolean showSaveConfirmation(Tab tab) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("保存");
        alert.setHeaderText(null); // 移除头部文本
        alert.setGraphic(null); // 移除图标

        String filePath = ((TabInfo) tab.getUserData()).getFilePath();
        alert.setContentText("保存文件 \"" + filePath + "\" ?");

        ButtonType buttonTypeYes = new ButtonType("是(Y)", ButtonBar.ButtonData.YES);
        ButtonType buttonTypeNo = new ButtonType("否(N)", ButtonBar.ButtonData.NO);
        ButtonType buttonTypeCancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo, buttonTypeCancel);

        // 设置对话框的宽度
        alert.getDialogPane().setMinWidth(420);

        // 自定义样式
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/custom-alert.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("custom-alert");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonTypeYes) {
            TabInfo tabInfo = (TabInfo) tab.getUserData();
            String content = getTabContent(tab);
            if (content != null) {
                try {
                    Files.write(Paths.get(tabInfo.getFilePath()), content.getBytes());
                    tabInfo.setModified(false);
                    tab.setText(tabInfo.getTitle());
                } catch (IOException e) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("保存错误");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText("保存文件时发生错误: " + e.getMessage());
                    errorAlert.showAndWait();
                }
            }
            return true;
        } else if (result.get() == buttonTypeNo) {
            return true;
        } else {
            return false;
        }
    }

    private boolean showSaveAsConfirmation(Tab tab) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("保存");
        alert.setHeaderText(null); // 移除头部文本
        alert.setGraphic(null); // 移除图标

        alert.setContentText("保存临时文件 \"" + ((TabInfo) tab.getUserData()).getTitle() + "\" ?");

        ButtonType buttonTypeYes = new ButtonType("是(Y)", ButtonBar.ButtonData.YES);
        ButtonType buttonTypeNo = new ButtonType("否(N)", ButtonBar.ButtonData.NO);
        ButtonType buttonTypeCancel = new ButtonType("取消", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonTypeYes, buttonTypeNo, buttonTypeCancel);

        // 设置对话框的宽度
        alert.getDialogPane().setMinWidth(420);

        // 自定义样式
        alert.getDialogPane().getStylesheets().add(getClass().getResource("/custom-alert.css").toExternalForm());
        alert.getDialogPane().getStyleClass().add("custom-alert");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == buttonTypeYes) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("保存文件");
            String lastOpenedDirectory = ConfigLoader.loadConfig("lastOpenedDirectory");
            if (lastOpenedDirectory != null && !lastOpenedDirectory.isEmpty()) {
                fileChooser.setInitialDirectory(new File(lastOpenedDirectory));
            }
            File file = fileChooser.showSaveDialog(getScene().getWindow());
            if (file != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(getTabContent(tab));
                    TabInfo tabInfo = (TabInfo) tab.getUserData();
                    tabInfo.setFilePath(file.getAbsolutePath());
                    tabInfo.setModified(false);
                    tabInfo.setTemp(false);
                    tab.setText(file.getName());
                    ConfigLoader.loadConfig().setProperty("lastOpenedDirectory", file.getParent());
                } catch (IOException e) {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("保存错误");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText("保存文件时发生错误: " + e.getMessage());
                    errorAlert.showAndWait();
                }
            }
            return true;
        } else if (result.get() == buttonTypeNo) {
            return true;
        } else {
            return false;
        }
    }

    private void updateTabTitle(Tab tab) {
        TabInfo tabInfo = (TabInfo) tab.getUserData();
        String title = tabInfo.getTitle();
        if (tabInfo.isModified()) {
            tab.setText(title + " ⚪");
        } else {
            tab.setText(title);
        }
    }

    public String getTabContent(Tab tab) {
        if (tab.getContent() instanceof BorderPane) {
            BorderPane tabContent = (BorderPane) tab.getContent();
            if (tabContent.getCenter() instanceof SplitPane) {
                // Markdown file
                SplitPane splitPane = (SplitPane) tabContent.getCenter();
                TextArea markdownArea = (TextArea) splitPane.getItems().get(0);
                return markdownArea.getText();
            } else if (tabContent.getCenter() instanceof TextArea) {
                // Non-Markdown file
                TextArea textArea = (TextArea) tabContent.getCenter();
                return textArea.getText();
            }
        }
        return null;
    }

    private boolean isDoubleClickOnEmptyTabHeader(MouseEvent event) {
        // 检查双击是否发生在标签头部的空白处
        for (Tab tab : getTabs()) {
            if (tab.getGraphic() != null && tab.getGraphic().getBoundsInParent().contains(event.getX(), event.getY())) {
                return false;
            }
        }
        return true;
    }
}
