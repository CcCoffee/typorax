package com.example.typora;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        MarkdownEditor editor = new MarkdownEditor(primaryStage);
        Scene scene = new Scene(editor, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        primaryStage.setTitle("Typora-like Markdown Editor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    class MarkdownEditor extends BorderPane {
        private TabPane tabPane;
        private MenuBar menuBar;
        private Stage primaryStage;
    
        public MarkdownEditor(Stage stage) {
            this.primaryStage = stage;
            tabPane = new TabPane();
            
            createMenuBar();
    
            setTop(menuBar);
            setCenter(tabPane);
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
                new FileChooser.ExtensionFilter("Markdown Files", "*.md"),
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
            );
            File selectedFile = fileChooser.showOpenDialog(primaryStage);
            if (selectedFile != null) {
                try {
                    String content = new String(Files.readAllBytes(Paths.get(selectedFile.getPath())));
                    createNewTab(selectedFile.getName(), content);
                } catch (IOException e) {
                    e.printStackTrace();
                    // 在这里可以添加错误处理，比如显示一个错误对话框
                }
            }
        }
        
        private void createNewTab(String title, String content) {
            Tab tab = new Tab(title);
            BorderPane tabContent = new BorderPane();
            
            TextArea markdownArea = new TextArea(content);
            WebView preview = new WebView();
            
            markdownArea.textProperty().addListener((observable, oldValue, newValue) -> {
                updatePreview(newValue, preview);
            });
            
            tabContent.setLeft(markdownArea);
            tabContent.setCenter(preview);
            
            tab.setContent(tabContent);
            tabPane.getTabs().add(tab);
            tabPane.getSelectionModel().select(tab);
            
            updatePreview(content, preview);
        }
        
        private void updatePreview(String markdown, WebView preview) {
            MutableDataSet options = new MutableDataSet();
            Parser parser = Parser.builder(options).build();
            HtmlRenderer renderer = HtmlRenderer.builder(options).build();
            String html = renderer.render(parser.parse(markdown));
            preview.getEngine().loadContent(html);
        }
    }
}
