package com.example.loginpage;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.hbb20.CountryCodePicker;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import android.util.Base64;

import java.io.InputStream;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class PersonalDetails extends AppCompatActivity {
    private ActivityResultLauncher<String> getContentLauncher;
    EditText edtname, etDob, edtphone;
    CountryCodePicker ccp;
    TextView dobError;
    Button savedetails;
    Spinner genderSpinner;
    CircleImageView setProfilePic;
    Uri selectedImageUri;
    private String encodedImage;

    FirebaseAuth auth;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_personal_details);

        genderSpinner = findViewById(R.id.genderSpinner);
        etDob = findViewById(R.id.etDob);
        edtname = findViewById(R.id.edtname);
        edtphone = findViewById(R.id.edtphone);
        ccp = findViewById(R.id.countryCodePicker);
        savedetails = findViewById(R.id.savedetails);
        setProfilePic = findViewById(R.id.setProfilePic);
        dobError = findViewById(R.id.dobError);

        ccp.registerCarrierNumberEditText(edtphone);

        getContentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) {
                        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    selectedImageUri = uri;
                    try {
                        // Preferred: decode stream to Bitmap (works on all API levels)
                        InputStream is = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        if (bitmap != null) {
                            setProfilePic.setImageBitmap(bitmap);

                            // If you need Base64 as in your old code:
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            byte[] imageBytes = baos.toByteArray();
                            encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
                        } else {
                            Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        Log.e("PersonalDetails", "Failed to load image", e);
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        setProfilePic.setOnClickListener(v -> getContentLauncher.launch("image/*"));


        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users");
        savedetails.setOnClickListener(V -> saveUserDetails());





        etDob.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Format the selected date
                        String dob = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        etDob.setText(dob);

                        if (!isDobValid(dob)){
                            dobError.setVisibility(View.VISIBLE);
                            dobError.setText(getString(R.string.dob_error));
                        } else {
                            dobError.setVisibility(View.GONE);
                        }
                    },
                    year, month, day
            );
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.gender_array,
                android.R.layout.simple_spinner_item
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position != 0) {
                    String gender = parent.getItemAtPosition(position).toString();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });



    }

    private boolean isDobValid(String dob) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);
            sdf.setLenient(false);
            Date date = sdf.parse(dob);

            if (date == null) return false;

            if (date.after(new Date())) {
                return false;
            }

            Calendar minAgeCalender = Calendar.getInstance();
            minAgeCalender.add(Calendar.YEAR, -18);
            return !date.after(minAgeCalender.getTime());
        } catch (ParseException e) {
            return false;
        }
    }

    private void saveUserDetails() {
        String name = edtname.getText().toString().trim();
        String dob = etDob.getText().toString().trim();
        String gender = genderSpinner.getSelectedItem().toString();
        String fullPhoneNo = ccp.getFullNumberWithPlus();

        if (name.isEmpty()) {
            edtname.setError("Name is required");
            edtname.requestFocus();
            return;
        }

        if (!isDobValid(dob)) {
            dobError.setVisibility(View.VISIBLE);
            dobError.setText(getString(R.string.load_dob_error));
            return;
        }

        if (gender.equals("Select Gender")) {
            Toast.makeText(this, "Please select a gender", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPhoneNumber(fullPhoneNo)) {
            edtphone.setError("Enter a valid phone number");
            edtphone.requestFocus();
            return;
        }

        if (encodedImage == null || encodedImage.isEmpty()) {
            Toast.makeText(this, "Please select a profile picture", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            HashMap<String, Object> userUpdate = new HashMap<>();
            userUpdate.put("name", name);
            userUpdate.put("dob", dob);
            userUpdate.put("gender", gender);
            userUpdate.put("phone", fullPhoneNo);
            userUpdate.put("setProfilePic", encodedImage);

            database.child(uid).updateChildren(userUpdate)
                    .addOnSuccessListener(V -> {
                        Toast.makeText(this, "Details saved successfully", Toast.LENGTH_SHORT).show();
                        Intent iMpin = new Intent(PersonalDetails.this, MpinPage.class);
                        startActivity(iMpin);
                        iMpin.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        finish();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save " + e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
        }
    }
    private boolean isValidPhoneNumber(String fullNumber) {
        return fullNumber != null && fullNumber.matches("^\\+[0-9]{6,15}$");
    }
}