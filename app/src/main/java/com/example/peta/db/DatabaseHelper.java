package com.example.peta.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.peta.db.DatabaseContract.ChildColumns;




public class DatabaseHelper extends SQLiteOpenHelper{
    public static String DATABASE_NAME = "dbchildapp";
    private static final int DATABASE_VERSION = 1;
    private static final String SQL_CREATE_TABLE_CHILD = String.format("CREATE TABLE %s"
                    + " (%s INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " %s TEXT NOT NULL," +
                    " %s TEXT NOT NULL," +
                    " %s TEXT NOT NULL," +
                    " %s TEXT NOT NULL," +
                    " %s TEXT NOT NULL," +
                    " %s TEXT NOT NULL," +
                    " %s TEXT NOT NULL)",
            ChildColumns.TABLE_NAME,
            ChildColumns._ID,
            ChildColumns.PARENT_ID,
            ChildColumns.CHILD_ID,
            ChildColumns.LOCATION,
            ChildColumns.LATITUDE,
            ChildColumns.LONGITUDE,
            ChildColumns.DATE,
            ChildColumns.TIME
    );

    private static final String SQL_CREATE_TABLE_PARENT = String.format("CREATE TABLE %s"
                    + " (%s INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " %s TEXT NOT NULL," +
                    " %s TEXT NOT NULL," +
                    " %s TEXT NOT NULL," +
                    " %s TEXT NOT NULL," +
                    " %s TEXT NOT NULL)",
            DatabaseContractChild.ParentColumns.TABLE_NAME,
            DatabaseContractChild.ParentColumns._ID,
            DatabaseContractChild.ParentColumns.CHILD_ID,
            DatabaseContractChild.ParentColumns.CHILD_NO,
            DatabaseContractChild.ParentColumns.CHILD_NAME,
            DatabaseContractChild.ParentColumns.CHILD_STATUS,
            DatabaseContractChild.ParentColumns.PARENT_NO
    );

    private static final String SQL_CREATE_TABLE_CHIP = String.format("CREATE TABLE %s"
                    + " (%s INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " %s TEXT NOT NULL," +
                    " %s TEXT NOT NULL)",
            DatabaseContractChip.ChipColumns.TABLE_NAME,
            DatabaseContractChip.ChipColumns._ID,
            DatabaseContractChip.ChipColumns.CHILD_ID,
            DatabaseContractChip.ChipColumns.CHIP_TEXT
    );

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE_CHILD);
        db.execSQL(SQL_CREATE_TABLE_PARENT);
        db.execSQL(SQL_CREATE_TABLE_CHIP);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ChildColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseContractChild.ParentColumns.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + DatabaseContractChip.ChipColumns.TABLE_NAME);
        onCreate(db);
    }
}
