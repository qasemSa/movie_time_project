/*
 * Copyright 2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.aws.bakero.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.iot.client.AWSIotException;
import com.amazonaws.services.iot.client.AWSIotMqttClient;
import com.amazonaws.services.iot.client.AWSIotTopic;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.google.android.gms.gcm.GoogleCloudMessaging;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

// Requires Android 2.2 or higher, Google Play Services on the target device, and an active google account on the device.

public class AndroidMobilePushApp extends Activity {
    private SharedPreferences prefs ;
    private SharedPreferences.Editor editor;
    private static AndroidMobilePushApp ins;
    private int MovieTimeInSeconds;
    private ProgressBar mProgressBar;
    Map<String, Integer> SittingsMap = new HashMap<String, Integer>();
    static private TextView periodTextView;
    public static AndroidMobilePushApp getIns(){
        return ins;
    }
    public void updateTheTextView() {
        AndroidMobilePushApp.this.runOnUiThread(new Runnable() {
            public void run() {
                initialization();
            }
        });
    }
    private void initialization(){
        if(prefs.getString("movie_mode","****").equals("stopped")){
            RadioGroup colorRG = (RadioGroup) findViewById(R.id.colorRG);
            RadioGroup brightnessRG = (RadioGroup) findViewById(R.id.brightnessRG);
            LinearLayout MovieDetails = (LinearLayout) findViewById(R.id.movie_details);
            for (int i = 1; i < colorRG.getChildCount(); i++) {
                colorRG.getChildAt(i).setEnabled(false);
                brightnessRG.getChildAt(i).setEnabled(false);
            }
            String[] DateString = prefs.getString("gmt_time","0:0:0").split(":");
            ProgressBar PBar = (ProgressBar) findViewById(R.id.progressBar2);
            PBar.setVisibility(View.INVISIBLE);
            TextView TBar = (TextView) findViewById(R.id.textView2);
            TBar.setVisibility(View.INVISIBLE);
            TextView TitleBar =(TextView) findViewById(R.id.textView);
            TitleBar.setVisibility(View.INVISIBLE);
            TextView MovieModeText=(TextView) findViewById(R.id.movie_mode);
            TextView MovieNameText=(TextView) findViewById(R.id.movie_name);
            TextView MovieTotalTime=(TextView) findViewById(R.id.movie_total_time);
            MovieModeText.setText("movie name: " + prefs.getInt("period",0));
            MovieNameText.setText("movie mode: ");
            MovieTotalTime.setText("movie total time: ");
        }else{
            ProgressBar PBar = (ProgressBar) findViewById(R.id.progressBar2);
            PBar.setVisibility(View.VISIBLE);
            TextView TBar = (TextView) findViewById(R.id.textView2);
            TBar.setVisibility(View.VISIBLE);
            TextView TitleBar =(TextView) findViewById(R.id.textView);
            TitleBar.setVisibility(View.VISIBLE);
            RadioGroup colorRG = (RadioGroup) findViewById(R.id.colorRG);
            RadioGroup brightnessRG = (RadioGroup) findViewById(R.id.brightnessRG);
            LinearLayout MovieDetails = (LinearLayout) findViewById(R.id.movie_details);
            TextView MovieModeText=(TextView) findViewById(R.id.movie_mode);
            TextView MovieNameText=(TextView) findViewById(R.id.movie_name);
            TextView MovieTotalTime=(TextView) findViewById(R.id.movie_total_time);
            if(prefs.getString("IP","****").equals(prefs.getString("myIP","0"))){
                for (int i = 1; i < colorRG.getChildCount(); i++) {
                    colorRG.getChildAt(i).setEnabled(true);
                    brightnessRG.getChildAt(i).setEnabled(true);
                }
            }else {
                for (int i = 1; i < colorRG.getChildCount(); i++) {
                    colorRG.getChildAt(i).setEnabled(false);
                    brightnessRG.getChildAt(i).setEnabled(false);
                }
            }
            MovieDetails.setVisibility(View.VISIBLE);

            MovieModeText.setText("movie mode: "+ prefs.getString("movie_mode","****"));
            MovieNameText.setText("movie name: "+ prefs.getString("movie_name","****"));
            MovieTotalTime.setText("movie total time: "+
                    prefs.getString("movie_total_time","****"));
            colorRG.check(SittingsMap.get(prefs.getString("color","White")));
            brightnessRG.check(SittingsMap.get(Integer.toString(prefs.getInt("brightness",0))));
            String[] DateString = prefs.getString("gmt_time","0:0:0").split(":");
            Date d1 = new Date(0,0,0,Integer.parseInt(DateString[0]),Integer.parseInt(DateString[1])
                    ,Integer.parseInt(DateString[2]));
            Date d2 = new Date(0,0,0,0,0,0);
        }
        MovieTimeInSeconds = between_date("0:0:0:" + prefs.getString("movie_total_time","0:0:0")
                ,"0:0:0:0:0:0");
        String movie_mode = prefs.getString("movie_mode","****");
        final Button PlayButton = (Button)findViewById(R.id.buttonPlayPause);
        final Button StopButton = (Button)findViewById(R.id.buttonStop);
        final Button StartButton = (Button)findViewById(R.id.buttonStart);
        if(movie_mode.equals("playing")){
            PlayButton.setBackgroundColor(0xffffbb33);
            PlayButton.setText("Pause");
            StopButton.setEnabled(true);
            StartButton.setEnabled(false);
            PlayButton.setEnabled(true);
        }else if (movie_mode.equals("paused")){
            PlayButton.setBackgroundColor(0xff99cc00);
            PlayButton.setText("Play");
            StopButton.setEnabled(true);
            StartButton.setEnabled(false);
            PlayButton.setEnabled(true);
        }else if(movie_mode.equals("stopped")){
            StopButton.setEnabled(false);
            StartButton.setEnabled(true);
            PlayButton.setEnabled(false);
        }
        new Thread(new Runnable() {
            public void run() {
                do  {//!prefs.getString("movie_mode","****").equals("stopped")
                    int x = doSomeWork();
                    mProgressBar.setProgress(x);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            periodTextView.setText(Integer.toString(
                                    mProgressBar.getProgress()*100/mProgressBar.getMax())+"%");
                        }
                    });

                } while((mProgressBar.getProgress() != mProgressBar.getMax()) &&
                        prefs.getString("movie_mode","****").equals("playing") );

            }
            private int doSomeWork() {
                int x=0;
                try {
                    // ---simulate doing some work---
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                    Date currentLocalTime = cal.getTime();
                    DateFormat date = new SimpleDateFormat("yyy:MM:dd:HH:mm:ss");
                    date.setTimeZone(TimeZone.getTimeZone("GMT"));
                    String localTime = date.format(currentLocalTime);
                    x = prefs.getInt("period", 0) + between_date(localTime,
                            prefs.getString("gmt_time", "0:0:0:0:0:0"));
                    if (x >= MovieTimeInSeconds) {
                        x = mProgressBar.getMax();
                    } else {
                        x = (mProgressBar.getMax() * x) / MovieTimeInSeconds;
                    }
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return x;
            }
        }).start();
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
    }


    private void registerTopic(String token){
        AWSCredentials awsCredentials = new BasicAWSCredentials("AKIAIPCCPQSVC6D7B4GQ",
                "Iwmf8g++aLQ2EGpB8De53/GfYIEMRGtR4y/9wukO");
        String platformApplicationArn = "arn:aws:sns:us-east-1:465056667065:app/GCM/movie-time";
        AmazonSNSClient pushClient = new AmazonSNSClient(awsCredentials);

        String customPushData = "my custom data";
        pushClient.setRegion(Region.getRegion(Regions.US_EAST_1));
        CreatePlatformEndpointRequest platformEndpointRequest = new CreatePlatformEndpointRequest();
        platformEndpointRequest.setCustomUserData(customPushData);
        platformEndpointRequest.setToken(token);
        platformEndpointRequest.setPlatformApplicationArn(platformApplicationArn);
        CreatePlatformEndpointResult result = pushClient.createPlatformEndpoint(platformEndpointRequest);
        pushClient.subscribe("arn:aws:sns:us-east-1:465056667065:first-topic","Application",result.getEndpointArn());
        Log.i( "result" ,result.getEndpointArn());
    }

    private void register(final GoogleCloudMessaging gcm) {
        new AsyncTask(){
            protected Object doInBackground(final Object... params) {
                String token;
                try {
                    token = gcm.register(getString(R.string.project_number));
                    Log.i("registrationId", token);
                    registerTopic(token);
                    editor.putBoolean(getString(R.string.first_launch), false);
                    editor.commit();
                }
                catch (IOException e) {
                    Log.i("Registration Error", e.getMessage());
                }
                return true;
            }
        }.execute(null, null, null);
    }

    private void registerPhone(){
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(getBaseContext());
        if(prefs.getBoolean(getString(R.string.first_launch), true)){
            register(gcm);
        }
    }

    private void send_status_to_pi(final String color,final Integer brightness,final String movie_mode,final String change){
        new AsyncTask(){
            protected Object doInBackground(final Object ... params) {
                String clientEndpoint = "a18cbjggcfl7xb.iot.us-east-1.amazonaws.com";       // replace <prefix> and <region> with your own
                String clientId = "qasem";                              // replace with your own client ID. Use unique client IDs for concurrent connections.
                AWSIotMqttClient client = new AWSIotMqttClient(clientEndpoint, clientId,"AKIAIPCCPQSVC6D7B4GQ",
                        "Iwmf8g++aLQ2EGpB8De53/GfYIEMRGtR4y/9wukO");
                JSONObject msg = new JSONObject();
                JSONObject state = new JSONObject();
                JSONObject reported = new JSONObject();
                try {
                    reported.put("color",color);
                    reported.put("brightness",brightness);
                    reported.put("movie_mode",movie_mode);
                    state.put("reported",reported);
                    state.put("change",change);
                    msg.put("state",state);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    client.connect();
                    AWSIotTopic topic = new AWSIotTopic("$aws/things/RasPi3/shadow/update");
                    client.publish("$aws/things/RasPi3/shadow/update", msg.toString());
                    client.disconnect();
                } catch (AWSIotException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }.execute();
    }

    private int between_date(String date1,String date2){
        String[] newDateString = date1.split(":");
        String[] oldDateString = date2.split(":");
        int yearNew = Integer.parseInt(newDateString[0]);
        int yearOld = Integer.parseInt(oldDateString[0]);
        int monthNew = Integer.parseInt(newDateString[1]);
        int monthOld = Integer.parseInt(oldDateString[1]);
        int dayNew = Integer.parseInt(newDateString[2]);
        int dayOld = Integer.parseInt(oldDateString[2]);
        int hourNew = Integer.parseInt(newDateString[3]);
        int hourOld = Integer.parseInt(oldDateString[3]);
        int minNew = Integer.parseInt(newDateString[4]);
        int minOld = Integer.parseInt(oldDateString[4]);
        int secNew = Integer.parseInt(newDateString[5]);
        int secOld = Integer.parseInt(oldDateString[5]);
        if(yearNew>yearOld || monthNew>monthOld || dayNew>dayOld){
            return hourNew*3600 + minNew*60 + secNew + (23-hourOld)*3600 + (60-minOld)*60 +(60-secOld);
        }
        return (hourNew - hourOld)*3600 + (minNew - minOld)*60 + (secNew-secOld);
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SittingsMap.put("0", R.id.RB0);
        SittingsMap.put("1", R.id.RB1);
        SittingsMap.put("2", R.id.RB2);
        SittingsMap.put("3", R.id.RB3);
        SittingsMap.put("4", R.id.RB4);
        SittingsMap.put("5", R.id.RB5);
        SittingsMap.put("6", R.id.RB6);
        SittingsMap.put("7", R.id.RB7);
        SittingsMap.put("8", R.id.RB8);
        SittingsMap.put("9", R.id.RB9);
        SittingsMap.put("10", R.id.RB10);
        SittingsMap.put("Blue", R.id.BlueRB);
        SittingsMap.put("Red", R.id.RedRB);
        SittingsMap.put("Brown", R.id.BrownRB);
        SittingsMap.put("Cyan", R.id.CyanRB);
        SittingsMap.put("Green", R.id.GreenRB);
        SittingsMap.put("Orange", R.id.OrangeRB);
        SittingsMap.put("Pink", R.id.PinkRB);
        SittingsMap.put("Purple", R.id.PurpleRB);
        SittingsMap.put("Silver", R.id.SilverRB);
        SittingsMap.put("White", R.id.WhiteRB);
        SittingsMap.put("Yellow", R.id.YellowRB);
        prefs = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        editor = prefs.edit();
        setContentView(R.layout.activity_main);

        registerPhone();
        ins = this;


        final ImageButton imgBtn = (ImageButton) findViewById(R.id.imageButton);
        imgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinearLayout choose_color_layout = (LinearLayout) findViewById(R.id.choose_color);
                float deg = imgBtn.getRotation();
                if (choose_color_layout.getVisibility() == View.VISIBLE) {
                    deg += 90F;
                    choose_color_layout.animate().alpha(0.5f);
                    choose_color_layout.setVisibility(View.INVISIBLE);

                    RadioGroup colorRG = (RadioGroup) findViewById(R.id.colorRG);
                    RadioGroup brightnessRG = (RadioGroup) findViewById(R.id.brightnessRG);
                    int colorButtonID = colorRG.getCheckedRadioButtonId();
                    int brightnessButtonID = brightnessRG.getCheckedRadioButtonId();
                    RadioButton colorButton = (RadioButton) findViewById(colorButtonID);
                    RadioButton brightnessButton = (RadioButton) findViewById(brightnessButtonID);

                    String color = ((String) colorButton.getText());
                    Integer brightness = Integer.valueOf((String) (brightnessButton.getText()));
                    if (colorRG.getChildAt(1).isEnabled()) {
                        send_status_to_pi(color, brightness, "", "yeeLight");
                        Toast.makeText(getApplicationContext(), "color and brightness of YeeLight updated", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    deg -= 90F;
                    choose_color_layout.animate().alpha(1.0f);
                    choose_color_layout.setVisibility(View.VISIBLE);

                }
                imgBtn.animate().rotation(deg).setInterpolator(new AccelerateDecelerateInterpolator());
            }
        });
        initialization();
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar2);
        mProgressBar.setScaleY(4f);
        mProgressBar.setMax(200);
        periodTextView =(TextView) findViewById(R.id.textView2);
        final Button PlayButton = (Button) findViewById(R.id.buttonPlayPause);
        final Button StartButton = (Button) findViewById(R.id.buttonStart);
        final Button StopButton = (Button) findViewById(R.id.buttonStop);
        PlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                if (PlayButton.getText().equals("Play")) {
                    send_status_to_pi("", -1, "play", "movie");
                } else {
                    send_status_to_pi("", -1, "pause", "movie");
                }
                PlayButton.setEnabled(false);
                StartButton.setEnabled(false);
                StopButton.setEnabled(false);
            }
        });
        StartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                send_status_to_pi("", -1, "start", "movie");
                PlayButton.setEnabled(false);
                StartButton.setEnabled(false);
                StopButton.setEnabled(false);
            }
        });
        StopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
                send_status_to_pi("", -1, "stop", "movie");
                PlayButton.setEnabled(false);
                StartButton.setEnabled(false);
                StopButton.setEnabled(false);
            }
        });
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        editor.putBoolean("isOpen", true);
        editor.commit();
    }
    public void onDestroy(){
        super.onDestroy();
    }

    public void onStop(){
        super.onStop();
        editor.putBoolean("isOpen", false);
        editor.commit();
    }

    public void onRestart(){
        super.onRestart();
    }

    public void onResume(){
        super.onResume();
    }

    public void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
    }

}
