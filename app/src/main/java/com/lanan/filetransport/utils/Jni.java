package com.lanan.filetransport.utils;

import android.util.Log;

import com.lanan.filetransport.main.MainActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.lanan.filetransport.utils.Parameters.mainPath;
import static com.lanan.filetransport.utils.Parameters.newFormat;

public class Jni {
    static {
        System.loadLibrary("jni");
    }

    private ArrayList<Map<String, Object>> dataList;

    public Jni() {
        super();
    }

    public Jni(ArrayList<Map<String, Object>> dataList) {
        this.dataList = dataList;
    }

    private synchronized void addItem(Map<String, Object> map) {
        dataList.add(map);
    }

    public synchronized ArrayList<Map<String, Object>> getList() {
        return dataList;
    }

    public synchronized int getLength() {
        return dataList.size();
    }

    @SuppressWarnings("unused")
    public void newFileRecv(String filename, String filepath, String ip) {
        Map<String, Object> map = new HashMap<>();
        String recvName = "";
        String appHome;

        for (int i = 0; i < MainActivity.dataList.size(); i++) {
            for (int j = 0; j < MainActivity.dataList.get(i).size(); j++) {
                String testIp = (String) MainActivity.dataList.get(i).get(j).get("ip");
                if (testIp.equals(ip)) {
                    recvName = (String) MainActivity.dataList.get(i).get(j).get("name");
                    break;
                }
            }
        }
        if (filename.endsWith(".pro")) {
            appHome = mainPath + "/.config/";
        } else if (!recvName.isEmpty()) {
            appHome = mainPath + "/FileTransport/" + recvName;
        } else {
            appHome = mainPath + "/FileTransport/unknown_user";
        }

        File dir = new File(appHome);
        File file = new File(dir, filename);
        if (file.exists() && !filename.endsWith(".pro")) {
            filename = filename.substring(0, filename.indexOf("."))
                    + dir.listFiles().length + filename.substring(filename.indexOf("."));
            file = new File(dir, filename);
        }

        Date curDate = new Date(System.currentTimeMillis());
        String recvdate = newFormat.format(curDate);
        map.put("recvdate", recvdate);
        map.put("hostip", ip);

        File recvFile = null;
        try {
            recvFile = new File(filepath);
            if (recvFile.exists()) {
                boolean f = recvFile.renameTo(file);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (recvFile != null) {
            map.put("recvfilename", file.getAbsolutePath());
            Log.d("Emilio", "recv:" + recvdate + ", " + ip + ", " + file.getAbsolutePath());
        }
        addItem(map);
    }

    public native int server_set_socket(int port, String certPath, String caPath);

    public native void set_stop();

    public native int client_send_file(String serverip, int port, String caPath, String filepath, String hostip);
}
