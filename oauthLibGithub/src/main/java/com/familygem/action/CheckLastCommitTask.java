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
import com.familygem.restapi.models.Commit;
import com.familygem.restapi.models.User;
import com.familygem.utility.Helper;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class CheckLastCommitTask {
    private static final String TAG = "CheckLastCommitTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, final String repoFullName, int treeId, Consumer<Boolean> afterExecution,
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
                String[] repoNameSegments = repoFullName.split("/");
                Log.d(TAG, "owner:" + repoNameSegments[0] + " repo:" + repoNameSegments[1]);

                // get last commit from server
                Call<List<Commit>> commitsCall = apiInterface.getLatestCommit(user.login, repoNameSegments[1]);
                Response<List<Commit>> commitsResponse = commitsCall.execute();
                if (commitsResponse.code() == 404) {
                    // delete local files related with repo
                    Helper.deleteLocalFilesOfRepo(context, treeId);
                    handler.post(() -> errorExecution.accept("E404"));
                    return;
                }
                List<Commit> commits = commitsResponse.body();
                Commit lastCommitServer = commits.get(0);
                Boolean isLocalCommitObsolete = true;
                // get last commit local
                File commitFile = new File(context.getFilesDir(), treeId + ".commit");
                if (commitFile.exists()) {
                    Commit lastCommitLocal = Helper.getCommit(commitFile);
                    if (lastCommitLocal != null && lastCommitLocal.sha != null)
                        isLocalCommitObsolete = !lastCommitLocal.sha.equals(lastCommitServer.sha);
                }

                final Boolean finalIsLocalCommitObsolete = isLocalCommitObsolete;
                handler.post(() -> afterExecution.accept(finalIsLocalCommitObsolete));
            }catch (Throwable ex) {
                Log.e(TAG, "CheckLastCommitTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
