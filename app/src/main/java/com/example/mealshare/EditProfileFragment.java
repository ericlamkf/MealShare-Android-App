package com.example.mealshare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText; // 🔥 Imported for Material Design
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class EditProfileFragment extends Fragment {

    private CircleImageView profileImageView;
    // 🔥 Changed to TextInputEditText to match your new XML
    private TextInputEditText editName, editPhone, editEmail, editAddress;
    private Button btnSave;
    private View profileContainer; // Wrapper for image + camera icon

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private Uri mImageUri;
    private ActivityResultLauncher<Intent> mImagePickerLauncher;

    public EditProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        mImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                        mImageUri = result.getData().getData();
                        profileImageView.setImageURI(mImageUri);
                    }
                });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Views
        profileImageView = view.findViewById(R.id.profile_pic);
        profileContainer = view.findViewById(R.id.profileContainer); // 🔥 Use the container for better clicking

        editName = view.findViewById(R.id.editName);
        editPhone = view.findViewById(R.id.editPhone);
        editEmail = view.findViewById(R.id.editEmail);
        editAddress = view.findViewById(R.id.editAddress);
        btnSave = view.findViewById(R.id.btnSave);

        // Load existing data
        loadUserData();

        // 🔥 Set listener on the CONTAINER so clicking the camera icon works too
        profileContainer.setOnClickListener(v -> openGallery());
        profileImageView.setOnClickListener(v -> openGallery()); // Fallback

        btnSave.setOnClickListener(v -> saveProfileChanges());
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        mImagePickerLauncher.launch(galleryIntent);
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            DocumentReference userRef = db.collection("users").document(currentUser.getUid());
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String name = documentSnapshot.getString("name");
                    String phone = documentSnapshot.getString("phone");
                    String email = documentSnapshot.getString("email");
                    String address = documentSnapshot.getString("address");
                    String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                    if (name != null) editName.setText(name);
                    if (phone != null) editPhone.setText(phone);
                    if (email != null) editEmail.setText(email);
                    if (address != null) editAddress.setText(address);

                    if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                        try {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.profile_pic)
                                    .into(profileImageView);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    private void saveProfileChanges() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in to make changes.", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();

        // Basic Validation
        if (name.isEmpty()) {
            editName.setError("Name is required");
            editName.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            editPhone.setError("Phone is required");
            editPhone.requestFocus();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        String uid = currentUser.getUid();
        DocumentReference userRef = db.collection("users").document(uid);

        if (mImageUri != null) {
            // Upload new image
            StorageReference fileRef = storage.getReference().child("profile_images/" + uid + ".jpg");
            fileRef.putFile(mImageUri)
                    .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String profileImageUrl = uri.toString();
                        updateUserData(userRef, profileImageUrl);
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        resetButton();
                    });
        } else {
            // Update text only
            updateUserData(userRef, null);
        }
    }

    private void updateUserData(DocumentReference userRef, @Nullable String newProfileImageUrl) {
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        String email = editEmail.getText().toString().trim();
        String address = editAddress.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("email", email);
        updates.put("address", address);

        if (newProfileImageUrl != null) {
            updates.put("profileImageUrl", newProfileImageUrl);
        }

        userRef.update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().popBackStack();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    resetButton();
                });
    }

    private void resetButton() {
        if (btnSave != null) {
            btnSave.setEnabled(true);
            btnSave.setText("Save Changes");
        }
    }
}