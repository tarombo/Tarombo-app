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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;

public class InviteCollaboratorsTask {
    private static final String TAG = "InviteCollaboratorsTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, final String repoFullName, final List<GithubUser> invitees,
                               Runnable afterExecution,
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
                User user = Helper.getUser(userFile);

                String[] repoNameSegments = repoFullName.split("/");
                Log.d(TAG, "owner:" + repoNameSegments[0] + " repo:" + repoNameSegments[1]);

                for (GithubUser invitee : invitees) {
                    Call<Void> addCollaboratorCall = apiInterface.addCollaborator(user.login, repoNameSegments[1], invitee.getUserName());
                    addCollaboratorCall.execute();
                }
                handler.post(afterExecution);

            } catch (Exception ex) {
                Log.e(TAG, "InviteCollaboratorsTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
