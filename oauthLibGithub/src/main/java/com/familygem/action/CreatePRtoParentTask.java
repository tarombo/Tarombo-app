package com.familygem.action;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.util.Consumer;

import com.familygem.oauthLibGithub.BuildConfig;
import com.familygem.restapi.APIInterface;
import com.familygem.restapi.ApiClient;
import com.familygem.restapi.models.Pull;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.PullRequestModel;
import com.familygem.restapi.requestmodels.PullRequestUpdateModel;
import com.familygem.utility.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class CreatePRtoParentTask {
    private static final String TAG = "CompareRepoTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, final String repoFullName, int treeId,
                               Runnable beforeExecution, Consumer<Boolean> afterExecution,
                               Consumer<String> errorExecution) {

        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // background thread
            try {
                handler.post(beforeExecution);

                // prepare api
                SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
                String oauthToken = prefs.getString("oauth_token", null);
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);

                // get username API /user
                File userFile = new File(context.getFilesDir(), "user.json");
                User user = Helper.getUser(userFile);

                // check if the repo belongs to himself
                String[] forkedRepoNameSegments = repoFullName.split("/");
                Log.d(TAG, "forked repo owner:" + forkedRepoNameSegments[0] + " repo:" + forkedRepoNameSegments[1]);

                Repo repo = Helper.getRepo(new File(context.getFilesDir(), treeId + ".repo"));
                boolean mergeable = false;
                if (repo.fork && repo.parent != null) {
                    /*
                     pull request from forked repo (putrasto) to parent repo (asiboro)
                     POST https://api.github.com/repos/asiboro/tarombo-asiboro-20220516191124/pulls
                     {
                        "title":"Amazing new feature XXXXX ",
                        "body":"Please pull these awesome changes in! 2",
                        "head":"putrasto:main",
                        "base":"main",
                        "draft": false
                    }
                     */
                    String[] repoParentNameSegments = repo.parent.fullName.split("/");

                    // create PR
                    PullRequestModel pullRequestModel = new PullRequestModel(
                            "Pull request from " + user.getUserName(),
                            "Pull request from " + user.getUserName(),
                            user.login + ":main",
                            "main"
                    );
                    File prFile = new File(context.getFilesDir(), treeId + ".PRtoParent");
                    if (prFile.exists()) {
                        Pull pullLocal = Helper.getPR(prFile);

                        // get last status of this PR
                        Call<Pull> getPrCall = apiInterface.getPR(repoParentNameSegments[0], forkedRepoNameSegments[1], pullLocal.number);
                        Response<Pull> getPrResponse = getPrCall.execute();
                        Pull getPr = getPrResponse.body();

                        if ("open".equals(getPr.state)) {
                            // close PR
                            PullRequestUpdateModel pullRequestUpdateModel = new PullRequestUpdateModel(
                                    "Cancel pull request",
                                    "Cancel pull request",
                                    "closed",
                                    "main"
                            );
                            Call<Void> updatePrCall = apiInterface.closePR(repoParentNameSegments[0], forkedRepoNameSegments[1], pullLocal.number, pullRequestUpdateModel);
                            Response<Void> updatePrRespone = updatePrCall.execute();
                            // ignore response
//                            if (updatePrRespone.code() == 422) {
//                                // something is wrong lets delete the file
//                                prFile.delete();
//                                // silently ignore this error
//                                handler.post(() -> afterExecution.accept(true));
//                                return;
//                            }
                        }
                    }

                    Call<Pull> createPrCall = apiInterface.createPR(repoParentNameSegments[0], forkedRepoNameSegments[1], pullRequestModel);
                    Response<Pull> createPrResponse = createPrCall.execute();
                    Pull createPr = createPrResponse.body();
                    if (createPr == null) {
                        handler.post(() -> errorExecution.accept("E404"));
                        return;
                    }

                    // give time the github server to process
                    Thread.sleep(5000);

                    // get PR to find out mergeable or not see https://github.com/octokit/octokit.net/issues/1710#issuecomment-342331188
                    Call<Pull> getPrCall = apiInterface.getPR(repoParentNameSegments[0], forkedRepoNameSegments[1], createPr.number);
                    Response<Pull> getPrResponse = getPrCall.execute();
                    Pull getPr = getPrResponse.body();
                    mergeable = getPr.mergeable != null && getPr.mergeable;

                    // save PR to local
                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    String jsonPr = gson.toJson(getPr);
                    FileUtils.writeStringToFile(prFile, jsonPr, "UTF-8");
                }
                boolean finalMergeable = mergeable;
                handler.post(() -> afterExecution.accept(finalMergeable));
            }catch (Throwable ex) {
                Log.e(TAG, "CompareRepoTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
