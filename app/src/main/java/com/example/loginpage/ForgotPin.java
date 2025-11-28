package com.example.loginpage;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ForgotPin extends AppCompatActivity {

    EditText edtpassword, edtnewpin, edtrenewpin;
    Button resetpin;
    DatabaseReference userRef;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_pin);

        edtpassword = findViewById(R.id.enterpass);
        edtnewpin = findViewById(R.id.forgotnewpin);
        edtrenewpin = findViewById(R.id.forgotrenewpin);
        resetpin = findViewById(R.id.resetpin);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            resetpin.setOnClickListener(v -> {
                String password = edtpassword.getText().toString().trim();
                String newpin = edtnewpin.getText().toString().trim();
                String reenternewpin = edtrenewpin.getText().toString().trim();

                if (TextUtils.isEmpty(password) || TextUtils.isEmpty(newpin) || TextUtils.isEmpty(reenternewpin)) {
                    Toast.makeText(ForgotPin.this, "All fields are required", Toast.LENGTH_SHORT).show();
                }

                if (newpin.length() != 4) {
                    Toast.makeText(ForgotPin.this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show();
                }

                if (!newpin.equals(reenternewpin)) {
                    Toast.makeText(ForgotPin.this, "Both the PINs should match", Toast.LENGTH_SHORT).show();
                }

                FirebaseUser user = auth.getCurrentUser();
                if (user == null || user.getEmail() == null) {
                    Toast.makeText(ForgotPin.this, "No user logged in", Toast.LENGTH_SHORT).show();
                    return;
                }

                user.reauthenticate(EmailAuthProvider.getCredential(user.getEmail(), password))
                        .addOnSuccessListener(unused ->
                            userRef.child("pin").setValue(newpin).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(ForgotPin.this, "PIN reset successful", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(ForgotPin.this, "Failed to reset PIN", Toast.LENGTH_SHORT).show();
                                }
                            })
                        ).addOnFailureListener(e -> Toast.makeText(ForgotPin.this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            });
        } else {
            Toast.makeText(this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
        }


    }
}