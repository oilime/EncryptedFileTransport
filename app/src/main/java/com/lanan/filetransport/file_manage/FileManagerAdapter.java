package com.lanan.filetransport.file_manage;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lanan.filetransport.R;
import com.lanan.filetransport.utils.GetThumbnail;

import java.io.File;
import java.util.List;
import java.util.Map;

class FileManagerAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private List<Map<String, Object>> mlist;
    private Boolean image;
    private int which;

    private int[] resId = new int[]{R.drawable.music,R.drawable.video,
                                    R.drawable.image, R.drawable.doc};

    FileManagerAdapter(Context context, List<Map<String, Object>> dataList) {
        mInflater = LayoutInflater.from(context);
        this.mlist = dataList;
        this.image = FileManager.image;
        this.which = FileManager.num;
    }

    @Override
    public int getCount()
    {
        return mlist.size();
    }

    @Override
    public Object getItem(int position)
    {
        return mlist.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        final ViewHolder holder;

        if(convertView == null) {
            if (!image) {
                convertView = mInflater.inflate(R.layout.adapter_regular, null);
                holder = new ViewHolder();
                holder.text = (TextView) convertView.findViewById(R.id.file_name);
                holder.thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);
                holder.check = (ImageView) convertView.findViewById(R.id.rcheck);
            } else {
                convertView = mInflater.inflate(R.layout.adapter_img, null);
                holder = new ViewHolder();
                holder.thumbnail = (ImageView) convertView.findViewById(R.id.thumbn);
                holder.check = (ImageView) convertView.findViewById(R.id.scheck);
            }
            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        File f=new File((String) mlist.get(position).get("path"));
        String filename = f.getName();

        if (!image){
            holder.text.setText(filename);
            holder.thumbnail.setImageResource(resId[which%4]);
            holder.check.setVisibility(View.VISIBLE);
        } else {
            if (!mlist.get(position).containsKey("thumbnail")){
                Bitmap bitmap = GetThumbnail.decodeFile(f);
                mlist.get(position).put("thumbnail", bitmap);
                holder.thumbnail.setImageBitmap(bitmap);
            } else {
                holder.thumbnail.setImageBitmap((Bitmap) mlist.get(position).get("thumbnail"));
            }
        }

        holder.check.setImageResource(R.drawable.selected);
        if ((Boolean) mlist.get(position).get("flag")){
            holder.check.setVisibility(View.VISIBLE);
        }else {
            holder.check.setVisibility(View.GONE);
        }

        return convertView;
    }

    private class ViewHolder {
        TextView text;
        ImageView thumbnail;
        ImageView check;
    }
}
