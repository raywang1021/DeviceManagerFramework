package cc.centralink.devicemanager.sdk.zwave;

import android.os.Bundle;
import android.os.Message;

import cc.centralink.devicemanager.sdk.AbstractDevice;
import cc.centralink.devicemanager.sdk.DeviceManager;

/**
 * Created by davidliu on 8/10/15.
 */
public class Zwave {

    public enum AlarmType {
        RESERVED,
        SMOKE,
        CO,
        CO2,
        HEAT,
        WATER,
        ACCESS_CONTROL,
        BURGLAR,
        POWER_MANAGEMENT,
        SYSTEM,
        EMERGENCY,
        CLOCK,
        FIRST
    };


    public static Command parse(String zwaveString) {
        return null;
    }

    public static class AlarmV1 {
        public static void alarmGet() {

        }
        public static void alarmReport() {

        }
    }

    public static class AlarmV2 {
        public static void alarmSet() {

        }
        public static void alarmGet() {

        }
        public static void alarmReport() {

        }
        public static void alarmTypeSupportedGet() {

        }
        public static void alarmTypeSupportedReport() {

        }
    }

    /**
     * Refer to Notification Command Class V3
     */
    public static class AlarmV3 {
    }

    public static class BatteryV1 {
        public static void batteryLevelGet() {

        }
        public static void batteryLevelReport() {

        }
    }

    @Deprecated
    public static class SensorBinaryV1 {

    }

    @Deprecated
    public static class SensorBinaryV2 {

    }

    public static class SwitchBinaryV1 {
        public static void switchBinarySet(boolean on, AbstractDevice device) {
            Message msg = Message.obtain();
            Bundle b = new Bundle();
            b.putString("cmd", "setval");
            if (on) {
                b.putString("nodeval", "255");
            } else {
                b.putString("nodeval", "0");
            }
            b.putString("nodeid", device.getNodeId());
            msg.setData(b);
            DeviceManager.get().sendDeviceCommand(msg);
        }
        public static void switchBinaryGet() {
        }
        public static void switchBinaryReport() {
        }
    }

    public static class SwitchBinaryV2 extends SwitchBinaryV1 {

        public static void switchBinarySet(boolean on, AbstractDevice device) {
            Message msg = Message.obtain();
            Bundle b = new Bundle();
            b.putString("cmd", "setval");
            if (on) {
                b.putString("nodeval", "255");
            } else {
                b.putString("nodeval", "0");
            }
            b.putString("nodeid", device.getNodeId());
            msg.setData(b);
            DeviceManager.get().sendDeviceCommand(msg);
        }

        public static void switchBinaryReport() {
        }
    }

    public static class SwitchLevelV1 {
        public static void switchLevelSet(int level, AbstractDevice device) {

        }

        public static void switchLevelGet() {

        }

        public static void switchLevelReport() {

        }

        public static void switchLevetStartLevelChange(int level, AbstractDevice device) {

        }

        public static void switchLevelStopLevelChange(int level, AbstractDevice device) {

        }
    }

    public static class SwitchLevelV2 {
        public static void switchLevelSet(int level, AbstractDevice device) {

        }

        public static void switchLevelGet() {

        }

        public static void switchLevelReport() {

        }

        public static void switchLevetStartLevelChange(int level, AbstractDevice device) {

        }

        public static void switchLevelStopLevelChange(int level, AbstractDevice device) {

        }
    }

    public static class SwitchLevelV3 {
        public static void switchLevelSet(int level, AbstractDevice device) {

        }

        public static void switchLevelGet() {

        }

        public static void switchLevelReport() {

        }

        public static void switchLevetStartLevelChange(int level, AbstractDevice device) {

        }

        public static void switchLevelStopLevelChange(int level, AbstractDevice device) {

        }

        public static void switchLevelSupportedGet() {

        }

        public static void switchLevelSupportedReport() {

        }
    }

    public static class SwitchLevelV4 {
        public static void switchLevelSet(int level, AbstractDevice device) {
            Message msg = Message.obtain();
            Bundle b = new Bundle();
            b.putString("cmd", "setval");
            b.putString("nodeval", String.valueOf(level));
            b.putString("nodeid", device.getNodeId());
            msg.setData(b);
            DeviceManager.get().sendDeviceCommand(msg);
        }

        public static void switchLevelGet() {

        }

        public static void switchLevelReport() {

        }

        public static void switchLevetStartLevelChange(int level, AbstractDevice device) {

        }

        public static void switchLevelStopLevelChange(int level, AbstractDevice device) {

        }
    }

    public static class DoorLockV1 {
        public enum DoorLockMode {
            DOOR_UNSECURED(0),
            DOOR_UNSECURED_WITH_TIMEOUT(1),
            DOOR_UNSECURED_FOR_INSIDE_DOOR_HANDLE(16),
            DOOR_UNSECURED_FOR_INSIDE_DOOR_HANDLE_WITH_TIMEOUT(17),
            DOOR_UNSECURED_FOR_OUTSIDE_DOOR_HANDLE(32),
            DOOR_UNSECURED_FOR_OUTSIDE_DOOR_HANDLE_WITH_TIMEOUT(13),
            DOOR_SECURED(255);

            int mode;
            DoorLockMode(int mode) {
                this.mode = mode;
            }
        }
        public static void doorLockOperationSet(DoorLockMode lockMode, AbstractDevice device) {
            Message msg = Message.obtain();
            Bundle b = new Bundle();
            b.putString("cmd", "setval");
            b.putString("nodeval", String.valueOf(lockMode.mode));
            b.putString("nodeid", device.getNodeId());
            msg.setData(b);
            DeviceManager.get().sendDeviceCommand(msg);
        }
        public static void doorLockOperationGet() {

        }
        public static void doorLockOperationReport() {

        }
        public static void doorLockConfigurationSet() {

        }
        public static void doorLockConfigurationGet() {

        }
        public static void doorLockConfigurationReport() {

        }
    }

    public static class DoorLockV2 extends DoorLockV1 {

    }

    public static class DoorLockV3 extends DoorLockV2 {
        public static void doorLockOperationReport() {

        }
    }
}
