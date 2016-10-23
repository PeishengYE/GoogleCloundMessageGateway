package com.radioyps.gcm_test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by yep on 12/10/16.
 */
public class BootUpReceiver extends BroadcastReceiver {
    private static String LOG_TAG = "BootUpReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO Auto-generated method stub
        Log.d(LOG_TAG, "Device Booted up...");
        GCMGateWay.start("bootup", context);
    }
}