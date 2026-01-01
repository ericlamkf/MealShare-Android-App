package com.example.mealshare.HomePage;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.mealshare.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MealDetailFragment extends Fragment {

    private Meal meal;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Button btnRequest, btnCancelRequest;
    private TextView donorNameTv, donorPhoneTv, donorRateTv, requestedQtyTv;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            meal = (Meal) getArguments().getSerializable("meal_data");
        }
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_meal_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageButton backButton = view.findViewById(R.id.btn_back);
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());

        // Initialize Views
        ImageView imageView = view.findViewById(R.id.detail_image);
        TextView nameTv = view.findViewById(R.id.detail_food_name);
        TextView locTv = view.findViewById(R.id.detail_location);
        TextView qtyTv = view.findViewById(R.id.detail_quantity);
        requestedQtyTv = view.findViewById(R.id.detail_requested_quantity);
        TextView descTv = view.findViewById(R.id.detail_description);
        donorNameTv = view.findViewById(R.id.detail_donor_name);
        donorPhoneTv = view.findViewById(R.id.detail_donor_phone);
        donorRateTv = view.findViewById(R.id.donor_feedback);
        btnRequest = view.findViewById(R.id.btn_request);
        btnCancelRequest = view.findViewById(R.id.btn_cancel_request);

        if (meal != null) {
            nameTv.setText(meal.getFoodName());
            locTv.setText("📍 " + meal.getLocation());
            qtyTv.setText(meal.getQuantity() + " available");
            requestedQtyTv.setText("(" + meal.getRequestedQuantity() + " requested)");
            descTv.setText(meal.getDescription());

            if (meal.getImageUrl() != null) {
                Glide.with(this).load(meal.getImageUrl()).into(imageView);
            }

            fetchDonorDetails(meal.getUserId());

            if (mAuth.getCurrentUser() != null) {
                checkExistingRequest();
            }

            checkStockAvailability();
        }

        btnRequest.setOnClickListener(v -> sendRequestToFirestore());
        btnCancelRequest.setOnClickListener(v -> cancelRequest());
    }

    // 1. Logic to check if user can request (based on stock vs requested count)
    private void checkStockAvailability() {
        if (meal == null || getContext() == null) return;
        int currentQty = 0;
        try {
            currentQty = Integer.parseInt(meal.getQuantity());
        } catch (NumberFormatException e) { /* default 0 */ }

        // If requested count meets or exceeds actual quantity, disable request button
        if (currentQty <= 0 || meal.getRequestedQuantity() >= currentQty || "Out of stock".equalsIgnoreCase(meal.getStatus())) {
            btnRequest.setText("Out of Stock");
            btnRequest.setEnabled(false);
            btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
            // Remove listener so they can't click
            btnRequest.setOnClickListener(null);
        }
    }

    // 2. Logic to fetch donor details + ratings
    private void fetchDonorDetails(String donorId) {
        if (donorId == null || donorId.isEmpty()) {
            donorNameTv.setText("Unknown Donor");
            donorPhoneTv.setVisibility(View.GONE);
            return;
        }

        db.collection("users").document(donorId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        donorNameTv.setText(name != null && !name.isEmpty() ? "👤 Donor: " + name : "👤 Donor: Anonymous");

                        String phone = documentSnapshot.getString("phone");
                        donorPhoneTv.setText(phone != null && !phone.isEmpty() ? "📞 Contact: " + phone : "📞 Contact: Phone number not filled");

                        // Safe Rating Fetch
                        Long highestRating = documentSnapshot.getLong("highestRating");
                        Long highestRatingCount = documentSnapshot.getLong("highestRatingCount");

                        if (highestRating == null || highestRatingCount == null || highestRatingCount == 0) {
                            donorRateTv.setText("⭐ Rating: 0 (0 reviews)");
                        } else {
                            donorRateTv.setText("⭐ " + highestRating + " stars (" + highestRatingCount + " reviews)");
                        }
                    }
                })
                .addOnFailureListener(e -> donorNameTv.setText("Error loading donor info"));
    }

    // 3. Logic to check if user ALREADY requested this meal
    private void checkExistingRequest() {
        if (mAuth.getCurrentUser() == null || meal == null) return;
        if (meal.getMealId() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("requests")
                .whereEqualTo("requesterId", uid)
                .whereEqualTo("mealId", meal.getMealId())
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) return;

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        // Found a request! Check its status.
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String status = doc.getString("status");
                        updateButtonUI(status);
                    } else {
                        // No request found, reset UI to default
                        resetButtonUI();
                    }
                });
    }

    private void resetButtonUI() {
        if (getContext() == null) return;

        // Only reset if stock allows it
        checkStockAvailability();

        // If checkStockAvailability disabled it, don't re-enable it blindly
        if (btnRequest.getText().toString().equals("Out of Stock")) return;

        btnRequest.setVisibility(View.VISIBLE);
        btnRequest.setText("Request This Food");
        btnRequest.setEnabled(true);
        btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.orange));
        btnRequest.setOnClickListener(v -> sendRequestToFirestore());

        btnCancelRequest.setVisibility(View.GONE);
        btnCancelRequest.setEnabled(false);
    }

    private void updateButtonUI(String status) {
        if (status == null || getContext() == null) return;

        // Default: hide cancel
        btnCancelRequest.setVisibility(View.GONE);
        btnCancelRequest.setEnabled(false);

        switch (status) {
            case "Pending":
                btnRequest.setText("Request Pending");
                btnRequest.setEnabled(false);
                btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.yellow_pending));
                // Allow Cancel
                btnCancelRequest.setVisibility(View.VISIBLE);
                btnCancelRequest.setEnabled(true);
                break;
            case "Accepted":
                btnRequest.setText("Request Approved! ✅");
                btnRequest.setEnabled(false);
                btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.green_approved));
                // Allow Cancel (if not yet collected)
                btnCancelRequest.setVisibility(View.VISIBLE);
                btnCancelRequest.setEnabled(true);
                break;
            default:
                btnRequest.setText(status);
                btnRequest.setEnabled(false);
                btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
                break;
        }
    }

    // 4. Logic to CANCEL a request (Decrement requestedQuantity)
    private void cancelRequest() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || meal == null) return;

        btnCancelRequest.setEnabled(false);
        btnCancelRequest.setText("Cancelling...");

        db.collection("requests")
                .whereEqualTo("mealId", meal.getMealId())
                .whereEqualTo("requesterId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(getContext(), "Could not find your request.", Toast.LENGTH_SHORT).show();
                        resetButtonUI();
                        return;
                    }
                    DocumentSnapshot requestDoc = queryDocumentSnapshots.getDocuments().get(0);
                    String requestId = requestDoc.getId();

                    // Transaction: Delete Request & Decrease 'requestedQuantity' on Meal
                    DocumentReference mealRef = db.collection("meals").document(meal.getMealId());
                    DocumentReference requestRef = db.collection("requests").document(requestId);

                    db.runTransaction(transaction -> {
                        DocumentSnapshot mealSnapshot = transaction.get(mealRef);
                        Long currentReqQty = mealSnapshot.getLong("requestedQuantity");
                        if (currentReqQty == null) currentReqQty = 0L;

                        if (currentReqQty > 0) {
                            transaction.update(mealRef, "requestedQuantity", currentReqQty - 1);
                        }
                        transaction.delete(requestRef);
                        return null;
                    }).addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Request Cancelled", Toast.LENGTH_SHORT).show();

                        // Update local object to reflect UI change immediately
                        meal.setRequestedQuantity(meal.getRequestedQuantity() - 1);
                        requestedQtyTv.setText("(" + meal.getRequestedQuantity() + " requested)");

                        resetButtonUI();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Cancellation failed.", Toast.LENGTH_SHORT).show();
                        btnCancelRequest.setEnabled(true);
                        btnCancelRequest.setText("Cancel Request");
                    });
                });
    }

    // 5. Logic to SEND a request (Increment requestedQuantity)
    private void sendRequestToFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please login.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (currentUser.getUid().equals(meal.getUserId())) {
            Toast.makeText(getContext(), "You can't request your own food!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRequest.setEnabled(false);
        btnRequest.setText("Sending...");

        final DocumentReference mealRef = db.collection("meals").document(meal.getMealId());

        // Transaction: Check stock -> Increase 'requestedQuantity' -> Create Request -> Create Chat
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(mealRef);

            // Check real-time quantity
            String qtyStr = snapshot.getString("quantity");
            Long reqQtyLong = snapshot.getLong("requestedQuantity");
            long currentReqQty = (reqQtyLong != null) ? reqQtyLong : 0;
            int totalQty = (qtyStr != null) ? Integer.parseInt(qtyStr) : 0;

            if (currentReqQty >= totalQty) {
                throw new FirebaseFirestoreException("Meal fully requested", FirebaseFirestoreException.Code.ABORTED);
            }

            // 1. Increment Requested Quantity
            transaction.update(mealRef, "requestedQuantity", currentReqQty + 1);

            // 2. Prepare Request Data
            DocumentReference newRequestRef = db.collection("requests").document(); // Generate new ID
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("requesterId", currentUser.getUid());
            requestMap.put("donorId", meal.getUserId());
            requestMap.put("mealId", meal.getMealId());
            requestMap.put("foodName", meal.getFoodName());
            requestMap.put("foodImage", meal.getImageUrl());
            requestMap.put("location", meal.getLocation());
            requestMap.put("status", "Pending");
            requestMap.put("timestamp", System.currentTimeMillis());

            transaction.set(newRequestRef, requestMap);

            return newRequestRef.getId(); // Return the new ID for the chat creation

        }).addOnSuccessListener(requestId -> {
            Toast.makeText(getContext(), "Request Sent!", Toast.LENGTH_SHORT).show();

            // Create Chat Room
            createChatRoom(requestId, currentUser.getUid(), meal.getUserId(), meal.getFoodName());

            // Update local UI
            meal.setRequestedQuantity(meal.getRequestedQuantity() + 1);
            requestedQtyTv.setText("(" + meal.getRequestedQuantity() + " requested)");
            updateButtonUI("Pending");

        }).addOnFailureListener(e -> {
            if (e instanceof FirebaseFirestoreException && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.ABORTED) {
                Toast.makeText(getContext(), "Sorry, this meal is now fully requested.", Toast.LENGTH_LONG).show();
                btnRequest.setText("Out of Stock");
            } else {
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                btnRequest.setEnabled(true);
                btnRequest.setText("Request This Food");
            }
        });
    }

    // 6. Logic to create Chat Room
    private void createChatRoom(String requestId, String requesterId, String donorId, String foodName) {
        Map<String, Object> chatMap = new HashMap<>();
        chatMap.put("participants", Arrays.asList(requesterId, donorId));
        chatMap.put("lastMessage", "Request sent for " + foodName);
        chatMap.put("lastMessageTime", System.currentTimeMillis());
        chatMap.put("foodName", foodName);
        chatMap.put("requestId", requestId);

        db.collection("chats").document(requestId)
                .set(chatMap)
                .addOnSuccessListener(aVoid -> Log.d("Chat", "Chat room created!"));
    }
}