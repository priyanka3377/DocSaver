package com.example.loginpage;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Security extends AppCompatActivity {

    TextView mpin, forgotmpin;
    FirebaseAuth auth;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_security);

        mpin = findViewById(R.id.mpin);
        forgotmpin = findViewById(R.id.forgotmpin);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users");

        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();
            database.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        mpin.setText(getString(R.string.masked_pin));
                    }else {
                        Toast.makeText(Security.this, "No data found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(Security.this, "Failed to load: "+error.getMessage(), Toast.LENGTH_SHORT).show();

                }
            });
        }else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }

        mpin.setOnClickListener(v -> startActivity(new Intent(Security.this, UpdatePin.class)));

        forgotmpin.setOnClickListener(v -> startActivity(new Intent(Security.this, ForgotPin.class)));



    }
}