package com.example.testjava;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.app.Notification;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.app.usage.UsageStats;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import java.util.Calendar;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;

public class UsageStatsService extends Service {

    public static final String ACTION_SET_LIMIT = "com.example.SET_LIMIT";
    private static final String CHANNEL_ID = "usage_chan";

    private Runnable task;
    private LimitsStorage store;
    public static final String ACTION_UPDATE = "com.example.UPDATE_USAGE";
    private Handler handler = new Handler(Looper.getMainLooper());



    public void onCreate(){

        store = new LimitsStorage(this);


        // 4. Инициализация Handler и Runnable для 5-минутных запусков
        handler = new Handler(Looper.getMainLooper());
        task = () -> {
            logUsageStats();               // здесь считаются и блокируютсяе пакеты
            handler.postDelayed(task,  60000);
        };
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        // 1) Обработка команды установки лимита
        if (intent != null && ACTION_SET_LIMIT.equals(intent.getAction())) {
            String pkg   = intent.getStringExtra("pkg");
            int    limit = intent.getIntExtra("limit", 0);
            if (pkg != null && limit > 0) {
                store.setLimit(pkg, limit);            // сохраняем новый лимит
                store.setBlockedToday(pkg, false);     // сбрасываем флаг блокировки
                Log.d("USAGE_SERVICE",
                        "Лимит для " + pkg + " = " + limit + " мин");
            }
            // однократная команда — не держим сервис «живым»
            return START_NOT_STICKY;
        }

        // 2) Обычный старт Foreground-сервиса и запуск цикла каждые 5 минут
        startForeground(1, buildNotification());
        handler.post(task);
        return START_STICKY;
    }


    private void logUsageStats() {

        long end   = System.currentTimeMillis();
        long start = localMidnight();            // 00:00 сегодня

        Log.d("USAGE_SERVICE",
                "[" + android.text.format.DateFormat.format("HH:mm:ss", System.currentTimeMillis())
                        + "] tick");

        Map<String, Long> map = collectDurations(start, end);

        long totalMin = 0;
        ArrayList<String> perApp = new ArrayList<>();

        for (Map.Entry<String, Long> e : map.entrySet()) {
            String pkg   = e.getKey();
            long   min   = TimeUnit.MILLISECONDS.toMinutes(e.getValue());
            totalMin    += min;

            int  limit   = store.getLimit(pkg);
            boolean blk  = store.isBlockedToday(pkg);

            Log.d("USAGE_SERVICE", pkg + " used=" + min +
                    " limit=" + limit + " blocked=" + blk);

            if (limit > 0 && min >= limit && !blk) {
                Log.d("USAGE_SERVICE", "Заблокировано: " + pkg);
                store.setBlockedToday(pkg, true);
            }
            perApp.add(pkg + " → " + min + " мин");
        }

        Intent out = new Intent(ACTION_UPDATE)
                .putExtra("totalMin", totalMin)
                .putStringArrayListExtra("perAppList", perApp);
        LocalBroadcastManager.getInstance(this).sendBroadcast(out);
    }


    private long localMidnight() {
        Calendar cal = Calendar.getInstance();   // сейчас, локальный TZ
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE,      0);
        cal.set(Calendar.SECOND,      0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }


    private Notification buildNotification() {

        // Создаём канал только на API ≥26
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Usage Stats Service",
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Сбор экранного времени");
            getSystemService(NotificationManager.class)
                    .createNotificationChannel(ch);
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Родительский контроль активен")
                .setContentText("Сбор экранного времени…")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)            // нельзя смахнуть
                .build();
    }

    private Map<String, Long> collectDurations(long start, long end) {

        UsageStatsManager usm = (UsageStatsManager) getSystemService(USAGE_STATS_SERVICE);
        UsageEvents evs = usm.queryEvents(start, end);

        Map<String, Long> result = new LinkedHashMap<>();
        String fgPkg = null;  long fgTs = 0;

        UsageEvents.Event ev = new UsageEvents.Event();
        while (evs.hasNextEvent()) {
            evs.getNextEvent(ev);

            int t = ev.getEventType();
            if (t == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                fgPkg = ev.getPackageName();
                fgTs  = ev.getTimeStamp();

            } else if (t == UsageEvents.Event.MOVE_TO_BACKGROUND &&
                    fgPkg != null &&
                    fgPkg.equals(ev.getPackageName())) {

                long ms = ev.getTimeStamp() - fgTs;
                result.merge(fgPkg, ms, Long::sum);      // суммируем
                fgPkg = null;
            }
        }

        if (fgPkg != null) {
            long ms = end - fgTs;
            result.merge(fgPkg, ms, Long::sum);
        }

        return result;         // «пакет → миллисекунды в фоне»
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restart = new Intent(getApplicationContext(), UsageStatsService.class);
        restart.setPackage(getPackageName());
        ContextCompat.startForegroundService(this, restart);

        super.onTaskRemoved(rootIntent);
    }

}
