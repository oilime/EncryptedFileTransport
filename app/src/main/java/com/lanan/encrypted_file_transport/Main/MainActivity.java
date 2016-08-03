package com.lanan.encrypted_file_transport.Main;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ExpandableListView;

import com.lanan.encrypted_file_transport.FileTransport.ChatActivity;
import com.lanan.encrypted_file_transport.R;
import com.lanan.encrypted_file_transport.Services.NotificationService;
import com.lanan.encrypted_file_transport.Utils.Mutex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    public static List<List<Map<String, Object>>> dataList;

    public static Mutex mutex = new Mutex();

    public static LocalBroadcastManager local;
    public static BroadcastReceiver mReceiver;
    public static final String RECVMSG = "com.main.recvmsg";

    private static String goname = "";
    private static final int WRITE_STORAGE = 1;
    private static final String mainPath = "/sdcard/alan/system/security/local/tmp/chs";

    private Intent serviceIntent;

    private MainAdapter mainAdapter;

    public static SQLiteDatabase sqLiteDatabase;

    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.expandlist);

        serviceIntent = new Intent().setClass(MainActivity.this, NotificationService.class);
        startService(serviceIntent);

        PermissionCheck();          //权限检查
        getTarData();               //读取文件获取目标名称和ip
        InitView();                 //界面初始化
        localBroadcastRegister();   //注册本地广播
    }

    @Override
    protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void localBroadcastRegister(){
        IntentFilter filter = new IntentFilter();
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

                        String recvPath = mainPath + "/FileTransport/" + recvname;
                        File recvFile = new File(recvPath, "historyinfo.txt");
                        if(!recvFile.exists()){
                            try {
                                recvFile.createNewFile();
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

                        RandomAccessFile recvWrite = new RandomAccessFile(recvFile, "rw");
                        long filelength = recvWrite.length();
                        recvWrite.seek(filelength);

                        String sd = "Mode=in;Date=" + recvdate + ";Filename=" + recvfilename + "\r\n";
                        recvWrite.write(sd.getBytes());
                        recvWrite.close();

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
        mainAdapter = new MainAdapter(MainActivity.this, dataList);

        ExpandableListView expandableListView = (ExpandableListView) findViewById(R.id.expandlist);
        expandableListView.setGroupIndicator(null);
        expandableListView.setAdapter(mainAdapter);
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

//        Window window = this.getWindow();
//        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
//        window.setStatusBarColor(Color.parseColor("#2e40a4"));
//
//        ViewGroup mContentView = (ViewGroup) this.findViewById(Window.ID_ANDROID_CONTENT);
//        View mChildView = mContentView.getChildAt(0);
//        if (mChildView != null) {
//            ViewCompat.setFitsSystemWindows(mChildView, true);
//        }
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

        dataList = new ArrayList<>();
        dataList.clear();

        List<Map<String, Object>> list1 = new ArrayList<>();
        List<Map<String, Object>> list2 = new ArrayList<>();
        List<Map<String, Object>> list3 = new ArrayList<>();
        List<Map<String, Object>> list4 = new ArrayList<>();

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(dataFile)));
            String tardata;
            while((tardata = in.readLine()) != null){
                String[] items = tardata.split(";");
                String[] citems = items[1].split("=");
                Map<String, Object> childMap = new HashMap<>();
                childMap.put("name", citems[0]);
                childMap.put("ip", citems[1]);
                switch (Integer.parseInt(items[0])){
                    case 1:
                        list1.add(childMap);
                        break;
                    case 2:
                        list2.add(childMap);
                        break;
                    case 3:
                        list3.add(childMap);
                        break;
                    case 4:
                        list4.add(childMap);
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
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission","不能写");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_STORAGE);
        }else
            Log.d("Permission","可以写");
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
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopService(serviceIntent);
        sqLiteDatabase.close();
    }
}  