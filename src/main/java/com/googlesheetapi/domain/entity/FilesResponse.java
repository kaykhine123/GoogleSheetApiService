package com.googlesheetapi.domain.entity;

import java.util.List;

import com.google.gson.annotations.SerializedName;

public class FilesResponse {
    @SerializedName("files")
    private List<File> files;

    public List<File> getFiles() {
        return files;
    }
}