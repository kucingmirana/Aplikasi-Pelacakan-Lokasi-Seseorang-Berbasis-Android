package com.example.peta.Notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
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
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.bumptech.glide.Priority;
import com.example.peta.AddParent;
import com.example.peta.BackgroundServices.FCMService;
import com.example.peta.ChatActivity;
import com.example.peta.ChildMainActivity;
import com.example.peta.Contact;
import com.example.peta.R;
import com.example.peta.db.Helper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static android.content.ContentValues.TAG;

public class FirebaseMessaging extends FirebaseMessagingService {

    PowerManager.WakeLock wakeLock;
    WifiManager.WifiLock wfl;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String sented = remoteMessage.getData().get("sented");
        String user = remoteMessage.getData().get("user");

        String status = remoteMessage.getData().get("status");
        String msg = remoteMessage.getData().get("msg");
        final String name = remoteMessage.getData().get("name");

        SharedPreferences preferences = getSharedPreferences("PREFS", MODE_PRIVATE);
        String currentUser = preferences.getString("currentuser", "none");

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null && sented.equals(firebaseUser.getUid())){
            if (!currentUser.equals(user)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    sendOreoNotification(remoteMessage);
                    if (status.equals("child")){
                        if (msg.equals("sos")){
                            SOSNotification(name);
                        }
                    }
                    if (status.equals("parent")){
                        if (msg.equals("location")){
                            Intent intentservice = new Intent(this,FCMService.class);
                            this.startForegroundService(intentservice);
                        }
                    }

                } else {
                    sendNotification(remoteMessage);
                    if (status.equals("child")){
                        if (msg.equals("sos")){
                            SOSNotification(name);
                        }
                    }
                    if (status.equals("parent")){
                        if (msg.equals("location")){
                            Intent intentservice = new Intent(this,FCMService.class);
                            this.startService(intentservice);
                        }
                    }
                }

            }
        }
    }

    private void sendOreoNotification(RemoteMessage remoteMessage){
        String sented = remoteMessage.getData().get("sented");
        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");
        String name = remoteMessage.getData().get("name");
        String image = remoteMessage.getData().get("image");

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        int j = Integer.parseInt(user.replaceAll("[\\D]", ""));
        Intent intent = new Intent(this, ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("visit_user_id", user);
        bundle.putString("visit_user_name", name);
        bundle.putString("visit_image", image);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT);
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        OreoNotification oreoNotification = new OreoNotification(this);
        Notification.Builder builder = oreoNotification.getOreoNotification(title, body, pendingIntent,
                defaultSound, icon);

        int i = 0;
        if (j > 0){
            i = j;
        }

        oreoNotification.getManager().notify(i, builder.build());

    }

    private void sendNotification(RemoteMessage remoteMessage) {

        String user = remoteMessage.getData().get("user");
        String icon = remoteMessage.getData().get("icon");
        String title = remoteMessage.getData().get("title");
        String body = remoteMessage.getData().get("body");

        RemoteMessage.Notification notification = remoteMessage.getNotification();
        int j = Integer.parseInt(user.replaceAll("[\\D]", ""));
        Intent intent = new Intent(this, ChatActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("visit_user_id", user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, j, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(Integer.parseInt(icon))
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setSound(defaultSound)
                .setContentIntent(pendingIntent);
        NotificationManager noti = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        int i = 0;
        if (j > 0){
            i = j;
        }

        noti.notify(i, builder.build());
    }

    @Override
    public void onNewToken(@NonNull String s) {
        super.onNewToken(s);
        //update user token

        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser!= null){
            //signed in update token
            updateToken(s);
        }

    }

    private void updateToken(String tokenRefresh) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Tokens");

        Token token = new Token(tokenRefresh);
        databaseReference.child(firebaseUser.getUid()).setValue(token);
    }

    public void SOSNotification(final String name){
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

        final WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                // Shrink the window to wrap the content rather than filling the screen
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                LAYOUT_FLAG,
                // Don't let it grab the input focus
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |  WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        |  WindowManager.LayoutParams.FLAG_FULLSCREEN,
                // Make the underlying application window visible through any transparent parts
                PixelFormat.TRANSLUCENT);

// Define the position of the window within the screen
        p.gravity = Gravity.TOP | Gravity.RIGHT;
        p.x = 0;
        p.y = 100;

        final WindowManager windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                final View myView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.sos_layout,null,false);
                ImageView close;
                TextView childName;
                Button goToApp;
                goToApp = myView.findViewById(R.id.go_to_app);
                close = myView.findViewById(R.id.close);
                childName = myView.findViewById(R.id.nama_anak_window);
                childName.setText(name);
                windowManager.addView(myView, p);
                goToApp.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        int maxVolume = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        float percent = 0.5f;
                        int seventyVolume = (int) (maxVolume*percent);
                        windowManager.removeView(myView);
                        manager.setStreamVolume(AudioManager.STREAM_MUSIC, seventyVolume, 0);
                        v.cancel();
                        player.stop();
                        Intent i = getApplicationContext().getPackageManager().getLaunchIntentForPackage(getPackageName());
                        getApplicationContext().startActivity(i);
                    }
                });
                close.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view2) {
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
            //Access UI for some activity
        });
    }

}
