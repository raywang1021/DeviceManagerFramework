package cc.centralink.devicemanager.sdk.zwave.alarm;

import cc.centralink.devicemanager.sdk.zwave.Zwave;

/**
 * Created by bipo on 16/1/8.
 */
public class COAlarm extends Alarm {

    private static final int ZWAVE_ALARM_EVENT_CO = 1;
    private static final int ZWAVE_ALARM_EVENT_CO_UNKNOWN_LOCATION = 2;

    public COAlarm() {
        setAlarmType(Zwave.AlarmType.CO);
    }

    @Override
    public int getAlarmLevel() {
        int alarmType = super.getZwavAlarmType();
        if (alarmType == ZWAVE_ALARM_EVENT_CO ||
                alarmType == ZWAVE_ALARM_EVENT_CO_UNKNOWN_LOCATION) {
            return ALARM_LEVEL_ON;
        } else {
            return ALARM_LEVEL_OFF;
        }
    }
}
