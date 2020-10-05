package com.example.peta;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.peta.db.Helper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static android.provider.BaseColumns._ID;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.CHILD_ID;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.CHILD_NAME;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.CHILD_NO;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.CHILD_STATUS;
import static com.example.peta.db.DatabaseContractChild.ParentColumns.PARENT_NO;


public class PhoneLoginActivity extends AppCompatActivity
{
    private ImageView logoanak,guide;
    private Button SendVerificationCodeButton, VerifyButton;
    private EditText InputPhoneNumber, InputVerificationCode;
    private TextView autoVerifText,changePhoneNum,userGuide;

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef,UsersRef,ChildRef;

    private ProgressDialog loadingBar;
    private ProgressBar progressBar;

    Helper helper;

    private String mVerificationId,phoneNumber;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        mAuth = FirebaseAuth.getInstance();
        RootRef = FirebaseDatabase.getInstance().getReference();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChildRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        SendVerificationCodeButton = (Button) findViewById(R.id.send_ver_code_button);
        VerifyButton = (Button) findViewById(R.id.verify_button);
        InputPhoneNumber = (EditText) findViewById(R.id.phone_number_input);
        InputVerificationCode = (EditText) findViewById(R.id.verification_code_input);
        progressBar = findViewById(R.id.pb_bar_token_verif);
        autoVerifText = findViewById(R.id.tv_auto_verif_text);
        changePhoneNum = findViewById(R.id.tv_change_phone_num);
        logoanak = findViewById(R.id.logo_anak);
        guide = findViewById(R.id.imgGuide);
        userGuide = findViewById(R.id.tv_user_guide);

