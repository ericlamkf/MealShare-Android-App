package com.example.mealshare;

import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseUser;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class RegisterActivity extends AppCompatActivity {
    private static final String DEFAULT_PROFILE_PIC_URL = "https://firebasestorage.googleapis.com/v0/b/mealshare-76ea8.firebasestorage.app/o/profile_pic.png?alt=media&token=2837c28e-a65f-4ee9-a286-1cc788f6f8ef";
    private AuthViewModel authViewModel;
    private EditText register_email, register_password, register_name, register_username;
    private Button register_btn;
    private ImageView btnRemovePic;
    private CircleImageView profile_pic;
    private TextView addPhotoTV;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private Uri mImageUri = null;
    private ActivityResultLauncher<Intent> mImagePickerLauncher;
    // pending registration data while ViewModel performs auth account creation
    private String pendingName = null;
    private String pendingUsername = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, 0, systemBars.right, 0);
            return insets;
        });

        TextView toLogin = findViewById(R.id.toLogin);
        toLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            }
        });

        // ALL firebase services initialisation
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // ViewModel for auth operations
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Observe auth results for registration flow
        authViewModel.getMessage().observe(this, msg -> {
            if (msg != null) {
                Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_SHORT).show();
                authViewModel.clearMessage();
            }
        });

        // Keep pending registration details while ViewModel creates auth account
        // When authUser becomes available, continue the rest of registration (upload image / save data)
        authViewModel.getAuthUser().observe(this, firebaseUser -> {
            if (firebaseUser != null && pendingName != null && pendingUsername != null) {
                // Continue registration flow in activity (upload image or save default)
                if (mImageUri != null) {
                    uploadImageAndSaveData(firebaseUser, pendingName, pendingUsername);
                } else {
                    saveUserDataToFirestore(firebaseUser, pendingName, pendingUsername, DEFAULT_PROFILE_PIC_URL);
                }
                // clear pending
                pendingName = null;
                pendingUsername = null;
                authViewModel.clearAuthUser();
            }
        });

        // ALL views initialisation
        register_email = findViewById(R.id.register_email);
        register_name = findViewById(R.id.register_name);
        register_username = findViewById(R.id.register_username);
        register_password = findViewById(R.id.register_password);
        register_btn = findViewById(R.id.register_btn);
        btnRemovePic = findViewById(R.id.BtnRemovePic);

        // Views for profile picture
        profile_pic = findViewById(R.id.profile_pic);
        addPhotoTV = findViewById(R.id.addPhotoTV);

        mImagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                            mImageUri = result.getData().getData();
                            profile_pic.setImageURI(mImageUri);
                            btnRemovePic.setVisibility(View.VISIBLE);
                            addPhotoTV.setVisibility(View.GONE);
                        }
        });

        View.OnClickListener photoClickListener = v -> openGallery();
        profile_pic.setOnClickListener(photoClickListener);
        addPhotoTV.setOnClickListener(photoClickListener);

        register_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        btnRemovePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removePic();
            }
        });

    }

    private void removePic() {
        mImageUri = null;
        profile_pic.setImageResource(R.drawable.profile_pic);
        btnRemovePic.setVisibility(View.GONE);
        addPhotoTV.setVisibility(View.VISIBLE);
    }

    private void openGallery(){
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        mImagePickerLauncher.launch(galleryIntent);
    }

    private void registerUser(){
        String userEmail = register_email.getText().toString().trim();
        String userPassword = register_password.getText().toString().trim();
        String userName = register_name.getText().toString().trim();
        String userUsername = register_username.getText().toString().trim().toLowerCase();

        // Full Validation
        if(TextUtils.isEmpty(userName)){
            Toast.makeText(this, "Full Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(userUsername)) {
            register_username.setError("Username cannot be empty");
            return;
        }
        if (TextUtils.isEmpty(userEmail)) {
            register_email.setError("Email cannot be empty");
            return;
        }
        if (TextUtils.isEmpty(userPassword)) {
            register_password.setError("Password cannot be empty");
            return;
        }
        if(userPassword.length()<6){
            register_password.setError("Password must be at least 6 characters long");
            return;
        }

        Toast.makeText(this, "Registering User...", Toast.LENGTH_SHORT).show();

        checkUsernameAndRegister(userName, userUsername, userEmail, userPassword);
    }

    private void checkUsernameAndRegister(String userName, String userUsername, String userEmail, String userPassword){
        DocumentReference usernameRef = db.collection("usernames").document(userUsername);
        usernameRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if(document.exists()){
                    register_username.setError("Username already exists");
                    Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show();
                }else{
                    createUserInAuth(userName, userUsername, userEmail, userPassword);}
            }else{
                Toast.makeText(this, "Error checking username", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createUserInAuth(String userName, String userUsername, String userEmail, String userPassword){
        // Store pending registration details so observer (below) can continue once ViewModel finishes creating
        pendingName = userName;
        pendingUsername = userUsername;
        // Delegate auth creation to ViewModel; result will be observed in onCreate
        authViewModel.createUser(userEmail, userPassword);
    }

    private void uploadImageAndSaveData(FirebaseUser firebaseUser, String name, String username){
        String uid = firebaseUser.getUid();
        StorageReference fileRef = storage.getReference().child("profile_images/" + uid + ".jpg");

        fileRef.putFile(mImageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    getDownloadUrlAndSaveData(fileRef, firebaseUser, name, username);
                })
                .addOnFailureListener(e -> {
                    firebaseUser.delete();
                    Toast.makeText(RegisterActivity.this, "Image upload failed" + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void getDownloadUrlAndSaveData(StorageReference fileRef, FirebaseUser firebaseUser, String name, String username){
        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
            String profileImageUrl = uri.toString();
            // Save data to Firestore
            saveUserDataToFirestore(firebaseUser, name, username, profileImageUrl);
        })
        .addOnFailureListener(e -> {
            fileRef.delete(); // Delete the orphaned file
            firebaseUser.delete(); // Delete the Auth account
            Toast.makeText(RegisterActivity.this, "Failed to get image URL : " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    private void saveUserDataToFirestore(FirebaseUser firebaseUser, String name, String username, String profileImageUrl){
        String uid = firebaseUser.getUid();
        String email = firebaseUser.getEmail();

        WriteBatch batch = db.batch();

        // 1. Main user document in 'users' collection
        DocumentReference userRef = db.collection("users").document(uid);
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("name", name);
        userData.put("username", username);
        userData.put("email", email);
        userData.put("profileImageUrl", profileImageUrl);
        batch.set(userRef, userData);

        // 2. Username lookup document in 'usernames' collection
        DocumentReference usernameRef = db.collection("usernames").document(username);
        Map<String, Object> usernameData = new HashMap<>();
        usernameData.put("uid", uid);
        batch.set(usernameRef, usernameData);

        batch.commit().addOnCompleteListener(task -> {
            if(task.isSuccessful()){
                Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                finish();
            }else{
                Toast.makeText(RegisterActivity.this, "Error saving user data:" + task.getException().getMessage(), Toast.LENGTH_SHORT).show();

                // Delete user from Firebase
                firebaseUser.delete()
                        .addOnCompleteListener(deleteTask -> {
                            if(deleteTask.isSuccessful()){
                                Toast.makeText(RegisterActivity.this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                            } else {
                                // This indicates a serious failure, maybe log it to Crashlytics
                                Toast.makeText(RegisterActivity.this, "Failed to delete account from Auth.", Toast.LENGTH_LONG).show();
                            }
                        });
            }
        });
    }


}