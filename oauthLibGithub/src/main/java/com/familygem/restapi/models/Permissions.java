
package com.familygem.restapi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Permissions {

    @SerializedName("admin")
    @Expose
    public Boolean admin;
    @SerializedName("maintain")
    @Expose
    public Boolean maintain;
    @SerializedName("push")
    @Expose
    public Boolean push;
    @SerializedName("triage")
    @Expose
    public Boolean triage;
    @SerializedName("pull")
    @Expose
    public Boolean pull;

}
