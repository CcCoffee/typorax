package com.example.typorax.tool.command;

import com.example.typorax.component.CustomTabPane;

public class SaveCommand implements Command {
    private final CustomTabPane tabPane;

    public SaveCommand(CustomTabPane tabPane) {
        this.tabPane = tabPane;
    }

    @Override
    public void execute() {
        tabPane.saveCurrentTab();
    }
}