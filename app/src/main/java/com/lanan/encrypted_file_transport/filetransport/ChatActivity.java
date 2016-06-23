package com.lanan.encrypted_file_transport.filetransport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lanan.encrypted_file_transport.FileActions.FileManager;
import com.lanan.encrypted_file_transport.R;
import com.lanan.encrypted_file_transport.service.UploadLogService;
import com.lanan.encrypted_file_transport.utils.AES_crypt;
import com.lanan.encrypted_file_transport.Main.MainActivity;

public class ChatActivity extends AppCompatActivity{

	private ImageView mBtnSend;// 发送btn
	private ImageView mBtnBack;// 返回btn
	private ListView mListView;
	private ChatMsgViewAdapter mAdapter;// 消息视图的Adapter
	private TextView tarName;
	private static ProgressDialog pDialog;

	private UploadLogService logService;
	public List<ChatMsgEntity> mDataArrays = new ArrayList<ChatMsgEntity>();// 消息对象数组
	private static Vibrator vibrator;
	private static IntentFilter filter;

	public static LocalBroadcastManager local;
	public static BroadcastReceiver mReceiver;

	private static String objname;
	private static String objpath;
	private static String serverip;
	private static String hostip;

	private static File info;

	public static final String DECRYPTFILE = "com.lanan.decryptfile";
	public static final String FILE = "com.lanan.openfile";
	public static final String RECVMSG = "com.lanan.recvmsg";
    public static final String SENDMSG = "com.lanan.sendmsg";

