package com.example.typorax.model;

import java.io.Serializable;

public class TabInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String title;
    private String content;
    private String filePath;
    private boolean modified;
    private boolean isMarkdown;
    private boolean isTemp;

    public TabInfo(String title, String content, String filePath, boolean modified, boolean isTemp) {
        this.title = title;
        this.content = content;
        this.filePath = filePath;
        this.modified = modified;
        this.isMarkdown = filePath.toLowerCase().endsWith(".md");
        this.isTemp = isTemp;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
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

    public boolean isTemp() {
        return isTemp;
    }

    public void setTemp(boolean isTemp) {
        this.isTemp = isTemp;
    }
}