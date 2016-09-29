package com.lanan.filetransport.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.lanan.filetransport.R;
import com.lanan.filetransport.main.MainActivity;
import com.lanan.filetransport.utils.Jni;

import java.util.ArrayList;
import java.util.Map;

public class NotificationService extends Service {

	private static int messageNotificationID = 1000;

	private static Notification messageNotification = null;
	private static NotificationManager messageNotificationManager = null;

	private RemoteViews customView;
    private ArrayList<Map<String, Object>> dataList = new ArrayList<>();

    private Jni server;
    private handleThread handle;
    private int ret;

	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, final int flags, final int startId) {

		customView = new RemoteViews(getPackageName(), R.layout.customerview);

        Intent messageIntent = new Intent(this, MainActivity.class);
        PendingIntent messagePendingIntent = PendingIntent.getActivity(this, messageNotificationID - 1,
				messageIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(getBaseContext())
				.setSmallIcon(R.drawable.sendbutton)
				.setTicker("您收到新的文件")
				.setContent(customView)
				.setDefaults(Notification.DEFAULT_SOUND)
				.setDefaults(Notification.DEFAULT_VIBRATE)
				.setContentIntent(messagePendingIntent)
				.setSound(RingtoneManager.getActualDefaultRingtoneUri(getBaseContext(),
						RingtoneManager.TYPE_NOTIFICATION))
				.setAutoCancel(true);

		messageNotification = builder.build();
		messageNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        server = new Jni(dataList);

        Thread recv_thread = new Thread(new Runnable() {
			@Override
			public void run() {
                ret = server.server_set_socket(1223,
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/rsacert/server.pem",
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/rsacert/rootca.pem");
                switch (ret) {
                    case 0:
                        Log.d("Emilio", "未知原因失败");
                        break;
                    case 1:
                        Log.d("Emilio", "创建ctx失败");
                        break;
                    case 2:
                        Log.d("Emilio", "加载ca证书失败");
                        break;
                    case 3:
                        Log.d("Emilio", "加载服务器证书失败");
                        break;
                    case 4:
                        Log.d("Emilio", "加载服务器私钥失败");
                        break;
                    case 5:
                        Log.d("Emilio", "验证服务器证书失败");
                        break;
                    case 6:
                        Log.d("Emilio", "创建socket失败");
                        break;
                    case 7:
                        Log.d("Emilio", "socket绑定失败");
                        break;
                    case 8:
                        Log.d("Emilio", "socket监听失败");
                        break;
                    case 9:
                        Log.d("Emilio", "创建文件夹失败");
                        break;
                    default:
                        break;
                }

			}
		});
		recv_thread.start();

        handle = new handleThread(server);
        handle.start();

		return super.onStartCommand(intent, flags, startId);
	}

    private class handleThread extends Thread {
        private boolean isStop = false;
        private Jni server;

        handleThread (Jni server) {
            this.server = server;
        }

        void setStop (boolean flag) {
            this.isStop = flag;
        }

        @Override
        public void run () {
            int i = 0;
            while (!isStop) {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (server.getLength() > i) {
                    for (; i < server.getLength(); i++) {
                        String recvdate = (String) server.getList().get(i).get("recvdate");
                        String filepath = (String) server.getList().get(i).get("recvfilename");
                        String hostip = (String) server.getList().get(i).get("hostip");
                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("recvdate", recvdate);
                        bundle.putString("recvfilename", filepath);
                        bundle.putString("hostip", hostip);

                        intent.putExtras(bundle);
                        intent.setAction(MainActivity.RECVMSG);
                        MainActivity.local.sendBroadcast(intent);

                        customView.setTextViewText(R.id.when, recvdate);
                        String recvName = "未知用户";
                        for(int n = 0; n < MainActivity.dataList.size(); n++){
                            for (int j = 0; j < MainActivity.dataList.get(n).size(); j++){
                                String testIp = (String) MainActivity.dataList.get(n).get(j).get("ip");
                                if(testIp.equals(hostip)){
                                    recvName = (String) MainActivity.dataList.get(n).get(j).get("name");
                                    break;
                                }
                            }
                        }
                        customView.setTextViewText(R.id.contentText, "您收到了来自"+recvName+"的新文件");
                        messageNotificationManager.notify(messageNotificationID,
                                messageNotification);
                        messageNotificationID++;
                    }
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        if (server != null && handle != null && handle.isAlive()) {
            handle.setStop(true);
            server.set_stop();
            handle = null;
            Log.d("Emilio", "服务器关闭");
        }
    }
}