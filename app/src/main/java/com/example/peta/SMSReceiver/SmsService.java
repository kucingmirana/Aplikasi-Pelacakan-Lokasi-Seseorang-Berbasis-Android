package com.example.peta.SMSReceiver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.telephony.SmsManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.peta.BackgroundServices.LocService;
import com.example.peta.MainActivity;
import com.example.peta.R;
import com.example.peta.db.DatabaseContract;
import com.example.peta.db.DatabaseContractChild;
import com.example.peta.db.DatabaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static android.content.ContentValues.TAG;

public class SmsService extends Service{

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    Location mLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;

    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef,ChildRef;

    Context mContext;

    SQLiteDatabase db;
    Cursor ftvcursor;

    String phoneNumber;
    String msg;

    PowerManager.WakeLock wakeLock;
    WifiManager.WifiLock wfl;

    Intent intentData;

    LocationManager locationManager;
    LocationListener locationListener;

    private class LocationListener implements android.location.LocationListener {

        Location mLastLocation;

        public LocationListener(String provider) {
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            mLastLocation.set(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }


    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1090, getNotification());
        }


        mAuth = FirebaseAuth.getInstance();

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

        phoneNumber = intent.getStringExtra("parent_no");
        msg = intent.getStringExtra("message");
        Log.d("intentphone", phoneNumber + " " + msg);

