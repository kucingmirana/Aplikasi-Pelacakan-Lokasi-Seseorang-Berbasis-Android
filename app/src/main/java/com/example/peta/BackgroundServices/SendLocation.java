package com.example.peta.BackgroundServices;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

public class SendLocation extends BroadcastReceiver implements LocationListener {

    @Override
    public void onReceive(final Context mContext, final Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Intent notificationIntent = new Intent(mContext, SendLocation3.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 12, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(mContext.ALARM_SERVICE);
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP,0,1000*60*5,pendingIntent);
        }
    }

    @Override
    public void onLocationChanged(Location location) {

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
