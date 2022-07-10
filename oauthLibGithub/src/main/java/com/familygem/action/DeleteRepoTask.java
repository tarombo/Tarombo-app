package com.familygem.action;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
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

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class DeleteRepoTask {
    private static final String TAG = "DeleteRepoTask";
    public static void execute(Activity activity, int treeId, String repoFullName,
                               Runnable beforeExecution, Runnable afterExecution,
                               Consumer<String> errorExecution) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
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
                SharedPreferences prefs = activity.getSharedPreferences("github_prefs", MODE_PRIVATE);
                String oauthToken = prefs.getString("oauth_token", null);
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);

                // get username API /user
                File userFile = new File(activity.getFilesDir(), "user.json");
                User user = Helper.getUser(userFile);

                // check if the repo belongs to himself
                String[] repoNameSegments = repoFullName.split("/");
                Log.d(TAG, "owner:" + repoNameSegments[0] + " repo:" + repoNameSegments[1]);
                assert user != null;
                if (!repoNameSegments[0].equals(user.login)) {
                    handler.post(afterExecution); // just delete the local files
                    return;
//                    Call<Void> checkCollaboratorCall = apiInterface.checkCollaborator(repoNameSegments[0], repoNameSegments[1], user.login);
//                    Response<Void> responseCheckCollaborator = checkCollaboratorCall.execute();
//                    if (responseCheckCollaborator.code() == 204) {
//                        handler.post(afterExecution);
//                        return;
//                    }
//                    handler.post(() -> errorExecution.accept("E000")); // can't delete somebody else repo
//                    return;
                }

                // call api delete repo
                Call<Void> repoCall = apiInterface.deleteUserRepo(repoNameSegments[0], repoNameSegments[1]);
                Response<Void> repoResponse = repoCall.execute();
                Log.d(TAG, "response code:" + repoResponse.code());

                // delete local files related with repo
                Helper.deleteLocalFilesOfRepo(activity, treeId);

                handler.post(afterExecution);
            } catch (Throwable ex) {
                Log.e(TAG, "DeleteRepoTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
