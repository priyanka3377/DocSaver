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

public class ForgotPassword extends AppCompatActivity {

    Button resetLink;
    EditText forgotEmail;
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        resetLink = findViewById(R.id.resetlink);
        forgotEmail = findViewById(R.id.forgotemail);
        auth = FirebaseAuth.getInstance();

        resetLink.setOnClickListener(v -> {
            String email = forgotEmail.getText().toString().trim();

            if (TextUtils.isEmpty(email)) {
                Toast.makeText(ForgotPassword.this, "Please enter your registered email", Toast.LENGTH_SHORT).show();
            }

            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(ForgotPassword.this, "Reset link sent to " + email + " Please check your email", Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(ForgotPassword.this, LoginPage.class);
                            intent.putExtra("resetFlow", true);
                            startActivity(intent);
                            finish();
                        } else {
                            Exception e = task.getException();
                            if (e != null && e.getMessage() != null) {
                                Toast.makeText(ForgotPassword.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(ForgotPassword.this, "Error", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

    }
}