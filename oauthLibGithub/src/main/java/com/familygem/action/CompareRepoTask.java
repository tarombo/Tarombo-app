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
import com.familygem.restapi.models.CompareCommit;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class CompareRepoTask {
    private static final String TAG = "CompareRepoTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, final String repoFullName, int treeId, FamilyGemTreeInfoModel treeInfoModel,
                               Runnable beforeExecution, Runnable afterExecution,
                               Consumer<String> errorExecution) {

        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // background thread
            try {
                handler.post(beforeExecution);

                if (repoFullName == null || "".equals(repoFullName)) {
                    handler.post(afterExecution);
                    return;
                }
                // prepare api
                SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
                String oauthToken = prefs.getString("oauth_token", null);
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);

                // get username API /user
                Call<User> userInfoCall = apiInterface.doGeMyUserInfo();
                Response<User> userResponse = userInfoCall.execute();
                User user = userResponse.body();

                // check if the repo belongs to himself
                String[] repoNameSegments = repoFullName.split("/");
                Log.d(TAG, "owner:" + repoNameSegments[0] + " repo:" + repoNameSegments[1]);

                Repo repo = Helper.getRepo(new File(context.getFilesDir(), treeId + ".repo"));
                if (repo.fork && repo.parent != null) {
                    String[] repoParentSegments = repo.parent.fullName.split("/");
                    // compare with original repo
                    String basehead = repoParentSegments[0] + ":main...main";
                    Call<CompareCommit> compareCommitCall = apiInterface.compareCommit(user.login, repoNameSegments[1], basehead);
                    Response<CompareCommit> compareCommitResponse = compareCommitCall.execute();
                    CompareCommit compareCommit = compareCommitResponse.body();
                    treeInfoModel.repoStatus = compareCommit.status;
                    treeInfoModel.aheadBy = compareCommit.aheadBy;
                    treeInfoModel.behindBy = compareCommit.behindBy;
                    treeInfoModel.totalCommits = compareCommit.totalCommits;

                    // TODO if aheadBy = 0 -> remove .PRtoParent  (if exist) also update Settings.json
                    // TODO if behindBy = 0 -> remove .PRfromParent (if exist) also update Settings.json
                }

                handler.post(afterExecution);
            }catch (Throwable ex) {
                Log.e(TAG, "CompareRepoTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
