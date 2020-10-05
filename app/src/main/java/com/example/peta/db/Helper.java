package com.example.peta.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import static android.provider.BaseColumns._ID;
import static com.example.peta.db.DatabaseContract.ChildColumns.TABLE_NAME;

public class Helper {

    private static final String DATABASE_TABLE = TABLE_NAME;
    private static final String DATABASE_TABLE_PARENT = DatabaseContractChild.ParentColumns.TABLE_NAME;
    private static final String DATABASE_TABLE_CHIP = DatabaseContractChip.ChipColumns.TABLE_NAME;
    private static DatabaseHelper dataBaseHelper;
    private static Helper INSTANCE;

    private static SQLiteDatabase database;

    private Helper(Context context) {
        dataBaseHelper = new DatabaseHelper(context);
    }

    public static Helper getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SQLiteOpenHelper.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Helper(context);
                }
            }
        }
        return INSTANCE;
    }

    public void open() throws SQLException {
        database = dataBaseHelper.getWritableDatabase();
    }

    public void openR() throws SQLException {
        database = dataBaseHelper.getReadableDatabase();
    }

    public void close() {
        dataBaseHelper.close();
        if (database.isOpen())
            database.close();
    }

    public Cursor queryAll() {
        return database.query(
                DATABASE_TABLE,
                null,
                null,
                null,
                null,
                null,
                _ID + " ASC");
    }

    public Cursor queryById(String id) {
        return database.query(DATABASE_TABLE, null
                , _ID + " = ?"
                , new String[]{id}
                , null
                , null
                , null
                , null);
    }

    public long insert(ContentValues values) {
        return database.insert(DATABASE_TABLE, null, values);
    }

    public long insertParent(ContentValues values) {
        return database.insert(DATABASE_TABLE_PARENT, null, values);
    }

    public long insertChip(ContentValues values) {
        return database.insert(DATABASE_TABLE_CHIP, null, values);
    }

    public int update(String id, ContentValues values) {
        return database.update(DATABASE_TABLE, values, _ID + " = ?", new String[]{id});
    }

    public int deleteById(String id) {
        return database.delete(DATABASE_TABLE, _ID + " = ?", new String[]{id});
    }

    public int deleteByIdParent(String id) {
        return database.delete(DATABASE_TABLE_PARENT, _ID + " = ?", new String[]{id});
    }

    public int deleteByIdChip(String id) {
        return database.delete(DATABASE_TABLE_CHIP, _ID + " = ?", new String[]{id});
    }

    public void deleteTable() {
        database.execSQL("DELETE FROM "+ TABLE_NAME);
    }

}
