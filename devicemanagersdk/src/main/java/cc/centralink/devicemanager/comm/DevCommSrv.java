package cc.centralink.devicemanager.comm;

import android.app.IntentService;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.centralink.Setting;
import com.centralink.account.framework.CentralinkAccounts;
import com.crashlytics.android.Crashlytics;
import com.kclin.einsteinsdk.capability.Cap;
import com.kclin.einsteinsdk.capability.Caps;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import cc.centralink.devicemanager.util.StringCodec;

import static java.lang.Thread.sleep;


public class DevCommSrv extends Service {

    private static final String TAG = DevCommSrv.class.getSimpleName();

    boolean isBound = false;
    boolean firstRun = false;
    private static final String PREFERENCE_FIRST_RUN = "FIRST_RUN";

    /* mMessenger use to receive from client */
    final Messenger mMessenger = new Messenger(new IncomingHandler()); // Target we publish for clients to send messages to IncomingHandler.

    /* devMessenger to handle request response from zwave / bt / zigbee */
    final Messenger devBackMessenger = new Messenger(new devBackHandler()); // Target we publish for clients to send messages to IncomingHandler.

    public static final int PROTOCOL_ZWAVE  = 0;
    public static final int PROTOCOL_ZIGBEE = 1;
    public static final int PROTOCOL_BT     = 2;

    public final int MSG_INSTALL_CLIENT = 1;
    public final int MSG_UNINSTALL_CLIENT = 2;
    public final int MSG_REGISTER_CLIENT = 3;
    public final int MSG_UNREGISTER_CLIENT = 4;
    public final int MSG_QUERY_CAPABILITY = 5;
    public final int MSG_TRIGGER_ACTION = 6;
    public final int MSG_SEND_SOCKET_RSP = 7;
    public final int MSG_GET_RULE_LIST = 8;
    public final int MSG_SET_RULE_LIST = 9;

    Messenger einsteinLocalSrv = null;
    Messenger EinteinResponseMessenger = null;

    private ServiceConnection myConnection;

    private static HashMap<String, Bundle> cmdBundleMap = new HashMap<String, Bundle>();
    private static ArrayList<Messenger> msgList = new ArrayList<Messenger>();
    private static ArrayList<Messenger> ruleMsgList = new ArrayList<Messenger>();
    private static SocketClient cmdSocket = null;
    private static Messenger dbgMsg = null;
    private static int commandBundleID = 0;

    private AsyncHttpClient httpClient = new AsyncHttpClient();

