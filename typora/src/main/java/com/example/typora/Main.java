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

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        MarkdownEditor editor = new MarkdownEditor();
        Scene scene = new Scene(editor, 800, 600);
        primaryStage.setTitle("Typora-like Markdown Editor");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    class MarkdownEditor extends BorderPane {
        private TextArea markdownArea;
        private WebView preview;
        private MenuBar menuBar;
    
        public MarkdownEditor() {
            markdownArea = new TextArea();
            preview = new WebView();
    
            markdownArea.textProperty().addListener((observable, oldValue, newValue) -> {
                updatePreview(newValue);
            });
    
            createMenuBar();
    
            setTop(menuBar);
            setLeft(markdownArea);
            setCenter(preview);
        }
    
        private void createMenuBar() {
            menuBar = new MenuBar();
    
            Menu fileMenu = new Menu("文件");
            Menu editMenu = new Menu("编辑");
            Menu formatMenu = new Menu("格式");
            Menu viewMenu = new Menu("视图");
            Menu helpMenu = new Menu("帮助");
    
            menuBar.getMenus().addAll(fileMenu, editMenu, formatMenu, viewMenu, helpMenu);
    
            // Add menu items to each menu as needed
            // For example:
            fileMenu.getItems().addAll(new MenuItem("新建"), new MenuItem("打开"), new MenuItem("保存"));
            editMenu.getItems().addAll(new MenuItem("撤销"), new MenuItem("重做"), new MenuItem("剪切"), new MenuItem("复制"), new MenuItem("粘贴"));
            formatMenu.getItems().addAll(new MenuItem("加粗"), new MenuItem("斜体"), new MenuItem("下划线"));
            viewMenu.getItems().addAll(new MenuItem("源代码模式"), new MenuItem("预览模式"));
            helpMenu.getItems().addAll(new MenuItem("关于"), new MenuItem("帮助文档"));
        }
    
        private void updatePreview(String markdown) {
            MutableDataSet options = new MutableDataSet();
            Parser parser = Parser.builder(options).build();
            HtmlRenderer renderer = HtmlRenderer.builder(options).build();
            String html = renderer.render(parser.parse(markdown));
            preview.getEngine().loadContent(html);
        }
    }
}
