package com.familygem.action;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.core.util.Consumer;

import com.familygem.oauthLibGithub.BuildConfig;
import com.familygem.restapi.APIInterface;
import com.familygem.restapi.ApiClient;
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.PRFile;
import com.familygem.restapi.models.Pull;
import com.familygem.restapi.models.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import retrofit2.Call;
import retrofit2.Response;

public class GetTreeJsonInPRTask {
    private static final String TAG = "GetTreeJsonInPRTask";
    public static void execute(Context context, String repoFullName, int treeId, int prNumber,
                               Consumer<Boolean> afterExecution,
                               Consumer<String> errorExecution) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // background thread
            try {
                // prepare api
                SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
                String oauthToken = prefs.getString("oauth_token", null);
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);

                // get username API /user
                Call<User> userInfoCall = apiInterface.doGeMyUserInfo();
                Response<User> userResponse = userInfoCall.execute();
                User user = userResponse.body();

                // check if the repo belongs to himself
                String[] repoNameSegments = repoFullName.split("/");
                Log.d(TAG, "owner:" + repoNameSegments[0] + " repo:" + repoNameSegments[1]);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                // get PR info
                Call<Pull> getPrCall = apiInterface.getPR(user.login, repoNameSegments[1], prNumber);
                Response<Pull> getPrResponse = getPrCall.execute();
                Pull getPr = getPrResponse.body();
                boolean mergeable = getPr.mergeable != null && getPr.mergeable;

                // get url of tree.json
                Call<List<PRFile>> getPRFilesCall = apiInterface.getPRFiles(user.login,repoNameSegments[1], prNumber);
                Response<List<PRFile>> getPRFilesResponse = getPRFilesCall.execute();
                List<PRFile> prFiles = getPRFilesResponse.body();
                if (prFiles != null && prFiles.size() > 1) {
                    PRFile prFile = null;
                    for (PRFile _prFile : prFiles) {
                        if (_prFile.filename.equals("tree.json")) {
                            prFile = _prFile;
                            break;
                        }
                    }

                    if (prFile != null) {
                        Uri uri = Uri.parse(prFile.contentsUrl);
                        // download file tree.json
                        Call<Content> downloadTreeJsonCall = apiInterface.downloadFileByRef(user.login, repoNameSegments[1], "tree.json", uri.getQueryParameter("ref"));
                        Response<Content> treeJsonContentResponse = downloadTreeJsonCall.execute();
                        Content treeJsonContent = treeJsonContentResponse.body();
                        // save tree.json to local directory
                        byte[] treeJsonContentBytes = Base64.decode(treeJsonContent.content, Base64.DEFAULT);
                        String treeJsonString = new String(treeJsonContentBytes, StandardCharsets.UTF_8);
                        File treeJsonFile = new File(context.getFilesDir(), treeId + ".json.PR");
                        FileUtils.writeStringToFile(treeJsonFile, treeJsonString, "UTF-8");
                    }
                }

                handler.post(() -> afterExecution.accept(mergeable));
            } catch (Throwable ex) {
                Log.e(TAG, "GetTreeJsonInPRTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}