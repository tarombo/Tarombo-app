package com.familygem.utility;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.joda.time.DateTime;

public class FamilyGemTreeInfoModel {
    @SerializedName("title")
    @Expose
    public String title;

    @SerializedName("persons")
    @Expose
    public int persons;

    @SerializedName("generations")
    @Expose
    public int generations;

    @SerializedName("media")
    @Expose
    public int media;

    @SerializedName("root")
    @Expose
    public String root;

    @SerializedName("grade")
    @Expose
    public int grade;

    public FamilyGemTreeInfoModel(String title, int persons, int generations, int media, String root, int grade, String createdAt, String updatedAt) {
        this.title = title;
        this.persons = persons;
        this.generations = generations;
        this.media = media;
        this.root = root;
        this.grade = grade;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String filePath;
    public String githubRepoFullName;
    public String repoStatus;
    public Integer aheadBy;
    public Integer behindBy;
    public Integer totalCommits;

    public Boolean submittedPRtoParent;
    public Boolean submittedPRtoParentMergeable;
    public Boolean submittedPRtoParentRejected;
    public Boolean submittedPRfromParent;
    public Boolean submittedPRfromParentRejected;
    public Boolean submittedPRfromParentMergeable;

    public Boolean isForked = false;

    public String createdAt;
    public String updatedAt;
}
