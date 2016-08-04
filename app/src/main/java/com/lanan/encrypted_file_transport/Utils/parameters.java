package com.lanan.encrypted_file_transport.Utils;

import android.os.Environment;

public class parameters {

    public static final String mainPath = Environment.getExternalStorageDirectory().getPath()
            + "/alan/system/security/local/tmp/chs";
    public static final String musicDir = mainPath + "/演示音乐/";
    public static final String videoDir = mainPath + "/演示视频/";
    public static final String imageDir = mainPath + "/演示图片/";
    public static final String docDir = mainPath + "/演示文档/";
    public static final String tempDir = mainPath + "/FileTransport/temp/";

    public static final String DECRYPTFILE = "com.lanan.decryptfile";
    public static final String FILE = "com.lanan.openfile";
    public static final String RECVMSG = "com.lanan.recvmsg";
    public static final String SENDMSG = "com.lanan.sendmsg";
    
}
