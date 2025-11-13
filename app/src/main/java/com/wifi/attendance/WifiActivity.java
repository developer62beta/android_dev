package com.wifi.attendance;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class WifiActivity extends AppCompatActivity {

    private EditText editSsid;
    private Button btnSaveWifi, scanHistory;
    private ListView listViewWifi;

    private DataBase db;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> wifiList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        // Initialize UI
        editSsid = findViewById(R.id.editSsid);
        btnSaveWifi = findViewById(R.id.btnSaveWifi);
        scanHistory = findViewById(R.id.ScanHistory);
        listViewWifi = findViewById(R.id.listViewWifi);

        db = new DataBase(this);
        wifiList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiList);
        listViewWifi.setAdapter(adapter);

        loadWifiList(); // load existing Wi-Fi names

        // Save Wi-Fi button click
        btnSaveWifi.setOnClickListener(v -> {
            String ssid = editSsid.getText().toString().trim();
            if (ssid.isEmpty()) {
                Toast.makeText(WifiActivity.this, "Please enter Wi-Fi name", Toast.LENGTH_SHORT).show();
                return;
            }

            long id = db.insertWifi(ssid);
            if (id != -1) {
                Toast.makeText(WifiActivity.this, "Wi-Fi saved", Toast.LENGTH_SHORT).show();
                editSsid.setText("");
                loadWifiList(); // refresh the list
            } else {
                Toast.makeText(WifiActivity.this, "Wi-Fi already exists", Toast.LENGTH_SHORT).show();
            }
        });

        scanHistory.setOnClickListener(v -> startActivity(new Intent(this, ScanHistoryActivity.class)));
    }

    private void loadWifiList() {
        wifiList.clear();
        Cursor cursor = db.getAllWifi();
        if (cursor != null && cursor.moveToFirst()) {
            do {
                wifiList.add(cursor.getString(1)); // assuming column 1 is SSID
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }
}
