package com.nayab.expensetracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class Databasehelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "ExpenseTracker.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "expenses";
    public static final String COL_ID = "id";
    public static final String COL_TYPE = "type";
    public static final String COL_TITLE = "title";
    public static final String COL_AMOUNT = "amount";
    public static final String COL_CATEGORY = "category";
    public static final String COL_DATE = "date";
    public static final String COL_NOTE = "note";

    public Databasehelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Table create karne ki query [cite: 65-72]
        String query = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_TYPE + " TEXT, " +
                COL_TITLE + " TEXT, " +
                COL_AMOUNT + " INTEGER, " +
                COL_CATEGORY + " TEXT, " +
                COL_DATE + " TEXT, " +
                COL_NOTE + " TEXT);";
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
