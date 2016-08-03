package com.lanan.encrypted_file_transport.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.RemoteViews;

import com.lanan.encrypted_file_transport.Main.MainActivity;
import com.lanan.encrypted_file_transport.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationService extends Service {

	private static int messageNotificationID = 1000;
	private static Notification messageNotification = null;
	private static NotificationManager messageNotificationManager = null;
	private RemoteViews customView;
	private SimpleDateFormat newFormat = new SimpleDateFormat("HH:mm:ss");

	private static ServerSocket serverSocket;

	private static final String mainPath = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/alan/system/security/local/tmp/chs";
    private static final String ALLCHAR = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

	private ExecutorService executorService;
    private Map<Long, FileLog> datas = new HashMap<>();

	final int MY_PORT = 12023;

	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		try {
			serverSocket = new ServerSocket(MY_PORT,100);
			serverSocket.setSoTimeout(0);
		} catch (IOException e) {
			e.printStackTrace();
		}

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

		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 50);

		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true){
					Socket socket = null;
					try {
						socket = serverSocket.accept();
						executorService.execute(new SocketTask(socket));
					} catch (Exception e){
						e.printStackTrace();
					}
				}
			}
		});
		thread.start();

		return super.onStartCommand(intent, flags, startId);
	}

	private class SocketTask implements Runnable {
		private Socket socket;

		public SocketTask(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			try {
				InputStream input = socket.getInputStream();
				BufferedReader in = new BufferedReader(new InputStreamReader(input));
				String head = in.readLine();

				System.out.println(head);
				if (head != null) {
					Log.d("Emilio", "头文件:" + head);
					String[] items = head.split(";");
					String hostip = items[0].substring(items[0].indexOf("=") + 1);
					String filelength = items[1].substring(items[1].indexOf("=") + 1);
					String filename = items[2].substring(items[2].indexOf("=") + 1);
					String sourceid = items[3].substring(items[3].indexOf("=") + 1);
					String recvname = "";

					for(int i = 0; i < MainActivity.dataList.size(); i++){
						for (int j = 0; j < MainActivity.dataList.get(i).size(); j++){
							String testip = (String)MainActivity.dataList.get(i).get(j).get("ip");
							if(testip.equals(hostip)){
								recvname = (String)MainActivity.dataList.get(i).get(j).get("name");
								break;
							}
						}
					}

					Long id = System.currentTimeMillis();
					FileLog log = null;
					if (!"".equals(sourceid)) {
						id = Long.valueOf(sourceid);
						log = find(id);
					}
					File file = null;
					int position = 0;
					if(log == null){
						String appHome = "";
						if (filename.endsWith(".pro")){
							appHome = mainPath + "/.config/";
						} else if(!recvname.isEmpty()){
							appHome = mainPath + "/FileTransport/" + recvname;
						} else {
							appHome = mainPath + "/FileTransport/unknown_user";
						}
						createDir(appHome);
						File dir = new File(appHome);
						file = new File(dir, filename);
						if(file.exists() && !filename.endsWith(".pro")){
							filename = filename.substring(0, filename.indexOf("."))
									+ dir.listFiles().length+ filename.substring(filename.indexOf("."));
							file = new File(dir, filename);
						}
						save(id, file);
					}else{
						file = new File(log.getPath());
						if(file.exists()){
							File logFile = new File(file.getParentFile(), file.getName()+".log");
							if(logFile.exists()) {
								Properties properties = new Properties();
								properties.load(new FileInputStream(logFile));
								position = Integer.valueOf(properties.getProperty("length"));//读取断点位置
							}
						}
					}

					Thread.sleep(500);
					OutputStream outStream = socket.getOutputStream();
					Thread.sleep(500);
					String response = "sourceid="+ id+ ";position="+ position+ "\r\n";
					Thread.sleep(500);
					outStream.write(response.getBytes());

					byte[] buffer = new byte[4096];
					int len = -1;
					int length = position;

					try{
						RandomAccessFile fileOutStream = new RandomAccessFile(file, "rwd");
						if(position==0)
							fileOutStream.setLength(Integer.valueOf(filelength));
						fileOutStream.seek(position);
						while ((len = input.read(buffer)) != -1){
							fileOutStream.write(buffer, 0, len);
							length += len;
						}
						delete(id);
						fileOutStream.close();
						in.close();
						input.close();
						Log.d("Emilio", "文件接收完成");

						Date curDate = new Date(System.currentTimeMillis());
						String recvdate	= newFormat.format(curDate);

						if (filename.endsWith(".pro")){
                            ContentValues cv = new ContentValues();
                            cv.put("filename", file.getName());
                            cv.put("path", file.getAbsolutePath());
                            cv.put("time", recvdate);
                            cv.put("randId", getRandomId(32));
                            cv.put("mode", "recv");
                            cv.put("type", 1);

                            MainActivity.sqLiteDatabase.insert("TransportHistory", null, cv);
							Log.d("Emilio", "接收到配置文件");
						}else {
                            ContentValues cv = new ContentValues();
                            cv.put("filename", file.getName());
                            cv.put("path", file.getAbsolutePath());
                            cv.put("time", recvdate);
                            cv.put("randId", getRandomId(32));
                            cv.put("mode", "recv");
							if (false){
								cv.put("type", 2);
								Log.d("Emilio", "接收到消息");
							}else {
								cv.put("type", 0);
								Log.d("Emilio", "接收到普通文件");
							}

                            MainActivity.sqLiteDatabase.insert("TransportHistory", null, cv);

                            Intent intent = new Intent();
                            Bundle bundle = new Bundle();
                            bundle.putString("recvdate", recvdate);
                            bundle.putString("recvfilename", file.getAbsolutePath());
                            bundle.putString("hostip", hostip);

							intent.putExtras(bundle);
							intent.setAction(MainActivity.RECVMSG);
							MainActivity.local.sendBroadcast(intent);

                            customView.setTextViewText(R.id.when, newFormat.format(curDate));
                            recvname = "未知用户";
                            for(int i = 0; i < MainActivity.dataList.size(); i++){
                                for (int j = 0;j < MainActivity.dataList.get(i).size();j++){
                                    String testip = (String)MainActivity.dataList.get(i).get(j).get("ip");
                                    if(testip.equals(hostip)){
                                        recvname = (String)MainActivity.dataList.get(i).get(j).get("name");
                                        break;
                                    }
                                }
                            }
                            customView.setTextViewText(R.id.contentText, "您收到了来自"+recvname+"的新文件");
                            messageNotificationManager.notify(messageNotificationID,
                                    messageNotification);
                            messageNotificationID++;
						}
					}catch(IOException e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if(socket != null && !socket.isClosed())
						socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public FileLog find(Long sourceid) {
		return datas.get(sourceid);
	}

	public void save(Long id, File saveFile) {
		datas.put(id, new FileLog(id, saveFile.getAbsolutePath()));
	}

	public void delete(long sourceid) {
		if (datas.containsKey(sourceid))
			datas.remove(sourceid);
	}

	private class FileLog {
		private Long id;
		private String path;

		public FileLog(Long id, String path) {
			super();
			this.id = id;
			this.path = path;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getPath() {
			return path;
		}
	}

	public static void createDir(String filePath) {
		File file = null;
		try {
			file = new File(filePath);
			if (!file.exists()) {
				file.mkdir();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    public static String getRandomId(int length) {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(ALLCHAR.charAt(random.nextInt(ALLCHAR.length())));
        }
        return sb.toString();
    }
}