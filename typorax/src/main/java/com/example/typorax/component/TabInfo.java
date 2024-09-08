package com.example.typorax.component;

import java.io.Serializable;

public class TabInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String title;
    private String content;
    private String filePath;
    private boolean modified;
    private boolean isMarkdown;

    public TabInfo(String title, String content, String filePath) {
        this.title = title;
        this.content = content;
        this.filePath = filePath;
        this.modified = false;
        this.isMarkdown = filePath.toLowerCase().endsWith(".md");
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isModified() {
        return modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public boolean isMarkdown() {
        return isMarkdown;
    }
}