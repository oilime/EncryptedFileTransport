package com.lanan.encrypted_file_transport.FileActions;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lanan.encrypted_file_transport.R;
import com.lanan.encrypted_file_transport.filetransport.ChatActivity;
import com.lanan.encrypted_file_transport.service.UploadLogService;
import com.lanan.encrypted_file_transport.utils.AES_crypt;
import com.lanan.encrypted_file_transport.utils.StreamTool;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;

/**
 * Created by lanan on 16-4-21.
 */

public class FileManager extends Activity {
    private TextView mPath;
    private ListView listView;
    private GridView gridView;
    private ImageView backButton;
    private Button sendButton;
    private LinearLayout cancel;
    private LinearLayout selectall;

    private String rootPath = null;
    private String pathName = null;
    private static String hostip = null;
    private static String serverip = null;

    public static Boolean image = false;
    private static newAdapter adapter = null;

    public static int num;
    private static Boolean decode = false;
    private static List<Map<String, Object>> mdatalist = null;
    private String[] namelist = new String[]{"音频", "视频", "图片", "文档"};

    private SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private UploadLogService logService;

    ProgressDialog pDialog;

    final int MY_PORT = 12024;

    @Override
    protected void onCreate(Bundle test){
        super.onCreate(test);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Bundle pathBundle = getIntent().getExtras();
        rootPath = pathBundle.getString("path");
        pathName = pathBundle.getString("name");
        image = pathBundle.getBoolean("flag");
        serverip = pathBundle.getString("serverip");
        hostip = pathBundle.getString("hostip");
        mdatalist = new ArrayList<>();

        Log.d("Emilio","path="+rootPath+" name="+pathName+" flag="+image);

        logService = new UploadLogService(this);

        if (pathName.equals("音频"))
            num = 0;
        else if (pathName.equals("视频"))
            num = 1;
        else if (pathName.equals("图片"))
            num = 2;
        else if (pathName.equals("文档"))
            num = 3;


        mdatalist.clear();
        mdatalist = getFileDir(ChatActivity.dir[num]);

        initview();
    }

