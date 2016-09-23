package com.lanan.encrypted_file_transport.services;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.File;

public class UploadLogService {
	private DatabaseOpenHelper databaseOpenHelper;
	
	public UploadLogService(Context context){
		this.databaseOpenHelper = new DatabaseOpenHelper(context);
	}
	
	public void save(String sourceid, File uploadFile){
		SQLiteDatabase db = databaseOpenHelper.getWritableDatabase();
		db.execSQL("insert into uploadlog(uploadfilepath, sourceid) values(?,?)",
				new Object[]{uploadFile.getAbsolutePath(),sourceid});
	}
	
	public String getBindId(File uploadFile){
		SQLiteDatabase db = databaseOpenHelper.getReadableDatabase();
		Cursor cursor = db.rawQuery("select sourceid from uploadlog where uploadfilepath=?", 
				new String[]{uploadFile.getAbsolutePath()});
		if(cursor.moveToFirst()){
            String ret = cursor.getString(0);
            cursor.close();
			return ret;
        }
		return null;
	}
}
