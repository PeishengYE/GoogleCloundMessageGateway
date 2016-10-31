package com.radioyps.gcm_test;

/**
 * Created by yep on 10/10/16.
 */
public class CommonConstants {
    public static int connectPort = 5028;
    /* for real door controller */
    public static final String IP_ADDR_DOOR_CONTROLLER = "192.168.12.240";
    /* for test door controller */
    //public static final String IP_ADDR_DOOR_CONTROLLER = "192.168.12.238";
    public final static int SOCKET_TIMEOUT = 10 * 1000; /*10 seconds */
    public final static int CONNECT_TIMEOUT = 2 * 1000; /*10 seconds */

    public static final String PREF_IS_LOCAL_TOKEN_RECEVIED = "com.radioyps.gcm_test.PREF_IS_LOCAL_TOKEN_RECEVIED";
    public static final String PREF_LOCAL_TOKEN_SAVING_KEY = "com.radioyps.gcm_test.PREF_LOCAL_TOKEN_SAVING_KEY";


    public static final String PREF_IS_REMOTE_TOKEN_RECEVIED = "com.radioyps.gcm_test.PREF_IS_REMOTE_TOKEN_RECEVIED";
    public static final String PREF_REMOTE_TOKEN_SAVING_KEY = "com.radioyps.gcm_test.PREF_REMOTE_TOKEN_SAVING_KEY";

    public static final String PREF_REMOTE_TOKEN_EMPTY = "com.radioyps.gcm_test.PREF_REMOTE_TOKEN_EMPTY";

    public static final String LOCAL_BROADCAST_ACTION = "com.radioyps.gcm_test.broadcast_action";
    public static final String LOCAL_BROADCAST_UPDATE_MESSAGE = "com.radioyps.gcm_test.LOCAL_BROADCAST_UPDATE_MESSAGE";

    public static final String GCM_SENDING_TOKEN_KEY = "GCMToken";
    public static final String GCM_SENDING_TIME_KEY = "sendTime";
    public static final int TMP_MAXIMUM_OUTPUT_LENGTH = 2*1024;
    public static final String TEMP_IMAG_FILENAME = "GCM_image_from_local.jpg";
    public static final int GCM_MAX_DATA_LENGTH = 2*1024;
    public static final String GCM_SENDING_FLAG = "DATA_";
    public static final String GCM_SENDING_END_FLAG = "DATA_888";
    public static final String GCM_SENDING_END_MESG = "FINISHED";

}
