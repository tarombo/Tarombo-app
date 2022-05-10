package com.familygem.restapi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class License {

    @SerializedName("key")
    @Expose
    public String key;
    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("spdx_id")
    @Expose
    public String spdxId;
    @SerializedName("url")
    @Expose
    public String url;
    @SerializedName("node_id")
    @Expose
    public String nodeId;
}
