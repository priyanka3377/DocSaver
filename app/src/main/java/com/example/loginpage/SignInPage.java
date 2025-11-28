package com.example.loginpage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignInPage extends AppCompatActivity {

    EditText emailInput, passwordInput, confirmPasswordInput;
    Button signupBtn;
    TextView signinLink;
    FirebaseAuth mAuth;
    FirebaseDatabase database;

    LottieAnimationView loading;
    private LinearLayout loadingOverlay;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin_page);

        emailInput = findViewById(R.id.signinemail);
        passwordInput = findViewById(R.id.signinpass);
        confirmPasswordInput = findViewById(R.id.signincpass);
        signupBtn = findViewById(R.id.signinBtn);
        signinLink = findViewById(R.id.signinlogin);
        database = FirebaseDatabase.getInstance();
        loading = findViewById(R.id.loadingbar);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        mAuth = FirebaseAuth.getInstance();

        signupBtn.setOnClickListener(v -> {
                View view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();
                String confirmPassword = confirmPasswordInput.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(SignInPage.this, "All fields are required", Toast.LENGTH_SHORT).show();
                } else if (password.length() < 8) {
                    passwordInput.setError("Password must be more than 7 characters");
                } else if (!password.equals(confirmPassword)) {
                    passwordInput.setError("Password doesn't match");
                } else {
                    loadingOverlay.setVisibility(View.VISIBLE);
                    loading.playAnimation();

                    mAuth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        loadingOverlay.setVisibility(View.GONE);
                                        loading.cancelAnimation();

                                        if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                                            String id = task.getResult().getUser().getUid();
                                            DatabaseReference reference = database.getReference().child("users").child(id);

                                            Users users = new Users(email, password, id);
                                            reference.setValue(users).addOnCompleteListener(task1 -> {
                                                if (task.isSuccessful()) {
                                                    FirebaseUser user = mAuth.getCurrentUser();
                                                    if (user != null) {
                                                        user.sendEmailVerification()
                                                                .addOnCompleteListener(verifyTask -> {
                                                                    if (verifyTask.isSuccessful()) {
                                                                        loading.setVisibility(View.VISIBLE);
                                                                        loading.playAnimation();
                                                                        Toast.makeText(SignInPage.this, "Registered! Check your email to verify your account.", Toast.LENGTH_LONG).show();
                                                                        mAuth.signOut();
                                                                        Intent iLoginPage = new Intent(SignInPage.this, LoginPage.class);
                                                                        startActivity(iLoginPage);
                                                                        iLoginPage.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                        finish();
                                                                    } else {
                                                                        Toast.makeText(SignInPage.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                });
                                                    } else {
                                                        Exception e = task.getException();
                                                        if (e != null && e.getMessage() != null) {
                                                            Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            Toast.makeText(SignInPage.this, "Registration failed", Toast.LENGTH_LONG).show();
                                                        }

                                                        loadingOverlay.setVisibility(View.GONE);
                                                        loading.cancelAnimation();
                                                    }
                                                }

                                            });
                                        }else {
                                            Toast.makeText(this, "Failed to create account", Toast.LENGTH_SHORT).show();
                                        }
                                    }else{
                                        Exception e = task.getException();
                                        if (e != null && e.getMessage() != null) {
                                            Toast.makeText(this, "Sign In Failed: "+e.getMessage(), Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(SignInPage.this, "Sign In Failed", Toast.LENGTH_SHORT).show();
                                        }

                                        loadingOverlay.setVisibility(View.GONE);
                                        loading.cancelAnimation();

                                    }
                            });
                }
        });

        signinLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignInPage.this, LoginPage.class);
            startActivity(intent);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            finish();
        });
    }
}

