package com.example.mealshare;

public class Listing {
    private String food;
    private String location;
    private String picture;
    private int quantity;

    public Listing() {
        // Default constructor required for calls to DataSnapshot.getValue(Listing.class)
    }

    public String getFood() {
        return food;
    }

    public String getLocation() {
        return location;
    }

    public String getPicture() {
        return picture;
    }

    public int getQuantity() {
        return quantity;
    }
}
