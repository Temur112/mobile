package zoro.benojir.callrecorder.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;

import zoro.benojir.callrecorder.R;
import zoro.benojir.callrecorder.helpers.CustomFunctions;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (preferences.getString("appearance", "device_default").equals("dark_mode")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else if (preferences.getString("appearance", "device_default").equals("light_mode")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }

        Button logoutButton = findViewById(R.id.logoutButton);

        logoutButton.setOnClickListener(v -> {
            CustomFunctions.logout(SettingsActivity.this);

            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar_include);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
        }
        toolbar.setNavigationIcon(AppCompatResources.getDrawable(this, R.drawable.arrow_back_24));
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // Appearance listener (existing)
            ListPreference appearancePreference = findPreference("appearance");
            if (appearancePreference != null) {
                appearancePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                    getActivity().recreate();
                    return true;
                });
            }

            // === PIN handling ===
            androidx.preference.Preference changePinPref = findPreference("change_pin");
            androidx.preference.SwitchPreferenceCompat enablePinPref = findPreference("enable_pin");

            if (changePinPref != null) {
                changePinPref.setOnPreferenceClickListener(preference -> {
                    showSetPinDialog();
                    return true;
                });
            }

            if (enablePinPref != null) {
                enablePinPref.setOnPreferenceChangeListener((preference, newValue) -> {
                    boolean enabled = (Boolean) newValue;
                    if (enabled && zoro.benojir.callrecorder.helpers.PinPreferencesHelper.INSTANCE.getPin(getContext()) == null)
                        {
                        showSetPinDialog();
                    }
                    return true;
                });
            }
        }

        private void showSetPinDialog() {
            android.widget.EditText input = new android.widget.EditText(getContext());
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER |
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            input.setHint("Enter new PIN");

            new androidx.appcompat.app.AlertDialog.Builder(getContext())
                    .setTitle("Set or Change PIN")
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String pin = input.getText().toString();
                        if (pin.length() < 4) {
                            android.widget.Toast.makeText(getContext(), "PIN must be at least 4 digits", android.widget.Toast.LENGTH_SHORT).show();
                        } else {
                            zoro.benojir.callrecorder.helpers.PinPreferencesHelper.INSTANCE.setPin(getContext(), pin);

                            android.widget.Toast.makeText(getContext(), "PIN updated", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }
}