    private void initview(){
        if (image){
            setContentView(R.layout.showfile_img);
            gridView = (GridView) findViewById(R.id.mygrid);

            cancel = (LinearLayout)findViewById(R.id.img_cancel);
            selectall = (LinearLayout)findViewById(R.id.img_selectall);
            backButton = (ImageView) findViewById(R.id.img_backbutton);
            sendButton = (Button) findViewById(R.id.img_sendbutton);
            mPath = (TextView) findViewById(R.id.img_title);
        } else{
            setContentView(R.layout.showfile_regular);
            listView = (ListView) findViewById(R.id.mylist);

            cancel = (LinearLayout)findViewById(R.id.reg_cancel);
            selectall = (LinearLayout)findViewById(R.id.reg_selectall);
            backButton = (ImageView)findViewById(R.id.reg_backbutton);
            sendButton = (Button)findViewById(R.id.reg_sendbutton);
            mPath = (TextView) findViewById(R.id.reg_title);
        }

        pDialog = new ProgressDialog(this);
        pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pDialog.setCanceledOnTouchOutside(false);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < mdatalist.size();i++)
                    mdatalist.get(i).put("flag", false);
                adapter.notifyDataSetChanged();
            }
        });

        selectall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < mdatalist.size();i++)
                    mdatalist.get(i).put("flag", true);
                adapter.notifyDataSetChanged();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < mdatalist.size(); i++) {
                    boolean isSelected = (boolean) mdatalist.get(i).get("flag");
                    if (isSelected) {
                        File file = new File((String) mdatalist.get(i).get("path"));
                        if (file.exists()){
                            uploadFile(file);
                        } else {
                            Toast.makeText(FileManager.this, "所选文件不存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                Intent gointent = new Intent();
                gointent.setClass(FileManager.this, ChatActivity.class);
                startActivity(gointent);
                Log.d("Emilio","jump!");
                FileManager.this.finish();
            }
        });

        if (image){
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.d("Emilio", "pressed");
                    boolean select = (boolean) mdatalist.get(position).get("flag");
                    mdatalist.get(position).put("flag", !select);
                    adapter.notifyDataSetChanged();
                }
            });
        }else {
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Log.d("Emilio", "pressed");
                    boolean select = (boolean) mdatalist.get(position).get("flag");
                    mdatalist.get(position).put("flag", !select);
                    adapter.notifyDataSetChanged();
                }
            });
        }

        mPath.setText(pathName);

        mdatalist = getFileDir(rootPath);

        adapter = new newAdapter(this, mdatalist);

        if (image){
            gridView.setAdapter(adapter);
        }else{
            listView.setAdapter(adapter);
        }

        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.parseColor("#2e40a4"));

        ViewGroup mContentView = (ViewGroup) this.findViewById(Window.ID_ANDROID_CONTENT);
        View mChildView = mContentView.getChildAt(0);
        if (mChildView != null) {
            ViewCompat.setFitsSystemWindows(mChildView, true);
        }
    }

    /* 文件发送 */
    private void uploadFile(final File uploadFile) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String souceid = logService.getBindId(uploadFile);
                    String head = "Hostip=" + hostip
                            + ";Content-Length="+ uploadFile.length()
                            + ";filename=" + uploadFile.getName()
                            + ";sourceid=" + (souceid==null? "" : souceid)+"\r\n";

                    InetAddress serverAddr = InetAddress.getByName(serverip); // 初始化目标地址
                    Socket socket = new Socket(serverAddr, MY_PORT); // 利用地址和端口定义一个Socket,
                    socket.setSoTimeout(0);

                    OutputStream outStream = socket.getOutputStream();
                    outStream.write(head.getBytes());
                    PushbackInputStream inStream = new PushbackInputStream(socket.getInputStream());
                    String response = StreamTool.readLine(inStream);

                    Log.d("Emilio", "socket成功");

                    String[] items = response.split(";");
                    String responseid = items[0].substring(items[0].indexOf("=")+1);
                    String position = items[1].substring(items[1].indexOf("=")+1);
                    if(souceid == null){//代表原来没有上传过此文件，往数据库添加一条绑定记录
                        logService.save(responseid, uploadFile);
                    }

                    InputStream inputStream = new FileInputStream(uploadFile);

                    if (uploadFile.getName().endsWith(".pro")){
                        byte[] buffer = new byte[1024];
                        int length = Integer.valueOf(position);
                        int sendLen = -1;

                        while ((sendLen = inputStream.read(buffer)) != -1){
                            outStream.write(buffer, 0, sendLen);
                            length += sendLen;
                        }

                        if (length == uploadFile.length())
                            Log.d("Emilio", "配置文件长度匹配，发送成功");

                    }else {
                        byte[] encryptBuffer = new byte[1024];
                        int length = Integer.valueOf(position);
                        int cryptLen = -1;

                        CipherInputStream cipherInputStream = new CipherInputStream(
                                inputStream, AES_crypt.cipherSet(Cipher.ENCRYPT_MODE));
                        while ((cryptLen = cipherInputStream.read(encryptBuffer)) != -1) {
                            outStream.write(encryptBuffer, 0, cryptLen);
                            length += cryptLen;
                        }

                        cipherInputStream.close();
                    }

                    Date curDate = new Date(System.currentTimeMillis());
                    String senddate	= newFormat.format(curDate);

                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putString("senddate", senddate);
                    bundle.putString("sendfilename", uploadFile.getAbsolutePath());

                    intent.putExtras(bundle);
                    intent.setAction(ChatActivity.SENDMSG);
                    ChatActivity.local.sendBroadcast(intent);

                    inputStream.close();
                    inStream.close();
                    outStream.close();
                    socket.close();


                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("Emilio", "socket失败");
                }
            }
        }).start();
    }

    private List<Map<String, Object>> getFileDir(final String filePath) {
        final List<Map<String, Object>> newlist = new ArrayList<Map<String, Object>>();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                File f = new File(filePath);
                File[] files = f.listFiles();
                for (int i = 0; i < files.length; i++) {
                    File file = files[i];
                    if(!file.getName().startsWith(".")){
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("name", file.getName());
                        map.put("path", file.getAbsolutePath());
                        map.put("flag", false);
                        if (num == 2) {
                            map.put("thumbnail", thumbnail.decodeFile(file));
                        }
                        newlist.add(map);
                    }
                }
                decode = true;
            }
        });
        thread.start();

        while (decode == false){
            try {
                Thread.sleep(100);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        decode = false;
        return newlist;
    }

    private void cleanFlag(){
        for (int i = 0; i < mdatalist.size(); i++)
            mdatalist.get(i).put("flag", false);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        cleanFlag();
    }
}