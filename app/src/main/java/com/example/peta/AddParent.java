package com.example.peta;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.peta.db.Helper;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

import static android.provider.BaseColumns._ID;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.CHILD_ID;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.CHILD_NAME;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.CHILD_NO;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.CHILD_STATUS;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.PARENT_NO;

public class AddParent extends AppCompatActivity {

    private RecyclerView myRequestsList;

    Helper helper;

    private DatabaseReference AddRequestsRef, UsersRef, ContactsRef, RootRef;
    private FirebaseAuth mAuth;
    private String currentUserID;
    ImageView logoutAddParent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_parent);

        logoutAddParent = findViewById(R.id.logout_add_parent);
        logoutAddParent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helper = Helper.getInstance(getApplicationContext());
                helper.open();
                helper.deleteByIdParent(Integer.toString(1));
                helper.close();
                updateUserStatus("offline");
                mAuth.signOut();
                SendUserToLoginActivity();
            }
        });

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        AddRequestsRef = FirebaseDatabase.getInstance().getReference().child("Add Requests");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        RootRef = FirebaseDatabase.getInstance().getReference();

        myRequestsList = (RecyclerView) findViewById(R.id.rv_parent_list);
        myRequestsList.setLayoutManager(new LinearLayoutManager(AddParent.this));

    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseRecyclerOptions<Profile> options =
                new FirebaseRecyclerOptions.Builder<Profile>()
                        .setQuery(AddRequestsRef.child(currentUserID), Profile.class)
                        .build();


        FirebaseRecyclerAdapter<Profile, RequestsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Profile, RequestsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull final RequestsViewHolder holder, int position, @NonNull Profile model)
                    {
                        holder.itemView.findViewById(R.id.request_accept_btn).setVisibility(View.VISIBLE);
                        holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.VISIBLE);


                        final String list_user_id = getRef(position).getKey();

                        DatabaseReference getTypeRef = getRef(position).child("request_type").getRef();

                        getTypeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                if (dataSnapshot.exists())
                                {
                                    String type = dataSnapshot.getValue().toString();

                                    if (type.equals("received"))
                                    {
                                        UsersRef.child(list_user_id).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot)
                                            {
                                                if (dataSnapshot.hasChild("image"))
                                                {
                                                    String requestProfileImage = dataSnapshot.child("image").getValue().toString();

                                                    Picasso.get().load(requestProfileImage).into(holder.profileImage);
                                                }

                                                final String requestUserName = dataSnapshot.child("name").getValue().toString();
                                                String requestUserPhone = dataSnapshot.child("phone").getValue().toString();

                                                holder.userName.setText(requestUserName);
                                                holder.userPhone.setText(requestUserPhone);
                                                holder.userStatus.setText("wants to to be your tracker");

                                                holder.CancelButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        AddRequestsRef.child(currentUserID).child(list_user_id)
                                                                .removeValue()
                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<Void> task)
                                                                    {
                                                                        if (task.isSuccessful())
                                                                        {
                                                                            AddRequestsRef.child(list_user_id).child(currentUserID)
                                                                                    .removeValue()
                                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                        @Override
                                                                                        public void onComplete(@NonNull Task<Void> task)
                                                                                        {
                                                                                            if (task.isSuccessful())
                                                                                            {
                                                                                                Toast.makeText(AddParent.this, "Request Deleted", Toast.LENGTH_SHORT).show();
                                                                                            }
                                                                                        }
                                                                                    });
                                                                        }
                                                                    }
                                                                });
                                                    }
                                                });

                                                holder.AcceptButton.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View v) {
                                                        ContactsRef.child(currentUserID).child(list_user_id).child("Contact")
                                                                .setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task)
                                                            {
                                                                if (task.isSuccessful())
                                                                {
                                                                    ContactsRef.child(list_user_id).child(currentUserID).child("Contact")
                                                                            .setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task)
                                                                        {
                                                                            if (task.isSuccessful())
                                                                            {
                                                                                AddRequestsRef.child(currentUserID).child(list_user_id)
                                                                                        .removeValue()
                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                            @Override
                                                                                            public void onComplete(@NonNull Task<Void> task)
                                                                                            {
                                                                                                if (task.isSuccessful())
                                                                                                {
                                                                                                    AddRequestsRef.child(list_user_id).child(currentUserID)
                                                                                                            .removeValue()
                                                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                @Override
                                                                                                                public void onComplete(@NonNull Task<Void> task)
                                                                                                                {
                                                                                                                    if (task.isSuccessful())
                                                                                                                    {

                                                                                                                        UsersRef.child(list_user_id).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                                                            @Override
                                                                                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                                                                                String parentPhone = dataSnapshot.child("phone").getValue().toString();
                                                                                                                                String parentId = dataSnapshot.child("uid").getValue().toString();
                                                                                                                                String parentName = dataSnapshot.child("name").getValue().toString();

                                                                                                                                String[] retImage = {"default_image"};

                                                                                                                                if (dataSnapshot.hasChild("image"))
                                                                                                                                {
                                                                                                                                    retImage[0] = dataSnapshot.child("image").getValue().toString();
                                                                                                                                }

                                                                                                                                helper = Helper.getInstance(getApplicationContext());
                                                                                                                                helper.open();
                                                                                                                                ContentValues values = new ContentValues();
                                                                                                                                values.put(_ID, 1);
                                                                                                                                values.put(CHILD_ID, currentUserID);
                                                                                                                                values.put(CHILD_NO, "+62" + getIntent().getExtras().get("child_phone_number").toString());
                                                                                                                                values.put(CHILD_NAME, getIntent().getExtras().get("child_name").toString());
                                                                                                                                values.put(CHILD_STATUS, "child");
                                                                                                                                values.put(PARENT_NO, "+62" + parentPhone);
                                                                                                                                helper.insertParent(values);
                                                                                                                                helper.close();
                                                                                                                                Intent intent = new Intent(AddParent.this,ChildMainActivity.class);
                                                                                                                                intent.putExtra("child_phone_number", getIntent().getExtras().get("child_phone_number").toString());
                                                                                                                                intent.putExtra("child_name", getIntent().getExtras().get("child_name").toString());
                                                                                                                                intent.putExtra("parent_id", parentId);
                                                                                                                                intent.putExtra("parent_name", parentName);
                                                                                                                                intent.putExtra("parent_image", retImage[0]);
                                                                                                                                intent.putExtra("parent_phone", parentPhone);
                                                                                                                                startActivity(intent);
                                                                                                                                Toast.makeText(AddParent.this, "Tracker Saved", Toast.LENGTH_SHORT).show();
                                                                                                                                finish();
                                                                                                                            }

                                                                                                                            @Override
                                                                                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                                                                            }
                                                                                                                        });

                                                                                                                    }
                                                                                                                }
                                                                                                            });
                                                                                                }
                                                                                            }
                                                                                        });
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                            }
                                                        });
                                                    }
                                                });

                                                holder.itemView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view)
                                                    {
                                                        CharSequence options[] = new CharSequence[]
                                                                {
                                                                        "Accept",
                                                                        "Cancel"
                                                                };

                                                        AlertDialog.Builder builder = new AlertDialog.Builder(AddParent.this);
                                                        builder.setTitle(requestUserName  + "  Chat Request");

                                                        builder.setItems(options, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i)
                                                            {
                                                                if (i == 0)
                                                                {
                                                                    ContactsRef.child(currentUserID).child(list_user_id).child("Contact")
                                                                            .setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                        @Override
                                                                        public void onComplete(@NonNull Task<Void> task)
                                                                        {
                                                                            if (task.isSuccessful())
                                                                            {
                                                                                ContactsRef.child(list_user_id).child(currentUserID).child("Contact")
                                                                                        .setValue("Saved").addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                    @Override
                                                                                    public void onComplete(@NonNull Task<Void> task)
                                                                                    {
                                                                                        if (task.isSuccessful())
                                                                                        {
                                                                                            AddRequestsRef.child(currentUserID).child(list_user_id)
                                                                                                    .removeValue()
                                                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                        @Override
                                                                                                        public void onComplete(@NonNull Task<Void> task)
                                                                                                        {
                                                                                                            if (task.isSuccessful())
                                                                                                            {
                                                                                                                AddRequestsRef.child(list_user_id).child(currentUserID)
                                                                                                                        .removeValue()
                                                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                            @Override
                                                                                                                            public void onComplete(@NonNull Task<Void> task)
                                                                                                                            {
                                                                                                                                if (task.isSuccessful())
                                                                                                                                {
                                                                                                                                    UsersRef.child(list_user_id).addListenerForSingleValueEvent(new ValueEventListener() {
                                                                                                                                        @Override
                                                                                                                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                                                                                            String parentPhone = dataSnapshot.child("phone").getValue().toString();
                                                                                                                                            String parentId = dataSnapshot.child("uid").getValue().toString();
                                                                                                                                            String parentName = dataSnapshot.child("name").getValue().toString();

                                                                                                                                            String[] retImage = {"default_image"};

                                                                                                                                            if (dataSnapshot.hasChild("image"))
                                                                                                                                            {
                                                                                                                                                retImage[0] = dataSnapshot.child("image").getValue().toString();
                                                                                                                                            }

                                                                                                                                            helper = Helper.getInstance(getApplicationContext());
                                                                                                                                            helper.open();
                                                                                                                                            ContentValues values = new ContentValues();
                                                                                                                                            values.put(_ID, 1);
                                                                                                                                            values.put(CHILD_ID, currentUserID);
                                                                                                                                            values.put(CHILD_NO, "+62" + getIntent().getExtras().get("child_phone_number").toString());
                                                                                                                                            values.put(CHILD_NAME, getIntent().getExtras().get("child_name").toString());
                                                                                                                                            values.put(CHILD_STATUS, "child");
                                                                                                                                            values.put(PARENT_NO, "+62" + parentPhone);
                                                                                                                                            helper.insertParent(values);
                                                                                                                                            helper.close();
                                                                                                                                            Intent intent = new Intent(AddParent.this,ChildMainActivity.class);
                                                                                                                                            intent.putExtra("child_phone_number", getIntent().getExtras().get("child_phone_number").toString());
                                                                                                                                            intent.putExtra("child_name", getIntent().getExtras().get("child_name").toString());
                                                                                                                                            intent.putExtra("parent_id", parentId);
                                                                                                                                            intent.putExtra("parent_name", parentName);
                                                                                                                                            intent.putExtra("parent_image", retImage[0]);
                                                                                                                                            intent.putExtra("parent_phone", parentPhone);
                                                                                                                                            startActivity(intent);
                                                                                                                                            Toast.makeText(AddParent.this, "Parent Saved", Toast.LENGTH_SHORT).show();
                                                                                                                                            finish();
                                                                                                                                        }

                                                                                                                                        @Override
                                                                                                                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                                                                                        }
                                                                                                                                    });
                                                                                                                                }
                                                                                                                            }
                                                                                                                        });
                                                                                                            }
                                                                                                        }
                                                                                                    });
                                                                                        }
                                                                                    }
                                                                                });
                                                                            }
                                                                        }
                                                                    });
                                                                }
                                                                if (i == 1)
                                                                {
                                                                    AddRequestsRef.child(currentUserID).child(list_user_id)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task)
                                                                                {
                                                                                    if (task.isSuccessful())
                                                                                    {
                                                                                        AddRequestsRef.child(list_user_id).child(currentUserID)
                                                                                                .removeValue()
                                                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                    @Override
                                                                                                    public void onComplete(@NonNull Task<Void> task)
                                                                                                    {
                                                                                                        if (task.isSuccessful())
                                                                                                        {
                                                                                                            Toast.makeText(AddParent.this, "Contact Deleted", Toast.LENGTH_SHORT).show();
                                                                                                        }
                                                                                                    }
                                                                                                });
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                                        builder.show();
                                                    }
                                                });

                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });
                                    }
                                    else if (type.equals("sent"))
                                    {
                                        Button request_sent_btn = holder.itemView.findViewById(R.id.request_accept_btn);
                                        request_sent_btn.setText("Req Sent");

                                        holder.itemView.findViewById(R.id.request_cancel_btn).setVisibility(View.INVISIBLE);

                                        UsersRef.child(list_user_id).addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot)
                                            {
                                                if (dataSnapshot.hasChild("image"))
                                                {
                                                    final String requestProfileImage = dataSnapshot.child("image").getValue().toString();

                                                    Picasso.get().load(requestProfileImage).into(holder.profileImage);
                                                }

                                                final String requestUserName = dataSnapshot.child("name").getValue().toString();
                                                final String requestUserStatus = dataSnapshot.child("status").getValue().toString();

                                                holder.userName.setText(requestUserName);
                                                holder.userStatus.setText("you have sent a request to " + requestUserName);


                                                holder.itemView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view)
                                                    {
                                                        CharSequence options[] = new CharSequence[]
                                                                {
                                                                        "Cancel Add Request"
                                                                };

                                                        AlertDialog.Builder builder = new AlertDialog.Builder(AddParent.this);
                                                        builder.setTitle("Already Sent Request");

                                                        builder.setItems(options, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i)
                                                            {
                                                                if (i == 0)
                                                                {
                                                                    AddRequestsRef.child(currentUserID).child(list_user_id)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task)
                                                                                {
                                                                                    if (task.isSuccessful())
                                                                                    {
                                                                                        AddRequestsRef.child(list_user_id).child(currentUserID)
                                                                                                .removeValue()
                                                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                    @Override
                                                                                                    public void onComplete(@NonNull Task<Void> task)
                                                                                                    {
                                                                                                        if (task.isSuccessful())
                                                                                                        {
                                                                                                            Toast.makeText(AddParent.this, "you have cancelled the chat request.", Toast.LENGTH_SHORT).show();
                                                                                                        }
                                                                                                    }
                                                                                                });
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });
                                                        builder.show();
                                                    }
                                                });

                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }

                    @NonNull
                    @Override
                    public RequestsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i)
                    {
                        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_request_layout, viewGroup, false);
                        RequestsViewHolder holder = new RequestsViewHolder(view);
                        return holder;
                    }
                };

        myRequestsList.setAdapter(adapter);
        adapter.startListening();

    }

    public static class RequestsViewHolder extends RecyclerView.ViewHolder
    {
        TextView userName, userStatus, userPhone;
        CircleImageView profileImage;
        Button AcceptButton, CancelButton;


        public RequestsViewHolder(@NonNull View itemView)
        {
            super(itemView);


            userName = itemView.findViewById(R.id.user_profile_name_req);
            userStatus = itemView.findViewById(R.id.user_status_req);
            userPhone = itemView.findViewById(R.id.user_phone_req);
            profileImage = itemView.findViewById(R.id.users_profile_image_req);
            AcceptButton = itemView.findViewById(R.id.request_accept_btn);
            CancelButton = itemView.findViewById(R.id.request_cancel_btn);
        }
    }

    private void updateUserStatus(String state)
    {
        String saveCurrentTime, saveCurrentDate;

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calendar.getTime());

        HashMap<String, Object> onlineStateMap = new HashMap<>();
        onlineStateMap.put("time", saveCurrentTime);
        onlineStateMap.put("date", saveCurrentDate);
        onlineStateMap.put("state", state);

        RootRef.child("Users").child(currentUserID).child("userState")
                .updateChildren(onlineStateMap);

    }

    private void SendUserToLoginActivity()
    {
        Intent loginIntent = new Intent(AddParent.this, PhoneLoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);

    }

}
