package com.familygem.action;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
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
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.FileContent;
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.CommitterRequestModel;
import com.familygem.restapi.requestmodels.FileRequestModel;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class SaveTreeFileTask {
    private static final String TAG = "SaveTreeFileTask";
    public static void execute(Context context, final String repoFullName, final String email, int treeId,
                               String gcJsonString,
                               Runnable beforeExecution, Runnable afterExecution,
                               Consumer<String> errorExecution) {

        Handler handler = new Handler(Looper.getMainLooper());
        final ExecutorService executor = ExecutorSingleton.getInstance().getExecutor();
        executor.execute(() -> {
            // background thread
            try {
                handler.post(beforeExecution);
                if (repoFullName == null || "".equals(repoFullName)) {
                    handler.post(afterExecution);
                    return;
                }

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
                Gson gson = new Gson();

                // upload tree.json
                File treeFileContent = new File(context.getFilesDir(), treeId + ".content");
                Content treeContentInfo = Helper.getContent(treeFileContent);
                String shaTreeString = treeContentInfo.sha;
                String treeFileContentBase64 = Base64.encodeToString(gcJsonString.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
                FileRequestModel replaceTreeJsonRequestModel = new FileRequestModel(
                        "save data",
                        treeFileContentBase64,
                        new CommitterRequestModel(user.name, email)
                );
                replaceTreeJsonRequestModel.sha = shaTreeString;
                Call<FileContent> replaceTreeJsonCall = apiInterface.replaceFile(user.login, repoNameSegments[1],
                        "tree.json", replaceTreeJsonRequestModel);
                Response<FileContent> treeJsonResponse = replaceTreeJsonCall.execute();
                if (treeJsonResponse.code() == 409) {
                    handler.post(() -> errorExecution.accept(context.getString(R.string.error_commit_hash_obsolete)));
                } else {
                    FileContent treeJsonFileContent = treeJsonResponse.body();
                    String treeJsonContent = gson.toJson(treeJsonFileContent.content);
                    FileUtils.writeStringToFile(treeFileContent, treeJsonContent, "UTF-8");

                    // get last commit
                    Call<List<Commit>> commitsCall = apiInterface.getLatestCommit(user.login, repoNameSegments[1]);
                    Response<List<Commit>> commitsResponse = commitsCall.execute();
                    List<Commit> commits = commitsResponse.body();
                    String commitStr = gson.toJson(commits.get(0));
                    FileUtils.writeStringToFile(new File(context.getFilesDir(), treeId + ".commit"), commitStr, "UTF-8");

                    handler.post(afterExecution);
                }
            }catch (Throwable ex) {
                Log.e(TAG, "SaveTreeAndInfoFileTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
