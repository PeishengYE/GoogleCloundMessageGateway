package com.radioyps.gcm_test;

import android.app.AlarmManager;
import android.app.IntentService;
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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Created by yep on 10/10/16.
 * To test your GCM, you can use the following link to do the magic
 *
 * http://udacity.github.io/Advanced_Android_Development/
 */
public class GCMGateWay extends Service {


    private final static String TAG = "GCMGateWay";
    private final static int RESULT_SUCCESS = 0x11;
    private final static int RESULT_IO_ERROR = 0x12;
    private final static int RESULT_TIMEOUT = 0x13;

    private final static int RESULT_HOST_UNAVAILABLE = 0x14;
    private final static int RESULT_HOST_REFUSED = 0x15;
    private final static int RESULT_NETWORK_UNREACHABLE = 0x16;
    private final static int RESULT_HOSTNAME_NOT_FOUND = 0x17;
    private final static int RESULT_UNKNOWN = 0x18;

    private final static String EXCEPTION_NETWORK_UNREACHABLE = "ENETUNREACH";
    private final static String EXCEPTION_HOST_UNAVAILABLE = "EHOSTUNREACH";
    private final static String EXCEPTION_HOST_REFUSED = "ECONNREFUSED";
    private static int response = RESULT_UNKNOWN;
    private static boolean isContinueConnect = true;
    private final  int NOTIFICATION_ID = 10;
    private Notification mNotification = null;

    private volatile CommandHandler commandHandler;
    public static final String EXTRA_COMMAND = "Command";
    private static final String EXTRA_REASON = "Reason";
    public enum Command {start, mesgArrived, tokenChanged, stop}
    private static final int MSG_SERVICE_INTENT = 0;
    private volatile Looper commandLooper;
    private boolean enabled = true;

    private State state = State.stop;
    private enum State {stop, started, stats}
    private static Context mContext = null;

    private static long TIME_INTERVAL = 15*1000;
    private static long TIME_DELAY = 3*1000;
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
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Destroy");

