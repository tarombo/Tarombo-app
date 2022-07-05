package com.familygem.utility;

public class GithubUser {
    private String name;
    private String userName;
    private String avatarUrl;

    public GithubUser(String userName, String name, String avatarUrl) {
        this.name = name;
        this.userName = userName;
        this.avatarUrl = avatarUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
