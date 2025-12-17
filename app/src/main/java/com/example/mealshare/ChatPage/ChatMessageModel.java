package com.example.mealshare.ChatPage; // ⚠️ Check package

public class ChatMessageModel {
    private String senderId;
    private String message;
    private long timestamp;

    public ChatMessageModel() {} // Required for Firestore

    public ChatMessageModel(String senderId, String message, long timestamp) {
        this.senderId = senderId;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getSenderId() { return senderId; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}