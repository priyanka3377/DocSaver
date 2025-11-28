package com.example.loginpage;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import de.hdodenhof.circleimageview.CircleImageView;

public class Settings extends AppCompatActivity {

    TextView nameProfile, myaccount, security, deleteaccount, help, about, logout;
    CircleImageView profilePic;
    FirebaseAuth auth;
    DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        myaccount = findViewById(R.id.myaccount);
        security = findViewById(R.id.security);
        deleteaccount = findViewById(R.id.deleteacc);
        help = findViewById(R.id.help);
        about = findViewById(R.id.about);
        logout = findViewById(R.id.logout);
        profilePic = findViewById(R.id.profileimage);
        nameProfile = findViewById(R.id.nameProfile);

        auth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference("users");

        if (auth.getCurrentUser() != null) {
            String uid = auth.getCurrentUser().getUid();

            dbRef.child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String name = snapshot.child("name").getValue(String.class);
                        String proPic = snapshot.child("setProfilePic").getValue(String.class);

                        nameProfile.setText(name);

                        if (proPic != null) {
                            byte[] decodedBytes = Base64.decode(proPic, Base64.DEFAULT);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            profilePic.setImageBitmap(decodedBitmap);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }



        myaccount.setOnClickListener(v -> startActivity(new Intent(Settings.this, MyAccount.class)));


        security.setOnClickListener(v -> startActivity(new Intent(Settings.this, Security.class)));

        deleteaccount.setOnClickListener(v -> startActivity(new Intent(Settings.this, DeleteAccount.class)));

        help.setOnClickListener(v -> startActivity(new Intent(Settings.this, Help.class)));

        about.setOnClickListener(v -> startActivity(new Intent(Settings.this, AboutPage.class)));

        logout.setOnClickListener(v -> {
                AlertDialog.Builder logoutAlert = new AlertDialog.Builder(Settings.this);
                logoutAlert.setTitle("Logout");
                logoutAlert.setMessage("Are you sure you want to Logout?");
                logoutAlert.setIcon(R.drawable.ic_logout);
                logoutAlert.setPositiveButton("Yes",(dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();

                    Intent iLoginLogout = new Intent(Settings.this, LoginPage.class);
                    iLoginLogout.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(iLoginLogout);
                    finish();
                });
                logoutAlert.setNegativeButton("No",(dialog, which) -> dialog.dismiss());
                logoutAlert.show();
        });
    }
}