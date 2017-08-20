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
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreatePlatformEndpointRequest;
import com.amazonaws.services.sns.model.CreatePlatformEndpointResult;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
// Requires Android 2.2 or higher, Google Play Services on the target device, and an active google account on the device.

public class AndroidMobilePushApp extends Activity {
    private TextView tView;
    private SharedPreferences prefs ;
    private SharedPreferences.Editor editor;
    private static AndroidMobilePushApp ins;

    public static AndroidMobilePushApp getIns(){
        return ins;
    }
    public void updateTheTextView(final String t) {
        AndroidMobilePushApp.this.runOnUiThread(new Runnable() {
            public void run() {
                TextView textV1 = (TextView) findViewById(R.id.tViewId);
                textV1.setText(t);
            }
        });
    }



    private void registerTopic(String token){
        AWSCredentials awsCredentials = new BasicAWSCredentials("AKIAIEMBOEMXDY6V5LDA",
                "EkppIWBRfZSCD0KM4cSB+sfhj+HtVcZ2dUDZ6FwS");
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("myPrefs", Context.MODE_PRIVATE);
        editor = prefs.edit();
        editor.putBoolean("isOpen", true);
        editor.commit();

        setContentView(R.layout.activity_main);
        tView = (TextView) findViewById(R.id.tViewId);
        tView.setMovementMethod(new ScrollingMovementMethod());

        registerPhone();
        ins = this;
        String str = prefs.getString("state","error");
        tView.setText(str);
        final ImageButton imgBtn = (ImageButton)findViewById(R.id.imageButton);
        imgBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LinearLayout aaa = (LinearLayout)findViewById(R.id.choose_color);
                float deg = imgBtn.getRotation();
                if(aaa.getVisibility() == View.VISIBLE){
                    deg += 90F;
                    aaa.setVisibility(View.INVISIBLE);
                }else{
                    deg -= 90F;
                    aaa.setVisibility(View.VISIBLE);
                }
                imgBtn.animate().rotation(deg).setInterpolator(new AccelerateDecelerateInterpolator());
            }
        });

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
