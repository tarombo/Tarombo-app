package com.familygem.restapi.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Invitation {
    @SerializedName("id")
    @Expose
    public Integer id;

    @SerializedName("node_id")
    @Expose
    public String nodeId;

    @SerializedName("repository")
    @Expose
    public Repo repository;

    @SerializedName("invitee")
    @Expose
    public User invitee;

    @SerializedName("inviter")
    @Expose
    public User inviter;

    @SerializedName("expired")
    @Expose
    public Boolean expired;

    @SerializedName("created_at")
    @Expose
    public String createdAt;
}
