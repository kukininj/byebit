package com.example.byebit.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

public class AppPreferencesRepository {

    private static final String TAG = "AppPreferencesRepo";
    private static final String PREFS_NAME = "app_preferences"; // Name for your SharedPreferences file
    public static final String KEY_ETHERSCAN_API_KEY = "etherscan_api_key"; // Key for the API key

    private static volatile AppPreferencesRepository INSTANCE;
    private final SharedPreferences sharedPreferences;

    private AppPreferencesRepository(Context context) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static AppPreferencesRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppPreferencesRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppPreferencesRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    /**
     * Reads the Etherscan API key from SharedPreferences.
     * This method is synchronous.
     *
     * @return The API key string, or null if not found.
     */
    public String getEtherscanApiKey() {

        // SharedPreferences.getString(key, defaultValue)
        // We'll return null if not found, as an empty string might be a valid key.
        return sharedPreferences.getString(KEY_ETHERSCAN_API_KEY, null);
    }

    /**
     * Saves the Etherscan API key to SharedPreferences.
     * This method writes asynchronously in the background.
     */
    public void saveEtherscanApiKey(String apiKey) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_ETHERSCAN_API_KEY, apiKey);
        editor.apply(); // Apply writes the changes asynchronously
        Log.d(TAG, "Etherscan API key save initiated.");
    }

    // No shutdown needed for SharedPreferences as it manages its own lifecycle.
}
