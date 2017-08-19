package com.aws.bakero.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import static android.os.Build.*;

public class ExternalReceiver extends BroadcastReceiver {

    public void onReceive(final Context context, Intent intent) {
        final NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Bundle extras = intent.getExtras();
        String state = extras.getString("default");
        if (state.contains("EndpointArn")){
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences("myPrefs",
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("myIP","0");
        char c = state.charAt(state.length()-1);

        new AsyncTask(){
            protected Object doInBackground(final Object... params) {
                String strl = "aaa";
                try {
                    SharedPreferences prefs = context.getSharedPreferences("myPrefs",
                            Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    URL url = new URL("http://checkip.amazonaws.com");
                    BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
                    strl = br.readLine();
                    editor.putString("myIP",strl);
                    editor.commit();
                    br.close();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    strl="b";
                } catch (IOException e) {
                    e.printStackTrace();
                    strl="c";
                }finally {
                    strl="d";
                }
                return true;
            }
        }.execute(null, null, null);

        while (prefs.getString("myIP","0").equals("0"));

        final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context,AndroidMobilePushApp.class), Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL);
        final Notification notification = new NotificationCompat.Builder(context).setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(prefs.getString("myIP","0"))
                .setContentText("")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .getNotification();
        mNotificationManager.notify(R.string.notification_number, notification);

        AudioManager mgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if(c == '1'){
            editor.putInt("RingerMode",mgr.getRingerMode());
            editor.commit();
            if(VERSION.SDK_INT <= 22){
                mgr.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            }else{
                mgr.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
        }else if(c == '0'){
            mgr.setRingerMode(prefs.getInt("RingerMode",AudioManager.RINGER_MODE_NORMAL));
        }

        editor.putString("state",state);
        editor.commit();
        if(prefs.getBoolean("isOpen", false)){
            AndroidMobilePushApp.getIns().updateTheTextView(state);
        }
    }
}

