package com.example.loginpage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class Help extends AppCompatActivity {
    TextView btnContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_help);

        btnContact = findViewById(R.id.btnContactSupport);
        btnContact.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"priyankataduri33@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Help needed with DocSaver app");
            startActivity(Intent.createChooser(intent, "Contact Support"));
        });
    }
}