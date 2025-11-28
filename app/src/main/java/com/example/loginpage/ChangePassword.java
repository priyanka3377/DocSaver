package com.example.loginpage;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePassword extends AppCompatActivity {

    EditText edtCurrentPassword, edtNewPassword, edtConfirmNewPassword;
    Button changePassword;

    FirebaseUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        edtCurrentPassword = findViewById(R.id.etCurrentPassword);
        edtNewPassword = findViewById(R.id.etNewPassword);
        edtConfirmNewPassword = findViewById(R.id.etConfirmPassword);
        changePassword = findViewById(R.id.btnUpdatePassword);

        user = FirebaseAuth.getInstance().getCurrentUser();

        changePassword.setOnClickListener(t -> {
            String currentPass = edtCurrentPassword.getText().toString().trim();
            String newPass = edtNewPassword.getText().toString().trim();
            String confirmPass = edtConfirmNewPassword.getText().toString().trim();

            if (TextUtils.isEmpty(currentPass) || TextUtils.isEmpty(newPass) || TextUtils.isEmpty(confirmPass)) {
                Toast.makeText(ChangePassword.this, "All fields are required", Toast.LENGTH_SHORT).show();
            }
            if (!newPass.equals(confirmPass)) {
                Toast.makeText(ChangePassword.this, "New Password and Confirm Password doesn't match", Toast.LENGTH_SHORT).show();
            }
            if (user != null && user.getEmail() != null) {
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);
                user.reauthenticate(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user.updatePassword(newPass).addOnCompleteListener(updatetask -> {
                                if (updatetask.isSuccessful()) {
                                    Toast.makeText(ChangePassword.this, "Password updated successfully", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(ChangePassword.this, "Failed to update password", Toast.LENGTH_SHORT).show();
                                }
                        });
                    } else {
                        Toast.makeText(ChangePassword.this, "Current Password is incorrect", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}