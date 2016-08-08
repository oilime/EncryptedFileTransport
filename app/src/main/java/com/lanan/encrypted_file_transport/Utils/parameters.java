package com.lanan.encrypted_file_transport.Utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

import java.text.SimpleDateFormat;
import java.util.Locale;

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

    public static final String KEY_MANAGER = "X509";
    public static final String KEY_STORE_PWD = "88283288djy";

    public static SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);

    private static final String BAR_COLOR = "#3f51b5";  //"#2e40a4"
    
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
    
    public static void setStatusBarColor(Activity activity, boolean isAbove) {
        if (isAbove) {
            Window window = activity.getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor(BAR_COLOR));

            ViewGroup mContentView = (ViewGroup) activity.findViewById(Window.ID_ANDROID_CONTENT);
            View mChildView = mContentView.getChildAt(0);
            if (mChildView != null) {
                ViewCompat.setFitsSystemWindows(mChildView, true);
            }
        }else {
            Window window = activity.getWindow();
            ViewGroup mContentView = (ViewGroup) activity.findViewById(Window.ID_ANDROID_CONTENT);

            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            int statusBarHeight = parameters.getStatusBarHeight(activity);

            View mChildView = mContentView.getChildAt(0);
            if (mChildView != null) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mChildView.getLayoutParams();
                if (lp != null && lp.topMargin < statusBarHeight && lp.height != statusBarHeight) {
                    ViewCompat.setFitsSystemWindows(mChildView, false);
                    lp.topMargin += statusBarHeight;
                    mChildView.setLayoutParams(lp);
                }
            }

            View statusBarView = mContentView.getChildAt(0);
            if (statusBarView != null && statusBarView.getLayoutParams() != null
                    && statusBarView.getLayoutParams().height == statusBarHeight) {
                statusBarView.setBackgroundColor(Color.parseColor(BAR_COLOR));
                return;
            }
            statusBarView = new View(activity);
            ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, statusBarHeight);
            statusBarView.setBackgroundColor(Color.parseColor(BAR_COLOR));
            mContentView.addView(statusBarView, 0, lp);
        }
    }
}
