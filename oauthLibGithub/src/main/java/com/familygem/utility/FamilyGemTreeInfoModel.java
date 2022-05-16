package com.familygem.utility;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

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

    public FamilyGemTreeInfoModel(String title, int persons, int generations, int media, String root, int grade) {
        this.title = title;
        this.persons = persons;
        this.generations = generations;
        this.media = media;
        this.root = root;
        this.grade = grade;
    }

    public String filePath;
    public String githubRepoFullName;
}
