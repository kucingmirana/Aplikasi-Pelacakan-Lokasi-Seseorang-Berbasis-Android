package com.example.peta;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.peta.BackgroundServices.SendLocation3;
import com.example.peta.db.Helper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChildAccountActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1;

    private String currentUserID;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef, ContactsRef,UsersRef,MessageRef,TokenRef;
    private StorageReference UserProfileImagesRef;

    TextView userStatus, nomorHpAnak;
    EditText userName;
    CircleImageView userProfile;
    Button updateButton;
    ImageView logoutaccount,deleteAccount;

    Helper helper;

    private String messageReceiverID;

    private ProgressDialog loadingBar;

    private Uri mImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_account);

        userStatus = findViewById(R.id.setting_child_status);
        userName =findViewById(R.id.set_child_user_name);
        nomorHpAnak = findViewById(R.id.set_nomor_hp_anak);
        userProfile = findViewById(R.id.set_child_profile_image);
        updateButton =findViewById(R.id.update_child_settings_button);
        logoutaccount =  findViewById(R.id.logout_from_acc_child);
        deleteAccount = findViewById(R.id.img_hapus_akun_anak);

        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        MessageRef = FirebaseDatabase.getInstance().getReference().child("Messages");
        TokenRef = FirebaseDatabase.getInstance().getReference().child("Tokens");

        messageReceiverID = getIntent().getExtras().get("parent_id").toString();

        deleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(ChildAccountActivity.this);
                alert.setTitle("Delete Account");
                alert.setMessage("Are you sure want to delete your account?");
                alert.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Prompt the user once explanation has been shown
                        DeleteContact();
                        AlarmManager alarmMgr = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
                        Intent intent = new Intent(getApplicationContext(), SendLocation3.class);
                        boolean alarmUp = (PendingIntent.getBroadcast(getApplicationContext(), 12,
                                intent,
                                PendingIntent.FLAG_NO_CREATE) != null);

                        if (alarmUp) {
                            Log.d("myTaghapus", "Alarm terminated");
                            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 12, intent, PendingIntent.FLAG_NO_CREATE);
                            pendingIntent.cancel();
                            alarmMgr.cancel(pendingIntent);
                        }
                    }
                });

                alert.show();
            }
        });

        logoutaccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helper = Helper.getInstance(getApplicationContext());
                helper.open();
                helper.deleteByIdParent(Integer.toString(1));
                helper.close();

                AlarmManager alarmMgr = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
                Intent intent = new Intent(getApplicationContext(), SendLocation3.class);
                boolean alarmUp = (PendingIntent.getBroadcast(getApplicationContext(), 12,
                        intent,
                        PendingIntent.FLAG_NO_CREATE) != null);

                if (alarmUp) {
                    Log.d("myTaghapus", "Alarm terminated");
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 12, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                    pendingIntent.cancel();
                    alarmMgr.cancel(pendingIntent);
                }

                updateUserStatus("offline");
                mAuth.signOut();
                SendUserToLoginActivity();
            }
        });

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        UserProfileImagesRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        loadingBar = new ProgressDialog(this);

        userProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePhotoProfile();
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UpdateSettings();
            }
        });

        RetrieveUserInfo();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
            mImageUri = data.getData();
            Picasso.get().load(mImageUri).fit().centerInside().into(userProfile);
            //imgProfile.setImageURI(mImageUri);
            sendImageProfileData();
        }
    }

    private void DeleteContact()
    {
        ContactsRef.child(currentUserID).child(messageReceiverID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            ContactsRef.child(messageReceiverID).child(currentUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                deleteMessages();
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void deleteMessages(){
        MessageRef.child(currentUserID).child(messageReceiverID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            MessageRef.child(messageReceiverID).child(currentUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                deleteAkun();
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private void deleteAkun(){
        TokenRef.child(currentUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            UsersRef.child(currentUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task)
                                        {
                                            if (task.isSuccessful())
                                            {
                                                helper = Helper.getInstance(getApplicationContext());
                                                helper.open();
                                                helper.deleteByIdParent(Integer.toString(1));
                                                helper.close();

                                                AlarmManager alarmManager =
                                                        (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                                                Intent intent = new Intent(getApplicationContext(), SendLocation3.class);
                                                boolean alarmUp = (PendingIntent.getBroadcast(getApplicationContext(), 12,
                                                        intent,
                                                        PendingIntent.FLAG_NO_CREATE) != null);

                                                if (alarmUp) {
                                                    PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 12, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                                                    pendingIntent.cancel();
                                                    alarmManager.cancel(pendingIntent);
                                                }

                                                mAuth.signOut();
                                                SendUserToLoginActivity();
                                            }
                                        }
                                    });
                        }
                    }
                });
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
        Intent loginIntent = new Intent(ChildAccountActivity.this, PhoneLoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        Toast.makeText(this, "Please wait for 5 minutes to login again", Toast.LENGTH_SHORT).show();

    }

    private void checkBatteryOptimize(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)){
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = this.getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    private void sendImageProfileData() {
        loadingBar.setTitle("Set Profile Image");
        loadingBar.setMessage("Please wait, your profile image is updating...");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();
        StorageReference fileReference = UserProfileImagesRef.child(currentUserID + "." + getFileExtension(mImageUri));
        fileReference.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                loadingBar.dismiss();
                Task<Uri> uri = taskSnapshot.getStorage().getDownloadUrl();
                while (!uri.isComplete()) ;
                Uri url = uri.getResult();
                RootRef.child("Users").child(currentUserID).child("image").setValue(url.toString());
//                Toast.makeText(getContext(), "Profile picture has been updated", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                loadingBar.dismiss();
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void RetrieveUserInfo()
    {
        RootRef.child("Users").child(currentUserID)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot)
                    {
                        String retrieveUserName = dataSnapshot.child("name").getValue().toString();
                        String retrievesStatus = dataSnapshot.child("status").getValue().toString();
                        String retrievesPhone = "+62" + dataSnapshot.child("phone").getValue().toString();

                        if (dataSnapshot.hasChild("image"))
                        {
                            String retrieveImage = dataSnapshot.child("image").getValue().toString();
                            Picasso.get().load(retrieveImage).fit().centerInside().into(userProfile);
                            Log.d("childimage", retrieveImage);
                        }
                        if (retrievesStatus.equals("child")){
                            userStatus.setText("tracked");
                        }

                        userName.setText(retrieveUserName);
                        nomorHpAnak.setText(retrievesPhone);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

    }

    private void UpdateSettings()
    {
        String setUserName = userName.getText().toString();

        if (TextUtils.isEmpty(setUserName))
        {
            Toast.makeText(getApplicationContext(), "Please write your user name first....", Toast.LENGTH_SHORT).show();
        }
        else
        {
            HashMap<String, Object> profileMap = new HashMap<>();
            profileMap.put("name", setUserName);
            RootRef.child("Users").child(currentUserID).updateChildren(profileMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task)
                        {
                            if (task.isSuccessful())
                            {
                                Toast.makeText(getApplicationContext(), "Username Updated Successfully...", Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                String message = task.getException().toString();
                                Toast.makeText(getApplicationContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void changePhotoProfile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

}
