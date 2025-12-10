package com.example.mealshare;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.lifecycle.ViewModelProvider; // REQUIRED for SharedViewModel

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mealshare.HomePage.MealShareAdapter;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class HomeFragment extends Fragment {
    private TextView HomeName;
    private Button BtnToAdd;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView Location;
    private FusedLocationProviderClient fusedLocationProviderClient;

    // 🔥 The location data is now managed by the ViewModel, but we keep this for initial setting
    private final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    // 🔥 NEW: Shared ViewModel instance
    private SharedViewModel sharedViewModel;

    // ... existing newInstance and onCreate methods ...

    public HomeFragment() {
        // Required empty public constructor
    }

    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        // ... (standard setup) ...
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 🔥 INITIALIZE SHARED VIEWMODEL (Scoped to the hosting Activity)
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // INITIALIZE LOCATION CLIENT AND UI
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        Location = view.findViewById(R.id.Location);

        // 🔥 SUBSCRIBE: HomeFragment observes its own ViewModel data to update the UI
        sharedViewModel.getLocation().observe(getViewLifecycleOwner(), address -> {
            Location.setText("📌 " + address);
        });

        // LOCATION PERMISSION AND FETCHING
        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }else{
            getDeviceLocation();
        }

        // USER DATA LOADING
        HomeName = view.findViewById(R.id.UserName);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null){
            String uid = currentUser.getUid();
            loadUserData(uid);
        }else{
            HomeName.setText("Guest");
        }

        // NAVIGATION BUTTON
        BtnToAdd = view.findViewById(R.id.BtnToAdd);
        BtnToAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toNextFragment();
            }
        });

        // TAB LAYOUT SETUP (VIEWPAGER)
        TabLayout tabLayout = view.findViewById(R.id.CardTabLayout);
        ViewPager2 viewPager2 = view.findViewById(R.id.CardFragmentView);

        MealShareAdapter adapter = new MealShareAdapter(requireActivity());
        viewPager2.setAdapter(adapter);

        String[] tabTitles = new String[]{"All", "Halal", "Ending Soon", "Vege"};

        new TabLayoutMediator(tabLayout, viewPager2, (tab, position)->{
            tab.setText(tabTitles[position]);
        }).attach();
    }

    private void getDeviceLocation() {
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {

                fusedLocationProviderClient.getLastLocation()
                        .addOnSuccessListener(requireActivity(), location -> {
                            if (location != null) {
                                reverseGeocode(location.getLatitude(), location.getLongitude());
                            } else {
                                // Update ViewModel on failure
                                sharedViewModel.setLocation("Location not found.");
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
            sharedViewModel.setLocation("Error fetching location.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getDeviceLocation();
            } else {
                sharedViewModel.setLocation("Location access denied.");
            }
        }
    }

    private void reverseGeocode(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String streetAddress = address.getAddressLine(0);

                // 🔥 DEPOSIT DATA: Update the ViewModel with the real address
                sharedViewModel.setLocation(streetAddress);

            } else {
                sharedViewModel.setLocation("Address not found.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            sharedViewModel.setLocation("Geocoding service unavailable.");
        }
    }

    private void toNextFragment() {
        FragmentManager fm = getParentFragmentManager();

        // 🔥 CLEAN NAVIGATION: No Bundle needed! Data is retrieved via ViewModel in AddFragment
        AddFragment addFragment = new AddFragment();

        fm.beginTransaction()
                .replace(R.id.frameLayout, addFragment)
                .addToBackStack(null)
                .commit();

        try{
            MainActivity activity = (MainActivity) getActivity();
            BottomNavigationView bottomNavigationView = activity.findViewById(R.id.bottomNavigationView);
            bottomNavigationView.setSelectedItemId(R.id.add);
        }catch(ClassCastException e){
            e.printStackTrace();
        }
    }

    private void loadUserData(String uid){
        DocumentReference userRef = db.collection("users").document(uid);

        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()){
                    DocumentSnapshot document = task.getResult();
                    if(document.exists()){
                        String name = document.getString("name");

                        if(name != null){
                            HomeName.setText(name);
                        }else{
                            HomeName.setText("Guest");
                        }
                    }else{
                        Toast.makeText(getContext(), "User Data Not Found.", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(getContext(), "Error Loading User Data." + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

}