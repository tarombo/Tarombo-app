package com.familygem.restapi.requestmodels;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CommitTreeRequest {
    @SerializedName("message")
    public String message;

    @SerializedName("tree")
    public String tree;

    @SerializedName("parents")
    public List<String> parents;

    @SerializedName("author")
    public CommitterRequestModel author;
}
