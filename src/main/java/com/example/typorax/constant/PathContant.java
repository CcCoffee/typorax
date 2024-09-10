package com.example.typorax.constant;

import java.io.File;

public class PathContant {
  
    public static final String USER_CONFIG_DIR = System.getProperty("user.home") + File.separator + ".typorax";
    public static final String USER_CONFIG_PATH = USER_CONFIG_DIR + File.separator + "config.properties";
    public static final String TEMP_CONFIG_PATH = USER_CONFIG_DIR + File.separator + "temp.properties";
    public static final String DEFAULT_CONFIG_PATH = "/default-config.properties";
}
