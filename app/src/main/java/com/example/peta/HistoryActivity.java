package com.example.peta;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Parcel;
import android.util.Log;
import android.widget.TextView;

import com.example.peta.db.DatabaseContract;
import com.example.peta.db.DatabaseHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.example.peta.db.DatabaseContract.ChildColumns.CHILD_ID;
import static com.example.peta.db.DatabaseContract.ChildColumns.PARENT_ID;
import static com.example.peta.db.DatabaseContract.ChildColumns.TABLE_NAME;

public class HistoryActivity extends AppCompatActivity {

    private String historyReceiverID, historyReceiverName,historyReceiverImage;
    private FirebaseAuth mAuth;
    private String senderUserID;
    TextView tvUserNameHis;
    CircleImageView profImgHis;

    HistoryAdapter adapter;

    RecyclerView recyclerView;

    ArrayList<History> listhistories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        mAuth = FirebaseAuth.getInstance();
        senderUserID = mAuth.getCurrentUser().getUid();

        historyReceiverID = getIntent().getExtras().get("visit_user_id").toString();
        historyReceiverName = getIntent().getExtras().get("visit_user_name").toString();
        historyReceiverImage = getIntent().getExtras().get("visit_image").toString();

        tvUserNameHis =findViewById(R.id.tv_user_name_his);
        tvUserNameHis.setText(historyReceiverName);

        profImgHis = findViewById(R.id.profile_image_his);
        if (!historyReceiverImage.equals("default_image")){
            Picasso.get().load(historyReceiverImage).fit().centerInside().into(profImgHis);
        }

        recyclerView = findViewById(R.id.rec_view__his_child);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new HistoryAdapter(this, listhistories);
        recyclerView.setAdapter(adapter);

        DatabaseHelper databaseHelper = new DatabaseHelper(this);
        SQLiteDatabase db;

        db = databaseHelper.getReadableDatabase();
        Cursor dataCursor = db.rawQuery("SELECT * FROM " + TABLE_NAME +
                " WHERE " + PARENT_ID + " = '" + senderUserID + "'" + " AND " +
                CHILD_ID + " = '" + historyReceiverID + "'", null);
        while (dataCursor.moveToNext()) {
            Log.d("repeated", listhistories.toString());

            if (listhistories.size() == 0){
                Log.d("repeated2", "data added");
                History history2 = new History();
                history2.setId(dataCursor.getInt(dataCursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns._ID)));
                history2.setDate(dataCursor.getString(dataCursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns.DATE)));
                history2.setParent_id(senderUserID);
                history2.setChild_id(historyReceiverID);
                listhistories.add(history2);
                adapter.notifyDataSetChanged();
            }else {
                for (int i = 1; i <= listhistories.size(); i++) {
                    History history = listhistories.get(i-1);
                    Log.d("repeated2", i + history.getDate());
                    if (history.getDate().equals(dataCursor.getString(dataCursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns.DATE)))){
                        Log.d("repeated2", "repeated");
                        break;
                    }else {
                        Log.d("repeated2", "not repeated");
                        if (i == listhistories.size()){
                            Log.d("repeated2", "data added");
                            History history2 = new History();
                            history2.setId(dataCursor.getInt(dataCursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns._ID)));
                            history2.setDate(dataCursor.getString(dataCursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns.DATE)));
                            history2.setParent_id(senderUserID);
                            history2.setChild_id(historyReceiverID);
                            listhistories.add(history2);
                            adapter.notifyDataSetChanged();
                        }

                    }

                }
            }



        }
        db.close();
        dataCursor.close();

    }
}
