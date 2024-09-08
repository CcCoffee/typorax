package com.example.typora;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;

import java.util.Optional;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.FileWriter;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        MarkdownEditor editor = new MarkdownEditor(primaryStage);
        Scene scene = new Scene(editor, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        primaryStage.setTitle("Typora-like Markdown Editor");
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            editor.saveSession();
            Platform.exit();
        });
    }

    public static void main(String[] args) {
        launch(args);
    }

    class MarkdownEditor extends BorderPane {
        private TabPane tabPane;
        private MenuBar menuBar;
        private Stage primaryStage;
        private String lastOpenedDirectory;

        public MarkdownEditor(Stage stage) {
            this.primaryStage = stage;
            tabPane = new TabPane();
            lastOpenedDirectory = loadLastOpenedDirectory();

            createMenuBar();

            setTop(menuBar);
            setCenter(tabPane);

            loadSession();
        }

        private void createMenuBar() {
            menuBar = new MenuBar();

            Menu fileMenu = new Menu("文件");
            Menu editMenu = new Menu("编辑");
            Menu formatMenu = new Menu("格式");
            Menu viewMenu = new Menu("视图");
            Menu helpMenu = new Menu("帮助");

            menuBar.getMenus().addAll(fileMenu, editMenu, formatMenu, viewMenu, helpMenu);

            MenuItem openMenuItem = new MenuItem("打开");
            MenuItem saveMenuItem = new MenuItem("保存");

            openMenuItem.setOnAction(event -> openFile());

            fileMenu.getItems().addAll(openMenuItem, saveMenuItem);
            editMenu.getItems().addAll(new MenuItem("撤销"), new MenuItem("重做"), new MenuItem("剪切"), new MenuItem("复制"), new MenuItem("粘贴"));
            formatMenu.getItems().addAll(new MenuItem("加粗"), new MenuItem("斜体"), new MenuItem("下划线"));
            viewMenu.getItems().addAll(new MenuItem("源代码模式"), new MenuItem("预览模式"));
            helpMenu.getItems().addAll(new MenuItem("关于"), new MenuItem("帮助文档"));
        }

        private void openFile() {
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
                try {
                    String content = new String(Files.readAllBytes(Paths.get(selectedFile.getPath())));
                    createNewTab(selectedFile.getName(), content, selectedFile.getAbsolutePath());

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

        private void createNewTab(String title, String content, String filePath) {
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
                });
                tabContent.setCenter(editArea);
            }

            tab.setContent(tabContent);
            tab.setUserData(new TabInfo(title, content, filePath));
            updateTabTitle(tab);
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);

            updatePreview(content, preview);

            tab.setOnCloseRequest(event -> {
                TabInfo tabInfo = (TabInfo) tab.getUserData();
                if (tabInfo.isModified()) {
                    if (!showSaveConfirmation(tab)) {
                        event.consume();
                    }
                }
            });
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
                String content = ((TextArea) ((BorderPane) tab.getContent()).getLeft()).getText();
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
                return true;
            } else if (result.get() == buttonTypeNo) {
                return true;
            } else {
                return false;
            }
        }

        private void updatePreview(String markdown, WebView preview) {
            MutableDataSet options = new MutableDataSet();
            Parser parser = Parser.builder(options).build();
            HtmlRenderer renderer = HtmlRenderer.builder(options).build();
            String html = renderer.render(parser.parse(markdown));
            preview.getEngine().loadContent(html);
        }

        private void loadSession() {
            List<TabInfo> savedTabs = SessionManager.loadSession();
            for (TabInfo tabInfo : savedTabs) {
                createNewTab(tabInfo.getTitle(), tabInfo.getContent(), tabInfo.getFilePath());
            }
        }

        public void saveSession() {
            List<TabInfo> tabsToSave = new ArrayList<>();
            for (Tab tab : tabPane.getTabs()) {
                TabInfo tabInfo = (TabInfo) tab.getUserData();
                String content = "";
                if (tab.getContent() instanceof BorderPane) {
                    BorderPane tabContent = (BorderPane) tab.getContent();
                    if (tabContent.getCenter() instanceof SplitPane) {
                        // Markdown file
                        SplitPane splitPane = (SplitPane) tabContent.getCenter();
                        TextArea markdownArea = (TextArea) splitPane.getItems().get(0);
                        content = markdownArea.getText();
                    } else if (tabContent.getCenter() instanceof TextArea) {
                        // Non-Markdown file
                        TextArea textArea = (TextArea) tabContent.getCenter();
                        content = textArea.getText();
                    }
                }
                tabsToSave.add(new TabInfo(tabInfo.getTitle(), content, tabInfo.getFilePath()));
            }
            SessionManager.saveSession(tabsToSave);
        }
    }

    static class TabInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private String title;
        private String content;
        private String filePath;
        private boolean modified;
        private boolean isMarkdown;

        public TabInfo(String title, String content, String filePath) {
            this.title = title;
            this.content = content;
            this.filePath = filePath;
            this.modified = false;
            this.isMarkdown = filePath.toLowerCase().endsWith(".md");
        }

        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getFilePath() { return filePath; }
        public boolean isModified() { return modified; }
        public void setModified(boolean modified) { this.modified = modified; }
        public boolean isMarkdown() { return isMarkdown; }
    }

    static class SessionManager {
        private static final String SESSION_FILE = "session.ser";

        public static void saveSession(List<TabInfo> tabs) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SESSION_FILE))) {
                oos.writeObject(tabs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public static List<TabInfo> loadSession() {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SESSION_FILE))) {
                return (List<TabInfo>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                return new ArrayList<>();
            }
        }
    }
}
