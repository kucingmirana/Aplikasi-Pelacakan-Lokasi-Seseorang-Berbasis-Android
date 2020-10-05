package com.example.peta.SMSReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SimpleSmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")){
            Bundle bundle = intent.getExtras();
            SmsMessage[] msgs = null;
            String msgBody;
            String msg_from;
            if(bundle != null){
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    msgs = new SmsMessage[pdus.length];
                    for (int i = 0; i<msgs.length; i++){
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                            String format = bundle.getString("format");
                            //From PDU we get all object and SmsMessage Object using following line of code
                            msgs[i] = SmsMessage.createFromPdu((byte[])pdus[i], format);
                        }else {
                            msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                        }
                        msg_from = msgs[i].getOriginatingAddress();
                        msgBody = msgs[i].getMessageBody();

                        Log.d("pesanmasuk", msg_from + " " + msgBody);

                        if (msg_from != null && msgBody!= null){
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                Intent intentservice = new Intent(context,SmsService.class);
                                intentservice.putExtra("parent_no", msg_from);
                                intentservice.putExtra("message", msgBody);
                                context.startForegroundService(intentservice);
                            } else {
                                Intent intentservice = new Intent(context,SmsService.class);
                                intentservice.putExtra("parent_no", msg_from);
                                intentservice.putExtra("message", msgBody);
                                context.startService(intentservice);
                            }
                        }

                        Toast.makeText(context, "From: " + msg_from + ", Body: " + msgBody, Toast.LENGTH_SHORT).show();
                    }

                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }
}
