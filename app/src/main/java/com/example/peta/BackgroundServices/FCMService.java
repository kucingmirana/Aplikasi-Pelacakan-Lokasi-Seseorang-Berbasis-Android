package com.example.peta.BackgroundServices;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.peta.Contact;
import com.example.peta.Notification.Data;
import com.example.peta.Notification.Sender;
import com.example.peta.Notification.Token;
import com.example.peta.R;
import com.example.peta.SMSReceiver.SmsService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class FCMService extends Service {

    PowerManager.WakeLock wakeLock;
    WifiManager.WifiLock wfl;

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private FirebaseAuth mAuth;

    private boolean notify = false;

    private String saveCurrentTime, saveCurrentDate;
    private Location mLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private DatabaseReference ChildRef,RootRef,UsersRef;
    private String currentUserID="", parent;

    private RequestQueue requestQueue;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(979, getNotification());
        }

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "peta:wakelock");
        wakeLock.acquire(5000L);

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wfl = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "sync_all_wifi");
        wfl.acquire();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                requestLocation();
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    stopSelf();
                }
            }
        }).start();
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true); //true will remove notification
        }

        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
        wfl.release();

        Toast.makeText(this, "Service Destroy", Toast.LENGTH_LONG).show();
    }

    private Notification getNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getApplicationContext().getString(R.string.app_name);
            String description = getApplicationContext().getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(getApplicationContext().getString(R.string.app_name), name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = null;
        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(),
                getApplicationContext().getString(R.string.app_name))
                .setContentTitle("Child APP")
                .setContentText("Check Chat")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true);

        return notificationBuilder.build();
    }

    private void requestLocation(){

        new Thread(new Runnable() {
            @Override
            public void run() {

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
                        mLocationCallback = new LocationCallback() {
                            @Override
                            public void onLocationResult(LocationResult locationResult) {
                                super.onLocationResult(locationResult);
                            }
                        };

                        LocationRequest mLocationRequest = new LocationRequest();
                        mLocationRequest.setInterval(500);
                        mLocationRequest.setFastestInterval(250);
                        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    }
                    //Access UI for some activity
                });

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        mFusedLocationClient
                                .getLastLocation()
                                .addOnCompleteListener(new OnCompleteListener<Location>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Location> task) {
                                        if (task.isSuccessful() && task.getResult() != null) {
                                            mLocation = task.getResult();

                                            String smsMessage = "posisi saya pada = " + "maps.google.com/?q=" + mLocation.getLatitude() + "," + mLocation.getLongitude() +"&z=15";
                                            sendMessageToParent(smsMessage);

                                            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                                        } else {

                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), getApplicationContext().getString(R.string.app_name))
                                                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                                                    .setContentTitle("Failed Send Location")
                                                    .setContentText("No Gps")
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

                                            // notificationId is a unique int for each notification that you must define
                                            notificationManager.notify(1091, builder.build());
                                            Log.w(TAG, "Failed to get location. no gps");
                                        }
                                    }
                                });
                    } catch (SecurityException unlikely) {
                        Log.e(TAG, "Lost location permission." + unlikely);
                    }
                }
            }
        }).start();

    }

    private void sendMessageToParent(final String textMsg){

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        ChildRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

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
        Calendar calendar = Calendar.getInstance();

        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a");
        saveCurrentTime = currentTime.format(calendar.getTime());

        RootRef = FirebaseDatabase.getInstance().getReference();
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
                    Toast.makeText(getApplicationContext(), "Message Sent Successfully...", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
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
                    sendNotification(parentId, user.getName(),retImage[0], messageText, status);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void sendNotification(final String receiver, final String username, final String userImage, final String message, final String userStatus){
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(receiver);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()){
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(currentUserID, R.mipmap.ic_launcher, username+": "+message, "New Message",
                            receiver,username,userImage,userStatus, message);

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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
