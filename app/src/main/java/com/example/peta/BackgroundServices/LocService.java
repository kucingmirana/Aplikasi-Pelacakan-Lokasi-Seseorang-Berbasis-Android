package com.example.peta.BackgroundServices;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.peta.R;
import com.example.peta.db.DatabaseContract;
import com.example.peta.db.DatabaseHelper;
import com.example.peta.db.Helper;
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
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static android.content.ContentValues.TAG;
import static com.example.peta.db.DatabaseContract.ChildColumns.CHILD_ID;
import static com.example.peta.db.DatabaseContract.ChildColumns.DATE;
import static com.example.peta.db.DatabaseContract.ChildColumns.LATITUDE;
import static com.example.peta.db.DatabaseContract.ChildColumns.LOCATION;
import static com.example.peta.db.DatabaseContract.ChildColumns.LONGITUDE;
import static com.example.peta.db.DatabaseContract.ChildColumns.PARENT_ID;
import static com.example.peta.db.DatabaseContract.ChildColumns.TABLE_NAME;
import static com.example.peta.db.DatabaseContract.ChildColumns.TIME;

public class LocService extends Service {

    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private Location mLocation;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private String phone;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef,UsersRef,ContactsRef;

    Helper helper;
    DatabaseHelper databaseHelper = new DatabaseHelper(this);
    SQLiteDatabase db;
    Cursor ftvcursor,cursor2;


    Geocoder geocoder;

    String currentUserID;
    String saveCurrentTime;
    boolean CheckDb;

