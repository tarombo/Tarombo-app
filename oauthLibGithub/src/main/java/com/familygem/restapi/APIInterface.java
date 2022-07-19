package com.familygem.restapi;

import com.familygem.restapi.models.Commit;
import com.familygem.restapi.models.CompareCommit;
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.CreateBlobResult;
import com.familygem.restapi.models.FileContent;
import com.familygem.restapi.models.Invitation;
import com.familygem.restapi.models.PRFile;
import com.familygem.restapi.models.Pull;
import com.familygem.restapi.models.RefResult;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.SearchUsersResult;
import com.familygem.restapi.models.TreeResult;
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.CommitTreeRequest;
import com.familygem.restapi.requestmodels.CreateBlobRequestModel;
import com.familygem.restapi.requestmodels.CreateTreeRequestModel;
import com.familygem.restapi.requestmodels.FileRequestModel;
import com.familygem.restapi.requestmodels.CreateRepoRequestModel;
import com.familygem.restapi.requestmodels.MergeUpstreamRequestModel;
import com.familygem.restapi.requestmodels.PullRequestModel;
import com.familygem.restapi.requestmodels.PullRequestUpdateModel;
import com.familygem.restapi.requestmodels.UpdateRefRequestModel;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.HTTP;
import retrofit2.http.Headers;
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

    @GET("/search/users")
    Call<SearchUsersResult> searchUsers(@Query("q") String q);

    @PUT("/repos/{owner}/{repo}/contents/{path}")
    Call<FileContent> createFile(@Path("owner") String owner,
                                 @Path("repo") String repoName,
                                 @Path("path") String fileName,
                                 @Body final FileRequestModel requestModel);

    @HTTP(method = "DELETE", path = "/repos/{owner}/{repo}/contents/{path}", hasBody = true)
    Call<FileContent> deleteMediaFile(@Path("owner") String owner,
                                 @Path("repo") String repoName,
                                 @Path("path") String path,
                                 @Body final FileRequestModel requestModel);

    @PUT("/repos/{owner}/{repo}/collaborators/{username}")
    Call<Void> addCollaborator(@Path("owner") String owner,
                                 @Path("repo") String repoName,
                                 @Path("username") String invitee);

    @GET("/repos/{owner}/{repo}/collaborators/{username}")
    Call<Void> checkCollaborator(@Path("owner") String owner,
                               @Path("repo") String repoName,
                               @Path("username") String invitee);

    @GET("/user/repository_invitations?per_page=100")
    Call<List<Invitation>> checkInvitationCollaborator();

    @PATCH("/user/repository_invitations/{invitationId}")
    Call<Void> acceptInvitationCollaborator(@Path("invitationId") Integer invitationId);

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

    @GET("/repos/{owner}/{repo}/contents/{path}")
    Call<Content> downloadFile(@Path("owner") String owner,
                               @Path("repo") String repoName,
                               @Path("path") String fileName);

    @Headers("Accept: application/vnd.github.v3.raw+json") //this is the correct way because it can handle ~100MB file but if using raw we can not get SHA of the file
    @GET("/repos/{owner}/{repo}/contents/{path}")
    Call<ResponseBody> downloadFile2(@Path("owner") String owner,
                                     @Path("repo") String repoName,
                                     @Path("path") String fileName);

    // example --> https://raw.githubusercontent.com/asiboro/tarombo-asiboro-20220719150814/main/media/Abbey.jpg
    @GET("/{owner}/{repo}/{path}")
    Call<ResponseBody> downloadRawFile(@Path("owner") String owner,
                                     @Path("repo") String repoName,
                                     @Path("path") String fileName);

    @GET("/repos/{owner}/{repo}/contents/{path}")
    Call<Content> downloadFileByRef(@Path("owner") String owner,
                               @Path("repo") String repoName,
                               @Path("path") String fileName,
                                @Query("ref") String ref);
    @Headers("Accept: application/vnd.github.v3.raw+json")
    @GET("/repos/{owner}/{repo}/contents/{path}")
    Call<ResponseBody> downloadFileByRef2(@Path("owner") String owner,
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

    @GET("/repos/{owner}/{repo}/pulls?state=open&per_page=1&page=1")
    Call<List<Pull>> getPRtoParent(@Path("owner") String owner,
                                @Path("repo") String repoName, @Query("head") String head);

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

    // get base tree sha
    @GET("/repos/{owner}/{repo}/git/trees/main")
    Call<TreeResult> getBaseTree(@Path("owner") String owner,
                                 @Path("repo") String repoName);

    // get base sub folder tree
    @GET("/repos/{owner}/{repo}/git/trees/{tree_sha}")
    Call<TreeResult> getSubFolderTree(@Path("owner") String owner,
                                      @Path("repo") String repoName,
                                      @Path("tree_sha") String treeSha);

    // create a blob
    @POST("/repos/{owner}/{repo}/git/blobs")
    Call<CreateBlobResult> createBlob(@Path("owner") String owner,
                                      @Path("repo") String repoName,
                                      @Body final CreateBlobRequestModel requestModel);

    // create tree
    @POST("/repos/{owner}/{repo}/git/trees")
    Call<TreeResult> createTree(@Path("owner") String owner,
                                @Path("repo") String repoName,
                                @Body final CreateTreeRequestModel requestModel);

    // create commit  tree
    @POST("/repos/{owner}/{repo}/git/commits")
    Call<Commit> createCommitTree(@Path("owner") String owner,
                              @Path("repo") String repoName,
                              @Body final CommitTreeRequest requestModel);

    // update reference of the branch to new commit sha
    @POST("/repos/{owner}/{repo}/git/refs/heads/main")
    Call<RefResult> updateRef(@Path("owner") String owner,
                              @Path("repo") String repoName,
                              @Body final UpdateRefRequestModel requestModel);
}
