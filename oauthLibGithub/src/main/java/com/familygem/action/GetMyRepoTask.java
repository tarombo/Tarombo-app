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
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class GetMyRepoTask {
    private static final String TAG = "GetMyRepoTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context,
                               Consumer<List<Repo>> afterExecution,
                               Consumer<String> errorExecution) {

        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // background thread
            try {
                // prepare api
                SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
                String oauthToken = prefs.getString("oauth_token", null);
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);

                // get my repos
                Call<List<Repo>> getMyReposCall = apiInterface.getMyRepos();
                Response<List<Repo>> getMyReposResponse = getMyReposCall.execute();
                List<Repo> myRepos = getMyReposResponse.body();

                handler.post(() -> afterExecution.accept(myRepos));
            }catch (Throwable ex) {
                Log.e(TAG, "GetMyRepoTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
