package com.example.typorax.manager;

import com.example.typorax.component.CustomContextMenu;
import com.example.typorax.component.StatusBar;
import com.example.typorax.model.TabInfo;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;

public class TabContentFactory {

    public static BorderPane createTabContent(Tab tab, TabInfo tabInfo, StatusBar statusBar) {
        BorderPane tabContent = new BorderPane();

        TextArea editArea = new TextArea(tabInfo.getContent());
        editArea.setWrapText(true);  // 启用自动换行

        WebView preview = new WebView();

        // 使用新的CustomContextMenu类
        CustomContextMenu contextMenu = new CustomContextMenu(editArea, statusBar);
        editArea.setContextMenu(contextMenu);

        boolean isMarkdown = tabInfo.getFilePath().toLowerCase().endsWith(".md");

        if (isMarkdown) {
            editArea.textProperty().addListener((observable, oldValue, newValue) -> {
                updatePreview(newValue, preview);
                tabInfo.setModified(!newValue.equals(tabInfo.getContent()));
                updateTabTitle(tab, tabInfo);
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

            updatePreview(tabInfo.getContent(), preview);
        } else {
            editArea.textProperty().addListener((observable, oldValue, newValue) -> {
                tabInfo.setModified(!newValue.equals(tabInfo.getContent()));
                updateTabTitle(tab, tabInfo);
                statusBar.updateStatusBar(editArea); // 更新状态栏
            });

            editArea.caretPositionProperty().addListener((obs, oldPos, newPos) -> {
                statusBar.updateStatusBar(editArea); // 更新状态栏
            });

            tabContent.setCenter(editArea);
        }

        return tabContent;
    }

    private static void updatePreview(String markdown, WebView preview) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        String html = renderer.render(parser.parse(markdown));
        preview.getEngine().loadContent(html);
    }

    private static void updateTabTitle(Tab tab, TabInfo tabInfo) {
        String title = tabInfo.getTitle();
        if (tabInfo.isModified()) {
            tab.setText(title + " ⚪");
        } else {
            tab.setText(title);
        }
    }
}