package com.example.loginpage;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class DeleteAccount extends AppCompatActivity {

    TextView deleteaccount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_delete_account);

        deleteaccount = findViewById(R.id.deleteaccount);

        deleteaccount.setOnClickListener(V -> confirmDeleteAccount());

    }

    private void confirmDeleteAccount() {
        AlertDialog.Builder deleteAcc = new AlertDialog.Builder(DeleteAccount.this);
        deleteAcc.setIcon(R.drawable.ic_deleteacc);
        deleteAcc.setTitle("Delete Account");
        deleteAcc.setMessage("Are you sure you want to delete this account?");
        deleteAcc.setPositiveButton("Yes", (dialog, which) -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(DeleteAccount.this);
            View dialogView = getLayoutInflater().inflate(R.layout.delete_acc, null);
            EditText input = dialogView.findViewById(R.id.confirmPass);
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            Button btnConfirm = dialogView.findViewById(R.id.btnConfirm);

            builder.setView(dialogView);

            AlertDialog confirmDelete = builder.create();
            btnCancel.setOnClickListener(t -> confirmDelete.dismiss());

            btnConfirm.setOnClickListener(t -> {
                String password = input.getText().toString().trim();
                if (!password.isEmpty()) {
                    reauthenticateAndDelete(password);
                } else {
                    Toast.makeText(this, "Password required", Toast.LENGTH_SHORT).show();
                }
            });

            confirmDelete.setCancelable(false);
            confirmDelete.show();
        });
        deleteAcc.setNegativeButton("No", null);
        deleteAcc.show();
    }

    private void reauthenticateAndDelete(String password) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);
            user.reauthenticate(credential)
                    .addOnSuccessListener(aVoid -> {
                        String uid = user.getUid();

                        FirebaseDatabase.getInstance().getReference("documents").child(uid)
                                .removeValue()
                                .addOnCompleteListener(taskDocs -> {
                                    if (taskDocs.isSuccessful()) {
                                        FirebaseDatabase.getInstance().getReference("users")
                                                .child(user.getUid())
                                                .removeValue()
                                                .addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        user.delete()
                                                                .addOnSuccessListener(v -> {
                                                                    Toast.makeText(this, "Account deleted", Toast.LENGTH_SHORT).show();
                                                                    FirebaseAuth.getInstance().signOut();
                                                                    new Handler().postDelayed(() -> {
                                                                        Intent iLoginAgain = new Intent(DeleteAccount.this, LoginPage.class);
                                                                        iLoginAgain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                                        startActivity(iLoginAgain);
                                                                        finish();
                                                                    }, 1000);
                                                                })
                                                                .addOnFailureListener( e -> Toast.makeText(this, "Error deleting account: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                                    } else {
                                                        Toast.makeText(this, "Error removing user data", Toast.LENGTH_SHORT).show();
                                                    }
                                                });

                                    } else {
                                        Toast.makeText(this, "Failed to delete documents: "+taskDocs.getException(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Re-authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}
