package com.lanan.filetransport.file_transport;

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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lanan.filetransport.R;
import com.lanan.filetransport.file_manage.FileManager;
import com.lanan.filetransport.main.MainActivity;
import com.lanan.filetransport.utils.Encryption;
import com.lanan.filetransport.utils.Parameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity{

	public List<ChatMsgEntity> mDataArrays = new ArrayList<>();
	private static Vibrator vibrator;

	public static LocalBroadcastManager local;
	public static BroadcastReceiver mReceiver;

	private static String objname;
	private static String serverip;
	private static String hostip;
	private static boolean recvFlag = true;

	private static File info;

	public static final String DECRYPTFILE = Parameters.DECRYPTFILE;
	public static final String FILE = Parameters.FILE;
	public static final String RECVMSG = Parameters.CHAT_RECVMSG;
    public static final String SENDMSG = Parameters.SENDMSG;

	private static final String mainPath = Parameters.mainPath;
	private static final String musicDir = Parameters.musicDir;
	private static final String videoDir = Parameters.videoDir;
	private static final String imageDir = Parameters.imageDir;
	private static final String docDir = Parameters.docDir;
	private static final String tempDir = Parameters.tempDir;
	public static final String[] dir = new String[]{musicDir, videoDir, imageDir, docDir, tempDir};

    ImageView mBtnSend;
    ImageView mBtnBack;
    ListView mListView;
    TextView tarName;
    ProgressDialog pDialog;
	ChatMsgViewAdapter mAdapter;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.mymain);

		register();
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

	private void register(){
		vibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);

        IntentFilter filter = new IntentFilter();
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
					Log.d("Emilio", "待解密文件名: " + filename);
					final File file = new File(filename);
					File tempDir = new File(mainPath + "/Filetransport/temp/");
					if (!tempDir.exists() || !tempDir.isDirectory()){
						recvFlag = tempDir.mkdirs();
					}

                    if (!recvFlag){
                        Log.d("Emilio", "临时文件夹创建失败");
                    }
					final File tmpFile = new File(tempDir, file.getName());
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
									if(!tmpFile.exists()){
										Encryption.fileDecrypt(file, tmpFile);
									}
									pDialog.dismiss();
									Intent intent = new Intent();
									intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
									intent.setAction(android.content.Intent.ACTION_VIEW);
									String type = getMIMEType(tmpFile);
									intent.setDataAndType(Uri.fromFile(tmpFile), type);
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
						String recvDate = bundle.getString("recvdate");
						String recvFilename = bundle.getString("recvfilename");
						String recvName = bundle.getString("recvname");

                        ChatMsgEntity recvEntity = new ChatMsgEntity();
                        recvEntity.setName(recvName);
                        recvEntity.setDate(recvDate);
                        recvEntity.setMessage(recvFilename);
                        recvEntity.setMsgType(true);

                        mDataArrays.add(recvEntity);
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

                        ChatMsgEntity sendEntity = new ChatMsgEntity();
                        sendEntity.setDate(senddate);
                        sendEntity.setMessage(sendfilename);
                        sendEntity.setMsgType(false);

                        mDataArrays.add(sendEntity);
                        mAdapter.notifyDataSetChanged();
                        mListView.setSelection(mListView.getCount() - 1);

                        MainActivity.mutex.lock();
                        RandomAccessFile sendw = new RandomAccessFile(info, "rw");
                        long fileLength = sendw.length();
                        sendw.seek(fileLength);

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
		mAdapter = new ChatMsgViewAdapter(this, mDataArrays);
		mListView.setAdapter(mAdapter);
        mListView.setSelection(mListView.getCount() - 1);

		mBtnSend = (ImageView) findViewById(R.id.select_button);
		mBtnSend.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                final AlertDialog alertDialog = builder.create();
                alertDialog.setCanceledOnTouchOutside(true);
                alertDialog.show();

                final Window window = alertDialog.getWindow();
				if (window != null) {
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
        if (objname.equals(""))
		    tarName.setText(objname);

		pDialog = new ProgressDialog(ChatActivity.this);
		pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		pDialog.setCanceledOnTouchOutside(false);

		Parameters.setStatusBarColor(this, MainActivity.isAbove);
	}

	public void initData() {
		mDataArrays.clear();
		objname = getIntent().getStringExtra("tarname");
		serverip = getIntent().getStringExtra("ip");
		hostip = getLocalIpAddress();

		File savePath = new File(mainPath + "/FileTransport/" + objname);
		if(!savePath.exists() || !savePath.isDirectory()){
			recvFlag = savePath.mkdirs();
		}

        if (!recvFlag){
            Log.d("Emilio", "对应文件夹创建失败");
        }

		info = new File(savePath, "historyinfo.txt");
		if(!info.exists()){
			try {
				recvFlag = info.createNewFile();
			}catch (Exception e){
				e.printStackTrace();
			}
		}

		try {
			MainActivity.mutex.lock();
			BufferedReader br = new BufferedReader(new FileReader(info));
			String tarData;
			String[] items;
			while((tarData = br.readLine()) != null){
				items = tarData.split(";");
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

	public String getLocalIpAddress() {
		WifiManager wifiManager=(WifiManager)this.getSystemService(Context.WIFI_SERVICE);
		if(!wifiManager.isWifiEnabled()){
			wifiManager.setWifiEnabled(true);
		}
		WifiInfo wifiinfo= wifiManager.getConnectionInfo();
		int i =wifiinfo.getIpAddress();
		String ip = intToIp(i);
		Log.d("Emilio", "当前设备ip:" + ip);
		return ip;
	}

	private String intToIp(int i)  {
		return (i & 0xFF)+ "." + ((i >> 8 ) & 0xFF) + "." + ((i >> 16 ) & 0xFF) +"."+((i >> 24 ) & 0xFF);
	}

	private static String getMIMEType(File f) {
		String type;
		String fName = f.getName();
		String end = fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toLowerCase();

		switch (end){
            case "m4a":
            case "mp3":
            case "mid":
            case "xmf":
            case "ogg":
            case "wav":
                type = "audio";
                break;
            case "3gp":
            case "mp4":
            case "avi":
                type = "video";
                break;
            case "jpg":
            case "gif":
            case "png":
            case "jpeg":
            case "bmp":
                type = "image";
                break;
            case "txt":
                type = "text";
                break;
            default:
                type = "*";
                break;
        }

		type += "/*";
		return type;
	}
}