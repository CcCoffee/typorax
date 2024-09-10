package com.example.typorax.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigLoader {
    private static final String USER_CONFIG_DIR = System.getProperty("user.home") + File.separator + ".typorax";
    private static final String USER_CONFIG_PATH = USER_CONFIG_DIR + File.separator + "config.properties";
    private static final String DEFAULT_CONFIG_PATH = "/default-config.properties";

    public static Properties loadConfig() {
        Properties properties = new Properties();

        // 检查并创建用户配置目录和文件
        createUserConfigIfNotExists();

        // 尝试从用户目录加载配置
        try (InputStream userConfigStream = new FileInputStream(USER_CONFIG_PATH)) {
            properties.load(userConfigStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return properties;
    }

    private static void createUserConfigIfNotExists() {
        try {
            // 创建用户配置目录
            Files.createDirectories(Paths.get(USER_CONFIG_DIR));

            // 如果用户配置文件不存在，则复制默认配置文件
            File userConfigFile = new File(USER_CONFIG_PATH);
            if (!userConfigFile.exists()) {
                try (InputStream defaultConfigStream = ConfigLoader.class.getResourceAsStream(DEFAULT_CONFIG_PATH);
                     OutputStream userConfigStream = new FileOutputStream(USER_CONFIG_PATH)) {
                    if (defaultConfigStream != null) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = defaultConfigStream.read(buffer)) != -1) {
                            userConfigStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}