package cc.centralink.devicemanager.sdk;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cc.centralink.devicemanager.comm.CommSrvProtocol;
import cc.centralink.devicemanager.sdk.rule.Rule;
import cc.centralink.devicemanager.sdk.rule.Rules;
import cc.centralink.devicemanager.sdk.zwave.BaseZwaveDevice;

/**
 * Created by davidliu on 8/20/15.
 */
public class DeviceManager {

    // TODO:
    // It needs to collect device name to those device class, not here, just a temporary workaround.
    public static final String ZWAVE_DEVICE_NAME_POWER_SWITCH = "Power Switch";
    public static final String ZWAVE_DEVICE_NAME_LOCK = "Lock";
    public static final String ZWAVE_DEVICE_NAME_SENSOR = "Sensor";

    public static final String ZWAVE_DEVICE_NAME_MULTILEVEL_SWITCH = "Multilevel Power Switch";
    public static final String ZWAVE_DEVICE_NAME_ROUTING_BINARY_SENSOR = "Routing Binary Sensor"; // PIR, Magnet
    public static final String ZWAVE_DEVICE_NAME_HOME_SECURITY_SENSOR = "Home Security Sensor";
    public static final String ZWAVE_DEVICE_NAME_ALARM_SENSOR = "Alarm Sensor"; // CO, Smoke

    private static final String TAG = DeviceManager.class.getSimpleName();

    private static Context context;
    private static Messenger deviceControlMessenger;
    private static ServiceConnection mDeviceServiceConnection;
    private static Messenger deviceResponseHandler;
    private static Messenger ruleResponseHandler;
    private static List<AbstractDevice> devicesList;
    private static List<DeviceListener> listeners;
    private static Rules rules;
    private boolean isBound = false;

    private static DeviceManager instance = null;
    public static DeviceManager get() {
        if (null == instance) {
            instance = new DeviceManager();
        }
        return instance;
    }

    public enum Command {
        LIST,
        ADD,
        ADDSECURITY,
        REMOVE,
        REMOVESECURITY,
        RESET,
        CANCEL
    }

