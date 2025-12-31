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

    private void checkStockAvailability() {
        if (meal == null || getContext() == null) return;
        int currentQty = 0;
        try {
            currentQty = Integer.parseInt(meal.getQuantity());
        } catch (NumberFormatException e) { /* default 0 */ }

        if (currentQty <= 0 || meal.getRequestedQuantity() >= currentQty || "Out of stock".equalsIgnoreCase(meal.getStatus())) {
            btnRequest.setText("Out of Stock");
            btnRequest.setEnabled(false);
            btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
            btnRequest.setOnClickListener(null);
        }
    }

    private void fetchDonorDetails(String donorId) {
         // ... (logic is correct and remains the same)
    }

    private void checkExistingRequest() {
        // ... (logic is correct and remains the same)
    }

    private void resetButtonUI() {
        if (getContext() == null) return;
        checkStockAvailability();
        if (!btnRequest.isEnabled()) return;
        
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

        // By default, hide cancel button
        btnCancelRequest.setVisibility(View.GONE);
        btnCancelRequest.setEnabled(false);

        switch (status) {
            case "Pending":
                btnRequest.setText("Request Pending");
                btnRequest.setEnabled(false);
                btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.yellow_pending));
                // Show and enable CANCEL button
                btnCancelRequest.setVisibility(View.VISIBLE);
                btnCancelRequest.setEnabled(true);
                break;
            case "Accepted":
                btnRequest.setText("Request Approved! ✅");
                btnRequest.setEnabled(false); // Can't re-request
                btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.green_approved));
                // Still allow cancellation if not yet collected
                btnCancelRequest.setVisibility(View.VISIBLE);
                btnCancelRequest.setEnabled(true);
                break;
            default:
                // For Completed, Rejected, or any other state, hide main request button and cancel button
                btnRequest.setText(status);
                btnRequest.setEnabled(false);
                btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
                btnCancelRequest.setVisibility(View.GONE);
                break;
        }
    }
    
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
                        Toast.makeText(getContext(), "Could not find your request to cancel.", Toast.LENGTH_SHORT).show();
                        resetButtonUI();
                        return;
                    }
                    // Assuming one user can only have one pending request for the same meal
                    DocumentSnapshot requestDoc = queryDocumentSnapshots.getDocuments().get(0);
                    String requestId = requestDoc.getId();

                    // Run a transaction to delete the request and decrement the meal's requested quantity
                    DocumentReference mealRef = db.collection("meals").document(meal.getMealId());
                    DocumentReference requestRef = db.collection("requests").document(requestId);

                    db.runTransaction(transaction -> {
                        DocumentSnapshot mealSnapshot = transaction.get(mealRef);
                        long requestedQty = mealSnapshot.getLong("requestedQuantity");

                        if (requestedQty > 0) {
                            transaction.update(mealRef, "requestedQuantity", requestedQty - 1);
                        }
                        transaction.delete(requestRef);
                        return null;
                    }).addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Request Cancelled", Toast.LENGTH_SHORT).show();
                        resetButtonUI();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Cancellation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        btnCancelRequest.setEnabled(true);
                        btnCancelRequest.setText("Cancel Request");
                    });
                });
    }

    private void sendRequestToFirestore() {
       // ... (logic is correct and remains the same)
    }

    private void createRequestDocument(FirebaseUser currentUser) {
        // ... (logic is correct and remains the same)
    }

    private void createChatRoom(String requestId, String requesterId, String donorId, String foodName) {
       // ... (logic is correct and remains the same)
    }
}