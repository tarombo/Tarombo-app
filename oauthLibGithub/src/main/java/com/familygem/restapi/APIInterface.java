package com.familygem.restapi;

import com.familygem.restapi.models.Repo;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;

public interface APIInterface {
    @GET("/user/repos")
    Call<List<Repo>> doGetListUserRepos();
}
