package com.lanan.encrypted_file_transport.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.lanan.encrypted_file_transport.R;

import java.util.List;
import java.util.Map;

public class MainAdapter extends BaseExpandableListAdapter {

    private List<List<Map<String, Object>>> mlist;
    private LayoutInflater mInflater;

    private String[] group_title_array = new String[] {"一院","二院","三院","四院"};

    public MainAdapter(Context context, List<List<Map<String, Object>>> mylist){
        this.mlist = mylist;
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getGroupCount() {
        return mlist.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mlist.get(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mlist.get(groupPosition).size();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mlist.get(groupPosition).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {

        convertView = mInflater.inflate(R.layout.adapter, null);

        TextView groupName = (TextView) convertView.findViewById(R.id.tarname);
        ImageView groupIcon = (ImageView) convertView.findViewById(R.id.groupicon);
        if (isExpanded){
            groupIcon.setImageResource(R.drawable.ic_arrow_down_12dp);
        } else {
            groupIcon.setImageResource(R.drawable.ic_arrow_right_12dp);
        }
        groupName.setText(group_title_array[groupPosition]);

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        convertView = mInflater.inflate(R.layout.childadapter, null);
        TextView childName = (TextView) convertView.findViewById(R.id.childtext);
        String name = (String) mlist.get(groupPosition).get(childPosition).get("name");
        childName.setText(name);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
