package cc.centralink.devicemanager.sdk;

import android.os.Message;
import android.os.Parcelable;

import java.util.HashMap;

/**
 * Created by davidliu on 4/21/15.
 */
public abstract class AbstractDevice implements Parcelable {

    public enum DeviceType {
        RESERVED,
        SWITCH,
        DOORLOCK,
        SENSOR,
        PIR,
        CO,
        SMOKE,
        MAGNET
    }
    private String name;
    private String nodeId;
    private String propertiesString;
    protected HashMap<String, String> propertiesMap;
    private DeviceType m_deviceType;

    public AbstractDevice(String name, String nodeId) {
        this.name = name;
        this.nodeId = nodeId;
        this.m_deviceType = DeviceType.RESERVED;
    }

    public AbstractDevice(String name, String nodeId, String propertiesString) {
        this.name = name;
        this.nodeId = nodeId;
        this.propertiesString = propertiesString;
        this.m_deviceType = DeviceType.RESERVED;
        parse(propertiesString);
    }

    public String getName() {
        return name;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getPropertiesString() {
        return propertiesString;
    }

    public HashMap<String, String> getPropertiesMap() {
        return propertiesMap;
    }

    public boolean getCapabilityByName(String capabilityName) {

        if (propertiesMap.containsKey(capabilityName)) {
            return true;
        } else {
            return false;
        }
    }

    public void setCapability(String capabilityName, String capabilityValue) {
        propertiesMap.put(capabilityName, capabilityValue);
    }

    public boolean isNeedTurnOffAlarm(String alarmName) { return false; }
    public int getAlarmValue(String alarmName) { return -1; }
    public DeviceType getDeviceType() { return m_deviceType; }
    public void setDeviceType(DeviceType type) { m_deviceType = type; }

    /**
     * Must implement this method to extract properties from @param{propertiesString}
     * @param propertiesString
     */
    public abstract void parse(String propertiesString);

    /**
     * Format the command message used to send to low-level controller
     */
    public abstract Message formatCommandMessage();

    public abstract boolean isActive();

    public abstract void setActive(boolean bActive, String value);
}