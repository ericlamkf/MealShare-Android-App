package com.example.mealshare;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SharedViewModel extends ViewModel {

    // MutableLiveData: Used internally in the ViewModel for changing the data
    // value.
    // Initialized to the 'Fetching location...' status, which HomeFragment will
    // display first.
    private final MutableLiveData<String> liveLocation = new MutableLiveData<>("Fetching location...");

    // LiveData for Request Status (Success / Error messages)
    private final MutableLiveData<String> requestStatus = new MutableLiveData<>();

    // LiveData for Caching User Name
    private final MutableLiveData<String> currentUserName = new MutableLiveData<>();

    private final FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // LiveData: Exposed publicly. Fragments observe this read-only version.
    public LiveData<String> getLocation() {
        return liveLocation;
    }

    public LiveData<String> getRequestStatus() {
        return requestStatus;
    }

    public LiveData<String> getCurrentUserName() {
        return currentUserName;
    }

    // LiveData for Login Status
    private final MutableLiveData<String> loginStatus = new MutableLiveData<>();

    public LiveData<String> getLoginStatus() {
        return loginStatus;
    }

    /**
     * Called by HomeFragment to deposit the new, fetched location address.
     * This automatically notifies all observing fragments (like AddFragment).
     * 
     * @param address The human-readable address string (e.g., "17, SS 2/73,
     *                Petaling Jaya").
     */
    public void setLocation(String address) {
        liveLocation.setValue(address);
    }

    // Task 2: Implement Request Logic
    public void requestFood(String mealId, String donorId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Validation 1: Not Logged In
        if (currentUser == null) {
            requestStatus.setValue("Error: You must be logged in to request food.");
            return;
        }

        String currentUserId = currentUser.getUid();

        // Validation 2: Self-Request
        if (currentUserId.equals(donorId)) {
            requestStatus.setValue("Error: You cannot request your own food.");
            return;
        }

        // Action: Update Firestore
        db.collection("meals").document(mealId)
                .update("status", "Reserved", "receiverId", currentUserId)
                .addOnSuccessListener(aVoid -> {
                    requestStatus.setValue("Success: Food requested successfully!");
                })
                .addOnFailureListener(e -> {
                    requestStatus.setValue("Error: Failed to request food. " + e.getMessage());
                });
    }

    // Task 2: Fetch Current User Name (Cache it)
    public void fetchCurrentUserName() {
        if (currentUserName.getValue() != null)
            return; // Already cached

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                String name = document.getString("name");
                                currentUserName.setValue(name);
                            }
                        }
                    });
        }
    }

    // Task 4: Auth Logic
    public void loginUser(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            loginStatus.setValue("Error: Empty Fields Are not Allowed");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    loginStatus.setValue("Success: Login Successful");
                })
                .addOnFailureListener(e -> {
                    loginStatus.setValue("Error: Login Failed. " + e.getMessage());
                });
    }
}