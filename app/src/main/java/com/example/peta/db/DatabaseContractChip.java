package com.example.peta.db;

import android.provider.BaseColumns;

public class DatabaseContractChip {

    public static final class ChipColumns implements BaseColumns {
        public static final String TABLE_NAME = "chip_tbl";
        public static final String CHILD_ID = "child_id";
        public static final String CHIP_TEXT = "chip_text";
    }

}
