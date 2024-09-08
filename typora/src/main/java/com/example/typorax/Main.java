package com.example.typora;

import com.example.typora.component.MarkdownEditor;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        MarkdownEditor editor = new MarkdownEditor(primaryStage);
        Scene scene = new Scene(editor, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/application.css").toExternalForm());
        primaryStage.setTitle("Typorax - Kevin's Editor");
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