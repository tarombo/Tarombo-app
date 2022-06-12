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
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.PullRequestUpdateModel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class RejectPRTask {
    private static final String TAG = "RejectPRTask";
    public static void execute(Context context, String repoFullName, int prNumber,
                               Runnable afterExecution,
                               Consumer<String> errorExecution) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
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

                String[] repoNameSegments = repoFullName.split("/");
                Log.d(TAG, "owner:" + repoNameSegments[0] + " repo:" + repoNameSegments[1]);

                // close PR
                PullRequestUpdateModel pullRequestUpdateModel = new PullRequestUpdateModel(
                        "Reject pull request",
                        "Reject pull request",
                        "closed",
                        "main"
                );
                Call<Void> updatePrCall = apiInterface.closePR(user.login, repoNameSegments[1], prNumber, pullRequestUpdateModel);
                updatePrCall.execute();

                handler.post(afterExecution);
            } catch (Throwable ex) {
                Log.e(TAG, "RejectPRTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
