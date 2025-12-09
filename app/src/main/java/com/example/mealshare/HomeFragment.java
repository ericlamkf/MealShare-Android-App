package com.example.mealshare;

import android.Manifest;
import android.content.Intent;
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
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
    private TextView HomeName;
    private Button BtnToAdd;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView Location;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private final int LOCATION_PERMISSION_REQUEST_CODE = 101;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Location = view.findViewById(R.id.Location);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if(ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }else{
            getDeviceLocation();
        }

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

        BtnToAdd = view.findViewById(R.id.BtnToAdd);
        BtnToAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toNextFragment();
            }
        });

        TabLayout tabLayout = view.findViewById(R.id.CardTabLayout);
        ViewPager2 viewPager2 = view.findViewById(R.id.CardFragmentView);



        MealShareAdapter adapter = new MealShareAdapter(requireActivity());
        viewPager2.setAdapter(adapter);

        String[] tabTitles = new String[]{"All", "Near Me", "Ending Soon", "Vege"};

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
                                // Coordinates found, now get the address
                                reverseGeocode(location.getLatitude(), location.getLongitude());
                            } else {
                                Location.setText("📌 Location not found.");
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Location.setText("📌 Error fetching location.");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getDeviceLocation(); // Permission granted, get location
            } else {
                // Permission denied, show a default message or prompt
                Location.setText("📌 Location access denied.");
            }
        }
    }

    private void reverseGeocode(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                // Combine address lines to get the desired format (e.g., street, city, state)
                String streetAddress = address.getAddressLine(0);

                // You can customize the format here:
                // Example: "17, SS 2/73, Petaling Jaya"
                // String customAddress = address.getSubThoroughfare() + ", " + address.getThoroughfare() + ", " + address.getLocality();

                Location.setText("📌 " + streetAddress);
            } else {
                Location.setText("📌 Address not found.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Location.setText("📌 Geocoding service unavailable.");
        }
    }

    private void toNextFragment() {
        FragmentManager fm = getParentFragmentManager();
        fm.beginTransaction()
                .replace(R.id.frameLayout, new AddFragment())
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