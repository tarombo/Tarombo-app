package com.familygem.restapi;

import com.familygem.restapi.models.CompareCommit;
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.FileContent;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.FileRequestModel;
import com.familygem.restapi.requestmodels.CreateRepoRequestModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
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
    Call<FileContent> createFile(@Path("owner") String owner,
                                 @Path("repo") String repoName,
                                 @Path("path") String fileName,
                                 @Body final FileRequestModel requestModel);

    @PUT("/repos/{owner}/{repo}/contents/{path}")
    Call<FileContent> replaceFile(@Path("owner") String owner,
                                 @Path("repo") String repoName,
                                 @Path("path") String fileName,
                                 @Body final FileRequestModel requestModel);

    @POST("/repos/{owner}/{repo}/forks")
    Call<Repo> forkUserRepo(@Path("owner") String owner,
                            @Path("repo") String repoName);

    @DELETE("/repos/{owner}/{repo}")
    Call<Void> deleteUserRepo(@Path("owner") String owner,
                            @Path("repo") String repoName);

//    @Headers("Accept: application/vnd.github.v3.raw+json") this is the correct way because it can handle ~100MB file but if using raw we can not get SHA of the file
    @GET("/repos/{owner}/{repo}/contents/{path}")
    Call<Content> downloadFile(@Path("owner") String owner,
                               @Path("repo") String repoName,
                               @Path("path") String fileName);

    @GET("/repos/{owner}/{repo}/compare/{basehead}?page=1&per_page=1")
    Call<CompareCommit> compareCommit(@Path("owner") String owner,
                                     @Path("repo") String repoName,
                                     @Path("basehead") String basehead);

}
