package com.lanan.encrypted_file_transport.service;

/**
 * Created by lanan on 16-5-12.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lanan.encrypted_file_transport.Main.MainActivity;
import com.lanan.encrypted_file_transport.utils.Appalive;


public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Appalive.isAppAlive(context, "com.lanan.fileEncryptedTransport")){
            Intent mainIntent = new Intent(context, MainActivity.class);
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
