package com.example.peta;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class InputStatus extends AppCompatActivity {

    private ImageView parent,child;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private String currentUserID,phoneNumber;
    private ImageButton imgTracked,imgTracker;
    private Button closePopupTracked, closePopupTracker;

    String status,name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input_status);

        final View popupView = LayoutInflater.from(this).inflate(R.layout.users_help_popup_status_tracked, null);
        final PopupWindow popupWindow = new PopupWindow(popupView, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        closePopupTracked = popupView.findViewById(R.id.btn_close_status_tracked_help);

        imgTracked = findViewById(R.id.information_status_tracked);

        imgTracked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);
            }
        });

        closePopupTracked.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow.dismiss();
            }
        });

        final View popupView2 = LayoutInflater.from(this).inflate(R.layout.users_help_popup_status_tracker, null);
        final PopupWindow popupWindow2 = new PopupWindow(popupView2, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        closePopupTracker = popupView2.findViewById(R.id.btn_close_status_tracker_help);
        imgTracker = findViewById(R.id.information_status_tracker);

        imgTracker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow2.showAtLocation(popupView2, Gravity.CENTER, 0, 0);
            }
        });

        closePopupTracker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupWindow2.dismiss();
            }
        });

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();

        phoneNumber = getIntent().getExtras().get("phone_number").toString();

        parent = findViewById(R.id.parent);
        child = findViewById(R.id.child);

        parent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                status = "parent";
                name = "tracker user";
                addUser();
            }
        });

        child.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                name = "tracked user";
                status = "child";
                addUser();
            }
        });



    }

    public void addUser(){
        HashMap<String, Object> profileMap = new HashMap<>();
        profileMap.put("uid", currentUserID);
        profileMap.put("name", name);
        profileMap.put("status", status);
        RootRef.child("Users").child(currentUserID).updateChildren(profileMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            if (status.equals("parent")){
                                SendUserToAddKidOrParentsReg();
                            }else {
                                SendUserToAddParent();
                            }

                            Toast.makeText(InputStatus.this, "Profile Updated Successfully...", Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            String message = task.getException().toString();
                            Toast.makeText(InputStatus.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });


    }

    private void SendUserToAddKidOrParentsReg()
    {
        Intent mainIntent = new Intent(InputStatus.this, MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
    private void SendUserToAddParent()
    {
        Intent mainIntent = new Intent(InputStatus.this, AddParent.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        mainIntent.putExtra("child_phone_number", phoneNumber);
        mainIntent.putExtra("child_name", "tracked user");
        startActivity(mainIntent);
        finish();
    }

}
