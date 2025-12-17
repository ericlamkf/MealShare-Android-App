package com.example.mealshare.ChatPage;

import android.content.Context;
import android.text.format.DateFormat;
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

        String formattedTime = "Now";
        if (msg.getTimestamp() > 0) {
            // Use 'android.text.format.DateFormat' for easy formatting
            formattedTime = DateFormat.format("hh:mm a", msg.getTimestamp()).toString();
        }

        if (msg.getSenderId() != null && msg.getSenderId().equals(currentUserId)) {
            // --- MY MESSAGE (Right Side) ---
            holder.sentLayout.setVisibility(View.VISIBLE);
            holder.receiveLayout.setVisibility(View.GONE);

            holder.sentTv.setText(msg.getMessage());

            // Set the time on the RIGHT text view
            holder.timeSent.setText(formattedTime);
            holder.timeSent.setVisibility(View.VISIBLE);

        } else {
            // --- THEIR MESSAGE (Left Side) ---
            holder.sentLayout.setVisibility(View.GONE);
            holder.receiveLayout.setVisibility(View.VISIBLE);

            holder.receiveTv.setText(msg.getMessage());

            // Set the time on the LEFT text view
            holder.timeReceive.setText(formattedTime);
            holder.timeReceive.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout sentLayout, receiveLayout;
        TextView sentTv, receiveTv;
        TextView timeReceive, timeSent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Match these IDs with item_message_bubble.xml
            sentLayout = itemView.findViewById(R.id.layout_sent);
            receiveLayout = itemView.findViewById(R.id.layout_receive);
            sentTv = itemView.findViewById(R.id.tv_msg_sent);
            receiveTv = itemView.findViewById(R.id.tv_msg_receive);

            timeReceive = itemView.findViewById(R.id.text_message_time_left);
            timeSent = itemView.findViewById(R.id.text_message_time_right);
        }
    }
}