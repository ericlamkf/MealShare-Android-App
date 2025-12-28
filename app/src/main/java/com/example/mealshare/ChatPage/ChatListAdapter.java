package com.example.mealshare.ChatPage;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.mealshare.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {

    private Context context;
    private List<ChatListModel> chatList;
    private String myUid;

    public ChatListAdapter(Context context, List<ChatListModel> chatList) {
        this.context = context;
        this.chatList = chatList;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_chat_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChatListModel chat = chatList.get(position);

        holder.foodNameTv.setText("• " + chat.getFoodName());
        holder.lastMessageTv.setText(chat.getLastMessage());

        String time = DateFormat.format("dd/MM hh:mm a", chat.getLastMessageTime()).toString();
        holder.timeTv.setText(time);

        String otherUserId = null;
        if (chat.getParticipants() != null) {
            for (String id : chat.getParticipants()) {
                if (myUid != null && !id.equals(myUid)) {
                    otherUserId = id;
                    break;
                }
            }
        }

        if (otherUserId != null) {
            FirebaseFirestore.getInstance().collection("users").document(otherUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            holder.userNameTv.setText(name != null ? name : "Unknown User");

                            String photoUrl = documentSnapshot.getString("profileImageUrl");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Glide.with(context)
                                        .load(photoUrl)
                                        .placeholder(R.drawable.ic_launcher_background)
                                        .circleCrop()
                                        .into(holder.avatarIv);
                            }
                        }
                    });
        } else {
            holder.userNameTv.setText("MealShare User");
        }

        long myUnreadCount = 0;
        if (chat.getUnreadCounts() != null && myUid != null && chat.getUnreadCounts().containsKey(myUid)) {
            Long count = chat.getUnreadCounts().get(myUid);
            if (count != null) myUnreadCount = count;
        }

        if (myUnreadCount > 0) {
            holder.unreadBadgeTv.setText(String.valueOf(myUnreadCount));
            holder.unreadBadgeTv.setVisibility(View.VISIBLE);
            holder.lastMessageTv.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.lastMessageTv.setTextColor(context.getResources().getColor(android.R.color.black));
        } else {
            holder.unreadBadgeTv.setVisibility(View.GONE);
            holder.lastMessageTv.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.lastMessageTv.setTextColor(android.graphics.Color.parseColor("#757575"));
        }

        String finalOtherUserId = otherUserId;
        holder.itemView.setOnClickListener(v -> {
            if (chat.getRequestId() == null || chat.getRequestId().isEmpty()) {
                Toast.makeText(context, "Error: Chat ID is missing.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (finalOtherUserId == null) {
                Toast.makeText(context, "Error: Could not determine the other user.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Fetch user details on click to prevent race condition
            FirebaseFirestore.getInstance().collection("users").document(finalOtherUserId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String otherUserName = documentSnapshot.getString("name");

                            Intent intent = new Intent(context, ChatRoomActivity.class);
                            intent.putExtra("requestId", chat.getRequestId());
                            intent.putExtra("otherUserId", finalOtherUserId);
                            intent.putExtra("foodName", chat.getFoodName());
                            intent.putExtra("otherUserName", otherUserName); // Use the fetched name
                            context.startActivity(intent);
                        } else {
                            Toast.makeText(context, "Could not find user details.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to get user details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarIv;
        TextView userNameTv, foodNameTv, lastMessageTv, timeTv;
        TextView unreadBadgeTv;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarIv = itemView.findViewById(R.id.iv_chat_avatar);
            userNameTv = itemView.findViewById(R.id.tv_chat_user_name);
            foodNameTv = itemView.findViewById(R.id.tv_chat_food_name);
            lastMessageTv = itemView.findViewById(R.id.tv_chat_last_msg);
            timeTv = itemView.findViewById(R.id.tv_chat_time);
            unreadBadgeTv = itemView.findViewById(R.id.tv_unread_count);
        }
    }
}
