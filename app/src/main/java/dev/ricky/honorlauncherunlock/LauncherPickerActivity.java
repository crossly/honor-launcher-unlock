package dev.ricky.honorlauncherunlock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
    private static final String TAG = "HonorLauncherUnlock";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showLauncherPicker();
    }

    private void showLauncherPicker() {
        List<LauncherCandidate> launchers = queryLauncherCandidates();
        if (launchers.isEmpty()) {
            showMessageAndFinish("未找到可用桌面应用");
            return;
        }

        CharSequence[] labels = new CharSequence[launchers.size()];
        for (int i = 0; i < launchers.size(); i++) {
            labels[i] = launchers.get(i).displayLabel;
        }

        new AlertDialog.Builder(this)
                .setTitle("选择默认桌面")
                .setItems(labels, (dialog, which) -> {
                    LauncherCandidate launcher = launchers.get(which);
                    dialog.dismiss();
                    confirmLauncher(launcher);
                })
                .setNegativeButton("取消", (dialog, which) -> {
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

    private void confirmLauncher(LauncherCandidate launcher) {
        new AlertDialog.Builder(this)
                .setTitle("设为默认桌面？")
                .setView(createLauncherView(launcher))
                .setNegativeButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                    showLauncherPicker();
                })
                .setPositiveButton("继续", (dialog, which) -> {
                    dialog.dismiss();
                    CommandResult result = setDefaultHome(launcher.componentName);
                    if (result.success) {
                        Toast.makeText(this, "已设置：" + launcher.displayLabel, Toast.LENGTH_SHORT)
                                .show();
                        finish();
                        overridePendingTransition(0, 0);
                    } else {
                        showFailureDialog(launcher, result);
                    }
                })
                .setOnCancelListener(dialog -> showLauncherPicker())
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
            Drawable icon = resolveInfo.loadIcon(packageManager);
            String displayLabel = label == null || label.length() == 0
                    ? activityInfo.packageName
                    : label.toString();
            launchers.add(new LauncherCandidate(componentName, displayLabel, icon));
        }

        Collections.sort(launchers, (left, right) ->
                left.displayLabel.compareToIgnoreCase(right.displayLabel));
        return launchers;
    }

    private CommandResult setDefaultHome(ComponentName componentName) {
        String packageName = componentName.getPackageName();
        String flattenedComponent = componentName.flattenToString();
        String quotedPackage = shellQuote(packageName);
        String quotedComponent = shellQuote(flattenedComponent);
        int userId = android.os.Process.myUid() / 100000;

        List<String> commands = new ArrayList<>();
        commands.add("cmd role add-role-holder --user " + userId
                + " android.app.role.HOME " + quotedPackage);
        commands.add("cmd role add-role-holder android.app.role.HOME " + quotedPackage);
        commands.add("cmd package set-home-activity --user " + userId + " " + quotedPackage);
        commands.add("cmd package set-home-activity " + quotedPackage);
        commands.add("cmd package set-home-activity --user " + userId + " " + quotedComponent);
        commands.add("cmd package set-home-activity " + quotedComponent);

        List<CommandResult> results = new ArrayList<>();
        for (String command : commands) {
            CommandResult result = runRootCommand(command);
            if (result.success) {
                return result;
            }
            results.add(result);
        }

        return CommandResult.combine(results);
    }

    private CommandResult runRootCommand(String command) {
        Process process = null;
        try {
            process = new ProcessBuilder("su").redirectErrorStream(true).start();
            try (DataOutputStream stdin = new DataOutputStream(process.getOutputStream())) {
                stdin.write((command + "\n").getBytes(StandardCharsets.UTF_8));
                stdin.write("exit\n".getBytes(StandardCharsets.UTF_8));
                stdin.flush();
            }

            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                return new CommandResult(false, command, -1, "Command timed out.");
            }

            String output = readOutput(process);
            boolean success = process.exitValue() == 0
                    && !output.toLowerCase().contains("exception")
                    && !output.toLowerCase().contains("error");
            CommandResult result = new CommandResult(success, command, process.exitValue(), output);
            log("set-home result success=" + result.success
                    + " exit=" + result.exitCode
                    + " command=" + result.command
                    + " output=" + result.output);
            return result;
        } catch (Throwable t) {
            log("set-home failed: " + t);
            return new CommandResult(false, command, -1, String.valueOf(t));
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

    private LinearLayout createLauncherView(LauncherCandidate launcher) {
        int horizontalPadding = dp(24);
        int topPadding = dp(8);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(horizontalPadding, topPadding, horizontalPadding, 0);

        ImageView iconView = new ImageView(this);
        iconView.setImageDrawable(launcher.icon);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(56), dp(56));
        iconParams.bottomMargin = dp(12);
        layout.addView(iconView, iconParams);

        TextView nameView = new TextView(this);
        nameView.setText(launcher.displayLabel);
        nameView.setTextSize(18);
        nameView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nameView.setGravity(Gravity.CENTER);
        nameView.setSingleLine(false);
        layout.addView(nameView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        addInfoLine(layout, "包名", launcher.componentName.getPackageName());
        addInfoLine(layout, "组件", launcher.componentName.flattenToShortString());
        return layout;
    }

    private void addInfoLine(LinearLayout layout, String label, String value) {
        TextView infoView = new TextView(this);
        infoView.setText(label + "：" + value);
        infoView.setTextSize(14);
        infoView.setGravity(Gravity.CENTER);
        infoView.setSingleLine(false);
        infoView.setPadding(0, dp(8), 0, 0);
        layout.addView(infoView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
    }

    private void showFailureDialog(LauncherCandidate launcher, CommandResult result) {
        new AlertDialog.Builder(this)
                .setTitle("设置失败")
                .setMessage("未能设置 " + launcher.displayLabel
                        + "\n\nexit=" + result.exitCode
                        + "\n" + result.output)
                .setPositiveButton("返回", (dialog, which) -> {
                    dialog.dismiss();
                    showLauncherPicker();
                })
                .setNegativeButton("关闭", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                    overridePendingTransition(0, 0);
                })
                .show();
    }

    private void showMessageAndFinish(String message) {
        TextView textView = new TextView(this);
        int padding = dp(24);
        textView.setText(message);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(padding, padding, padding, padding);
        setContentView(textView);
        textView.postDelayed(this::finish, 1500);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static void log(String message) {
        android.util.Log.i(TAG, message);
    }

    private static final class LauncherCandidate {
        final ComponentName componentName;
        final String displayLabel;
        final Drawable icon;

        LauncherCandidate(ComponentName componentName, String displayLabel, Drawable icon) {
            this.componentName = componentName;
            this.displayLabel = displayLabel;
            this.icon = icon;
        }
    }

    private static final class CommandResult {
        final boolean success;
        final String command;
        final int exitCode;
        final String output;

        CommandResult(boolean success, String command, int exitCode, String output) {
            this.success = success;
            this.command = command;
            this.exitCode = exitCode;
            this.output = output == null || output.isEmpty() ? "(no output)" : output.trim();
        }

        static CommandResult combine(List<CommandResult> results) {
            StringBuilder command = new StringBuilder();
            StringBuilder output = new StringBuilder();
            int exitCode = -1;
            for (int i = 0; i < results.size(); i++) {
                CommandResult result = results.get(i);
                if (i > 0) {
                    command.append('\n');
                    output.append("\n\n");
                }
                command.append(result.command);
                output.append("attempt ").append(i + 1)
                        .append(" exit=").append(result.exitCode)
                        .append('\n')
                        .append(result.output);
                exitCode = result.exitCode;
            }
            return new CommandResult(
                    false,
                    command.toString(),
                    exitCode,
                    output.toString());
        }
    }
}
