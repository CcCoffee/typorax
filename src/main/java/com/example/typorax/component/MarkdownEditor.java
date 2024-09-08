package com.example.typorax.component;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;

public class MarkdownEditor extends BorderPane {
    private TabPane tabPane;
    private MenuBar menuBar;
    private Stage primaryStage;
    private String lastOpenedDirectory;
    private StatusBar statusBar;
    private Stage searchReplaceStage;

    public MarkdownEditor(Stage stage) {
        this.primaryStage = stage;
        tabPane = new TabPane();
        lastOpenedDirectory = loadLastOpenedDirectory();

        createMenuBar();
        createStatusBar();
        createSearchReplaceDialog(stage);

        setTop(menuBar);
        setCenter(tabPane);

        loadSession();

        // 添加键盘事件处理
        this.setOnKeyPressed(this::handleKeyPress);
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

    private void createStatusBar() {
        statusBar = new StatusBar();
        setBottom(statusBar);
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

        tab.setContent(tabContent);
        tab.setUserData(new TabInfo(title, content, filePath));
        updateTabTitle(tab);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);

        statusBar.updateStatusBar(editArea); // 初始化状态栏

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

    private String getTabContent(Tab tab) {
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
            String content = getTabContent(tab);
            tabsToSave.add(new TabInfo(tabInfo.getTitle(), content, tabInfo.getFilePath()));
        }
        SessionManager.saveSession(tabsToSave);
    }

    private void handleKeyPress(KeyEvent event) {
        KeyCombination saveCombination = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        KeyCombination findCombination = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);
        
        if (saveCombination.match(event)) {
            saveCurrentTab();
            event.consume(); // 防止事件进一步传播
        } else if (findCombination.match(event)) {
            showSearchReplaceDialog();
            event.consume();
        }
    }

    private void saveCurrentTab() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
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

    private void createSearchReplaceDialog(Stage parentStage) {
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
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        searchReplaceStage.setScene(scene);
    }

    private void showSearchReplaceDialog() {
        if (!searchReplaceStage.isShowing()) {
            searchReplaceStage.show();
        } else {
            searchReplaceStage.requestFocus();
        }
    }

    private void findNext(String searchText) {
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

    private void replace(String searchText, String replaceText) {
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

    private void replaceAll(String searchText, String replaceText) {
        TextArea currentTextArea = getCurrentTextArea();
        if (currentTextArea != null) {
            String text = currentTextArea.getText();
            String newText = text.replace(searchText, replaceText);
            currentTextArea.setText(newText);
        }
    }

    private TextArea getCurrentTextArea() {
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

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}