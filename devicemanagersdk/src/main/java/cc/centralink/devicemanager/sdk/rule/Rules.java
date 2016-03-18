package cc.centralink.devicemanager.sdk.rule;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by bipo on 16/1/19.
 */
public class Rules {
    @SerializedName("rule")
    public List<Rule> ruleList = new ArrayList();
}

