package com.familygem.restapi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PRFile {

    @SerializedName("sha")
    @Expose
    public String sha;
    @SerializedName("filename")
    @Expose
    public String filename;
    @SerializedName("status")
    @Expose
    public String status;
    @SerializedName("additions")
    @Expose
    public Integer additions;
    @SerializedName("deletions")
    @Expose
    public Integer deletions;
    @SerializedName("changes")
    @Expose
    public Integer changes;
    @SerializedName("blob_url")
    @Expose
    public String blobUrl;
    @SerializedName("raw_url")
    @Expose
    public String rawUrl;
    @SerializedName("contents_url")
    @Expose
    public String contentsUrl;
    @SerializedName("patch")
    @Expose
    public String patch;

}