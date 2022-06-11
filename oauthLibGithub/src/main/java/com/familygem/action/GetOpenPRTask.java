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
import com.familygem.restapi.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class GetOpenPRTask {
    private static final String TAG = "GetOpenPRTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, final String repoFullName, int treeId,
                               Consumer<List<Pull>> afterExecution,
                               Consumer<String> errorExecution) {

        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // background thread
            try {
                if (repoFullName == null || "".equals(repoFullName)) {
                    handler.post(() -> afterExecution.accept(new ArrayList<>()));
                    return;
                }
                String[] repoNameSegments = repoFullName.split("/");

                // prepare api
                SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
                String oauthToken = prefs.getString("oauth_token", null);
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);

                // get username API /user
                Call<User> userInfoCall = apiInterface.doGeMyUserInfo();
                Response<User> userResponse = userInfoCall.execute();
                User user = userResponse.body();


                // get open PR
                // TODO implement paging
                Call<List<Pull>> listPRCall = apiInterface.listOpenPR(user.login, repoNameSegments[1], 10, 1);
                Response<List<Pull>> listPRResponse = listPRCall.execute();
                List<Pull> listPR = listPRResponse.body();


                handler.post(() -> afterExecution.accept(listPR));
            } catch (Throwable ex) {
                Log.e(TAG, "GetOpenPRTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
