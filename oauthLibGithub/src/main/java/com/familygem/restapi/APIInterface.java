package com.familygem.restapi;

import com.familygem.restapi.models.FileContent;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.CreateFileRequestModel;
import com.familygem.restapi.requestmodels.CreateRepoRequestModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface APIInterface {
    @GET("/user/repos")
    Call<List<Repo>> doGetListUserRepos();

    @POST("/user/repos")
    Call<Repo> createUserRepo(@Body final CreateRepoRequestModel requestModel);

    @GET("/user")
    Call<User> doGeMyUserInfo();

    @PUT("/repos/{owner}/{repo}/contents/{path}")
    Call<FileContent> createTreeJsonFile(@Path("owner") String owner,
                                         @Path("repo") String repoName,
                                         @Path("path") String fileName,
                                         @Body final CreateFileRequestModel requestModel);
}