        new Thread(new Runnable() {
            @Override
            public void run() {

                CheckDatabase(phoneNumber,msg);
                try {
                    Thread.sleep(5000);
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
                .setContentText("Check SMS")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true);

        return notificationBuilder.build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void CheckDatabase(String phoneNumber, String msg){
        if (msg.equals("location")){
            DatabaseHelper databaseHelper = new DatabaseHelper(getApplicationContext());
            db = databaseHelper.getReadableDatabase();
            ftvcursor = db.rawQuery("SELECT * FROM " + DatabaseContractChild.ParentColumns.TABLE_NAME + "", null);
            while (ftvcursor.moveToNext()) {
                String parentNo = ftvcursor.getString(ftvcursor.getColumnIndexOrThrow(DatabaseContractChild.ParentColumns.PARENT_NO));
                String childName = ftvcursor.getString(ftvcursor.getColumnIndexOrThrow(DatabaseContractChild.ParentColumns.CHILD_NAME));
                String childStatus = ftvcursor.getString(ftvcursor.getColumnIndexOrThrow(DatabaseContractChild.ParentColumns.CHILD_STATUS));
                if (childStatus.equals("child")){
                    if (phoneNumber.equals(parentNo)){
                        fn_location(parentNo,childName);
                        Log.i("pesanmasukdb", ftvcursor.getString(ftvcursor.getColumnIndexOrThrow(DatabaseContractChild.ParentColumns.PARENT_NO)) + " " +  phoneNumber + " success");

                    }
                }

            }
        }

        if (msg.equals("sos")){
            checkParent(phoneNumber, msg);
        }

    }

    private void fn_location(final String parentNo, final String childName){
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {


            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {

                        if (mAuth.getCurrentUser() != null) {

                            requestLocation(parentNo,childName);

                        }
                    }
                }
                else {
                    requestLocationOffline(parentNo, childName);
                }
            }
            else {
                try {
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                        if (mAuth.getCurrentUser() != null) {

                            requestLocation(parentNo, childName);

                        }
                    }
                    else {

                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                requestLocationOffline(parentNo, childName);
                            }
                        });
                    }
                } catch (Exception e) {
                    Log.i("FailLocationUpdate", "" + e.getMessage());
                }
            }
        }

    }

    private void requestLocationOffline(final String parentNo, final String childName){

        new Thread(new Runnable() {
            @Override
            public void run() {

                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);

                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                0,
                                0, new SmsService.LocationListener(LocationManager.GPS_PROVIDER));
                    }
                    //Access UI for some activity
                });

                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    final Location locationSaved = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (locationSaved != null) {
                        Handler handler2 = new Handler(Looper.getMainLooper());
                        handler2.post(new Runnable() {
                            @Override
                            public void run() {
                                smsSendMessage(parentNo, childName, locationSaved.getLatitude(), locationSaved.getLongitude());
                            }
                            //Access UI for some activity
                        });
                    }
                }
            }
        }).start();

    }

    private void checkParent(final String phoneNumber, final String msg){
        mAuth = FirebaseAuth.getInstance();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        String currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("status").getValue().equals("parent")){
                    checkChild(phoneNumber, msg);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void requestLocation(final String parentNo, final String childName){

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());
        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
            }
        };

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        try {
            mFusedLocationClient
                    .getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();

                                smsSendMessage(parentNo, childName, mLocation.getLatitude(), mLocation.getLongitude());

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

    private void smsSendMessage(String parentNo, String childName, Double latitude, Double longitude){

        // Use format with "smsto:" and phone number to create smsNumber.
        String smsNumber = parentNo;
        // Get the text of the sms message.
        String smsMessage = "posisi " + childName + "= " + "maps.google.com/?q=" + latitude + "," + longitude +"&z=15";
        // Set the service center address if needed, otherwise null.
        String scAddress = null;
        // Set pending intents to broadcast
        // when message sent and when delivered, or set to null.
        PendingIntent sentIntent = null, deliveryIntent = null;
        // Use SmsManager.
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(smsNumber, null, smsMessage, null, null);
            Toast.makeText(getApplicationContext(), "SMS Sent Successfully!",
                    Toast.LENGTH_LONG).show();
        }catch (Exception e){

            Toast.makeText(getApplicationContext(),
                    "SMS failed, please try again later ! ",
                    Toast.LENGTH_LONG).show();
            e.printStackTrace();

        }
    }

    private void checkChild(final String phoneNumber, final String msg) {
        Log.d("notifberhasil", "notif berhasil");
        String currentUserID = mAuth.getCurrentUser().getUid();
        mAuth = FirebaseAuth.getInstance();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChildRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);

        ChildRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (DataSnapshot s : dataSnapshot.getChildren()){
                        final String childKey = s.getKey();
                        UsersRef.child(childKey).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.hasChild("phone")){
                                    String retName = dataSnapshot.child("name").getValue().toString();
                                    String retPhone = dataSnapshot.child("phone").getValue().toString();
                                    if (phoneNumber.equals("+62" + retPhone)){
                                        if (msg.equals("sos")){
                                            Log.d("notifberhasil", "notif berhasil");
                                            SOSNotification(retName);
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

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

    public void SOSNotification(String name){
        final AudioManager manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        final MediaPlayer player = MediaPlayer.create(this, notification);
        player.setLooping(true);
        player.start();

        long[] pattern = {1500, 800, 800, 800};

        final Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createWaveform( pattern,0));
        } else {
            //deprecated in API 26
            v.vibrate(pattern, 0);
        }

        int LAYOUT_FLAG;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        } else {
            LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE;
        }

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                // Shrink the window to wrap the content rather than filling the screen
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                LAYOUT_FLAG,
                // Don't let it grab the input focus
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |  WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                |  WindowManager.LayoutParams.FLAG_FULLSCREEN,
                // Make the underlying application window visible through any transparent parts
                PixelFormat.TRANSLUCENT);

        final View myView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.sos_layout,null,false);
        ImageView close;
        TextView childName;
        close = myView.findViewById(R.id.close);
        childName = myView.findViewById(R.id.nama_anak_window);
        childName.setText(name);
// Define the position of the window within the screen
        p.gravity = Gravity.TOP | Gravity.RIGHT;
        p.x = 0;
        p.y = 100;

        final WindowManager windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        windowManager.addView(myView, p);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                float percent = 0.5f;
                int seventyVolume = (int) (maxVolume*percent);
                windowManager.removeView(myView);
                manager.setStreamVolume(AudioManager.STREAM_MUSIC, seventyVolume, 0);
                v.cancel();
                player.stop();
            }
        });
    }

}
