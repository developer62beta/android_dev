package com.wifi.attendance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
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

    // Updated wake lock type (NO DEPRECATION)
    private PowerManager.WakeLock wakeLock;

    private static final int SCAN_INTERVAL_MS = 10 * 1000;
    private static final int MAX_SCAN_DURATION_MS = 5 * 60 * 1000;
    private long startTime;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();

        acquireWakeLock();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        db = new DataBase(this);
        handler = new Handler();
        startTime = System.currentTimeMillis();

        createNotificationChannel();
        startForeground(1, buildNotification("Wi-Fi Attendance active"));

        scanTask = () -> {

            if (System.currentTimeMillis() - startTime >= MAX_SCAN_DURATION_MS) {
                Log.i(TAG, "⛔ Scan timeout (5 min) — Stopping service");
                stopSelf();
                return;
            }

            wifiScanRecords();
            new Thread(this::performWifiScan).start();
            handler.postDelayed(scanTask, SCAN_INTERVAL_MS);
        };

        handler.post(scanTask);
    }

    // -----------------------------
    //   SAFE WAKE LOCK (No warning)
    // -----------------------------
    private void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,          // CPU stays ON
                "WifiAttendance:PartialWakeLock"
        );

        try {
            wakeLock.acquire(10 * 60 * 1000L); // Max 10 minutes
            Log.i(TAG, "🔐 WakeLock acquired");
        } catch (Exception e) {
            Log.e(TAG, "WakeLock error: " + e.getMessage());
        }
    }

    // -----------------------------
    //  MAIN WIFI SCAN FUNCTION
    // -----------------------------
    private void performWifiScan() {

        // Required permission check (fixes warning)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "❌ Missing ACCESS_FINE_LOCATION permission");
            stopSelf();
            return;
        }

        try {
            String targetWifi = db.getWifi();
            if (targetWifi == null || targetWifi.isEmpty()) {
                Log.w(TAG, "❌ No Wi-Fi saved in DB");
                stopSelf();
                return;
            }

            // Turn ON Wi-Fi if needed
            if (!wifiManager.isWifiEnabled()) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    wifiManager.setWifiEnabled(true);
                } else {
                    Intent panelIntent = new Intent(Settings.Panel.ACTION_WIFI);
                    panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(panelIntent);
                    return;
                }
            }

            boolean started = wifiManager.startScan();
            if (!started) {
                Log.w(TAG, "⚠️ Scan could not start");
                return;
            }

            List<ScanResult> results = wifiManager.getScanResults();

            for (ScanResult result : results) {
                if (result.SSID.equalsIgnoreCase(targetWifi)) {
                    markPresent(targetWifi);
                    return;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Scan error: " + e.getMessage());
        }
    }

    // -----------------------------
    //   MARK PRESENT
    // -----------------------------
    private void markPresent(String ssid) {
        Log.i(TAG, "✅ Found " + ssid);

        try {
            String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            String shift = (hour >= 1 && hour < 12) ? "day" : "night";

            if (!db.isWorkAlreadyMarked(date, shift)) {
                db.insertWork(date, shift);
            }

            // Turn off WiFi for older devices
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && wifiManager.isWifiEnabled()) {
                wifiManager.setWifiEnabled(false);
            }

        } catch (Exception e) {
            Log.e(TAG, "⚠️ Error marking: " + e.getMessage());
        }

        stopSelf();
    }

    // -----------------------------
    // Save scan record to DB
    // -----------------------------
    private void wifiScanRecords() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        db.insertWifiScan(date, time);
    }

    // -----------------------------
    // Notification
    // -----------------------------
    private Notification buildNotification(String msg) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Wi-Fi Attendance")
                .setContentText(msg)
                .setSilent(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Wi-Fi Attendance Channel",
                    NotificationManager.IMPORTANCE_MIN
            );
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    // -----------------------------
    // ON DESTROY
    // -----------------------------
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            Log.i(TAG, "🔓 WakeLock released");
        }

        handler.removeCallbacks(scanTask);
        Log.i(TAG, "🛑 Service stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
