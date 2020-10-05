package com.example.peta;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class PopupHistoryAdapter extends RecyclerView.Adapter<PopupHistoryAdapter.PopupViewHolder> {

    Context mContext;
    private ArrayList<History> listPopupHistory = new ArrayList<>();

    public PopupHistoryAdapter(Context mContext, ArrayList<History> listPopupHistory) {
        this.mContext = mContext;
        this.listPopupHistory = listPopupHistory;
    }

    @NonNull
    @Override
    public PopupHistoryAdapter.PopupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.users_popup_history, parent, false);
        return new PopupHistoryAdapter.PopupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PopupHistoryAdapter.PopupViewHolder holder, int position) {
        final History history = listPopupHistory.get(position);
        holder.tv_time.setText(history.getTime());
        holder.tv_location.setText(history.getAddress());
        holder.imgGmapLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + history
                                .getLatitude()+ "," + history.getLongitude() +"&z=15");
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        mContext.startActivity(mapIntent);
                    }
                }, 1000);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listPopupHistory.size();
    }

    public class PopupViewHolder extends RecyclerView.ViewHolder {

        TextView tv_time;
        TextView tv_location;
        Button btnClose;
        ImageView imgGmapLink;

        public PopupViewHolder(@NonNull View itemView) {
            super(itemView);
            tv_time = itemView.findViewById(R.id.tv_time);
            tv_location = itemView.findViewById(R.id.tv_location);
            imgGmapLink = itemView.findViewById(R.id.img_link_history);
            btnClose = itemView.findViewById(R.id.btn_close_his);
        }
    }
}
