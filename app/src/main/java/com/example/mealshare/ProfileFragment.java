package com.example.mealshare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

// Make sure you import Glide and CircleImageView
import com.bumptech.glide.Glide;
import de.hdodenhof.circleimageview.CircleImageView;

// Import your fragments
// import com.example.mealshare.HomePage.MyActivityFragment; // UNCOMMENT THIS if you have it
// import com.example.mealshare.ProfilePage.MyRequestsFragment; // UNCOMMENT THIS if you have it
import com.example.mealshare.EditProfileFragment;

import com.example.mealshare.ProfilePage.MyListsFragment;
import com.example.mealshare.ProfilePage.MyRequestsFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    // UI Components matching the new XML
    private CircleImageView profileImageView;
    private TextView userNameTextView, locationTextView;
    private TextView tvName, tvEmail, tvPhone, tvAddress;
    private Button btnEdit, btnMyList, btnMyReq;

    // Firebase & Location
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. Initialize Views (Matching IDs from new XML)
        profileImageView = view.findViewById(R.id.profile_pic);
        userNameTextView = view.findViewById(R.id.UserName);
        locationTextView = view.findViewById(R.id.Location);

        // Card Details
        tvName = view.findViewById(R.id.tvName);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPhone = view.findViewById(R.id.tvPhone);
        tvAddress = view.findViewById(R.id.tvAddress);

        // Buttons
        btnEdit = view.findViewById(R.id.BtnEdit);
        btnMyList = view.findViewById(R.id.BtnMyList);
        btnMyReq = view.findViewById(R.id.BtnMyReq);

        // 2. Set Click Listeners
        // Note: Make sure EditProfileFragment exists or create it
        btnEdit.setOnClickListener(v -> navigateTo(new EditProfileFragment()));

        // Navigation for My Lists (Assuming MyActivityFragment handles this)
        btnMyList.setOnClickListener(v -> navigateTo(new MyListsFragment()));

        // Navigation for My Requests
        btnMyReq.setOnClickListener(v -> navigateTo(new MyRequestsFragment()));

        // 3. Setup Location
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        checkLocationPermission();

        Button btnLogout = view.findViewById(R.id.BtnLogout);

        btnLogout.setOnClickListener(v -> {
            // 1. Sign out from Firebase
            FirebaseAuth.getInstance().signOut();

            // 2. Redirect to Login Activity
            Intent intent = new Intent(getActivity(), LoginActivity.class);

            // Clear back stack so user can't press "Back" to return
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData(); // Reload data when returning to this screen
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            DocumentReference userRef = db.collection("users").document(currentUser.getUid());

            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {

                        // Extract Data
                        String name = document.getString("name");
                        String email = document.getString("email");
                        String phone = document.getString("phone");
                        String address = document.getString("address");
                        String profileUrl = document.getString("profileImageUrl");

                        // Update Text Views
                        if (name != null) {
                            userNameTextView.setText(name);
                            tvName.setText(name);
                        }
                        if (email != null)
                            tvEmail.setText(email);

                        // Handle empty fields gracefully
                        tvPhone.setText(phone != null && !phone.isEmpty() ? phone : "Phone not set");
                        tvAddress.setText(address != null && !address.isEmpty() ? address : "Address not set");

                        // 🔥 Load Profile Image using Glide
                        if (profileUrl != null && !profileUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileUrl)
                                    .placeholder(R.drawable.profile_pic) // Default image while loading
                                    .error(R.drawable.profile_pic) // Default if error
                                    .into(profileImageView);
                        }

                    } else {
                        Toast.makeText(getContext(), "User data not found.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "Error loading user data.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            userNameTextView.setText("Guest");
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getDeviceLocation();
        }
    }

    private void getDeviceLocation() {
        try {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationProviderClient.getLastLocation()
                        .addOnSuccessListener(requireActivity(), location -> {
                            if (location != null) {
                                reverseGeocode(location.getLatitude(), location.getLongitude());
                            } else {
                                locationTextView.setText("📌 Location not found.");
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
            locationTextView.setText("📌 Error fetching location.");
        }
    }

    private void reverseGeocode(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                locationTextView.setText("📌 " + addresses.get(0).getAddressLine(0));
            } else {
                locationTextView.setText("📌 Address not found.");
            }
        } catch (IOException e) {
            locationTextView.setText("📌 Geocoding unavailable.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getDeviceLocation();
        } else {
            locationTextView.setText("📌 Location access denied.");
        }
    }

    private void navigateTo(Fragment fragment) {
        getParentFragmentManager().beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .addToBackStack(null)
                .commit();
    }
}