package com.example.byebit.web3j;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads configuration properties (like API keys) for tests from a properties file.
 * Uses a static initializer block to load keys once when the class is first accessed.
 * <p>
 * IMPORTANT: Ensure the properties file (e.g., test_config.properties)
 * is added to your .gitignore to avoid committing secrets!
 */
public class TestConfig {

    // --- Public Static Final fields for tests ---
    public static final String TEST_WALLET_ADDRESS;
    public static final String TEST_WALLET_PRIVATE_KEY;
    public static final String SEPOLIA_RPC_URL;


    private static final String PROPERTIES_FILE = "test_config.properties";
    private static final Properties properties = new Properties();

    // Add more static final fields for other keys if needed

    // --- Static Initializer Block ---
    // This block runs automatically only ONCE when the TestConfig class is first loaded.
    static {
        try (InputStream input = TestConfig.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {

            if (input == null) {
                // Throw a clear error if the config file is missing
                throw new RuntimeException("Configuration file not found in classpath: " + PROPERTIES_FILE);
            }

            // Load the properties file
            properties.load(input);

            // Load individual properties into the static final fields
            TEST_WALLET_ADDRESS = loadProperty("TEST_WALLET_ADDRESS");
            TEST_WALLET_PRIVATE_KEY = loadProperty("TEST_WALLET_PRIVATE_KEY");
            SEPOLIA_RPC_URL = loadProperty("SEPOLIA_RPC_URL");
            // Load other properties here...

            // Optional: Log success or loaded keys (avoid logging actual secrets in production logs)
            System.out.println("TestConfig loaded successfully.");
            // System.out.println("Loaded API Key (masked): " + (API_KEY != null && API_KEY.length() > 4 ? API_KEY.substring(0, 4) + "..." : "N/A"));


        } catch (IOException e) {
            // Throw an unchecked exception if loading fails, stopping test execution early
            throw new RuntimeException("Failed to load configuration file: " + PROPERTIES_FILE, e);
        } catch (IllegalArgumentException e) {
            // Catch errors from loadProperty
            throw new RuntimeException("Configuration error in " + PROPERTIES_FILE, e);
        }
    }

    /**
     * Helper method to load a property and ensure it's not null or empty.
     *
     * @param key The property key to load.
     * @return The property value.
     * @throws IllegalArgumentException if the property is missing or empty.
     */
    private static String loadProperty(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing or empty required property: '" + key + "' in " + PROPERTIES_FILE);
        }
        return value.trim();
    }

    private TestConfig() {
    }

    @Test
    public void testSecretsWork() {
        System.out.println("Testing TestConfig loading...");
        Assertions.assertNotNull(TestConfig.TEST_WALLET_ADDRESS);
        Assertions.assertNotNull(TestConfig.TEST_WALLET_PRIVATE_KEY);
//        System.out.println("API_KEY: " + TestConfig.TEST_WALLET_ADDRESS);
//        System.out.println("API_SECRET: " + TestConfig.TEST_WALLET_PRIVATE_KEY);
    }
}