    SocketClient.OnMessageReceived listener = new SocketClient.OnMessageReceived() {
        @Override
        public void messageReceived(String message) throws RemoteException {

            Log.e(TAG, "Receive Socket data: " + message);


            Message msg = Message.obtain();
            final JSONObject jsMessage = new JSONObject();
            JSONObject serverMsg;
            try {
                serverMsg = new JSONObject(message);
                if (serverMsg.get(CommSrvProtocol.PACKET_KEY_PROTOCOL).equals("Z-Wave")) {
                    msg.what = PROTOCOL_ZWAVE;
                }

                if (serverMsg.get(CommSrvProtocol.PACKET_KEY_TYPE).equals("command")) {
                    Bundle b;
                    String bundleId = serverMsg.getString(CommSrvProtocol.PACKET_KEY_BUNDLEID);
                    if (bundleId == null) {
                        Log.e("DevCommSrv", "No bundle id Error");
                        return;
                    }
                    synchronized (cmdBundleMap) {
                        b = cmdBundleMap.get(bundleId);

                        if (b == null) {
                            Log.e("DevCommSrv", "bundleid: " + bundleId +  " is not in the bundle map.");
                            return;
                        }

                        cmdBundleMap.remove(bundleId);
                        b.putString("result", message);
                        msg.setData(b);
                    }

                    try {
                        devBackMessenger.send(msg);
                        if (dbgMsg != null) {
                            Message dbgM = new Message();
                            dbgM.setData(b);
                            dbgMsg.send(dbgM);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                else if (serverMsg.get(CommSrvProtocol.PACKET_KEY_TYPE).equals("notification") &&
                         serverMsg.get(CommSrvProtocol.PACKET_KEY_COMMAND).equals("ValueChanged")) {
                    String nodeId = null, valueId = null, value = null, nodeCap = null, nodeName = null;
                    Bundle b = new Bundle();

                    try {
                        //need protect null pointer
                        nodeId = serverMsg.getJSONObject(CommSrvProtocol.PACKET_KEY_RESULT).get("node_id").toString();
                        valueId = serverMsg.getJSONObject(CommSrvProtocol.PACKET_KEY_RESULT).get("val_id").toString();
                        nodeCap = serverMsg.getJSONObject(CommSrvProtocol.PACKET_KEY_RESULT).get("com/kclin/einsteinsdk/capability").toString();
                        nodeName = nodeCap;
                        jsMessage.put("from",    "OnNotification");
                        jsMessage.put("node_id", nodeId);
                        jsMessage.put("val_id",  valueId);
                        jsMessage.put("nodeCapability", nodeCap);
                        jsMessage.put("nodeName", nodeName);

                        // TODO: what's this for?
                        value = serverMsg.getJSONObject("result").get("val").toString();
                        jsMessage.put("value",      value);

                        Log.e(TAG, "update id: " + jsMessage.getString("node_id"));
                        Log.e(TAG, "update value: " + jsMessage.getString("value"));
                        Log.e(TAG, "update node type: " + jsMessage.getString("nodeCapability"));
                        Log.e(TAG, "update node name: " + jsMessage.getString("nodeName"));

                    } catch (JSONException e) {
                        Crashlytics.logException(e);
                        e.printStackTrace();
                    }
                    b.putString("result", jsMessage.toString());
                    b.putString("from", "OnNotification");
                    b.putString("nodeid", nodeId);

                    Log.e(TAG, "jsMessage.toString" + jsMessage.toString());

                    msg.setData(b);

                    sendMessageToListener(msg, msgList);
                    sendDebugMessage(msg);

                    final String nodeIdF = nodeId;
                    final String valueIdF = valueId;
                    final String valueF = value;

                    // Need nodeCapability & nodeName?

                    // Report to cloud server
                    String currentDateTimeString = new Date().toString();
                    RequestParams params = new RequestParams();
                    Log.e(TAG, "TabletId: " + Setting.getUUID(getApplicationContext()));
                    Log.e(TAG, "session: " + CentralinkAccounts.getSession(getApplicationContext()));
                    Log.e(TAG, "t:" + currentDateTimeString);
                    Log.e(TAG, "hash:" + StringCodec.hmacSha1Digest(currentDateTimeString, CentralinkAccounts.getToken(getApplicationContext())));
                    params.put("session", CentralinkAccounts.getSession(getApplicationContext()));
                    params.put("t", currentDateTimeString);
                    params.put("hash", StringCodec.hmacSha1Digest(currentDateTimeString, CentralinkAccounts.getToken(getApplicationContext())));
                    params.put("tabletId", Setting.getUUID(getApplicationContext()));
                    params.put("nodeId", nodeIdF);
                    params.put("nodeFunction", valueIdF);
                    params.put("nodeStatus", valueF);
                    params.put("nodeCapability", nodeCap);
                    params.put("nodeName", nodeName);
                    params.put("ops", "update");
                    params.put("Content-Type", "application/json");
                    params.put("Accept", "application/json");
                    httpClient.post("http://dev.centralink.cc:443/api/v1/node/update/", params, new JsonHttpResponseHandler() {
                        @Override
                        public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                            super.onSuccess(statusCode, headers, response);
                            Log.e(TAG, "device status update success! response: " + (response == null ? "null" : response.toString()));
                        }

                        @Override
                        public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                            super.onFailure(statusCode, headers, throwable, errorResponse);
                            Log.e(TAG, "device status update failed! errorResponse: " + (errorResponse == null ? "null" : errorResponse.toString()));
                            Crashlytics.logException(throwable);
                        }
                    });
                    return;
                }
                else if (serverMsg.get(CommSrvProtocol.PACKET_KEY_TYPE).equals("notification") &&
                         serverMsg.get(CommSrvProtocol.PACKET_KEY_COMMAND).toString().contains("StateChanged")) {
                    Bundle b = new Bundle();
                    final String operationCommand = "state";
                    String deviceState = null;
                    String nodeId;
                    String commandName = serverMsg.get("name").toString();
                    if (commandName.contains("NodeQueryComplete")) {
                        deviceState = "device_added";
                    } else if (commandName.contains("NodeRemoved")) {
                        deviceState = "device_removed";
                    } else if (commandName.contains("NodeSleep")) {
                        deviceState = "device_sleep";
                    } else if (commandName.contains("NodeDead")) {
                        deviceState = "device_dead";
                    }
                    else {
                        Log.e("Handle state changed: ", "Get unhandled state " + commandName);
                        return;
                    }

                    nodeId = serverMsg.getJSONObject(CommSrvProtocol.PACKET_KEY_RESULT).
                                       get(CommSrvProtocol.PACKET_RES_KEY_NODEID).toString();

                    b.putString(operationCommand, deviceState);
                    b.putString("nodeid", nodeId);
                    b.putString("result", serverMsg.getJSONObject(CommSrvProtocol.PACKET_KEY_RESULT).toString());
                    msg.setData(b);
                    sendMessageToListener(msg, msgList);
                    return;
                }
            } catch (JSONException e) {
                Log.e("message: ", message);
                Crashlytics.logException(e);
                return;
            }

        }
    };

    private void sendMessageToEinsteinSafely(Message msg) {
        if (einsteinLocalSrv != null) {
            try {
                einsteinLocalSrv.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
            }
        } else {
            Crashlytics.log("DevCommSrv einstein messenger is NULL when trying to send message: " + msg.toString());
        }
    }

    private void sendDebugMessage(Message msg) {
        if (null == dbgMsg) return;

        try {
            dbgMsg.send(msg);
        } catch (RemoteException e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }

    }

    private void sendMessageToListener(Message msg,  ArrayList<Messenger> listenerList) {
        for (int i = 0 ; i < listenerList.size(); i++) {

            try {
                listenerList.get(i).send(msg);
            } catch (DeadObjectException e) {
                // DeadObjectException is thrown if the target Handler no longer exists,
                // so remove it from the list.
                listenerList.remove(i);
                e.printStackTrace();
                Crashlytics.logException(e);
            } catch (RemoteException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
            }
        }
    }

    public DevCommSrv() {
    }

    public static synchronized void setDbg(Messenger msg) {

        dbgMsg = msg;

    }

    public static synchronized void addMessenger(Messenger msg) {

        if (msgList.indexOf(msg) > -1) {
            Log.e(TAG, "addMessenger: already in list");
            msgList.remove(msg);
        }

        msgList.add(msg);

    }

    public static synchronized void removeMessenger(Messenger msg) {
        if (msgList.contains(msg)) {
            msgList.remove(msg);
        }
    }

    public static synchronized void addCmdBundle(Bundle b) {
        synchronized (cmdBundleMap) {
            b.putString("bundleid", Integer.toString(commandBundleID++));
            cmdBundleMap.put(b.getString("bundleid"), b);
        }
    }


    @Override
    public void onCreate() {
        super.onCreate();

        //Bind Einstein Service

        /* Handle Response from Einstein */
        class EinsteinResponseHandler extends Handler {

            @Override
            public void handleMessage(Message msg) {
                Log.e(TAG, "EinsteinResponseHandler got response");

                if (msg.getData().getString("rules") != null) {

                    Message sendMsg = new Message();
                    sendMsg.what = msg.what;
                    sendMsg.setData(msg.getData());
                    sendMessageToListener(sendMsg, ruleMsgList);
                    return;
                }

                try {
                    JSONObject proCmd = new JSONObject(msg.getData().getString("data"));
                    msg.getData().putString("nodeid", proCmd.getString("nodeId"));
                    msg.getData().putString("nodeval", proCmd.getString("value"));

                    addCmdBundle(msg.getData());

                    startActionSetVal(getApplicationContext(), msg.getData());
                } catch (JSONException e) {
                    Crashlytics.logException(e);
                    e.printStackTrace();
                }

                /* TODO: this part should be placed in onResultReceive ?? */

                Message rspmsg = Message.obtain();
                Bundle b = new Bundle();
                //Log.e("handleMessage", msg.getData().getString("data"));

                try {
                    JSONObject jo = new JSONObject(msg.getData().getString("data"));

                    Log.e(TAG, "handleMessage JO " + jo);
                    Log.e(TAG, "handleMessage JO nodeId: " + jo.getString("nodeId"));
                    Log.e(TAG, "handleMessage JO _id: " + jo.getString("_id"));
                    b.putString("src", "DEV");
                    b.putString("target", "CLOUD");
                    b.putString("status", "done");
                    b.putString("value", jo.getString("value"));
                    b.putString("_id", jo.getString("_id"));
                    rspmsg.what = MSG_SEND_SOCKET_RSP;
                    rspmsg.setData(b);
                    sendMessageToEinsteinSafely(rspmsg);
                } catch (JSONException e) {
                    e.printStackTrace();
                    Crashlytics.logException(e);
                }
            }
        }


        /* Bind Service */
        myConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                Log.e(TAG, "onServiceConnected");
                einsteinLocalSrv = new Messenger(service);
                EinteinResponseMessenger = new Messenger(new EinsteinResponseHandler());
                isBound = true;

                Message msg = Message.obtain();

                if (firstRun) {

                    //Register capabilities
                    msg.getData().putParcelable("com/kclin/einsteinsdk/capability", initCapability());
                    msg.what = MSG_INSTALL_CLIENT;
                    Log.e(TAG, "initCapability: " + initCapability());
                    sendMessageToEinsteinSafely(msg);
                }

                //Provide messenger
                Log.e(TAG, "register client to Einstein.");
                msg.replyTo = EinteinResponseMessenger;
                msg.what = MSG_REGISTER_CLIENT;
                Bundle data = new Bundle();
                data.putString("component", "DEV");
                msg.setData(data);
                sendMessageToEinsteinSafely(msg);

            }

            public void onServiceDisconnected(ComponentName className) {
                Log.e(TAG, "onServiceDisconnected");
                einsteinLocalSrv = null;
                isBound = false;
            }
        };

