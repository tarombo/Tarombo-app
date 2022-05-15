
package com.familygem.restapi.models;

import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Commit {

    @SerializedName("sha")
    @Expose
    public String sha;
    @SerializedName("node_id")
    @Expose
    public String nodeId;
    @SerializedName("url")
    @Expose
    public String url;
    @SerializedName("html_url")
    @Expose
    public String htmlUrl;
    @SerializedName("author")
    @Expose
    public Author author;
    @SerializedName("committer")
    @Expose
    public Committer committer;
    @SerializedName("tree")
    @Expose
    public Tree tree;
    @SerializedName("message")
    @Expose
    public String message;
    @SerializedName("parents")
    @Expose
    public List<Parent> parents = null;
    @SerializedName("verification")
    @Expose
    public Verification verification;

}
