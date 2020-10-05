package com.example.peta;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
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

import java.util.ArrayList;

public class AddKidOrParentUnREG extends AppCompatActivity {

    private Toolbar mToolbar;
    RecyclerView recyclerView;
    ArrayList<Contact> listContact = new ArrayList<>();
    ContactAdapter adapter;
    private DatabaseReference UsersRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_kid_or_parent_un_reg);

        mAuth = FirebaseAuth.getInstance();

        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        recyclerView = findViewById(R.id.contacts_recycler_un_reg);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mToolbar = (Toolbar) findViewById(R.id.find_friends_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Orang tua atau anak UnRegis");

        adapter = new ContactAdapter(this, listContact);
        recyclerView.setAdapter(adapter);

        reqpermission();
    }

    private void reqpermission(){
        Dexter.withActivity(this)
                .withPermission(Manifest.permission.READ_CONTACTS)
                .withListener(new PermissionListener(){
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        if (response.getPermissionName().equals(Manifest.permission.READ_CONTACTS)){
                            getContacts();
                        }
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(AddKidOrParentUnREG.this, "Permission Should Be Granted", Toast.LENGTH_SHORT).show();
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
        while (phones.moveToNext()) {
            final String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            final String phoneUri = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI));
            phoneNumber = phoneNumber.replace("-", "");
            phoneNumber = phoneNumber.replace(" ", "");
            if (phoneNumber.substring(0,3).equals("+62")){
                phoneNumber = phoneNumber.substring(3);
            }
            else
                phoneNumber = phoneNumber.substring(1);

            Query query = UsersRef.orderByChild("phone").equalTo(phoneNumber);
            Log.i("TAG", "anak : " + query);
            final String finalPhoneNumber = "+62" + phoneNumber;
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                    if (dataSnapshot.exists()) {

                    }
                    else {
                        Contact contact = new Contact();
                        contact.setName(name);
                        contact.setPhone(finalPhoneNumber);
                        contact.setPhoto(phoneUri);
                        listContact.add(contact);
                        adapter.notifyDataSetChanged();
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });



        }
    }

}
