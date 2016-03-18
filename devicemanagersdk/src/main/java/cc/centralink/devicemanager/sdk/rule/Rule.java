package cc.centralink.devicemanager.sdk.rule;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by bipo on 16/1/19.
 */
public class Rule {
    @SerializedName("rule_id")
    public String id;

    @SerializedName("cond")
    public List<RuleCondition> conditions;

    @SerializedName("action")
    public List<RuleAction> actions;
}
