package com.example.loginpage;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MpinPage extends AppCompatActivity {
    EditText edtpin, edtconfirmpin;
    Button setpin;
    DatabaseReference database;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mpin_page);

        edtpin = findViewById(R.id.edtpin);
        edtconfirmpin = findViewById(R.id.edtconfirmpin);
        setpin = findViewById(R.id.setpin);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users");

        setpin.setOnClickListener(v -> {
            String pin = edtpin.getText().toString().trim();
            String confirmPin = edtconfirmpin.getText().toString().trim();

            if (TextUtils.isEmpty(pin) || TextUtils.isEmpty(confirmPin)) {
                Toast.makeText(MpinPage.this, "Enter PIN in both fields", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pin.equals(confirmPin)) {
                Toast.makeText(MpinPage.this, "PINs do not match!", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {

                String uid = currentUser.getUid();
                database.child(uid).child("pin").setValue(pin)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(MpinPage.this, "PIN set successfully!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MpinPage.this, MainActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(MpinPage.this, "Failed to set PIN: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}