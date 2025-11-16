package com.wifi.attendance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class WifiSchedulerReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiSchedulerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {

        SharedPreferences p = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE);

        boolean enabled = p.getBoolean("alarm_enabled", true);

        if (!enabled) {
            Log.i("WifiSchedulerReceiver", "⛔ Attendance disabled — alarm ignored");
            return;
        }


        Log.i(TAG, "⏰ Alarm triggered — starting Wi-Fi Scan Service");
        Log.i(TAG, "⏰ Alarm triggered - starting main activity");

        // ⭐ Launch MainActivity to wake screen
        Intent openMain = new Intent(context, MainActivity.class);
        openMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(openMain);
        Log.i(TAG, "⏰ Alarm triggered - started main activity");

        // ✅ Start Wi-Fi scanning service
        Intent serviceIntent = new Intent(context, WifiScanService.class);
        ContextCompat.startForegroundService(context, serviceIntent);


        // Re-schedule Day alarm for next day
        AlarmSetup.rescheduleAfterTrigger(context, 8, 0, 1);
        Log.i(TAG, "🔁 Rescheduled next Day alarm at 8:00 AM");

        // Re-schedule Night alarm for next day
        AlarmSetup.rescheduleAfterTrigger(context, 20, 0, 2);
        Log.i(TAG, "🔁 Rescheduled next Night alarm at 8:00 PM");
    }
}
