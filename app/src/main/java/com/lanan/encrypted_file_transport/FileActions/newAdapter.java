package com.lanan.encrypted_file_transport.FileActions;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lanan.encrypted_file_transport.R;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by lanan on 16-4-21.
 */
public class newAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private List<Map<String, Object>> mlist;
    private Boolean image;
    private Boolean select;
    private int which;

    private int[] resId = new int[]{R.drawable.music,R.drawable.video,
                                    R.drawable.image, R.drawable.doc};

    public newAdapter(Context context, List<Map<String, Object>> datalist) {
        mInflater = LayoutInflater.from(context);
        this.mlist = datalist;
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
                holder.thunmbnail = (ImageView) convertView.findViewById(R.id.thumbnail);
                holder.check = (ImageView) convertView.findViewById(R.id.rcheck);
            } else {
                convertView = mInflater.inflate(R.layout.adapter_img, null);
                holder = new ViewHolder();
                holder.thunmbnail = (ImageView) convertView.findViewById(R.id.thumbn);
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
            holder.thunmbnail.setImageResource(resId[which%4]);
            holder.check.setVisibility(View.VISIBLE);
        } else {
            if (!mlist.get(position).containsKey("thumbnail")){
                Bitmap bitmap = thumbnail.decodeFile(f);
                mlist.get(position).put("thumbnail", bitmap);
                holder.thunmbnail.setImageBitmap(bitmap);
                Log.d("Emilio", "create thumbnail");
            } else {
                holder.thunmbnail.setImageBitmap((Bitmap) mlist.get(position).get("thumbnail"));
                Log.d("Emilio", "get thumbnail");
            }
        }

        holder.check.setImageResource(R.drawable.selected);
        select = (Boolean) mlist.get(position).get("flag");
        if (select){
            holder.check.setVisibility(View.VISIBLE);
        }else {
            holder.check.setVisibility(View.GONE);
        }

        return convertView;
    }

    public void refresh(List<Map<String, Object>> list) {
        this.mlist = list;
        notifyDataSetChanged();
    }

    private class ViewHolder {
        TextView text;
        ImageView thunmbnail;
        ImageView check;
    }
}
