package com.example.peta;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class FragmentUserProfile extends Fragment {

    private RecyclerView childList;

    private DatabaseReference ChildRef, UsersRef, RootRef;
    private FirebaseAuth mAuth;
    private String currentUserID="";


    public FragmentUserProfile() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View childView = inflater.inflate(R.layout.fragment_user_profile, container, false);
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        ChildRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        RootRef = FirebaseDatabase.getInstance().getReference();

        childList = childView.findViewById(R.id.rec_view_child);
        childList.setLayoutManager(new LinearLayoutManager(getContext()));

        return childView;
    }

    @Override
    public void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Contact> options =
                new FirebaseRecyclerOptions.Builder<Contact>()
                        .setQuery(ChildRef, Contact.class)
                        .build();

        FirebaseRecyclerAdapter<Contact, ChildViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contact, ChildViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final ChildViewHolder holder, int position, @NonNull Contact model)
                    {
                        final String usersIDs = getRef(position).getKey();
                        final String[] retImage = {"default_image"};

                        UsersRef.child(usersIDs).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                if (dataSnapshot.exists())
                                {
                                    final String retName;
                                    final String retPhone;
                                    Log.d("tesrvchild", usersIDs);
                                    if (dataSnapshot.hasChild("image"))
                                    {
                                        retImage[0] = dataSnapshot.child("image").getValue().toString();
                                        Picasso.get().load(retImage[0]).into(holder.profileImage);
                                    }

                                    if (dataSnapshot.hasChild("name"))
                                    {
                                        retName = dataSnapshot.child("name").getValue().toString();
                                        holder.userName.setText(retName);
                                        if (dataSnapshot.hasChild("phone"))
                                        {
                                            retPhone = dataSnapshot.child("phone").getValue().toString();

                                            holder.imgBtnHistory.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Intent hisIntent = new Intent(getContext(), HistoryActivity.class);
                                                    hisIntent.putExtra("visit_user_id", usersIDs);
                                                    hisIntent.putExtra("visit_user_name", retName);
                                                    hisIntent.putExtra("visit_image", retImage[0]);
                                                    startActivity(hisIntent);
                                                }
                                            });

                                            holder.btn_Chat.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                                    chatIntent.putExtra("visit_user_id", usersIDs);
                                                    chatIntent.putExtra("visit_user_name", retName);
                                                    chatIntent.putExtra("visit_image", retImage[0]);
                                                    startActivity(chatIntent);
                                                }
                                            });

                                            holder.btn_Call.setOnClickListener(new View.OnClickListener() {
                                                @Override
                                                public void onClick(View v) {
                                                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                                                    callIntent.setData(Uri.parse("tel:"+"+62"+retPhone));
                                                    startActivity(callIntent);
                                                }
                                            });
                                        }
                                    }

                                    if (dataSnapshot.child("userState").hasChild("state"))
                                    {
                                        String state = dataSnapshot.child("userState").child("state").getValue().toString();
                                        String date = dataSnapshot.child("userState").child("date").getValue().toString();
                                        String time = dataSnapshot.child("userState").child("time").getValue().toString();

                                        if (state.equals("online"))
                                        {
                                            holder.userStatus.setText("online");
                                        }
                                        else if (state.equals("offline"))
                                        {
                                            holder.userStatus.setText("Last Seen: " + date + " " + time);
                                        }
                                    }
                                    else
                                    {
                                        holder.userStatus.setText("offline");
                                    }

                                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {

                                            Intent profileIntent = new Intent(v.getContext(), ProfileActivity.class);
                                            profileIntent.putExtra("visit_user_id", usersIDs);
                                            v.getContext().startActivity(profileIntent);
                                        }
                                    });

                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }

                    @NonNull
                    @Override
                    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
                    {
                        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout, viewGroup, false);
                        return new ChildViewHolder(view);
                    }
                };
        childList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class  ChildViewHolder extends RecyclerView.ViewHolder
    {
        CircleImageView profileImage;
        TextView userStatus, userName;
        Button btn_Chat, btn_Call;
        ImageView imgBtnHistory;


        public ChildViewHolder(@NonNull View itemView)
        {
            super(itemView);

            btn_Call = itemView.findViewById(R.id.btn_call);
            btn_Chat = itemView.findViewById(R.id.btn_chat);
            imgBtnHistory = itemView.findViewById(R.id.btn_img_his);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            userStatus = itemView.findViewById(R.id.user_status);
            userName = itemView.findViewById(R.id.user_profile_name);
        }
    }

}
