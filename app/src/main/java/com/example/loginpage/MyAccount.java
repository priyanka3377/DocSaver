package com.example.loginpage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

public class MyAccount extends AppCompatActivity {

    TextView showemail, showpassword;
    FirebaseAuth auth;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_account);

        showemail = findViewById(R.id.showemail);
        showpassword = findViewById(R.id.showpassword);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users");

        if (auth.getCurrentUser() != null){
            String uid = auth.getCurrentUser().getUid();

            database.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Log.d("MyAccount", "Full snapshot: " + snapshot.getValue());
                        String useremail = snapshot.child("useremail").getValue(String.class);
                        Log.d("MyAccount", "Fetched email: " + useremail);
                        showemail.setText(getString(R.string.email_label, useremail != null ? useremail : ""));

                        showpassword.setText(getString(R.string.password_label));
                    }else {
                        Toast.makeText(MyAccount.this, "No data found", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(MyAccount.this, "Failed to load: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
        }

        showpassword.setOnClickListener(v -> {
            Intent intent = new Intent(MyAccount.this, ChangePassword.class);
            startActivity(intent);
        });

    }
}