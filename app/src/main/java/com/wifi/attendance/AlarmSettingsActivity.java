package com.wifi.attendance;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

public class AlarmSettingsActivity extends AppCompatActivity {

    private TimePicker timeDay, timeNight;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch alarmSwitch;
    private SharedPreferences prefs;

    private static final String PREFS = "alarm_prefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_settings);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        timeDay = findViewById(R.id.timeDay);
        timeNight = findViewById(R.id.timeNight);
        alarmSwitch = findViewById(R.id.alarmSwitch);

        timeDay.setIs24HourView(true);
        timeNight.setIs24HourView(true);

        // Load saved times
        int dH = prefs.getInt("dayHour", 8);
        int dM = prefs.getInt("dayMin", 0);
        int nH = prefs.getInt("nightHour", 20);
        int nM = prefs.getInt("nightMin", 0);

        timeDay.setHour(dH);
        timeDay.setMinute(dM);
        timeNight.setHour(nH);
        timeNight.setMinute(nM);

        // Load switch state
        boolean enabled = prefs.getBoolean("alarm_enabled", true);
        alarmSwitch.setChecked(enabled);

        alarmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("alarm_enabled", isChecked).apply();

            if (isChecked) {
                Toast.makeText(this, "Attendance Enabled", Toast.LENGTH_SHORT).show();
                AlarmSetup.scheduleDailyAlarms(this);
            } else {
                Toast.makeText(this, "Attendance Disabled", Toast.LENGTH_SHORT).show();
                AlarmSetup.cancelAllAlarms(this);
            }
        });

        // Save button
        findViewById(R.id.btnSaveAlarm).setOnClickListener(v -> {

            prefs.edit()
                    .putInt("dayHour", timeDay.getHour())
                    .putInt("dayMin", timeDay.getMinute())
                    .putInt("nightHour", timeNight.getHour())
                    .putInt("nightMin", timeNight.getMinute())
                    .apply();

            Toast.makeText(this, "Alarm times updated!", Toast.LENGTH_SHORT).show();

            // Apply new times only if attendance enabled
            if (alarmSwitch.isChecked()) {
                AlarmSetup.scheduleDailyAlarms(this);
            }

            finish();
        });
    }
}
