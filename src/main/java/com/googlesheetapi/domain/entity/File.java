package com.googlesheetapi.domain.entity;

import com.google.gson.annotations.SerializedName;

public class File {
    @SerializedName("mimeType")
    private String mimeType;

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    public String getMimeType() {
        return mimeType;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
