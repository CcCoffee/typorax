package com.example.typorax.manager;

import com.example.typorax.component.CustomTabPane;
import com.example.typorax.model.TabInfo;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SessionManager {
    private static final String SESSION_FILE = "session.ser";

    public static void saveSession(List<TabInfo> tabs) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(SESSION_FILE))) {
            oos.writeObject(tabs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<TabInfo> loadSession() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SESSION_FILE))) {
            return (List<TabInfo>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            return new ArrayList<>();
        }
    }

    public static void saveSession(CustomTabPane tabPane) {
        List<TabInfo> tabsToSave = new ArrayList<>();
        for (Tab tab : tabPane.getTabs()) {
            TabInfo tabInfo = (TabInfo) tab.getUserData();
            String content = tabPane.getTabContent(tab);
            tabsToSave.add(new TabInfo(tabInfo.getTitle(), content, tabInfo.getFilePath()));
        }
        SessionManager.saveSession(tabsToSave);
    }
}