    PowerManager.WakeLock wakeLock;
    WifiManager.WifiLock wfl;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(1101, getNotification());
        }

        mAuth = FirebaseAuth.getInstance();
        RootRef = FirebaseDatabase.getInstance().getReference();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        geocoder = new Geocoder(this, Locale.getDefault());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire(10000L);

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wfl = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "sync_all_wifi");
        wfl.acquire();

        new Thread(new Runnable() {
            @Override
            public void run() {
                fn_location();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    stopSelf();
                }
            }
        }).start();
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        return Service.START_STICKY;
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

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    private void fn_location(){
        ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {


            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities != null) {
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {

                        if (mAuth.getCurrentUser() != null) {

                            VerifyChildOrParent();

                        }
                    }
                }
                else {
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), getApplicationContext().getString(R.string.app_name))
                            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                            .setContentTitle("Fail Location Update")
                            .setContentText("No Internet Connection")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify(1004, builder.build());
                    Log.w(TAG, "Failed to get location.");
                }
            } else {

                try {
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
                        if (mAuth.getCurrentUser() != null) {

                            VerifyChildOrParent();

                        }
                    }
                    else {
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), getApplicationContext().getString(R.string.app_name))
                                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                                .setContentTitle("Fail Location Update")
                                .setContentText("No Internet Connection")
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

                        // notificationId is a unique int for each notification that you must define
                        notificationManager.notify(1004, builder.build());
                        Log.w(TAG, "Failed to get location.");
                    }
                } catch (Exception e) {
                    Log.i("FailLocationUpdate", "" + e.getMessage());
                }
            }
        }

    }

    private Notification getNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getApplicationContext().getString(R.string.app_name);
            String description = getApplicationContext().getString(R.string.app_name);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(getApplicationContext().getString(R.string.app_name), name, importance);
            channel.setDescription(description);
            channel.setSound(null,null);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getApplicationContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = null;
        notificationBuilder = new NotificationCompat.Builder(getApplicationContext(),
                getApplicationContext().getString(R.string.app_name))
                .setContentTitle("TRACKER APP")
                .setContentText("Memperbarui Lokasi.....")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true);

        return notificationBuilder.build();
    }

    private void VerifyChildOrParent(){
        String currentUserID = mAuth.getCurrentUser().getUid();
        UsersRef.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.child("status").getValue().equals("parent")){
                    RequestChildLocation();
                }
                else {
                    UpdateChildLocation();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void UpdateChildLocation(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                currentUserID = mAuth.getCurrentUser().getUid();
                Calendar calendar = Calendar.getInstance();

                SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm");
                saveCurrentTime = currentTime.format(calendar.getTime());

                Date c = Calendar.getInstance().getTime();

                SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
                final String formattedDate = df.format(c);

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
                        mLocationRequest.setInterval(1000);
                        mLocationRequest.setFastestInterval(500);
                        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                    }
                    //Access UI for some activity
                });

                try {
                    Thread.sleep(5000);
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
                                            HashMap<String, Object> profileMap = new HashMap<>();
                                            profileMap.put("time", saveCurrentTime);
                                            profileMap.put("tanggal", formattedDate);
                                            profileMap.put("latitude", mLocation.getLatitude());
                                            profileMap.put("longitude", mLocation.getLongitude());
                                            RootRef.child("Users").child(currentUserID).child("location").updateChildren(profileMap)
                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<Void> task)
                                                        {
                                                            if (task.isSuccessful())
                                                            {
                                                                Toast.makeText(LocService.this, "Success: tambah lokasi dan jam", Toast.LENGTH_SHORT).show();
                                                            }
                                                            else
                                                            {
                                                                String message = task.getException().toString();
                                                                Toast.makeText(LocService.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                                                            }
                                                        }
                                                    });

                                            Log.i(TAG, "success to get location.");

                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), getApplicationContext().getString(R.string.app_name))
                                                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                                                    .setContentTitle("New Location Update")
                                                    .setContentText("You are at " + mLocation.getLatitude() +","+ mLocation.getLongitude())
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                                    .setStyle(new NotificationCompat.BigTextStyle().bigText("You are at " + mLocation.getLatitude() +" " + mLocation.getLongitude()));

                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

                                            // notificationId is a unique int for each notification that you must define
                                            notificationManager.notify(1001, builder.build());

                                            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
                                        } else {
                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), getApplicationContext().getString(R.string.app_name))
                                                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                                                    .setContentTitle("Fail Location Update")
                                                    .setContentText(phone + " at " + 0 +","+ 0)
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

                                            // notificationId is a unique int for each notification that you must define
                                            notificationManager.notify(1003, builder.build());
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

    private void RequestChildLocation(){
        currentUserID = mAuth.getCurrentUser().getUid();

        ContactsRef.child(currentUserID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    for (final DataSnapshot childSnapshot : dataSnapshot.getChildren()){
                        Log.d("child", childSnapshot.getKey());
                        UsersRef.child(childSnapshot.getKey()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()){
                                    if (dataSnapshot.hasChild("location")){
                                        String alamat;
                                        String placeName;

                                        String child = dataSnapshot.getKey();
                                        String time = dataSnapshot.child("location").child("time").getValue().toString();
                                        String tanggal = dataSnapshot.child("location").child("tanggal").getValue().toString();
                                        String latitude = dataSnapshot.child("location").child("latitude").getValue().toString();
                                        String longitude = dataSnapshot.child("location").child("longitude").getValue().toString();

                                        Double nLat = Double.parseDouble(latitude);
                                        Double nLong = Double.parseDouble(longitude);

                                        List<Address> addresses;
                                        try {

                                            addresses = geocoder.getFromLocation(nLat, nLong, 1);
                                            alamat = addresses.get(0).getAddressLine(0);

                                            db = databaseHelper.getReadableDatabase();

                                            ftvcursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + "", null);
                                            while (ftvcursor.moveToNext()) {
                                                int id = ftvcursor.getInt(ftvcursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns._ID));
                                                String parentId = ftvcursor.getString(ftvcursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns.PARENT_ID));
                                                String childId = ftvcursor.getString(ftvcursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns.CHILD_ID));
                                                String date = ftvcursor.getString(ftvcursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns.DATE));
                                                String waktu = ftvcursor.getString(ftvcursor.getColumnIndexOrThrow(DatabaseContract.ChildColumns.TIME));

                                                if (parentId.equals(currentUserID) && childId.equals(child) && date.equals(tanggal)
                                                        && waktu.equals(time)) {
                                                    Log.d("DATABASE_CHECk", "DATA ALREADY EXIST");
                                                    CheckDb = true;
                                                    break;
                                                }else {
                                                    Log.d("DATABASE_CHECk", id + "DATA NOT EXIST");
                                                }
                                            }

                                            if (CheckDb){
                                                CheckDb= false;
                                                Log.d("DATABASE_CHECk", "yes");
                                            }else {
                                                Log.d("DATABASE_CHECk", "no");
                                                String count = "SELECT count(*) FROM "+ TABLE_NAME +"";
                                                Cursor mcursor = db.rawQuery(count, null);
                                                mcursor.moveToFirst();
                                                int icount = mcursor.getInt(0);
                                                if(icount==0){
                                                    helper = Helper.getInstance(getApplicationContext());
                                                    helper.open();
                                                    ContentValues values = new ContentValues();
                                                    values.put(PARENT_ID, currentUserID);
                                                    values.put(CHILD_ID, childSnapshot.getKey());
                                                    values.put(LOCATION, alamat);
                                                    values.put(LATITUDE, latitude);
                                                    values.put(LONGITUDE, longitude);
                                                    values.put(DATE, tanggal);
                                                    values.put(TIME, time);
                                                    helper.insert(values);
                                                    helper.close();
                                                    mcursor.close();
                                                }
                                                else{
                                                    mcursor.close();
                                                    cursor2 = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + DatabaseContract.ChildColumns._ID + " DESC LIMIT 1", null);
                                                    while (cursor2.moveToNext())
                                                    {
                                                        String lokasi = cursor2.getString(cursor2.getColumnIndexOrThrow(LOCATION));
                                                        String parentId = cursor2.getString(cursor2.getColumnIndexOrThrow(DatabaseContract.ChildColumns.PARENT_ID));
                                                        if (!parentId.equals(currentUserID)){
                                                            helper = Helper.getInstance(getApplicationContext());
                                                            helper.open();
                                                            ContentValues values = new ContentValues();
                                                            values.put(PARENT_ID, currentUserID);
                                                            values.put(CHILD_ID, childSnapshot.getKey());
                                                            values.put(LOCATION, alamat);
                                                            values.put(LATITUDE, latitude);
                                                            values.put(LONGITUDE, longitude);
                                                            values.put(DATE, tanggal);
                                                            values.put(TIME, time);
                                                            helper.insert(values);
                                                            helper.close();
                                                            cursor2.close();
                                                        }
                                                        if (parentId.equals(currentUserID)){
                                                            if (!lokasi.equals(alamat)){
                                                                helper = Helper.getInstance(getApplicationContext());
                                                                helper.open();
                                                                ContentValues values = new ContentValues();
                                                                values.put(PARENT_ID, currentUserID);
                                                                values.put(CHILD_ID, childSnapshot.getKey());
                                                                values.put(LOCATION, alamat);
                                                                values.put(LATITUDE, latitude);
                                                                values.put(LONGITUDE, longitude);
                                                                values.put(DATE, tanggal);
                                                                values.put(TIME, time);
                                                                helper.insert(values);
                                                                helper.close();
                                                                cursor2.close();
                                                            }
                                                        }

                                                    }
                                                }


                                            }

                                            ftvcursor.close();
                                            db.close();

                                        } catch (IOException e) {
                                            e.printStackTrace();
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

}
