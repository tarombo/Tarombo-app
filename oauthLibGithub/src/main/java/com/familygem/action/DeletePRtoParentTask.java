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
import com.familygem.restapi.requestmodels.PullRequestUpdateModel;
import com.familygem.utility.Helper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class DeletePRtoParentTask {
    private static final String TAG = "DeletePRtoParentTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, final String repoFullName, int treeId,
                               Runnable beforeExecution, Runnable afterExecution,
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
                Call<User> userInfoCall = apiInterface.doGeMyUserInfo();
                Response<User> userResponse = userInfoCall.execute();
                User user = userResponse.body();

                // check if the repo belongs to himself
                String[] forkedRepoNameSegments = repoFullName.split("/");
                Log.d(TAG, "forked repo owner:" + forkedRepoNameSegments[0] + " repo:" + forkedRepoNameSegments[1]);

                Repo repo = Helper.getRepo(new File(context.getFilesDir(), treeId + ".repo"));
                if (repo.fork && repo.parent != null) {
                    /*
                     pull request from forked repo (putrasto) to parent repo (asiboro)
                     POST https://api.github.com/repos/asiboro/tarombo-asiboro-20220516191124/pulls/6
                     {
                            "title":"CLOSE Amazing new feature",
                            "body":"CLOSE Please pull these awesome changes in!",
                            "base":"main",
                            "state": "closed"
                    }
                     */
                    String[] repoParentNameSegments = repo.parent.fullName.split("/");

                    // get PR file
                    File filePull = new File(context.getFilesDir(), treeId + ".PRtoParent");
                    Pull pullLocal = Helper.getPR(filePull);

                    // close PR
                    PullRequestUpdateModel pullRequestModel = new PullRequestUpdateModel(
                            "Discard pull request",
                            "Discard pull request",
                            "closed",
                            "main"
                    );
                    Call<Void> updatePrCall = apiInterface.closePR(repoParentNameSegments[0], forkedRepoNameSegments[1], pullLocal.number, pullRequestModel);
                    updatePrCall.execute();

                    // delete PR to parent .PRtoParent file
                    new File(context.getFilesDir(), treeId + ".PRtoParent").delete();
//                    FileUtils.deleteQuietly(new File(context.getFilesDir(), treeId + ".PRtoParent"));
                }
                handler.post(afterExecution);
            }catch (Throwable ex) {
                Log.e(TAG, "DeletePRtoParentTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