        // Create socket to connect to OZW
        cmdSocket = new SocketClient(listener);
        new Thread(cmdSocket).start();

        //Invoke Einstein
        Intent i = createExplicitFromImplicitIntent(this, new Intent("einstein.locaservice"));
        ComponentName c = getApplicationContext().startService(i);
        if (c == null) {
            Log.e(TAG, "DeviceManager failed to start with " + i);
        } else {
            bindService(i, myConnection, BIND_AUTO_CREATE);
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (myConnection != null) {
            unbindService(myConnection);
        }
    }

    class IncomingHandler extends Handler { // Handler of incoming requests from callers.
        @Override
        public void handleMessage(Message msg) {

            Log.e(TAG, "Receive message " + msg.what);



            while (!getSocket().isConnected()) {

                try {
                    sleep(1000, 0);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Bundle b = msg.getData();
            Log.e(TAG, "message bundle: " + b);
            String cmd = b.getString("cmd");
            String einsteinCmd = b.getString("einsteincmd");

            if (einsteinCmd != null) {
                handleEinsteinMessage(msg, einsteinCmd);
            } else if (cmd != null && b.getParcelable("rspClient") != null) {

                b.putParcelable("replyto", devBackMessenger);

                if (!cmd.equalsIgnoreCase("register_rsp") &&
                        !cmd.equalsIgnoreCase("unregister_rsp")) {
                    DevCommSrv.addCmdBundle(b);
                }

                Log.e(TAG, "rspClient:" + b.getParcelable("rspClient").toString());

                switch (cmd) {
                    case "add":

                        startActionAdd(getApplicationContext(), b, false);

                        break;

                    case "addsecurity":

                        startActionAdd(getApplicationContext(), b, true);

                        break;

                    case "remove":

                        startActionRemove(getApplicationContext(), b, false);

                        break;

                    case "removesecurity":

                        startActionRemove(getApplicationContext(), b, true);

                        break;

                    case "cancel":

                        startActionCancel(getApplicationContext(), b);

                        break;

                    case "reset":

                        startActionReset(getApplicationContext(), b);

                        break;

                    case "setval":

                        startActionSetVal(getApplicationContext(), b);

                        break;

                    case "list":

                        startActionList(getApplicationContext(), b);

                        break;

                    case "register_rsp":

                        Messenger regRsp = b.getParcelable("rspClient");
                        addMessenger(regRsp);

                        break;

                    case "unregister_rsp":

                        removeMessenger((Messenger) b.getParcelable("rspClient"));

                        break;

                    default:
                        Log.e(TAG, "msg.what: default");
                        super.handleMessage(msg);
                }
            }
        }

        private void handleEinsteinMessage(Message msg, String einsteinCmd) {
            Bundle b = msg.getData();

            switch (einsteinCmd) {
                // TODO:
                // it is not go to put this event here, it will be refactor later.
                case "notification_event": {
                    msg.what = MSG_SEND_SOCKET_RSP;
                    notfiDeviceEvent(msg);
                    break;
                }

                case "getrulelist": {
                    msg.what = MSG_GET_RULE_LIST;
                    Messenger msgHandler = b.getParcelable("rspClient");
                    ruleMsgList.add(msgHandler);
                    notfiDeviceEvent(msg);
                    break;
                }

                case "setrulelist": {
                    msg.what = MSG_SET_RULE_LIST;
                    notfiDeviceEvent(msg);
                    break;
                }

                default:
                    Log.e(TAG, "msg.what: default");
            }
        }
    }

    // Handler of incoming messages from ZWave/ZigBee/BT HW modules.
    class devBackHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            Log.e(TAG, "Device HW modules Handler receive message " + msg.what);

            Bundle b = msg.getData();
            Messenger rspClient = b.getParcelable("rspClient");

            if (rspClient == null) {
                Log.e(TAG, "Device HW module handler> Do nothing since no 'rspClient'");
                return;

            }

            Message sendmsg = new Message();
            sendmsg.what = msg.what;
            sendmsg.setData(b);

            switch (msg.what) {
                case PROTOCOL_ZWAVE:
                    Log.e(TAG, "Received message ( " + sendmsg.getData() + " ) from PROTOCOL_ZWAVE");
                    try {
                        rspClient.send(sendmsg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                        Crashlytics.logException(e);
                    }
                    break;

                case PROTOCOL_ZIGBEE:
                    Log.e(TAG, "Received message ( " + sendmsg.getData() + " ) from PROTOCOL_ZIGBEE");
                    break;

                case PROTOCOL_BT:
                    Log.e(TAG, "Received message ( " + sendmsg.getData() + " ) from PROTOCOL_BT");
                    break;

                default:
                    Log.e(TAG, "Received message ( " + sendmsg.getData() + " ) from default");
                    super.handleMessage(msg);
            }
        }
    }


