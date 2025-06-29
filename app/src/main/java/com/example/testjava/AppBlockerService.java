package com.example.testjava;


import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.content.Intent;
public class AppBlockerService extends AccessibilityService{

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d("BLOCKER", "service connected");
    }
    @Override
    public void onInterrupt() {}

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        Log.d("BLOCKER", "event=" + e.getPackageName());
        int type = e.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED   &&
                type != AccessibilityEvent.TYPE_WINDOWS_CHANGED        &&
                type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            Log.d("BLOCKER", "Ютуб не прошёл");
            return;
        }
        String pkg = String.valueOf(e.getPackageName());
        LimitsStorage store = new LimitsStorage(this);
        Log.d("BLOCKER", "Или Сюда");
        if (store.isBlockedToday(pkg)) {
            Log.d("BLOCKER", "Ютуб прошёл");
            Intent i = new Intent(this, BlockedActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.putExtra("pkg", pkg);
            startActivity(i);
        }
    }
}

