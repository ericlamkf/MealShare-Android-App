package com.example.mealshare.HomePage; // Check your package name

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.List;

public class Meal implements Serializable {
    private String foodName;
    private String description;
    private String quantity;
    private String location;
    private String imageUrl;
    private String userId;
    private List<String> tags;
    private Timestamp expiryTime; // Firestore Timestamp
    private Timestamp timestamp; // Creation time

    private String status; // Available or Reserved
    private String receiverId; // Who claimed it
    private String donorId; // Who uploaded it

    @com.google.firebase.firestore.DocumentId
    private String mealId; // Firestore Document ID (Auto-populated)

    // Empty constructor is REQUIRED for Firebase
    public Meal() {
        this.status = "Available"; // Default status
    }

    public Meal(String foodName, String description, String quantity, String location,
            String imageUrl, String userId, List<String> tags,
            Timestamp expiryTime, Timestamp timestamp, String donorId) {
        this.foodName = foodName;
        this.description = description;
        this.quantity = quantity;
        this.location = location;
        this.imageUrl = imageUrl;
        this.userId = userId;
        this.tags = tags;
        this.expiryTime = expiryTime;
        this.timestamp = timestamp;
        this.donorId = donorId;
        this.status = "Available";
        this.receiverId = null;
    }

    // Getters are REQUIRED for Firebase
    public String getFoodName() {
        return foodName;
    }

    public String getDescription() {
        return description;
    }

    public String getQuantity() {
        return quantity;
    }

    public String getLocation() {
        return location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getUserId() {
        return userId;
    }

    public List<String> getTags() {
        return tags;
    }

    public Timestamp getExpiryTime() {
        return expiryTime;
    }

    // New Getters
    public String getStatus() {
        return status;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getDonorId() {
        return donorId;
    }

    // Setters (Added for flexibility)
    public void setStatus(String status) {
        this.status = status;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public void setDonorId(String donorId) {
        this.donorId = donorId;
    }

    public String getMealId() {
        return mealId;
    }

    public void setMealId(String mealId) {
        this.mealId = mealId;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }
}