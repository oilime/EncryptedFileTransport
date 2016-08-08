package com.lanan.encrypted_file_transport.Services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lanan.encrypted_file_transport.Main.mainActivity;
import com.lanan.encrypted_file_transport.Utils.appAlive;

public class myBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(appAlive.isAppAlive(context, "com.lanan.fileEncryptedTransport")){
            Intent mainIntent = new Intent(context, mainActivity.class);
            context.startActivity(mainIntent);
        }else {
            Intent launchIntent = context.getPackageManager().
                    getLaunchIntentForPackage("com.lanan.fileEncryptedTransport");
            launchIntent.setFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(launchIntent);
        }
    }
}
