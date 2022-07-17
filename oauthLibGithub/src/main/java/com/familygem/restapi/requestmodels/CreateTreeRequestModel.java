package com.familygem.restapi.requestmodels;

import com.familygem.restapi.models.Tree;
import com.familygem.restapi.models.TreeItem;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CreateTreeRequestModel {
    @SerializedName("base_tree")
    @Expose
    public String baseTree;

    @SerializedName("tree")
    @Expose
    public List<TreeItem> tree;
}
