package com.example.typorax.component;

import com.example.typorax.model.TabInfo;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import com.example.typorax.util.ConfigLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class CustomMenuBar extends MenuBar {

    private String lastOpenedDirectory;

    public CustomMenuBar(Stage primaryStage, CustomTabPane tabPane) {
        lastOpenedDirectory = loadLastOpenedDirectory();
        Menu fileMenu = new Menu("文件");
        Menu editMenu = new Menu("编辑");
        Menu formatMenu = new Menu("格式");
        Menu viewMenu = new Menu("视图");
        Menu helpMenu = new Menu("帮助");

        this.getMenus().addAll(fileMenu, editMenu, formatMenu, viewMenu, helpMenu);

        Properties config = ConfigLoader.loadConfig();
        String fileTypes = config.getProperty("file.types", "*.*");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("打开文件");
        for (String fileType : fileTypes.split(",")) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(fileType + " Files", fileType));
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
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All Files", "*.*"),
                new FileChooser.ExtensionFilter("Markdown Files", "*.md"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

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

    private String loadLastOpenedDirectory() {
        JSONParser parser = new JSONParser();
        String userHome = System.getProperty("user.home");
        String configPath = userHome + File.separator + ".typorax_conf";

        try (FileReader reader = new FileReader(configPath)) {
            JSONObject jsonObject = (JSONObject) parser.parse(reader);
            return (String) jsonObject.get("lastOpenedDirectory");
        } catch (IOException | ParseException e) {
            // 如果文件不存在或解析失败，返回null
            return null;
        }
    }

    private void saveLastOpenedDirectory(String directory) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("lastOpenedDirectory", directory);

        String userHome = System.getProperty("user.home");
        String configPath = userHome + File.separator + ".typorax_conf";

        try (FileWriter file = new FileWriter(configPath)) {
            file.write(jsonObject.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}