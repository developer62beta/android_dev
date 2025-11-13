package com.wifi.attendance;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.Calendar;

public class AlarmSetup {

    private static final String TAG = "AlarmSetup";

    // Schedule both day and night alarms
    public static void scheduleDailyAlarms(Context context) {
        SharedPreferences p = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE);
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // ✅ Check if exact alarms are allowed (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    Log.w(TAG, "⚠️ Exact alarm permission not granted — opening settings");
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                    return;
                }
            }


            int dH = p.getInt("dayHour", 8);
            int dM = p.getInt("dayMin", 0);
            int nH = p.getInt("nightHour", 20);
            int nM = p.getInt("nightMin", 0);

            scheduleExactAlarm(context, alarmManager, dH, dM, 1);
            scheduleExactAlarm(context, alarmManager, nH, nM, 2);


        } catch (SecurityException e) {
            Log.e(TAG, "❌ SecurityException while scheduling alarm: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "❌ Error scheduling alarm: " + e.getMessage());
        }
    }

    private static void scheduleExactAlarm(Context context, AlarmManager alarmManager,
                                           int hour, int minute, int requestCode) {

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        if (cal.getTimeInMillis() < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(context, WifiSchedulerReceiver.class);
        intent.putExtra("requestCode", requestCode);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            // ✅ Use exact alarm that bypasses Doze mode
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.getTimeInMillis(),
                    pi
            );
            Log.i(TAG, "⏰ Alarm set for " + hour + ":" + String.format("%02d", minute));

        } catch (SecurityException e) {
            Log.e(TAG, "🚫 App not allowed to set exact alarms: " + e.getMessage());
        }
    }

    // Called by receiver after alarm triggers
    public static void rescheduleAfterTrigger(Context context, int hour, int minute, int requestCode) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            scheduleExactAlarm(context, alarmManager, hour, minute, requestCode);
        } catch (Exception e) {
            Log.e(TAG, "❌ Error rescheduling alarm: " + e.getMessage());
        }
    }
}
