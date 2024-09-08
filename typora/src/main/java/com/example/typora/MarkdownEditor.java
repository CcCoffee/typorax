package com.example.typora;

import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;

public class MarkdownEditor extends BorderPane {
    private TextArea markdownArea;
    private WebView preview;

    public MarkdownEditor() {
        markdownArea = new TextArea();
        preview = new WebView();

        markdownArea.textProperty().addListener((observable, oldValue, newValue) -> {
            updatePreview(newValue);
        });

        setLeft(markdownArea);
        setCenter(preview);
    }

    private void updatePreview(String markdown) {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        HtmlRenderer renderer = HtmlRenderer.builder(options).build();
        String html = renderer.render(parser.parse(markdown));
        preview.getEngine().loadContent(html);
    }
}