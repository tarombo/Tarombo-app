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
import com.familygem.restapi.models.SearchUsersResult;
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

public class SearchUsersTask {
    private static final String TAG = "SearchUsersTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, String query,
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

                // search users
                Call<SearchUsersResult> searchUsersCall = apiInterface.searchUsers(query);
                Response<SearchUsersResult> searchUsersResponse = searchUsersCall.execute();
                SearchUsersResult searchUsersResult = searchUsersResponse.body();
                if (searchUsersResult != null && searchUsersResult.totalCount > 0) {
                    List<GithubUser> users = new ArrayList<>();
                    for (User item : searchUsersResult.items) {
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
                Log.e(TAG, "GetOpenPRTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
