package com.wifi.attendance;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class ScanHistoryActivity extends AppCompatActivity {

    private ListView listView;
    private TextView emptyView;
    private Button btnClearAll;
    private DataBase db;
    private ArrayList<String> scans;
    private ArrayList<Integer> ids;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_history);

        listView = findViewById(R.id.listViewScanHistory);
        emptyView = findViewById(R.id.textViewEmpty);
        btnClearAll = findViewById(R.id.btnClearAll);
        db = new DataBase(this);

        loadScanHistory();

        // Long press to delete single record
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            int scanId = ids.get(position);
            showDeleteDialog(scanId);
            return true;
        });

        // Clear all button click
        btnClearAll.setOnClickListener(v -> showClearAllDialog());
    }

    private void loadScanHistory() {
        scans = new ArrayList<>();
        ids = new ArrayList<>();

        Cursor cursor = db.getAllWifiScan();
        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("active_date"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("scan_time"));
                ids.add(id);
                scans.add(date + "   " + time);
            } while (cursor.moveToNext());
        }
        cursor.close();

        if (scans.isEmpty()) {
            emptyView.setText("No scan data available.");
            listView.setEmptyView(emptyView);
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, scans);
        listView.setAdapter(adapter);
    }

    private void showDeleteDialog(int scanId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Scan Record")
                .setMessage("Do you want to delete this scan entry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    db.deleteWifiScanById(scanId);
                    loadScanHistory();
                    Toast.makeText(this, "Deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearAllDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All History")
                .setMessage("Do you want to delete all scan history?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    db.clearAllWifiScan();
                    loadScanHistory();
                    Toast.makeText(this, "All scan history cleared", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
