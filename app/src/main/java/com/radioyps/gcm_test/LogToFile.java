package com.radioyps.gcm_test;

/**
 * Created by developer on 25/08/16.
 */
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
 This class contains different methods to preform file operation like read/write a local/asset file
 */
public class LogToFile {
    public static final String TAG = "GCM_log";
    public static final String DEFAULT_FILE_NAME = "gcm_logs";
    public static final boolean isTesting = true;

    /*
     This function will write something to a local file.
     */
    public static void writeToFile(String content, String fileName, String header) {
        try {
            boolean write = false;
            File f = new File(Environment.getExternalStorageDirectory(), fileName);

            if (!f.exists()) {
                write = true;
            }
            FileWriter fw = new FileWriter(f, true); // the true will append the new data
            if (write) {
                if (header != null) {
                    fw.write(header + "\n");//
                }
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
            String currentDateandTime = sdf.format(new Date());
            fw.write(currentDateandTime + "," + content + "\n");
            fw.close();
        } catch (Exception ioe) {

            Log.e(TAG,ioe.getMessage());
        }
    }

    public static void toFile(String tag, String content) {
        if (isTesting) {
            Log.d(tag, content);
            writeToFile(tag + ":" + content, DEFAULT_FILE_NAME, null);
        }
    }

    /**
     * Writes exception stackTrace to file
     * @param e
     */
    public static void toFile(String tag,Exception e) {

        if(isTesting) {
            String exceptionString = "";
            StackTraceElement[] stackTraceElement = e.getStackTrace();
            exceptionString = e.getLocalizedMessage()+"\n";
            for(StackTraceElement stackTraceElement1:stackTraceElement){
                exceptionString += stackTraceElement1.toString() +"\n";
            }
            Log.e(tag,exceptionString);
            writeToFile(exceptionString, DEFAULT_FILE_NAME, null);

        }
    }
}
