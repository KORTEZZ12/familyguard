package com.example.testjava;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {

    public void onReceive(Context ctx, Intent i) {
        Intent svc = new Intent(ctx, UsageStatsService.class);
        ContextCompat.startForegroundService(ctx, svc);
    }
}