	private static final String mainPath = "/sdcard/alan/system/security/local/tmp/chs";
//    private static final String mainPath = "/sdcard";
	private static final String musicDir = mainPath + "/演示音乐/";
	private static final String videoDir = mainPath + "/演示视频/";
	private static final String imageDir = mainPath + "/演示图片/";
	private static final String docDir = mainPath + "/演示文档/";
	private static final String tempDir = mainPath + "/FileTransport/temp/";
	public static final String[] dir = new String[]{musicDir, videoDir, imageDir, docDir, tempDir};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.mymain);

		regist();
		initData();
		initView();
	}

	@Override
	protected void onNewIntent(Intent intent){
		super.onNewIntent(intent);
		setIntent(intent);
	}

	@Override
	protected void onDestroy(){
		super.onDestroy();
        local.unregisterReceiver(mReceiver);
	}

	private void regist(){
		logService = new UploadLogService(this);

		vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);

		filter = new IntentFilter();
		filter.addAction(DECRYPTFILE);
		filter.addAction(FILE);
		filter.addAction(RECVMSG);
        filter.addAction(SENDMSG);

		Log.d("Emilio", "Chat filter注册成功");

		local = LocalBroadcastManager.getInstance(this);
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals(DECRYPTFILE)){
					String filename = intent.getStringExtra("filename");
					final File file = new File(filename);
					File tempdir = new File(mainPath + "/Filetransport/temp/");
					if (!tempdir.exists() || !tempdir.isDirectory()){
						tempdir.mkdirs();
					}
					final File tmpfile = new File(tempdir, file.getName());
					if(file.exists()){
						pDialog.setMessage("解密中...");
						pDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
							}
						});
						pDialog.show();
						new Thread(){
							public void run(){
								try{
									Thread.sleep(500);
									if(!tmpfile.exists()){
										AES_crypt.fileDecrypt(file, tmpfile);
									}
									pDialog.dismiss();
									Intent intent = new Intent();
									intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									intent.setAction(android.content.Intent.ACTION_VIEW);
									String type = getMIMEType(tmpfile);
									intent.setDataAndType(Uri.fromFile(tmpfile), type);
									startActivity(intent);
								}catch (Exception e){
									e.printStackTrace();
								}
							}
						}.start();
					}else {
						Toast.makeText(ChatActivity.this, "该文件已被删除或转移到其他位置", Toast.LENGTH_SHORT).show();
					}
				}else if (intent.getAction().equals(FILE)){
					String filename = intent.getStringExtra("filename");
					final File file = new File(filename);
					if(file.exists()){
						new Thread(){
                            public void run(){
                                try{
                                    Intent intent = new Intent();
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    intent.setAction(android.content.Intent.ACTION_VIEW);
                                    String type = getMIMEType(file);
                                    intent.setDataAndType(Uri.fromFile(file), type);
                                    startActivity(intent);
                                }catch (Exception e){
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }else
                        Toast.makeText(ChatActivity.this, "该文件已被删除或转移到其他位置", Toast.LENGTH_SHORT).show();
                }else if (intent.getAction().equals(RECVMSG)){
                    try {
                        Log.d("Emilio", "接收到CHAT_RECVMSG广播");

						Bundle bundle = intent.getExtras();
						String recvdate = bundle.getString("recvdate");
						String recvfilename = bundle.getString("recvfilename");
						String recvname = bundle.getString("recvname");

                        ChatMsgEntity recventity = new ChatMsgEntity();
                        recventity.setName(recvname);
                        recventity.setDate(recvdate);
                        recventity.setMessage(recvfilename);
                        recventity.setMsgType(true);

                        mDataArrays.add(recventity);
                        mAdapter.notifyDataSetChanged();
                        mListView.setSelection(mListView.getCount() - 1);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }else if (intent.getAction().equals(SENDMSG)){
                    try {
                        Log.d("Emilio", "接收到SENDMSG广播");
                        Bundle bundle = intent.getExtras();
                        String senddate = bundle.getString("senddate");
                        String sendfilename = bundle.getString("sendfilename");

                        ChatMsgEntity sendentity = new ChatMsgEntity();
                        sendentity.setDate(senddate);
                        sendentity.setMessage(sendfilename);
                        sendentity.setMsgType(false);

                        mDataArrays.add(sendentity);
                        mAdapter.notifyDataSetChanged();
                        mListView.setSelection(mListView.getCount() - 1);

                        MainActivity.mutex.lock();
                        RandomAccessFile sendw = new RandomAccessFile(info, "rw");
                        long filelength = sendw.length();
                        sendw.seek(filelength);

                        String sd = "Mode=out;Date=" + senddate + ";Filename=" + sendfilename + "\r\n";
                        sendw.write(sd.getBytes());
                        sendw.close();
                        vibrator.vibrate(1000);
                        MainActivity.mutex.unlock();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        };
        local.registerReceiver(mReceiver, filter);
	}

	private void initView() {
		mListView = (ListView) findViewById(R.id.listview);

		mBtnSend = (ImageView) findViewById(R.id.select_button);
		mBtnSend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                final AlertDialog alertDialog = builder.create();
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();

                final Window window = alertDialog.getWindow();
                window.setContentView(R.layout.choosedialog);
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                LinearLayout smusic = (LinearLayout)window.findViewById(R.id.musicselect);
                LinearLayout svideo = (LinearLayout)window.findViewById(R.id.videoselect);
                LinearLayout simage = (LinearLayout)window.findViewById(R.id.imageselect);
                LinearLayout sdoc = (LinearLayout)window.findViewById(R.id.docselect);

                smusic.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ChatActivity.this, FileManager.class);
                        Bundle des = new Bundle();
                        des.putString("path",dir[0]);
                        des.putString("name","音频");
                        des.putString("serverip", serverip);
                        des.putString("hostip", hostip);
                        des.putBoolean("flag", false);
                        intent.putExtras(des);
                        startActivity(intent);
                        alertDialog.cancel();
                    }
                });

                svideo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ChatActivity.this, FileManager.class);
                        Bundle des = new Bundle();
                        des.putString("path",dir[1]);
                        des.putString("name","视频");
                        des.putString("serverip", serverip);
                        des.putString("hostip", hostip);
                        des.putBoolean("flag", false);
                        intent.putExtras(des);
                        startActivity(intent);
                        alertDialog.cancel();
                    }
                });

                simage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ChatActivity.this, FileManager.class);
                        Bundle des = new Bundle();
                        des.putString("path",dir[2]);
                        des.putString("name","图片");
                        des.putString("serverip", serverip);
                        des.putString("hostip", hostip);
                        des.putBoolean("flag", true);
                        intent.putExtras(des);
                        startActivity(intent);
                        alertDialog.cancel();
                    }
                });

                sdoc.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ChatActivity.this, FileManager.class);
                        Bundle des = new Bundle();
                        des.putString("path",dir[3]);
                        des.putString("name","文档");
                        des.putString("serverip", serverip);
                        des.putString("hostip", hostip);
                        des.putBoolean("flag", false);
                        intent.putExtras(des);
                        startActivity(intent);
                        alertDialog.cancel();
                    }
                });
			}
		});

		mBtnBack = (ImageView) findViewById(R.id.back_button);
		mBtnBack.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(ChatActivity.this, MainActivity.class);
                startActivity(intent);
				finish();
			}
		});

		tarName = (TextView) findViewById(R.id.tarName);
		tarName.setText(objname);

		pDialog = new ProgressDialog(ChatActivity.this);
		pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pDialog.setCanceledOnTouchOutside(false);


		mAdapter = new ChatMsgViewAdapter(this, mDataArrays);
		mListView.setAdapter(mAdapter);
		mListView.setSelection(mAdapter.getCount() - 1);

		Window window = this.getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		window.setStatusBarColor(Color.parseColor("#2e40a4"));

		ViewGroup mContentView = (ViewGroup) this.findViewById(Window.ID_ANDROID_CONTENT);
		View mChildView = mContentView.getChildAt(0);
		if (mChildView != null) {
			ViewCompat.setFitsSystemWindows(mChildView, true);
		}
	}

	public void initData() {
		mDataArrays.clear();
		objname = getIntent().getStringExtra("tarname");
		objpath = mainPath + "/FileTransport/" + objname;
		serverip = getIntent().getStringExtra("ip");
		hostip = getLocalIpAddress();

		File savePath = new File(objpath);
		if(!savePath.exists() || !savePath.isDirectory()){
			savePath.mkdirs();
		}

		info = new File(savePath, "historyinfo.txt");
		if(!info.exists()){
			try {
				info.createNewFile();
			}catch (Exception e){
				e.printStackTrace();
			}
		}

		try {
			MainActivity.mutex.lock();
			BufferedReader br = new BufferedReader(new FileReader(info));
			String tardata = null;
			String[] items = null;
			while((tardata = br.readLine()) != null){
				items = tardata.split(";");
				String mode = items[0].substring(items[0].indexOf("=") + 1);
				String date = items[1].substring(items[1].indexOf("=") + 1);
				String filename = items[2].substring(items[2].indexOf("=") + 1);

				ChatMsgEntity entity = new ChatMsgEntity();
				entity.setDate(date);
				entity.setMessage(filename);
				if (mode.equals("in")){
					entity.setMsgType(true);
					entity.setName(objname);
				}else {
					entity.setMsgType(false);
					entity.setName("me");
				}
				mDataArrays.add(entity);
			}
			br.close();
			MainActivity.mutex.unlock();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	/* 获取wifi的ip地址 */
	public String getLocalIpAddress() {
		WifiManager wifimanage=(WifiManager)this.getSystemService(Context.WIFI_SERVICE);
		if(!wifimanage.isWifiEnabled()){
			wifimanage.setWifiEnabled(true);
		}
		WifiInfo wifiinfo= wifimanage.getConnectionInfo();
		int i =wifiinfo.getIpAddress();
		String ip = intToIp(i);
		Log.d("Emilio", "当前设备ip:" + ip);
		return ip;
	}

	private String intToIp(int i)  {
		return (i & 0xFF)+ "." + ((i >> 8 ) & 0xFF) + "." + ((i >> 16 ) & 0xFF) +"."+((i >> 24 ) & 0xFF);
	}

	/* 获取文件类型 */
	private static String getMIMEType(File f) {
		String type = null;
		String fName = f.getName();
		String end = fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toLowerCase();
		if (end.equals("m4a") || end.equals("mp3") || end.equals("mid")
				|| end.equals("xmf") || end.equals("ogg") || end.equals("wav")) {
			type = "audio";
		} else if (end.equals("3gp") || end.equals("mp4")|| end.equals("avi")) {
			type = "video";
		} else if (end.equals("jpg") || end.equals("gif") || end.equals("png")
				|| end.equals("jpeg") || end.equals("bmp")) {
			type = "image";
		} else {
			type = "*";
		}
		type += "/*";
		return type;
	}
}