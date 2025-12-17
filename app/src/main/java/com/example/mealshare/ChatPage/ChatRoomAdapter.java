package com.example.mealshare.ChatPage; // ⚠️ Check package name

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealshare.ChatPage.ChatMessageModel;
import com.example.mealshare.R;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ViewHolder> {

    private Context context;
    private List<ChatMessageModel> messageList;
    private String currentUserId;

    public ChatRoomAdapter(Context context, List<ChatMessageModel> messageList) {
        this.context = context;
        this.messageList = messageList;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message_bubble, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatMessageModel msg = messageList.get(position);

        // 🔥 LOGIC: Check who sent the message
        if (msg.getSenderId().equals(currentUserId)) {
            // MY MESSAGE: Show Right Bubble, Hide Left
            holder.sentLayout.setVisibility(View.VISIBLE);
            holder.receiveLayout.setVisibility(View.GONE);
            holder.sentTv.setText(msg.getMessage());
        } else {
            // THEIR MESSAGE: Show Left Bubble, Hide Right
            holder.sentLayout.setVisibility(View.GONE);
            holder.receiveLayout.setVisibility(View.VISIBLE);
            holder.receiveTv.setText(msg.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout sentLayout, receiveLayout;
        TextView sentTv, receiveTv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Match these IDs with item_message_bubble.xml
            sentLayout = itemView.findViewById(R.id.layout_sent);
            receiveLayout = itemView.findViewById(R.id.layout_receive);
            sentTv = itemView.findViewById(R.id.tv_msg_sent);
            receiveTv = itemView.findViewById(R.id.tv_msg_receive);
        }
    }
}