package com.wifi.attendance;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

public class AlarmSettingsActivity extends AppCompatActivity {

    private TimePicker timeDay, timeNight;
    // private Button btnSave;
    private SharedPreferences prefs;
    private static final String PREFS = "alarm_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_settings);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        timeDay = findViewById(R.id.timeDay);
        timeNight = findViewById(R.id.timeNight);
        // btnSave = findViewById(R.id.btnSaveAlarm);

        timeDay.setIs24HourView(true);
        timeNight.setIs24HourView(true);

        // load existing
        int dH = prefs.getInt("dayHour", 8);
        int dM = prefs.getInt("dayMin", 0);
        int nH = prefs.getInt("nightHour", 20);
        int nM = prefs.getInt("nightMin", 0);
        timeDay.setHour(dH); timeDay.setMinute(dM);
        timeNight.setHour(nH); timeNight.setMinute(nM);

        findViewById(R.id.btnSaveAlarm).setOnClickListener(v -> {
            prefs.edit()
                    .putInt("dayHour", timeDay.getHour())
                    .putInt("dayMin", timeDay.getMinute())
                    .putInt("nightHour", timeNight.getHour())
                    .putInt("nightMin", timeNight.getMinute())
                    .apply();

            Toast.makeText(this, "Alarms updated!", Toast.LENGTH_SHORT).show();
            AlarmSetup.scheduleDailyAlarms(this); // re-schedule both
            finish();
        });
    }
}
