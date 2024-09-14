package com.example.typorax.component;

import com.example.typorax.manager.QueryAndReplaceManager;
import com.example.typorax.manager.SessionManager;
import com.example.typorax.model.TabInfo;
import com.example.typorax.tool.command.Command;
import com.example.typorax.tool.command.FindCommand;
import com.example.typorax.tool.command.SaveCommand;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class MarkdownEditor extends BorderPane {
    private final CustomTabPane tabPane;
    private final StatusBar statusBar;

    public MarkdownEditor(Stage stage) {
        statusBar = new StatusBar();
        tabPane = new CustomTabPane(statusBar);
        CustomMenuBar menuBar = new CustomMenuBar(stage, tabPane);
        QueryAndReplaceManager.createSearchReplaceDialog(stage, tabPane);
        setTop(menuBar);
        setCenter(tabPane);
        setBottom(statusBar);
        loadSession();
        this.setOnKeyPressed(this::handleKeyPress);
    }

    private void handleKeyPress(KeyEvent event) {
        KeyCombination saveCombination = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        KeyCombination findCombination = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);

        Command command = null;
        if (saveCombination.match(event)) {
            command = new SaveCommand(tabPane);
        } else if (findCombination.match(event)) {
            command = new FindCommand();
        }

        if (command != null) {
            command.execute();
            event.consume();
        }
    }

    private void loadSession() {
        List<TabInfo> savedTabs = SessionManager.loadSession();
        for (TabInfo tabInfo : savedTabs) {
            String filePath = tabInfo.getFilePath();
            String fileContent = "";
            if (filePath != null && new File(filePath).exists()) {
                try {
                    fileContent = new String(Files.readAllBytes(Paths.get(filePath)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            boolean isModified = !tabInfo.getContent().equals(fileContent);
            tabPane.getTabManager().createNewTab(tabInfo.getTitle(), tabInfo.getContent(), tabInfo.getFilePath(), tabInfo.isTemp(), isModified);
        }
        
        // 如果没有加载任何标签，则确保创建一个新的临时标签
        if (savedTabs.isEmpty()) {
            tabPane.ensureTabExists();
        }
    }

    public void saveSession() {
        SessionManager.saveSession(tabPane);
    }
}