package com.example.loginpage;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

@SuppressLint("CustomSplashScreen")
public class SplashScreen extends AppCompatActivity {

    LottieAnimationView lottieAnimationView;
    FirebaseAuth auth;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash_screen);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users");


        try {
            lottieAnimationView = findViewById(R.id.lottieview);
            lottieAnimationView.setAnimation(R.raw.docureader);
            lottieAnimationView.playAnimation();
        } catch (Exception e) {
            Log.e("LottieError", "Failed to play animation", e);
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (auth.getCurrentUser() != null){
                auth.getCurrentUser().reload()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && auth.getCurrentUser() != null) {
                                if (auth.getCurrentUser().isEmailVerified()) {
                                    String uid = auth.getCurrentUser().getUid();
                                    database.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            Intent intent;
                                            if (snapshot.exists() && snapshot.child("name").getValue() != null) {
                                                if (snapshot.child("pin").exists()) {
                                                    intent = new Intent(SplashScreen.this, EnterPin.class);
                                                } else {
                                                    intent = new Intent(SplashScreen.this, MpinPage.class);
                                                }
                                            } else {
                                                intent = new Intent(SplashScreen.this, PersonalDetails.class);
                                            }
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Toast.makeText(SplashScreen.this, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(SplashScreen.this, LoginPage.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                                } else {
                                    Intent intent = new Intent(SplashScreen.this, LoginPage.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                }
                            } else {
                                FirebaseAuth.getInstance().signOut();
                                Intent intent = new Intent(SplashScreen.this, LoginPage.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            }
                        });
            }else {
                Intent intent = new Intent(SplashScreen.this, LoginPage.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }, 3000);

    }
}