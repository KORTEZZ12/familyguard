package com.example.testjava;

import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.content.Context;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.TextView;
import android.app.AppOpsManager;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import android.provider.Settings;
import android.content.Intent;
import android.os.Process;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.Manifest;
import android.app.ActivityManager;
import android.widget.Toast;

import java.util.ArrayList;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_DOZE     = 11;
    private static final int REQ_OVERLAY  = 12;
    private static final int REQ_USAGE    = 13;
    private static final int REQ_ACCESS   = 14;
    private TextView usageTextView;

    private static final int REQ_FG_MEDIA = 2024;


    private SharedPreferences prefs() {
        return getSharedPreferences("asks_once", MODE_PRIVATE);
    }
    private boolean ensureFgServiceProjection() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return true;                       // до 34-го всё ок
        }
        if (checkSelfPermission(
                Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION)
                == PackageManager.PERMISSION_GRANTED) {
            return true;                       // уже выдано
        }
        requestPermissions(
                new String[]{ Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION },
                REQ_FG_MEDIA);
        return false;                          // ждём ответа пользователя
    }

    @Override
    public void onRequestPermissionsResult(int code, @NonNull String[] p, @NonNull int[] g) {
        super.onRequestPermissionsResult(code, p, g);
        if (code == REQ_FG_MEDIA &&
                g.length > 0 && g[0] == PackageManager.PERMISSION_GRANTED) {
            launchCaptureRequest();            // ваш метод, который запускает пустышку
        }
    }
    private void launchCaptureRequest() {
        Intent i = new Intent(this, CaptureRequestActivity.class);
        startActivity(i);
    }


    private BroadcastReceiver usageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (usageTextView == null) return;

            long totalMin = intent.getLongExtra("totalMin", 0);
                ArrayList<String> perApp = intent.getStringArrayListExtra("perAppList");

                StringBuilder sb = new StringBuilder();
                sb.append("Общее экранное время (24ч): ")
                        .append(totalMin).append(" мин\n\n");
                if (perApp != null) {
                    for (String line : perApp) {
                        sb.append(line).append("\n");
                    }
                }
                usageTextView.setText(sb.toString());
            }
    };


    private void ensureAccessibility() {

        String svc = getPackageName() + "/"
                + AppBlockerService.class.getName();

        String enabled = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        if (enabled != null && enabled.contains(svc)) return;
        if (prefs().getBoolean("askedAcc", false)) return;

        Intent acc = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivityForResult(acc, REQ_ACCESS);
        prefs().edit().putBoolean("askedAcc", true).apply();
    }

    private void ensureOverlay() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        if (Settings.canDrawOverlays(this)) return;

        if (prefs().getBoolean("askedOverlay", false)) return;

        Intent over = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(over, REQ_OVERLAY);
        prefs().edit().putBoolean("askedOverlay", true).apply();
    }

    private void ensureUsageStats() {
        AppOpsManager ops = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) return;

        if (prefs().getBoolean("askedUsage", false)) return;

        Intent usg = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivityForResult(usg, REQ_USAGE);
        prefs().edit().putBoolean("askedUsage", true).apply();
    }


    private void maybeStartScreenshotFlow() {

        if (isShotRunning()) {            // сервис уже жив?   (см. ниже)
            Log.d("USAGE_SERVICE", "service already running");
            return;
        }

        if (!ensureFgServiceProjection()) {
            return;
        }

        Log.d("USAGE_SERVICE", "launch CaptureRequestActivity");
        Intent i = new Intent(this, CaptureRequestActivity.class);
        startActivity(i);
    }


    private boolean isShotRunning() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo info :
                am.getRunningServices(Integer.MAX_VALUE)) {
            if (ScreenshotService.class.getName().equals(info.service.getClassName()))
                return true;
        }
        return false;
    }


        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

            askAllOnce();

            maybeStartScreenshotFlow();
        usageTextView = findViewById(R.id.usageTextView);

        AppOpsManager appOps =
                (AppOpsManager)getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), getPackageName());
        if (mode != AppOpsManager.MODE_ALLOWED) {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            return;
        }


        Intent svcIntent = new Intent(this, UsageStatsService.class);
        ContextCompat.startForegroundService(this, svcIntent);
        Log.d("USAGE_SERVICE", ">>> UsageStatsService стартанул");

        Intent testLimit = new Intent(this, UsageStatsService.class);
        testLimit.setAction(UsageStatsService.ACTION_SET_LIMIT);
        testLimit.putExtra("pkg",   "com.google.android.youtube"); // пакет YouTube
        testLimit.putExtra("limit", 5);                            // 5 минут
        startService(testLimit);


    }

    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);

        switch (req) {

            case REQ_DOZE:
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
                if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Toast.makeText(this,
                            "Без выключения оптимизации батареи Android может остановить сбор статистики",
                            Toast.LENGTH_LONG).show();
                }
                break;

            case REQ_OVERLAY:
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this,
                            "Разрешите «Показ поверх других окон» для корректной работы блокировок",
                            Toast.LENGTH_LONG).show();
                }
                break;

            case REQ_USAGE:
                AppOpsManager ops = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
                int mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        Process.myUid(), getPackageName());
                if (mode != AppOpsManager.MODE_ALLOWED) {
                    Toast.makeText(this,
                            "Доступ к «Статистике использования» нужен для лимитов экранного времени",
                            Toast.LENGTH_LONG).show();
                }
                break;

            case REQ_ACCESS:
                String svc = getPackageName() + "/" + AppBlockerService.class.getName();
                String enabled = Settings.Secure.getString(
                        getContentResolver(),
                        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                if (enabled == null || !enabled.contains(svc)) {
                    Toast.makeText(this,
                            "Включите службу AppBlocker в списке специальных возможностей",
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager
                .getInstance(this)
                .registerReceiver(
                        usageReceiver,
                        new IntentFilter("com.example.UPDATE_USAGE")
                );
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager
                .getInstance(this)
                .unregisterReceiver(usageReceiver);
        super.onStop();
    }

    private void ensureDozeExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm.isIgnoringBatteryOptimizations(getPackageName())) return;

        if (prefs().getBoolean("askedDoze", false)) return;

        Intent i = new Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(i, REQ_DOZE);
        prefs().edit().putBoolean("askedDoze", true).apply();
    }

    private void askAllOnce() {
        ensureDozeExemption();
        ensureAccessibility();
        ensureOverlay();
        ensureUsageStats();
    }
}

