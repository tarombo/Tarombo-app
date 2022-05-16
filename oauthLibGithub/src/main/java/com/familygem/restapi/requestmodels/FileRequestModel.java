package com.familygem.restapi.requestmodels;

import com.google.gson.annotations.SerializedName;

public class FileRequestModel {
    @SerializedName("message")
    public String message;

    @SerializedName("content")
    public String content;

    @SerializedName("committer")
    public CommitterRequestModel committer;

    @SerializedName("sha") //only needed for update file
    public String sha;

    public FileRequestModel(String message, String content, CommitterRequestModel committer) {
        this.message = message;
        this.content = content;
        this.committer = committer;
    }
}
