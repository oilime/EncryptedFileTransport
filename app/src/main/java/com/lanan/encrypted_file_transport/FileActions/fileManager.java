package com.lanan.encrypted_file_transport.FileActions;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.lanan.encrypted_file_transport.FileTransport.chatActivity;
import com.lanan.encrypted_file_transport.Main.mainActivity;
import com.lanan.encrypted_file_transport.R;
import com.lanan.encrypted_file_transport.Services.uploadLogService;
import com.lanan.encrypted_file_transport.Utils.encryption;
import com.lanan.encrypted_file_transport.Utils.getThumbnail;
import com.lanan.encrypted_file_transport.Utils.parameters;
import com.lanan.encrypted_file_transport.Utils.streamTool;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class fileManager extends AppCompatActivity {

    private ListView listView;
    private GridView gridView;

    private String rootPath = null;
    private String pathName = null;
    private static String hostip = null;
    private static String serverip = null;

    public static Boolean image = false;
    private static fileManagerAdapter adapter = null;

    public static int num;
    private static Boolean decode = false;
    private static List<Map<String, Object>> mdatalist = null;

    private static SSLContext ctx;

    private SimpleDateFormat timeFormat = parameters.newFormat;

    private uploadLogService logService;

    ProgressDialog pDialog;
    TextView mPath;
    ImageView backButton;
    Button sendButton;
    LinearLayout cancel;
    LinearLayout selectAll;

    final int MY_PORT = 10717;

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

        switch (pathName){
            case "音频":
                num = 0;
                break;
            case "视频":
                num = 1;
                break;
            case "图片":
                num = 2;
                break;
            case "文档":
                num = 3;
                break;
        }

        logService = new uploadLogService(this);

        mdatalist.clear();
        mdatalist = getFileDir(chatActivity.dir[num]);

        initViews();
    }

    private void initViews(){
        if (image){
            setContentView(R.layout.showfile_img);
            gridView = (GridView) findViewById(R.id.mygrid);

            cancel = (LinearLayout)findViewById(R.id.img_cancel);
            selectAll = (LinearLayout)findViewById(R.id.img_selectall);
            backButton = (ImageView) findViewById(R.id.img_backbutton);
            sendButton = (Button) findViewById(R.id.img_sendbutton);
            mPath = (TextView) findViewById(R.id.img_title);
        }else {
            setContentView(R.layout.showfile_regular);
            listView = (ListView) findViewById(R.id.mylist);

            cancel = (LinearLayout)findViewById(R.id.reg_cancel);
            selectAll = (LinearLayout)findViewById(R.id.reg_selectall);
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

        selectAll.setOnClickListener(new View.OnClickListener() {
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
                            Toast.makeText(fileManager.this, "所选文件不存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                Intent goIntent = new Intent();
                goIntent.setClass(fileManager.this, chatActivity.class);
                startActivity(goIntent);
                fileManager.this.finish();
            }
        });

        if (image){
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    boolean select = (boolean) mdatalist.get(position).get("flag");
                    mdatalist.get(position).put("flag", !select);
                    adapter.notifyDataSetChanged();
                }
            });
        }else {
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    boolean select = (boolean) mdatalist.get(position).get("flag");
                    mdatalist.get(position).put("flag", !select);
                    adapter.notifyDataSetChanged();
                }
            });
        }

        mPath.setText(pathName);

        mdatalist = getFileDir(rootPath);

        adapter = new fileManagerAdapter(this, mdatalist);

        if (image){
            gridView.setAdapter(adapter);
        }else{
            listView.setAdapter(adapter);
        }

        parameters.setStatusBarColor(this, mainActivity.isAbove);
        sslInit();
    }

    private void sslInit() {
        try {
            //初始化加密密钥库和信任密钥库
            KeyStore clientKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            KeyStore trustedKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try {
                clientKeyStore.load(getBaseContext().getResources().getAssets().open("Client.bks"),
                        parameters.KEY_STORE_PWD.toCharArray());
                trustedKeyStore.load(getBaseContext().getResources().getAssets().open("trustedClient.bks"),
                        parameters.KEY_STORE_PWD.toCharArray());
            }catch (Exception e){
                e.printStackTrace();
            }

            //初始化密钥管理器和信任管理器
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(parameters.KEY_MANAGER);
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(parameters.KEY_MANAGER);
            try {
                keyManagerFactory.init(clientKeyStore, parameters.KEY_STORE_PWD.toCharArray());
                trustManagerFactory.init(trustedKeyStore);
                Log.d("Emilio", "信任密钥管理器加载完毕");
            }catch (Exception e){
                e.printStackTrace();
            }

            //初始化SSL上下文
            ctx = SSLContext.getInstance("TLS");
            Log.d("Emilio", "SSL模式选择成功");
            try {
                ctx.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
                Log.d("Emilio", "SSL上下文初始化成功");
            }catch (Exception e){
                e.printStackTrace();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /* 文件发送 */
    private void uploadFile(final File uploadFile) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String sourceId = logService.getBindId(uploadFile);
                    String head = "Hostip=" + hostip
                            + ";Content-Length="+ uploadFile.length()
                            + ";filename=" + uploadFile.getName()
                            + ";sourceid=" + (sourceId == null ? "" : sourceId)+"\r\n";

                    InetAddress serverAddr = InetAddress.getByName(serverip);
                    SSLSocketFactory ssf = ctx.getSocketFactory();
                    SSLSocket socket = (SSLSocket) ssf.createSocket(serverAddr, MY_PORT);
                    socket.setSoTimeout(0);

                    OutputStream outStream = socket.getOutputStream();
                    outStream.write(head.getBytes());
                    PushbackInputStream inStream = new PushbackInputStream(socket.getInputStream());
                    Log.d("Emilio", "socket成功");

                    String response = streamTool.readLine(inStream);
                    String[] items = response.split(";");
                    String responseId = items[0].substring(items[0].indexOf("=")+1);
                    String position = items[1].substring(items[1].indexOf("=")+1);
                    if(sourceId == null){
                        logService.save(responseId, uploadFile);
                    }

                    InputStream inputStream = new FileInputStream(uploadFile);

                    if (uploadFile.getName().endsWith(".pro")){
                        byte[] buffer = new byte[1024];
                        int length = Integer.valueOf(position);
                        int sendLen;

                        while ((sendLen = inputStream.read(buffer)) != -1){
                            outStream.write(buffer, 0, sendLen);
                            length += sendLen;
                        }

                        if (length == uploadFile.length())
                            Log.d("Emilio", "配置文件长度匹配，发送成功");

                    }else {
                        byte[] encryptBuffer = new byte[1024];
                        int cryptLen;

                        CipherInputStream cipherInputStream = new CipherInputStream(
                                inputStream, encryption.cipherSet(Cipher.ENCRYPT_MODE));
                        while ((cryptLen = cipherInputStream.read(encryptBuffer)) != -1) {
                            outStream.write(encryptBuffer, 0, cryptLen);
                        }

                        cipherInputStream.close();
                    }

                    Date curDate = new Date(System.currentTimeMillis());
                    String senddate	= timeFormat.format(curDate);

                    Intent intent = new Intent();
                    Bundle bundle = new Bundle();
                    bundle.putString("senddate", senddate);
                    bundle.putString("sendfilename", uploadFile.getAbsolutePath());

                    intent.putExtras(bundle);
                    intent.setAction(chatActivity.SENDMSG);
                    chatActivity.local.sendBroadcast(intent);

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
        final List<Map<String, Object>> newList = new ArrayList<>();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                File f = new File(filePath);
                File[] files = f.listFiles();
                for (File file: files) {
                    if(!file.getName().startsWith(".")){
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", file.getName());
                        map.put("path", file.getAbsolutePath());
                        map.put("flag", false);
                        if (num == 2) {
                            map.put("thumbnail", getThumbnail.decodeFile(file));
                        }
                        newList.add(map);
                    }
                }
                decode = true;
            }
        });
        thread.start();

        while (!decode){
            try {
                Thread.sleep(100);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        decode = false;
        return newList;
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