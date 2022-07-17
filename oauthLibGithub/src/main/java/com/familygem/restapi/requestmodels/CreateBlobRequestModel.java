package com.familygem.restapi.requestmodels;

import com.google.gson.annotations.SerializedName;

public class CreateBlobRequestModel {
    @SerializedName("content")
    public String content;
    @SerializedName("encoding")
    public String encoding;
}
