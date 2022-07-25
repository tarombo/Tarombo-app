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
import com.familygem.utility.GithubUser;
import com.familygem.utility.Helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class GetColloratorsTask {
    private static final String TAG = "GetColloratorsTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, final String repoFullName,
                               Consumer<List<GithubUser>> afterExecution,
                               Consumer<String> errorExecution) {
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                // prepare api
                SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
                String oauthToken = prefs.getString("oauth_token", null);
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);

                // get username API /user
                File userFile = new File(context.getFilesDir(), "user.json");
                User me = Helper.getUser(userFile);

                String[] repoNameSegments = repoFullName.split("/");
                Log.d(TAG, "owner:" + repoNameSegments[0] + " repo:" + repoNameSegments[1]);

                Call<List<User>> getContributorsCall = apiInterface.getCollorators(repoNameSegments[0], repoNameSegments[1]);
                Response<List<User>> responseGetContributors = getContributorsCall.execute();
                final List<User> contributors = responseGetContributors.body();
                if (contributors != null && contributors.size() > 0) {
                    List<GithubUser> users = new ArrayList<>();
                    for (User item : contributors) {
                        if (item.login.equals(me.login))
                            continue;
                        GithubUser user = new GithubUser(item.login, item.getUserName(), item.avatarUrl);
                        users.add(user);
                    }
                    handler.post(() -> afterExecution.accept(users));
                } else {
                    handler.post(() -> afterExecution.accept(new ArrayList<>()));
                }
            } catch (Exception ex) {
                Log.e(TAG, "GetColloratorsTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
