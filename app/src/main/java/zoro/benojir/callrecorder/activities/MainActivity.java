package zoro.benojir.callrecorder.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.StrictMode;
import android.provider.Settings;
import android.provider.Telephony;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

import zoro.benojir.callrecorder.BuildConfig;
import zoro.benojir.callrecorder.R;
import zoro.benojir.callrecorder.adapters.CallListAdapter;
import zoro.benojir.callrecorder.data.CallListViewModel;
import zoro.benojir.callrecorder.databinding.ActivityMainBinding;
import zoro.benojir.callrecorder.helpers.CustomFunctions;
import zoro.benojir.callrecorder.helpers.PinPreferencesHelper;
import zoro.benojir.callrecorder.services.SmsObserverService;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private static final int REQUEST_PERMISSION_CODE = 4528;
    public static final String TAG = "MADARA";
    private boolean doubleBackPressed;
    public static MenuItem searchBtn, settingsBtn, selectedItemsCountMenu;
    private SharedPreferences preferences;
    private String lastAppearance;
    private boolean isGoingToBackground = false;

    // ✅ New: Call list components
    private CallListAdapter adapter;
    private CallListViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔒 Session control
        ProcessLifecycleOwner.get().getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            if (event == Lifecycle.Event.ON_STOP) {
                isGoingToBackground = true;
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (isGoingToBackground) {
                        PinPreferencesHelper.INSTANCE.setSessionUnlocked(this, false);
                    }
                }, 700);
            } else if (event == Lifecycle.Event.ON_START) {
                isGoingToBackground = false;
            }
        });

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // 🌓 Theme setup
        String appearancePref = preferences.getString("appearance", "device_default");
        switch (appearancePref) {
            case "dark_mode":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                lastAppearance = "dark_mode";
                break;
            case "light_mode":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                lastAppearance = "light_mode";
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                lastAppearance = "device_default";
                break;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 📩 Request default SMS app
        Intent smsIntent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        smsIntent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, this.getPackageName());
        startActivity(smsIntent);

        // 🛰 Start SMS observer service
        Intent smsObserverIntent = new Intent(this, SmsObserverService.class);
        Log.i("MainActivity", "Starting SMS Observer Service");
        startService(smsObserverIntent);

        // 🔐 Redirect to login if not logged in
        if (!CustomFunctions.isUserLoggedIn(this)) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        // ⚙️ Toolbar setup
        MaterialToolbar toolbar = findViewById(R.id.toolbar_include);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Recordings");
        }

        // ⚙️ StrictMode for file URIs
        StrictMode.VmPolicy.Builder smBuilder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(smBuilder.build());

        // 🧱 Drawer setup
        android.view.View headView = binding.navigationViewMenu.getHeaderView(0);
        if (CustomFunctions.isDarkModeOn(this)) {
            headView.setBackground(AppCompatResources.getDrawable(this, R.drawable.header_bg_night));
        }

        String versionString = getString(R.string.app_version) + BuildConfig.VERSION_NAME;
        ((TextView) headView.findViewById(R.id.header_layout_version_tv)).setText(versionString);

        Button updateBtn = headView.findViewById(R.id.updateBtnInHeaderLayout);
        CustomFunctions.checkForUpdateOnStartApp(this, updateBtn);
        updateBtn.setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_release_page_link)));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        });

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, binding.getRoot(), toolbar, 0, 0);
        binding.getRoot().addDrawerListener(toggle);
        toggle.syncState();

        navigationViewItemsClickedActions();

        if (!CustomFunctions.isSystemApp(this)) {
            showNonSystemAppWarning();
        } else {
            checkPermissions();
            setupNewCallListUI();
            androidx.work.PeriodicWorkRequest cleanupWork =
                    new androidx.work.PeriodicWorkRequest.Builder(
                            zoro.benojir.callrecorder.workers.DataCleanupWorker.class,
                            1, java.util.concurrent.TimeUnit.DAYS
                    ).build();

            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "data_cleanup",
                    androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                    cleanupWork
            );
        }
    }

    // ✅ Replaces the old recordings list with the new call list
    private void setupNewCallListUI() {
        binding.nothingFoundDesignContainer.setVisibility(android.view.View.GONE);
        binding.recyclerView.setVisibility(android.view.View.VISIBLE);

        // RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);

        // Adapter and ViewModel
        adapter = new CallListAdapter(
                new ArrayList<>(),
                new kotlin.jvm.functions.Function1<java.io.File, kotlin.Unit>() {
                    @Override
                    public kotlin.Unit invoke(java.io.File file) {
                        new zoro.benojir.callrecorder.dialogs.AudioPlayerDialog(MainActivity.this, file);
                        return kotlin.Unit.INSTANCE;
                    }
                }
        );
        binding.recyclerView.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(CallListViewModel.class);
        viewModel.getCallRecordsLiveData().observe(this, callRecords -> {
            adapter.update(callRecords);
        });

        viewModel.loadAllRecords();
    }

    // ⚠️ Not system app warning
    private void showNonSystemAppWarning() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.not_system_app_message_title));
        builder.setMessage(getString(R.string.not_system_app_message_body));
        builder.setIcon(R.drawable.error);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", (dialog, which) -> {
            dialog.dismiss();
            finishAndRemoveTask();
        });
        builder.setNegativeButton("Read Post", (dialog, which) -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.tutorial_post_link))));
                new Handler(Looper.getMainLooper()).postDelayed(this::finishAndRemoveTask, 2000);
            } catch (Exception e) {
                Toast.makeText(this, "No app found to open this link", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                onBackPressed();
            }
        });
        builder.create();
        builder.show();
    }

    private void navigationViewItemsClickedActions() {
        binding.navigationViewMenu.setNavigationItemSelectedListener(item -> {
            Intent intent;
            if (item.getItemId() == R.id.nav_sms_list) {
                intent = new Intent(MainActivity.this, SmsListActivity.class);
                startActivity(intent);
                return true;
            } else if (item.getItemId() == R.id.nav_call_list) {
                intent = new Intent(MainActivity.this, CallListActivity.class);
                startActivity(intent);
                return true;
            } else {
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar_menu, menu);
        searchBtn = menu.findItem(R.id.menu_search_action);
        settingsBtn = menu.findItem(R.id.menu_settings_action);
        selectedItemsCountMenu = menu.findItem(R.id.menu_selected_items_count);
        searchBtn.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menu_settings_action) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            intent.putExtra("activity_started_by", getPackageName());
            startActivity(intent);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void checkPermissions() {
        boolean granted = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;

        if (!granted) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.READ_CONTACTS
            }, REQUEST_PERMISSION_CODE);
        }
    }

    @SuppressLint("BatteryLife")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_PERMISSION_CODE || grantResults.length < 2
                || grantResults[0] != PackageManager.PERMISSION_GRANTED
                || grantResults[1] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please allow all permissions", Toast.LENGTH_SHORT).show();
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
            } catch (Exception e) {
                CustomFunctions.simpleAlert(this, "Error",
                        getString(R.string.app_info_page_opening_failed_message),
                        "OK", AppCompatResources.getDrawable(this, R.drawable.error));
            }
        } else {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            String pkg = getPackageName();
            if (!pm.isIgnoringBatteryOptimizations(pkg)) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + pkg));
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (PinPreferencesHelper.INSTANCE.isPinEnabled(this)) {
            boolean isUnlocked = PinPreferencesHelper.INSTANCE.isSessionUnlocked(this);
            boolean shouldRelock = PinPreferencesHelper.INSTANCE.shouldRelock(this, 5);
            if (!isUnlocked || shouldRelock) {
                Intent lockIntent = new Intent(this, PinLockActivity.class);
                startActivity(lockIntent);
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        String appearance = preferences.getString("appearance", "device_default");
        if (!lastAppearance.equalsIgnoreCase(appearance)) {
            recreate();
        }
    }

    @Override
    public void onBackPressed() {
        if (binding.getRoot().isDrawerOpen(GravityCompat.START)) {
            binding.getRoot().closeDrawer(GravityCompat.START);
        } else {
            if (doubleBackPressed) {
                super.onBackPressed();
                finish();
            } else {
                this.doubleBackPressed = true;
                Snackbar.make(binding.getRoot(), "Double back press to exit.", Snackbar.LENGTH_LONG).show();
                new Handler(Looper.getMainLooper()).postDelayed(() -> doubleBackPressed = false, 2000);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PinPreferencesHelper.INSTANCE.setSessionUnlocked(this, false);
    }
}
