package com.example.mealshare;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;

/**
 * ViewModel that centralises Firebase Auth sign-in and sign-up calls
 * with improved error handling and user-friendly error messages.
 */
public class AuthViewModel extends ViewModel {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();
    private final MutableLiveData<AuthError> authError = new MutableLiveData<>();
    private final MutableLiveData<FirebaseUser> authUser = new MutableLiveData<>();

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<AuthError> getAuthError() { return authError; }
    public LiveData<FirebaseUser> getAuthUser() { return authUser; }

    public void clearAuthUser(){ authUser.setValue(null); }
    public void clearMessage(){ message.setValue(null); }
    public void clearAuthError(){ authError.setValue(null); }

    /**
     * Parses FirebaseAuthException error codes into user-friendly AuthError types
     */
    private AuthError parseAuthException(Exception e) {
        if (e instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) e).getErrorCode();
            
            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    return AuthError.INVALID_EMAIL;
                case "ERROR_WRONG_PASSWORD":
                    return AuthError.WRONG_PASSWORD;
                case "ERROR_USER_NOT_FOUND":
                    return AuthError.USER_NOT_FOUND;
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return AuthError.EMAIL_ALREADY_IN_USE;
                case "ERROR_WEAK_PASSWORD":
                    return AuthError.WEAK_PASSWORD;
                case "ERROR_TOO_MANY_REQUESTS":
                    return AuthError.TOO_MANY_REQUESTS;
                case "ERROR_INVALID_CREDENTIAL":
                    return AuthError.INVALID_CREDENTIALS;
                default:
                    return AuthError.UNKNOWN_ERROR;
            }
        }
        
        // Network or other errors
        if (e.getMessage() != null && e.getMessage().contains("network")) {
            return AuthError.NETWORK_ERROR;
        }
        
        return AuthError.UNKNOWN_ERROR;
    }

    public void login(String email, String password){
        if(email == null || password == null) {
            authError.setValue(AuthError.INVALID_CREDENTIALS);
            message.setValue(AuthError.INVALID_CREDENTIALS.getMessage());
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
                        AuthError error = parseAuthException(e);
                        authError.setValue(error);
                        message.setValue(error.getMessage());
                        authUser.setValue(null);
                    }
                });
    }

    public void createUser(String email, String password){
        if(email == null || password == null){
            authError.setValue(AuthError.INVALID_CREDENTIALS);
            message.setValue(AuthError.INVALID_CREDENTIALS.getMessage());
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
                        AuthError error = parseAuthException(e);
                        authError.setValue(error);
                        message.setValue(error.getMessage());
                        authUser.setValue(null);
                    }
                });
    }
}
