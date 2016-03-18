package cc.centralink.devicemanager.sdk;

import com.google.gson.annotations.SerializedName;

/**
 * Created by davidliu on 6/3/15.
 */
public class DeviceStateNotification implements Notification {
    @SerializedName("from")
    public String from;

    @SerializedName("node_id")
    public String nodeId;

    @SerializedName("val_id")
    public String key;

    @SerializedName("value")
    public String value;

    @SerializedName("nodeCapability")
    public String capability;

    @SerializedName("nodeName")
    public String name;


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("DeviceStateNotification {");
        sb.append("from: ").append(from)
                .append(", nodeId: ").append(nodeId)
                .append(", key: ").append(key)
                .append(", value: ").append(value)
                .append(", nodeCapability: ").append(capability)
                .append(", nodeName: ").append(name).append("}");
        return sb.toString();
    }
}
