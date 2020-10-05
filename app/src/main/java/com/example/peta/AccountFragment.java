package com.example.peta;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.peta.BackgroundServices.SendLocation3;
import com.example.peta.db.DatabaseHelper;
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

import static android.content.Context.ALARM_SERVICE;
import static com.example.peta.db.DatabaseContract.ChildColumns.TABLE_NAME;


/**
 * A simple {@link Fragment} subclass.
 */
public class AccountFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;

    private Button UpdateAccountSettings;
    private EditText userName;
    private TextView userStatus,userPhone;
    private CircleImageView userProfileImage;
    private ImageView logout_btn,deleteAccount;

    private String currentUserID;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef,ContactsRef,UsersRef,MessageRef,TokenRef,AddRequestRef;
    private StorageReference UserProfileImagesRef;

    private ProgressDialog loadingBar;

    private Uri mImageUri;

    private Helper helper;

    public AccountFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_account, container, false);
        UpdateAccountSettings = v.findViewById(R.id.update_settings_button);
        userName = v.findViewById(R.id.set_user_name);
        userStatus = v.findViewById(R.id.setting_status);
        userPhone = v.findViewById(R.id.set_nomor_hp_ortu);
        userProfileImage = v.findViewById(R.id.set_profile_image);
        logout_btn = v.findViewById(R.id.logout_from_acc_frag);
        deleteAccount = v.findViewById(R.id.img_hapus_akun_ortu);

        deleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext());
                alert.setTitle("Delete Account");
                alert.setMessage("Are you sure want to delete your account?");
                alert.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Prompt the user once explanation has been shown
                        deleteAnak();

                    }
                });

                alert.show();
            }
        });

        logout_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(ALARM_SERVICE);
                Intent intent = new Intent(getContext(), SendLocation3.class);
                boolean alarmUp = (PendingIntent.getBroadcast(getContext(), 12,
                        intent,
                        PendingIntent.FLAG_NO_CREATE) != null);

                if (alarmUp) {
                    Log.d("myTaghapus", "Alarm terminated");
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 12, intent, PendingIntent.FLAG_CANCEL_CURRENT);
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
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        MessageRef = FirebaseDatabase.getInstance().getReference().child("Messages");
        TokenRef = FirebaseDatabase.getInstance().getReference().child("Tokens");
        AddRequestRef = FirebaseDatabase.getInstance().getReference().child("Add Requests");

        UserProfileImagesRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        loadingBar = new ProgressDialog(getContext());

        RetrieveUserInfo();

        UpdateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                UpdateSettings();
            }
        });

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                changePhotoProfile();
            }
        });

        return v;
    }

    private void deleteAnak(){
        ContactsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    for (final DataSnapshot childSnapshot : snapshot.getChildren()){
                        DeleteContact(childSnapshot.getKey());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        deleteAkun();
    }

    private void DeleteContact(final String childID)
    {
        ContactsRef.child(currentUserID).child(childID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task)
                    {
                        if (task.isSuccessful())
                        {
                            ContactsRef.child(childID).child(currentUserID)
                                    .removeValue();
                        }
                    }
                });

        MessageRef.child(childID)
                .removeValue();

    }

    private void deleteAkun(){
        MessageRef.child(currentUserID)
                .removeValue();
        AddRequestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild(currentUserID)){
                    for (DataSnapshot childSnapshot : snapshot.child(currentUserID).getChildren()){
                        AddRequestRef.child(currentUserID).removeValue();
                        AddRequestRef.child(childSnapshot.getKey()).removeValue();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

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
                                                helper = Helper.getInstance(getContext());
                                                helper.open();
                                                helper.deleteTable();
                                                helper.close();

                                                AlarmManager alarmMgr = (AlarmManager) getContext().getSystemService(ALARM_SERVICE);
                                                Intent intent = new Intent(getContext(), SendLocation3.class);
                                                boolean alarmUp = (PendingIntent.getBroadcast(getContext(), 12,
                                                        intent,
                                                        PendingIntent.FLAG_NO_CREATE) != null);

                                                if (alarmUp) {
                                                    Log.d("myTaghapus", "Alarm terminated");
                                                    PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 12, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                                                    pendingIntent.cancel();
                                                    alarmMgr.cancel(pendingIntent);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == PICK_IMAGE_REQUEST && data != null && data.getData() != null) {
            mImageUri = data.getData();
            Picasso.get().load(mImageUri).fit().centerInside().into(userProfileImage);
            //imgProfile.setImageURI(mImageUri);
            sendImageProfileData();
        }
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getActivity().getContentResolver();
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
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
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
                        String retrievesPhone = dataSnapshot.child("phone").getValue().toString();

                        if (dataSnapshot.hasChild("image"))
                        {
                            String retrieveImage = dataSnapshot.child("image").getValue().toString();
                            Picasso.get().load(retrieveImage).fit().centerInside().into(userProfileImage);
                            Log.d("childimage", retrieveImage);
                        }
                        if (retrievesStatus.equals("parent")){
                            userStatus.setText("tracker");
                        }

                        userName.setText(retrieveUserName);
                        userPhone.setText("+62"+retrievesPhone);

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
            Toast.makeText(getContext(), "Please write your user name first....", Toast.LENGTH_SHORT).show();
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
                                Toast.makeText(getContext(), "Profile Updated Successfully...", Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                String message = task.getException().toString();
                                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
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

    private void SendUserToLoginActivity()
    {
        Intent loginIntent = new Intent(getContext(), PhoneLoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
        Toast.makeText(getContext(), "Please wait for 5 minutes to login again", Toast.LENGTH_SHORT).show();
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

}
