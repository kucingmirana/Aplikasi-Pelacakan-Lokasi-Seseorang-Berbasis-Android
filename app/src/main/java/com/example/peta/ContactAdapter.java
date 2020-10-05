package com.example.peta;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.ContentValues.TAG;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    Context mContext;
    private ArrayList<Contact> listContact = new ArrayList<>();

    public void setContact(ArrayList<Contact> items) {
        listContact.clear();
        listContact.addAll(items);
        notifyDataSetChanged();
    }

    public ContactAdapter(Context mContext, ArrayList<Contact> listContact) {
        this.mContext = mContext;
        this.listContact = listContact;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_contact, parent, false);
        return new  ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, final int position) {

        Contact contact = listContact.get(position);
        holder.name_contact.setText(contact.getName());
        holder.phone_contact.setText(contact.getPhone());

        if (contact.getPhoto()!=null){
            Picasso.get().load(contact.getPhoto()).into(holder.img_contact);
        }else {
            holder.img_contact.setImageResource(R.mipmap.ic_launcher_round);
        }

        if (contact.getFriend()!=null){
            if (contact.getFriend().equals("friend")){
                holder.img_child.setVisibility(View.VISIBLE);
                holder.img_add.setVisibility(View.INVISIBLE);
            }

            else {
                holder.img_child.setVisibility(View.INVISIBLE);
                holder.img_add.setVisibility(View.VISIBLE);
            }
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                String visit_user_id = listContact.get(position).getUid();
                Log.i(TAG, visit_user_id);

                Intent profileIntent = new Intent(view.getContext(), ProfileActivity.class);
                profileIntent.putExtra("visit_user_id", visit_user_id);
                view.getContext().startActivity(profileIntent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return listContact.size();
    }

    public class ContactViewHolder extends RecyclerView.ViewHolder {

        TextView name_contact, phone_contact;
        CircleImageView img_contact;
        ImageView img_child,img_add;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            name_contact = itemView.findViewById(R.id.name_contact);
            phone_contact = itemView.findViewById(R.id.phone_contact);
            img_contact = itemView.findViewById(R.id.img_contact);
            img_child = itemView.findViewById(R.id.img_friend);
            img_add = itemView.findViewById(R.id.btn_tambah);
        }
    }
}
