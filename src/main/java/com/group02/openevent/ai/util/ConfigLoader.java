package com.group02.openevent.ai.util;

import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static Properties properties = new Properties();

    static {
        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("❌ Không tìm thấy file application.properties!");
            }
            properties.load(input);
        } catch (Exception e) {
            throw new RuntimeException("❌ Lỗi khi load properties: " + e.getMessage());
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }
}
