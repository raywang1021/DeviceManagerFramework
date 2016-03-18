package cc.centralink.devicemanager.sdk.rule;

import com.google.gson.annotations.SerializedName;

/**
 * Created by bipo on 16/1/19.
 */
public class RuleAction {
    @SerializedName("controller_id")
    public String controllerId;

    @SerializedName("node_id")
    public String nodeId;

    @SerializedName("name")
    public String nodePropertyName;

    @SerializedName("value")
    public String nodePropertyValue;
}
