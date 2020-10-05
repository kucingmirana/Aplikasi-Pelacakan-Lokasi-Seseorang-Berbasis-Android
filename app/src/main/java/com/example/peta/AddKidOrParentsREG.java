package com.example.peta;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashSet;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.content.ContentValues.TAG;

public class AddKidOrParentsREG extends AppCompatActivity {

    private Toolbar mToolbar;
    Button refreshBtn;
    RecyclerView recyclerView;
    ArrayList<Contact> listContact = new ArrayList<>();
    ContactAdapter adapter;

    ProgressBar refreshBar;

    private DatabaseReference ContactsRef,UsersRef,ChildRef;
    private FirebaseAuth mAuth;
    private String senderUserID, phone = "", uid = "", name = "", friend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_kid_or_parent_reg);

        mAuth = FirebaseAuth.getInstance();
        senderUserID = mAuth.getCurrentUser().getUid();
        ChildRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(senderUserID);
        refreshBtn = findViewById(R.id.btn_refresh);
        refreshBar = findViewById(R.id.refresh_bar);
        refreshBar.setVisibility(View.INVISIBLE);

        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        recyclerView = findViewById(R.id.contacts_recycler);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mToolbar = (Toolbar) findViewById(R.id.find_friends_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Add or Remove Tracked User");

        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listContact.clear();
                reqpermission();
                refreshBtn.setEnabled(false);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        refreshBtn.setEnabled(true);
                    }
                }, 1000);


            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshBtn.setEnabled(false);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                reqpermission();
                refreshBtn.setEnabled(true);
            }
        }, 1000);
    }

    private void reqpermission(){
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_CONTACTS)
                .withListener(new PermissionListener(){
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        if (response.getPermissionName().equals(Manifest.permission.READ_CONTACTS)){
                            listContact.clear();
                            getContacts();
                        }
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(AddKidOrParentsREG.this, "Permission Should Be Granted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void getContacts() {

        Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,null,null,null);

        try {
            HashSet<String> normalizedNumbersAlreadyFound = new HashSet<>();
            int indexOfNormalizedNumber = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER);
            int indexOfDisplayNumber = phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            while (phones.moveToNext()) {
                String normalizedNumber = phones.getString(indexOfNormalizedNumber);
                if (normalizedNumbersAlreadyFound.add(normalizedNumber)) {
                    String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phoneNumber = phoneNumber.replace("-", "");
                    phoneNumber = phoneNumber.replace(" ", "");
                    if (phoneNumber.substring(0,3).equals("+62")){
                        phoneNumber = phoneNumber.substring(3);
                    }
                    else
                        phoneNumber = phoneNumber.substring(1);

                    Query query = UsersRef.orderByChild("phone").equalTo(phoneNumber);
                    Log.i("TAG", "anak : " + query);
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                            if (dataSnapshot.exists()) {

                                for (final DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                                    dataSnapshot.getKey();
                                    Log.i("TAG", "key:" + childSnapshot.getKey());

                                    if (!childSnapshot.getKey().equals(senderUserID)){

                                        if (childSnapshot.hasChild("name") && childSnapshot.hasChild("status")){
                                            ContactsRef.child(senderUserID)
                                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                                        @Override
                                                        public void onDataChange(DataSnapshot dataSnapshot)
                                                        {
                                                            if (dataSnapshot.hasChild(childSnapshot.getKey()))
                                                            {
                                                                friend = "friend";

                                                                Log.i("TAG", "friend dalam:" + friend);


                                                            }

                                                            else {
                                                                friend = "no";
                                                            }


                                                            if (childSnapshot.child("phone").getValue() != null) {
                                                                phone = "+62" + childSnapshot.child("phone").getValue().toString();
                                                            }
                                                            if (childSnapshot.child("name").getValue() != null) {
                                                                name = childSnapshot.child("name").getValue().toString();
                                                            }
                                                            if (childSnapshot.child("uid").getValue() != null) {
                                                                uid = childSnapshot.child("uid").getValue().toString();
                                                            }
                                                            if (childSnapshot.child("status").getValue().equals("child")){
                                                                Contact contact = new Contact();
                                                                contact.setUid(uid);
                                                                contact.setFriend(friend);
                                                                contact.setName(name);
                                                                contact.setPhone(phone);
                                                                listContact.add(contact);
                                                                adapter.notifyDataSetChanged();
                                                            }

                                                        }

                                                        @Override
                                                        public void onCancelled(DatabaseError databaseError) {

                                                        }
                                                    });

                                        }
                                    }

                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                    adapter = new ContactAdapter(this, listContact);
                    recyclerView.setAdapter(adapter);
                }
            }
        } finally {
            phones.close();
        }

    }
}
