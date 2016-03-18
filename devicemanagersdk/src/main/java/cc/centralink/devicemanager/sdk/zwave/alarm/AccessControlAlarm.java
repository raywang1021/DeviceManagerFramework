package cc.centralink.devicemanager.sdk.zwave.alarm;

import android.util.Log;

import cc.centralink.devicemanager.sdk.zwave.Zwave;

/**
 * Created by bipo on 16/1/7.
 */
public class AccessControlAlarm extends Alarm {

    private static final int ZWAVE_ALARM_EVENT_MAGNET_OPEN = 22;
    private static final int ZWAVE_ALARM_EVENT_MAGNET_CLOSE = 23;
    private static final int ZWAVE_ALARM_EVENT_MAGNET_UNKNOWN_EV = 254;

    public AccessControlAlarm() {
        setAlarmType(Zwave.AlarmType.ACCESS_CONTROL);
    }

    @Override
    public int getAlarmLevel() {
        int alarmType = super.getZwavAlarmType();
        if (alarmType == ZWAVE_ALARM_EVENT_MAGNET_OPEN || alarmType == ZWAVE_ALARM_EVENT_MAGNET_UNKNOWN_EV) {
            return ALARM_LEVEL_ON;
        } else if (alarmType == ZWAVE_ALARM_EVENT_MAGNET_CLOSE) {
            return ALARM_LEVEL_OFF;
        } else {
            Log.e("AccessControlAlarm", "UnKnown Alarm Type");
            return ALARM_LEVEL_OFF;
        }
    }
}
