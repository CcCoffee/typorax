package com.example.typorax.component;

import com.example.typorax.constant.PathContant;
import com.example.typorax.model.TabInfo;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.example.typorax.util.ConfigLoader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomMenuBar extends MenuBar {

        private static final Logger logger = LoggerFactory.getLogger(CustomMenuBar.class);


    private String lastOpenedDirectory;

    public CustomMenuBar(Stage primaryStage, CustomTabPane tabPane) {
        lastOpenedDirectory = ConfigLoader.loadConfig("lastOpenedDirectory");
        Menu fileMenu = new Menu("文件");
        Menu editMenu = new Menu("编辑");
        Menu formatMenu = new Menu("格式");
        Menu viewMenu = new Menu("视图");
        Menu helpMenu = new Menu("帮助");

        this.getMenus().addAll(fileMenu, editMenu, formatMenu, viewMenu, helpMenu);

        String fileTypes = ConfigLoader.loadConfig("file.types");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("打开文件");

        for (String fileType : fileTypes.split(",")) {
            String[] parts = fileType.split(":");
            if (parts.length == 2) {
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(parts[1], parts[0]));
            } else {
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(parts[0] + " Files", parts[0]));
            }
        }

        MenuItem openMenuItem = new MenuItem("打开");
        MenuItem saveMenuItem = new MenuItem("保存");

        openMenuItem.setOnAction(event -> openFile(primaryStage, tabPane));

        fileMenu.getItems().addAll(openMenuItem, saveMenuItem);
        editMenu.getItems().addAll(new MenuItem("撤销"), new MenuItem("重做"), new MenuItem("剪切"), new MenuItem("复制"), new MenuItem("粘贴"));
        MenuItem jsonFormatItem = new MenuItem("JSON 格式化");
        jsonFormatItem.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCodeCombination.CONTROL_DOWN, KeyCodeCombination.SHIFT_DOWN, KeyCodeCombination.ALT_DOWN));
        jsonFormatItem.setOnAction(event -> formatJson(tabPane));
        MenuItem jsonCompressItem = new MenuItem("JSON 压缩");
        jsonCompressItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCodeCombination.CONTROL_DOWN, KeyCodeCombination.SHIFT_DOWN, KeyCodeCombination.ALT_DOWN));
        jsonCompressItem.setOnAction(event -> compressJson(tabPane));
        formatMenu.getItems().addAll(jsonFormatItem, jsonCompressItem, new SeparatorMenuItem(), new MenuItem("加粗"), new MenuItem("斜体"), new MenuItem("下划线"));
        viewMenu.getItems().addAll(new MenuItem("源代码模式"), new MenuItem("预览模式"));
        helpMenu.getItems().addAll(new MenuItem("关于"), new MenuItem("帮助文档"));
    }

    private void openFile(Stage primaryStage, CustomTabPane tabPane) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("打开文件");

        String fileTypes = ConfigLoader.loadConfig("file.types");

        for (String fileType : fileTypes.split(",")) {
            String[] parts = fileType.split(":");
            if (parts.length == 2) {
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(parts[1], parts[0]));
            } else {
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(parts[0] + " Files", parts[0]));
            }
        }

        // 设置初始目录
        if (lastOpenedDirectory != null && !lastOpenedDirectory.isEmpty()) {
            fileChooser.setInitialDirectory(new File(lastOpenedDirectory));
        }

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            // 检查文件是否已经被打开
            for (Tab tab : tabPane.getTabs()) {
                TabInfo tabInfo = (TabInfo) tab.getUserData();
                if (tabInfo.getFilePath().equals(selectedFile.getAbsolutePath())) {
                    // 切换到已打开的标签
                    tabPane.getSelectionModel().select(tab);
                    return;
                }
            }

            try {
                String content = new String(Files.readAllBytes(Paths.get(selectedFile.getPath())));
                tabPane.getTabManager().createNewTab(selectedFile.getName(), content, selectedFile.getAbsolutePath());

                // 保存新的目录
                lastOpenedDirectory = selectedFile.getParent();
                saveLastOpenedDirectory(lastOpenedDirectory);
            } catch (IOException e) {
                e.printStackTrace();
                // 在这里可以添加错误处理，比如显示一个错误对话框
            }
        }
    }

    private void formatJson(CustomTabPane tabPane) {
        logger.info("Format JSON");
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        TextArea textArea = getCurrentTextArea(selectedTab);
        if (selectedTab != null) {
            TabInfo tabInfo = (TabInfo) selectedTab.getUserData();
            String content = textArea.getText();

            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Object json = gson.fromJson(content, Object.class);
                String formattedJson = gson.toJson(json);

                // 直接更新TextArea的内容
                textArea.setText(formattedJson);

                // 更新TabInfo
                tabInfo.setContent(formattedJson);
                tabInfo.setModified(true);

                // 刷新Tab标题
                tabPane.getTabManager().updateTabTitle(selectedTab);

                logger.info("JSON formatted successfully");
            } catch (JsonParseException e) {
                logger.warn("JSON formatting failed", e);
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("JSON 格式化错误");
                alert.setHeaderText(null);
                alert.setContentText("无法格式化JSON。请检查JSON字符串是否有效。");
                alert.showAndWait();
            }
        }
    }

    private void compressJson(CustomTabPane tabPane) {
        logger.info("Compress JSON");
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        TextArea textArea = getCurrentTextArea(selectedTab);
        if (selectedTab != null) {
            TabInfo tabInfo = (TabInfo) selectedTab.getUserData();
            String content = textArea.getText();

            try {
                Gson gson = new Gson();
                Object json = gson.fromJson(content, Object.class);
                String compressedJson = gson.toJson(json);

                // 直接更新TextArea的内容
                textArea.setText(compressedJson);

                // 更新TabInfo
                tabInfo.setContent(compressedJson);
                tabInfo.setModified(true);

                // 刷新Tab标题
                tabPane.getTabManager().updateTabTitle(selectedTab);

                logger.info("JSON compressed successfully");
            } catch (JsonParseException e) {
                logger.warn("JSON compression failed", e);
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("JSON 压缩错误");
                alert.setHeaderText(null);
                alert.setContentText("无法压缩JSON。请检查JSON字符串是否有效。");
                alert.showAndWait();
            }
        }
    }

    private static TextArea getCurrentTextArea(Tab currentTab) {
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

    private void saveLastOpenedDirectory(String directory) {
        Properties properties = new Properties();
        properties.setProperty("lastOpenedDirectory", directory);
        try (FileWriter writer = new FileWriter(PathContant.TEMP_CONFIG_PATH)) {
            properties.store(writer, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}