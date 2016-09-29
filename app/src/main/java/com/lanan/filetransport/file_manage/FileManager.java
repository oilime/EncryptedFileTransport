package com.lanan.filetransport.file_manage;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
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

import com.lanan.filetransport.R;
import com.lanan.filetransport.file_transport.ChatActivity;
import com.lanan.filetransport.main.MainActivity;
import com.lanan.filetransport.utils.GetThumbnail;
import com.lanan.filetransport.utils.Jni;
import com.lanan.filetransport.utils.Parameters;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileManager extends AppCompatActivity {

    private ListView listView;
    private GridView gridView;

    private String rootPath = null;
    private String pathName = null;
    private static String hostip = null;
    private static String serverip = null;

    public static Boolean image = false;
    private static FileManagerAdapter adapter = null;

    public static int num;
    private static Boolean decode = false;
    private static List<Map<String, Object>> mdatalist = null;

    private SimpleDateFormat timeFormat = Parameters.newFormat;

    private int ret;

    ProgressDialog pDialog;
    TextView mPath;
    ImageView backButton;
    Button sendButton;
    LinearLayout cancel;
    LinearLayout selectAll;

    @Override
    protected void onCreate(Bundle test) {
        super.onCreate(test);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Bundle pathBundle = getIntent().getExtras();
        rootPath = pathBundle.getString("path");
        pathName = pathBundle.getString("name");
        image = pathBundle.getBoolean("flag");
        serverip = pathBundle.getString("serverip");
        hostip = pathBundle.getString("hostip");
        mdatalist = new ArrayList<>();

        switch (pathName) {
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

        mdatalist.clear();
        mdatalist = getFileDir(ChatActivity.dir[num]);

        initViews();
    }

    private void initViews() {
        if (image) {
            setContentView(R.layout.showfile_img);
            gridView = (GridView) findViewById(R.id.mygrid);

            cancel = (LinearLayout) findViewById(R.id.img_cancel);
            selectAll = (LinearLayout) findViewById(R.id.img_selectall);
            backButton = (ImageView) findViewById(R.id.img_backbutton);
            sendButton = (Button) findViewById(R.id.img_sendbutton);
            mPath = (TextView) findViewById(R.id.img_title);
        } else {
            setContentView(R.layout.showfile_regular);
            listView = (ListView) findViewById(R.id.mylist);

            cancel = (LinearLayout) findViewById(R.id.reg_cancel);
            selectAll = (LinearLayout) findViewById(R.id.reg_selectall);
            backButton = (ImageView) findViewById(R.id.reg_backbutton);
            sendButton = (Button) findViewById(R.id.reg_sendbutton);
            mPath = (TextView) findViewById(R.id.reg_title);
        }

        pDialog = new ProgressDialog(this);
        pDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pDialog.setCanceledOnTouchOutside(false);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < mdatalist.size(); i++)
                    mdatalist.get(i).put("flag", false);
                adapter.notifyDataSetChanged();
            }
        });

        selectAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < mdatalist.size(); i++)
                    mdatalist.get(i).put("flag", true);
                adapter.notifyDataSetChanged();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileManager.this.finish();
            }
        });

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < mdatalist.size(); i++) {
                    boolean isSelected = (boolean) mdatalist.get(i).get("flag");
                    if (isSelected) {
                        File file = new File((String) mdatalist.get(i).get("path"));
                        if (file.exists()) {
                            uploadFile(file);
                        } else {
                            Toast.makeText(FileManager.this, "所选文件不存在", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
                Intent goIntent = new Intent();
                goIntent.setClass(FileManager.this, ChatActivity.class);
                startActivity(goIntent);
                FileManager.this.finish();
            }
        });

        if (image) {
            gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    boolean select = (boolean) mdatalist.get(position).get("flag");
                    mdatalist.get(position).put("flag", !select);
                    adapter.notifyDataSetChanged();
                }
            });
        } else {
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

        adapter = new FileManagerAdapter(this, mdatalist);

        if (image) {
            gridView.setAdapter(adapter);
        } else {
            listView.setAdapter(adapter);
        }

        Parameters.setStatusBarColor(this, MainActivity.isAbove);
    }

    private void uploadFile(final File uploadFile) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Jni client = new Jni();
                    ret = client.client_send_file(serverip, 1223,
                            Environment.getExternalStorageDirectory().getAbsolutePath() + "/rsacert/rootca.pem",
                            uploadFile.getAbsolutePath(), hostip);
                    if (ret == 0) {
                        Date curDate = new Date(System.currentTimeMillis());
                        String senddate = timeFormat.format(curDate);

                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putString("senddate", senddate);
                        bundle.putString("sendfilename", uploadFile.getAbsolutePath());

                        intent.putExtras(bundle);
                        intent.setAction(ChatActivity.SENDMSG);
                        ChatActivity.local.sendBroadcast(intent);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
                for (File file : files) {
                    if (!file.getName().startsWith(".")) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("name", file.getName());
                        map.put("path", file.getAbsolutePath());
                        map.put("flag", false);
                        if (num == 2) {
                            map.put("thumbnail", GetThumbnail.decodeFile(file));
                        }
                        newList.add(map);
                    }
                }
                decode = true;
            }
        });
        thread.start();

        while (!decode) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        decode = false;
        return newList;
    }

    private void cleanFlag() {
        for (int i = 0; i < mdatalist.size(); i++)
            mdatalist.get(i).put("flag", false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanFlag();
    }
}