package com.lanan.encrypted_file_transport.Main;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ExpandableListView;

import com.lanan.encrypted_file_transport.R;
import com.lanan.encrypted_file_transport.filetransport.ChatActivity;
import com.lanan.encrypted_file_transport.receive.SocketServer;
import com.lanan.encrypted_file_transport.service.NotificationService;
import com.lanan.encrypted_file_transport.utils.Mutex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static List<List<Map<String, Object>>> dataList;

    private static ServerSocket server2 = null;
    private static SocketServer server;
	final int MY_PORT = 12024;

    public static Mutex mutex = new Mutex();

    public static LocalBroadcastManager local;
    public static BroadcastReceiver mReceiver;
    private static IntentFilter filter;
    public static final String RECVMSG = "com.main.recvmsg";

    private static String goname = "";
    private static final int WRITE_STORAGE = 1;
    private static final int READ_STORAGE = 2;
    private static final int MOUNT = 3;
    private static final String mainPath = "/sdcard/alan/system/security/local/tmp/chs";
//    private static final String mainPath = "/sdcard";

    private Intent noticeIntent;

    private ExpandAdapter myadapter;

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopService(noticeIntent);
    }

    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.expandlist);

        noticeIntent = new Intent(MainActivity.this, NotificationService.class);
        startService(noticeIntent);

        PermissionCheck();//权限检查
        getTarData();//读取文件获取目标名称和ip
        InitView();
        regist();
		CreateMySocket();//监听端口等待接收
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void regist(){
        filter = new IntentFilter();
        filter.addAction(RECVMSG);
        Log.d("Emilio", "Main filter注册成功");

        local = LocalBroadcastManager.getInstance(this);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(RECVMSG)){
                    try {
                        Log.d("Emilio", "接收到MAIN_RECVMSG广播");
                        Bundle bundle = intent.getExtras();
                        String recvdate = bundle.getString("recvdate");
                        String recvfilename = bundle.getString("recvfilename");
                        String recvip = bundle.getString("hostip");
                        String recvname = "unknown_user";

                        for(int i = 0;i < MainActivity.dataList.size();i++){
                            for (int j = 0;j < MainActivity.dataList.get(i).size();j++){
                                String testip = (String)MainActivity.dataList.get(i).get(j).get("ip");
                                if(testip.equals(recvip)){
                                    recvname = (String)MainActivity.dataList.get(i).get(j).get("name");
                                    break;
                                }
                            }
                        }

                        String recvpath = mainPath + "/FileTransport/" + recvname;
                        File recvfile = new File(recvpath, "historyinfo.txt");
                        if(!recvfile.exists()){
                            try {
                                recvfile.createNewFile();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        if(recvname.equals(goname)){
                            Intent intent1 = new Intent();
                            Bundle bundle1 = new Bundle();
                            bundle1.putString("recvdate", recvdate);
                            bundle1.putString("recvname", recvname);
                            bundle1.putString("recvfilename", recvfilename);
                            intent1.putExtras(bundle1);
                            intent1.setAction(ChatActivity.RECVMSG);
                            ChatActivity.local.sendBroadcast(intent1);
                            MainActivity.mutex.lock();
                        }

                        RandomAccessFile recvw = new RandomAccessFile(recvfile, "rw");
                        long filelength = recvw.length();
                        recvw.seek(filelength);

                        String sd = "Mode=in;Date=" + recvdate + ";Filename=" + recvfilename + "\r\n";
                        recvw.write(sd.getBytes());
                        recvw.close();

                        if(recvname.equals(goname))
                            MainActivity.mutex.unlock();

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        };
        local.registerReceiver(mReceiver, filter);
    }

    private void InitView(){
        myadapter = new ExpandAdapter(MainActivity.this, dataList);

        ExpandableListView expandableListView = (ExpandableListView) findViewById(R.id.expandlist);
        expandableListView.setGroupIndicator(null);
        expandableListView.setAdapter(myadapter);
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Intent intent = new Intent();
                intent.setClass(MainActivity.this, ChatActivity.class);
                goname = (String)dataList.get(groupPosition).get(childPosition).get("name");
                intent.putExtra("tarname", goname);
                intent.putExtra("ip", (String)dataList.get(groupPosition).get(childPosition).get("ip"));
                startActivity(intent);
                return true;
            }
        });

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#2e40a4"));

        ViewGroup mContentView = (ViewGroup) this.findViewById(Window.ID_ANDROID_CONTENT);
        View mChildView = mContentView.getChildAt(0);
        if (mChildView != null) {
            ViewCompat.setFitsSystemWindows(mChildView, true);
        }
    }

    private void getTarData(){
        File path = new File(mainPath + "/.config/");
        if(!path.isDirectory()||!path.exists())
            path.mkdirs();

        File dataFile = new File(path, "Filetransport.pro");
        if(!dataFile.exists()){
            try {
                dataFile.createNewFile();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        dataList = new ArrayList<List<Map<String, Object>>>();
        dataList.clear();

        List<Map<String, Object>> list1 = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> list2 = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> list3 = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> list4 = new ArrayList<Map<String, Object>>();

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile)));
            String tardata = null;
            String[] items = null;
            String[] citems = null;
            while((tardata = in.readLine()) != null){
                items = tardata.split(";");
                citems = items[1].split("=");
                Map<String, Object> childmap = new HashMap<String, Object>();
                childmap.put("name", citems[0]);
                childmap.put("ip", citems[1]);
                switch (Integer.parseInt(items[0])){
                    case 1:
                        list1.add(childmap);
                        break;
                    case 2:
                        list2.add(childmap);
                        break;
                    case 3:
                        list3.add(childmap);
                        break;
                    case 4:
                        list4.add(childmap);
                        break;
                }
            }
            in.close();
            dataList.add(list1);
            dataList.add(list2);
            dataList.add(list3);
            dataList.add(list4);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void PermissionCheck(){
        /* 权限检查 */
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission","不能写");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_STORAGE);
        }else
            Log.d("Permission","可以写");

//        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            Log.d("Permission","不能读");
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
//                    READ_STORAGE);
//        }else
//            Log.d("Permission","可以读");
//
//        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS)
//                != PackageManager.PERMISSION_GRANTED) {
//            Log.d("Permission","没挂载");
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS},
//                    MOUNT);
//        }else
//            Log.d("Permission","可挂载");
    }

    /* 检查当前app权限 */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case WRITE_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission", "已获取写权限");
                } else {
                    Log.d("Permission", "申请写权限失败");
                }
                break;
            case READ_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission", "已获取读权限");
                } else {
                    Log.d("Permission", "申请读权限失败");
                }
                break;
            case MOUNT:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission", "已获取挂载权限");
                } else {
                    Log.d("Permission", "申请挂载权限失败");
                }
                break;
        }
    }

	private void CreateSocket() 
	{
		try {
			server2 = new ServerSocket(MY_PORT,100);
			server2.setSoTimeout(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public boolean CreateMySocket() {
		// 创建Socket 服务器
		CreateSocket();
        server = new SocketServer(server2);
    	try {
            server.start();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return true;
    }
}  