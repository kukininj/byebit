package com.example.byebit.ui.settings;

import static com.example.byebit.repository.AppPreferencesRepository.KEY_ETHERSCAN_API_KEY;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;

import com.example.byebit.R;

public class SettingsFragment extends PreferenceFragmentCompat {

    // Define a constant for the preference key (good practice)

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Get the EditTextPreference for the API key
        EditTextPreference etherscanApiKeyPref = findPreference(KEY_ETHERSCAN_API_KEY);

        // Set a listener to update the summary when the preference value changes
        if (etherscanApiKeyPref != null) {
            // Set initial summary
            String currentApiKey = etherscanApiKeyPref.getText();
            if (currentApiKey != null && !currentApiKey.isEmpty()) {
                etherscanApiKeyPref.setSummary(currentApiKey);
            } else {
                etherscanApiKeyPref.setSummary("Not set");
            }

            etherscanApiKeyPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String stringValue = newValue.toString();
                    // Update the summary to show the new value
                    preference.setSummary(stringValue);
                    // Return true to save the new value to SharedPreferences
                    return true;
                }
            });
        }
    }
}
