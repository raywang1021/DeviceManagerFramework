package cc.centralink.devicemanager.sdk;

/**
 * Created by bipo on 15/12/10.
 */
public class ControllerStateNotification implements Notification {

    public static final String STATE_DEVICE_ADDED = "state_device_added";
    public static final String STATE_DEVICE_REMOVED = "state_device_removed";
    public static final String STATE_DEVICE_SLEEP = "state_device_sleep";
    public static final String STATE_CONTROLLER_ERROR = "state_controller_error";

    private String mState;

    public ControllerStateNotification(String state) {
        this.mState = state;
    }

    public String getState() {
        return mState;
    }
}