    private DeviceManager() {
        mDeviceServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.e(TAG, "onServiceConnected: " + name);
                deviceControlMessenger = new Messenger(service);
                isBound = true;

                // Register state change callback
                registerDeviceStateChangeCallback(deviceResponseHandler);

                // Update status of connected devices
                listConnectedDevices();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.e(TAG, "onServiceDisconnected.");
                deviceControlMessenger = null;
                unregisterDeviceStateChangeCallback(deviceResponseHandler);
                isBound = false;
            }
        };
        deviceResponseHandler = new Messenger(new DeviceResponseHandler());
        ruleResponseHandler = new Messenger(new RuleResponseHandler());

        devicesList = new ArrayList();
        listeners = new ArrayList();
        rules = new Rules();
    }

    public static void setAndroidContext(Context ctx) {
        context = ctx.getApplicationContext();
    }
    public void start() {
        // Always call `setAndroidContext` before any operation.
        if (null == context) {
            throw new IllegalStateException("'setAndroidContext' must be call before any calls.");
        }
        if (!isBound) {
            // Bind device service
            Intent i = new Intent("devicemanager.DevCommService");
            context.bindService(i, mDeviceServiceConnection, Activity.BIND_AUTO_CREATE);
        }
        retrieveRulesListFromEinstein();
    }

    public void sendDeviceCommand(Message message) {
        if (null != message.getData()) {
            message.getData().putParcelable("rspClient", deviceResponseHandler);
        }
        if (deviceControlMessenger != null) {
            try {
                deviceControlMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendRuleCommand(Message message) {
        if (null != message.getData()) {
            message.getData().putParcelable("rspClient", ruleResponseHandler);
        }
        if (deviceControlMessenger != null) {
            try {
                deviceControlMessenger.send(message);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public List<AbstractDevice> getDevicesList() {
        return devicesList;
    }

    public void addListener(DeviceListener listener) {
        synchronized (listeners) {
            if (listeners.contains(listener)) return;
            listeners.add(listener);
        }
    }

    public void removeListener(DeviceListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Public Static Helper Methods

    public static Message getCommandMessage(Command command) {
        Message msg = new Message();
        Bundle b = new Bundle();
        switch (command) {
            case LIST:
                b.putString("cmd", "list");
                break;
            case ADD:
                b.putString("cmd", "add");
                break;
            case ADDSECURITY:
                b.putString("cmd", "addsecurity");
                break;
            case REMOVE:
                b.putString("cmd", "remove");
                break;
            case REMOVESECURITY:
                b.putString("cmd", "removesecurity");
                break;
            case RESET:
                b.putString("cmd", "reset");
                break;
            case CANCEL:
                b.putString("cmd", "cancel");
                break;
            default:
        }
        msg.setData(b);
        return msg;
    }

    public void setRuleList(List<Rule> ruleList) {
        rules.ruleList = ruleList;
        setRuleListToEinstein();
    }

    public List<Rule> getRuleList() {
        retrieveRulesListFromEinstein();
        return rules.ruleList;
    }

    // ---------------------------------------------------------------------------------------------
    // Private helpers

    private void registerDeviceStateChangeCallback(Messenger deviceMessenger) {
        if (deviceMessenger == null) {
            throw new IllegalArgumentException("Given messenger == null");
        }

        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("cmd", "register_rsp");
        b.putParcelable("rspClient", deviceMessenger);
        msg.setData(b);
        sendDeviceCommand(msg);
    }

    private void unregisterDeviceStateChangeCallback(Messenger deviceMessenger) {
        if (deviceMessenger == null) {
            throw new IllegalArgumentException("Given messenger == null");
        }

        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("cmd", "unregister_rsp");
        b.putParcelable("rspClient", deviceMessenger);
        msg.setData(b);
        sendDeviceCommand(msg);
    }

    private void listConnectedDevices() {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("cmd", "list");
        b.putParcelable("rspClient", deviceResponseHandler);
        msg.setData(b);
        sendDeviceCommand(msg);
    }

    private void notifyDeviceEvent(AbstractDevice device, String alarmName, String alarmValue) {
        int deviceAlarmValue = device.getAlarmValue(alarmName);

        if (deviceAlarmValue >= 0) {
            alarmValue = Integer.toString(deviceAlarmValue);
        }

        Message msg = new Message();
        Bundle b = new Bundle();

        b.putString("einsteincmd", "notification_event");
        b.putString("node_id", device.getNodeId());
        b.putString("name", alarmName);
        b.putString("value", alarmValue);
        b.putParcelable("rspClient", deviceResponseHandler);
        msg.setData(b);
        sendDeviceCommand(msg);
    }

    private void retrieveRulesListFromEinstein() {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("einsteincmd", "getrulelist");
        b.putParcelable("rspClient", ruleResponseHandler);
        msg.setData(b);
        sendRuleCommand(msg);
    }

    private void setRuleListToEinstein() {
        Message msg = new Message();
        Bundle b = new Bundle();
        b.putString("einsteincmd", "setrulelist");
        b.putString("rules", new Gson().toJson(rules));
        msg.setData(b);
        sendDeviceCommand(msg);
    }

    private void notifyListeners(Notification notification) {
        synchronized (listeners) {
            for (DeviceListener listener : listeners) {
                listener.onUpdate(notification);
            }
        }
    }

    private void handleDeviceUpdate(AbstractDevice device, Notification notification) {
        if (notification instanceof DeviceStateNotification) {
            DeviceStateNotification deviceStateNotification = (DeviceStateNotification) notification;
            String capabilityName = deviceStateNotification.key.toLowerCase(Locale.US);
            String capabilityValue = deviceStateNotification.value.toLowerCase(Locale.US);

            device.setCapability(capabilityName, capabilityValue);

            // notify listener : homedashboard and einstein.
            notifyDeviceEvent(device, capabilityName, capabilityValue);
            DeviceManager.this.notifyListeners(notification);
        }
    }

    // ---------------------------------------------------------------------------------------------

    // Handler of incoming messages from 'DevCommSrv'.
    class DeviceResponseHandler extends Handler {
        private BaseZwaveDevice parseDeviceFromString(String deviceStr) {
            if ((deviceStr.indexOf(ZWAVE_DEVICE_NAME_POWER_SWITCH) > 0) ||
                    (deviceStr.indexOf(ZWAVE_DEVICE_NAME_LOCK) > 0) ||
                    (deviceStr.indexOf(ZWAVE_DEVICE_NAME_SENSOR) > 0))
            {
                JsonObject deviceJson = new JsonParser().parse(deviceStr).getAsJsonObject();
                String deviceStatus = deviceJson.get(CommSrvProtocol.NODE_STATUS).toString();
                String deviceProperty = deviceJson.get(CommSrvProtocol.NODE_PROP).toString();

                BaseZwaveDevice device = new BaseZwaveDevice(deviceJson.get(CommSrvProtocol.NODE_NAME).toString()
                        , deviceJson.get(CommSrvProtocol.NODE_ID).getAsString());
                device.parse(deviceProperty);
                device.parseDeviceType(deviceJson.get(CommSrvProtocol.NODE_TYPE).toString());
                if (deviceStatus != null && deviceStatus.contains("Failed")) {
                    device.setDeviceStatus(BaseZwaveDevice.DeviceStatus.Failed);
                }

                devicesList.add(device);
                return device;
            }

            return null;
        }

        @Override
        public void handleMessage(Message msg) {
            Log.e("deviceMsgHandler", "Receive message what: " + msg.what);

            Bundle data = msg.getData();

            String cmd = data.getString("cmd");
            String result = data.getString("result");
            String state = data.getString("state");


            Log.e("deviceMsgHandler", "Receive message cmd: " + cmd);
            Log.e("deviceMsgHandler", "Receive message result: " + result);

            if (cmd != null && cmd.equalsIgnoreCase("list")) {

                    devicesList.clear();
                    JsonArray deviceAry = new JsonParser().parse(result)
                            .getAsJsonObject().getAsJsonArray(CommSrvProtocol.PACKET_KEY_RESULT);

                    for (int j = 0; j < deviceAry.size(); j++) {
                        Log.e("Device", deviceAry.get(j).toString());
                        JsonObject deviceObj = deviceAry.get(j).getAsJsonObject();
                        String nodeType = deviceObj.get(CommSrvProtocol.NODE_TYPE).toString();
                        if (nodeType.indexOf(ZWAVE_DEVICE_NAME_POWER_SWITCH) > 0 ||
                            nodeType.indexOf(ZWAVE_DEVICE_NAME_LOCK) > 0 ||
                            nodeType.indexOf(ZWAVE_DEVICE_NAME_SENSOR) > 0)
                        {
                            String deviceStatus = deviceObj.get(CommSrvProtocol.NODE_STATUS).toString();
                            BaseZwaveDevice newDevice = new BaseZwaveDevice(deviceObj.get(CommSrvProtocol.NODE_NAME).toString()
                                                            , deviceObj.get(CommSrvProtocol.NODE_ID).getAsString());
                            newDevice.parse(deviceObj.get(CommSrvProtocol.NODE_PROP).toString());
                            newDevice.parseDeviceType(nodeType);

                            if (deviceStatus != null && deviceStatus.contains("Failed")) {
                                newDevice.setDeviceStatus(BaseZwaveDevice.DeviceStatus.Failed);
                            }

                            devicesList.add(newDevice);
                        }

                    }

                    if (devicesList.size() == 0) {
                        Log.e(TAG, "ERROR: devices list is clear with wrong devices list result, result: " + result);
                    }
                //TODO:
//                BusProvider.getInstance().post(new UpdateDashboardEvent());
                return;
            } else if (cmd != null) {
                Log.e(TAG, "Zwave " + cmd + " result: " + result);
                return;
            }

            String nodeId = data.getString("nodeid");
            AbstractDevice currentDevice = null;
            for (final AbstractDevice device : devicesList) {
                if (device.getNodeId().equalsIgnoreCase(nodeId)) {
                    currentDevice = device;
                    break;
                }
            }

            //Handle state changed
            if (state != null) {
                ControllerStateNotification notification = null;
                BaseZwaveDevice.DeviceStatus deviceStatus = BaseZwaveDevice.DeviceStatus.Alive;

                if (state.equalsIgnoreCase("device_added")) {
                    if (currentDevice != null) {
                        Log.e(TAG, "ERROR: node(" + nodeId + ") already exist in deviceList");
                    }
                    else {
                        currentDevice = parseDeviceFromString(result);
                    }
                    notification = new ControllerStateNotification(ControllerStateNotification.STATE_DEVICE_ADDED);
                } else if (state.equalsIgnoreCase("device_removed")) {
                    if (currentDevice != null) {
                        devicesList.remove(currentDevice);
                        Log.e("Device", "remove node" + nodeId + " in deviceList");
                    }
                    notification = new ControllerStateNotification((ControllerStateNotification.STATE_DEVICE_REMOVED));
                } else if (state.equalsIgnoreCase("device_sleep")) {
                    notification = new ControllerStateNotification((ControllerStateNotification.STATE_DEVICE_SLEEP));
                } else if (state.equalsIgnoreCase("device_dead")) {
                    deviceStatus = BaseZwaveDevice.DeviceStatus.Failed;
                } else if (state.equalsIgnoreCase("controll_state_error")) {
                    notification = new ControllerStateNotification((ControllerStateNotification.STATE_CONTROLLER_ERROR));
                }

                if (currentDevice != null && currentDevice instanceof BaseZwaveDevice) {
                    ((BaseZwaveDevice) currentDevice).setDeviceStatus(deviceStatus);
                }

                DeviceManager.this.notifyListeners(notification);
            }

            // Handle OnNotification

            if (result != null) {
                Log.e(TAG, "OnNotification result: " + result);
                try {
                    DeviceStateNotification notification = new Gson().fromJson(result, DeviceStateNotification.class);
                    Log.e(TAG, "DeviceStateNotification: " + notification.toString());

                    if (currentDevice != null &&
                        notification.capability != null &&
                        currentDevice.getCapabilityByName(notification.key.toLowerCase(Locale.US))) {

                        handleDeviceUpdate(currentDevice, notification);

                        if (currentDevice.isNeedTurnOffAlarm(notification.key.toLowerCase(Locale.US))) {
                            //set 24seconds for default Philio PIR check interval
                            //TODO: using PIR ReDetect Interval TIme setting by device
                            final DeviceStateNotification stopNotification;
                            stopNotification = notification;
                            final AbstractDevice stopDevice;
                            stopDevice = currentDevice;

                            new CountDownTimer(24000,24000){
                                @Override
                                public void onFinish() {
                                    stopNotification.value = "0";
                                    handleDeviceUpdate(stopDevice, stopNotification);
                                }

                                @Override
                                public void onTick(long millisUntilFinished) {
                                }
                            }.start();
                        }
                    }
                } catch (JsonParseException e) {
                    e.printStackTrace();
                    Crashlytics.logException(e);
                }
                return;
            }
        }
    }

    class RuleResponseHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            String rules = data.getString("rules");

            if (rules != null) {
                Rules ruleSet = new Gson().fromJson(rules, Rules.class);

                if (ruleSet != null) {
                    DeviceManager.rules.ruleList = ruleSet.ruleList;
                }
                return;
            }
        }
    }
}
