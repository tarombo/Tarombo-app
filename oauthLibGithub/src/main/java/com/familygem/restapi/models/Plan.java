package com.familygem.restapi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Plan {
    @SerializedName("name")
    @Expose
    public String name;
    @SerializedName("space")
    @Expose
    public Integer space;
    @SerializedName("collaborators")
    @Expose
    public Integer collaborators;
    @SerializedName("private_repos")
    @Expose
    public Integer privateRepos;

}