        commandLooper.quit();
        CancelAlarm(mContext);
        state = State.stop;
        super.onDestroy();
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
                if(Utility.isTokenRecevied(mContext)){
                    Utility.updateUIMessage("token recevied", mContext);
                }
                return;
            }
            initDefaultSharePreference();
            /* */
            String token = Utility.startRegistration(getBaseContext());
            if(token == null){
                return;
            }

            try {
                saveToken(token);
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



        private void mesgRecived(){
            Log.i(TAG, "CommandHandler()>> mesgRecived()" );
            String timeForSending = "Not avaiable";
            Long sendTimeLong;
            Long currentTime;
            Long timePassed = 100L;
            try{
            Bundle data = MyGcmListenerService.dataMessage;

            String message = data.getString("message");
            String sendTime = data.getString("sendTime");
            if(sendTime != null){
                sendTimeLong= Long.parseLong(sendTime);
                currentTime = System.currentTimeMillis();

                timePassed = (currentTime - sendTimeLong)/1000;
                timeForSending = "Time elapsed on sending: " + timePassed + "seconds";
            }

            if(GCMGateWay.isAuthorized(message) && (timePassed < 20)){
                GCMGateWay.sendCmd(BuildConfig.DoorOpenCmdUsedByLocalNetwork);
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
                saveToken(token);
                Utility.subscribeTopics(token, getBaseContext());
            }catch (Exception e){
                e.printStackTrace();
                Log.i(TAG, "CommandHandler()>> start() failed on subscibe topic ");
                return;
            }
        }
    }

    private  void saveToken(String token) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.edit().putBoolean(CommonConstants.PREF_IS_TOKEN_RECEVIED, true).apply();
        prefs.edit().putString(CommonConstants.PREF_SAVED_TOKEN, token).apply();
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

        prefs.edit().putBoolean(CommonConstants.PREF_IS_TOKEN_RECEVIED, false).apply();
        prefs.edit().putString(CommonConstants.PREF_SAVED_TOKEN, "empty").apply();

    }


    public static void onReceiveGCm(String reason, Context context) {
        Intent intent = new Intent(context, GCMGateWay.class);
        intent.putExtra(EXTRA_COMMAND, Command.mesgArrived);
        intent.putExtra(EXTRA_REASON, reason);
        context.startService(intent);
    }

    public static void onGoolgeTokenChanged(String reason, Context context) {
        Intent intent = new Intent(context, GCMGateWay.class);
        intent.putExtra(EXTRA_COMMAND, Command.tokenChanged);
        intent.putExtra(EXTRA_REASON, reason);
        context.startService(intent);
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



    private static String getStatusString(int status, Context context) {
        String ret = context.getString(R.string.result_cmd_unknown);
        switch (status) {
            case RESULT_HOST_REFUSED:
                ret = context.getString(R.string.result_cmd_host_refused);
                break;
            case RESULT_HOST_UNAVAILABLE:
                ret = context.getString(R.string.result_cmd_host_unavailable);
                break;
            case RESULT_HOSTNAME_NOT_FOUND:
                ret = context.getString(R.string.result_cmd_hostname_not_found);
                break;
            case RESULT_IO_ERROR:
                ret = context.getString(R.string.result_cmd_io_error);
                break;
            case RESULT_NETWORK_UNREACHABLE:
                ret = context.getString(R.string.result_cmd_network_unreachable);
                break;
            case RESULT_TIMEOUT:
                ret = context.getString(R.string.result_cmd_timeout);
                break;
            case RESULT_SUCCESS:
                ret = context.getString(R.string.result_cmd_success);
                break;
            default:
                ret = context.getString(R.string.result_cmd_unknown);


        }
        return ret;

    }

    private static int getConnectionErrorCode(String error) {
        int ret = RESULT_UNKNOWN;

        if (error.indexOf(EXCEPTION_NETWORK_UNREACHABLE) != -1) {
            ret = RESULT_NETWORK_UNREACHABLE;
        } else if (error.indexOf(EXCEPTION_HOST_UNAVAILABLE) != -1) {
            ret = RESULT_HOST_UNAVAILABLE;
        } else if (error.indexOf(EXCEPTION_HOST_REFUSED) != -1) {
            ret = RESULT_HOST_REFUSED;
        }
        return ret;

    }

    private static String getConnectionError(ConnectException ex) {


        String ret = null;

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionStr = sw.toString();
        String lines[] = exceptionStr.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].indexOf("ConnectException") != -1) {
                ret = lines[i];
                break;
            }
        }


        Log.d(TAG, "getConnectionError()>> Line: " + ret);
        return ret;

    }

    public   static boolean isAuthorized(String mesg){
        boolean ret = false;
        if(mesg.equals(BuildConfig.MyDoorConfirmKeyFromGCM)){
            ret = true;
            Log.d(TAG, "isAuthorized()>> GCM message authorized" );
        }else{
            Log.d(TAG, "isAuthorized()>> GCM message NOT authorized" );
        }
        return ret;
    }


    public static String sendCmd(String cmd) {

        Socket socket = null;
        String stringReceived = "";


        try {

            response = RESULT_UNKNOWN;
            String ipAddressFromPref = Utility.getPreferredIPAdd(mContext);
            int  ipPortFromPref = Utility.getPreferredIPPort(mContext);
            socket = new Socket(ipAddressFromPref, ipPortFromPref);
            socket.setSoTimeout(CommonConstants.SOCKET_TIMEOUT);

            ByteArrayOutputStream byteArrayOutputStream =
                    new ByteArrayOutputStream(1024);

            byte[] buffer = new byte[1024];

            int bytesRead;
            InputStream inputStream = socket.getInputStream();
            OutputStream outputStream = socket.getOutputStream();


            outputStream.write(cmd.getBytes());
            outputStream.flush();


            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
                stringReceived += byteArrayOutputStream.toString("UTF-8");
            }
            outputStream.close();
            inputStream.close();
            response = RESULT_SUCCESS;

        } catch (ConnectException e) {
            e.printStackTrace();
            String errorStr = getConnectionError(e);
            if (errorStr != null)
                response = getConnectionErrorCode(errorStr);
            else
                response = RESULT_UNKNOWN;

        } catch (UnknownHostException e) {
            e.printStackTrace();
            response = RESULT_HOSTNAME_NOT_FOUND;

        } catch (SocketTimeoutException e) {

            e.printStackTrace();
            response = RESULT_TIMEOUT;

        } catch (IOException e) {
            e.printStackTrace();
            response = RESULT_IO_ERROR;


        } finally {

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        String ret = null;
        if(response == RESULT_SUCCESS){
            ret = stringReceived;
        }else {
            //ret = getStatusString(response);

        }
        Log.d(TAG, "sendCmd()>> reply with " + ret);
        return ret;

    }


}
