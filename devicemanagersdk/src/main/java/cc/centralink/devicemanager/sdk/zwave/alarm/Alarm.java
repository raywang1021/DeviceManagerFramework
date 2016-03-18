package cc.centralink.devicemanager.sdk.zwave.alarm;

import cc.centralink.devicemanager.sdk.zwave.Zwave;

/**
 * Created by bipo on 16/1/7.
 */
public abstract class Alarm {

    public static final int ALARM_LEVEL_ON = 255;
    public static final int ALARM_LEVEL_OFF = 0;
    public static final int ALARM_NOT_EXIST = -1;
    private String alarmName = null;
    private Zwave.AlarmType alarmType = Zwave.AlarmType.RESERVED;
    private int alarmLevel = ALARM_LEVEL_OFF;
    private int zwavAlarmType = 0;

    public Alarm() {
        alarmType = Zwave.AlarmType.RESERVED;
    }

    public void setAlarmType(Zwave.AlarmType alarmType) {
        this.alarmType = alarmType;
    }
    public int getAlarmLevel() {
        return alarmLevel;
    }
    public int getZwavAlarmType() {
        return zwavAlarmType;
    }
    public void setZwavAlarmType(int zwavAlarmType) {
        this.zwavAlarmType = zwavAlarmType;
    }
}

