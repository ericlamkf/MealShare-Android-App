package com.example.mealshare;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.DocumentReference;
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

    public void clearRequestStatus() {
        requestStatus.setValue(null);
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

    // Task 2: Implement Request Logic (Safely with Transaction)
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

        // Action: Run Database Transaction
        com.google.firebase.firestore.DocumentReference mealRef = db.collection("meals").document(mealId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(mealRef);

            // 1. Check if Meal Exists
            if (!snapshot.exists()) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Meal does not exist!",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }

            // 2. Check Availability (Race Condition Check)
            String status = snapshot.getString("status");
            if ("Reserved".equals(status)) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Sorry, this meal is already reserved!",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }

            // 3. Handle Quantity
            String qtyStr = snapshot.getString("quantity");
            int newQty = 0;
            boolean isFullyReserved = true;

            try {
                // Try to parse quantity as number
                int currentQty = Integer.parseInt(qtyStr != null ? qtyStr : "0");
                if (currentQty > 0) {
                    newQty = currentQty - 1;
                    transaction.update(mealRef, "quantity", String.valueOf(newQty));

                    if (newQty > 0) {
                        isFullyReserved = false; // Still available for others
                    }
                }
            } catch (NumberFormatException e) {
                // If quantity is text (e.g. "5 kg"), we assume single unit and reserve it
                // fully.
                isFullyReserved = true;
            }

            // 4. Update Status & Receiver
            if (isFullyReserved) {
                transaction.update(mealRef, "status", "Reserved");
            }

            // We always Record the receiver.
            // Note: If multiple people claim parts, this field overwrites the previous
            // receiver.
            // For a proper system, we'd need a subcollection 'requests'.
            // For now, tracking the *latest* requester on the main doc is acceptable for
            // MVP.
            transaction.update(mealRef, "receiverId", currentUserId);

            return null;
        }).addOnSuccessListener(aVoid -> requestStatus.setValue("Success: Food requested successfully!"))
                .addOnFailureListener(e -> requestStatus.setValue("Error: " + e.getMessage()));
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

    // Task: Implement Cancel Request Logic
    public void cancelRequest(String mealId) {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            requestStatus.setValue("Error: You must be logged in.");
            return;
        }

        String currentUserId = currentUser.getUid();
        DocumentReference mealRef = db.collection("meals").document(mealId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(mealRef);

            if (!snapshot.exists()) {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "Meal not found.",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }

            // 1. Verify Ownership of Request
            String receiverId = snapshot.getString("receiverId");
            if (receiverId == null || !receiverId.equals(currentUserId)) {
                // Note: In a multi-quantity scenario with a single receiverId field,
                // this only allows the *last* person to cancel.
                // This is a known limitation of the current data model.
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                        "You don't have an active request for this meal.",
                        com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }

            // 2. Increment Quantity
            String qtyStr = snapshot.getString("quantity");
            int newQty = 1; // Default if parsing fails
            try {
                int currentQty = Integer.parseInt(qtyStr != null ? qtyStr : "0");
                newQty = currentQty + 1;
                transaction.update(mealRef, "quantity", String.valueOf(newQty));
            } catch (NumberFormatException e) {
                // If text (e.g. "5 boxes"), we can't increment mathematically.
                // We just ensure it's available.
            }

            // 3. Update Status
            // Since we are cancelling, there is now at least 1 item (the one we returned).
            // So status MUST be Available.
            transaction.update(mealRef, "status", "Available");

            // 4. Clear Receiver
            // We clear it so the button toggles back.
            // Warning: If there were other requesters, this Field doesn't track them.
            transaction.update(mealRef, "receiverId", null);

            return null;
        }).addOnSuccessListener(aVoid -> requestStatus.setValue("Success: Request canceled."))
                .addOnFailureListener(e -> requestStatus.setValue("Error: " + e.getMessage()));
    }
}