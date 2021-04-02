package com.you.company;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * A tool to read the properties from config.properties.
 */
public class Util {
    Logger logger = LoggerFactory.getLogger(getClass());

    private Properties properties = new Properties();
    private static Util util;

    /**
     * Read configuration from the config.properties file.
     */
    private Util() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader("config.properties"));
            properties.load(bufferedReader);
        } catch (FileNotFoundException e) {
            logger.error(e.toString());
        } catch (IOException e) {
            logger.error(e.toString());
            logger.error(e.getMessage());
        }
    }

    /**
     * Return the Util instance.
     * @return the Util instance
     */
    public static Util getInstance() {
        if (util == null) {
            util = new Util();
        }
        return util;
    }

    /**
     * Get the value of the key
     * @param key the key
     * @return the value of the key
     */
    public String get(String key) {
        return properties.getProperty(key);
    }
}
