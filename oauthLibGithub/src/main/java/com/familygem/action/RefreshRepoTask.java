package com.familygem.action;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.core.util.Consumer;

import com.familygem.oauthLibGithub.BuildConfig;
import com.familygem.oauthLibGithub.R;
import com.familygem.restapi.APIInterface;
import com.familygem.restapi.ApiClient;
import com.familygem.restapi.models.Commit;
import com.familygem.restapi.models.FileContent;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.TreeItem;
import com.familygem.restapi.models.TreeResult;
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.CommitterRequestModel;
import com.familygem.restapi.requestmodels.FileRequestModel;
import com.familygem.utility.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;

import retrofit2.Call;
import retrofit2.Response;

public class RefreshRepoTask {
    private static final String TAG = "RefreshRepoTask";
    public static void execute(Context context, final String repoFullName, int treeId,
                               Runnable afterExecution,
                               Consumer<String> errorExecution) {

        Handler handler = new Handler(Looper.getMainLooper());
        final ExecutorService executor = ExecutorSingleton.getInstance().getExecutor();
        executor.execute(() -> {
            // background thread
            try {
                if (repoFullName == null || "".equals(repoFullName)) {
                    handler.post(afterExecution);
                    return;
                }

                // prepare api
                SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
                String oauthToken = prefs.getString("oauth_token", null);
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);

                // check if the repo belongs to himself
                String[] repoNameSegments = repoFullName.split("/");
                Log.d(TAG, "owner:" + repoNameSegments[0] + " repo:" + repoNameSegments[1]);

                refreshRepo(context, apiInterface, repoNameSegments[0], repoNameSegments[1], treeId);
                handler.post(afterExecution);
            }catch (Throwable ex) {
                Log.e(TAG, "Error failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }

    public static void refreshRepo(Context context, APIInterface apiInterface, String owner, String repoName, int treeId) throws IOException {
        Call<Repo> getRepoCall = apiInterface.getRepo(owner, repoName);
        Response<Repo> repoResponse = getRepoCall.execute();
        Log.d(TAG, "repo response code:" + repoResponse.code());

        if(!repoResponse.isSuccessful()){
            return;
        }

        Repo repo = repoResponse.body();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String jsonRepo = gson.toJson(repo);
        FileUtils.writeStringToFile(new File(context.getFilesDir(), treeId + ".repo"), jsonRepo, "UTF-8");
    }
}
