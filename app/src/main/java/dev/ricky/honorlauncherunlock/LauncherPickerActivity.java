package dev.ricky.honorlauncherunlock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class LauncherPickerActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLauncherPicker();
    }

    private void showLauncherPicker() {
        List<LauncherCandidate> launchers = queryLauncherCandidates();
        if (launchers.isEmpty()) {
            showMessageAndFinish("No launcher apps found");
            return;
        }

        CharSequence[] labels = new CharSequence[launchers.size()];
        for (int i = 0; i < launchers.size(); i++) {
            labels[i] = launchers.get(i).displayLabel;
        }

        new AlertDialog.Builder(this)
                .setTitle("Select default launcher")
                .setItems(labels, (dialog, which) -> {
                    LauncherCandidate launcher = launchers.get(which);
                    if (setDefaultHome(launcher.componentName)) {
                        Toast.makeText(this, "Set: " + launcher.displayLabel, Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        Toast.makeText(this, "Failed. Check root permission.", Toast.LENGTH_LONG)
                                .show();
                    }
                    finish();
                    overridePendingTransition(0, 0);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                    overridePendingTransition(0, 0);
                })
                .setOnCancelListener(dialog -> {
                    finish();
                    overridePendingTransition(0, 0);
                })
                .show();
    }

    private List<LauncherCandidate> queryLauncherCandidates() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);

        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(homeIntent, 0);
        List<LauncherCandidate> launchers = new ArrayList<>();
        for (ResolveInfo resolveInfo : resolveInfos) {
            ActivityInfo activityInfo = resolveInfo.activityInfo;
            if (activityInfo == null || activityInfo.packageName == null || activityInfo.name == null) {
                continue;
            }

            ComponentName componentName = new ComponentName(
                    activityInfo.packageName,
                    activityInfo.name);
            CharSequence label = resolveInfo.loadLabel(packageManager);
            String displayLabel = label == null || label.length() == 0
                    ? activityInfo.packageName
                    : label.toString();
            launchers.add(new LauncherCandidate(componentName, displayLabel));
        }

        Collections.sort(launchers, (left, right) ->
                left.displayLabel.compareToIgnoreCase(right.displayLabel));
        return launchers;
    }

    private boolean setDefaultHome(ComponentName componentName) {
        String flattenedComponent = componentName.flattenToString();
        String command = "cmd package set-home-activity " + shellQuote(flattenedComponent);
        return runRootCommand(command);
    }

    private boolean runRootCommand(String command) {
        Process process = null;
        try {
            process = new ProcessBuilder("su").redirectErrorStream(true).start();
            try (DataOutputStream stdin = new DataOutputStream(process.getOutputStream())) {
                stdin.write((command + "\n").getBytes(StandardCharsets.UTF_8));
                stdin.write("exit\n".getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            }

            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                return false;
            }

            String output = readOutput(process);
            return process.exitValue() == 0 && !output.toLowerCase().contains("exception");
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String readOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
        }
        return output.toString();
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private void showMessageAndFinish(String message) {
        TextView textView = new TextView(this);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        textView.setText(message);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(padding, padding, padding, padding);
        setContentView(textView);
        textView.postDelayed(this::finish, 1500);
    }

    private static final class LauncherCandidate {
        final ComponentName componentName;
        final String displayLabel;

        LauncherCandidate(ComponentName componentName, String displayLabel) {
            this.componentName = componentName;
            this.displayLabel = displayLabel;
        }
    }
}
