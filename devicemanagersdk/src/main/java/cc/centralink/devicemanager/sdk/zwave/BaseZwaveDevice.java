package cc.centralink.devicemanager.sdk.zwave;

import android.os.Bundle;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import cc.centralink.devicemanager.sdk.AbstractDevice;
import cc.centralink.devicemanager.sdk.DeviceManager;
import cc.centralink.devicemanager.sdk.zwave.alarm.AccessControlAlarm;
import cc.centralink.devicemanager.sdk.zwave.alarm.Alarm;
import cc.centralink.devicemanager.sdk.zwave.alarm.BurglarAlarm;
import cc.centralink.devicemanager.sdk.zwave.alarm.COAlarm;
import cc.centralink.devicemanager.sdk.zwave.alarm.SmokeAlarm;

/**
 * Created by davidliu on 8/26/15.
 */
public class BaseZwaveDevice extends AbstractDevice {
    public enum DeviceStatus {
        Alive,
        Failed
    }
    public static final String FAILED_DEVICE = " ( Connection Lost ) ";
    private DeviceStatus deviceStatus = DeviceStatus.Alive;
    protected HashMap<String, Alarm> alarmMap;

    public BaseZwaveDevice(String name, String nodeId) {
        super(name, nodeId);
    }

    public BaseZwaveDevice(String name, String nodeId, String propertiesString) {
        super(name, nodeId, propertiesString);
    }

    public DeviceStatus getDeviceStatus() {
        return deviceStatus;
    }

    public void setDeviceStatus(DeviceStatus deviceStatus) {
        this.deviceStatus = deviceStatus;
    }

    public boolean setAlarmValue(String alarmName, String alarmValue) {
        if (alarmName == null || alarmValue == null) {
            return false;
        }

        Zwave.AlarmType alarmType = getAlarmTypeByName(alarmName);
        if (alarmType != Zwave.AlarmType.RESERVED) {
            Alarm alarm = this.alarmMap.get(alarmName);
            if (alarm == null) {
                alarm = getAlarmFactory(alarmType);
                this.alarmMap.put(alarmName, alarm);
            }

            alarm.setZwavAlarmType(Integer.parseInt(alarmValue.replaceAll("\"", "")));
            return true;
        }

        return false;
    }

