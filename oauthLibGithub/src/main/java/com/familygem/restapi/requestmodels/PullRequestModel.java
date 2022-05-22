package com.familygem.restapi.requestmodels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PullRequestModel {

    @SerializedName("title")
    @Expose
    public String title;
    @SerializedName("body")
    @Expose
    public String body;
    @SerializedName("head")
    @Expose
    public String head;
    @SerializedName("base")
    @Expose
    public String base;
    @SerializedName("draft")
    @Expose
    public Boolean draft = false;

    public PullRequestModel(String title, String body, String head, String base) {
        this.title = title;
        this.body = body;
        this.head = head;
        this.base = base;
    }
}