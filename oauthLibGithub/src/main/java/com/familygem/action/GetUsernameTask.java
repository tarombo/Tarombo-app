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
import com.familygem.utility.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class GetUsernameTask {
    private static final String TAG = "GetUsernameTask";
    public static void execute(Context context, Consumer<String> afterExecution,
                               Consumer<String> errorExecution) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                File userFile = new File(context.getFilesDir(), "user.json");
                if (userFile.exists()) {
                    User user = Helper.getUser(userFile);
                    if (user != null && user.login != null) {
                        handler.post(() -> afterExecution.accept(user.login));
                        return;
                    }
                }

                // prepare api
                SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
                String oauthToken = prefs.getString("oauth_token", null);
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);

                // get username API /user
                Call<User> userInfoCall = apiInterface.doGeMyUserInfo();
                Response<User> userResponse = userInfoCall.execute();
                User user = userResponse.body();

                // save user object to user.json
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonUser = gson.toJson(user);
                FileUtils.writeStringToFile(new File(context.getFilesDir(), "user.json"), jsonUser, "UTF-8");

                //UI Thread work here
                handler.post(() -> afterExecution.accept(user.login));

            } catch (Exception ex) {
                Log.e(TAG, "GetUsernameTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
