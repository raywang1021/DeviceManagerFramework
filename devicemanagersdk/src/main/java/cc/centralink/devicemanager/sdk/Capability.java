package cc.centralink.devicemanager.sdk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cc.centralink.devicemanager.sdk.zwave.BaseZwaveDevice;
import cc.centralink.devicemanager.sdk.zwave.Zwave;

/**
 * Created by davidliu on 9/4/15.
 */
public final class Capability {
    public static final String ALARM = "capability_alarm";
    public static final String Battery = "capability_battery";
    public static final String SWITCH = "capability_switch";
    public static final String SWITCH_LEVEL = "capability_switch_level";
    public static final String DOORLOCK = "capability_door_lock";

    /**
     * Measurement
     */
    public static final String TEMPERATURE_MEASUREMENT = "capability_temperature_measurement";
    public static final String HUMIDITY_MEASUREMENT = "capability_humidity_measurement";

    public static List<String> fromDevice(AbstractDevice device) {
        List<String> capabilities = new ArrayList<String>();
        Map propertyMap = device.getPropertiesMap();
        if (propertyMap.containsKey("switch")) {
            capabilities.add(SWITCH);
        }
        if (propertyMap.containsKey("level")) {
            capabilities.add(SWITCH_LEVEL);
        }
        if (propertyMap.containsKey("locked")) {
            capabilities.add(DOORLOCK);
        }
        if (propertyMap.containsKey("temperature")) {
            capabilities.add(TEMPERATURE_MEASUREMENT);
        }
        return capabilities;
    }

    // ---------------------------------------------------------------------------------------------

    public static class SwitchCapDecor {
        private static final String KEY_SWITCH = "switch";
        private AbstractDevice device;
        private boolean on;

        public SwitchCapDecor(AbstractDevice device) {
            this.device = device;
            String switchStr = device.getPropertiesMap().get(KEY_SWITCH);
            on = (switchStr != null && switchStr.equalsIgnoreCase("true")) ? true : false;
        }

        public synchronized void on() {
            on = true;
            String switchStr = device.getPropertiesMap().get(KEY_SWITCH);
            if (switchStr != null) {
                device.getPropertiesMap().put(KEY_SWITCH, "true");
                if (device instanceof BaseZwaveDevice) {
                    Zwave.SwitchBinaryV2.switchBinarySet(true, device);
                }
            }
        }

        public synchronized void off() {
            on = false;
            String switchStr = device.getPropertiesMap().get(KEY_SWITCH);
            if (switchStr != null) {
                device.getPropertiesMap().put(KEY_SWITCH, "false");
                if (device instanceof BaseZwaveDevice) {
                    Zwave.SwitchBinaryV2.switchBinarySet(false, device);
                }
            }
        }

        public synchronized boolean isOn() {
            return on;
        }

        public static boolean isCapable(AbstractDevice device) {
            String switchStr = device.getPropertiesMap().get(KEY_SWITCH);
            return (switchStr != null) ? true : false;
        }
    }

    public static class SwitchLevelCapDecor {
        private static final String KEY_SWITCH_LEVEL = "level";
        private AbstractDevice device;
        private int level;

        public SwitchLevelCapDecor(AbstractDevice device) {
            this.device = device;
            String levelStr = device.getPropertiesMap().get(KEY_SWITCH_LEVEL);
            level = (levelStr != null) ? Integer.valueOf(levelStr).intValue() : 0;
        }

        public synchronized void setLevel(int level) {
            this.level = level;
            String levelStr = device.getPropertiesMap().get(KEY_SWITCH_LEVEL);
            if (levelStr != null) {
                device.getPropertiesMap().put(KEY_SWITCH_LEVEL, String.valueOf(level));
                if (device instanceof BaseZwaveDevice) {
                    Zwave.SwitchLevelV4.switchLevelSet(level, device);
                }
            }
        }

        public synchronized int getLevel() {
            return this.level;
        }

        public static boolean isCapable(AbstractDevice device) {
            return (device.getPropertiesMap().get(KEY_SWITCH_LEVEL) != null) ? true : false;
        }
    }

    public static class DoorLockCapDecor {
        private static final String KEY_LOCKED = "locked";
        private AbstractDevice device;
        private boolean locked;

        public DoorLockCapDecor(AbstractDevice device) {
            this.device = device;
            String lockedStr = device.getPropertiesMap().get(KEY_LOCKED);
            locked = (lockedStr != null && lockedStr.equalsIgnoreCase("true")) ? true : false;
        }

        public synchronized void lock() {
            locked = true;
            String lockedStr = device.getPropertiesMap().get(KEY_LOCKED);
            if (lockedStr != null) {
                device.getPropertiesMap().put(KEY_LOCKED, "true");
                if (device instanceof BaseZwaveDevice) {
                    Zwave.DoorLockV3.doorLockOperationSet(Zwave.DoorLockV1.DoorLockMode.DOOR_SECURED, device);
                }
            }
        }

        public synchronized void unlock() {
            locked = false;
            String lockedStr = device.getPropertiesMap().get(KEY_LOCKED);
            if (lockedStr != null) {
                device.getPropertiesMap().put(KEY_LOCKED, "false");
                if (device instanceof BaseZwaveDevice) {
                    Zwave.DoorLockV3.doorLockOperationSet(Zwave.DoorLockV1.DoorLockMode.DOOR_UNSECURED, device);
                }
            }
        }

        public synchronized boolean isLocked() {
            return locked;
        }

        public static boolean isCapable(AbstractDevice device) {
            return (device.getPropertiesMap().get(KEY_LOCKED) != null) ? true : false;
        }
    }
}
