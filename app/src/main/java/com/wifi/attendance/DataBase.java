package com.wifi.attendance;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DataBase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "wifi_attendance.db";
    private static final int DATABASE_VERSION = 2; // incremented version for new table

    // Table names//
    private static final String TABLE_WIFI = "wifi";
    private static final String TABLE_WORK = "work";
    private static final String TABLE_WIFI_SCAN = "wifi_scan";

    // Columns - wifi
    private static final String COL_WIFI_ID = "id";
    private static final String COL_WIFI_SSID = "ssid";

    // Columns - work
    private static final String COL_WORK_ID = "id";
    private static final String COL_WORK_DATE = "date";
    private static final String COL_WORK_TIME = "time_work";

    // Columns - wifi_scan
    private static final String COL_SCAN_ID = "id";
    private static final String COL_SCAN_DATE = "active_date";
    private static final String COL_SCAN_TIME = "scan_time";

    public DataBase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_WIFI_TABLE = "CREATE TABLE " + TABLE_WIFI + " ("
                + COL_WIFI_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_WIFI_SSID + " TEXT UNIQUE)";

        String CREATE_WORK_TABLE = "CREATE TABLE " + TABLE_WORK + " ("
                + COL_WORK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_WORK_DATE + " TEXT, "
                + COL_WORK_TIME + " TEXT)";

        String CREATE_WIFI_SCAN_TABLE = "CREATE TABLE " + TABLE_WIFI_SCAN + " ("
                + COL_SCAN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_SCAN_DATE + " TEXT, "
                + COL_SCAN_TIME + " TEXT)";

        db.execSQL(CREATE_WIFI_TABLE);
        db.execSQL(CREATE_WORK_TABLE);
        db.execSQL(CREATE_WIFI_SCAN_TABLE);

        Log.i("DatabaseHelper", "Tables created successfully");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIFI);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORK);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WIFI_SCAN);
        onCreate(db);
    }

    // ---------------------------
    //  Insert operations
    // ---------------------------

    public long insertWifi(String ssid) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WIFI, null, null); // Keep only one SSID
        ContentValues values = new ContentValues();
        values.put(COL_WIFI_SSID, ssid);
        long id = db.insert(TABLE_WIFI, null, values);
        db.close();
        return id;
    }

    public String getWifi() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_WIFI_SSID + " FROM " + TABLE_WIFI + " LIMIT 1", null);
        String ssid = null;
        if (cursor.moveToFirst()) {
            ssid = cursor.getString(0);
        }
        cursor.close();
        db.close();
        return ssid;
    }

    public long insertWork(String date, String timeWork) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_WORK_DATE, date);
        values.put(COL_WORK_TIME, timeWork);
        long id = db.insert(TABLE_WORK, null, values);
        db.close();
        return id;
    }

    public boolean isWorkAlreadyMarked(String date, String timeWork) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id FROM " + TABLE_WORK + " WHERE " + COL_WORK_DATE + "=? AND " + COL_WORK_TIME + "=?",
                new String[]{date, timeWork}
        );
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    // ==============================
    //  Wi-Fi Scan Log Operations
    // ==============================

    public long insertWifiScan(String date, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SCAN_DATE, date);
        values.put(COL_SCAN_TIME, time);
        long id = db.insert(TABLE_WIFI_SCAN, null, values);
        db.close();
        return id;
    }

    public Cursor getAllWifiScan() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_WIFI_SCAN + " ORDER BY " + COL_SCAN_ID + " DESC", null);
    }

    public void clearWifiScan() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WIFI_SCAN, null, null);
        db.close();
    }

    public String getLastWifiScanTime() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + COL_SCAN_DATE + ", " + COL_SCAN_TIME +
                " FROM " + TABLE_WIFI_SCAN + " ORDER BY " + COL_SCAN_ID + " DESC LIMIT 1", null);
        String lastScan = null;
        if (cursor.moveToFirst()) {
            lastScan = cursor.getString(0) + " " + cursor.getString(1);
        }
        cursor.close();
        db.close();
        return lastScan;
    }

    // ---------------------------
    //  Query operations
    // ---------------------------

    public Cursor getPagedWork(int limit, int offset) {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_WORK + " ORDER BY id DESC LIMIT " + limit + " OFFSET " + offset, null);
    }

    public void deleteWorkById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WORK, "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public Cursor getAllWifi() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_WIFI, null);
    }

    public Cursor getAllWork() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_WORK + " ORDER BY " + COL_WORK_ID + " DESC", null);
    }

    public int getDayCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WORK + " WHERE " + COL_WORK_TIME + "='day'", null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public int getNightCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WORK + " WHERE " + COL_WORK_TIME + "='night'", null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    public int getTotalCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_WORK, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // ===============================
    //  Current Month Summary Methods
    // ===============================

    public int getDayCountThisMonth() {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT COUNT(*) FROM " + TABLE_WORK +
                " WHERE " + COL_WORK_TIME + "='day' " +
                " AND strftime('%m', " + COL_WORK_DATE + ") = strftime('%m', 'now')" +
                " AND strftime('%Y', " + COL_WORK_DATE + ") = strftime('%Y', 'now')";

        Cursor c = db.rawQuery(query, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        db.close();
        return count;
    }

    public int getNightCountThisMonth() {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT COUNT(*) FROM " + TABLE_WORK +
                " WHERE " + COL_WORK_TIME + "='night' " +
                " AND strftime('%m', " + COL_WORK_DATE + ") = strftime('%m', 'now')" +
                " AND strftime('%Y', " + COL_WORK_DATE + ") = strftime('%Y', 'now')";

        Cursor c = db.rawQuery(query, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        db.close();
        return count;
    }

    public int getTotalCountThisMonth() {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT COUNT(*) FROM " + TABLE_WORK +
                " WHERE strftime('%m', " + COL_WORK_DATE + ") = strftime('%m', 'now')" +
                " AND strftime('%Y', " + COL_WORK_DATE + ") = strftime('%Y', 'now')";

        Cursor c = db.rawQuery(query, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        db.close();
        return count;
    }

    // ==============================
    //  Delete and Clear Operations
    // ==============================

    public void deleteWifiScanById(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WIFI_SCAN, COL_SCAN_ID + "=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public void clearAllWifiScan() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_WIFI_SCAN, null, null);
        db.close();
    }

}
