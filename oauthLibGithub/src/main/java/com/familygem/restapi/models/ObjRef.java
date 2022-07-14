package com.familygem.restapi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ObjRef {
    @SerializedName("type")
    @Expose
    public String type;

    @SerializedName("sha")
    @Expose
    public String sha;

    @SerializedName("url")
    @Expose
    public String url;
}