        guide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent guideIntent = new Intent(PhoneLoginActivity.this, GuideActivity.class);
                startActivity(guideIntent);
            }
        });

        changePhoneNum.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(PhoneLoginActivity.this, "please wait 1 minutes to login again...", Toast.LENGTH_SHORT).show();
                VerifyButton.setVisibility(View.INVISIBLE);
                InputVerificationCode.setVisibility(View.INVISIBLE);
                progressBar.setVisibility(View.INVISIBLE);
                autoVerifText.setVisibility(View.INVISIBLE);
                changePhoneNum.setVisibility(View.INVISIBLE);

                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        SendVerificationCodeButton.setVisibility(View.VISIBLE);
                        InputPhoneNumber.setVisibility(View.VISIBLE);
                        logoanak.setVisibility(View.VISIBLE);
                        guide.setVisibility(View.VISIBLE);
                        userGuide.setVisibility(View.VISIBLE);

                    }
                }, 1000 * 60);

            }
        });

        loadingBar = new ProgressDialog(this);


        SendVerificationCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                phoneNumber = "+62" + InputPhoneNumber.getText().toString();

                if (TextUtils.isEmpty(phoneNumber))
                {
                    Toast.makeText(PhoneLoginActivity.this, "Please enter your phone number first...", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    loadingBar.setTitle("Phone Verification");
                    loadingBar.setMessage("please wait, while we are authenticating your phone...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthProvider.getInstance().verifyPhoneNumber(
                            phoneNumber,        // Phone number to verify
                            60,                 // Timeout duration
                            TimeUnit.SECONDS,   // Unit of timeout
                            PhoneLoginActivity.this,               // Activity (for callback binding)
                            callbacks);        // OnVerificationStateChangedCallbacks
                }
            }
        });


        VerifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                SendVerificationCodeButton.setVisibility(View.INVISIBLE);
                InputPhoneNumber.setVisibility(View.INVISIBLE);

                String verificationCode = InputVerificationCode.getText().toString();

                if (TextUtils.isEmpty(verificationCode))
                {
                    Toast.makeText(PhoneLoginActivity.this, "Please write verification code first...", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    loadingBar.setTitle("Verification Code");
                    loadingBar.setMessage("please wait, while we are verifying verification code...");
                    loadingBar.setCanceledOnTouchOutside(false);
                    loadingBar.show();

                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
                    signInWithPhoneAuthCredential(credential);
                }
            }
        });


        callbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential)
            {
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e)
            {
                loadingBar.dismiss();
                Toast.makeText(PhoneLoginActivity.this, "Invalid Phone Number, Please enter correct phone number with your country code...", Toast.LENGTH_SHORT).show();

                SendVerificationCodeButton.setVisibility(View.VISIBLE);
                InputPhoneNumber.setVisibility(View.VISIBLE);
                logoanak.setVisibility(View.VISIBLE);
                guide.setVisibility(View.VISIBLE);
                userGuide.setVisibility(View.VISIBLE);

                VerifyButton.setVisibility(View.INVISIBLE);
                InputVerificationCode.setVisibility(View.INVISIBLE);
            }

            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token)
            {
                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;

                loadingBar.dismiss();
                progressBar.setVisibility(View.VISIBLE);
                autoVerifText.setVisibility(View.VISIBLE);
                changePhoneNum.setVisibility(View.VISIBLE);
                Toast.makeText(PhoneLoginActivity.this, "Code has been sent, please check and verify...", Toast.LENGTH_SHORT).show();

                SendVerificationCodeButton.setVisibility(View.INVISIBLE);
                InputPhoneNumber.setVisibility(View.INVISIBLE);
                logoanak.setVisibility(View.INVISIBLE);
                guide.setVisibility(View.INVISIBLE);
                userGuide.setVisibility(View.INVISIBLE);

                VerifyButton.setVisibility(View.VISIBLE);
                InputVerificationCode.setVisibility(View.VISIBLE);
            }
        };
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful())
                        {
                            loadingBar.dismiss();
                            Log.i("USER", mAuth.getCurrentUser().getUid());
                            Toast.makeText(PhoneLoginActivity.this, "Congratulations, you're logged in successfully...", Toast.LENGTH_SHORT).show();
                            VerifyUserExistance();
                        }
                        else
                        {
                            String message = task.getException().toString();
                            Toast.makeText(PhoneLoginActivity.this, "Error : "  +  message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private void VerifyUserExistance()
    {
        String currentUserID = mAuth.getCurrentUser().getUid();

        RootRef.child("Users").child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                if (dataSnapshot.child("name").exists() && dataSnapshot.child("status").exists()
                        && dataSnapshot.child("phone").exists())
                {
                    VerifyChildOrParent();
                }
                else
                {
                    SendUserToInputStatus();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void SendUserToInputStatus()
    {
        String currentUserID = mAuth.getCurrentUser().getUid();

        HashMap<String, Object> profileMap = new HashMap<>();
        profileMap.put("phone", phoneNumber.substring(3));
        RootRef.child("Users").child(currentUserID).updateChildren(profileMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            Intent inputIntent = new Intent(PhoneLoginActivity.this, InputStatus.class);
                            inputIntent.putExtra("phone_number", phoneNumber);
                            startActivity(inputIntent);
                            finish();
                        }
                        else
                        {
                            String message = task.getException().toString();
                            Toast.makeText(PhoneLoginActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });


    }

    private void SendUserToMainActivity()
    {
        Intent mainIntent = new Intent(PhoneLoginActivity.this, MainActivity.class);
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
                    String childName = dataSnapshot.child("name").getValue().toString();
                    CheckChildHasParent(childName);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void CheckChildHasParent(final String childName){
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
                                String parentPhone = "+62" + dataSnapshot.child("phone").getValue().toString();
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
                                values.put(CHILD_NO, phoneNumber);
                                values.put(CHILD_NAME, childName);
                                values.put(CHILD_STATUS, "child");
                                values.put(PARENT_NO, parentPhone);
                                helper.insertParent(values);
                                helper.close();
                                Intent intent = new Intent(PhoneLoginActivity.this,ChildMainActivity.class);
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
                    Intent intent = new Intent(PhoneLoginActivity.this,AddParent.class);
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
