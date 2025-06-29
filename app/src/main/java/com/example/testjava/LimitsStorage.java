package com.example.testjava;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;
import java.util.Set;

public class LimitsStorage {
    private static final String PREFS = "app_time_limits";
    private SharedPreferences prefs;

    public LimitsStorage(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void setLimit(String pkg, int minutes) {
        prefs.edit()
                .putInt(pkg + "_limit", minutes)
                .apply();
    }

    public int getLimit(String pkg) {
        return prefs.getInt(pkg + "_limit", 0);
    }

    public void setBlockedToday(String pkg, boolean blocked) {
        prefs.edit()
                .putBoolean(pkg + "_blocked", blocked)
                .apply();
    }

    public boolean isBlockedToday(String pkg) {
        return prefs.getBoolean(pkg + "_blocked", false);
    }

    public void clearAllBlocked() {
        for (String key : prefs.getAll().keySet()) {
            if (key.endsWith("_blocked")) {
                prefs.edit().remove(key).apply();
            }
        }
    }

    public Set<String> getAllKeys() {
        return prefs.getAll().keySet();
    }

}
