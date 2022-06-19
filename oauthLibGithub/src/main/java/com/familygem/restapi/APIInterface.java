package com.familygem.restapi;

import com.familygem.restapi.models.Commit;
import com.familygem.restapi.models.CompareCommit;
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.FileContent;
import com.familygem.restapi.models.PRFile;
import com.familygem.restapi.models.Pull;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.FileRequestModel;
import com.familygem.restapi.requestmodels.CreateRepoRequestModel;
import com.familygem.restapi.requestmodels.MergeUpstreamRequestModel;
import com.familygem.restapi.requestmodels.PullRequestModel;
import com.familygem.restapi.requestmodels.PullRequestUpdateModel;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface APIInterface {
    @GET("/user/repos")
    Call<List<Repo>> getMyRepos();

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

    @GET("/repos/{owner}/{repo}")
    Call<Repo> getRepo(@Path("owner") String owner,
                            @Path("repo") String repoName);

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
    @GET("/repos/{owner}/{repo}/contents/{path}")
    Call<Content> downloadFileByRef(@Path("owner") String owner,
                               @Path("repo") String repoName,
                               @Path("path") String fileName,
                                @Query("ref") String ref);

    @GET("/repos/{owner}/{repo}/compare/{basehead}?page=1&per_page=1")
    Call<CompareCommit> compareCommit(@Path("owner") String owner,
                                     @Path("repo") String repoName,
                                     @Path("basehead") String basehead);

    @POST("/repos/{owner}/{repo}/pulls")
    Call<Pull> createPR(@Path("owner") String owner,
                        @Path("repo") String repoName,
                        @Body final PullRequestModel requestModel);


    @GET("/repos/{owner}/{repo}/pulls?state=open")
    Call<List<Pull>> listOpenPR(@Path("owner") String owner,
                        @Path("repo") String repoName,
                        @Query("per_page") int pageSize,
                          @Query("page") int pageNo);

    @GET("/repos/{owner}/{repo}/pulls/{pull_number}")
    Call<Pull> getPR(@Path("owner") String owner,
                     @Path("repo") String repoName,
                     @Path("pull_number") int pullNumber);

    @PUT("/repos/{owner}/{repo}/pulls/{pull_number}/merge")
    Call<Void> mergePR(@Path("owner") String owner,
                     @Path("repo") String repoName,
                     @Path("pull_number") int pullNumber);

    @GET("/repos/{owner}/{repo}/pulls/{pull_number}/files")
    Call<List<PRFile>> getPRFiles(@Path("owner") String owner,
                                  @Path("repo") String repoName,
                                  @Path("pull_number") int pullNumber);

    @PATCH("/repos/{owner}/{repo}/pulls/{pull_number}")
    Call<Void> closePR(@Path("owner") String owner,
                       @Path("repo") String repoName,
                       @Path("pull_number") int pullNumber,
                       @Body final PullRequestUpdateModel requestModel);

    @GET("/repos/{owner}/{repo}/commits?per_page=1")
    Call<List<Commit>> getLatestCommit(@Path("owner") String owner,
                                     @Path("repo") String repoName);

    @GET("/repos/{owner}/{repo}/commits?per_page=2")
    Call<List<Commit>> getPreviousCommitBeforeSha(@Path("owner") String owner,
                                             @Path("repo") String repoName,
                                             @Query("sha") String sha);

    // Sync a fork branch with the upstream repository
    @POST("/repos/{owner}/{repo}/merge-upstream")
    Call<Void> mergeUpstream(@Path("owner") String owner,
                             @Path("repo") String repoName,
                             @Body final MergeUpstreamRequestModel requestModel);

}
