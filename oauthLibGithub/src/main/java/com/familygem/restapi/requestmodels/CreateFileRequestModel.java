package com.familygem.restapi.requestmodels;

import com.google.gson.annotations.SerializedName;

public class CreateFileRequestModel {
    @SerializedName("message")
    public String message;

    @SerializedName("content")
    public String content;

    @SerializedName("committer")
    public CommitterRequestModel committer;

    public CreateFileRequestModel(String message, String content, CommitterRequestModel committer) {
        this.message = message;
        this.content = content;
        this.committer = committer;
    }
}
