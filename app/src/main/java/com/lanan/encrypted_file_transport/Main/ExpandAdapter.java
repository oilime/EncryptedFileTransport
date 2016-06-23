package com.lanan.encrypted_file_transport.Main;

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

/**
 * Created by lanan on 16-5-18.
 */
public class ExpandAdapter extends BaseExpandableListAdapter {

    private List<List<Map<String, Object>>> mlist;
    private LayoutInflater mInflater;

    private String[] group_title_arry = new String[] {"一院","二院","三院","四院","五院","六院","七院","八院"};

    public ExpandAdapter(Context context, List<List<Map<String, Object>>> mylist){
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

        TextView groupname = (TextView) convertView.findViewById(R.id.tarname);
        ImageView groupicon = (ImageView) convertView.findViewById(R.id.groupicon);
        if (isExpanded){
            groupicon.setImageResource(R.drawable.ic_arrow_down_12dp);
        } else {
            groupicon.setImageResource(R.drawable.ic_arrow_right_12dp);
        }
        groupname.setText(group_title_arry[groupPosition]);

        return convertView;
    }

    /**
     * 对一级标签下的二级标签进行设置
     */
    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        convertView = mInflater.inflate(R.layout.childadapter, null);

        TextView childname = (TextView) convertView.findViewById(R.id.childtext);

        String name = (String) mlist.get(groupPosition).get(childPosition).get("name");

        childname.setText(name);

        return convertView;
    }

    /**
     * 当选择子节点的时候，调用该方法
     */
    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
}
