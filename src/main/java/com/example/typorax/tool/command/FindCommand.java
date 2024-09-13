package com.example.typorax.tool.command;

import com.example.typorax.manager.QueryAndReplaceManager;

public class FindCommand implements Command {
    @Override
    public void execute() {
        QueryAndReplaceManager.showSearchReplaceDialog();
    }
}