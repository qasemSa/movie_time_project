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
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import javax.xml.datatype.Duration;

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
        int yeeLightSittingsChanged = 0;
        JSONObject obj;
        String IP;
        String MovieName;
        String MovieMode;
        try {
            obj = new JSONObject(state);
            yeeLightSittingsChanged = obj.getInt("yeeLight_sittings_changed");
            if(yeeLightSittingsChanged == 0){
                IP = obj.getString("IP");
                MovieName = obj.getString("movie_name");
                MovieMode = obj.getString("movie_mode");
                if(MovieMode.equals("stopped")){
                    editor.putString("current_movie_time","0:0:0");
                    editor.putInt("period",0);
                    editor.putInt("timer",0);
                }else{
                    String[] current_time = prefs.getString("current_movie_time","0:0:0").split(":");
                    int period = Integer.parseInt(current_time[0])*3600 +
                            Integer.parseInt(current_time[1])*60 + Integer.parseInt(current_time[2]);
                    editor.putInt("period",period);
                    editor.putString("current_movie_time",obj.getString("current_movie_time"));
                }
                editor.putString("IP",IP);
                editor.putString("prev_movie_mode",prefs.getString("movie_mode","stopped"));
                editor.putString("movie_mode",MovieMode);
                editor.putString("movie_name",MovieName);
                editor.putString("movie_total_time",obj.getString("movie_total_time"));
                editor.putString("color",obj.getString("color"));
                editor.putInt("brightness",obj.getInt("brightness"));
                editor.putString("gmt_time",obj.getString("gmt_time"));
                editor.commit();
            }else{
                IP = obj.getString("IP");
                MovieName = prefs.getString("movie_name","****");
                MovieMode = prefs.getString("movie_mode","****");
                editor.putString("IP",IP);
                editor.putString("color",obj.getString("color"));
                editor.putInt("brightness",obj.getInt("brightness"));
                editor.commit();
            }
        } catch (Throwable t) {
            return;
        }

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


        if ( (IP.equals(MyIP) || MovieMode.equals("stopped") || MovieMode.equals("playing"))
                && yeeLightSittingsChanged == 0){
            final PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context,AndroidMobilePushApp.class), Notification.DEFAULT_LIGHTS | Notification.FLAG_AUTO_CANCEL);
            final Notification notification = new NotificationCompat.Builder(context).setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(MovieName+" is " + MovieMode)
                    .setContentText("")
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .getNotification();
            mNotificationManager.notify(R.string.notification_number, notification);
        }
        if ( IP.equals(MyIP)){
            AudioManager mgr = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if(MovieMode.equals("stopped")){
                mgr.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            }else{
                if(VERSION.SDK_INT <= 22){
                    mgr.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
                }else{
                    mgr.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                }
            }
        }
        if(prefs.getString("IP","**").equals(prefs.getString("myIP","0"))) {
            AndroidMobilePushApp.getIns().EnableButtons();
        }else{
            AndroidMobilePushApp.getIns().DisableButtons();
        }
        if(prefs.getBoolean("isOpen", false) ){
            AndroidMobilePushApp.getIns().updateTheTextView();
        }
    }
}
