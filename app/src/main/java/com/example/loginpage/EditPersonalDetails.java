package com.example.loginpage;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
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
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditPersonalDetails extends AppCompatActivity {
    private ActivityResultLauncher<String> getContentLauncher;

    EditText loadName, loadDob, loadPhone;

    TextView loadDobError;
    CircleImageView loadPic;
    Spinner loadGender;
    Button loadUpdatedDetails;
    FirebaseAuth auth;
    DatabaseReference database;
    CountryCodePicker ccp;
    Uri selectedImageUri;
    String encodedImage = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_personal_details);

        loadName = findViewById(R.id.loadName);
        loadDob = findViewById(R.id.loadDob);
        loadPhone = findViewById(R.id.loadPhone);
        loadPic = findViewById(R.id.loadPic);
        loadGender = findViewById(R.id.loadGender);
        loadDobError = findViewById(R.id.loadDobError);
        loadUpdatedDetails = findViewById(R.id.saveLoadedDetails);
        ccp = findViewById(R.id.ccp);
        ccp.registerCarrierNumberEditText(loadPhone);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users");

        loadExistingDetails();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.gender_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        loadGender.setAdapter(adapter);

        loadDob.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    EditPersonalDetails.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        String dob = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                        loadDob.setText(dob);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        getContentLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri == null) {
                        Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    selectedImageUri = uri;
                    try {
                        InputStream is = getContentResolver().openInputStream(uri);
                        Bitmap bitmap = BitmapFactory.decodeStream(is);
                        if (bitmap != null) {
                            loadPic.setImageBitmap(bitmap);

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


        loadPic.setOnClickListener(v -> getContentLauncher.launch("image/*"));


        loadUpdatedDetails.setOnClickListener(v -> {
            String name = loadName.getText().toString().trim();
            String dob = loadDob.getText().toString().trim();
            String gender = loadGender.getSelectedItem().toString();
            String fullPhoneNo = ccp.getFullNumberWithPlus();

            if (name.isEmpty()) {
                loadName.setError("Name is required");
                loadName.requestFocus();
                return;
            }

            if (!isDobValid(dob)) {
                loadDobError.setVisibility(View.VISIBLE);
                loadDobError.setText(getString(R.string.load_dob_error));
                return;
            }

            if (gender.equals("Select Gender")) {
                Toast.makeText(EditPersonalDetails.this, "Please select a gender", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isValidPhoneNumber(fullPhoneNo)) {
                loadPhone.setError("Enter a valid phone number");
                loadPhone.requestFocus();
                return;
            }

            if (encodedImage == null || encodedImage.isEmpty()) {
                Toast.makeText(EditPersonalDetails.this, "Please select a profile picture", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(EditPersonalDetails.this, "Details updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> Toast.makeText(EditPersonalDetails.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });


    }

    private void loadExistingDetails() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            database.child(uid).get().addOnSuccessListener(dataSnapshot -> {
                if (dataSnapshot.exists()) {
                    loadName.setText(dataSnapshot.child("name").getValue(String.class));
                    loadDob.setText(dataSnapshot.child("dob").getValue(String.class));

                    String savedPhone = dataSnapshot.child("phone").getValue(String.class);
                    if (savedPhone != null && savedPhone.startsWith("+")) {
                        String countryCode = savedPhone.substring(1, Math.min(4, savedPhone.length())); // get up to 3 digits country code
                        ccp.setFullNumber(savedPhone.substring(1)); // This sets the entire number in ccp (country + number)

                        String numberOnly = ccp.getFullNumber().replace(ccp.getSelectedCountryCode(), "");
                        loadPhone.setText(numberOnly);
                    }


                    String savedGender = dataSnapshot.child("gender").getValue(String.class);
                    if (savedGender != null) {
                        SpinnerAdapter rawAdapter = loadGender.getAdapter();
                        if (rawAdapter instanceof ArrayAdapter) {
                            @SuppressWarnings("unchecked")
                            ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) rawAdapter;
                            int spinnerPosition = adapter.getPosition(savedGender);
                            if (spinnerPosition >= 0)
                                loadGender.setSelection(spinnerPosition);
                        }
                    }

                    String savedImage = dataSnapshot.child("setProfilePic").getValue(String.class);
                    if (savedImage != null && !savedImage.isEmpty()) {
                        encodedImage = savedImage;
                        byte[] decodedString = android.util.Base64.decode(savedImage, android.util.Base64.DEFAULT);
                        Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                        loadPic.setImageBitmap(bitmap);
                    }
                } else {
                    Toast.makeText(EditPersonalDetails.this, "Failed to load profile data", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidPhoneNumber(String fullNumber) {
        return fullNumber != null && fullNumber.matches("^\\+[0-9]{6,15}$");
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


}