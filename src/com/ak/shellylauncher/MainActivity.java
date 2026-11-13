package com.ak.shellylauncher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** A HOME activity with one automatic Shelly launch per boot and manual retries. */
public final class MainActivity extends Activity {
    public static final String EXTRA_SHOW_LAUNCHER = "com.ak.shellylauncher.SHOW_LAUNCHER";
    private TextView status;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createFallbackScreen();
        handleLaunchIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.getBooleanExtra(EXTRA_SHOW_LAUNCHER, false)) {
            handleLaunchIntent();
        }
    }

    private void handleLaunchIntent() {
        boolean showLauncher = getIntent().getBooleanExtra(EXTRA_SHOW_LAUNCHER, false);
        SharedPreferences preferences = getSharedPreferences("launch_guard", MODE_PRIVATE);
        int boot;
        try {
            boot = Settings.Global.getInt(getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        } catch (SecurityException unavailable) {
            boot = -1;
        }
        boolean automatic = AutoLaunchPolicy.shouldAttempt(
                preferences.getBoolean("attempted", false),
                preferences.getInt("boot", -1), boot, showLauncher);
        if (showLauncher) {
            status.setText("Choose Launch Shelly or Android Settings.");
        }
        if (showLauncher || automatic) {
            // Commit before startActivity: even a target crash/process death must not loop.
            // Deliberately opening the launcher also consumes this boot's auto attempt.
            if (preferences.edit().putBoolean("attempted", true).putInt("boot", boot).commit()) {
                if (automatic) {
                    launchShelly();
                }
            } else {
                status.setText("Could not save the launch guard. Use Launch Shelly to continue.");
            }
        }
    }

    private void createFallbackScreen() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        int padding = (int) (24 * getResources().getDisplayMetrics().density + 0.5f);
        layout.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText("Shelly Launcher");
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        status = new TextView(this);
        status.setText("Ready. Tap Launch Shelly to open Shelly Smart Control.");
        status.setTextSize(18);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, padding, 0, padding);
        layout.addView(status);

        Button launch = new Button(this);
        launch.setText("Launch Shelly");
        launch.setAllCaps(false);
        launch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchShelly();
            }
        });
        Button settings = new Button(this);
        settings.setText("Android Settings");
        settings.setAllCaps(false);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettings();
            }
        });
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.addView(launch, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        controls.addView(settings, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        layout.addView(controls, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        setContentView(layout);
    }

    private void openSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (ActivityNotFoundException unavailable) {
            status.setText("Android Settings is unavailable on this device.");
        } catch (SecurityException blocked) {
            status.setText("Android blocked opening Settings.");
        }
    }

    private void launchShelly() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setComponent(new ComponentName("cloud.shelly.smartcontrol",
                "cloud.shelly.smartcontrol.MainActivity_Default"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            // Keep this HOME activity alive underneath Shelly. Do not retry in onResume
            // or onNewIntent: returning HOME must leave the fallback screen usable.
            startActivity(intent);
            status.setText("Shelly was opened. If it closed, tap Launch Shelly to try again.");
        } catch (ActivityNotFoundException unavailable) {
            status.setText("Shelly Smart Control is unavailable. Install or enable it, then tap Launch Shelly.");
        } catch (SecurityException blocked) {
            status.setText("Android blocked opening Shelly Smart Control. Check that it is enabled and accessible, then retry.");
        }
    }
}
