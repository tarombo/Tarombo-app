
package com.familygem.restapi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Content {

    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("path")
    @Expose
    public String path;
    @SerializedName("sha")
    @Expose
    public String sha;
    @SerializedName("size")
    @Expose
    public Integer size;
    @SerializedName("url")
    @Expose
    public String url;
    @SerializedName("html_url")
    @Expose
    public String htmlUrl;
    @SerializedName("git_url")
    @Expose
    public String gitUrl;
    @SerializedName("download_url")
    @Expose
    public String downloadUrl;
    @SerializedName("type")
    @Expose
    public String type;
    @SerializedName("_links")
    @Expose
    public Links links;

    @SerializedName("content")
    @Expose
    public String content;

    public transient String contentStr;
}
