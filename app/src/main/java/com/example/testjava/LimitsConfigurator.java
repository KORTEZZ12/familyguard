package com.example.testjava;


import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
public class LimitsConfigurator {

    private final LimitsStorage storage;

    public LimitsConfigurator(Context context) {
        this.storage = new LimitsStorage(context);
    }

    public void setAppLimit(String pkg, int minutes) {
        if (pkg == null || pkg.isEmpty() || minutes <= 0) {
            throw new IllegalArgumentException(
                    "Неверные параметры: pkg=" + pkg + ", minutes=" + minutes);
        }
        storage.setLimit(pkg, minutes);
        storage.setBlockedToday(pkg, false);
    }

    /** Возвращает текущий лимит (минут) для pkg, или 0, если не задан. */
    public int getAppLimit(String pkg) {
        return storage.getLimit(pkg);
    }

    /** Убирает лимит для pkg (делает 0) и сбрасывает флаг блокировки. */
    public void clearAppLimit(String pkg) {
        storage.setLimit(pkg, 0);
        storage.setBlockedToday(pkg, false);
    }

    /**
     * Возвращает карту «pkg → limit» для всех приложений,
     * лимит которых был когда-либо задан.
     */
    public Map<String,Integer> getAllLimits() {
        Map<String,Integer> result = new HashMap<>();
        // убедитесь, что в LimitsStorage есть метод getAllKeys()
        Set<String> keys = storage.getAllKeys();
        for (String key : keys) {
            if (key.endsWith("_limit")) {
                String pkg  = key.substring(0, key.length() - 6);
                int minutes = storage.getLimit(pkg);
                if (minutes > 0) {
                    result.put(pkg, minutes);
                }
            }
        }
        return result;
    }
}

