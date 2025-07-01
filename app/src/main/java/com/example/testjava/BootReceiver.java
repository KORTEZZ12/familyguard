package com.example.testjava;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.test.platform.app.InstrumentationRegistry;

public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Bundle args = InstrumentationRegistry.getArguments();
        boolean isTest = args != null && "true".equals(args.getString("IS_TEST"));
        if (!isTest && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent svc = new Intent(context, UsageStatsService.class);
            ContextCompat.startForegroundService(context, svc);
        }
    }
}
