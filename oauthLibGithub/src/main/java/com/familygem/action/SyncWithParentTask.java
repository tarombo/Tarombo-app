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
import com.familygem.restapi.requestmodels.MergeUpstreamRequestModel;
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

public class SyncWithParentTask {
    private static final String TAG = "SyncWithParentTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, final String repoFullName, int treeId,
                               Consumer<Boolean> afterExecution,
                               Consumer<String> errorExecution) {

        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // background thread
            try {

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
                boolean mergeable = false;
                if (repo.fork && repo.parent != null) {
                    /*
                     Sync a branch of a forked repository to keep it up-to-date with the upstream repository.
                    curl \
                          -X POST \
                          -H "Accept: application/vnd.github.v3+json" \
                          https://api.github.com/repos/OWNER/REPO/merge-upstream \
                          -d '{"branch":"main"}'
                     */
                    // merge-upstream
                    MergeUpstreamRequestModel requestModel = new MergeUpstreamRequestModel();
                    requestModel.branch = "main";
                    Call<Void> mergeUpstreamCall = apiInterface.mergeUpstream(
                            user.login, forkedRepoNameSegments[1], requestModel);
                    Response<Void> mergeUpstreamResponse = mergeUpstreamCall.execute();
                    mergeable = mergeUpstreamResponse.code() == 200;

                }
                boolean finalMergeable = mergeable;
                handler.post(() -> afterExecution.accept(finalMergeable));
            }catch (Throwable ex) {
                Log.e(TAG, "SyncWithParentTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
