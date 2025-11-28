package com.example.loginpage;

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

public class UpdatePin extends AppCompatActivity {

    EditText oldpin, newpin, reenternewpin;
    Button updatepin;

    DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_update_pin);

        oldpin = findViewById(R.id.oldpin);
        newpin = findViewById(R.id.newpin);
        reenternewpin = findViewById(R.id.reenternewpin);
        updatepin = findViewById(R.id.updatepin);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "No user signed in. Please login again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String uid = currentUser.getUid();
        userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        updatepin.setOnClickListener(v -> {
                String edtoldpin = oldpin.getText().toString().trim();
                String edtnewpin = newpin.getText().toString().trim();
                String edtreenternewpin = reenternewpin.getText().toString().trim();

                if (TextUtils.isEmpty(edtoldpin) || TextUtils.isEmpty(edtnewpin) || TextUtils.isEmpty(edtreenternewpin)) {
                    Toast.makeText(UpdatePin.this, "All fields are required", Toast.LENGTH_SHORT).show();
                }

                if (edtnewpin.length() != 4) {
                    Toast.makeText(UpdatePin.this, "PIN must be 4 digits", Toast.LENGTH_SHORT).show();
                }

                if (!edtnewpin.equals(edtreenternewpin)) {
                    Toast.makeText(UpdatePin.this, "Both the PINs should match", Toast.LENGTH_SHORT).show();
                }

                userRef.child("pin").get().addOnSuccessListener(dataSnapshot -> {
                        if (dataSnapshot.exists()) {
                            String savedpin = dataSnapshot.getValue(String.class);

                            if (savedpin != null && savedpin.equals(edtoldpin)) {
                                userRef.child("pin").setValue(edtnewpin).addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            Toast.makeText(UpdatePin.this, "PIN updated successfully", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Toast.makeText(UpdatePin.this, "Failed to update PIN", Toast.LENGTH_SHORT).show();
                                        }
                                });
                            } else {
                                Toast.makeText(UpdatePin.this, "Old PIN is incorrect", Toast.LENGTH_SHORT).show();
                            }
                        }else {
                            Toast.makeText(UpdatePin.this, "No PIN found", Toast.LENGTH_SHORT).show();
                        }
                }) .addOnFailureListener(e -> Toast.makeText(UpdatePin.this, "Error: "+e.getMessage(), Toast.LENGTH_SHORT).show());
        });



    }
}