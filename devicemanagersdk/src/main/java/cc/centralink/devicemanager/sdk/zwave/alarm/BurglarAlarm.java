package cc.centralink.devicemanager.sdk.zwave.alarm;

import cc.centralink.devicemanager.sdk.zwave.Zwave;

/**
 * Created by bipo on 16/1/8.
 */
public class BurglarAlarm  extends Alarm {

    private static final int ZWAVE_ALARM_EVENT_BUGLAR = 3;
    private static final int ZWAVE_ALARM_EVENT_MOTION = 8;

    public BurglarAlarm() {
        setAlarmType(Zwave.AlarmType.BURGLAR);
    }

    @Override
    public int getAlarmLevel() {
        int alarmType = super.getZwavAlarmType();
        if (alarmType == ZWAVE_ALARM_EVENT_BUGLAR ||
                alarmType == ZWAVE_ALARM_EVENT_MOTION) {
            return ALARM_LEVEL_ON;
        } else {
            return ALARM_LEVEL_OFF;
        }
    }
}
