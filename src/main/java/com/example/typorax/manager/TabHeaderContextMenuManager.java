package com.example.typorax.manager;

import com.example.typorax.component.CustomTabPane;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.input.MouseEvent;

public class TabHeaderContextMenuManager {

    private final CustomTabPane tabPane;
    private final ContextMenu contextMenu;

    public TabHeaderContextMenuManager(CustomTabPane tabPane) {
        this.tabPane = tabPane;
        this.contextMenu = new ContextMenu();

        MenuItem closeCurrentTab = new MenuItem("关闭当前标签");
        closeCurrentTab.setOnAction(e -> closeCurrentTab());

        MenuItem closeAllTabs = new MenuItem("关闭所有标签");
        closeAllTabs.setOnAction(e -> closeAllTabs());

        MenuItem closeOtherTabs = new MenuItem("关闭其他标签");
        closeOtherTabs.setOnAction(e -> closeOtherTabs());

        contextMenu.getItems().addAll(closeCurrentTab, closeAllTabs, closeOtherTabs);
    }

    public void showContextMenu(MouseEvent event) {
        contextMenu.show(tabPane, event.getScreenX(), event.getScreenY());
    }

    public void hideContextMenu() {
        contextMenu.hide();
    }

    private void closeCurrentTab() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            tabPane.getTabs().remove(selectedTab);
        }
    }

    private void closeAllTabs() {
        tabPane.getTabs().clear();
        tabPane.getTabManager().ensureAtLeastOneTab();
    }

    private void closeOtherTabs() {
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
        if (selectedTab != null) {
            tabPane.getTabs().retainAll(selectedTab);
        }
    }
}