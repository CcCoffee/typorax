package com.example.typorax.component;

import com.example.typorax.manager.TabHeaderContextMenuManager;
import com.example.typorax.manager.TabManager;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;

public class CustomTabPane extends TabPane {

    private static final Logger logger = LoggerFactory.getLogger(CustomTabPane.class);

    private final TabHeaderContextMenuManager contextMenuManager;
    private final TabManager tabManager;

    public CustomTabPane(StatusBar statusBar) {
        this.contextMenuManager = new TabHeaderContextMenuManager(this);
        this.tabManager = new TabManager(this, statusBar);

        // 添加双击事件监听器
        this.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getClickCount() == 2 && isDoubleClickOnEmptyTabHeader(event)) {
                tabManager.createNewTemporaryTab();
            }
        });

        // 添加右键点击事件监听器
        this.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.getButton() == MouseButton.SECONDARY && isClickOnTabHeader(event)) {
                contextMenuManager.showContextMenu(event);
            } else {
                contextMenuManager.hideContextMenu();
            }
        });

        // 添加标签选择变化监听器
        this.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            contextMenuManager.hideContextMenu();
        });

        // 监听标签的添加和移除
        this.getTabs().addListener((ListChangeListener<Tab>) c -> {
            while (c.next()) {
                if (c.wasRemoved()) {
                    tabManager.ensureAtLeastOneTab();
                }
            }
        });

        // 添加拖放事件监听器
        this.setOnDragOver(this::handleDragOver);
        this.setOnDragDropped(this::handleDragDropped);
    }

    private boolean isDoubleClickOnEmptyTabHeader(MouseEvent event) {
        // 检查双击是否发生在标签头部的空白处
        logger.info("Target class name: {}", event.getTarget().getClass().getName());
        return event.getTarget() instanceof StackPane && event.getClickCount() == 2;
    }

    private boolean isClickOnTabHeader(MouseEvent event) {
        Node target = (Node) event.getTarget();
        while (target != null && !(target instanceof TabPane)) {
            if (target.getStyleClass().contains("tab-header-area")) {
                return true;
            }
            target = target.getParent();
        }
        return false;
    }

    public TabManager getTabManager() {
        return tabManager;
    }

    public void ensureTabExists() {
        if (getTabs().isEmpty()) {
            tabManager.ensureAtLeastOneTab();
        }
    }

    private void handleDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    private void handleDragDropped(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            for (File file : db.getFiles()) {
                tabManager.handleFileDrop(file);
            }
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }
}
