package com.radioyps.gcm_test;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * Created by yep on 10/10/16.
 * To test your GCM, you can use the following link to do the magic
 *
 * http://udacity.github.io/Advanced_Android_Development/
 */
public class GCMGateWay extends Service {


    private final static String TAG = "GCMGateWay";

    private final  int NOTIFICATION_ID = 10;
    private Notification mNotification = null;

    private volatile CommandHandler commandHandler;
    private static volatile GCMSendingHandler gcmSendingHandler;
    public static final String EXTRA_COMMAND = "Command";
    private static final String EXTRA_REASON = "Reason";
    public enum Command {start, mesgArrived, tokenChanged, imageArrived,GCMSendingFinished,stop}
    private static final int MSG_SERVICE_INTENT = 0;
    private static final int MSG_IMG_DATA_SENDING_START = 1;
    private static final int MSG_GCM_SENDING_FINISHED = 2;

    private volatile Looper commandLooper;
    private volatile Looper gcmSendingLooper;
    private boolean enabled = true;

    private State state = State.stop;
    private enum State {stop, started, stats}
    private static Context mContext = null;

    private static long TIME_INTERVAL = 15*1000;
    private static long TIME_DELAY = 3*1000;
    ServerSocket serverSocket=null;
    private  static  boolean isServerStarted = false;
    Socket mSocket = null;
    static final int socketServerPORT = 8099;
    private  byte [] imageRecevied = null;
    String base64ImageData = null;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {




        if (intent == null) {
            Log.i(TAG, "Restart");

            // Recreate intent
            intent = new Intent(this, GCMGateWay.class);
            intent.putExtra(EXTRA_COMMAND, enabled ? Command.start : Command.stop);
        }


        commandHandler.queue(intent);
        return START_STICKY;


    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getBaseContext();
        if(!Utility.checkPlayServices(mContext)){
            /* Fixme pupup a warning dialog */
            Log.d(TAG, "onCreate()>> the device not support Google Play, Please install it");
            return;
        }
        HandlerThread commandThread = new HandlerThread(getString(R.string.app_name) + " command");
        commandThread.start();
        commandLooper = commandThread.getLooper();
        commandHandler = new CommandHandler(commandLooper);


        HandlerThread GCMSendingHandlerThread = new HandlerThread(getString(R.string.app_name) + " GCMsending");
        GCMSendingHandlerThread.start();
        gcmSendingLooper = GCMSendingHandlerThread.getLooper();
        gcmSendingHandler = new GCMSendingHandler(gcmSendingLooper);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroy");

        commandLooper.quit();
        gcmSendingLooper.quit();
        CancelAlarm(mContext);
        state = State.stop;
        super.onDestroy();
    }

    private final class GCMSendingHandler extends Handler {

        private  int currentBegin, currentEnd,totalLength;
        private  int indexPacket;
        private  boolean isAllSendOut = true;
        private  boolean isNeedSendFinishFlag = false;

