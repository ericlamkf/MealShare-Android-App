package com.example.mealshare.ProfilePage;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mealshare.HomePage.Meal;
import com.example.mealshare.HomePage.MealAdapter;
import com.example.mealshare.HomePage.MealDetailFragment;
import com.example.mealshare.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class MyListsFragment extends Fragment {

    private RecyclerView recyclerView;
    private MealAdapter adapter;
    private List<Meal> mealList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private TextView emptyText;

    public MyListsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_lists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.recycler_view_my_lists);
        emptyText = view.findViewById(R.id.tv_empty_lists);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mealList = new ArrayList<>();
        // Use the existing MealAdapter. We need to handle item clicks.
        // For now, let's navigate to detail page just like home page.
        adapter = new MealAdapter(getContext(), mealList, meal -> {
            MealDetailFragment detailFragment = new MealDetailFragment();
            Bundle bundle = new Bundle();
            bundle.putSerializable("meal_data", meal);
            detailFragment.setArguments(bundle);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.frameLayout, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });
        recyclerView.setAdapter(adapter);

        loadMyListings();
    }

    private void loadMyListings() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        db.collection("meals")
                .whereEqualTo("donorId", userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            mealList.clear();
                            for (DocumentSnapshot doc : task.getResult()) {
                                Meal meal = doc.toObject(Meal.class);
                                if (meal != null) {
                                    meal.setMealId(doc.getId()); // Ensure ID is set
                                    mealList.add(meal);
                                }
                            }

                            adapter.notifyDataSetChanged();

                            if (mealList.isEmpty()) {
                                recyclerView.setVisibility(View.GONE);
                                emptyText.setVisibility(View.VISIBLE);
                            } else {
                                recyclerView.setVisibility(View.VISIBLE);
                                emptyText.setVisibility(View.GONE);
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to load listings", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}