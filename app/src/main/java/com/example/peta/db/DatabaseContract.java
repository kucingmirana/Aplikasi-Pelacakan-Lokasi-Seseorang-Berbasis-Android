package com.example.peta.db;

import android.provider.BaseColumns;

public class DatabaseContract {

    public static final class ChildColumns implements BaseColumns {
        public static final String TABLE_NAME = "child_location_tbl";
        public static final String PARENT_ID = "parent_id";
        public static final String CHILD_ID = "child_id";
        public static final String LOCATION = "location";
        public static final String LATITUDE = "latitude";
        public static final String LONGITUDE = "longitude";
        public static final String DATE = "date";
        public static final String TIME = "time";
    }
}
