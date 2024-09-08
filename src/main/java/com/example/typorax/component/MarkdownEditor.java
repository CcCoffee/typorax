package com.example.typorax.component;

import com.example.typorax.manager.QueryAndReplaceManager;
import com.example.typorax.manager.SessionManager;
import com.example.typorax.model.TabInfo;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;

public class MarkdownEditor extends BorderPane {
    private final CustomTabPane tabPane;

    public MarkdownEditor(Stage stage) {
        tabPane = new CustomTabPane(createStatusBar());
        CustomMenuBar menuBar = new CustomMenuBar(stage, tabPane);
        QueryAndReplaceManager.createSearchReplaceDialog(stage, tabPane);
        setTop(menuBar);
        setCenter(tabPane);
        loadSession();
        // 添加键盘事件处理
        this.setOnKeyPressed(this::handleKeyPress);
    }

    private void handleKeyPress(KeyEvent event) {
        KeyCombination saveCombination = new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN);
        KeyCombination findCombination = new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN);

        if (saveCombination.match(event)) {
            tabPane.saveCurrentTab();
            event.consume(); // 防止事件进一步传播
        } else if (findCombination.match(event)) {
            QueryAndReplaceManager.showSearchReplaceDialog();
            event.consume();
        }
    }

    private StatusBar createStatusBar() {
        StatusBar statusBar = new StatusBar();
        setBottom(statusBar);
        return statusBar;
    }

    private void loadSession() {
        List<TabInfo> savedTabs = SessionManager.loadSession();
        for (TabInfo tabInfo : savedTabs) {
            tabPane.createNewTab(tabInfo.getTitle(), tabInfo.getContent(), tabInfo.getFilePath());
        }
    }

    public void saveSession() {
        SessionManager.saveSession(tabPane);
    }

}