    /***
     * Android L (lollipop, API 21) introduced a new problem when trying to invoke implicit intent,
     * "java.lang.IllegalArgumentException: Service Intent must be explicit"
     * <p/>
     * If you are using an implicit intent, and know only 1 target would answer this intent,
     * This method will help you turn the implicit intent into the explicit form.
     * <p/>
     * Inspired from SO answer: http://stackoverflow.com/a/26318757/1446466
     *
     * @param context
     * @param implicitIntent - The original implicit intent
     * @return Explicit Intent created from the implicit original intent
     */
    Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "OnBind");
        return mMessenger.getBinder();
    }


    public Caps initCapability() {
        Caps caps = new Caps();
        caps.add(new Cap(0, "DEV", "ADD"));
        caps.add(new Cap(0, "DEV", "REMOVE"));
        caps.add(new Cap(0, "DEV", "QUERY"));
        return caps;

    }

    // ---------------------------------------------------------------------------------------------
    // Private helpers

    /**
     * Starts this service to perform action Add with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    private void startActionAdd(Context context,Bundle b, boolean security) {

        String bundleID = b.getString("bundleid");
        Log.e(TAG, "Add");

        // Send ZWave command
        comZwave("CONTROLLER~adddsecurity" + "~" + bundleID);

        // Report server
        String currentDateTimeString = new Date().toString();
        RequestParams params = new RequestParams();
        params.put("session", CentralinkAccounts.getSession(getApplicationContext()));
        params.put("t", currentDateTimeString);
        params.put("hash", StringCodec.hmacSha1Digest(currentDateTimeString, CentralinkAccounts.getToken(getApplicationContext())));
        params.put("tabletId", Setting.getUUID(getApplicationContext()));
        params.add("nodeId",            "");
        params.add("nodeName",          "");
        params.add("nodeCapability",    "");
        params.add("nodeName",          "");
        params.add("ops",               "add");
        httpClient.post("http://dev.centralink.cc:443/api/v1/node/ops/", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.e(TAG, "device status update success! response: " + (response == null ? "null" : response.toString()));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Log.e(TAG, "device status update failed! response: " + (errorResponse == null ? "null" : errorResponse.toString()));
                Crashlytics.logException(throwable);
            }
        });
    }

    /**
     * Starts this service to perform action Remove with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    private void startActionRemove(Context context, Bundle b, boolean security) {

        String bundleID = b.getString("bundleid");
        Log.e(TAG, "Remove");

        // Send ZWave command
        if (security) {
            comZwave("CONTROLLER~remdsecurity~" + b.getString("nodeid") + "~" + bundleID);
        } else {
            comZwave("CONTROLLER~remd~" +b.getString("nodeid") + "~" + bundleID);
        }


        // Report server
        String currentDateTimeString = new Date().toString();
        RequestParams params = new RequestParams();
        params.put("session", CentralinkAccounts.getSession(getApplicationContext()));
        params.put("t", currentDateTimeString);
        params.put("hash", StringCodec.hmacSha1Digest(currentDateTimeString, CentralinkAccounts.getToken(getApplicationContext())));
        params.put("tabletId", Setting.getUUID(getApplicationContext()));
        params.add("nodeId",            "");
        params.add("nodeName",          "");
        params.add("nodeCapability",    "");
        params.add("nodeName",          "");
        params.add("ops",               "del");
        httpClient.post("http://dev.centralink.cc:443/api/v1/node/ops/", params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                super.onSuccess(statusCode, headers, response);
                Log.e(TAG, "device status update success! response: " + (response == null ? "null" : response.toString()));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                super.onFailure(statusCode, headers, throwable, errorResponse);
                Log.e(TAG, "device status update failed! response: " + (errorResponse == null ? "null" : errorResponse.toString()));
                Crashlytics.logException(throwable);
            }
        });
    }


    /**
     * Starts this service to perform action Remove with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    private void startActionList(Context context, Bundle b) {

        String bundleID = b.getString("bundleid");
        Log.e(TAG, "List");

        comZwave("ALIST" + "~" + bundleID);
    }

    /**
     * Starts this service to perform action Remove with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    private void startActionReset(Context context, Bundle b) {

        String bundleID = b.getString("bundleid");
        Log.e(TAG, "Reset");

        comZwave("CONTROLLER~reset" + "~" + bundleID);
    }



    /**
     * Starts this service to perform action Remove with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    private void startActionCancel(Context context, Bundle b) {

        String bundleID = b.getString("bundleid");
        Log.e(TAG, "Reset");

        comZwave("CONTROLLER~cancel" + "~" + bundleID);
    }


    /**
     * Starts this service to perform action Remove with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    private void startActionSetVal(Context context, Bundle b) {

        Log.e(TAG, "SetVal");
        String bundleID = b.getString("bundleid");

        comZwave("DEVICE~" + b.getString("nodeid") + "~" +
                b.getString("nodeval") + "~100" + "~" + bundleID);
    }

    private void notfiDeviceEvent(Message msg) {

        Bundle b = msg.getData();

        b.putString("src", "DEV");
        b.putString("target", "LOCAL");
        b.putString("status", "done");

        msg.setData(b);
        sendMessageToEinsteinSafely(msg);
    }

    private void comZwave(String cmd) {

        Log.e(TAG, "comZwave start cmd: " + cmd);
        try {
            getSocket().sendMessage(cmd);
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }
    }

    private synchronized SocketClient getSocket() {
        if (cmdSocket == null || !cmdSocket.isConnected()) {
            if (cmdSocket != null) {
                cmdSocket.stopClient();
            }
            cmdSocket = new SocketClient(listener);
            new Thread(cmdSocket).start();
        }
        return cmdSocket;
    }
}
