package com.radioyps.gcm_test;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.IOException;

/**
 * Created by yep on 23/10/16.
 */
public class Utility {

    private static final String TAG = Utility.class.getName();
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String[] TOPICS = {"global"};

    /* we should use share preference to check google play */
    public static boolean checkPlayServices(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
             return false;
        }
        return true;
    }


    public static void subscribeTopics(String token, Context context) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(context);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, "/topics/" + topic, null);
        }
    }

    public static  String startRegistration(Context context){
        String token =null;
        try{
            InstanceID instanceID = InstanceID.getInstance(context);
            token = instanceID.getToken(BuildConfig.MySenderID,
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);

            LogToFile.toFile("TAG", "GCM Registration Token: " + token);
            // Subscribe to topic channels



        } catch (Exception e) {
            Log.d(TAG, "Failed to complete token refresh", e);
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.

        }
        return token;
    }

    public static  void displayTokernOnScreen(String token, Context context) {
        // Add custom implementation, as needed.
        Log.d(TAG, "displayTokernOnScreen()>> <" + token + ">");
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(token, BarcodeFormat.QR_CODE, 200, 200);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);

            Intent intent = new Intent(context, QrActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("pic", bitmap);
            context.startActivity(intent);
        } catch (WriterException e) {
            e.printStackTrace();
        }


    }
}
