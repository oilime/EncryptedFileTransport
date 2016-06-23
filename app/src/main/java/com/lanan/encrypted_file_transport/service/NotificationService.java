package com.lanan.encrypted_file_transport.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;

import com.lanan.encrypted_file_transport.Main.MainActivity;
import com.lanan.encrypted_file_transport.R;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NotificationService extends Service {

	private IntentFilter noticefilter;

	private Intent messageIntent = null;
	private PendingIntent messagePendingIntent = null;

	// 通知栏消息
	private int messageNotificationID = 1000;
	private Notification messageNotification = null;
	private NotificationManager messageNotificatioManager = null;
	private Notification.Builder builder;
	private RemoteViews customView;
	private SimpleDateFormat newFormat = new SimpleDateFormat("HH:mm:ss");
	private Date curDate;
	public static String SHOWPUSH = "com.lanan.showpush";
    private String recvip;
    private String recvname;

	public static LocalBroadcastManager shownotice;
	public BroadcastReceiver bReceiver;

	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		noticefilter = new IntentFilter();
		noticefilter.addAction(SHOWPUSH);

		if (shownotice == null){
			shownotice = LocalBroadcastManager.getInstance(this);
			Log.d("Emilio", "推送服务注册成功");
			if (bReceiver == null){
				bReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						if (intent.getAction().equals(SHOWPUSH)){
							Log.d("Emilio", "收到推送通知");
							curDate = new Date(System.currentTimeMillis());
							customView.setTextViewText(R.id.when, newFormat.format(curDate));
                            recvip = intent.getStringExtra("hostip");
                            recvname = "未知用户";
                            for(int i = 0; i < MainActivity.dataList.size(); i++){
                                for (int j = 0;j < MainActivity.dataList.get(i).size();j++){
                                    String testip = (String)MainActivity.dataList.get(i).get(j).get("ip");
                                    if(testip.equals(recvip)){
                                        recvname = (String)MainActivity.dataList.get(i).get(j).get("name");
                                        break;
                                    }
                                }
                            }
                            customView.setTextViewText(R.id.contentText, "您收到了来自"+recvname+"的新文件");
                            messageNotificatioManager.notify(messageNotificationID,
									messageNotification);
							messageNotificationID++;
						}
					}
				};
                shownotice.registerReceiver(bReceiver, noticefilter);
			}
		}

		customView = new RemoteViews(getPackageName(), R.layout.customerview);

        messageIntent = new Intent(this, MainActivity.class);
        messageIntent.putExtra("tarname", recvname);
        messageIntent.putExtra("ip", recvip);
        messagePendingIntent = PendingIntent.getActivity(this, messageNotificationID - 1,
                messageIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder = new Notification.Builder(getBaseContext())
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
		messageNotificatioManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		return super.onStartCommand(intent, flags, startId);
	}
}