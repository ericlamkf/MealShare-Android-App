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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

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
        donorRateTv = view.findViewById(R.id.donor_feedback); // From Left

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
        }

        btnRequest.setOnClickListener(v -> sendRequestToFirestore());
    }

    private void fetchDonorDetails(String donorId) {
        if (donorId == null || donorId.isEmpty()) {
            donorNameTv.setText("Unknown Donor");
            donorPhoneTv.setVisibility(View.GONE);
            return;
        }

        db.collection("users").document(donorId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Get Name
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            donorNameTv.setText("👤 Donor: " + name);
                        } else {
                            donorNameTv.setText("👤 Donor: Anonymous");
                        }

                        // Get Phone
                        String phone = documentSnapshot.getString("phone");
                        if (phone != null && !phone.isEmpty()) {
                            donorPhoneTv.setText("📞 Contact: " + phone);
                        } else {
                            donorPhoneTv.setText("📞 Contact: Phone number not filled");
                        }

                        // ⭐ Fetch rating info safely (From Left File)
                        Long highestRating = documentSnapshot.getLong("highestRating");
                        Long highestRatingCount = documentSnapshot.getLong("highestRatingCount");

                        if (highestRating == null || highestRatingCount == null) {
                            donorRateTv.setText("⭐ This user does not have a rating yet");
                            return;
                        }

                        if (highestRating == 0 || highestRatingCount == 0) {
                            donorRateTv.setText("⭐ Rating: 0 (0 reviews)");
                        } else {
                            donorRateTv.setText(
                                    "⭐ " + highestRating + " stars (" + highestRatingCount + " reviews)"
                            );
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    donorNameTv.setText("Error loading donor info");
                    donorRateTv.setText("⭐ Rating unavailable");
                });
    }

    private void checkExistingRequest() {
        if (mAuth.getCurrentUser() == null || meal == null) return;
        if (meal.getMealId() == null) return;

        String uid = mAuth.getCurrentUser().getUid();

        db.collection("requests")
                .whereEqualTo("requesterId", uid)
                .whereEqualTo("mealId", meal.getMealId())
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) return;

                    if (queryDocumentSnapshots != null) {
                        String activeStatus = null;

                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String status = doc.getString("status");
                            if ("Pending".equals(status) || "Accepted".equals(status)) {
                                activeStatus = status;
                                break;
                            }
                        }

                        if (activeStatus != null) {
                            updateButtonUI(activeStatus);
                        } else {
                            resetButtonUI();
                        }
                    }
                });
    }

    private void resetButtonUI() {
        if (getContext() == null) return;

        btnRequest.setText("Request This Food");
        btnRequest.setEnabled(true);
        btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.orange));
        btnRequest.setOnClickListener(v -> sendRequestToFirestore());
    }

    private void updateButtonUI(String status) {
        if (status == null || getContext() == null) return;

        switch (status) {
            case "Pending":
                btnRequest.setText("Request Pending");
                btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.yellow_pending));
                btnRequest.setEnabled(false);
                break;

            case "Accepted":
                btnRequest.setText("Request Approved! ✅");
                btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.green_approved));
                btnRequest.setEnabled(true);
                break;

            case "Completed":
                btnRequest.setText("Collected");
                btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
                btnRequest.setEnabled(false);
                break;

            case "Rejected":
                btnRequest.setText("Request Rejected");
                btnRequest.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), android.R.color.darker_gray));
                btnRequest.setEnabled(false);
                break;

            default:
                btnRequest.setText("Request This Food");
                btnRequest.setEnabled(true);
        }
    }

    private void sendRequestToFirestore() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), "Please login to request food.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser.getUid().equals(meal.getUserId())) {
            Toast.makeText(getContext(), "You can't request your own food!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRequest.setEnabled(false);
        btnRequest.setText("Sending...");

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("requesterId", currentUser.getUid());
        requestMap.put("donorId", meal.getUserId());
        requestMap.put("mealId", meal.getMealId());
        requestMap.put("foodName", meal.getFoodName());
        requestMap.put("foodImage", meal.getImageUrl());
        requestMap.put("location", meal.getLocation());
        requestMap.put("status", "Pending");
        requestMap.put("timestamp", System.currentTimeMillis());

        db.collection("requests")
                .add(requestMap)
                .addOnSuccessListener(documentReference -> {
                    String newRequestId = documentReference.getId();

                    // From Right: Create Chat Room
                    createChatRoom(newRequestId, currentUser.getUid(), meal.getUserId(), meal.getFoodName());

                    // From Left: Update Quantity
                    updateMealCollection();

                    Toast.makeText(getContext(), "Request Sent Successfully!", Toast.LENGTH_SHORT).show();
                    updateButtonUI("Pending");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    btnRequest.setEnabled(true);
                    btnRequest.setText("Request This Food");
                });
    }

    // From Right: Chat Room Creation
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

    // From Left: Quantity Update
    private void updateMealCollection() {
        if (meal == null || meal.getMealId() == null) {
            Toast.makeText(getContext(), "Meal data not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(db.collection("meals").document(meal.getMealId()));

            String qtyStr = snapshot.getString("quantity");
            if (qtyStr == null || qtyStr.isEmpty()) {
                throw new FirebaseFirestoreException("Quantity not set",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            int currentQty = Integer.parseInt(qtyStr);
            if (currentQty <= 0) {
                throw new FirebaseFirestoreException("No more meals available",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            int newQty = currentQty - 1;
            transaction.update(db.collection("meals").document(meal.getMealId()),
                    "quantity", String.valueOf(newQty));

            if (newQty == 0) {
                transaction.update(db.collection("meals").document(meal.getMealId()),
                        "status", "Reserved");
            }

            return newQty;
        }).addOnSuccessListener(newQty -> {
            Toast.makeText(getContext(), "Quantity updated to " + newQty, Toast.LENGTH_SHORT).show();
            meal.setQuantity(String.valueOf(newQty));
            if (newQty == 0) {
                meal.setStatus("Reserved");
                TextView qtyTv = getView().findViewById(R.id.detail_quantity);
                if (qtyTv != null) {
                    qtyTv.setText("0 available, all meals reserved");
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}