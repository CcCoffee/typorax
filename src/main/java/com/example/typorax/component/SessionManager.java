package com.example.typorax.component;

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
}