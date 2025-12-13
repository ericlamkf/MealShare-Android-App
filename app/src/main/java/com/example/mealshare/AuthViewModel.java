package com.example.mealshare;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Simple ViewModel that centralises Firebase Auth sign-in and sign-up calls
 * and exposes results via LiveData so Activities/Fragments can observe.
 */
public class AuthViewModel extends ViewModel {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<FirebaseUser> authUser = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<FirebaseUser> getAuthUser() { return authUser; }

    public void clearAuthUser(){ authUser.setValue(null); }
    public void clearMessage(){ message.setValue(null); }

    public void login(String email, String password){
        if(email == null || password == null) {
            message.setValue("Invalid credentials");
            return;
        }
        isLoading.setValue(true);
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        isLoading.setValue(false);
                        message.setValue("Login Successful");
                        authUser.setValue(authResult.getUser());
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        isLoading.setValue(false);
                        message.setValue("Login Failed: " + e.getMessage());
                        authUser.setValue(null);
                    }
                });
    }

    public void createUser(String email, String password){
        if(email == null || password == null){
            message.setValue("Invalid credentials");
            return;
        }
        isLoading.setValue(true);
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        isLoading.setValue(false);
                        message.setValue("Registration successful");
                        authUser.setValue(authResult.getUser());
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        isLoading.setValue(false);
                        message.setValue("Registration failed: " + e.getMessage());
                        authUser.setValue(null);
                    }
                });
    }
}
