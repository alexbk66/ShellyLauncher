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
    private TextView status;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createFallbackScreen();

        SharedPreferences preferences = getSharedPreferences("launch_guard", MODE_PRIVATE);
        int boot;
        try {
            boot = Settings.Global.getInt(getContentResolver(), Settings.Global.BOOT_COUNT, -1);
        } catch (SecurityException unavailable) {
            boot = -1;
        }
        if (AutoLaunchPolicy.shouldAttempt(preferences.getBoolean("attempted", false),
                preferences.getInt("boot", -1), boot)) {
            // Commit before startActivity: even a target crash/process death must not loop.
            if (preferences.edit().putBoolean("attempted", true).putInt("boot", boot).commit()) {
                launchShelly();
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
        launch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchShelly();
            }
        });
        layout.addView(launch);
        setContentView(layout);
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
