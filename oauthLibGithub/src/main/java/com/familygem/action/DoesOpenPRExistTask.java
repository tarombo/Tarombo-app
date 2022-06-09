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
import com.familygem.utility.FamilyGemTreeInfoModel;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class DoesOpenPRExistTask {
    private static final String TAG = "DoesOpenPRExistTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, final String repoFullName, int treeId,
                               Consumer<Boolean> afterExecution,
                               Consumer<String> errorExecution) {

        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // background thread
            try {
                if (repoFullName == null || "".equals(repoFullName)) {
                    handler.post(() -> afterExecution.accept(false));
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

                Boolean isOpenPRExist = false;
                // get one open PR
                Call<List<Pull>> listPRCall = apiInterface.listOpenPR(user.login, repoNameSegments[1], 1, 1);
                Response<List<Pull>> listPRResponse = listPRCall.execute();
                List<Pull> listPR = listPRResponse.body();
                if (listPR != null && listPR.size() > 0)
                    isOpenPRExist = true;

                Boolean finalIsOpenPRExist = isOpenPRExist;
                handler.post(() -> afterExecution.accept(finalIsOpenPRExist));
            } catch (Throwable ex) {
                Log.e(TAG, "DoesOpenPRExistTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
