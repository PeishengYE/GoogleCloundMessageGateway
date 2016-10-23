package com.radioyps.gcm_test;
/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



        import android.app.AlarmManager;
        import android.app.PendingIntent;
        import android.content.BroadcastReceiver;
        import android.content.Context;
        import android.content.Intent;
        import android.content.IntentFilter;
        import android.content.SharedPreferences;
        import android.graphics.Bitmap;
        import android.os.Bundle;
        import android.os.SystemClock;
        import android.preference.PreferenceManager;
        import android.support.v4.content.LocalBroadcastManager;
        import android.support.v7.app.AppCompatActivity;
        import android.util.Log;
        import android.view.Menu;
        import android.view.MenuItem;
        import android.widget.ProgressBar;
        import android.widget.TextView;

        import com.google.android.gms.common.ConnectionResult;
        import com.google.android.gms.common.GoogleApiAvailability;
        import com.google.zxing.BarcodeFormat;
        import com.google.zxing.MultiFormatWriter;
        import com.google.zxing.WriterException;
        import com.google.zxing.common.BitMatrix;
        import com.journeyapps.barcodescanner.BarcodeEncoder;

public class MainActivity extends AppCompatActivity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";


    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private ProgressBar mRegistrationProgressBar;
    private TextView mInformationTextView;
    private TextView messageRec;
    private boolean isReceiverRegistered;
    private static long TIME_INTERVAL = 15*1000;
    private static long TIME_DELAY = 3*1000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRegistrationProgressBar = (ProgressBar) findViewById(R.id.registrationProgressBar);
        mInformationTextView = (TextView) findViewById(R.id.informationTextView);
        messageRec = (TextView)findViewById(R.id.messageRecevied);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.i(TAG, "onReceiver()>> ....");
                mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    mInformationTextView.setText(getString(R.string.gcm_send_message));
                } else {
                    mInformationTextView.setText(getString(R.string.token_error_message));
                }
                if(intent.hasExtra(QuickstartPreferences.MSG_UPDATE)){
                    String msg = intent.getStringExtra(QuickstartPreferences.MSG_UPDATE);
                    String orig = messageRec.getText().toString();
                    msg = orig + "\n==============================\n"+ msg;
                    messageRec.setText(msg);
                }else{

                }

            }
        };

        initDefaultSharePreference();

        // Registering BroadcastReceiver
        registerReceiver();

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
        }
        SetAlarm(this);


    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }


    private  void initDefaultSharePreference()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        prefs.edit().putBoolean(CommonConstants.PREF_IS_TOKEN_RECEVIED, false).apply();
        prefs.edit().putString(CommonConstants.PREF_SAVED_TOKEN, "empty").apply();

    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
        isReceiverRegistered = false;
        super.onPause();
    }

    private void registerReceiver(){
        if(!isReceiverRegistered) {
            IntentFilter msgIntentFilter =  new IntentFilter(QuickstartPreferences.BROADCAST_ACTION);
            msgIntentFilter.addCategory(Intent.CATEGORY_DEFAULT);
            LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
                    msgIntentFilter);
            isReceiverRegistered = true;
        }
    }
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CancelAlarm(this);
    }

    public void SetAlarm(Context context) {
        //Toast.makeText(context, R.string.updating_in_progress, Toast.LENGTH_LONG).show(); // For example
        Log.d(TAG, "Set alarm!");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intnt = new Intent(context, AlarmReceiver.class);
        PendingIntent pendngIntnt = PendingIntent.getBroadcast(context, 0, intnt, 0);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + TIME_DELAY, TIME_INTERVAL, pendngIntnt);
    }

    public void CancelAlarm(Context context) {
        Log.d(TAG, "Cancle alarm!");
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        String token = null;
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //startActivity(new Intent(this, WifiScanResult.class));
            Log.d(TAG, "setting pressed ");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            //prefs.edit().putBoolean("filter", true).apply();
//            prefs.edit().putBoolean(CommonConstants.PREF_IS_TOKEN_RECEVIED, true).apply();
//            prefs.edit().putString(CommonConstants.PREF_SAVED_TOKEN, token).apply();
            token = prefs.getString(CommonConstants.PREF_SAVED_TOKEN,null);
            if((token != null)){

                if(token.equalsIgnoreCase("empty")){
                  Log.d(TAG, "token empty, do nothing");
                }else
                     displayTokernOnScreen(token);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    private void displayTokernOnScreen(String token) {
        // Add custom implementation, as needed.
        Log.d(TAG, "displayTokernOnScreen()>> <" + token +">");
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(token, BarcodeFormat.QR_CODE,200,200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);
            Context context = getBaseContext();
            Intent intent = new Intent(context, QrActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("pic",bitmap);
            context.startActivity(intent);
        } catch (WriterException e) {
            e.printStackTrace();
        }


    }
}
