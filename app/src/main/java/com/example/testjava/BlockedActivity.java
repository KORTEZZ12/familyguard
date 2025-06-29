package com.example.testjava;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.view.View;
public class BlockedActivity extends Activity {

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        setContentView(R.layout.activity_blocked);

        String pkg = getIntent().getStringExtra("pkg");
        ((TextView)findViewById(R.id.blockMsg))
                .setText("Лимит времени для «" + pkg + "» исчерпан");

        // Закрыть по нажатию
        findViewById(R.id.btnOk).setOnClickListener(v -> finish());
    }

    @Override public void onBackPressed() {/*Должно быть пустым чтобы пользователь не вышел назад*/}
}