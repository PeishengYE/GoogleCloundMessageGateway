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
    public final static int SOCKET_TIMEOUT = 10 * 000; /*10 seconds */

    public static final String PREF_IS_TOKEN_RECEVIED = "com.radioyps.gcm_test.PREF_IS_TOKEN_RECEVIED";
    public static final String PREF_SAVED_TOKEN = "com.radioyps.gcm_test.PREF_SAVED_TOKEN";


    public static final String LOCAL_BROADCAST_ACTION = "com.radioyps.gcm_test.broadcast_action";
    public static final String LOCAL_BROADCAST_UPDATE_MESSAGE = "com.radioyps.gcm_test.LOCAL_BROADCAST_UPDATE_MESSAGE";


}
