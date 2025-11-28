package com.example.loginpage;

import android.content.ClipData;
import android.content.Intent;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private boolean showFavouritesOnly = false;
    private String selectedCategory = "All";


    ImageView mainMenu;

    RecyclerView recyclerView;
    DocumentAdapter adapter;
    List<DocumentModel> docList;

    FloatingActionButton fab;

    Spinner categorySpinner;
    private ActivityResultLauncher<Intent> chooserLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        docList = new ArrayList<>();
        getItemTouchHelper().attachToRecyclerView(recyclerView);

        categorySpinner = findViewById(R.id.categorySpinner);

        String[] categories = {"All", "ID Proof", "Education", "Finance", "Work", "Favourites", "Others"};

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);


        categorySpinner.setAdapter(spinnerAdapter);


        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = parent.getItemAtPosition(position).toString();
                showFavouritesOnly = selectedCategory.equalsIgnoreCase("Favourites");
                filterDocuments(selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Show all documents if nothing is selected
                selectedCategory = "All";
                showFavouritesOnly = false;
                filterDocuments(selectedCategory);
            }
        });

        chooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result == null) {
                        Toast.makeText(MainActivity.this, "No file selected", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (result.getResultCode() != RESULT_OK) {
                        // user cancelled or non-OK result
                        return;
                    }

                    Intent data = result.getData();
                    if (data == null) {
                        Toast.makeText(MainActivity.this, "No file selected", Toast.LENGTH_SHORT).show();
                        return;
                    }


                    if (data.getData() != null) {
                        handlePickedFileUri(data.getData());
                    } else if (data.getClipData() != null) {
                        ClipData clip = data.getClipData();
                        if (clip.getItemCount() > 0) {
                            Uri first = clip.getItemAt(0).getUri();
                            handlePickedFileUri(first);
                        } else {
                            Toast.makeText(MainActivity.this, "No file selected", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "No file selected", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        adapter = new DocumentAdapter(this, docList, new DocumentActionListener() {
            @Override
            public void onFavouriteToggled(DocumentModel doc, int position) {
                filterDocuments(selectedCategory);
            }

            @Override
            public void onRename(DocumentModel doc, int position) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
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
                            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                            FirebaseDatabase.getInstance().getReference("documents")
                                    .child(uid)
                                    .child(doc.getId())
                                    .child("docName").setValue(newName);

                            docList.set(position, doc);
                            adapter.notifyItemChanged(position);
                            dialog.dismiss();
                        } else {
                            Toast.makeText(MainActivity.this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
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
                        if (!downloadDir.mkdirs()) {
                            Toast.makeText(MainActivity.this, "Failed to create directory", Toast.LENGTH_SHORT).show();
                        }
                    }

                    File file = new File(downloadDir, doc.getDocName() + extension);
                    FileOutputStream fos = new FileOutputStream(file);
                    fos.write(fileBytes);
                    fos.close();

                    Toast.makeText(MainActivity.this, "Downloaded to: " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e("Main Activity", "Download failed", e);
                    Toast.makeText(MainActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onDelete(DocumentModel doc, int position) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Delete");
                builder.setMessage("Are you sure you want to delete this doc?");
                builder.setIcon(R.drawable.ic_action_name);
                builder.setPositiveButton("Delete", (dialog, which) -> {
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
                        Toast.makeText(MainActivity.this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
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
                            MainActivity.this,
                            getPackageName() + ".provider",
                            file
                    );

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType(doc.getMimeType());
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivity(Intent.createChooser(intent, "Share document via"));

                } catch (Exception e) {
                    Log.e("Main Activity", "Failed to share file", e);
                    Toast.makeText(MainActivity.this, "Failed to share file", Toast.LENGTH_SHORT).show();
                }
            }
        });

        recyclerView.setAdapter(adapter);


        fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> openGalleryChooser());

        loadDocuments();
        mainMenu = findViewById(R.id.main_options_menu);


        mainMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.main_option_menu, popupMenu.getMenu());

            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.search) {
                    startActivity(new Intent(MainActivity.this, Search.class));
                    return true;
                } else if (item.getItemId() == R.id.profile) {
                    startActivity(new Intent(MainActivity.this, Profile.class));
                    return true;
                } else {
                    startActivity(new Intent(MainActivity.this, Settings.class));
                    return true;
                }

            });
            popupMenu.show();
        });


    }

    private void openGalleryChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"application/pdf", "image/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        Intent chooser = Intent.createChooser(intent, "Select File");
        chooserLauncher.launch(chooser);
    }

    private void handlePickedFileUri(Uri fileUri) {
        View dialogView = getLayoutInflater().inflate(R.layout.save_doc, null);

        EditText inputDocName = dialogView.findViewById(R.id.saveDocName);
        Spinner categorySpinner = dialogView.findViewById(R.id.categorySpinner);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnSave.setOnClickListener(v -> {
            String docName = inputDocName.getText().toString().trim();
            String selectedCategory = categorySpinner.getSelectedItem().toString();

            if (!docName.isEmpty()) {
                try {
                    uploadFile(fileUri, docName, selectedCategory, false);
                    dialog.dismiss();
                } catch (IOException e) {
                    Log.e("Main Activity", "Upload failed", e);
                    Toast.makeText(MainActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(MainActivity.this, "Document name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void uploadFile(Uri fileUri, String docName, String category, boolean isFavourite) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(fileUri);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            if (inputStream == null) {
                Toast.makeText(this, "Unable to open file", Toast.LENGTH_SHORT).show();
                return;
            }


            byte[] fileBytes = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(fileBytes)) != -1) {
                byteArrayOutputStream.write(fileBytes, 0, bytesRead);
            }

            String encodedFile = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);

            String mimeType = getContentResolver().getType(fileUri);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("documents").child(uid);

                String docId = dbRef.push().getKey();


                HashMap<String, Object> fileMap = new HashMap<>();
                fileMap.put("id", docId);
                fileMap.put("docName", docName);
                fileMap.put("fileData", encodedFile);
                fileMap.put("mimeType", mimeType);
                fileMap.put("category", category.trim());
                fileMap.put("favourite", isFavourite);

                if (docId != null && !docId.isEmpty()) {
                    dbRef.child(docId).setValue(fileMap)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Document saved successfully!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Upload failed: " + task.getException(), Toast.LENGTH_SHORT).show();
                                }
                            });
                }else {
                    Toast.makeText(this, "Document ID is missing!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("Main Activity", "The selected document cannot be saved", e);
        }


    }

    private void loadDocuments() {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("documents").child(uid);

            dbRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    docList.clear();
                    for (DataSnapshot docSnap : snapshot.getChildren()) {
                        DocumentModel doc = docSnap.getValue(DocumentModel.class);
                        if (doc != null) {
                            doc.setId(docSnap.getKey());  // ✅ force set id from Firebase key
                            docList.add(doc);
                        }
                    }
                    filterDocuments(selectedCategory);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                        Toast.makeText(MainActivity.this, "Failed to load docs: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            Toast.makeText(this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void filterDocuments(String category) {
        List<DocumentModel> filteredList = new ArrayList<>();

        filteredList.clear();

        String selected = category != null ? category.trim().toLowerCase() : "all";

        for (DocumentModel doc : docList) {
            if (doc == null) continue;

            String docCategory = doc.getCategory() != null ? doc.getCategory().trim().toLowerCase() : "";

            boolean isFav = doc.isFavourite();
            boolean matches = false;
            if (showFavouritesOnly || selected.equalsIgnoreCase("favourites")) {
                matches = isFav;
            } else if (selected.equals("all")) {
                matches = true;
            } else {
                matches = docCategory.equals(selected);
            }

            if (matches) {
                filteredList.add(doc);
            }
        }

        adapter.updateList(filteredList);

    }

    private ItemTouchHelper getItemTouchHelper() {
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {


            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) {
                    return;
                }
                DocumentModel docToDelete = adapter.getDocumentAt(pos);

                if (docToDelete == null) {
                    adapter.notifyItemChanged(pos);
                    return;
                }

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Delete Document")
                        .setMessage("Are you sure you want to delete \"" + docToDelete.getDocName() + "\"?")
                        .setIcon(R.drawable.ic_action_name)
                        .setPositiveButton("Delete", (dialog, which) -> {
                            DocumentModel removedDoc = adapter.removeItem(pos);

                            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                            if (currentUser != null) {
                                String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                                if (removedDoc != null && removedDoc.getId() != null) {
                                    DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("documents")
                                            .child(uid)
                                            .child(removedDoc.getId());

                                    dbRef.removeValue().addOnCompleteListener(task -> {
                                        if (!task.isSuccessful()) {
                                            Toast.makeText(MainActivity.this, "Failed to delete from server", Toast.LENGTH_SHORT).show();
                                            adapter.addItem(pos, removedDoc);
                                        }
                                    });
                                }
                                View parentLayout = findViewById(android.R.id.content);
                                Snackbar snackbar = Snackbar.make(parentLayout, "Document deleted!", Snackbar.LENGTH_LONG);
                                snackbar.setAction("UNDO", v -> {

                                    adapter.addItem(pos, removedDoc);

                                    if (removedDoc != null) {
                                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("documents")
                                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                                .child(removedDoc.getId());

                                        HashMap<String, Object> map = new HashMap<>();
                                        map.put("id", removedDoc.getId());
                                        map.put("docName", removedDoc.getDocName());
                                        map.put("fileData", removedDoc.getFileData());
                                        map.put("mimeType", removedDoc.getMimeType());
                                        map.put("category", removedDoc.getCategory());
                                        map.put("favourite", removedDoc.isFavourite());

                                        ref.setValue(map).addOnCompleteListener(t -> {
                                            if (!t.isSuccessful()) {
                                                Toast.makeText(MainActivity.this, "Failed to restore on server", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    }
                                });

                                snackbar.show();
                            } else {
                                Toast.makeText(MainActivity.this, "No user logged in. Please login again.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancel", (dialog, which) -> adapter.notifyItemChanged(pos))
                        .setOnCancelListener(dialog -> adapter.notifyItemChanged(pos))
                        .show();
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        };
        return new ItemTouchHelper(simpleCallback);
    }
}
