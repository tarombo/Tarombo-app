package com.familygem.restapi.requestmodels;

import com.familygem.action.CreateRepoTask;
import com.google.gson.annotations.SerializedName;

public class CreateRepoRequestModel {
    @SerializedName("name")
    public String name;
    @SerializedName("description")
    public String description;
    @SerializedName("private")
    public Boolean isPrivate = false;
    @SerializedName("auto_init")
    public Boolean autoInit = true;

    public CreateRepoRequestModel(String name, String description) {
        this.name = name;
        this.description = description;
    }
}