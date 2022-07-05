package com.familygem.restapi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class SearchUsersResult {
    @SerializedName("total_count")
    @Expose
    public Integer totalCount;

    @SerializedName("items")
    @Expose
    public List<User> items = null;
}
