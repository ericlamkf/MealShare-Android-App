package com.example.mealshare.ProfilePage;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mealshare.AddFragment;
import com.example.mealshare.Listing;
import com.example.mealshare.ListingsAdapter;
import com.example.mealshare.R;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class MyListsFragment extends Fragment {

    private RecyclerView listingsRecyclerView;
    private ListingsAdapter listingsAdapter;
    private List<Listing> listings;
    private FirebaseFirestore db;

    public MyListsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_lists, container, false);

        listingsRecyclerView = view.findViewById(R.id.listingsRecyclerView);
        listingsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        listings = new ArrayList<>();
        listingsAdapter = new ListingsAdapter(getContext(), listings);
        listingsRecyclerView.setAdapter(listingsAdapter);

        db = FirebaseFirestore.getInstance();
        fetchListings();

        Button addFoodBtn = view.findViewById(R.id.AddFoodBtn);
        addFoodBtn.setOnClickListener(v -> {
            Fragment addFragment = new AddFragment();
            FragmentManager fragmentManager = getParentFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(R.id.frameLayout, addFragment);
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
        });

        return view;
    }

    private void fetchListings() {
        db.collection("listings")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        listings.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            listings.add(document.toObject(Listing.class));
                        }
                        listingsAdapter.notifyDataSetChanged();
                    } else {
                        Log.d("MyListsFragment", "Error getting documents: ", task.getException());
                    }
                });
    }
}
