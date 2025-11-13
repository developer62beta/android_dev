package com.wifi.attendance;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private DataBase db;
    private ListView listViewHistory;
    private Button btnPrev, btnNext, btnAdd;
    private TextView pageInfo;

    private ArrayList<String> historyList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private int currentPage = 0;
    private static final int PAGE_SIZE = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        db = new DataBase(this);

        listViewHistory = findViewById(R.id.listViewHistory);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnAdd = findViewById(R.id.btnAdd);
        pageInfo = findViewById(R.id.pageInfo);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historyList);
        listViewHistory.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> showManualEntryDialog());
        btnPrev.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                loadAttendance();
            }
        });
        btnNext.setOnClickListener(v -> {
            currentPage++;
            loadAttendance();
        });

        // delete on long press
        listViewHistory.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDelete(position);
            return true;
        });

        loadAttendance();
    }

    private void loadAttendance() {
        historyList.clear();

        int offset = currentPage * PAGE_SIZE;
        Cursor cursor = db.getPagedWork(PAGE_SIZE, offset);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String date = cursor.getString(cursor.getColumnIndexOrThrow("date"));
                String time = cursor.getString(cursor.getColumnIndexOrThrow("time_work"));
                historyList.add("ID: " + id + " | 📅 " + date + " | ⏰ " + time);
            } while (cursor.moveToNext());
            cursor.close();
        } else {
            historyList.add("No records found");
        }

        adapter.notifyDataSetChanged();
        updatePageInfo();
    }

    private void updatePageInfo() {
        int total = db.getTotalCount();
        int totalPages = (int) Math.ceil((double) total / PAGE_SIZE);
        pageInfo.setText("Page " + (currentPage + 1) + " of " + Math.max(totalPages, 1));

        btnPrev.setEnabled(currentPage > 0);
        btnNext.setEnabled((currentPage + 1) * PAGE_SIZE < total);
    }

    private void showManualEntryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_entry, null);
        EditText dateInput = dialogView.findViewById(R.id.inputDate);
        Spinner shiftSpinner = dialogView.findViewById(R.id.spinnerShift);

        // default date
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
        dateInput.setText(today);

        new AlertDialog.Builder(this)
                .setTitle("Manual Entry")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String date = dateInput.getText().toString().trim();
                    String time = shiftSpinner.getSelectedItem().toString().toLowerCase(Locale.ROOT);
                    db.insertWork(date, time);
                    Toast.makeText(this, "Entry added", Toast.LENGTH_SHORT).show();
                    loadAttendance();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(int position) {
        String item = historyList.get(position);
        if (!item.startsWith("ID:")) return; // skip "No records found"

        int id = Integer.parseInt(item.split(" ")[1]);

        new AlertDialog.Builder(this)
                .setTitle("Delete Entry")
                .setMessage("Are you sure you want to delete this record?")
                .setPositiveButton("Delete", (d, w) -> {
                    db.deleteWorkById(id);
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadAttendance();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
