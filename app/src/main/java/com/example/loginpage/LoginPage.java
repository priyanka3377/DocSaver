package com.example.loginpage;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginPage extends AppCompatActivity {

    EditText emailInput, passwordInput;
    Button loginBtn, resendEmailBtn;
    TextView signupLink, forgotPassword;

    LottieAnimationView lottieAnimationView;
    FirebaseAuth mAuth;
    private LinearLayout loadingOverlay;

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseAuth.getInstance().signOut();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);

        emailInput = findViewById(R.id.edtemail);
        passwordInput = findViewById(R.id.edtpassword);
        loginBtn = findViewById(R.id.login);
        resendEmailBtn = findViewById(R.id.resendEmailBtn);
        signupLink = findViewById(R.id.signinBtn);
        lottieAnimationView = findViewById(R.id.loading);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        forgotPassword = findViewById(R.id.forgotpass);


        mAuth = FirebaseAuth.getInstance();

        boolean fromReset = getIntent().getBooleanExtra("resetFlow",false);

        resendEmailBtn.setOnClickListener(v -> resendVerificationEmail());

        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginPage.this, ForgotPassword.class);
            startActivity(intent);
            finish();
        });

        signupLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginPage.this, SignInPage.class);
            startActivity(intent);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            finish();
        });

        loginBtn.setOnClickListener(v -> {
            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }

            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();


            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginPage.this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }


            if(TextUtils.isEmpty(email)){
                Toast.makeText(LoginPage.this, "Please enter Email", Toast.LENGTH_SHORT).show();
            }else if(TextUtils.isEmpty(password)){
                Toast.makeText(LoginPage.this, "Please enter Password", Toast.LENGTH_SHORT).show();
            }else if(password.length()<8){
                Toast.makeText(LoginPage.this, "Password should be more than 7 characters", Toast.LENGTH_SHORT).show();
            }else {
                loadingOverlay.setVisibility(View.VISIBLE);
                lottieAnimationView.playAnimation();
                mAuth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            lottieAnimationView.cancelAnimation();
                            loadingOverlay.setVisibility(View.GONE);
                            if (task.isSuccessful()) {

                                if (fromReset) {
                                    Toast.makeText(LoginPage.this, "Password Changed Successfully!", Toast.LENGTH_SHORT).show();
                                }
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null && user.isEmailVerified()) {
                                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                                            .getReference("users")
                                            .child(user.getUid());
                                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists() && snapshot.hasChild("name") && snapshot.hasChild("useremail")) {
                                                Intent intent = new Intent(LoginPage.this, EnterPin.class);
                                                startActivity(intent);
                                            } else {
                                                Intent intent = new Intent(LoginPage.this, PersonalDetails.class);
                                                startActivity(intent);
                                            }
                                            finish();
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Toast.makeText(LoginPage.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });

                                } else {
                                    Toast.makeText(LoginPage.this, "Please verify your email before logging in", Toast.LENGTH_LONG).show();
                                    resendEmailBtn.setVisibility(View.VISIBLE);
                                }

                            }else {
                                Exception e = task.getException();
                                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                    Toast.makeText(LoginPage.this, "Incorrect email or password", Toast.LENGTH_SHORT).show();
                                } else if (e instanceof FirebaseAuthInvalidUserException) {
                                    Toast.makeText(LoginPage.this, "No account found with this email", Toast.LENGTH_SHORT).show();
                                } else {
                                    if (e != null && e.getMessage() != null) {
                                        Toast.makeText(LoginPage.this, "Login failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(LoginPage.this, "Login failed", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }).addOnFailureListener(e -> Toast.makeText(LoginPage.this, "Login failed", Toast.LENGTH_SHORT).show());
            }
        });
        }





    private void resendVerificationEmail() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && !user.isEmailVerified()) {
            user.sendEmailVerification().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(LoginPage.this, "Verification email sent.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(LoginPage.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
