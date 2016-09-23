package com.lanan.encrypted_file_transport.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileInputStream;

public class getThumbnail {

    public static Bitmap decodeFile(File f){
        try {
            BitmapFactory.Options bfOptions = new BitmapFactory.Options();
            bfOptions.inDither = false;
            bfOptions.inTempStorage = new byte[32 * 1024];
            bfOptions.inJustDecodeBounds = true;
            bfOptions.inSampleSize = 1;

            FileInputStream fs = new FileInputStream(f);
            BitmapFactory.decodeFileDescriptor(fs.getFD(), null, bfOptions);

            final int REQUIRED_SIZE = 320;
            int scale = 1;
            while (bfOptions.outWidth/scale >= REQUIRED_SIZE && bfOptions.outHeight/scale >= REQUIRED_SIZE)
                scale *= 2;

            BitmapFactory.Options bfOptions2 = new BitmapFactory.Options();
            bfOptions2.inDither = false;
            bfOptions2.inTempStorage = new byte[32 * 1024];
            bfOptions2.inJustDecodeBounds = false;
            bfOptions2.inSampleSize = scale;

            fs = new FileInputStream(f);

            return BitmapFactory.decodeFileDescriptor(fs.getFD(), null, bfOptions2);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
