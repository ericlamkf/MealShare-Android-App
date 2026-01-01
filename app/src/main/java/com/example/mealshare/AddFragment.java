package com.example.mealshare;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.Observer;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddFragment extends Fragment {

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private final int GALLERY_PERMISSION_CODE = 100;

    private String mParam1;
    private String mParam2;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private EditText foodNameEditText;
    private EditText descriptionEditText;

    // 🔥 THIS IS NOW UPDATED ASYNCHRONOUSLY BY THE VIEWMODEL
    private String postingLocation = "Location Not Set";

    private EditText quantityEditText;
    private Uri mImageUri = null;
    private ImageView imagePreview;
    private TextView tapToUploadText;
    private ActivityResultLauncher<Intent> mImagePickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ImageView removeBTN;
    private ChipGroup chipGroupTags;
    private Button postListingButton;

    private EditText et_expiry_time;
    private Calendar expiryCalendar;

    // 🔥 NEW: Shared ViewModel instance
    private SharedViewModel sharedViewModel;


    public AddFragment() {
        // Required empty public constructor
    }

    // ... existing newInstance and onCreate methods ...

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 🔥 1. INITIALIZE SHARED VIEWMODEL AND SUBSCRIBE TO LOCATION UPDATES
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        sharedViewModel.getLocation().observe(getViewLifecycleOwner(), location -> {
            // This runs immediately upon fragment creation and whenever HomeFragment updates the location
            postingLocation = location;
            // You can optionally add a log/toast here for final confirmation during debug
            // Toast.makeText(requireContext(), "Location Received: " + postingLocation, Toast.LENGTH_SHORT).show();
        });


        // 2. INITIALIZE SERVICES
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // 3. INITIALIZE UI VIEWS
        et_expiry_time = view.findViewById(R.id.et_expiry_time);
        // ... (rest of EditText and ChipGroup initialization) ...
        foodNameEditText = view.findViewById(R.id.et_food_name);
        descriptionEditText = view.findViewById(R.id.et_description);
        quantityEditText = view.findViewById(R.id.et_quantity);
        chipGroupTags = view.findViewById(R.id.chip_group_tags);
        postListingButton = view.findViewById(R.id.btn_post_listing);

        // 4. QUANTITY BUTTONS
        Button btnMinus = view.findViewById(R.id.btn_minus);
        Button btnPlus = view.findViewById(R.id.btn_plus);

        btnMinus.setOnClickListener(v -> updateQuantity(-1));
        btnPlus.setOnClickListener(v -> updateQuantity(1));


        // 5. IMAGE & PERMISSION LAUNCHERS
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openGalleryIntent();
                    } else {
                        Toast.makeText(requireContext(), "Permission denied to access gallery.", Toast.LENGTH_SHORT).show();
                    }
                });

        imagePreview = view.findViewById(R.id.image_preview);
        tapToUploadText = view.findViewById(R.id.tv_tap_to_upload);
        ConstraintLayout photoUploadContainer = view.findViewById(R.id.photo_upload_container);

        mImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK &&
                            result.getData() != null &&
                            result.getData().getData() != null) {

                        mImageUri = result.getData().getData();
                        imagePreview.setImageURI(mImageUri);

                        removeBTN.setVisibility(View.VISIBLE);
                        tapToUploadText.setVisibility(View.GONE);
                    }
                });

        photoUploadContainer.setOnClickListener(v -> openGallery());
        imagePreview.setOnClickListener(v -> openGallery());

        // 6. REMOVE BUTTON LOGIC
        removeBTN = view.findViewById(R.id.removeBTN);
        removeBTN.setVisibility(View.GONE);
        removeBTN.setOnClickListener(v -> resetImage());

        // 7. EXPIRY TIME PICKER
        expiryCalendar = Calendar.getInstance();
        et_expiry_time.setOnClickListener(v -> showDatePicker());

        // 8. POST LISTING
        postListingButton.setOnClickListener(v -> postListing());
    }

    private void updateQuantity(int change) {
        String currentText = quantityEditText.getText().toString();
        int quantity = 0;
        if (!currentText.isEmpty()) {
            try {
                quantity = Integer.parseInt(currentText);
            } catch (NumberFormatException e) {
                quantity = 0;
            }
        }
        quantity += change;
        if (quantity < 0) quantity = 0;
        quantityEditText.setText(String.valueOf(quantity));
    }

    private void postListing() {
        // Validation check uses the asynchronously updated postingLocation
        if (postingLocation.equals("Fetching location...") || postingLocation.equals("Location Not Set") || postingLocation.equals("Unknown Location")) {
            Toast.makeText(requireContext(), "Please wait for location to load before posting.", Toast.LENGTH_SHORT).show();
            return;
        }

        String foodName = foodNameEditText.getText().toString().trim();
        String quantity = quantityEditText.getText().toString().trim();

        if (foodName.isEmpty() || quantity.isEmpty() || et_expiry_time.getText().toString().isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        String mealId = db.collection("meals").document().getId();

        if (mImageUri != null) {
            uploadImageAndSaveData(mealId);
        } else {
            saveMealDataToFirestore(mealId, null);
        }
    }

    // ... uploadImageAndSaveData, saveMealDataToFirestore, resetUI, resetImage,
    // openGallery, openGalleryIntent, showDatePicker, showTimePicker, and
    // updateExpiryTimeLabel methods remain the same as your previous code ...

    // The implementation of these methods is identical to the last version you shared.

    private void uploadImageAndSaveData(String mealId) {
        StorageReference fileRef = storage.getReference()
                .child("meals/" + mealId + ".jpg");

        fileRef.putFile(mImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        saveMealDataToFirestore(mealId, imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    saveMealDataToFirestore(mealId, null);
                });
    }

    private void saveMealDataToFirestore(String mealId, @Nullable String imageUrl) {
        String userId = auth.getCurrentUser().getUid();

        List<String> selectedTagsList = new ArrayList<>();
        List<Integer> checkedChipIds = chipGroupTags.getCheckedChipIds();
        for (int id : checkedChipIds) {
            Chip chip = chipGroupTags.findViewById(id);
            if (chip != null) {
                selectedTagsList.add(chip.getText().toString());
            }
        }

        Map<String, Object> mealData = new HashMap<>();
        mealData.put("userId", userId);
        mealData.put("foodName", foodNameEditText.getText().toString().trim());
        mealData.put("description", descriptionEditText.getText().toString().trim());
        mealData.put("quantity", quantityEditText.getText().toString().trim());
        mealData.put("expiryTime", expiryCalendar.getTime());
        mealData.put("imageUrl", imageUrl);
        mealData.put("tags", selectedTagsList);
        mealData.put("location", postingLocation); // 🔥 Uses the reliably updated postingLocation
        mealData.put("timestamp", FieldValue.serverTimestamp());

        db.collection("meals")
                .document(mealId)
                .set(mealData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Listing posted successfully!", Toast.LENGTH_SHORT).show();
                    resetUI();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Error posting listing: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void resetUI() {
        foodNameEditText.setText("");
        descriptionEditText.setText("");
        quantityEditText.setText("");
        et_expiry_time.setText("");
        chipGroupTags.clearCheck();
        resetImage();
    }

    private void resetImage() {
        mImageUri = null;
        imagePreview.setImageURI(null);
        imagePreview.setImageResource(0);
        tapToUploadText.setVisibility(View.VISIBLE);
        removeBTN.setVisibility(View.GONE);
    }

    private void openGallery() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
            openGalleryIntent();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        }
    }

    private void openGalleryIntent() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        mImagePickerLauncher.launch(galleryIntent);
    }

    private void showDatePicker() {
        // ... (DatePickerDialog implementation) ...
        new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    expiryCalendar.set(Calendar.YEAR, year);
                    expiryCalendar.set(Calendar.MONTH, month);
                    expiryCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    showTimePicker();
                },
                expiryCalendar.get(Calendar.YEAR),
                expiryCalendar.get(Calendar.MONTH),
                expiryCalendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void showTimePicker() {
        // ... (TimePickerDialog implementation) ...
        new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    expiryCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    expiryCalendar.set(Calendar.MINUTE, minute);
                    updateExpiryTimeLabel();
                },
                expiryCalendar.get(Calendar.HOUR_OF_DAY),
                expiryCalendar.get(Calendar.MINUTE),
                false)
                .show();
    }

    private void updateExpiryTimeLabel() {
        String format = "dd MMM yyyy, h:mm a";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());
        et_expiry_time.setText(sdf.format(expiryCalendar.getTime()));
    }
}