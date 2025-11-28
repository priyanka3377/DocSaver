package com.example.loginpage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class DocumentAdapter extends RecyclerView.Adapter<DocumentAdapter.MyViewHolder> {
    private final Context context;
    private final List<DocumentModel> documentModelList;
    private final DocumentActionListener actionListener;


    public DocumentAdapter(Context context, List<DocumentModel> documentModelList, DocumentActionListener listener) {
        this.context = context;
        this.documentModelList = new ArrayList<>();
        if (documentModelList != null) this.documentModelList.addAll(documentModelList);
        this.actionListener = listener;
    }

    public void updateList(List<DocumentModel> newList) {
        this.documentModelList.clear();
        if (newList != null  && !newList.isEmpty()) {
            this.documentModelList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return documentModelList != null ? documentModelList.size() : 0;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.document, parent, false);
        return new MyViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        DocumentModel doc = documentModelList.get(position);

        holder.heartIcon.setImageResource(doc.isFavourite() ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        holder.heartIcon.setOnClickListener(v -> {
            boolean newStatus = !doc.isFavourite();
            doc.setFavourite(newStatus);
            holder.heartIcon.setImageResource(newStatus ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("documents")
                        .child(currentUser.getUid())
                        .child(doc.getId());
                dbRef.child("favourite").setValue(newStatus);
            }

            if (actionListener != null) {
                actionListener.onFavouriteToggled(doc, holder.getBindingAdapterPosition());
            }

            holder.heartIcon.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        holder.heartIcon.setImageResource(newStatus ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                        holder.heartIcon.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
                    }).start();
        });


        holder.tvDocName.setText(doc.getDocName());

        holder.menuButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), holder.menuButton);
            popupMenu.inflate(R.menu.card_menu);
            popupMenu.setOnMenuItemClickListener(item ->  {
                int id = item.getItemId();
                if (id == R.id.rename) {
                    actionListener.onRename(doc, holder.getBindingAdapterPosition());
                    return true;
                } else if (id == R.id.download) {
                    actionListener.onDownload(doc, holder.getBindingAdapterPosition());
                    return true;
                } else if (id == R.id.delete) {
                    actionListener.onDelete(doc, holder.getBindingAdapterPosition());
                    return true;
                } else if (id == R.id.share) {
                    actionListener.onShare(doc, holder.getBindingAdapterPosition());
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });

        holder.itemView.setOnClickListener(v ->  {
            try {
                byte[] fileBytes = Base64.decode(doc.getFileData(), Base64.DEFAULT);

                String extension = doc.getMimeType().contains("pdf") ? ".pdf" : ".jpg";
                File file = new File(v.getContext().getCacheDir(), "temp" + extension);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(fileBytes);
                fos.close();
                Uri uri = FileProvider.getUriForFile(
                        v.getContext(),
                        v.getContext().getPackageName() + ".provider",
                        file
                );
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, doc.getMimeType());
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                v.getContext().startActivity(intent);

                Log.d("DOC_URI", "Generated URI: " + uri.toString());

            } catch (Exception e) {
                Log.e("Document Adapter", "Failed to open file", e);
                Toast.makeText(context, "Failed to open file", Toast.LENGTH_SHORT).show();
            }
        });

        // Show icon based on file type
        if (doc.getMimeType().contains("pdf")) {
            holder.imgIcon.setImageResource(R.drawable.ic_pdf);
        } else if (doc.getMimeType().contains("image")) {
            holder.imgIcon.setImageResource(R.drawable.ic_image);
        } else {
            holder.imgIcon.setImageResource(R.drawable.ic_document); //default icon
        }
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView tvDocName;
        ImageView imgIcon, menuButton, heartIcon;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDocName = itemView.findViewById(R.id.tvDocName);
            imgIcon = itemView.findViewById(R.id.imgIcon);
            menuButton = itemView.findViewById(R.id.menuButton);
            heartIcon = itemView.findViewById(R.id.heartIcon);
        }
    }
    public DocumentModel removeItem(int position) {
        if (position < 0 || position >= documentModelList.size()) {
            return null;
        }
        DocumentModel removed = documentModelList.remove(position);
        notifyItemRemoved(position);
        return removed;
    }

    public void addItem(int position, DocumentModel item) {
        if (item == null) {
            return;
        }
        if (position < 0 || position > documentModelList.size()) {
            documentModelList.add(item);
            notifyItemInserted(documentModelList.size() - 1);
        } else {
            documentModelList.add(position, item);
            notifyItemInserted(position);
        }
    }

    public DocumentModel getDocumentAt(int position) {
        if (position < 0 || position >= documentModelList.size()) return null;
        return documentModelList.get(position);
    }


}
