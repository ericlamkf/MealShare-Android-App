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
    private Timestamp timestamp;  // Creation time

    // Empty constructor is REQUIRED for Firebase
    public Meal() {}

    // Getters are REQUIRED for Firebase
    public String getFoodName() { return foodName; }
    public String getDescription() { return description; }
    public String getQuantity() { return quantity; }
    public String getLocation() { return location; }
    public String getImageUrl() { return imageUrl; }
    public String getUserId() { return userId; }
    public List<String> getTags() { return tags; }
    public Timestamp getExpiryTime() { return expiryTime; }
}