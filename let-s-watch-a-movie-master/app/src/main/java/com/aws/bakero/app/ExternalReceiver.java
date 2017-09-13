package com.aws.bakero.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import static android.os.Build.*;

public class ExternalReceiver extends BroadcastReceiver {
    public void onReceive(final Context context, Intent intent) {
        final NotificationManager mNotificationManager;
        Bundle extras;
        String state;
        SharedPreferences prefs;
        SharedPreferences.Editor editor;
        JSONObject obj;
        String IP;
        String MovieName;
        String MovieMode;
        int yeeLightSittingsChanged = 0;
        try {
            mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            extras = intent.getExtras();
            state = extras.getString("default");
            if (state.contains("EndpointArn")){
                return;
            }
            prefs = context.getSharedPreferences("myPrefs",
                    Context.MODE_PRIVATE);
            editor = prefs.edit();
            editor.putString("myIP","0");
            obj = new JSONObject(state);
            IP = prefs.getString("IP","132.68.60.198");
            MovieName =  prefs.getString("movie_name","****");
            MovieMode = prefs.getString("movie_mode","stopped");
        }catch (Exception e){
            return;
        }

        try{IP = obj.getString("IP");}catch (Exception e){}
        try{MovieName = obj.getString("movie_name");}catch (Exception e){}
        try{MovieMode = obj.getString("movie_mode");}catch (Exception e){}
        try{editor.putString("current_movie_time",obj.getString("current_movie_time"));
        }catch (Exception e){}
        try{editor.putString("movie_total_time",obj.getString("movie_total_time"));
        }catch (Exception e){}
        try{editor.putString("color",obj.getString("color"));}catch (Exception e){}
        try{editor.putInt("brightness",obj.getInt("brightness"));}catch (Exception e){}
        try{editor.putString("gmt_time",obj.getString("gmt_time"));}catch (Exception e){}
        try{yeeLightSittingsChanged = obj.getInt("yeeLight_sittings_changed");}catch (Exception e){}
        editor.commit();
        try {
            if(MovieMode.equals("stopped")){
                editor.putString("current_movie_time","0:0:0");
                editor.putInt("period",0);
            }else{
                String[] current_time = prefs.getString("current_movie_time","0:0:0").split(":");
                int period = Integer.parseInt(current_time[0])*3600 +
                        Integer.parseInt(current_time[1])*60 + Integer.parseInt(current_time[2]);
                editor.putInt("period",period);
            }
            editor.putString("IP",IP);
            editor.putString("movie_mode",MovieMode);
            editor.putString("movie_name",MovieName);
        } catch (Throwable t) {
            return;
        }
        editor.commit();

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


        String MyIP = prefs.getString("myIP","****");
        try{
            if ( (IP.equals(MyIP) || MovieMode.equals("stopped") || MovieMode.equals("playing"))
                    && yeeLightSittingsChanged == 0) {
                final PendingIntent pendingIntent;
                final Notification notification;
                pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, AndroidMobilePushApp.class), Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL);
                notification = new NotificationCompat.Builder(context).setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(MovieName + " is " + MovieMode)
                        .setContentText("")
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .getNotification();
                mNotificationManager.notify(R.string.notification_number, notification);
            }
            if ( IP.equals(MyIP)){
                AudioManager mgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if(!MovieMode.equals("playing")){
                    mgr.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                }else{
                    if(VERSION.SDK_INT <= 22){
                        mgr.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                    }else{
                        mgr.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    }
                }
            }
            if(prefs.getBoolean("isOpen", false)){
                AndroidMobilePushApp.getIns().updateTheTextView();
            }
        }catch (Exception e){
            return;
        }
    }
}
