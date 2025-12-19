package com.example.mealshare.HomePage;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout; // Import added
import android.widget.TextView;     // Import added

import com.example.mealshare.R;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NearMeFragment extends Fragment {
    private RecyclerView recyclerView;
    private MealAdapter mealAdapter;
    private List<Meal> mealList;
    private FirebaseFirestore db;

    // 🔥 NEW: Variable for the Empty State Layout
    private LinearLayout emptyStateLayout;

    // ... (Your params and constructor code remains the same) ...
    // Skipping boilerplate params code for clarity...

    public NearMeFragment() {
        // Required empty public constructor
    }

    public static NearMeFragment newInstance(String param1, String param2) {
        NearMeFragment fragment = new NearMeFragment();
        // ... existing code ...
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // ... existing code ...
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_near_me, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Setup RecyclerView
        recyclerView = view.findViewById(R.id.recycler_view_all);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 2. 🔥 NEW: Setup Empty State Layout
        // Make sure this ID matches what you put in your XML!
        emptyStateLayout = view.findViewById(R.id.layout_empty_state);

        mealList = new ArrayList<>();
        mealAdapter = new MealAdapter(getContext(), mealList, this::onMealClick);
        recyclerView.setAdapter(mealAdapter);

        db = FirebaseFirestore.getInstance();
        fetchmeals();
    }

    private void fetchmeals() {
        db.collection("meals")
                .whereEqualTo("category", "Halal")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null) return;

                    if (snapshot != null) {
                        List<Meal> fetchedMeals = new ArrayList<>();

                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Meal meal = doc.toObject(Meal.class);

                            if (meal != null) {
                                meal.setMealId(doc.getId());

                                // Safe Quantity Check
                                int quantity = 0;
                                try {
                                    Object qtyObj = doc.get("quantity");
                                    if (qtyObj instanceof String) {
                                        quantity = Integer.parseInt((String) qtyObj);
                                    } else if (qtyObj instanceof Long) {
                                        quantity = ((Long) qtyObj).intValue();
                                    }
                                } catch (Exception ex) { quantity = 0; }

                                // FILTER: Only add if Quantity > 0
                                if (quantity > 0) {
                                    fetchedMeals.add(meal);
                                }
                            }
                        }

                        // 🔥 NEW LOGIC: Check if the Final List is Empty
                        if (fetchedMeals.isEmpty()) {
                            // Empty? Show the "No Halal Food" message
                            recyclerView.setVisibility(View.GONE);
                            if (emptyStateLayout != null) {
                                emptyStateLayout.setVisibility(View.VISIBLE);

                                // Optional: Update text specifically for this fragment
                                TextView emptyText = emptyStateLayout.findViewById(R.id.tv_empty_message);
                                if (emptyText != null) emptyText.setText("No Halal meals found nearby.");
                            }
                        } else {
                            // Not Empty? Show the list
                            recyclerView.setVisibility(View.VISIBLE);
                            if (emptyStateLayout != null) {
                                emptyStateLayout.setVisibility(View.GONE);
                            }
                        }

                        mealAdapter.updateList(fetchedMeals);
                    }
                });
    }

    public void onMealClick(Meal meal) {
        MealDetailFragment detailFragment = new MealDetailFragment();
        Bundle args = new Bundle();
        args.putSerializable("meal_data", meal);
        detailFragment.setArguments(args);

        requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, detailFragment)
                .addToBackStack(null)
                .commit();
    }
}