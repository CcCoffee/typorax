package com.example.typorax.util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class ConfigLoader {
    private static final String USER_CONFIG_DIR = System.getProperty("user.home") + File.separator + ".typorax";
    private static final String USER_CONFIG_PATH = USER_CONFIG_DIR + File.separator + "config.properties";
    public static final String TEMP_CONFIG_PATH = USER_CONFIG_DIR + File.separator + "temp.properties";
    private static final String DEFAULT_CONFIG_PATH = "/default-config.properties";
    private static Properties cachedProperties = null;

    public static Properties loadConfig() {
        if (cachedProperties == null) {
            cachedProperties = new Properties();

            // 检查并创建用户配置目录和文件
            createUserConfigIfNotExists();

            // 加载默认配置
            cachedProperties = loadPropertiesFromResource(DEFAULT_CONFIG_PATH, cachedProperties);

            // 加载用户配置
            cachedProperties = loadPropertiesFromFile(USER_CONFIG_PATH, cachedProperties);

            // 加载临时配置
            cachedProperties = loadPropertiesFromFile(TEMP_CONFIG_PATH, cachedProperties);
        }
        return cachedProperties;
    }

    public static String loadConfig(String key) {
        Properties properties = loadConfig();
        return properties.getProperty(key);
    }

    private static Properties loadPropertiesFromResource(String resourcePath, Properties properties) {
        try (InputStream resourceStream = ConfigLoader.class.getResourceAsStream(resourcePath)) {
            if (resourceStream != null) {
                Properties tempProperties = new Properties();
                tempProperties.load(resourceStream);
                properties.putAll(tempProperties);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private static Properties loadPropertiesFromFile(String filePath, Properties properties) {
        try (InputStream fileStream = new FileInputStream(filePath)) {
            Properties tempProperties = new Properties();
            tempProperties.load(fileStream);
            properties.putAll(tempProperties);
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

            // 如果临时配置文件不存在，则创建空文件
            File tempConfigFile = new File(TEMP_CONFIG_PATH);
            if (!tempConfigFile.exists()) {
                tempConfigFile.createNewFile();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}