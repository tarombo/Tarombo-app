
package com.familygem.restapi.requestmodels;


import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;


public class CommitterRequestModel {
    @SerializedName("name")
    @Expose
    public String name;

    @SerializedName("email")
    @Expose
    public String email;

    public CommitterRequestModel(String name, String email) {
        this.name = name;
        this.email = email;
    }
}
