package com.example.peta;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.example.peta.db.Helper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static android.provider.BaseColumns._ID;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.CHILD_ID;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.CHILD_NAME;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.CHILD_NO;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.PARENT_NO;

public class SplashScreenActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference RootRef,UsersRef,SplashRef,ChildRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        mAuth = FirebaseAuth.getInstance();
        RootRef = FirebaseDatabase.getInstance().getReference();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChildRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        Thread timer = new Thread(){
            public void run(){
                try{
                    sleep(2000);
                }catch (InterruptedException e){
                    e.printStackTrace();
                }finally {
                    if (mAuth.getCurrentUser() != null){
                        VerifyUserExistance();
                    }
                    else {
                        startActivity(new Intent(SplashScreenActivity.this, PhoneLoginActivity.class));
                        finish();
                    }
                }
            }
        };
        timer.start();
    }

    private void VerifyUserExistance()
    {
        String currentUserID = mAuth.getCurrentUser().getUid();
        SplashRef = RootRef.child("Users").child(currentUserID);

        SplashRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.child("name").exists() && dataSnapshot.child("status").exists())
                {
                    VerifyChildOrParent();
                }
                else
                {
                    String phoneNumber = dataSnapshot.child("phone").getValue().toString();
                    SendUserToInputStatus(phoneNumber);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendUserToInputStatus(String phoneNumber)
    {
        Intent inputIntent = new Intent(SplashScreenActivity.this, InputStatus.class);
        inputIntent.putExtra("phone_number", phoneNumber);
        startActivity(inputIntent);
        finish();
    }

    private void SendUserToMainActivity()
    {
        Intent mainIntent = new Intent(SplashScreenActivity.this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }

    private void VerifyChildOrParent(){
        String currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("status").getValue().equals("parent")){
                    SendUserToMainActivity();
                }
                else {
                    String childPhone = dataSnapshot.child("phone").getValue().toString();
                    String childName = dataSnapshot.child("name").getValue().toString();
                    CheckChildHasParent(childPhone, childName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void CheckChildHasParent(final String phoneNumber, final String childName){
        final String currentUserID = mAuth.getCurrentUser().getUid();
        ChildRef.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){

                    for (DataSnapshot parentSnapshot : dataSnapshot.getChildren()){

                        String parent = parentSnapshot.getKey();

                        UsersRef.child(parent).addListenerForSingleValueEvent(new ValueEventListener() {
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

                                Intent intent = new Intent(SplashScreenActivity.this,ChildMainActivity.class);
                                intent.putExtra("parent_id", parentId);
                                intent.putExtra("parent_name", parentName);
                                intent.putExtra("parent_image", retImage[0]);
                                intent.putExtra("parent_phone", parentPhone);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });

                    }

                }

                else {
                    Intent intent = new Intent(SplashScreenActivity.this,AddParent.class);
                    intent.putExtra("child_phone_number", phoneNumber);
                    intent.putExtra("child_name", childName);

                    startActivity(intent);
                    finish();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


}
