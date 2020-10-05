package com.example.peta;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.peta.BackgroundServices.SendLocation3;
import com.example.peta.Notification.Data;
import com.example.peta.Notification.Sender;
import com.example.peta.Notification.Token;
import com.example.peta.db.DatabaseContract;
import com.example.peta.db.DatabaseContractChip;
import com.example.peta.db.DatabaseHelper;
import com.example.peta.db.Helper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipDrawable;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.gson.Gson;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.provider.BaseColumns._ID;

public class ChildMainActivity extends AppCompatActivity {

    int i;

    private ChipGroup chipGroup;

    CircleImageView parentProfileImage,settingsProfileImage;

    private TextView tvGoChat;

    Helper helper;

    private DatabaseReference ChildRef,RootRef,UsersRef,ContactsRef;
    private FirebaseAuth mAuth;

    private String saveCurrentTime, saveCurrentDate;
    private String messageReceiverID, messageReceiverName, messageReceiverImage, parentPhone;

    DatabaseHelper databaseHelper = new DatabaseHelper(this);
    SQLiteDatabase db;
    Cursor ftvcursor;

    private RequestQueue requestQueue;

    private boolean notify = false;

    private String currentUserID="", parent;

    private ValueEventListener mDbListener;

    int PERMISSION_ALL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_main);

        chipGroup = findViewById(R.id.chip_group_messages);
        tvGoChat = findViewById(R.id.tv_go_chat);
        parentProfileImage = findViewById(R.id.child_main_parent_profile);
        settingsProfileImage = findViewById(R.id.child_main_settings_profile);

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        ChildRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        RootRef = FirebaseDatabase.getInstance().getReference();
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        parentPhone = getIntent().getExtras().get("parent_phone").toString();
        messageReceiverID = getIntent().getExtras().get("parent_id").toString();
        messageReceiverName = getIntent().getExtras().get("parent_name").toString();
        messageReceiverImage = getIntent().getExtras().get("parent_image").toString();

        UsersRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild("image"))
                {
                    String retImage = dataSnapshot.child("image").getValue().toString();
                    Picasso.get().load(retImage).into(settingsProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if (!messageReceiverImage.equals("default_image")){
            Picasso.get().load(messageReceiverImage).fit().centerInside().into(parentProfileImage);
        }

        parentProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent profileIntent = new Intent(ChildMainActivity.this, ProfileActivity.class);
                profileIntent.putExtra("visit_user_id", messageReceiverID);
                startActivity(profileIntent);
            }
        });

        settingsProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent accountIntent = new Intent(ChildMainActivity.this, ChildAccountActivity.class);
                accountIntent.putExtra("parent_id",messageReceiverID);
                startActivity(accountIntent);
            }
        });

        tvGoChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chatIntent = new Intent(ChildMainActivity.this, ChatActivity.class);
                chatIntent.putExtra("visit_user_id", messageReceiverID);
                chatIntent.putExtra("visit_user_name", messageReceiverName);
                chatIntent.putExtra("visit_image", messageReceiverImage);
                startActivity(chatIntent);
            }
        });

        BottomNavigationView navViewChild = findViewById(R.id.bottomNavigationViewChild);
        navViewChild.getMenu().findItem(R.id.navigation_alert_sos).setChecked(true);
        navViewChild.setOnNavigationItemSelectedListener(NavigationSelectedChild);

        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calendar.getTime());

        checkParent();
        setAddChip();
        checkDatabaseChip();

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(ChildMainActivity.this, new
                OnSuccessListener<InstanceIdResult>() {
                    @Override
                    public void onSuccess(InstanceIdResult instanceIdResult) {
                        updateToken(instanceIdResult.getToken());
                    }
                });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!canDrawOverlays(ChildMainActivity.this)){
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Permission Needed");
            alert.setMessage("Please enable make app show on front window");
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //Prompt the user once explanation has been shown
                    requestPermission(1234);;
                }
            });

            alert.show();

        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            final String[] PERMISSIONS = {
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.CALL_PHONE
            };
            if (!hasPermissions(this, PERMISSIONS)) {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Permission Needed");
                alert.setMessage("Please let app have permission for receiving sms and send sms, get location from background  and make phone call");
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Prompt the user once explanation has been shown
                        ActivityCompat.requestPermissions(ChildMainActivity.this, PERMISSIONS, PERMISSION_ALL);
                    }
                });

                alert.show();

            }
        }
        else {
            final String[] PERMISSIONS = {
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_SMS,
                    Manifest.permission.CALL_PHONE
            };
            if (!hasPermissions(this, PERMISSIONS)) {
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Permission Needed");
                alert.setMessage("Please let app have permission for receiving sms and send sms, get location and make phone call ");
                alert.setPositiveButton("Check Permission", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //Prompt the user once explanation has been shown
                        ActivityCompat.requestPermissions(ChildMainActivity.this, PERMISSIONS, PERMISSION_ALL);
                    }
                });

                alert.show();

            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        scheduleNotify();
        updateUserStatus("online");
    }

    private BottomNavigationView.OnNavigationItemSelectedListener NavigationSelectedChild = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            Fragment fragment;
            switch (menuItem.getItemId()){
                case R.id.navigation_chat:
                    Intent chatIntent = new Intent(ChildMainActivity.this, ChatActivity.class);
                    chatIntent.putExtra("visit_user_id", messageReceiverID);
                    chatIntent.putExtra("visit_user_name", messageReceiverName);
                    chatIntent.putExtra("visit_image", messageReceiverImage);
                    startActivity(chatIntent);
                    return true;
                case R.id.navigation_alert_sos:
                    sendMessageToParent("sos");
                    return true;
                case R.id.navigation_phone:
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:"+"+62"+parentPhone));
                    startActivity(callIntent);
                    return true;
            }
            return false;
        }
    };

    private static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void requestPermission(int requestCode){
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, requestCode);
    }

    private static boolean canDrawOverlays(Context context){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }else{
            return Settings.canDrawOverlays(context);
        }
    }

    private void updateToken(String token){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token1 = new Token(token);
        reference.child(currentUserID).setValue(token1);
    }

    private void checkParent(){
        mDbListener = ContactsRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()){
                    UsersRef.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            String phoneNumber = dataSnapshot.child("phone").getValue().toString();
                            String childName = dataSnapshot.child("name").getValue().toString();

                            helper = Helper.getInstance(getApplicationContext());
                            helper.open();
                            helper.deleteByIdParent(Integer.toString(1));
                            helper.close();
                            ContactsRef.child(currentUserID).removeEventListener(mDbListener);

                            Intent addParentIntent =  new Intent(ChildMainActivity.this, AddParent.class);
                            addParentIntent.putExtra("child_phone_number", phoneNumber);
                            addParentIntent.putExtra("child_name", childName);
                            startActivity(addParentIntent);
                            finish();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void checkDatabaseChip(){
        db = databaseHelper.getReadableDatabase();

        ftvcursor = db.rawQuery("SELECT * FROM " + DatabaseContractChip.ChipColumns.TABLE_NAME + "", null);
        while (ftvcursor.moveToNext()) {
            int id = ftvcursor.getInt(ftvcursor.getColumnIndexOrThrow(DatabaseContractChip.ChipColumns._ID));
            String childId = ftvcursor.getString(ftvcursor.getColumnIndexOrThrow(DatabaseContractChip.ChipColumns.CHILD_ID));
            String chipText = ftvcursor.getString(ftvcursor.getColumnIndexOrThrow(DatabaseContractChip.ChipColumns.CHIP_TEXT));
            if (childId.equals(currentUserID)){
                i=id;
                addChip(chipText, id);
            }

        }

        ftvcursor.close();
        db.close();
    }

    private void setAddChip(){
        Chip chip = new Chip(this);
        ChipDrawable drawable1 = ChipDrawable.createFromAttributes(ChildMainActivity.this, null,
                0,R.style.Widget_MaterialComponents_Chip_Action);
        chip.setChipDrawable(drawable1);
        chip.setCheckable(false);
        chip.setClickable(true);
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addTextAlert();
            }
        });
        chip.setChipIconResource(R.drawable.ic_add_black_24dp);
        chip.setIconStartPadding(25f);
        chipGroup.addView(chip);
    }

    private void addTextAlert(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Masukan Pesan Singkat:");

// Set an EditText view to get user input
        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                i++;
                // Do something with value!
                helper = Helper.getInstance(getApplicationContext());
                helper.open();
                ContentValues values = new ContentValues();
                values.put(_ID,i);
                values.put(DatabaseContractChip.ChipColumns.CHILD_ID, currentUserID);
                values.put(DatabaseContractChip.ChipColumns.CHIP_TEXT, input.getText().toString());
                helper.insertChip(values);
                helper.close();
                final Chip chip = new Chip(ChildMainActivity.this);
                ChipDrawable drawable = ChipDrawable.createFromAttributes(ChildMainActivity.this, null,
                        0,R.style.Widget_MaterialComponents_Chip_Entry);
                chip.setChipDrawable(drawable);
                chip.setCheckable(false);
                chip.setClickable(true);
                chip.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        sendMessageToParent(input.getText().toString());
                        Toast.makeText(ChildMainActivity.this, chip.getText().toString(), Toast.LENGTH_SHORT).show();
                    }
                });
                chip.setChipIconResource(R.drawable.ic_account_circle_black_24dp);
                chip.setIconStartPadding(3f);
                chip.setPadding(60,10,60,10);
                chip.setText(input.getText().toString());
                chip.setOnCloseIconClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        i--;
                        helper = Helper.getInstance(getApplicationContext());
                        helper.open();
                        helper.deleteByIdChip(Integer.toString(i));
                        helper.close();
                        chipGroup.removeView(view);
                    }
                });
                chipGroup.addView(chip);

            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    private void addChip(final String text, final int id){
        final Chip chip = new Chip(ChildMainActivity.this);
        ChipDrawable drawable = ChipDrawable.createFromAttributes(ChildMainActivity.this, null,
                0,R.style.Widget_MaterialComponents_Chip_Entry);
        chip.setChipDrawable(drawable);
        chip.setCheckable(false);
        chip.setClickable(true);
        chip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessageToParent(text);
                Toast.makeText(ChildMainActivity.this, chip.getText().toString(), Toast.LENGTH_SHORT).show();
            }
        });
        chip.setChipIconResource(R.drawable.ic_account_circle_black_24dp);
        chip.setIconStartPadding(3f);
        chip.setPadding(60,10,60,10);
        chip.setText(text);
        chip.setOnCloseIconClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helper = Helper.getInstance(getApplicationContext());
                helper.open();
                helper.deleteByIdChip(Integer.toString(id));
                helper.close();
                chipGroup.removeView(view);
            }
        });
        chipGroup.addView(chip);
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

    private void sendMessageToParent(final String textMsg){
        ChildRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()){

                        parent = childSnapshot.getKey();

                        UsersRef.child(parent).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot)
                            {
                                final String[] retImage = {"default_image"};
                                if (dataSnapshot.exists())
                                {
                                    if (dataSnapshot.hasChild("image"))
                                    {
                                        retImage[0] = dataSnapshot.child("image").getValue().toString();
                                    }

                                    String retName = dataSnapshot.child("name").getValue().toString();

                                    SendMessage(parent, textMsg);


                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void SendMessage(final String parentId, String chipMessage)
    {
        notify =true;
        final String messageText = chipMessage;

        String messageSenderRef = "Messages/" + currentUserID + "/" + parentId;
        String messageReceiverRef = "Messages/" + parentId + "/" + currentUserID;

        DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                .child(currentUserID).child(parentId).push();

        String messagePushID = userMessageKeyRef.getKey();

        Map messageTextBody = new HashMap();
        messageTextBody.put("message", messageText);
        messageTextBody.put("type", "text");
        messageTextBody.put("from", currentUserID);
        messageTextBody.put("to", parentId);
        messageTextBody.put("messageID", messagePushID);
        messageTextBody.put("time", saveCurrentTime);
        messageTextBody.put("date", saveCurrentDate);

        Map messageBodyDetails = new HashMap();
        messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
        messageBodyDetails.put( messageReceiverRef + "/" + messagePushID, messageTextBody);

        RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task)
            {
                if (task.isSuccessful())
                {
                    Toast.makeText(ChildMainActivity.this, "Message Sent Successfully...", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(ChildMainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                }
            }
        });

        final DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(currentUserID);
        database.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Contact user = dataSnapshot.getValue(Contact.class);

                String[] retImage = {"default_image"};
                String status = dataSnapshot.child("status").getValue().toString();

                if (dataSnapshot.hasChild("image"))
                {
                    retImage[0] = dataSnapshot.child("image").getValue().toString();
                }

                if (notify){
                    sendNotification(messageReceiverID, user.getName(),retImage[0], messageText, status);
                }
                    notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void sendNotification(String receiver, final String username, final String userImage, final String message, final String userStatus){
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(receiver);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(currentUserID, R.mipmap.ic_launcher, username+": "+message, "New Message",
                            messageReceiverID,username,userImage,userStatus, message);

                    Sender sender = new Sender(data, token.getToken(), "high", 10);

                    //fcm json object request

                    try {
                        JSONObject senderJsonObj = new JSONObject(new Gson().toJson(sender));
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send",
                                senderJsonObj,
                                new Response.Listener<JSONObject>() {
                                    @Override
                                    public void onResponse(JSONObject response) {
                                        //MyResponse of the request
                                        Log.d("JSON_RESPONSE", "onResponse:" + response.toString());
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("JSON_RESPONSE", "onResponse:" + error.toString());
                            }
                        }){
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                //put params
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Content-Type", "application/json");
                                headers.put("Authorization", "key=AAAA-iEmlTI:APA91bFVnMe5SsV52b7M8w5U9wcb5fQiEHFYG62aqDHSkDZyqokopL6r_Ncp7iQyqnENuRjjsIOctHHpMNGxy6vpfpToa3kUS6W337RTnzcAtM2NtPUhbkbffWCq-Kn-Wzj1KAZvAdS7");

                                return headers;
                            }
                        };

                        //add this request to queue
                        requestQueue.add(jsonObjectRequest);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void scheduleNotify()
    {
        Intent intent = new Intent(getApplicationContext(), SendLocation3.class);
        boolean alarmUp = (PendingIntent.getBroadcast(getApplicationContext(), 12,
                intent,
                PendingIntent.FLAG_NO_CREATE) != null);

        if (alarmUp) {
            Log.d("myTag", "Alarm is already active");
        }
        else {
            Log.d("myTag", "Alarm is not active");
            Intent intent2 = new Intent(getApplicationContext(), SendLocation3.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 12, intent2, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager alarmMgr = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
            alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,0,1000*60*5,pendingIntent);
        }
    }

}
