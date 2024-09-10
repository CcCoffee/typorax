package com.example.typorax.component;

import com.example.typorax.model.TabInfo;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.example.typorax.util.ConfigLoader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class CustomMenuBar extends MenuBar {

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
        formatMenu.getItems().addAll(new MenuItem("加粗"), new MenuItem("斜体"), new MenuItem("下划线"));
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
                tabPane.createNewTab(selectedFile.getName(), content, selectedFile.getAbsolutePath());

                // 保存新的目录
                lastOpenedDirectory = selectedFile.getParent();
                saveLastOpenedDirectory(lastOpenedDirectory);
            } catch (IOException e) {
                e.printStackTrace();
                // 在这里可以添加错误处理，比如显示一个错误对话框
            }
        }
    }

    private void saveLastOpenedDirectory(String directory) {
        Properties properties = new Properties();
        properties.setProperty("lastOpenedDirectory", directory);
        try (FileWriter writer = new FileWriter(ConfigLoader.TEMP_CONFIG_PATH)) {
            properties.store(writer, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}