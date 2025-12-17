package com.example.mealshare.ChatPage; // ⚠️ Check your package name

import java.util.List;
import java.util.Map;

public class ChatListModel {
    private String foodName;
    private String lastMessage;
    private long lastMessageTime;
    private String requestId;
    private List<String> participants;
    private Map<String, Long> unreadCounts;

    // Empty constructor required for Firestore
    public ChatListModel() {}

    public ChatListModel(String foodName, String lastMessage, long lastMessageTime, String requestId, List<String> participants) {
        this.foodName = foodName;
        this.lastMessage = lastMessage;
        this.lastMessageTime = lastMessageTime;
        this.requestId = requestId;
        this.participants = participants;
    }

    public String getFoodName() { return foodName; }
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    public String getLastMessage() { return lastMessage; }
    public long getLastMessageTime() { return lastMessageTime; }
    public String getRequestId() { return requestId; }
    public List<String> getParticipants() { return participants; }
    public Map<String, Long> getUnreadCounts() { return unreadCounts; }
}