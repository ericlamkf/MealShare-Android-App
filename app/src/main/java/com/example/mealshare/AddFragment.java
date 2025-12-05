package com.example.mealshare;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link AddFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

public class AddFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private final int GALLERY_PERMISSION_CODE = 100;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private EditText foodNameEditText;
    private EditText descriptionEditText;
    private EditText quantityEditText;
    private Uri mImageUri = null; // Holds the URI of the selected image
    private ImageView imagePreview;
    private TextView tapToUploadText; // For hiding/showing the overlay text
    private ActivityResultLauncher<Intent> mImagePickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ImageView removeBTN;
    private ChipGroup chipGroupTags;
    private Button postListingButton;

    public AddFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment AddFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static AddFragment newInstance(String param1, String param2) {
        AddFragment fragment = new AddFragment();
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

    private EditText et_expiry_time;
    private Calendar expiryCalendar;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        // Permission granted, proceed to open the gallery
                        openGalleryIntent();
                    } else {
                        // Permission denied
                        Toast.makeText(requireContext(), "Permission denied to access gallery.", Toast.LENGTH_SHORT).show();
                    }
                });
        // FIREBASE INITIALIZATION
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        et_expiry_time = view.findViewById(R.id.et_expiry_time);
        expiryCalendar = Calendar.getInstance();
        et_expiry_time.setOnClickListener(v -> showDatePicker());

        imagePreview = view.findViewById(R.id.image_preview);
        tapToUploadText = view.findViewById(R.id.tv_tap_to_upload);
        ConstraintLayout photoUploadContainer = view.findViewById(R.id.photo_upload_container);

        mImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK &&
                            result.getData() != null &&
                            result.getData().getData() != null) {

                        // Success! Get the URI and set the preview
                        mImageUri = result.getData().getData();
                        imagePreview.setImageURI(mImageUri);

                        // Hide the "Tap to upload photo" text
                        removeBTN.setVisibility(View.VISIBLE);
                        tapToUploadText.setVisibility(View.GONE);
                    }
                });
        photoUploadContainer.setOnClickListener(v -> openGallery());
        imagePreview.setOnClickListener(v -> openGallery());

        removeBTN = view.findViewById(R.id.removeBTN);
        removeBTN.setVisibility(View.GONE);
        removeBTN.setOnClickListener(v -> resetImage());

        foodNameEditText = view.findViewById(R.id.et_food_name);
        descriptionEditText = view.findViewById(R.id.et_description);
        quantityEditText = view.findViewById(R.id.et_quantity);
        chipGroupTags = view.findViewById(R.id.chip_group_tags);
        postListingButton = view.findViewById(R.id.btn_post_listing);
        postListingButton.setOnClickListener(v -> postListing());
    }

    private void postListing() {
        String foodName = foodNameEditText.getText().toString().trim();
        String quantity = quantityEditText.getText().toString().trim();

        // Simple Validation Check
        if (foodName.isEmpty() || quantity.isEmpty() || et_expiry_time.getText().toString().isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Generate a unique Meal ID now for storage reference
        // We use the Firestore Document ID generation method here for consistency
        String mealId = db.collection("meals").document().getId();

        if (mImageUri != null) {
            uploadImageAndSaveData(mealId);
        } else {
            // Use a placeholder/default URL if no image is provided
            saveMealDataToFirestore(mealId, null);
        }
    }

    private void uploadImageAndSaveData(String mealId) {
        StorageReference fileRef = storage.getReference()
                .child("meals/" + mealId + ".jpg");

        fileRef.putFile(mImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get the URL after successful upload
                    fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String imageUrl = uri.toString();
                        saveMealDataToFirestore(mealId, imageUrl);
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    // Optional: Proceed without image or halt post
                    saveMealDataToFirestore(mealId, null);
                });
    }

    private void saveMealDataToFirestore(String mealId, @Nullable String imageUrl) {
        String userId = auth.getCurrentUser().getUid();

        // 1. Collect Selected Tags
        List<String> selectedTagsList = new ArrayList<>();
        List<Integer> checkedChipIds = chipGroupTags.getCheckedChipIds();
        for (int id : checkedChipIds) {
            Chip chip = chipGroupTags.findViewById(id);
            if (chip != null) {
                selectedTagsList.add(chip.getText().toString());
            }
        }

        // 2. Create the data map
        Map<String, Object> mealData = new HashMap<>();
        mealData.put("userId", userId);
        mealData.put("foodName", foodNameEditText.getText().toString().trim());
        mealData.put("description", descriptionEditText.getText().toString().trim());
        mealData.put("quantity", quantityEditText.getText().toString().trim());
        // Store the expiry date as a Firebase Timestamp object
        mealData.put("expiryTime", expiryCalendar.getTime());
        mealData.put("imageUrl", imageUrl);
        mealData.put("tags", selectedTagsList);
        mealData.put("location", "Petaling Jaya, Selangor"); // Placeholder, integrate GPS later
        mealData.put("timestamp", FieldValue.serverTimestamp());

        // 3. Write the document to the 'meals' collection
        db.collection("meals")
                .document(mealId) // Use the generated ID
                .set(mealData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "Listing posted successfully!", Toast.LENGTH_SHORT).show();
                    resetUI(); // Clear the form
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
        resetImage(); // Clear the image preview
    }

    private void resetImage() {
        mImageUri = null;

        // Clear the image preview
        imagePreview.setImageURI(null);
        imagePreview.setImageResource(0); // Ensure no residual image is visible

        // Revert the UI state
        tapToUploadText.setVisibility(View.VISIBLE); // Show the prompt text
        removeBTN.setVisibility(View.GONE); // Hide the 'X' button
    }

    private void openGallery() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {

            // Permission is already granted, proceed directly
            openGalleryIntent();
        } else {
            // Permission is NOT granted, launch the request
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        }
    }

    // Renamed method to avoid confusion
    private void openGalleryIntent() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        mImagePickerLauncher.launch(galleryIntent);
    }

    private void showDatePicker() {
        new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    expiryCalendar.set(Calendar.YEAR, year);
                    expiryCalendar.set(Calendar.MONTH, month);
                    expiryCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    showTimePicker(); // Call time picker immediately after date is set
                },
                expiryCalendar.get(Calendar.YEAR),
                expiryCalendar.get(Calendar.MONTH),
                expiryCalendar.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void showTimePicker() {
        new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> {
                    expiryCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    expiryCalendar.set(Calendar.MINUTE, minute);
                    updateExpiryTimeLabel(); // Display the final chosen date and time
                },
                expiryCalendar.get(Calendar.HOUR_OF_DAY),
                expiryCalendar.get(Calendar.MINUTE),
                false) // false for 12-hour clock, true for 24-hour clock
                .show();
    }

    private void updateExpiryTimeLabel() {
        // Define the desired format for display (e.g., "dd MMM yyyy, hh:mm a")
        String format = "dd MMM yyyy, h:mm a";
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.getDefault());

        // Update the EditText with the formatted date/time
        et_expiry_time.setText(sdf.format(expiryCalendar.getTime()));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_add, container, false);
    }
}