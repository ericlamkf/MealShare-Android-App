package com.example.mealshare.ProfilePage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // Import Button
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mealshare.HomeFragment;
import com.example.mealshare.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MyRequestsFragment extends Fragment {

    private RecyclerView recyclerView;
    private MyRequestsAdapter adapter;
    private List<RequestModel> requestList;

    // UI Elements
    private LinearLayout emptyStateLayout;
    private ProgressBar progressBar;
    private Button btnBrowse; //

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_requests, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Find Views
        recyclerView = view.findViewById(R.id.rv_my_requests);
        progressBar = view.findViewById(R.id.progressBar_myreq);
        emptyStateLayout = view.findViewById(R.id.layout_empty_req);
        btnBrowse = view.findViewById(R.id.btn_browse_food); //
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        requestList = new ArrayList<>();
        adapter = new MyRequestsAdapter(getContext(), requestList);
        recyclerView.setAdapter(adapter);

        // Since Profile is usually a separate tab, we might need to rely on the user navigating via bottom bar,
        // OR we can just simulate a "Back" press if they came here from somewhere else.
        btnBrowse.setOnClickListener(v -> {
            if (getActivity() != null) {
                // Find the BottomNavigationView inside MainActivity
                com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                        getActivity().findViewById(R.id.bottomNavigationView);

                // Simulate clicking the "Home" icon (Use the ID from your menu_bottom_nav.xml)
                if (bottomNav != null) {
                    bottomNav.setSelectedItemId(R.id.home);
                }
            }
        });

        loadMyRequests();
    }

    private void loadMyRequests() {
        if (mAuth.getCurrentUser() == null) return;
        String myUserId = mAuth.getCurrentUser().getUid();

        db.collection("requests")
                .whereEqualTo("requesterId", myUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    progressBar.setVisibility(View.GONE);

                    if (error != null) {
                        Log.e("MyRequests", "Error", error);
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

                        // 🔥 Toggle Empty State visibility
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