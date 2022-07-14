package com.familygem.restapi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TreeItem {
    @SerializedName("sha")
    @Expose
    public String sha;
    @SerializedName("url")
    @Expose
    public String url;

    @SerializedName("path")
    @Expose
    public String path;

    @SerializedName("mode")
    @Expose
    public String mode;

    @SerializedName("type")
    @Expose
    public String type;
}
