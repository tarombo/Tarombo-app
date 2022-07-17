package com.familygem.restapi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class RefResult {
    @SerializedName("ref")
    @Expose
    public String ref;

    @SerializedName("node_id")
    @Expose
    public String nodeId;

    @SerializedName("url")
    @Expose
    public String url;

    @SerializedName("object")
    @Expose
    public ObjRef _object;
}
