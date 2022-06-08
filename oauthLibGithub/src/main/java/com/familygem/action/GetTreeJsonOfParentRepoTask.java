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
import com.familygem.restapi.APIInterface;
import com.familygem.restapi.ApiClient;
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;
import com.familygem.utility.Helper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class GetTreeJsonOfParentRepoTask {
    private static final String TAG = "GetTreeJsonOfParent";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, int treeId,
                               Runnable afterExecution,
                               Consumer<String> errorExecution) {
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // background thread
            try {
                // prepare api
                SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
                String oauthToken = prefs.getString("oauth_token", null);
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);

                Repo repo = Helper.getRepo(new File(context.getFilesDir(), treeId + ".repo"));
                String[] repoParentNameSegments = repo.parent.fullName.split("/");

                // download file tree.json
                Call<Content> downloadTreeJsonCall = apiInterface.downloadFile(repo.parent.owner.login, repoParentNameSegments[1], "tree.json");
                Response<Content> treeJsonContentResponse = downloadTreeJsonCall.execute();
                Content treeJsonContent = treeJsonContentResponse.body();
                // save tree.json to local directory
                byte[] treeJsonContentBytes = Base64.decode(treeJsonContent.content, Base64.DEFAULT);
                String treeJsonString = new String(treeJsonContentBytes, StandardCharsets.UTF_8);
                File treeJsonFile = new File(context.getFilesDir(), treeId + ".json.parent");
                FileUtils.writeStringToFile(treeJsonFile, treeJsonString, "UTF-8");

                handler.post(afterExecution);
            } catch (Throwable ex) {
                Log.e(TAG, "GetTreeJsonOfParentRepoTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
