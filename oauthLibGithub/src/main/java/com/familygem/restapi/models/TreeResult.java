package com.familygem.restapi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TreeResult {
    @SerializedName("sha")
    @Expose
    public String sha;
    @SerializedName("url")
    @Expose
    public String url;

    @SerializedName("tree")
    @Expose
    public List<TreeItem> tree;

    @SerializedName("truncated")
    @Expose
    public Boolean truncated;
}