    @Override
    public boolean isNeedTurnOffAlarm(String alarmName) {
        if (alarmName == null) {
            return false;
        }

        if (isAutoCloseAlarmType(alarmName)) {
            if (getAlarmValue(alarmName) == Alarm.ALARM_LEVEL_ON) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void parse(String propertiesString) {
        this.propertiesMap = new HashMap<>();
        this.alarmMap = new HashMap<>();
        String key = "";
        String val = "";

        JsonObject propList = new JsonParser().parse(propertiesString).getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : propList.entrySet()) {
            key = entry.getKey().toString();
            val = entry.getValue().toString();
            if (!setAlarmValue(key.toLowerCase(Locale.US), val.toLowerCase(Locale.US))) {
                propertiesMap.put(key.toLowerCase(Locale.US), val.toLowerCase(Locale.US));
            }
        }

    }

    // FIXME: Workaround function for now, should parse nodeType from device properties
    public void parseDeviceType(String nodeType)
    {
        if (nodeType.contains(DeviceManager.ZWAVE_DEVICE_NAME_POWER_SWITCH)) {
            setDeviceType(DeviceType.SWITCH);
        } else if (nodeType.contains(DeviceManager.ZWAVE_DEVICE_NAME_LOCK)) {
            setDeviceType(DeviceType.DOORLOCK);
        } else if (nodeType.contains(DeviceManager.ZWAVE_DEVICE_NAME_ALARM_SENSOR)) {
            if (nodeType.contains("Smoke") || getAlarmValue("smoke") != Alarm.ALARM_NOT_EXIST) { // Smoke device
                setDeviceType(DeviceType.SMOKE);
            } else if (nodeType.contains("CO") ||
                    getAlarmValue("carbon monoxide") != Alarm.ALARM_NOT_EXIST) { // CO device
                setDeviceType(DeviceType.CO);
            } else {
                setDeviceType(DeviceType.SENSOR);
            }
        } else if (nodeType.contains(DeviceManager.ZWAVE_DEVICE_NAME_SENSOR)) {
            if (getPropertiesMap().containsKey("external switch") ||
                    getAlarmValue("access control") != Alarm.ALARM_NOT_EXIST) { // Magnet (open/close)
                setDeviceType(DeviceType.MAGNET);
            } else if (getPropertiesMap().containsKey("temperature") ||
                    getAlarmValue("burglar") != Alarm.ALARM_NOT_EXIST) { // Vision PIR with temperature
                setDeviceType(DeviceType.PIR);
            } else {
                setDeviceType(DeviceType.SENSOR);
            }
        }
        else{
            setDeviceType(DeviceType.RESERVED);
        }
    }

    @Override
    public Message formatCommandMessage() {
        Message msg = Message.obtain();
        Bundle b = new Bundle();
        msg.setData(b);
        return msg;
    }

    @Override
    public boolean isActive() {

        // TODO:
        // put all logic in base class temporary from HomeDashBoard, it needs to move to each child class
        boolean bIsActive = false;

        Zwave.AlarmType alarmType = Zwave.AlarmType.RESERVED;
        switch (getDeviceType())
        {
            case SWITCH: {
                String val = getPropertiesMap().get("switch");
                if (val.equalsIgnoreCase("true")) {
                    bIsActive = true;
                }
                break;
            }
            case DOORLOCK:{
                String val = getPropertiesMap().get("locked");
                if (val.equalsIgnoreCase("true")) {
                    bIsActive = true;
                }
                break;
            }
            case SMOKE:
                alarmType = Zwave.AlarmType.SMOKE;
                break;
            case CO:
                alarmType = Zwave.AlarmType.CO;
                break;
            case PIR:
                alarmType = Zwave.AlarmType.BURGLAR;
                break;
            case MAGNET:
                alarmType = Zwave.AlarmType.ACCESS_CONTROL;
            default:
        }

        if (alarmType != Zwave.AlarmType.RESERVED) {
            bIsActive = (getAlarmValue(getAlarmNameByType(alarmType)) == Alarm.ALARM_LEVEL_ON)
                            ? true : false;
        }

        return bIsActive;
    }

    @Override
    public void setActive(boolean bActive, String value) {
        switch (getDeviceType()) {
            case PIR: {
                if (!bActive) {
                    if (getAlarmValue("burglar") != 0) {
                        setAlarmValue("burglar", "0");
                    }
                }
                else {
                    //TODO, now we don't have request for turning status on.
                }
                break;
            }
            default:
        }
    }

    // ---------------------------------------------------------------------------------------------

    public static final Creator<AbstractDevice> CREATOR = new Creator<AbstractDevice>() {
        @Override
        public BaseZwaveDevice createFromParcel(Parcel in) {
            BaseZwaveDevice device = new BaseZwaveDevice(in.readString(), in.readString(), in.readString());
            return device;
        }

        @Override
        public BaseZwaveDevice[] newArray(int size) {
            return new BaseZwaveDevice[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getName());
        dest.writeString(getNodeId());
        dest.writeString(getPropertiesString());
    }

    @Override
    public String getName() {
        String failedString = (getDeviceStatus() == DeviceStatus.Failed) ? FAILED_DEVICE : "";
        String deviceName = "Node " + super.getNodeId() + " - " + super.getName() + failedString;

        return deviceName;
    }

    private Zwave.AlarmType getAlarmTypeByName(String alarmName) {

        Zwave.AlarmType alarmType = Zwave.AlarmType.RESERVED;
        switch (alarmName) {
            case "smoke":
                alarmType = Zwave.AlarmType.SMOKE;
                break;
            case "carbon monoxide":
                alarmType = Zwave.AlarmType.CO;
                break;
            case "access control":
                alarmType = Zwave.AlarmType.ACCESS_CONTROL;
                break;
            case "burglar":
                alarmType = Zwave.AlarmType.BURGLAR;
                break;
            default:
        }

        return alarmType;
    }

    @Override
    public boolean getCapabilityByName(String capabilityName) {

        if (super.getCapabilityByName(capabilityName) || alarmMap.containsKey(capabilityName)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void setCapability(String capabilityName, String capabilityValue) {

        if ( !setAlarmValue(capabilityName, capabilityValue)) {
            super.setCapability(capabilityName, capabilityValue);
        }
    }

    @Override
    public int getAlarmValue(String alarmName) {

        if (alarmName == null) {
            return Alarm.ALARM_NOT_EXIST;
        }

        Alarm alarm = this.alarmMap.get(alarmName);

        if (alarm != null) {
            return alarm.getAlarmLevel();
        } else {
            return Alarm.ALARM_NOT_EXIST;
        }
    }

    private String getAlarmNameByType(Zwave.AlarmType alarmType) {

        String alarmName = null;
        switch (alarmType) {
            case SMOKE:
                alarmName = "smoke";
                break;
            case CO:
                alarmName = "carbon monoxide";
                break;
            case ACCESS_CONTROL:
                alarmName = "access control";
                break;
            case BURGLAR:
                alarmName = "burglar";
                break;
            default:
        }

        return alarmName;
    }

    private Alarm getAlarmFactory(Zwave.AlarmType alarmType) {

        Alarm alarm;
        switch (alarmType) {
            case SMOKE:
                alarm = new SmokeAlarm();
                break;
            case CO:
                alarm = new COAlarm();
                break;
            case ACCESS_CONTROL:
                alarm = new AccessControlAlarm();
                break;
            case BURGLAR:
                alarm = new BurglarAlarm();
                break;
            default:
                Log.e("BaseZwaveDevice", "No Handle alarm Type");
                alarm = new Alarm() {
                    @Override
                    public int getAlarmLevel() {
                        return super.getAlarmLevel();
                    }
                };
        }

        return  alarm;
    }

    private boolean isAutoCloseAlarmType(String alarmName) {
        if (alarmName == null) {
            return false;
        }

        Zwave.AlarmType alarmType = getAlarmTypeByName(alarmName);
        if (alarmType == Zwave.AlarmType.BURGLAR) {
            return  true;
        }

        return false;
    }
}
