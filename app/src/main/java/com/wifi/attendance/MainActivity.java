package com.wifi.attendance;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CODE = 123;
    private static final String PREFS_NAME = "alarm_prefs";

    private Button btwifi, btnHistory, btnAlarmSettings;
    private TextView dayCountText, nightCountText, totalDaysText, monthLabel;
    private TextView dayTimeText, nightTimeText; // new text views
    private ProgressBar attendanceCircle;
    private DataBase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI
        btwifi = findViewById(R.id.wifiAdd);
        btnHistory = findViewById(R.id.historyText);
        btnAlarmSettings = findViewById(R.id.btnAlarmSettings);
        dayCountText = findViewById(R.id.dayCount);
        nightCountText = findViewById(R.id.nightCount);
        totalDaysText = findViewById(R.id.totalDaysText);
        monthLabel = findViewById(R.id.monthLabel);
        attendanceCircle = findViewById(R.id.attendanceCircle);
        dayTimeText = findViewById(R.id.dayTimeText);
        nightTimeText = findViewById(R.id.nightTimeText);

        db = new DataBase(this);

        // Buttons
        btwifi.setOnClickListener(v -> startActivity(new Intent(this, WifiActivity.class)));
        btnHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryActivity.class)));
        btnAlarmSettings.setOnClickListener(v -> startActivity(new Intent(this, AlarmSettingsActivity.class)));

        // Setup
        checkPermissions();
        updateAttendanceSummary();
        updateAlarmTimeLabels(); // show set times on load
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAttendanceSummary();
        updateAlarmTimeLabels(); // refresh after user changes time
    }

    private void updateAttendanceSummary() {
        // Fetch only current month data
        int dayCount = db.getDayCountThisMonth();
        int nightCount = db.getNightCountThisMonth();
        int totalCount = db.getTotalCountThisMonth();

        // Update UI
        dayCountText.setText("DAYS: " + dayCount);
        nightCountText.setText("NIGHT: " + nightCount);
        totalDaysText.setText(totalCount + " days");

        // Current month name
        Calendar cal = Calendar.getInstance();
        String monthName = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
        monthLabel.setText("Month: " + monthName);

        // Progress bar
        int percent = 0;
        if (totalCount > 0) {
            percent = (int) (((float) dayCount / (float) totalCount) * 100);
        }
        attendanceCircle.setProgress(percent);
    }

    private void updateAlarmTimeLabels() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int dH = prefs.getInt("dayHour", 8);
        int dM = prefs.getInt("dayMin", 0);
        int nH = prefs.getInt("nightHour", 20);
        int nM = prefs.getInt("nightMin", 0);

        String dayTime = String.format(Locale.getDefault(), "Day: %02d:%02d", dH, dM);
        String nightTime = String.format(Locale.getDefault(), "Night: %02d:%02d", nH, nM);

        dayTimeText.setText(dayTime);
        nightTimeText.setText(nightTime);
    }

    private void checkPermissions() {
        String[] perms = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.POST_NOTIFICATIONS
        };

        boolean allGranted = true;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, perms, REQ_CODE);
        } else {
            AlarmSetup.scheduleDailyAlarms(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CODE) {
            boolean granted = true;
            for (int res : grantResults)
                if (res != PackageManager.PERMISSION_GRANTED) granted = false;

            if (granted) {
                Toast.makeText(this, "Permissions granted, scheduling alarms", Toast.LENGTH_SHORT).show();
                AlarmSetup.scheduleDailyAlarms(this);
            } else {
                Toast.makeText(this, "Permissions required for Wi-Fi attendance", Toast.LENGTH_LONG).show();
            }
        }
    }
}
