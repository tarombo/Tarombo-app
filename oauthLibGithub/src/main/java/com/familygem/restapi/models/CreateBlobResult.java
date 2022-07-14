package com.familygem.restapi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class CreateBlobResult {
    @SerializedName("sha")
    @Expose
    public String sha;
    @SerializedName("url")
    @Expose
    public String url;
}
