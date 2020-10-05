package com.example.peta;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.peta.db.DatabaseContract;
import com.example.peta.db.DatabaseHelper;

import java.util.ArrayList;

import static com.example.peta.db.DatabaseContract.ChildColumns.CHILD_ID;
import static com.example.peta.db.DatabaseContract.ChildColumns.DATE;
import static com.example.peta.db.DatabaseContract.ChildColumns.PARENT_ID;
import static com.example.peta.db.DatabaseContract.ChildColumns.TABLE_NAME;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    Context mContext;
    private ArrayList<History> listHistory = new ArrayList<>();

    public HistoryAdapter(Context mContext, ArrayList<History> listHistory) {
        this.mContext = mContext;
        this.listHistory = listHistory;
    }

    @NonNull
    @Override
    public HistoryAdapter.HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.users_date_history, parent, false);
        return new HistoryAdapter.HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryAdapter.HistoryViewHolder holder, int position) {
        final History history = listHistory.get(position);
        holder.tv_tanggal.setText(history.getDate());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View popupView = LayoutInflater.from(mContext).inflate(R.layout.popup_history, null);
                final PopupWindow popupWindow = new PopupWindow(popupView, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT);

                RecyclerView recyclerView = (RecyclerView) popupView.findViewById(R.id.rv_popup_his);
                RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(mContext);
                recyclerView.setLayoutManager(mLayoutManager);
                ArrayList<History> listPopupHistory = new ArrayList<>();

                DatabaseHelper databaseHelper = new DatabaseHelper(mContext);
                SQLiteDatabase db;

                db = databaseHelper.getReadableDatabase();
                Cursor dataCursor = db.rawQuery("SELECT * FROM " + TABLE_NAME +
                        " WHERE " + PARENT_ID + " = '" + history.getParent_id() + "'" + " AND " +
                        CHILD_ID + " = '" + history.getChild_id() + "'" + " AND " +
                        DATE + " = '" + history.getDate() + "'", null);
                while (dataCursor.moveToNext()) {
                    History history = new History();
                    history.setId(dataCursor.getInt(dataCursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns._ID)));
                    history.setTime(dataCursor.getString(dataCursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns.TIME)));
                    history.setAddress(dataCursor.getString(dataCursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns.LOCATION)));
                    history.setLatitude(dataCursor.getString(dataCursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns.LATITUDE)));
                    history.setLongitude(dataCursor.getString(dataCursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns.LONGITUDE)));
                    listPopupHistory.add(history);
                }

                for (int i = 0; i < listPopupHistory.size(); i++) {
                    Log.d("historyrepeat", listPopupHistory.get(i).toString());
                }

                PopupHistoryAdapter adapter = new PopupHistoryAdapter(mContext,listPopupHistory);
                recyclerView.setAdapter(adapter);

                Button btn = (Button) popupView.findViewById(R.id.btn_close_his);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        popupWindow.dismiss();
                    }
                });

                popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listHistory.size();
    }

    public class HistoryViewHolder extends RecyclerView.ViewHolder {

        TextView tv_tanggal;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_tanggal = itemView.findViewById(R.id.tv_user_date_his);
        }
    }
}
