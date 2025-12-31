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
import com.google.firebase.firestore.FieldValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MealDetailFragment extends Fragment {

    private Meal meal;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Button btnRequest;

    private TextView donorNameTv, donorPhoneTv, donorRateTv;

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

        ImageView imageView = view.findViewById(R.id.detail_image);
        TextView nameTv = view.findViewById(R.id.detail_food_name);
        TextView locTv = view.findViewById(R.id.detail_location);
        TextView qtyTv = view.findViewById(R.id.detail_quantity);
        TextView descTv = view.findViewById(R.id.detail_description);

        donorNameTv = view.findViewById(R.id.detail_donor_name);
        donorPhoneTv = view.findViewById(R.id.detail_donor_phone);
        donorRateTv = view.findViewById(R.id.donor_feedback);

        btnRequest = view.findViewById(R.id.btn_request);

        if (meal != null) {
            nameTv.setText(meal.getFoodName());
            locTv.setText("📍 " + meal.getLocation());
            qtyTv.setText(meal.getQuantity() + " available");
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
    }

    private void checkStockAvailability() {
        if (meal == null || getContext() == null) return;

        int currentQty = 0;
        try {
            currentQty = Integer.parseInt(meal.getQuantity());
        } catch (NumberFormatException e) {
            currentQty = 0;
        }

        int requestedQty = meal.getRequestedQuantity();

        if (currentQty <= 0 || requestedQty >= currentQty || "Out of stock".equalsIgnoreCase(meal.getStatus())) {
            btnRequest.setText("Out of Stock");
            btnRequest.setEnabled(false);
            btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
            btnRequest.setOnClickListener(null);
        }
    }

    private void fetchDonorDetails(String donorId) {
        // ... (fetchDonorDetails logic remains the same)
    }

    private void checkExistingRequest() {
        // ... (checkExistingRequest logic remains the same)
    }

    private void resetButtonUI() {
        if (getContext() == null) return;

        checkStockAvailability();
        if (!btnRequest.isEnabled()) return;

        btnRequest.setText("Request This Food");
        btnRequest.setEnabled(true);
        btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.orange));
        btnRequest.setOnClickListener(v -> sendRequestToFirestore());
    }

    private void updateButtonUI(String status) {
        // ... (updateButtonUI logic remains the same)
    }

    private void sendRequestToFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null || meal == null) {
            Toast.makeText(getContext(), "Error: User or meal data is missing.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser.getUid().equals(meal.getUserId())) {
            Toast.makeText(getContext(), "You can't request your own food!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRequest.setEnabled(false);
        btnRequest.setText("Sending...");

        DocumentReference mealRef = db.collection("meals").document(meal.getMealId());
        // Create a new request document with a unique ID
        DocumentReference newRequestRef = db.collection("requests").document();

        db.runTransaction(transaction -> {
            DocumentSnapshot mealSnapshot = transaction.get(mealRef);

            if (!mealSnapshot.exists()) {
                throw new FirebaseFirestoreException("This meal is no longer available.", FirebaseFirestoreException.Code.ABORTED);
            }

            // Safely get current quantities from the database snapshot
            long currentQty = 0;
            try {
                 currentQty = Long.parseLong(mealSnapshot.getString("quantity"));
            } catch(Exception e) { /* default to 0 */}

            Long requestedQtyLong = mealSnapshot.getLong("requestedQuantity");
            long requestedQty = (requestedQtyLong != null) ? requestedQtyLong : 0;

            if (requestedQty >= currentQty) {
                throw new FirebaseFirestoreException("All portions have been requested.", FirebaseFirestoreException.Code.ABORTED);
            }

            // ALL CHECKS PASSED, PROCEED WITH TRANSACTION

            // 1. Create the new request document
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("requesterId", currentUser.getUid());
            requestMap.put("donorId", meal.getUserId());
            requestMap.put("mealId", meal.getMealId());
            requestMap.put("foodName", meal.getFoodName());
            requestMap.put("foodImage", meal.getImageUrl());
            requestMap.put("location", meal.getLocation());
            requestMap.put("status", "Pending");
            requestMap.put("timestamp", FieldValue.serverTimestamp()); // Use server timestamp

            transaction.set(newRequestRef, requestMap);

            // 2. Update the meal's requested quantity
            transaction.update(mealRef, "requestedQuantity", requestedQty + 1);

            return newRequestRef.getId(); // Pass the new request ID out of the transaction

        }).addOnSuccessListener(newRequestId -> {
            Toast.makeText(getContext(), "Request Sent Successfully!", Toast.LENGTH_SHORT).show();
            updateButtonUI("Pending");
            createChatRoom(newRequestId, currentUser.getUid(), meal.getUserId(), meal.getFoodName());

        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Request Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            // Reset button to allow another attempt if it was a transient error
            resetButtonUI();
        });
    }

    private void createChatRoom(String requestId, String requesterId, String donorId, String foodName) {
       // ... (createChatRoom logic remains the same)
    }
}
