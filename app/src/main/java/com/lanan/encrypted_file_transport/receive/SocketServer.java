package com.lanan.encrypted_file_transport.receive;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.lanan.encrypted_file_transport.Main.MainActivity;
import com.lanan.encrypted_file_transport.service.NotificationService;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketServer{
	private ExecutorService executorService;// 线程池
	private ServerSocket ss = null;
	private Map<Long, FileLog> datas = new HashMap<Long, FileLog>();
	private static final String mainPath = "/sdcard/alan/system/security/local/tmp/chs";
//	private static final String mainPath = "/sdcard";

	private SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public SocketServer(ServerSocket ss1) {
		ss=ss1;
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
				.availableProcessors() * 50);
	}

	// 启动服务
	public void start() throws Exception {
		Thread thread1=new Thread(new Runnable()  
        {  
            @Override  
            public void run()  
            {  
            	while (true) {
        			Socket socket = null;
					try {
						socket = ss.accept();
					} catch (IOException e) {
						e.printStackTrace();
						Log.d("Emilio", "cant accept");
					}
        			executorService.execute(new SocketTask(socket));// 启动一个线程来处理请求
        		}
            }
        });  
        thread1.start();  
	}

	public static void createDir(String filePath) {
		File file = null;
		try {
			file = new File(filePath);
			if (!file.exists()) {
				file.mkdir();
			}
		} catch (Exception e) {
			Log.i("error:", e+"");
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
				// 得到客户端发来的第一行协议数据：Content-Length=143253434;filename=xxx.3gp;sourceid=
				// 如果用户初次上传文件，sourceid的值为空。
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
					String recvname = null;

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
					if (null != sourceid && !"".equals(sourceid)) {
						id = Long.valueOf(sourceid);
						log = find(id);//查找上传的文件是否存在上传记录
					}
					File file = null;
					int position = 0;
					if(log == null){//如果上传的文件不存在上传记录,为文件添加跟踪记录
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
                        if(file.exists() && !filename.endsWith(".pro")){//如果上传的文件发生重名，然后进行改名
                            filename = filename.substring(0, filename.indexOf("."))
                                    + dir.listFiles().length+ filename.substring(filename.indexOf("."));
                            file = new File(dir, filename);
                        }
                        save(id, file);
					}else{// 如果上传的文件存在上传记录,读取上次的断点位置
						file = new File(log.getPath());//从上传记录中得到文件的路径
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
							fileOutStream.setLength(Integer.valueOf(filelength));//设置文件长度
						fileOutStream.seek(position);//移动文件指定的位置开始写入数据
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

						Intent intent = new Intent();
						Bundle bundle = new Bundle();
						bundle.putString("recvdate", recvdate);
						bundle.putString("recvfilename", file.getAbsolutePath());
						bundle.putString("hostip", hostip);

						if (filename.endsWith(".pro")){
							Log.d("Emilio", "接收到配置文件");
						}else {
							intent.putExtras(bundle);
							intent.setAction(MainActivity.RECVMSG);
							MainActivity.local.sendBroadcast(intent);

							Intent showintent = new Intent();
							showintent.putExtra("hostip", hostip);
							showintent.setAction(NotificationService.SHOWPUSH);
							NotificationService.shownotice.sendBroadcast(showintent);

							Log.d("Emilio", "广播发送完成");
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

	// 保存上传记录
	public void save(Long id, File saveFile) {
		// 日后可以改成通过数据库存放
		datas.put(id, new FileLog(id, saveFile.getAbsolutePath()));
	}

	// 当文件上传完毕，删除记录
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
}
