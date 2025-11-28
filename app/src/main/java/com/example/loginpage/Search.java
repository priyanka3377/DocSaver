package com.example.loginpage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Search extends AppCompatActivity {
    EditText searchBar;
    RecyclerView recyclerView;
    DocumentAdapter adapter;
    List<DocumentModel> docList;
    List<DocumentModel> filteredList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);

        searchBar = findViewById(R.id.searchBar);
        recyclerView = findViewById(R.id.searchrecyclerView);

        docList = new ArrayList<>();
        filteredList = new ArrayList<>();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DocumentAdapter(this, filteredList, new DocumentActionListener() {
            @Override
            public void onRename(DocumentModel doc, int position) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Search.this);
                View dialogView = getLayoutInflater().inflate(R.layout.rename, null);
                EditText input = dialogView.findViewById(R.id.inputDocName);
                Button btnSave = dialogView.findViewById(R.id.btnSave);
                Button btnCancel = dialogView.findViewById(R.id.btnCancel);
                input.setText(doc.getDocName());
                builder.setView(dialogView);

                AlertDialog dialog = builder.create();

                btnSave.setOnClickListener(v -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            doc.setDocName(newName);

                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (currentUser != null) {
                                String uid = currentUser.getUid();
                                FirebaseDatabase.getInstance().getReference("documents")
                                        .child(uid)
                                        .child(doc.getId())
                                        .child("docName").setValue(newName);

                                docList.set(position, doc);
                                adapter.notifyItemChanged(position);
                                dialog.dismiss();
                            }else {
                                Toast.makeText(Search.this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(Search.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        }
                });

                btnCancel.setOnClickListener(v -> dialog.dismiss());

                dialog.show();

                if (dialog.getWindow() != null) {
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                }
            }

            @Override
            public void onDownload(DocumentModel doc, int position) {
                try {
                    byte[] fileBytes = Base64.decode(doc.getFileData(), Base64.DEFAULT);

                    String extension = doc.getMimeType().contains("pdf") ? ".pdf" : ".jpg";

                    File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "");

                    if (!downloadDir.exists()) {
                        if (!downloadDir.mkdirs()){
                            Toast.makeText(Search.this, "Failed to create directory", Toast.LENGTH_SHORT).show();
                        }
                    }

                    File file = new File(downloadDir, doc.getDocName() + extension);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(fileBytes);
                    fos.close();

                    Toast.makeText(Search.this, "Downloaded to: " +file.getAbsolutePath(), Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e("Search", "Error while fetching the documents",e);
                    Toast.makeText(Search.this, "Download failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDelete(DocumentModel doc, int position) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Search.this);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure you want to delete this doc?");
                builder.setIcon(R.drawable.ic_action_name);
                builder.setPositiveButton("Delete", (dialog, which) ->  {
                    FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (currentUser != null) {
                        String uid = currentUser.getUid();
                        FirebaseDatabase.getInstance().getReference("documents")
                                .child(uid)
                                .child(doc.getId())
                                .removeValue();

                        docList.remove(position);
                        adapter.notifyItemRemoved(position);
                    } else {
                        Toast.makeText(Search.this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
                    }
                });

                builder.setNegativeButton("Cancel", null);
                builder.show();
            }

            @Override
            public void onShare(DocumentModel doc, int position) {
                try {
                    byte[] fileBytes = Base64.decode(doc.getFileData(), Base64.DEFAULT);

                    String extension = doc.getMimeType().contains("pdf") ? ".pdf" : ".jpg";

                    File file = new File(getCacheDir(), doc.getDocName() + extension);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(fileBytes);
                    fos.close();

                    Uri uri = FileProvider.getUriForFile(
                            Search.this,
                            getPackageName() + ".provider",
                            file
                    );

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType(doc.getMimeType());
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivity(Intent.createChooser(intent, "Share document via"));

                } catch (Exception e) {
                    Log.e("Search", "Failed to share file", e);
                    Toast.makeText(Search.this, "Failed to share file", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFavouriteToggled(DocumentModel doc, int position) {
                filteredList.set(position, doc);

                for (int i = 0; i < docList.size(); i++) {
                    if (docList.get(i).getId().equals(doc.getId())) {
                        docList.set(i, doc);
                        break;
                    }
                }

                adapter.notifyItemChanged(position);
            }
        });

        recyclerView.setAdapter(adapter);

        loadDocuments();

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void filter(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(docList);
        } else {
            for (DocumentModel doc : docList) {
                if (doc.getDocName() != null && doc.getDocName().toLowerCase().contains(text.toLowerCase())) {
                    filteredList.add(doc);
                }
            }
        }
        adapter.updateList(filteredList);
    }

    private void loadDocuments() {
        FirebaseUser currentUser =  FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("documents").child(uid);
            dbRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    docList.clear();
                    for (DataSnapshot docSnap : snapshot.getChildren()) {
                        DocumentModel doc = docSnap.getValue(DocumentModel.class);
                        if (doc != null) {
                            doc.setId(docSnap.getKey());
                            docList.add(doc);
                        }
                    }

                    filteredList.clear();
                    filteredList.addAll(docList);
                    adapter.updateList(filteredList);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } else {
            Toast.makeText(this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
        }
    }
}