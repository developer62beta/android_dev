package com.wifi.attendance;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WifiScanService extends Service {

    private static final String TAG = "WifiScanService";
    private static final String CHANNEL_ID = "wifi_attendance_channel";

    private WifiManager wifiManager;
    private Handler handler;
    private Runnable scanTask;
    private DataBase db;

    // Scan settings
    private static final int SCAN_INTERVAL_MS = 10 * 1000; // every 10 seconds
    private static final int MAX_SCAN_DURATION_MS = 5 * 60 * 1000; // 5 minutes
    private long startTime;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        db = new DataBase(this);
        handler = new Handler();
        startTime = System.currentTimeMillis();

        createNotificationChannel();
        startForeground(1, buildNotification("Wi-Fi Attendance active"));

        scanTask = new Runnable() {
            @Override
            public void run() {
                // ⏱ Stop after 5 minutes
                if (System.currentTimeMillis() - startTime >= MAX_SCAN_DURATION_MS) {
                    Log.i(TAG, "⏹️ Stopping scan after 5 minutes");
                    stopSelf();
                    return;
                }
                wifiScanRecords();
                new Thread(() -> performWifiScan()).start(); // Run in background thread
                handler.postDelayed(this, SCAN_INTERVAL_MS);
            }
        };

        handler.post(scanTask);
    }

    private void performWifiScan() {
        try {
            String targetWifi = db.getWifi();
            if (targetWifi == null || targetWifi.isEmpty()) {
                Log.w(TAG, "❌ No Wi-Fi SSID saved in database");
                stopSelf();
                return;
            }

            if (!wifiManager.isWifiEnabled()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    wifiManager.setWifiEnabled(true);
                    Log.i(TAG, "📶 Wi-Fi enabled for scanning");
                } else {
                    Log.w(TAG, "⚠️ Can't enable Wi-Fi programmatically (Android 10+)");
                    Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                    panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(panelIntent);
                    return;
                }
            }

            boolean started = wifiManager.startScan();
            if (!started) {
                Log.w(TAG, "⚠️ Wi-Fi scan could not start");
                return;
            }

            List<ScanResult> results = wifiManager.getScanResults();
            for (ScanResult result : results) {
                Log.d(TAG, "🔍 Found: " + result.SSID);
                if (result.SSID.equalsIgnoreCase(targetWifi)) {
                    markPresent(targetWifi);
                    return; // stop after marking
                }
            }

            Log.d(TAG, "📡 Target Wi-Fi not in range");

        } catch (SecurityException se) {
            Log.e(TAG, "⚠️ Missing permission: " + se.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "❌ Error during Wi-Fi scan", e);
        }
    }

    private void markPresent(String ssid) {
        Log.i(TAG, "✅ Within range of " + ssid + " — Marked Present!");

        try {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            String shift = (hour >= 1 && hour < 12) ? "day" : "night";

            if (!db.isWorkAlreadyMarked(date, shift)) {
                long id = db.insertWork(date, shift);
                if (id != -1)
                    Log.i(TAG, "🗓️ Attendance saved: " + date + " (" + shift + ")");
                else
                    Log.w(TAG, "⚠️ Failed to insert attendance record");
            } else {
                Log.i(TAG, "⚠️ Already marked for " + shift + " shift on " + date);
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
                Log.i(TAG, "📴 Wi-Fi turned OFF (Android 9 or below)");
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Error marking attendance: " + e.getMessage());
        }

        stopSelf(); // stop scanning after success
    }

    private void wifiScanRecords(){

        DataBase db = new DataBase(getApplicationContext());
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        db.insertWifiScan(date, time);
        Log.i(TAG, "✅ Scan Record on DB");
    }

    private Notification buildNotification(String msg) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Wi-Fi Attendance")
                .setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setSilent(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Wi-Fi Attendance Channel",
                    NotificationManager.IMPORTANCE_MIN);
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(scanTask);
        Log.i(TAG, "🛑 WifiScanService destroyed after 5-minute cycle");
    }
}
