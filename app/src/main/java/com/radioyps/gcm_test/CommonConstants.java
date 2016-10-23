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
    public static final String GCM_authrized_mesg = "ds`133*(/.Mnd";
    public static String CMD_PRESS_DOOR_BUTTON = "A412..&35?@!";
    public static String ACK_PRESS_DOOR_BUTTON =  "B835??/!xx";

    public static final String CMD_PING_CONTROLLER = "78*(^@/uid";
    public static final String PING_ACK = "^3234adsfa/?";
    public static final String ACTION_PING = "com.radioyps.gcm_test.doorcontroller.ACTION_PING";
    public static final String ACTION_PRESS_DOOR_BUTTON = "com.radioyps.gcm_test.doorcontroller.ACTION_PRESS_DOOR_BUTTON";
    public static final String ACTION_PRESS_REMOTE_BUTTON = "com.radioyps.gcm_test.doorcontroller.ACTION_PRESS_REMOTE_BUTTON";
    public static final String ACTION_STOP_CONNECTING = "com.radioyps.gcm_test.doorcontroller.ACTION_STOP_CONNECTING";
    public static final String ACTION_START_CONNECTING = "com.radioyps.gcm_test.doorcontroller.ACTION_START_CONNECTING";

    public static final String PREF_IS_TOKEN_RECEVIED = "com.radioyps.gcm_test.doorcontroller.PREF_IS_TOKEN_RECEVIED";
    public static final String PREF_SAVED_TOKEN = "com.radioyps.gcm_test.doorcontroller.PREF_SAVED_TOKEN";


    public static final String LOCAL_BROADCAST_ACTION = "broadcast_action";


}
