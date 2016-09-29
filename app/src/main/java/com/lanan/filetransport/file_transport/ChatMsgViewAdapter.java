package com.lanan.filetransport.file_transport;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.lanan.filetransport.R;

import java.util.List;

class ChatMsgViewAdapter extends BaseAdapter {

	interface IMsgViewType {
		int COM_MSG = 0;						// 收到对方的消息
		int TO_MSG = 1;							// 自己发送出去的消息
	}

	private static final int ITEM_COUNT = 2;	// 消息类型的总数
	private List<ChatMsgEntity> coll;			// 消息对象数组
	private LayoutInflater mInflater;

	ChatMsgViewAdapter(Context context, List<ChatMsgEntity> coll) {
		this.coll = coll;
		mInflater = LayoutInflater.from(context);
	}

	public int getCount() {
		return coll.size();
	}

	public Object getItem(int position) {
		return coll.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public int getItemViewType(int position) {
		ChatMsgEntity entity = coll.get(position);

		if (entity.getMsgType()) {				//收到的消息
			return IMsgViewType.COM_MSG;
		} else {								//自己发送的消息
			return IMsgViewType.TO_MSG;
		}
	}

	public int getViewTypeCount() {
		return ITEM_COUNT;
	}

	public View getView(int position, View convertView, ViewGroup parent) {

		ChatMsgEntity entity = coll.get(position);
		final boolean isComMsg = entity.getMsgType();

		ViewHolder viewHolder;
		if (convertView == null) {
			if (isComMsg) {
				convertView = mInflater.inflate(R.layout.msg_left, null);
			} else {
				convertView = mInflater.inflate(R.layout.msg_right, null);
			}

			viewHolder = new ViewHolder();
			viewHolder.tvSendTime = (TextView) convertView.findViewById(R.id.tv_sendtime);
			viewHolder.tvContent = (TextView) convertView.findViewById(R.id.tv_chatcontent);
			viewHolder.isComMsg = isComMsg;

			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		viewHolder.tvSendTime.setText(entity.getDate());

		final String filepath = entity.getMessage();
		String[] items = filepath.split("/");
		int len = items.length;
		final String filename = items[len - 1];
		viewHolder.tvContent.setText(filename);

		viewHolder.tvContent.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.putExtra("filename", filepath);
				if (isComMsg) {
					intent.setAction(ChatActivity.DECRYPTFILE);
				}else {
					intent.setAction(ChatActivity.FILE);
				}
				ChatActivity.local.sendBroadcast(intent);
			}
		});

		return convertView;
	}

	private static class ViewHolder {
		TextView tvSendTime;
		TextView tvContent;
		boolean isComMsg = true;
	}
}
