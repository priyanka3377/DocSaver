package com.example.loginpage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ValueEventListener;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import de.hdodenhof.circleimageview.CircleImageView;

public class Profile extends AppCompatActivity {
    TextView edtName, edtDob, edtPhone, edtEmail;
    TextView edtGender;
    Button btnSaveDetails;

    CircleImageView profileImageView;

    FirebaseAuth auth;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);

            edtName = findViewById(R.id.viewname);
            edtDob = findViewById(R.id.viewdob);
            edtPhone = findViewById(R.id.viewphone);
            edtGender = findViewById(R.id.viewgender);
            edtEmail = findViewById(R.id.viewemail);
            profileImageView = findViewById(R.id.profileImageView);
            btnSaveDetails = findViewById(R.id.edtdetails);

            btnSaveDetails.setOnClickListener(v -> {
                    Intent intent = new Intent(Profile.this, EditPersonalDetails.class);
                    startActivity(intent);
            });

            auth = FirebaseAuth.getInstance();
            database = FirebaseDatabase.getInstance().getReference("users");


            if (auth.getCurrentUser() != null) {
                String uid = auth.getCurrentUser().getUid();

                database.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {

                            String name = snapshot.child("name").getValue(String.class);
                            String dob = snapshot.child("dob").getValue(String.class);
                            String phone = snapshot.child("phone").getValue(String.class);
                            String gender = snapshot.child("gender").getValue(String.class);
                            String email = snapshot.child("useremail").getValue(String.class);
                            String profileImage = snapshot.child("setProfilePic").getValue(String.class);

                            if (phone != null) {
                                phone = formatPhoneNo(phone);
                            }


                            edtName.setText(getString(R.string.name_label, name != null ? name : ""));
                            edtDob.setText(getString(R.string.dob_label, dob != null ? dob : ""));
                            edtPhone.setText(getString(R.string.phone_label, phone != null ? phone : ""));
                            edtGender.setText(getString(R.string.gender_label, gender != null ? gender : ""));
                            edtEmail.setText(getString(R.string.email_label, email != null ? email : ""));

                            if (profileImage != null) {
                                byte[] decodedBytes = Base64.decode(profileImage, Base64.DEFAULT);
                                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                profileImageView.setImageBitmap(decodedBitmap);
                            }
                        } else {
                            Toast.makeText(Profile.this, "No data found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Profile.this, "Failed to load: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            }
    }

    private String formatPhoneNo(String fullNumber) {
        if (fullNumber == null || fullNumber.isEmpty()) {
            return "";
        }

        // If number starts with +91 and length is correct, format it
        if (fullNumber.startsWith("+91") && fullNumber.length() == 13) {
            String numberPart = fullNumber.substring(3); // remove +91
            return "+91 " + numberPart.substring(0, 5) + "-" + numberPart.substring(5);
        }

        // If number already has + but is not +91, just return as is
        if (fullNumber.startsWith("+")) {
            return fullNumber;
        }

        // If it is a plain 10 digit number (without +91), add format
        if (fullNumber.length() == 10) {
            return "+91 " + fullNumber.substring(0, 5) + "-" + fullNumber.substring(5);
        }

        // Otherwise, return as is
        return fullNumber;
    }


    @Override
    protected void onResume() {
        super.onResume();
        loadUserDetails();
    }

    private void loadUserDetails() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

            dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String dob = snapshot.child("dob").getValue(String.class);
                        String phone = snapshot.child("phone").getValue(String.class);
                        String gender = snapshot.child("gender").getValue(String.class);
                        String email = snapshot.child("useremail").getValue(String.class);
                        String profileImage = snapshot.child("setProfilePic").getValue(String.class);


                        edtName.setText(getString(R.string.name_label, name != null ? name : ""));
                        edtDob.setText(getString(R.string.dob_label, dob != null ? dob : ""));
                        String formattedPhone = (phone != null) ? formatPhoneNo(phone) : "";
                        edtPhone.setText(getString(R.string.phone_label, formattedPhone));
                        edtGender.setText(getString(R.string.gender_label, gender != null ? gender : ""));
                        edtEmail.setText(getString(R.string.email_label, email != null ? email : ""));

                        if (profileImage != null) {
                            byte[] decodedBytes = Base64.decode(profileImage, Base64.DEFAULT);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            profileImageView.setImageBitmap(decodedBitmap);
                        }
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(Profile.this, "Failed to load details", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
        }
    }



}
