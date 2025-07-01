package com.example.testjava;

import android.app.Activity;
import android.os.Bundle;

/**
 * Тестовая пустая Activity без всякой логики.
 */
public class TestEmptyActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Не вызываем setContentView — экран останется полностью пустым,
        // но Activity запустится мгновенно и не будет блокировать UI-поток.
    }
}
