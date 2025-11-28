package com.example.loginpage;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EnterPin extends AppCompatActivity {
    EditText enterPin;
    Button nextPin;
    FirebaseAuth auth;
    DatabaseReference dbRef;
    String savedPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_enter_pin);

        enterPin = findViewById(R.id.enterPin);
        nextPin = findViewById(R.id.nextPin);

        auth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("users");

        fetchSavedPin();

        nextPin.setOnClickListener(v -> {
            String enteredPin = enterPin.getText().toString().trim();

            if (TextUtils.isEmpty(enteredPin)) {
                enterPin.setError("Please enter your PIN");
                return;
            }

            if (savedPin != null && savedPin.equals(enteredPin)) {
                startActivity(new Intent(EnterPin.this, MainActivity.class));
                finish();
            } else {
                enterPin.setError("Incorrect PIN");
            }
        });
    }

    private void fetchSavedPin() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            dbRef.child(uid).child("pin").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    savedPin = snapshot.getValue(String.class);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(EnterPin.this, "Failed to fetch PIN", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
        }
    }
}