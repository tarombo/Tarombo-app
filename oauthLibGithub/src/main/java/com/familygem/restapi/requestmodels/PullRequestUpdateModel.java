package com.familygem.restapi.requestmodels;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class PullRequestUpdateModel {

    @SerializedName("title")
    @Expose
    public String title;
    @SerializedName("body")
    @Expose
    public String body;
    @SerializedName("state")
    @Expose
    public String state;
    @SerializedName("base")
    @Expose
    public String base;


    public PullRequestUpdateModel(String title, String body, String state, String base) {
        this.title = title;
        this.body = body;
        this.state = state;
        this.base = base;
    }
}