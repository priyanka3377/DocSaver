package com.example.loginpage;


public interface DocumentActionListener {

    void onRename(DocumentModel doc, int position);
    void onDownload(DocumentModel doc, int position);
    void onDelete(DocumentModel doc, int position);
    void onShare(DocumentModel doc, int position);
    void onFavouriteToggled(DocumentModel doc, int position);
}
