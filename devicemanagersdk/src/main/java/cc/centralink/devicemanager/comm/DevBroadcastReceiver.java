package cc.centralink.devicemanager.comm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by kclin on 3/16/15.
 */
public class DevBroadcastReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        Intent startServiceIntent = new Intent(context, DevCommSrv.class);
        context.startService(startServiceIntent);
    }
}
