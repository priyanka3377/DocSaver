package com.example.loginpage;

public class DocumentModel {
    private String id;
    private String docName;
    private String mimeType;
    private String fileData;
    private String category;
    private boolean favourite;

    public DocumentModel() {}

    public DocumentModel(String id, String docName, String mimeType, String fileData, String category, boolean isFavourite) {
        this.id = id;
        this.docName = docName;
        this.mimeType = mimeType;
        this.fileData = fileData;
        this.category = category;
        this.favourite = isFavourite;
    }

    public boolean isFavourite() {
        return favourite;
    }

    public void setFavourite(boolean favourite) {
        this.favourite = favourite;
    }

    public String getId() {
        return id;
    }

    public String getDocName() {
        return docName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileData() {
        return fileData;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setDocName(String docName) {
        this.docName = docName;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setFileData(String fileData) {
        this.fileData = fileData;
    }

    public String getCategory() { return category; }

    public void setCategory(String category) { this.category = category; }


}
