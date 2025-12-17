package com.example.mealshare.ProfilePage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Import for Logging
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealshare.AddFragment; // Ensure this import is correct for your project
import com.example.mealshare.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MyListsFragment extends Fragment {

    private RecyclerView recyclerView;
    private RequestsAdapter adapter;
    private List<RequestModel> requestList;

    private LinearLayout emptyStateLayout;
    private Button btnAddFood;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_lists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = view.findViewById(R.id.rv_my_lists);
        progressBar = view.findViewById(R.id.progressBar_mylists);
        emptyStateLayout = view.findViewById(R.id.layout_empty_state);
        btnAddFood = view.findViewById(R.id.btn_add_food_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestList = new ArrayList<>();
        adapter = new RequestsAdapter(getContext(), requestList);
        recyclerView.setAdapter(adapter);

        btnAddFood.setOnClickListener(v -> {
            if (getActivity() != null) {
                // 1. Find the Bottom Navigation View in the Main Activity
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                        getActivity().findViewById(R.id.bottomNavigationView);

                // 2. Simulate clicking the "Add" button (Change R.id.add to your actual menu ID)
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.add);
                }
            }
        });

        loadIncomingRequests();
    }

    private void loadIncomingRequests() {
        if (mAuth.getCurrentUser() == null) return;

        String myUserId = mAuth.getCurrentUser().getUid();

        db.collection("requests")
                .whereEqualTo("donorId", myUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    progressBar.setVisibility(View.GONE);

                    // 1. Log Errors (Check Logcat if list is empty!)
                    if (error != null) {
                        Log.e("FirestoreError", "Error fetching data", error);
                        Toast.makeText(getContext(), "Error: Check Logcat", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        requestList.clear();

                        for (DocumentSnapshot doc : value.getDocuments()) {
                            RequestModel req = doc.toObject(RequestModel.class);
                            if (req != null) {
                                requestList.add(req);
                            }
                        }
                        adapter.notifyDataSetChanged();

                        // 3. Toggle Empty State
                        if (requestList.isEmpty()) {
                            emptyStateLayout.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        } else {
                            emptyStateLayout.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }
}