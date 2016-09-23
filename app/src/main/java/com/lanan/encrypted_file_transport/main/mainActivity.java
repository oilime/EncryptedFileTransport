package com.lanan.encrypted_file_transport.main;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ExpandableListView;

import com.lanan.encrypted_file_transport.file_transport.chatActivity;
import com.lanan.encrypted_file_transport.R;
import com.lanan.encrypted_file_transport.services.notificationService;
import com.lanan.encrypted_file_transport.utils.mutex;
import com.lanan.encrypted_file_transport.utils.parameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class mainActivity extends AppCompatActivity {

    public static List<List<Map<String, Object>>> dataList;
    public static boolean isAbove = Build.VERSION.SDK_INT >= 21;

    public static mutex mutex = new mutex();

    public static LocalBroadcastManager local;
    public static BroadcastReceiver mReceiver;
    public static final String RECVMSG = parameters.MAIN_RECVMSG;

    private static String goname = "";
    private static final int WRITE_STORAGE = 1;
    private static final String mainPath = parameters.mainPath;

    private Intent serviceIntent;

    boolean retFlag;

    @Override  
    public void onCreate(Bundle savedInstanceState) {  
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.expandlist);

        serviceIntent = new Intent().setClass(mainActivity.this, notificationService.class);
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

        if (local == null)
            local = LocalBroadcastManager.getInstance(this);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(RECVMSG)){
                    try {
                        Log.d("Emilio", "接收到MAIN_RECVMSG广播");
                        Bundle bundle = intent.getExtras();
                        String recvDate = bundle.getString("recvdate");
                        String recvFilename = bundle.getString("recvfilename");
                        String recvIp = bundle.getString("hostip");
                        String recvname = "unknown_user";

                        for(int i = 0; i < mainActivity.dataList.size(); i++){
                            for (int j = 0; j < mainActivity.dataList.get(i).size(); j++){
                                String testIp = (String) mainActivity.dataList.get(i).get(j).get("ip");
                                if(testIp.equals(recvIp)){
                                    recvname = (String) mainActivity.dataList.get(i).get(j).get("name");
                                    break;
                                }
                            }
                        }

                        String recvPath = mainPath + "/FileTransport/" + recvname;
                        File recvFile = new File(recvPath, "historyinfo.txt");
                        if(!recvFile.exists()){
                            try {
                                retFlag = recvFile.createNewFile();
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }

                        if(recvname.equals(goname)){
                            Intent noticeIntent = new Intent();
                            Bundle dataBundle = new Bundle();
                            dataBundle.putString("recvdate", recvDate);
                            dataBundle.putString("recvname", recvname);
                            dataBundle.putString("recvfilename", recvFilename);
                            noticeIntent.putExtras(dataBundle);
                            noticeIntent.setAction(chatActivity.RECVMSG);
                            chatActivity.local.sendBroadcast(noticeIntent);
                            mainActivity.mutex.lock();
                        }

                        RandomAccessFile recvWrite = new RandomAccessFile(recvFile, "rw");
                        long fileLength = recvWrite.length();
                        recvWrite.seek(fileLength);

                        String sd = "Mode=in;Date=" + recvDate + ";Filename=" + recvFilename + "\r\n";
                        recvWrite.write(sd.getBytes());
                        recvWrite.close();

                        if(recvname.equals(goname))
                            mainActivity.mutex.unlock();

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        };
        local.registerReceiver(mReceiver, filter);
    }

    private void InitView(){
        mainAdapter mainAdapter = new mainAdapter(mainActivity.this, dataList);

        ExpandableListView expandableListView = (ExpandableListView) findViewById(R.id.expandlist);
        expandableListView.setGroupIndicator(null);
        expandableListView.setAdapter(mainAdapter);
        expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                Intent intent = new Intent();
                intent.setClass(mainActivity.this, chatActivity.class);
                goname = (String)dataList.get(groupPosition).get(childPosition).get("name");
                intent.putExtra("tarname", goname);
                intent.putExtra("ip", (String)dataList.get(groupPosition).get(childPosition).get("ip"));
                startActivity(intent);
                return true;
            }
        });

        parameters.setStatusBarColor(this, isAbove);
    }

    private void getTarData(){
        File path = new File(mainPath + "/.config/");
        if(!path.isDirectory()||!path.exists())
            retFlag = path.mkdirs();

        File dataFile = new File(path, "Filetransport.pro");
        if(!dataFile.exists()){
            try {
                retFlag = dataFile.createNewFile();
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
            String tarData;
            while((tarData = in.readLine()) != null){
                String[] items = tarData.split(";");
                String[] cItems = items[1].split("=");
                Map<String, Object> childMap = new HashMap<>();
                childMap.put("name", cItems[0]);
                childMap.put("ip", cItems[1]);
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
        if (ContextCompat.checkSelfPermission(mainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("Emilio","不能写");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_STORAGE);
        }else
            Log.d("Emilio","可以写");
    }

    /* 检查当前app权限 */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case WRITE_STORAGE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Emilio", "已获取写权限");
                } else {
                    Log.d("Emilio", "申请写权限失败");
                }
                break;
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        local.unregisterReceiver(mReceiver);
        stopService(serviceIntent);
    }
}  