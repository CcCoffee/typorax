package com.example.typorax.manager;

import com.example.typorax.component.CustomContextMenu;
import com.example.typorax.component.CustomTabPane;
import com.example.typorax.component.StatusBar;
import com.example.typorax.model.TabInfo;
import com.example.typorax.util.ConfigLoader;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TabManager {

    // getLogger
    private Logger logger = LoggerFactory.getLogger(TabManager.class);

    private final CustomTabPane tabPane;
    private final StatusBar statusBar;

    public TabManager(CustomTabPane tabPane, StatusBar statusBar) {
        this.tabPane = tabPane;
        this.statusBar = statusBar;
    }

    public void createNewTemporaryTab() {
        int newTabIndex = getNextTempFileIndex();
        logger.info("Creating new temporary tab with index: " + newTabIndex);
        createNewTab("新文件 " + newTabIndex, "", "", true, false);
    }

    private int getNextTempFileIndex() {
        int maxIndex = 0;
        for (Tab tab : tabPane.getTabs()) {
            String title = tab.getText();
            if (title.startsWith("新文件 ")) {
                try {
                    // replace " ⚪" in the last of title
                    title = title.replace(" ⚪", "");
                    int index = Integer.parseInt(title.substring(4).trim());
                    maxIndex = Math.max(maxIndex, index);
                } catch (NumberFormatException e) {
                    // 忽略无法解析的标题
                }
            }
        }
        logger.info("Next temporary file index: " + (maxIndex + 1));
        return maxIndex + 1;
    }

    public void createNewTab(String title, String content, String filePath){
        this.createNewTab(title,content, filePath,false,false);
    }

    public void createNewTab(String title, String content, String filePath, boolean isTemp, boolean modified) {
        Tab tab = new Tab(title);
        BorderPane tabContent = new BorderPane();

        TextArea editArea = new TextArea(content);
        editArea.setWrapText(true);  // 启用自动换行

        WebView preview = new WebView();

        // 使用新的CustomContextMenu类
        CustomContextMenu contextMenu = new CustomContextMenu(editArea, statusBar);
        editArea.setContextMenu(contextMenu);

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

        TabInfo tabInfo = new TabInfo(title, content, filePath, modified, isTemp);
        tab.setContent(tabContent);
        tab.setUserData(tabInfo);
        updateTabTitle(tab);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        statusBar.updateStatusBar(editArea); // 初始化状态栏

        tab.setOnCloseRequest(event -> {
            if (tabInfo.isModified()) {
                if (tabInfo.isTemp()) {
                    if (!showSaveAsConfirmation(tab)) {
                        event.consume();
                        return;
                    }
                } else {
                    if (!showSaveConfirmation(tab)) {
                        event.consume();
                        return;
                    }
                }
            }
        });
    }

    public void saveCurrentTab() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab != null) {
            TabInfo tabInfo = (TabInfo) currentTab.getUserData();
            if (tabInfo.isModified()) {
                if (tabInfo.isTemp()) {
                    if (!showSaveAsConfirmation(currentTab)) {
                        return;
                    }
                } else {
                    String content = getTabContent(currentTab);
                    if (content != null) {
                        try {
                            Files.write(Paths.get(tabInfo.getFilePath()), content.getBytes());
                            tabInfo.setModified(false);
                            updateTabTitle(currentTab);
                            // 保存成功提示
                            statusBar.showMessage("文件已保存");
                        } catch (IOException e) {
                            e.printStackTrace();
                            // 显示错误消息
                            showErrorAlert("保存错误", "保存文件时发生错误: " + e.getMessage());
                        }
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
                    showErrorAlert("保存错误", "保存文件时发生错误: " + e.getMessage());
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
            String firstSaveableFileType = ConfigLoader.getFirstSaveableFileType();
            fileChooser.setInitialFileName(((TabInfo) tab.getUserData()).getTitle() + firstSaveableFileType.replaceFirst("\\*", ""));

            // 从配置中加载可选文件类型，并默认选中firstSaveableFileType类型
            String fileTypes = ConfigLoader.loadConfig("file.types");
            if (fileTypes != null && !fileTypes.isEmpty()) {
                for (String fileType : fileTypes.split(",")) {
                    String[] parts = fileType.split(":");
                    if (parts.length == 2) {
                        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter(parts[1], parts[0]);
                        fileChooser.getExtensionFilters().add(filter);
                        if (parts[0].equals(firstSaveableFileType)) {
                            fileChooser.setSelectedExtensionFilter(filter);
                        }
                    }
                }
            }

            File file = fileChooser.showSaveDialog(tabPane.getScene().getWindow());
            if (file != null) {
                try (FileWriter writer = new FileWriter(file)) {
                    writer.write(getTabContent(tab));
                    TabInfo tabInfo = (TabInfo) tab.getUserData();
                    tabInfo.setFilePath(file.getAbsolutePath());
                    tabInfo.setModified(false);
                    tabInfo.setTemp(false);
                    tabInfo.setTitle(file.getName());
                    updateTabTitle(tab);
                    ConfigLoader.loadConfig().setProperty("lastOpenedDirectory", file.getParent());
                } catch (IOException e) {
                    showErrorAlert("保存错误", "保存文件时发生错误: " + e.getMessage());
                }
            }
            return true;
        } else if (result.get() == buttonTypeNo) {
            return true;
        } else {
            return false;
        }
    }

    public void updateTabTitle(Tab tab) {
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

    public void ensureAtLeastOneTab() {
        if (tabPane.getTabs().isEmpty()) {
            createNewTemporaryTab();
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setTitle(title);
        errorAlert.setHeaderText(null);
        errorAlert.setContentText(message);
        errorAlert.showAndWait();
    }

    public void handleFileDrop(File file) {
        if (file.isFile()) {
            try {
                String content = new String(Files.readAllBytes(file.toPath()));
                String fileName = file.getName();
                String filePath = file.getAbsolutePath();
                createNewTab(fileName, content, filePath);
            } catch (IOException e) {
                logger.error("读取文件时发生错误: " + e.getMessage(), e);
                showErrorAlert("文件读取错误", "无法读取文件: " + file.getName());
            }
        }
    }
}
