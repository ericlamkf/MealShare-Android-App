package com.example.mealshare;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class FeedbackFragment extends Fragment {

    private String donorId;
    private static final String DONOR_ID_KEY = "DONOR_ID_KEY";
    private static final String REQUEST_ID_KEY = "REQUEST_ID_KEY";
    private String requestId;
    private RatingBar ratingBar;
    private EditText feedbackBox;
    private Button btnSubmit;
    private TextView tvRate;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    public FeedbackFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            donorId = getArguments().getString(DONOR_ID_KEY);
            requestId = getArguments().getString(REQUEST_ID_KEY);
        }

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (requestId == null) {
            Toast.makeText(getContext(), "Invalid request", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feedback, container, false);

        ratingBar = view.findViewById(R.id.ratingBar);
        feedbackBox = view.findViewById(R.id.feedback_box);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        tvRate = view.findViewById(R.id.tvRate);

        loadVendorName();
        setupSubmitButton();

        return view;
    }

    private void loadVendorName() {
        if (donorId == null) return;

        db.collection("users")
                .document(donorId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String vendorName = doc.getString("name");
                        tvRate.setText("Rate " + vendorName);
                    }
                });
    }

    private void setupSubmitButton() {
        btnSubmit.setOnClickListener(v -> {

            float rating = ratingBar.getRating();
            String comment = feedbackBox.getText().toString().trim();

            if (rating == 0) {
                Toast.makeText(getContext(), "Please give a rating", Toast.LENGTH_SHORT).show();
                return;
            }
            db.collection("users")
                    .document(donorId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Toast.makeText(getContext(), "You have successfully rated " + doc.getString("name"), Toast.LENGTH_SHORT).show();
                        }
                    });

            saveFeedback(rating, comment);
        });
    }
    private void saveFeedback(float rating, String comment) {

        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> feedback = new HashMap<>();
        feedback.put("donorId", donorId);
        feedback.put("requesterId", userId);
        feedback.put("requestId", requestId);
        feedback.put("rating", rating);
        feedback.put("comment", comment);
        feedback.put("createdAt", FieldValue.serverTimestamp());

        db.collection("feedbacks")
                .document(requestId)   // 🔥 one feedback per request
                .set(feedback)
                .addOnSuccessListener(aVoid -> {
                    updateVendorRatingSummary(rating);
                    Toast.makeText(getContext(), "Feedback submitted ❤️", Toast.LENGTH_SHORT).show();

                    if (getActivity() != null) {
                        // Select Home tab in BottomNavigationView
                        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                                getActivity().findViewById(R.id.bottomNavigationView);

                        if (bottomNav != null) {
                            bottomNav.setSelectedItemId(R.id.home);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateVendorRatingSummary(float newRating) {

        db.runTransaction(transaction -> {

            // Reference to vendor document
            var userRef = db.collection("users").document(donorId);
            var snapshot = transaction.get(userRef);

            long highestRating = snapshot.contains("highestRating")
                    ? snapshot.getLong("highestRating")
                    : 0;

            long highestRatingCount = snapshot.contains("highestRatingCount")
                    ? snapshot.getLong("highestRatingCount")
                    : 0;

            if (newRating > highestRating) {
                // New highest rating found
                transaction.update(userRef, "highestRating", (long) newRating);
                transaction.update(userRef, "highestRatingCount", 1);

            } else if (newRating == highestRating) {
                // Same as current highest → increment count
                transaction.update(
                        userRef,
                        "highestRatingCount",
                        highestRatingCount + 1
                );
            }

            return null;
        }).addOnFailureListener(e ->
                Toast.makeText(getContext(),
                        "Rating update failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );
    }
}