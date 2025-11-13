package com.wifi.attendance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.content.ContextCompat;

public class WifiSchedulerReceiver extends BroadcastReceiver {

    private static final String TAG = "WifiSchedulerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "⏰ Alarm triggered — starting Wi-Fi Scan Service");

        // ✅ Start Wi-Fi scanning service
        Intent serviceIntent = new Intent(context, WifiScanService.class);
        ContextCompat.startForegroundService(context, serviceIntent);

        // ✅ Determine which alarm (day/night) fired
        int requestCode = intent.getIntExtra("requestCode", 1);

        if (requestCode == 1) {
            // Re-schedule Day alarm for next day
            AlarmSetup.rescheduleAfterTrigger(context, 8, 0, 1);
            Log.i(TAG, "🔁 Rescheduled next Day alarm at 8:00 AM");
        } else {
            // Re-schedule Night alarm for next day
            AlarmSetup.rescheduleAfterTrigger(context, 20, 0, 2);
            Log.i(TAG, "🔁 Rescheduled next Night alarm at 8:00 PM");
        }
    }
}
