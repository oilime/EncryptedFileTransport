package com.lanan.encrypted_file_transport.Services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import com.lanan.encrypted_file_transport.Main.mainActivity;
import com.lanan.encrypted_file_transport.R;
import com.lanan.encrypted_file_transport.Utils.parameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManagerFactory;

public class notificationService extends Service {

	private static int messageNotificationID = 1000;

    private static final String mainPath = parameters.mainPath;
	private static Notification messageNotification = null;
	private static NotificationManager messageNotificationManager = null;

	private RemoteViews customView;
	private SimpleDateFormat newFormat = parameters.newFormat;

    private static SSLServerSocket serverSocket;

	private ExecutorService executorService;
    private Map<Long, FileLog> datas = new HashMap<>();

	final int MY_PORT = 10717;

	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		customView = new RemoteViews(getPackageName(), R.layout.customerview);

        Intent messageIntent = new Intent(this, mainActivity.class);
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

        init();
        Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true){
					Socket socket;
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

    private void init() {
        try {
            //初始化加密密钥库和信任密钥库
            KeyStore serverKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            KeyStore trustedKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try {
                serverKeyStore.load(getBaseContext().getResources().getAssets().open("Server.bks"),
                        parameters.KEY_STORE_PWD.toCharArray());
                trustedKeyStore.load(getBaseContext().getResources().getAssets().open("trustedServer.bks"),
                        parameters.KEY_STORE_PWD.toCharArray());
            }catch (Exception e){
                Log.d("Emilio", e.toString());
            }

            //初始化密钥管理器和信任管理器
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(parameters.KEY_MANAGER);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(parameters.KEY_MANAGER);
            try {
                keyManagerFactory.init(serverKeyStore, parameters.KEY_STORE_PWD.toCharArray());
                trustManagerFactory.init(trustedKeyStore);
            }catch (Exception e){
                e.printStackTrace();
            }

            //初始化SSL上下文
            SSLContext ctx = SSLContext.getInstance("TLS");
            try {
                ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
            }catch (Exception e){
                e.printStackTrace();
            }

            //初始化server socket
            try {
                serverSocket = (SSLServerSocket)ctx.getServerSocketFactory().createServerSocket(MY_PORT);
                serverSocket.setSoTimeout(0);
            }catch (Exception e){
                e.printStackTrace();
            }

            //初始化线程池
            executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 50);
        }catch (Exception e){
            e.printStackTrace();
        }
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
					String fileLength = items[1].substring(items[1].indexOf("=") + 1);
					String filename = items[2].substring(items[2].indexOf("=") + 1);
					String sourceId = items[3].substring(items[3].indexOf("=") + 1);
					String recvName = "";

					for(int i = 0; i < mainActivity.dataList.size(); i++){
						for (int j = 0; j < mainActivity.dataList.get(i).size(); j++){
							String testIp = (String) mainActivity.dataList.get(i).get(j).get("ip");
							if(testIp.equals(hostip)){
								recvName = (String) mainActivity.dataList.get(i).get(j).get("name");
								break;
							}
						}
					}

					Long id = System.currentTimeMillis();
					FileLog log = null;
					if (!"".equals(sourceId)) {
						id = Long.valueOf(sourceId);
						log = find(id);
					}
					File file = null;
					int position = 0;
					if(log == null){
						String appHome = "";
						if (filename.endsWith(".pro")){
							appHome = mainPath + "/.config/";
						} else if(!recvName.isEmpty()){
							appHome = mainPath + "/FileTransport/" + recvName;
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
							fileOutStream.setLength(Integer.valueOf(fileLength));
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
							Log.d("Emilio", "接收到配置文件");
						}else {
                            Intent intent = new Intent();
                            Bundle bundle = new Bundle();
                            bundle.putString("recvdate", recvdate);
                            bundle.putString("recvfilename", file.getAbsolutePath());
                            bundle.putString("hostip", hostip);

							intent.putExtras(bundle);
							intent.setAction(mainActivity.RECVMSG);
							mainActivity.local.sendBroadcast(intent);

                            customView.setTextViewText(R.id.when, newFormat.format(curDate));
                            recvName = "未知用户";
                            for(int i = 0; i < mainActivity.dataList.size(); i++){
                                for (int j = 0; j < mainActivity.dataList.get(i).size(); j++){
                                    String testIp = (String) mainActivity.dataList.get(i).get(j).get("ip");
                                    if(testIp.equals(hostip)){
                                        recvName = (String) mainActivity.dataList.get(i).get(j).get("name");
                                        break;
                                    }
                                }
                            }
                            customView.setTextViewText(R.id.contentText, "您收到了来自"+recvName+"的新文件");
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
		try {
            File file = new File(filePath);
			if (!file.exists()) {
                boolean recvFlag = file.mkdir();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}