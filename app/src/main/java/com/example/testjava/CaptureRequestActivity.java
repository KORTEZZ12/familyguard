package com.example.testjava;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class CaptureRequestActivity extends Activity {

    private static final int REQ = 777;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);


        Log.d("USAGE_SERVICE","CRActivity onCreate");
        MediaProjectionManager mpm =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        // запускаем СИСТЕМНЫЙ диалог
        startActivityForResult(mpm.createScreenCaptureIntent(), REQ);
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ && res == RESULT_OK && data != null) {

            Intent svc = new Intent(this, ScreenshotService.class)
                    .putExtra("resultCode", res)
                    .putExtra("resultData", data);
            ContextCompat.startForegroundService(this, svc);
        }
        finish();      // activity-обёртка закрывается
    }
}

