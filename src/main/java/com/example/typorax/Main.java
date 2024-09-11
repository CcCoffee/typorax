package com.example.typorax;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.typorax.component.MarkdownEditor;
import com.example.typorax.util.ConfigLoader;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    @Override
    public void start(Stage primaryStage) {
        // 加载配置
        logger.info("start application, load config...");
        ConfigLoader.loadConfig();
        MarkdownEditor editor = new MarkdownEditor(primaryStage);
        Scene scene = new Scene(editor, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        primaryStage.setTitle("Typorax - My Editor");
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/typorax.ico"))); // 设置应用图标
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
}