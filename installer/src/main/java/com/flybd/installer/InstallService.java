package com.flybd.installer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.DrawableRes;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class InstallService extends Service {

    private static final String TAG = "InstallService";

    private static final long INTERVAL_TIME = 30 * 1000;

    private static final long REFERRER_INTERVAL_TIME = 3 * 1000;

    private HandlerThread handlerThread = new HandlerThread(TAG);

    private Handler handler;

    public static final String NOTIFICATION_ID = "notification_id";
    public static final String ICON = "icon";
    public static final String INTENT = "intent";
    public static final String TITLE = "title";
    public static final String CONTENT = "content";
    public static final String TICKER = "ticker";

    private boolean isInit = false;

    public static Callback sCallback = null;

    private boolean isReferrerJobRunning = false;

    public static Intent getInstallServiceIntent(Context context, int notificationId, @DrawableRes int icon, Intent intent, String title, String content, String ticker) {
        Intent serviceIntent = new Intent(context, InstallService.class);
        serviceIntent.putExtra(NOTIFICATION_ID, notificationId);
        serviceIntent.putExtra(ICON, icon);
        serviceIntent.putExtra(INTENT, intent);
        serviceIntent.putExtra(TITLE, title);
        serviceIntent.putExtra(CONTENT, content);
        serviceIntent.putExtra(TICKER, ticker);
        return serviceIntent;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        handler.removeCallbacksAndMessages(null);
        handlerThread.quit();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand");
        if (!isInit) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    int notificationId = intent.getIntExtra(NOTIFICATION_ID, -1);
                    int icon = intent.getIntExtra(ICON, -1);
                    Intent targetIntent = intent.getParcelableExtra(INTENT);
                    String title = intent.getStringExtra(TITLE);
                    String content = intent.getStringExtra(CONTENT);
                    String ticker = intent.getStringExtra(TICKER);
                    if (notificationId != -1 || icon != -1) {
                        if (needShowNotification()) {
                            sendNotification(notificationId, icon, targetIntent, title, content, ticker);
                        }
                        handler.postDelayed(this, INTERVAL_TIME);
                    }
                }
            });
            isInit = true;
        }

        if (!isReferrerJobRunning) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Context context = InstallService.this;
                    SharedPreferences pref = context.getSharedPreferences(InstallReferrerReceiver.PREF_REFERRER, MODE_MULTI_PROCESS);
                    String referrer = pref.getString(InstallReferrerReceiver.install_referrer_store_key, "");
                    if (!TextUtils.isEmpty(referrer)) {
                        Log.i(TAG, "referrer is " + referrer);
                        if (saveReferrer(referrer)) {
                            isReferrerJobRunning = false;
                            return;
                        }
                    }
                    handler.postDelayed(this, REFERRER_INTERVAL_TIME);
                }
            });
            isReferrerJobRunning = true;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(TAG, "onTaskRemoved");
        super.onTaskRemoved(rootIntent);
    }

    private boolean saveReferrer(String referrer) {
        FileOutputStream os = null;
        try {
            JSONObject jObj = new JSONObject();
            jObj.put("referrer", referrer);
            File dir = Environment.getExternalStorageDirectory();
            os = new FileOutputStream(new File(dir, InstallReferrerReceiver.PREF_REFERRER));
            os.write(jObj.toString().getBytes());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void sendNotification(int notificationId, @DrawableRes int icon, Intent intent, String title, String content, String ticker) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(BuildConfig.APPLICATION_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);

            Notification.Builder builder = new Notification.Builder(this, BuildConfig.APPLICATION_ID);
            builder.setContentTitle(title);
            builder.setContentText(content);
            builder.setSmallIcon(icon);
            builder.setWhen(System.currentTimeMillis());
            builder.setTicker(ticker);
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);
            builder.setAutoCancel(true);
            builder.setContentIntent(PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT));
            manager.notify(TAG, notificationId, builder.build());
        } else {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, BuildConfig.APPLICATION_ID);
            builder.setContentTitle(title);
            builder.setContentText(content);
            builder.setSmallIcon(icon);
            builder.setWhen(System.currentTimeMillis());
            builder.setTicker(ticker);
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            builder.setAutoCancel(true);
            builder.setContentIntent(PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT));
            NotificationManagerCompat.from(this).notify(TAG, notificationId, builder.build());
        }
    }

    protected boolean needShowNotification() {
        if (sCallback != null) {
            return sCallback.needShowNotification();
        }
        return true;
    }

    public interface Callback {
        boolean needShowNotification();
    }
}
