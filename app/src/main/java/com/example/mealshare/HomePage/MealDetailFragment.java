package com.example.mealshare.HomePage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.mealshare.R;

public class MealDetailFragment extends Fragment {

    private Meal meal;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve the object passed from the previous screen
        if (getArguments() != null) {
            meal = (Meal) getArguments().getSerializable("meal_data");
        }
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

        // 2. Set Listener
        backButton.setOnClickListener(v -> {
            // This tells the Activity to behave as if the hardware back button was pressed.
            // It pops the current fragment off the stack, returning to Home.
            requireActivity().onBackPressed();
        });

        // Initialize ViewModel
        com.example.mealshare.SharedViewModel viewModel = new androidx.lifecycle.ViewModelProvider(requireActivity())
                .get(com.example.mealshare.SharedViewModel.class);

        // Find Views
        ImageView imageView = view.findViewById(R.id.detail_image);
        TextView nameTv = view.findViewById(R.id.detail_food_name);
        TextView locTv = view.findViewById(R.id.detail_location);
        TextView qtyTv = view.findViewById(R.id.detail_quantity);
        TextView descTv = view.findViewById(R.id.detail_description);
        android.widget.Button requestBtn = view.findViewById(R.id.btn_request);

        // Populate Data
        if (meal != null) {
            nameTv.setText(meal.getFoodName());
            locTv.setText("📍 " + meal.getLocation());
            qtyTv.setText(meal.getQuantity() + " available");
            descTv.setText(meal.getDescription());

            if (meal.getImageUrl() != null) {
                Glide.with(this).load(meal.getImageUrl()).into(imageView);
            }

            // Logic: Request vs Cancel
            String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();

            // Toggle Logic
            if (currentUserId != null && currentUserId.equals(meal.getReceiverId())) {
                // User already requested this -> Show Cancel
                requestBtn.setText("Cancel Request");
                requestBtn.setBackgroundColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), android.R.color.holo_red_light));
                requestBtn.setOnClickListener(v -> {
                    viewModel.cancelRequest(meal.getMealId());
                });
            } else {
                // Default -> Show Request
                requestBtn.setText("Request Food");
                // Reset color if needed, or rely on distinct Button
                requestBtn.setOnClickListener(v -> {
                    viewModel.requestFood(meal.getMealId(), meal.getDonorId());
                });
            }
        }

        // Observer
        viewModel.getRequestStatus().observe(getViewLifecycleOwner(), status -> {
            if (status != null) {
                // Fix: Check if fragment is attached to avoid crash
                if (!isAdded() || getContext() == null) {
                    return;
                }

                android.widget.Toast.makeText(getContext(), status, android.widget.Toast.LENGTH_SHORT).show();

                if (status.startsWith("Success")) {
                    viewModel.clearRequestStatus(); // Consumed the event
                    requireActivity().onBackPressed(); // Navigate back
                }
            }
        });
    }
}
