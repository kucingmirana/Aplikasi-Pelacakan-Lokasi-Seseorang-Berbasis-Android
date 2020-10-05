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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;

import com.example.peta.BackgroundServices.SendLocation3;
import com.example.peta.Notification.Token;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseUser currentUser;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private String currentUserID;

    int PERMISSION_ALL = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        currentUserID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();

        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(MainActivity.this, new
                OnSuccessListener<InstanceIdResult>() {
                    @Override
                    public void onSuccess(InstanceIdResult instanceIdResult) {
                        updateToken(instanceIdResult.getToken());
                    }
                });

    }

    @Override
    protected void onStart() {
        super.onStart();
        scheduleNotify();
        updateUserStatus("online");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(!canDrawOverlays(MainActivity.this)){
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

        final String[] PERMISSIONS = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.READ_SMS,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS
        };
        if (!hasPermissions(this, PERMISSIONS)) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Permission Needed");
            alert.setMessage("Please let app have permission for receiving sms and send sms, get location, read contacts and make phone call");
            alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    //Prompt the user once explanation has been shown
                    ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, PERMISSION_ALL);
                }
            });

            alert.show();

        }

        BottomNavigationView navView = findViewById(R.id.bottomNavigationView);
        Fragment fragment1;
        fragment1 = new MapFragment();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.FragmentView, fragment1, fragment1.getClass().getSimpleName())
                .commit();
        navView.getMenu().findItem(R.id.navigation_lokasi).setChecked(true);
        navView.setOnNavigationItemSelectedListener(NavigationSelected);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener NavigationSelected = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            Fragment fragment;
            switch (menuItem.getItemId()){
                case R.id.navigation_list_tracked:
                    fragment = new FragmentUserProfile();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.FragmentView, fragment, fragment.getClass().getSimpleName())
                            .commit();
                    return true;
                case R.id.navigation_lokasi:
                    fragment = new MapFragment();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.FragmentView, fragment, fragment.getClass().getSimpleName())
                            .commit();
                    return true;
                case R.id.navigation_akun:
                    fragment = new AccountFragment();
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.FragmentView, fragment, fragment.getClass().getSimpleName())
                            .commit();
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

    private void updateToken(String token){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token1 = new Token(token);
        reference.child(currentUserID).setValue(token1);
    }

    private void scheduleNotify()
    {
        Intent intent = new Intent(this, SendLocation3.class);
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
