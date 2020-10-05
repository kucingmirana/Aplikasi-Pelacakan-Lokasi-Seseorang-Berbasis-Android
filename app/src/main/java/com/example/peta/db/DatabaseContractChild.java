package com.example.peta.db;

import android.provider.BaseColumns;

public class DatabaseContractChild {

    public static final class ParentColumns implements BaseColumns {
        public static final String TABLE_NAME = "parent_tbl";
        public static final String CHILD_ID = "child_id";
        public static final String CHILD_NO = "child_no";
        public static final String CHILD_NAME = "child_name";
        public static final String CHILD_STATUS = "child_status";
        public static final String PARENT_NO = "parent_no";
    }
}
