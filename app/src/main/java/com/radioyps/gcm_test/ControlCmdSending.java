package com.radioyps.gcm_test;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Created by developer on 28/10/16.
 */
public class ControlCmdSending extends AsyncTask<String, Void, String>{

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

private static final String TAG="sendControlCmd";
@Override
protected String doInBackground(String...params){

    Socket socket = null;
    String stringReceived = "";
    String cmd = params[0];

    try {

        response = RESULT_UNKNOWN;
        int ipPortFromPref = GCMGateWay.getipPortFromPref();
        String ipAddressFromPref = GCMGateWay.getipAddressFromPref();

        socket = new Socket();
        socket.connect(new InetSocketAddress(ipAddressFromPref, ipPortFromPref), CommonConstants.CONNECT_TIMEOUT);
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
}