        public GCMSendingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                switch (msg.what) {
                    case MSG_IMG_DATA_SENDING_START:
                        initSending();
                        sendBase64Data();
                        break;
                    case MSG_GCM_SENDING_FINISHED:
                        if(!isAllSendOut)
                            sendBase64Data();
                        else if(isNeedSendFinishFlag)
                            sendEndPacketMesg();
                        break;
                    default:
                        Log.e(TAG, "GCMSendingHandler()>> unknown cmd" + msg.what);
                }
            } catch (Throwable ex) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            } finally {

            }
        }

        private void initSending(){
//            base64ImageData = "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
            totalLength = base64ImageData.length();
            currentBegin = 0;
            indexPacket = 0;
            isAllSendOut = false;

        }

        private void sendBase64Data(){
            currentEnd = currentBegin + CommonConstants.GCM_MAX_DATA_LENGTH;
            if(currentEnd > totalLength){
                currentEnd = totalLength;
                isAllSendOut = true;
                isNeedSendFinishFlag = true;
            }


            String toSendData = base64ImageData.substring(currentBegin, currentEnd);
            String flag = CommonConstants.GCM_SENDING_FLAG + indexPacket;
            sendGCMMessageOnDataPayload(flag, toSendData);
            currentBegin = currentEnd;
            indexPacket ++;
        }
        private  void sendEndPacketMesg(){
            sendGCMMessageOnDataPayload(CommonConstants.GCM_SENDING_END_FLAG, CommonConstants.GCM_SENDING_END_MESG);
            isNeedSendFinishFlag = false;
        }
    }




    private final class CommandHandler extends Handler {
        public int queue = 0;

        public CommandHandler(Looper looper) {
            super(looper);
        }

        public void queue(Intent intent) {
            synchronized (this) {
                queue++;
            }
            Message msg = commandHandler.obtainMessage();
            msg.obj = intent;
            msg.what = MSG_SERVICE_INTENT;
            commandHandler.sendMessage(msg);
        }

        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case MSG_SERVICE_INTENT:
                        handleIntent((Intent) msg.obj);
                        break;
                    default:
                        Log.e(TAG, "Unknown command message=" + msg.what);
                }
            } catch (Throwable ex) {
                Log.e(TAG, ex.toString() + "\n" + Log.getStackTraceString(ex));
            } finally {

            }
        }


        private void handleIntent(Intent intent) {


            Command cmd = (Command) intent.getSerializableExtra(EXTRA_COMMAND);
            String reason = intent.getStringExtra(EXTRA_REASON);
            try {
                switch (cmd) {
                    case start:
                         start();
                        break;

                    case mesgArrived:
                        mesgRecived();
                        break;

                    case tokenChanged:
                        gcmTokenChanged();
                        break;
                    case imageArrived:
                        sendImage();
                        break;
                    default:
                        Log.e(TAG, "Unknown command=" + cmd);
                }
            }catch (Throwable ex){
                Log.e(TAG, "handleIntent()>> exception on: " + cmd);
            }
        }

        private void start(){
            Log.e(TAG, "CommandHandler()>> start()");

            if(state == state.started){
                Log.i(TAG, "CommandHandler()>> start() already started, give up");
                if(Utility.isLocalTokenRecevied(mContext)){
                    Utility.updateUIMessage("token recevied", mContext);
                }
                return;
            }
            initDefaultSharePreference();
            startScocketListener();
            /* */
            String token = Utility.startRegistration(getBaseContext());
            if(token == null){
                return;
            }

            try {
                Utility.saveLocalToken(token, getBaseContext());
                Utility.subscribeTopics(token, getBaseContext());
            }catch (Exception e){
                e.printStackTrace();
                Log.i(TAG, "CommandHandler()>> start() failed on subscibe topic ");
                return;
            }
            Utility.updateUIMessage("token recevied", mContext);
            SetAlarm(getBaseContext());
            setUpAsForeground("GCMGateWay");
            state = State.started;
        }

        private  void sendImage(){
            Log.i(TAG, "CommandHandler()>> sendImage() prepare to send image" );
            if(imageRecevied != null){
                base64ImageData =  Base64.encodeToString(imageRecevied,Base64.DEFAULT);
                Log.i(TAG, "base64 total length: " + base64ImageData.length());
                onStartImageDataSending();
            }


        }

        private void mesgRecived(){
            Log.i(TAG, "CommandHandler()>> mesgRecived()" );
            String timeForSending = "Not avaiable";
            Long sendTimeLong;
            Long currentTime;
            Long timePassed = 100L;
            try{
            Bundle data = MyGcmListenerService.dataMessage;


            String sendTime = data.getString(CommonConstants.GCM_SENDING_TIME_KEY);
            if(sendTime != null){
                sendTimeLong= Long.parseLong(sendTime);
                currentTime = System.currentTimeMillis();

                timePassed = (currentTime - sendTimeLong)/1000;
                timeForSending = "Time elapsed on sending: " + timePassed + "seconds";
            }

                String remoteToken = data.getString(CommonConstants.GCM_SENDING_TOKEN_KEY);
                if(remoteToken != null){
                    LogToFile.toFile(TAG,"recevied token: " + remoteToken);
                    Utility.saveRemoteToken(remoteToken,getBaseContext());
                    sendGCMMessageOnMessagePayload(CommonConstants.REMOTE_CONFIRM_MESG);
                }else{
                    LogToFile.toFile(TAG,"remote token NOT found ");
                }

                String message = data.getString("message");
            if(GCMGateWay.isAuthorized(message) && (timePassed < 20)){
                sendControlCmdd(BuildConfig.DoorOpenCmdUsedByLocalNetwork);
                LogToFile.toFile(TAG, "sending open door cmd");
                LogToFile.toFile(TAG,timeForSending);
            }else{
                if(timePassed < 20)
                    LogToFile.toFile(TAG,"no Authorized message recevied, abort. message: " + message);
                else
                    LogToFile.toFile(TAG,"aborted pending message: " + message);
            }
            }catch (Exception e){
                e.printStackTrace();
                LogToFile.toFile(TAG, "Exception on receving message : " + e.toString());
            }

        }

        private void gcmTokenChanged(){
            Log.e(TAG, "CommandHandler()>> gcmTokenChanged()" );
            sendNotification("Emergency: token changed");
            /* FIXME need ask user to rescan the QR code on Screen  */
            String token = Utility.startRegistration(getBaseContext());
            LogToFile.toFile(TAG, "gcmTokenChanged()>> Attention Token changed ");
            if(token == null){
                return;
            }

            try {
                Utility.saveLocalToken(token, getBaseContext());
                Utility.subscribeTopics(token, getBaseContext());
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(TAG, "CommandHandler()>> start() failed on subscibe topic ");
                return;
            }
        }
    }

	private void startScocketListener(){
        if(!isServerStarted) {

            Thread initThread = new Thread(new initSocketConnection());
            initThread.start();
        }
    }


   private class initSocketConnection extends Thread {


            public void run() {
                try {
                    isServerStarted = true;
                    serverSocket = new ServerSocket(socketServerPORT);
                    Log.d(TAG, "initSocketConnection()>> Socket Started");
                    while (true) {

                        mSocket = serverSocket.accept();
                        Log.d(TAG, "initSocketConnection()>> connected, making new thread");
                        Thread socketServerThread = new Thread(new SocketServerThread());
                        socketServerThread.start();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                isServerStarted = false;
            }
    }



        public class SocketServerThread extends Thread {
            InputStream isSocket =null;
            Socket workingSocket = mSocket;

            @Override
            public void run() {
                String connectionInfo = "";
                connectionInfo +=  " from "
                        + workingSocket.getInetAddress() + ":"
                        + workingSocket.getPort() + "\n";
                Log.i(TAG, "SocketServerThread()>>  current: " + connectionInfo);

                try {
                    File fileOutPut = new File(Environment.getExternalStorageDirectory(), CommonConstants.TEMP_IMAG_FILENAME);
                    OutputStream osFile = new FileOutputStream(fileOutPut);

                    isSocket = workingSocket.getInputStream();
                    clearImageRecived();
                    byte[] buffer = new byte[CommonConstants.TMP_MAXIMUM_OUTPUT_LENGTH];
                    int count;
                        try{
                            while ((count = isSocket.read(buffer)) > 0) {
                                osFile.write(buffer,0,count);
                                appendImageReceived(Arrays.copyOf(buffer, count));
                                Log.i(TAG, "SocketServerThread()>>  count: " + count);
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                     osFile.close();
                     isSocket.close();
                     Log.i(TAG, "imageRecevied: byte length: " + imageRecevied.length);
                    onReceiveImage("Motion detected",mContext);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } finally {
                    closeSocket(workingSocket);
                    Log.i(TAG, "SocketServerThread()>>  thread finished ");
                }
            }

        }

    private void closeSocket(Socket socketInput){
        if (socketInput != null) {
            try {
                socketInput.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

   private  void clearImageRecived(){
       imageRecevied = null;
   }

    private  void appendImageReceived(byte[] data){
        if(imageRecevied == null){
            imageRecevied = data;
        }else {
            imageRecevied = concatenateByteArrays(imageRecevied, data);
        }
    }

    private  byte[] concatenateByteArrays(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
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

    public static void start(String reason, Context context) {
        Intent intent = new Intent(context, GCMGateWay.class);
        intent.putExtra(EXTRA_COMMAND, Command.start);
        intent.putExtra(EXTRA_REASON, reason);
        context.startService(intent);
    }

    private  void initDefaultSharePreference()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        prefs.edit().putBoolean(CommonConstants.PREF_IS_LOCAL_TOKEN_RECEVIED, false).apply();
        prefs.edit().putString(CommonConstants.PREF_LOCAL_TOKEN_SAVING_KEY, "empty").apply();

    }


    public static void onReceiveGCm(String reason, Context context) {
        Intent intent = new Intent(context, GCMGateWay.class);
        intent.putExtra(EXTRA_COMMAND, Command.mesgArrived);
        intent.putExtra(EXTRA_REASON, reason);
        context.startService(intent);
    }

    public static void onReceiveImage(String reason, Context context) {
        Intent intent = new Intent(context, GCMGateWay.class);
        intent.putExtra(EXTRA_COMMAND, Command.imageArrived);
        intent.putExtra(EXTRA_REASON, reason);
        context.startService(intent);
    }
    public static void onGoolgeTokenChanged(String reason, Context context) {
        Intent intent = new Intent(context, GCMGateWay.class);
        intent.putExtra(EXTRA_COMMAND, Command.tokenChanged);
        intent.putExtra(EXTRA_REASON, reason);
        context.startService(intent);
    }

    public static void onGCMSendingFinished() {
        Message msg = gcmSendingHandler.obtainMessage();
        msg.what = MSG_GCM_SENDING_FINISHED;
        gcmSendingHandler.sendMessage(msg);
    }

    public static void onStartImageDataSending() {
        Message msg = gcmSendingHandler.obtainMessage();
        msg.what = MSG_IMG_DATA_SENDING_START;
        gcmSendingHandler.sendMessage(msg);
    }

    void setUpAsForeground(String text) {

        mNotification = makeNotification(text);
        startForeground(NOTIFICATION_ID, mNotification);
        Log.d(TAG, "onHandleIntent()>> setUpAsForeground()");
    }
    /* FIXME duplicated code */
    private void sendNotification(String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("GCM Changed")
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build());
    }


    private Notification  makeNotification(String message) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.notification_icon_name))
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(13, notificationBuilder.build());/* ID of notification */
        return notificationBuilder.build();
    }

     public   static boolean isAuthorized(String mesg){
        boolean ret = false;

        if((mesg != null)&&(mesg.equals(BuildConfig.MyDoorConfirmKeyFromGCM))){
            ret = true;
            Log.d(TAG, "isAuthorized()>> GCM message authorized" );
        }else{
            Log.d(TAG, "isAuthorized()>> GCM message NOT authorized" );
        }
        return ret;
    }

    /*
     FIXME: according the Android performance course from udacity.
    * all the Async task use a single thread
    * they may be blocked each other
    * */
    public void sendControlCmdd(String inputCmd) {
        ControlCmdSending sendingTask = new ControlCmdSending();
        String [] cmd = new String[] {inputCmd, ""};
        sendingTask.execute(cmd);

    }

     public static String getRemoteToken(){
         return Utility.getRemoteToken(mContext);
     }


    private void sendGCMMessageOnMessagePayload(String message){
        GcmSendTask gcmTask = new GcmSendTask();
        String [] cmd = new String[] {"message", message, ""};
        gcmTask.execute(cmd);
    }

    private void sendGCMMessageOnDataPayload(String flag, String data){
        GcmSendTask gcmTask = new GcmSendTask();
        String [] cmd = new String[] {flag, data, ""};
        gcmTask.execute(cmd);
    }

    public static String getipAddressFromPref(){
       String ipAddressFromPref = Utility.getPreferredIPAdd(mContext);
        return ipAddressFromPref;
    }

    public  static  int getipPortFromPref(){
        int  ipPortFromPref = Utility.getPreferredIPPort(mContext);
        return ipPortFromPref;
    }

}
